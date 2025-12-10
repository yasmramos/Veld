package com.veld.processor;

import com.veld.runtime.Scope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * Generates the Veld bootstrap class as bytecode using ASM.
 * 
 * This is THE container - ultra-fast with complete functionality.
 * No separate Veld needed.
 * 
 * Optimizations:
 * - final static fields (enables JIT inlining)
 * - <clinit> initialization (JVM-guaranteed thread safety, zero runtime overhead)
 * - Topological sorting (correct dependency order)
 * - invokespecial for constructors (direct call, no virtual dispatch)
 * - Array-based type lookup (no Maps, no reflection)
 * - Compile-time resolved dependencies
 */
public class VeldBootstrapGenerator implements Opcodes {
    
    private static final String VELD_CLASS = "com/veld/generated/Veld";
    private static final String LIST_CLASS = "java/util/List";
    private static final String ARRAYLIST_CLASS = "java/util/ArrayList";
    private static final String COLLECTIONS_CLASS = "java/util/Collections";
    private static final String SYNTHETIC_SETTER_PREFIX = "__di_set_";
    
    private final List<ComponentInfo> components;
    
    public VeldBootstrapGenerator(List<ComponentInfo> components) {
        this.components = components;
    }
    
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // public final class Veld
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, VELD_CLASS, null, "java/lang/Object", null);
        
        // Separate singletons from prototypes
        List<ComponentInfo> singletons = new ArrayList<>();
        List<ComponentInfo> prototypes = new ArrayList<>();
        for (ComponentInfo comp : components) {
            if (comp.getScope() == Scope.SINGLETON) {
                singletons.add(comp);
            } else {
                prototypes.add(comp);
            }
        }
        
        // === SINGLETON FIELDS ===
        for (ComponentInfo comp : singletons) {
            cw.visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                getFieldName(comp),
                "L" + comp.getInternalName() + ";",
                null,
                null
            ).visitEnd();
        }
        
        // === LOOKUP ARRAYS (for get(Class) and getAll(Class)) ===
        // private static final Class<?>[] _types;
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_types", 
            "[Ljava/lang/Class;", null, null).visitEnd();
        // private static final Object[] _instances;
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_instances",
            "[Ljava/lang/Object;", null, null).visitEnd();
        // private static final int[] _scopes; (0=singleton, 1=prototype)
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_scopes",
            "[I", null, null).visitEnd();
        // private static final int[] _protoIdx; (prototype index for each mapping, -1 if singleton)
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_protoIdx",
            "[I", null, null).visitEnd();
        
        // Generate <clinit> with topologically sorted initialization
        generateStaticInit(cw, singletons, prototypes);
        
        // Private constructor
        generatePrivateConstructor(cw);
        
        // === DIRECT GETTERS (ultra-fast path) ===
        for (ComponentInfo comp : singletons) {
            generateSingletonGetter(cw, comp);
        }
        for (ComponentInfo comp : prototypes) {
            generatePrototypeGetter(cw, comp);
        }
        
        // === CONTAINER API ===
        generateCreateContainer(cw);
        generateCreateRegistry(cw);
        generateCreatePrototype(cw, prototypes);
        generateGetByClass(cw);
        generateGetAllByClass(cw);
        generateContains(cw);
        generateComponentCount(cw);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    /**
     * Generates <clinit> that initializes all singletons and lookup arrays.
     */
    private void generateStaticInit(ClassWriter cw, List<ComponentInfo> singletons, 
                                     List<ComponentInfo> prototypes) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        // Topologically sort singletons
        List<ComponentInfo> sorted = topologicalSort(singletons);
        
        // Initialize each singleton
        for (ComponentInfo comp : sorted) {
            String fieldName = getFieldName(comp);
            String fieldType = "L" + comp.getInternalName() + ";";
            
            mv.visitTypeInsn(NEW, comp.getInternalName());
            mv.visitInsn(DUP);
            
            StringBuilder constructorDesc = new StringBuilder("(");
            InjectionPoint constructor = comp.getConstructorInjection();
            List<InjectionPoint.Dependency> deps = constructor != null ? 
                constructor.getDependencies() : Collections.emptyList();
            
            for (InjectionPoint.Dependency dep : deps) {
                String depType = dep.getTypeName().replace('.', '/');
                constructorDesc.append("L").append(depType).append(";");
                
                ComponentInfo depComp = findComponentByType(depType);
                if (depComp != null && depComp.getScope() == Scope.SINGLETON) {
                    mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(depComp), 
                        "L" + depComp.getInternalName() + ";");
                } else if (depComp != null) {
                    mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(depComp),
                        "()L" + depComp.getInternalName() + ";", false);
                } else {
                    mv.visitInsn(ACONST_NULL);
                }
            }
            constructorDesc.append(")V");
            
            mv.visitMethodInsn(INVOKESPECIAL, comp.getInternalName(), "<init>", 
                constructorDesc.toString(), false);
            mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, fieldName, fieldType);
            
            // Field injections for this singleton
            for (InjectionPoint field : comp.getFieldInjections()) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                loadDependencyForInjection(mv, field.getDependencies().get(0));
                
                if (field.requiresSyntheticSetter()) {
                    String setterName = SYNTHETIC_SETTER_PREFIX + field.getName();
                    String setterDesc = "(" + field.getDescriptor() + ")V";
                    mv.visitMethodInsn(INVOKEVIRTUAL, comp.getInternalName(), setterName, setterDesc, false);
                } else {
                    mv.visitFieldInsn(PUTFIELD, comp.getInternalName(), field.getName(), field.getDescriptor());
                }
            }
            
            // Method injections for this singleton
            for (InjectionPoint method : comp.getMethodInjections()) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                for (InjectionPoint.Dependency dep : method.getDependencies()) {
                    loadDependencyForInjection(mv, dep);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, comp.getInternalName(), 
                    method.getName(), method.getDescriptor(), false);
            }
        }
        
        // === Initialize lookup arrays ===
        int totalComponents = components.size();
        
        // Build type->component mapping including interfaces
        List<TypeMapping> mappings = buildTypeMappings();
        int mappingCount = mappings.size();
        
        // _types = new Class[mappingCount];
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        
        // _instances = new Object[mappingCount];
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        
        // _scopes = new int[mappingCount];
        pushInt(mv, mappingCount);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_scopes", "[I");
        
        // _protoIdx = new int[mappingCount];
        pushInt(mv, mappingCount);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_protoIdx", "[I");
        
        // Build prototype index map
        Map<String, Integer> prototypeIndexMap = new HashMap<>();
        for (int p = 0; p < prototypes.size(); p++) {
            prototypeIndexMap.put(prototypes.get(p).getInternalName(), p);
        }
        
        // Fill arrays
        for (int i = 0; i < mappings.size(); i++) {
            TypeMapping m = mappings.get(i);
            
            // _types[i] = Type.class;
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
            pushInt(mv, i);
            mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(m.typeInternal));
            mv.visitInsn(AASTORE);
            
            // _instances[i] = singleton or null (for prototypes, store method index)
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
            pushInt(mv, i);
            if (m.component.getScope() == Scope.SINGLETON) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(m.component),
                    "L" + m.component.getInternalName() + ";");
            } else {
                mv.visitInsn(ACONST_NULL); // Prototype - will call getter
            }
            mv.visitInsn(AASTORE);
            
            // _scopes[i] = scope
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
            pushInt(mv, i);
            pushInt(mv, m.component.getScope() == Scope.SINGLETON ? 0 : 1);
            mv.visitInsn(IASTORE);
            
            // _protoIdx[i] = prototype index or -1
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
            pushInt(mv, i);
            Integer protoIdx = prototypeIndexMap.get(m.component.getInternalName());
            pushInt(mv, protoIdx != null ? protoIdx : -1);
            mv.visitInsn(IASTORE);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Build type mappings: each component is accessible by its class AND its interfaces.
     */
    private List<TypeMapping> buildTypeMappings() {
        List<TypeMapping> mappings = new ArrayList<>();
        for (ComponentInfo comp : components) {
            // Map by concrete type
            mappings.add(new TypeMapping(comp.getInternalName(), comp));
            // Map by interfaces
            for (String iface : comp.getImplementedInterfacesInternal()) {
                mappings.add(new TypeMapping(iface, comp));
            }
        }
        return mappings;
    }
    
    private static class TypeMapping {
        final String typeInternal;
        final ComponentInfo component;
        TypeMapping(String typeInternal, ComponentInfo component) {
            this.typeInternal = typeInternal;
            this.component = component;
        }
    }
    
    /**
     * Generates createContainer() - returns this Veld class as the container.
     */
    private void generateCreateContainer(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "createContainer",
            "()Ljava/lang/Object;",
            null,
            null
        );
        mv.visitCode();
        // Return the Veld class itself as the container
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(VELD_CLASS));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates _createPrototype(int index) - switch-based prototype factory.
     */
    private void generateCreatePrototype(ClassWriter cw, List<ComponentInfo> prototypes) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PRIVATE | ACC_STATIC,
            "_createPrototype",
            "(I)Ljava/lang/Object;",
            null,
            null
        );
        mv.visitCode();
        
        if (prototypes.isEmpty()) {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        } else {
            // tableswitch for prototype creation
            Label defaultLabel = new Label();
            Label[] caseLabels = new Label[prototypes.size()];
            for (int i = 0; i < caseLabels.length; i++) {
                caseLabels[i] = new Label();
            }
            
            mv.visitVarInsn(ILOAD, 0);
            mv.visitTableSwitchInsn(0, prototypes.size() - 1, defaultLabel, caseLabels);
            
            for (int i = 0; i < prototypes.size(); i++) {
                mv.visitLabel(caseLabels[i]);
                ComponentInfo comp = prototypes.get(i);
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(comp),
                    "()L" + comp.getInternalName() + ";", false);
                mv.visitInsn(ARETURN);
            }
            
            mv.visitLabel(defaultLabel);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates createRegistry() - returns ComponentRegistry.
     */
    private void generateCreateRegistry(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "createRegistry",
            "()Lcom/veld/runtime/ComponentRegistry;",
            null,
            null
        );
        mv.visitCode();
        // Create and return a new ComponentRegistry
        mv.visitTypeInsn(NEW, "com/veld/runtime/ComponentRegistry");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "com/veld/runtime/ComponentRegistry", "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates get(Class<T> type) - O(n) scan but ultra-fast for small n.
     * For large component counts, could use perfect hashing.
     */
    private void generateGetByClass(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "get",
            "(Ljava/lang/Class;)Ljava/lang/Object;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;)TT;",
            null
        );
        mv.visitCode();
        
        // int len = _types.length;
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 1); // len
        
        // for (int i = 0; i < len; i++)
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2); // i = 0
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        // if (_types[i] == type)
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ALOAD, 0); // type parameter
        
        Label notEqual = new Label();
        mv.visitJumpInsn(IF_ACMPNE, notEqual);
        
        // Found! Check if singleton or prototype
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IALOAD);
        
        Label isPrototype = new Label();
        mv.visitJumpInsn(IFNE, isPrototype);
        
        // Singleton: return _instances[i]
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitInsn(ARETURN);
        
        // Prototype: call _createPrototype(_protoIdx[i])
        mv.visitLabel(isPrototype);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IALOAD);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "_createPrototype", "(I)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(notEqual);
        mv.visitIincInsn(2, 1); // i++
        mv.visitJumpInsn(GOTO, loopStart);
        
        mv.visitLabel(loopEnd);
        // Not found - return null
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates getAll(Class<T> type) - returns List<T> of all matching components.
     */
    private void generateGetAllByClass(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "getAll",
            "(Ljava/lang/Class;)Ljava/util/List;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;)Ljava/util/List<TT;>;",
            null
        );
        mv.visitCode();
        
        // List result = new ArrayList();
        mv.visitTypeInsn(NEW, ARRAYLIST_CLASS);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST_CLASS, "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 1); // result
        
        // int len = _types.length;
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 2);
        
        // for (int i = 0; i < len; i++)
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        // if (type.isAssignableFrom(_types[i]))
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom", 
            "(Ljava/lang/Class;)Z", false);
        
        Label notMatch = new Label();
        mv.visitJumpInsn(IFEQ, notMatch);
        
        // result.add(_instances[i])
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEINTERFACE, LIST_CLASS, "add", "(Ljava/lang/Object;)Z", true);
        mv.visitInsn(POP);
        
        mv.visitLabel(notMatch);
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        
        mv.visitLabel(loopEnd);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates contains(Class<?> type).
     */
    private void generateContains(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "contains",
            "(Ljava/lang/Class;)Z",
            null,
            null
        );
        mv.visitCode();
        
        // Simple: get(type) != null
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "get", 
            "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        
        Label isNull = new Label();
        mv.visitJumpInsn(IFNULL, isNull);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        
        mv.visitLabel(isNull);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates componentCount().
     */
    private void generateComponentCount(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "componentCount",
            "()I",
            null,
            null
        );
        mv.visitCode();
        pushInt(mv, components.size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }
    
    private List<ComponentInfo> topologicalSort(List<ComponentInfo> singletons) {
        Map<String, ComponentInfo> byType = new HashMap<>();
        for (ComponentInfo comp : singletons) {
            byType.put(comp.getInternalName(), comp);
            for (String iface : comp.getImplementedInterfacesInternal()) {
                byType.put(iface, comp);
            }
        }
        
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<ComponentInfo> result = new ArrayList<>();
        
        for (ComponentInfo comp : singletons) {
            visit(comp, byType, visited, visiting, result);
        }
        
        return result;
    }
    
    private void visit(ComponentInfo comp, Map<String, ComponentInfo> byType,
                       Set<String> visited, Set<String> visiting, List<ComponentInfo> result) {
        String key = comp.getInternalName();
        if (visited.contains(key)) return;
        if (visiting.contains(key)) return;
        
        visiting.add(key);
        
        // Constructor dependencies
        InjectionPoint constructor = comp.getConstructorInjection();
        if (constructor != null) {
            for (InjectionPoint.Dependency dep : constructor.getDependencies()) {
                String depType = dep.getTypeName().replace('.', '/');
                ComponentInfo depComp = byType.get(depType);
                if (depComp != null && depComp.getScope() == Scope.SINGLETON) {
                    visit(depComp, byType, visited, visiting, result);
                }
            }
        }
        
        // Field injection dependencies
        for (InjectionPoint field : comp.getFieldInjections()) {
            for (InjectionPoint.Dependency dep : field.getDependencies()) {
                String depType = dep.getTypeName().replace('.', '/');
                ComponentInfo depComp = byType.get(depType);
                if (depComp != null && depComp.getScope() == Scope.SINGLETON) {
                    visit(depComp, byType, visited, visiting, result);
                }
            }
        }
        
        // Method injection dependencies
        for (InjectionPoint method : comp.getMethodInjections()) {
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                String depType = dep.getTypeName().replace('.', '/');
                ComponentInfo depComp = byType.get(depType);
                if (depComp != null && depComp.getScope() == Scope.SINGLETON) {
                    visit(depComp, byType, visited, visiting, result);
                }
            }
        }
        
        visiting.remove(key);
        visited.add(key);
        result.add(comp);
    }
    
    private void generatePrivateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates ULTRA-FAST singleton getter - just 2 bytecode instructions.
     */
    private void generateSingletonGetter(ClassWriter cw, ComponentInfo comp) {
        String methodName = getMethodName(comp);
        String fieldName = getFieldName(comp);
        String returnType = "L" + comp.getInternalName() + ";";
        
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            methodName,
            "()" + returnType,
            null,
            null
        );
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, returnType);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generatePrototypeGetter(ClassWriter cw, ComponentInfo comp) {
        String methodName = getMethodName(comp);
        String returnType = "L" + comp.getInternalName() + ";";
        
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            methodName,
            "()" + returnType,
            null,
            null
        );
        mv.visitCode();
        
        mv.visitTypeInsn(NEW, comp.getInternalName());
        mv.visitInsn(DUP);
        
        StringBuilder constructorDesc = new StringBuilder("(");
        InjectionPoint constructor = comp.getConstructorInjection();
        List<InjectionPoint.Dependency> deps = constructor != null ? 
            constructor.getDependencies() : Collections.emptyList();
        
        for (InjectionPoint.Dependency dep : deps) {
            String depType = dep.getTypeName().replace('.', '/');
            constructorDesc.append("L").append(depType).append(";");
            
            ComponentInfo depComp = findComponentByType(depType);
            if (depComp != null) {
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(depComp),
                    "()L" + depComp.getInternalName() + ";", false);
            } else {
                mv.visitInsn(ACONST_NULL);
            }
        }
        constructorDesc.append(")V");
        
        mv.visitMethodInsn(INVOKESPECIAL, comp.getInternalName(), "<init>", 
            constructorDesc.toString(), false);
        
        // Store in local var for field/method injection
        mv.visitVarInsn(ASTORE, 0);
        
        // Field injections
        for (InjectionPoint field : comp.getFieldInjections()) {
            mv.visitVarInsn(ALOAD, 0);
            loadDependencyForInjection(mv, field.getDependencies().get(0));
            
            if (field.requiresSyntheticSetter()) {
                String setterName = SYNTHETIC_SETTER_PREFIX + field.getName();
                String setterDesc = "(" + field.getDescriptor() + ")V";
                mv.visitMethodInsn(INVOKEVIRTUAL, comp.getInternalName(), setterName, setterDesc, false);
            } else {
                mv.visitFieldInsn(PUTFIELD, comp.getInternalName(), field.getName(), field.getDescriptor());
            }
        }
        
        // Method injections
        for (InjectionPoint method : comp.getMethodInjections()) {
            mv.visitVarInsn(ALOAD, 0);
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                loadDependencyForInjection(mv, dep);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, comp.getInternalName(), 
                method.getName(), method.getDescriptor(), false);
        }
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private ComponentInfo findComponentByType(String internalName) {
        for (ComponentInfo comp : components) {
            if (comp.getInternalName().equals(internalName)) {
                return comp;
            }
            for (String iface : comp.getImplementedInterfacesInternal()) {
                if (iface.equals(internalName)) {
                    return comp;
                }
            }
        }
        return null;
    }
    
    private String getMethodName(ComponentInfo comp) {
        String simpleName = comp.getClassName();
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = simpleName.substring(lastDot + 1);
        }
        if (simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(1))) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
    
    private String getFieldName(ComponentInfo comp) {
        return "_" + getMethodName(comp);
    }
    
    /**
     * Loads a dependency onto the stack for field/method injection.
     * Uses Veld.get(Class) to resolve at runtime.
     */
    private void loadDependencyForInjection(MethodVisitor mv, InjectionPoint.Dependency dep) {
        String depInternal = dep.getTypeName().replace('.', '/');
        
        // Veld.get(DependencyType.class)
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(depInternal));
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "get",
                "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        
        // Cast to dependency type
        mv.visitTypeInsn(CHECKCAST, depInternal);
    }
    
    public String getClassName() {
        return VELD_CLASS.replace('/', '.');
    }
}
