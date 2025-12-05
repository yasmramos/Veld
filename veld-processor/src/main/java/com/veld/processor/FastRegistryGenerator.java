package com.veld.processor;

import com.veld.runtime.Scope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Generates ultra-fast registry with array-based lookups.
 * 
 * <p>Generated class structure:
 * <pre>
 * public final class VeldFastRegistry implements FastRegistry {
 *     // Pre-computed index mappings (IdentityHashMap for Class, HashMap for String)
 *     private static final IdentityHashMap&lt;Class&lt;?&gt;, Integer&gt; TYPE_INDICES;
 *     private static final HashMap&lt;String, Integer&gt; NAME_INDICES;
 *     
 *     // Component metadata arrays
 *     private static final Scope[] SCOPES;
 *     private static final boolean[] LAZY_FLAGS;
 *     
 *     // Supertype mappings for getAll()
 *     private static final int[][] SUPERTYPE_INDICES;
 *     
 *     static {
 *         TYPE_INDICES = new IdentityHashMap&lt;&gt;();
 *         TYPE_INDICES.put(MyService.class, 0);
 *         TYPE_INDICES.put(Logger.class, 1);
 *         // ... pre-computed at compile time
 *         
 *         SCOPES = new Scope[] { Scope.SINGLETON, Scope.SINGLETON, ... };
 *         LAZY_FLAGS = new boolean[] { false, true, ... };
 *     }
 *     
 *     public int getIndex(Class&lt;?&gt; type) {
 *         Integer idx = TYPE_INDICES.get(type);
 *         return idx != null ? idx : -1;
 *     }
 *     
 *     // Ultra-fast switch-based create() with inlined factory calls
 *     public Object create(int index, FastContainer container) {
 *         switch (index) {
 *             case 0: return new MyService(container.get(Logger.class));
 *             case 1: return new Logger();
 *             default: throw new VeldException("Invalid index");
 *         }
 *     }
 * }
 * </pre>
 */
public final class FastRegistryGenerator {
    
    private static final String REGISTRY_NAME = "com/veld/generated/VeldFastRegistry";
    private static final String FAST_REGISTRY = "com/veld/runtime/fast/FastRegistry";
    private static final String FAST_CONTAINER = "com/veld/runtime/fast/FastContainer";
    private static final String COMPONENT_FACTORY = "com/veld/runtime/ComponentFactory";
    private static final String VELD_EXCEPTION = "com/veld/runtime/VeldException";
    private static final String SCOPE = "com/veld/runtime/Scope";
    private static final String OBJECT = "java/lang/Object";
    private static final String CLASS = "java/lang/Class";
    private static final String STRING = "java/lang/String";
    private static final String INTEGER = "java/lang/Integer";
    private static final String IDENTITY_HASHMAP = "java/util/IdentityHashMap";
    private static final String HASHMAP = "java/util/HashMap";
    
    private final List<ComponentInfo> components;
    private final Map<String, Integer> componentIndices = new LinkedHashMap<>();
    private final Map<String, List<Integer>> supertypeIndices = new HashMap<>();
    
    public FastRegistryGenerator(List<ComponentInfo> components) {
        this.components = components;
        buildIndices();
    }
    
    private void buildIndices() {
        int index = 0;
        for (ComponentInfo comp : components) {
            componentIndices.put(comp.getClassName(), index);
            
            // Map component type
            addSupertypeIndex(comp.getClassName(), index);
            
            // Map all implemented interfaces
            for (String iface : comp.getImplementedInterfaces()) {
                addSupertypeIndex(iface, index);
            }
            
            index++;
        }
    }
    
    private void addSupertypeIndex(String type, int index) {
        supertypeIndices.computeIfAbsent(type, k -> new ArrayList<>()).add(index);
    }
    
    public String getRegistryClassName() {
        return "com.veld.generated.VeldFastRegistry";
    }
    
    public String getRegistryInternalName() {
        return REGISTRY_NAME;
    }
    
    /**
     * Generates the complete bytecode for the fast registry class.
     */
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // Class declaration
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                REGISTRY_NAME,
                null,
                OBJECT,
                new String[]{FAST_REGISTRY});
        
        // Static fields for index mappings
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "TYPE_INDICES",
                "L" + IDENTITY_HASHMAP + ";", null, null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "NAME_INDICES",
                "L" + HASHMAP + ";", null, null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "SCOPES",
                "[L" + SCOPE + ";", null, null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "LAZY_FLAGS",
                "[Z", null, null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "SUPERTYPE_INDICES",
                "L" + HASHMAP + ";", null, null).visitEnd();
        
        // Component count constant
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "COMPONENT_COUNT",
                "I", null, components.size()).visitEnd();
        
        // Static initializer
        generateStaticInitializer(cw);
        
        // Constructor
        generateConstructor(cw);
        
        // Interface methods
        generateGetIndexByType(cw);
        generateGetIndexByName(cw);
        generateGetFactory(cw);
        generateGetScope(cw);
        generateGetComponentCount(cw);
        generateGetIndicesForType(cw);
        generateIsLazy(cw);
        generateCreate(cw);
        generateInvokePostConstruct(cw);
        generateInvokePreDestroy(cw);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateStaticInitializer(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        // TYPE_INDICES = new IdentityHashMap<>()
        mv.visitTypeInsn(NEW, IDENTITY_HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, IDENTITY_HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
        
        // Populate TYPE_INDICES
        int index = 0;
        for (ComponentInfo comp : components) {
            // TYPE_INDICES.put(Component.class, index)
            mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
            mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(comp.getInternalName()));
            pushInteger(mv, index);
            mv.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", "(I)L" + INTEGER + ";", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, IDENTITY_HASHMAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
            mv.visitInsn(POP);
            
            // Also add all implemented interfaces
            for (String iface : comp.getImplementedInterfacesInternal()) {
                mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
                mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(iface));
                pushInteger(mv, index);
                mv.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", "(I)L" + INTEGER + ";", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, IDENTITY_HASHMAP, "put",
                        "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
                mv.visitInsn(POP);
            }
            
            index++;
        }
        
        // NAME_INDICES = new HashMap<>()
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "NAME_INDICES", "L" + HASHMAP + ";");
        
        // Populate NAME_INDICES
        index = 0;
        for (ComponentInfo comp : components) {
            mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "NAME_INDICES", "L" + HASHMAP + ";");
            mv.visitLdcInsn(comp.getComponentName());
            pushInteger(mv, index);
            mv.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", "(I)L" + INTEGER + ";", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, HASHMAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
            mv.visitInsn(POP);
            index++;
        }
        
        // SCOPES = new Scope[count]
        pushInteger(mv, components.size());
        mv.visitTypeInsn(ANEWARRAY, SCOPE);
        index = 0;
        for (ComponentInfo comp : components) {
            mv.visitInsn(DUP);
            pushInteger(mv, index);
            String scopeName = comp.getScope() == Scope.SINGLETON ? "SINGLETON" : "PROTOTYPE";
            mv.visitFieldInsn(GETSTATIC, SCOPE, scopeName, "L" + SCOPE + ";");
            mv.visitInsn(AASTORE);
            index++;
        }
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "SCOPES", "[L" + SCOPE + ";");
        
        // LAZY_FLAGS = new boolean[count]
        pushInteger(mv, components.size());
        mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
        index = 0;
        for (ComponentInfo comp : components) {
            if (comp.isLazy()) {
                mv.visitInsn(DUP);
                pushInteger(mv, index);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(BASTORE);
            }
            index++;
        }
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "LAZY_FLAGS", "[Z");
        
        // SUPERTYPE_INDICES = new HashMap<>()
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "SUPERTYPE_INDICES", "L" + HASHMAP + ";");
        
        // Populate SUPERTYPE_INDICES with int[] arrays
        for (Map.Entry<String, List<Integer>> entry : supertypeIndices.entrySet()) {
            String typeName = entry.getKey();
            List<Integer> indices = entry.getValue();
            
            mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "SUPERTYPE_INDICES", "L" + HASHMAP + ";");
            mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(typeName.replace('.', '/')));
            
            // Create int[] array
            pushInteger(mv, indices.size());
            mv.visitIntInsn(NEWARRAY, T_INT);
            for (int i = 0; i < indices.size(); i++) {
                mv.visitInsn(DUP);
                pushInteger(mv, i);
                pushInteger(mv, indices.get(i));
                mv.visitInsn(IASTORE);
            }
            
            mv.visitMethodInsn(INVOKEVIRTUAL, HASHMAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
            mv.visitInsn(POP);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetIndexByType(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getIndex",
                "(L" + CLASS + ";)I", null, null);
        mv.visitCode();
        
        // Integer idx = TYPE_INDICES.get(type);
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, IDENTITY_HASHMAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", false);
        mv.visitTypeInsn(CHECKCAST, INTEGER);
        mv.visitVarInsn(ASTORE, 2);
        
        // return idx != null ? idx.intValue() : -1;
        mv.visitVarInsn(ALOAD, 2);
        Label nullLabel = new Label();
        mv.visitJumpInsn(IFNULL, nullLabel);
        
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, INTEGER, "intValue", "()I", false);
        mv.visitInsn(IRETURN);
        
        mv.visitLabel(nullLabel);
        mv.visitFrame(F_APPEND, 1, new Object[]{INTEGER}, 0, null);
        mv.visitInsn(ICONST_M1);
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetIndexByName(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getIndex",
                "(L" + STRING + ";)I", null, null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "NAME_INDICES", "L" + HASHMAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, HASHMAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", false);
        mv.visitTypeInsn(CHECKCAST, INTEGER);
        mv.visitVarInsn(ASTORE, 2);
        
        mv.visitVarInsn(ALOAD, 2);
        Label nullLabel = new Label();
        mv.visitJumpInsn(IFNULL, nullLabel);
        
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, INTEGER, "intValue", "()I", false);
        mv.visitInsn(IRETURN);
        
        mv.visitLabel(nullLabel);
        mv.visitFrame(F_APPEND, 1, new Object[]{INTEGER}, 0, null);
        mv.visitInsn(ICONST_M1);
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetFactory(ClassWriter cw) {
        // Returns null - we don't use factories in fast mode
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getFactory",
                "(I)L" + COMPONENT_FACTORY + ";", null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetScope(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getScope",
                "(I)L" + SCOPE + ";", null, null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "SCOPES", "[L" + SCOPE + ";");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(AALOAD);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetComponentCount(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getComponentCount", "()I", null, null);
        mv.visitCode();
        pushInteger(mv, components.size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetIndicesForType(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getIndicesForType",
                "(L" + CLASS + ";)[I", null, null);
        mv.visitCode();
        
        // int[] indices = SUPERTYPE_INDICES.get(type);
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "SUPERTYPE_INDICES", "L" + HASHMAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, HASHMAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", false);
        mv.visitTypeInsn(CHECKCAST, "[I");
        mv.visitVarInsn(ASTORE, 2);
        
        // return indices != null ? indices : new int[0];
        mv.visitVarInsn(ALOAD, 2);
        Label nullLabel = new Label();
        mv.visitJumpInsn(IFNULL, nullLabel);
        
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(nullLabel);
        mv.visitFrame(F_APPEND, 1, new Object[]{"[I"}, 0, null);
        mv.visitInsn(ICONST_0);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateIsLazy(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isLazy", "(I)Z", null, null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "LAZY_FLAGS", "[Z");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(BALOAD);
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates the ultra-fast create() method using switch/tableswitch.
     * Each case directly instantiates the component with inlined constructor calls.
     */
    private void generateCreate(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "create",
                "(IL" + FAST_CONTAINER + ";)L" + OBJECT + ";",
                "<T:L" + OBJECT + ";>(IL" + FAST_CONTAINER + ";)TT;", null);
        mv.visitCode();
        
        if (components.isEmpty()) {
            // throw new VeldException("No components registered");
            mv.visitTypeInsn(NEW, VELD_EXCEPTION);
            mv.visitInsn(DUP);
            mv.visitLdcInsn("No components registered");
            mv.visitMethodInsn(INVOKESPECIAL, VELD_EXCEPTION, "<init>",
                    "(L" + STRING + ";)V", false);
            mv.visitInsn(ATHROW);
        } else {
            // Generate tableswitch for O(1) dispatch
            Label[] caseLabels = new Label[components.size()];
            Label defaultLabel = new Label();
            
            for (int i = 0; i < components.size(); i++) {
                caseLabels[i] = new Label();
            }
            
            mv.visitVarInsn(ILOAD, 1); // load index
            mv.visitTableSwitchInsn(0, components.size() - 1, defaultLabel, caseLabels);
            
            // Generate each case
            for (int i = 0; i < components.size(); i++) {
                mv.visitLabel(caseLabels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);
                
                ComponentInfo comp = components.get(i);
                generateComponentCreation(mv, comp);
                mv.visitInsn(ARETURN);
            }
            
            // Default case - throw exception
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            mv.visitTypeInsn(NEW, VELD_EXCEPTION);
            mv.visitInsn(DUP);
            mv.visitLdcInsn("Invalid component index");
            mv.visitMethodInsn(INVOKESPECIAL, VELD_EXCEPTION, "<init>",
                    "(L" + STRING + ";)V", false);
            mv.visitInsn(ATHROW);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates inline component instantiation code.
     * No factory indirection - direct constructor call with resolved dependencies.
     */
    private void generateComponentCreation(MethodVisitor mv, ComponentInfo comp) {
        String componentInternal = comp.getInternalName();
        InjectionPoint constructor = comp.getConstructorInjection();
        
        // new Component(...)
        mv.visitTypeInsn(NEW, componentInternal);
        mv.visitInsn(DUP);
        
        if (constructor == null || constructor.getDependencies().isEmpty()) {
            // No-arg constructor
            mv.visitMethodInsn(INVOKESPECIAL, componentInternal, "<init>", "()V", false);
        } else {
            // Load each dependency from container
            for (InjectionPoint.Dependency dep : constructor.getDependencies()) {
                loadDependencyFromFastContainer(mv, dep);
            }
            mv.visitMethodInsn(INVOKESPECIAL, componentInternal, "<init>",
                    constructor.getDescriptor(), false);
        }
        
        // Field injections
        if (!comp.getFieldInjections().isEmpty()) {
            mv.visitVarInsn(ASTORE, 3); // Store instance temporarily
            
            for (InjectionPoint field : comp.getFieldInjections()) {
                mv.visitVarInsn(ALOAD, 3);
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                loadDependencyFromFastContainer(mv, dep);
                
                if (field.requiresSyntheticSetter()) {
                    String setterName = "__di_set_" + field.getName();
                    String setterDesc = "(" + field.getDescriptor() + ")V";
                    mv.visitMethodInsn(INVOKEVIRTUAL, componentInternal, setterName, setterDesc, false);
                } else {
                    mv.visitFieldInsn(PUTFIELD, componentInternal, field.getName(), field.getDescriptor());
                }
            }
            
            mv.visitVarInsn(ALOAD, 3);
        }
        
        // Method injections
        for (InjectionPoint method : comp.getMethodInjections()) {
            mv.visitInsn(DUP);
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                loadDependencyFromFastContainer(mv, dep);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, componentInternal,
                    method.getName(), method.getDescriptor(), false);
        }
    }
    
    private void loadDependencyFromFastContainer(MethodVisitor mv, InjectionPoint.Dependency dep) {
        String depInternal = dep.getTypeName().replace('.', '/');
        
        // container.get(DependencyType.class)
        mv.visitVarInsn(ALOAD, 2); // Load container (parameter 2)
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(depInternal));
        mv.visitMethodInsn(INVOKEVIRTUAL, FAST_CONTAINER, "get",
                "(L" + CLASS + ";)L" + OBJECT + ";", false);
        mv.visitTypeInsn(CHECKCAST, depInternal);
    }
    
    private void generateInvokePostConstruct(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invokePostConstruct",
                "(IL" + OBJECT + ";)V", null, null);
        mv.visitCode();
        
        if (components.isEmpty()) {
            mv.visitInsn(RETURN);
        } else {
            Label[] caseLabels = new Label[components.size()];
            Label defaultLabel = new Label();
            Label endLabel = new Label();
            
            for (int i = 0; i < components.size(); i++) {
                caseLabels[i] = new Label();
            }
            
            mv.visitVarInsn(ILOAD, 1);
            mv.visitTableSwitchInsn(0, components.size() - 1, defaultLabel, caseLabels);
            
            for (int i = 0; i < components.size(); i++) {
                mv.visitLabel(caseLabels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);
                
                ComponentInfo comp = components.get(i);
                if (comp.hasPostConstruct()) {
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, comp.getInternalName());
                    mv.visitMethodInsn(INVOKEVIRTUAL, comp.getInternalName(),
                            comp.getPostConstructMethod(),
                            comp.getPostConstructDescriptor(), false);
                }
                mv.visitJumpInsn(GOTO, endLabel);
            }
            
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            
            mv.visitLabel(endLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateInvokePreDestroy(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invokePreDestroy",
                "(IL" + OBJECT + ";)V", null, null);
        mv.visitCode();
        
        if (components.isEmpty()) {
            mv.visitInsn(RETURN);
        } else {
            Label[] caseLabels = new Label[components.size()];
            Label defaultLabel = new Label();
            Label endLabel = new Label();
            
            for (int i = 0; i < components.size(); i++) {
                caseLabels[i] = new Label();
            }
            
            mv.visitVarInsn(ILOAD, 1);
            mv.visitTableSwitchInsn(0, components.size() - 1, defaultLabel, caseLabels);
            
            for (int i = 0; i < components.size(); i++) {
                mv.visitLabel(caseLabels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);
                
                ComponentInfo comp = components.get(i);
                if (comp.hasPreDestroy()) {
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, comp.getInternalName());
                    mv.visitMethodInsn(INVOKEVIRTUAL, comp.getInternalName(),
                            comp.getPreDestroyMethod(),
                            comp.getPreDestroyDescriptor(), false);
                }
                mv.visitJumpInsn(GOTO, endLabel);
            }
            
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            
            mv.visitLabel(endLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            mv.visitInsn(RETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void pushInteger(MethodVisitor mv, int value) {
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
}
