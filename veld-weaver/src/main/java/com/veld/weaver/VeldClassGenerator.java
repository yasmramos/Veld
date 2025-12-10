package com.veld.weaver;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Generates Veld.class bytecode from component metadata.
 * This runs AFTER the weaver adds synthetic setters, so all injection works.
 */
public class VeldClassGenerator implements Opcodes {
    
    private static final String VELD_CLASS = "com/veld/Veld";
    private static final String SYNTHETIC_SETTER_PREFIX = "__di_set_";
    
    private final List<ComponentMeta> components;
    
    public VeldClassGenerator(List<ComponentMeta> components) {
        this.components = components;
    }
    
    /**
     * Reads component metadata from the standard location.
     */
    public static List<ComponentMeta> readMetadata(Path classesDir) throws IOException {
        Path metaFile = classesDir.resolve("META-INF/veld/components.meta");
        if (!Files.exists(metaFile)) {
            return Collections.emptyList();
        }
        
        List<ComponentMeta> result = new ArrayList<>();
        for (String line : Files.readAllLines(metaFile)) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            result.add(ComponentMeta.parse(line));
        }
        return result;
    }
    
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, VELD_CLASS, null, "java/lang/Object", null);
        
        List<ComponentMeta> singletons = new ArrayList<>();
        List<ComponentMeta> prototypes = new ArrayList<>();
        for (ComponentMeta comp : components) {
            if ("SINGLETON".equals(comp.scope)) {
                singletons.add(comp);
            } else {
                prototypes.add(comp);
            }
        }
        
        // Singleton fields
        for (ComponentMeta comp : singletons) {
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                getFieldName(comp), "L" + comp.internalName + ";", null, null).visitEnd();
        }
        
        // Lookup arrays
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_types", "[Ljava/lang/Class;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_instances", "[Ljava/lang/Object;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_scopes", "[I", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_protoIdx", "[I", null, null).visitEnd();
        
        generateStaticInit(cw, singletons, prototypes);
        generatePrivateConstructor(cw);
        
        for (ComponentMeta comp : singletons) {
            generateSingletonGetter(cw, comp);
        }
        for (ComponentMeta comp : prototypes) {
            generatePrototypeGetter(cw, comp);
        }
        
        generateCreatePrototype(cw, prototypes);
        generateGetByClass(cw);
        generateGetAllByClass(cw);
        generateContains(cw);
        generateComponentCount(cw);
        generateShutdown(cw, singletons);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateStaticInit(ClassWriter cw, List<ComponentMeta> singletons, List<ComponentMeta> prototypes) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        List<ComponentMeta> sorted = topologicalSort(singletons);
        
        for (ComponentMeta comp : sorted) {
            String fieldName = getFieldName(comp);
            String fieldType = "L" + comp.internalName + ";";
            
            mv.visitTypeInsn(NEW, comp.internalName);
            mv.visitInsn(DUP);
            
            StringBuilder ctorDesc = new StringBuilder("(");
            for (String depType : comp.constructorDeps) {
                String depInternal = depType.replace('.', '/');
                ctorDesc.append("L").append(depInternal).append(";");
                loadDependency(mv, depType);
            }
            ctorDesc.append(")V");
            
            mv.visitMethodInsn(INVOKESPECIAL, comp.internalName, "<init>", ctorDesc.toString(), false);
            mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, fieldName, fieldType);
            
            // Field injections (skip @Value primitive fields - they're handled by factories)
            for (FieldInjectionMeta field : comp.fieldInjections) {
                // Skip primitive types and String (likely @Value injections)
                if (isPrimitiveOrValueType(field.descriptor)) {
                    continue;
                }
                
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                
                if (field.isOptional) {
                    // Optional<T> injection - wrap in Optional.of() or Optional.empty()
                    loadOptionalDependency(mv, field.depType);
                } else {
                    loadDependency(mv, field.depType);
                }
                
                if (!"PUBLIC".equals(field.visibility)) {
                    String setterName = SYNTHETIC_SETTER_PREFIX + field.name;
                    String setterDesc = "(" + field.descriptor + ")V";
                    mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, setterName, setterDesc, false);
                } else {
                    mv.visitFieldInsn(PUTFIELD, comp.internalName, field.name, field.descriptor);
                }
            }
            
            // Method injections
            for (MethodInjectionMeta method : comp.methodInjections) {
                if (method.descriptor == null || method.descriptor.isEmpty()) {
                    continue; // Skip invalid method injections
                }
                // Validate descriptor format
                if (!method.descriptor.startsWith("(") || !method.descriptor.contains(")")) {
                    System.err.println("WARNING: Invalid method descriptor for " + comp.className + "." + method.name + ": " + method.descriptor);
                    continue;
                }
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                for (String depType : method.depTypes) {
                    loadDependency(mv, depType);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, method.name, method.descriptor, false);
            }
            
            // EventBus registration (if has @Subscribe methods)
            if (comp.hasSubscribeMethods) {
                mv.visitMethodInsn(INVOKESTATIC, "com/veld/runtime/event/EventBus", "getInstance", 
                    "()Lcom/veld/runtime/event/EventBus;", false);
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/veld/runtime/event/EventBus", "register", 
                    "(Ljava/lang/Object;)V", false);
            }
            
            // @PostConstruct callback (via reflection for package-private access)
            if (comp.postConstructMethod != null) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                generateReflectiveMethodCall(mv, comp.postConstructMethod);
            }
        }
        
        // Initialize lookup arrays
        List<TypeMapping> mappings = buildTypeMappings();
        int mappingCount = mappings.size();
        
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        
        pushInt(mv, mappingCount);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_scopes", "[I");
        
        pushInt(mv, mappingCount);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_protoIdx", "[I");
        
        Map<String, Integer> protoIdxMap = new HashMap<>();
        for (int i = 0; i < prototypes.size(); i++) {
            protoIdxMap.put(prototypes.get(i).internalName, i);
        }
        
        for (int i = 0; i < mappings.size(); i++) {
            TypeMapping m = mappings.get(i);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
            pushInt(mv, i);
            mv.visitLdcInsn(Type.getObjectType(m.typeInternal));
            mv.visitInsn(AASTORE);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
            pushInt(mv, i);
            if ("SINGLETON".equals(m.component.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(m.component),
                    "L" + m.component.internalName + ";");
            } else {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitInsn(AASTORE);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
            pushInt(mv, i);
            pushInt(mv, "SINGLETON".equals(m.component.scope) ? 0 : 1);
            mv.visitInsn(IASTORE);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
            pushInt(mv, i);
            Integer idx = protoIdxMap.get(m.component.internalName);
            pushInt(mv, idx != null ? idx : -1);
            mv.visitInsn(IASTORE);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates bytecode to invoke a no-arg method via reflection with setAccessible(true).
     * This allows calling package-private @PostConstruct/@PreDestroy methods.
     * Stack: [..., instance] -> [...]
     * 
     * Equivalent to:
     *   Method m = instance.getClass().getDeclaredMethod(methodName);
     *   m.setAccessible(true);
     *   m.invoke(instance);
     */
    private void generateReflectiveMethodCall(MethodVisitor mv, String methodName) {
        // Stack: [instance]
        mv.visitInsn(DUP);  // [instance, instance]
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", 
            "()Ljava/lang/Class;", false);  // [instance, Class]
        mv.visitLdcInsn(methodName);  // [instance, Class, methodName]
        mv.visitInsn(ICONST_0);  // [instance, Class, methodName, 0]
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");  // [instance, Class, methodName, Class[0]]
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
            "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);  // [instance, Method]
        mv.visitInsn(DUP);  // [instance, Method, Method]
        mv.visitInsn(ICONST_1);  // [instance, Method, Method, true]
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible",
            "(Z)V", false);  // [instance, Method]
        mv.visitInsn(SWAP);  // [Method, instance]
        mv.visitInsn(ICONST_0);  // [Method, instance, 0]
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");  // [Method, instance, Object[0]]
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);  // [result]
        mv.visitInsn(POP);  // []
    }
    
    private void loadDependency(MethodVisitor mv, String typeName) {
        String depInternal = typeName.replace('.', '/');
        ComponentMeta depComp = findComponentByType(depInternal);
        
        if (depComp != null) {
            if ("SINGLETON".equals(depComp.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(depComp),
                    "L" + depComp.internalName + ";");
            } else {
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(depComp),
                    "()L" + depComp.internalName + ";", false);
            }
        } else {
            // Unknown dependency - just push null (will be resolved by factory if needed)
            mv.visitInsn(ACONST_NULL);
        }
    }
    
    /**
     * Loads an Optional<T> dependency.
     * If the component exists, returns Optional.of(component), otherwise Optional.empty().
     */
    private void loadOptionalDependency(MethodVisitor mv, String typeName) {
        String depInternal = typeName.replace('.', '/');
        ComponentMeta depComp = findComponentByType(depInternal);
        
        if (depComp != null) {
            // Component exists - wrap in Optional.of()
            if ("SINGLETON".equals(depComp.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(depComp),
                    "L" + depComp.internalName + ";");
            } else {
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(depComp),
                    "()L" + depComp.internalName + ";", false);
            }
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of",
                "(Ljava/lang/Object;)Ljava/util/Optional;", false);
        } else {
            // Component doesn't exist - return Optional.empty()
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "empty",
                "()Ljava/util/Optional;", false);
        }
    }
    
    private List<TypeMapping> buildTypeMappings() {
        List<TypeMapping> mappings = new ArrayList<>();
        for (ComponentMeta comp : components) {
            mappings.add(new TypeMapping(comp.internalName, comp));
            for (String iface : comp.interfaces) {
                mappings.add(new TypeMapping(iface.replace('.', '/'), comp));
            }
        }
        return mappings;
    }
    
    private List<ComponentMeta> topologicalSort(List<ComponentMeta> singletons) {
        Map<String, ComponentMeta> byType = new HashMap<>();
        for (ComponentMeta comp : singletons) {
            byType.put(comp.internalName, comp);
            for (String iface : comp.interfaces) {
                byType.put(iface.replace('.', '/'), comp);
            }
        }
        
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<ComponentMeta> result = new ArrayList<>();
        
        for (ComponentMeta comp : singletons) {
            visit(comp, byType, visited, visiting, result);
        }
        return result;
    }
    
    private void visit(ComponentMeta comp, Map<String, ComponentMeta> byType,
                       Set<String> visited, Set<String> visiting, List<ComponentMeta> result) {
        if (visited.contains(comp.internalName)) return;
        if (visiting.contains(comp.internalName)) return;
        
        visiting.add(comp.internalName);
        
        for (String dep : comp.constructorDeps) {
            ComponentMeta depComp = byType.get(dep.replace('.', '/'));
            if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                visit(depComp, byType, visited, visiting, result);
            }
        }
        
        for (FieldInjectionMeta field : comp.fieldInjections) {
            ComponentMeta depComp = byType.get(field.depType.replace('.', '/'));
            if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                visit(depComp, byType, visited, visiting, result);
            }
        }
        
        for (MethodInjectionMeta method : comp.methodInjections) {
            for (String dep : method.depTypes) {
                ComponentMeta depComp = byType.get(dep.replace('.', '/'));
                if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                    visit(depComp, byType, visited, visiting, result);
                }
            }
        }
        
        visiting.remove(comp.internalName);
        visited.add(comp.internalName);
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
    
    private void generateSingletonGetter(ClassWriter cw, ComponentMeta comp) {
        String methodName = getMethodName(comp);
        String returnType = "L" + comp.internalName + ";";
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            methodName, "()" + returnType, null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(comp), returnType);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generatePrototypeGetter(ClassWriter cw, ComponentMeta comp) {
        String methodName = getMethodName(comp);
        String returnType = "L" + comp.internalName + ";";
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName,
            "()" + returnType, null, null);
        mv.visitCode();
        
        mv.visitTypeInsn(NEW, comp.internalName);
        mv.visitInsn(DUP);
        
        StringBuilder ctorDesc = new StringBuilder("(");
        for (String depType : comp.constructorDeps) {
            String depInternal = depType.replace('.', '/');
            ctorDesc.append("L").append(depInternal).append(";");
            loadDependency(mv, depType);
        }
        ctorDesc.append(")V");
        
        mv.visitMethodInsn(INVOKESPECIAL, comp.internalName, "<init>", ctorDesc.toString(), false);
        mv.visitVarInsn(ASTORE, 0);
        
        for (FieldInjectionMeta field : comp.fieldInjections) {
            // Skip @Value primitive fields
            if (isPrimitiveOrValueType(field.descriptor)) {
                continue;
            }
            
            mv.visitVarInsn(ALOAD, 0);
            if (field.isOptional) {
                loadOptionalDependency(mv, field.depType);
            } else {
                loadDependency(mv, field.depType);
            }
            
            if (!"PUBLIC".equals(field.visibility)) {
                String setterName = SYNTHETIC_SETTER_PREFIX + field.name;
                String setterDesc = "(" + field.descriptor + ")V";
                mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, setterName, setterDesc, false);
            } else {
                mv.visitFieldInsn(PUTFIELD, comp.internalName, field.name, field.descriptor);
            }
        }
        
        for (MethodInjectionMeta method : comp.methodInjections) {
            mv.visitVarInsn(ALOAD, 0);
            for (String depType : method.depTypes) {
                loadDependency(mv, depType);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, method.name, method.descriptor, false);
        }
        
        // EventBus registration (if has @Subscribe methods)
        if (comp.hasSubscribeMethods) {
            mv.visitMethodInsn(INVOKESTATIC, "com/veld/runtime/event/EventBus", "getInstance", 
                "()Lcom/veld/runtime/event/EventBus;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/veld/runtime/event/EventBus", "register", 
                "(Ljava/lang/Object;)V", false);
        }
        
        // @PostConstruct callback (via reflection for package-private access)
        if (comp.postConstructMethod != null) {
            mv.visitVarInsn(ALOAD, 0);
            generateReflectiveMethodCall(mv, comp.postConstructMethod);
        }
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateCreatePrototype(ClassWriter cw, List<ComponentMeta> prototypes) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "_createPrototype",
            "(I)Ljava/lang/Object;", null, null);
        mv.visitCode();
        
        if (prototypes.isEmpty()) {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        } else {
            Label defaultLabel = new Label();
            Label[] labels = new Label[prototypes.size()];
            for (int i = 0; i < labels.length; i++) labels[i] = new Label();
            
            mv.visitVarInsn(ILOAD, 0);
            mv.visitTableSwitchInsn(0, prototypes.size() - 1, defaultLabel, labels);
            
            for (int i = 0; i < prototypes.size(); i++) {
                mv.visitLabel(labels[i]);
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(prototypes.get(i)),
                    "()L" + prototypes.get(i).internalName + ";", false);
                mv.visitInsn(ARETURN);
            }
            
            mv.visitLabel(defaultLabel);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetByClass(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "get",
            "(Ljava/lang/Class;)Ljava/lang/Object;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;)TT;", null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 1);
        
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ALOAD, 0);
        
        Label notEqual = new Label();
        mv.visitJumpInsn(IF_ACMPNE, notEqual);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IALOAD);
        
        Label isProto = new Label();
        mv.visitJumpInsn(IFNE, isProto);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(isProto);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IALOAD);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "_createPrototype", "(I)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(notEqual);
        mv.visitIincInsn(2, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        
        mv.visitLabel(loopEnd);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetAllByClass(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "getAll",
            "(Ljava/lang/Class;)Ljava/util/List;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;)Ljava/util/List<TT;>;", null);
        mv.visitCode();
        
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 1);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 2);
        
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z", false);
        
        Label notMatch = new Label();
        mv.visitJumpInsn(IFEQ, notMatch);
        
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
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
    
    private void generateContains(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "contains",
            "(Ljava/lang/Class;)Z", null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "get", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        
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
    
    private void generateComponentCount(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "componentCount", "()I", null, null);
        mv.visitCode();
        pushInt(mv, components.size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateShutdown(ClassWriter cw, List<ComponentMeta> singletons) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "shutdown", "()V", null, null);
        mv.visitCode();
        
        // Call @PreDestroy on all singletons (in reverse order)
        for (int i = singletons.size() - 1; i >= 0; i--) {
            ComponentMeta comp = singletons.get(i);
            if (comp.preDestroyMethod != null) {
                String fieldName = getFieldName(comp);
                String fieldType = "L" + comp.internalName + ";";
                
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                generateReflectiveMethodCall(mv, comp.preDestroyMethod);
            }
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private ComponentMeta findComponentByType(String internalName) {
        for (ComponentMeta comp : components) {
            if (comp.internalName.equals(internalName)) return comp;
            for (String iface : comp.interfaces) {
                if (iface.replace('.', '/').equals(internalName)) return comp;
            }
        }
        return null;
    }
    
    private String getMethodName(ComponentMeta comp) {
        String simpleName = comp.className;
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot >= 0) simpleName = simpleName.substring(lastDot + 1);
        if (simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(1))) return simpleName;
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
    
    private String getFieldName(ComponentMeta comp) {
        return "_" + getMethodName(comp);
    }
    
    private void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) mv.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) mv.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) mv.visitIntInsn(SIPUSH, value);
        else mv.visitLdcInsn(value);
    }
    
    /**
     * Checks if a descriptor represents a primitive type or String (likely @Value injection).
     * These are handled by the factories, not by Veld.class.
     */
    private boolean isPrimitiveOrValueType(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) return true;
        char first = descriptor.charAt(0);
        // Primitives: Z=boolean, B=byte, C=char, S=short, I=int, J=long, F=float, D=double
        if (first != 'L' && first != '[') return true;
        // String is also typically a @Value injection
        if ("Ljava/lang/String;".equals(descriptor)) return true;
        return false;
    }
    
    private static class TypeMapping {
        final String typeInternal;
        final ComponentMeta component;
        TypeMapping(String typeInternal, ComponentMeta component) {
            this.typeInternal = typeInternal;
            this.component = component;
        }
    }
    
    // === Data classes for parsed metadata ===
    
    public static class ComponentMeta {
        public final String className;
        public final String internalName;
        public final String scope;
        public final boolean lazy;
        public final List<String> constructorDeps;
        public final List<FieldInjectionMeta> fieldInjections;
        public final List<MethodInjectionMeta> methodInjections;
        public final List<String> interfaces;
        public final String postConstructMethod;
        public final String postConstructDescriptor;
        public final String preDestroyMethod;
        public final String preDestroyDescriptor;
        public final boolean hasSubscribeMethods;
        
        public ComponentMeta(String className, String scope, boolean lazy,
                            List<String> constructorDeps, List<FieldInjectionMeta> fieldInjections,
                            List<MethodInjectionMeta> methodInjections, List<String> interfaces,
                            String postConstructMethod, String postConstructDescriptor,
                            String preDestroyMethod, String preDestroyDescriptor,
                            boolean hasSubscribeMethods) {
            this.className = className;
            this.internalName = className.replace('.', '/');
            this.scope = scope;
            this.lazy = lazy;
            this.constructorDeps = constructorDeps;
            this.fieldInjections = fieldInjections;
            this.methodInjections = methodInjections;
            this.interfaces = interfaces;
            this.postConstructMethod = postConstructMethod;
            this.postConstructDescriptor = postConstructDescriptor;
            this.preDestroyMethod = preDestroyMethod;
            this.preDestroyDescriptor = preDestroyDescriptor;
            this.hasSubscribeMethods = hasSubscribeMethods;
        }
        
        public static ComponentMeta parse(String line) {
            String[] parts = line.split("\\|\\|", -1);
            
            String className = parts[0];
            String scope = parts[1];
            boolean lazy = Boolean.parseBoolean(parts[2]);
            
            List<String> ctorDeps = new ArrayList<>();
            if (parts.length > 3 && !parts[3].isEmpty()) {
                ctorDeps.addAll(Arrays.asList(parts[3].split(",")));
            }
            
            List<FieldInjectionMeta> fields = new ArrayList<>();
            if (parts.length > 4 && !parts[4].isEmpty()) {
                for (String f : parts[4].split("@")) {
                    if (f.isEmpty()) continue;
                    String[] fp = f.split("~", 6); // use ~ as delimiter
                    if (fp.length >= 4) {
                        boolean isOptional = fp.length > 4 && Boolean.parseBoolean(fp[4]);
                        boolean isProvider = fp.length > 5 && Boolean.parseBoolean(fp[5]);
                        fields.add(new FieldInjectionMeta(fp[0], fp[1], fp[2], fp[3], isOptional, isProvider));
                    }
                }
            }
            
            List<MethodInjectionMeta> methods = new ArrayList<>();
            if (parts.length > 5 && !parts[5].isEmpty()) {
                for (String m : parts[5].split("@")) {
                    if (m.isEmpty()) continue;
                    String[] mp = m.split("~", 3); // use ~ as delimiter
                    if (mp.length >= 2) {
                        List<String> deps = mp.length > 2 && !mp[2].isEmpty() 
                            ? Arrays.asList(mp[2].split(",")) : new ArrayList<>();
                        methods.add(new MethodInjectionMeta(mp[0], mp[1], deps));
                    }
                }
            }
            
            List<String> ifaces = new ArrayList<>();
            if (parts.length > 6 && !parts[6].isEmpty()) {
                ifaces.addAll(Arrays.asList(parts[6].split(",")));
            }
            
            // Parse postConstruct (index 7)
            String postConstructMethod = null;
            String postConstructDescriptor = null;
            if (parts.length > 7 && !parts[7].isEmpty()) {
                String[] pc = parts[7].split("~", 2);
                if (pc.length >= 2) {
                    postConstructMethod = pc[0];
                    postConstructDescriptor = pc[1];
                }
            }
            
            // Parse preDestroy (index 8)
            String preDestroyMethod = null;
            String preDestroyDescriptor = null;
            if (parts.length > 8 && !parts[8].isEmpty()) {
                String[] pd = parts[8].split("~", 2);
                if (pd.length >= 2) {
                    preDestroyMethod = pd[0];
                    preDestroyDescriptor = pd[1];
                }
            }
            
            // Parse hasSubscribeMethods (index 9)
            boolean hasSubscribeMethods = false;
            if (parts.length > 9 && !parts[9].isEmpty()) {
                hasSubscribeMethods = Boolean.parseBoolean(parts[9]);
            }
            
            return new ComponentMeta(className, scope, lazy, ctorDeps, fields, methods, ifaces,
                postConstructMethod, postConstructDescriptor, preDestroyMethod, preDestroyDescriptor,
                hasSubscribeMethods);
        }
    }
    
    public static class FieldInjectionMeta {
        public final String name;
        public final String depType;      // actualType (the real type, not wrapper)
        public final String descriptor;
        public final String visibility;
        public final boolean isOptional;  // true if Optional<T>
        public final boolean isProvider;  // true if Provider<T>
        
        public FieldInjectionMeta(String name, String depType, String descriptor, String visibility,
                                  boolean isOptional, boolean isProvider) {
            this.name = name;
            this.depType = depType;
            this.descriptor = descriptor;
            this.visibility = visibility;
            this.isOptional = isOptional;
            this.isProvider = isProvider;
        }
    }
    
    public static class MethodInjectionMeta {
        public final String name;
        public final String descriptor;
        public final List<String> depTypes;
        
        public MethodInjectionMeta(String name, String descriptor, List<String> depTypes) {
            this.name = name;
            this.descriptor = descriptor;
            this.depTypes = depTypes;
        }
    }
}
