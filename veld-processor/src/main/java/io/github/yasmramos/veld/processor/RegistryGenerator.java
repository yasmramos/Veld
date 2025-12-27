package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.Scope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Generates ultra-optimized bytecode for VeldRegistry.
 * 
 * <p>Optimizations:
 * <ul>
 *   <li>IdentityHashMap for O(1) Class-to-index lookups
 *   <li>Pre-computed arrays for scopes and lazy flags
 *   <li>Switch-based create() for fast instantiation
 *   <li>Numeric indices for all components
 * </ul>
 * 
 * <p>Generated structure:
 * <pre>
 * public final class VeldRegistry implements ComponentRegistry {
 *     // Static index maps for ultra-fast lookups
 *     private static final IdentityHashMap&lt;Class, Integer&gt; TYPE_INDICES;
 *     private static final HashMap&lt;String, Integer&gt; NAME_INDICES;
 *     private static final Scope[] SCOPES;
 *     private static final boolean[] LAZY_FLAGS;
 *     
 *     // Component factories
 *     private final ComponentFactory&lt;?&gt;[] factories;
 *     
 *     static {
 *         TYPE_INDICES = new IdentityHashMap&lt;&gt;();
 *         TYPE_INDICES.put(MyService.class, 0);
 *         // ...
 *     }
 *     
 *     public int getIndex(Class&lt;?&gt; type) {
 *         Integer idx = TYPE_INDICES.get(type);
 *         return idx != null ? idx : -1;
 *     }
 *     
 *     public Object create(int index, Veld container) {
 *         switch (index) {
 *             case 0: return factories[0].create(container);
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 */
public final class RegistryGenerator {
    
    private static final String REGISTRY_NAME = "io/github/yasmramos/veld/generated/VeldRegistry";
    private static final String COMPONENT_REGISTRY = "io/github/yasmramos/veld/runtime/ComponentRegistry";
    private static final String COMPONENT_FACTORY = "io/github/yasmramos/veld/runtime/ComponentFactory";
    private static final String VELD_CLASS = "io/github/yasmramos/veld/Veld";
    private static final String VELD_EXCEPTION = "io/github/yasmramos/veld/VeldException";
    private static final String SCOPE = "io/github/yasmramos/veld/runtime/Scope";
    private static final String OBJECT = "java/lang/Object";
    private static final String CLASS = "java/lang/Class";
    private static final String STRING = "java/lang/String";
    private static final String INTEGER = "java/lang/Integer";
    private static final String MAP = "java/util/Map";
    private static final String HASHMAP = "java/util/HashMap";
    private static final String IDENTITY_HASHMAP = "java/util/IdentityHashMap";
    private static final String LIST = "java/util/List";
    private static final String ARRAYLIST = "java/util/ArrayList";
    
    private final List<ComponentInfo> components;
    private final Map<String, List<Integer>> supertypeIndices = new HashMap<>();
    
    public RegistryGenerator(List<ComponentInfo> components) {
        this.components = components;
        buildSupertypeIndices();
    }
    
    private void buildSupertypeIndices() {
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            addSupertypeIndex(comp.getClassName(), i);
            for (String iface : comp.getImplementedInterfaces()) {
                addSupertypeIndex(iface, i);
            }
        }
    }
    
    private void addSupertypeIndex(String type, int index) {
        supertypeIndices.computeIfAbsent(type, k -> new ArrayList<>()).add(index);
    }
    
    public String getRegistryClassName() {
        return "io.github.yasmramos.veld.generated.VeldRegistry";
    }
    
    public String getRegistryInternalName() {
        return REGISTRY_NAME;
    }
    
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // Class declaration
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                REGISTRY_NAME,
                null,
                OBJECT,
                new String[]{COMPONENT_REGISTRY});
        
        // Static fields for ultra-fast lookups
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
        
        // Instance field for factories array
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factories",
                "[L" + COMPONENT_FACTORY + ";", null, null).visitEnd();
        
        // Legacy fields for compatibility
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factoriesByType",
                "L" + MAP + ";", null, null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factoriesByName",
                "L" + MAP + ";", null, null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factoriesBySupertype",
                "L" + MAP + ";", null, null).visitEnd();
        
        // Static initializer
        generateStaticInitializer(cw);
        
        // Constructor
        generateConstructor(cw);
        
        // Ultra-fast methods
        generateGetIndexByType(cw);
        generateGetIndexByName(cw);
        generateGetComponentCount(cw);
        generateGetScope(cw);
        generateIsLazy(cw);
        generateCreate(cw);
        generateGetIndicesForType(cw);
        generateInvokePostConstruct(cw);
        generateInvokePreDestroy(cw);
        
        // Legacy interface methods
        generateGetAllFactories(cw);
        generateGetFactoryByType(cw);
        generateGetFactoryByName(cw);
        generateGetFactoriesForType(cw);
        
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
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            
            // TYPE_INDICES.put(Component.class, index)
            mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
            mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(comp.getInternalName()));
            pushInteger(mv, i);
            mv.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", "(I)L" + INTEGER + ";", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, IDENTITY_HASHMAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
            mv.visitInsn(POP);
            
            // Also add all implemented interfaces
            for (String iface : comp.getImplementedInterfacesInternal()) {
                mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
                mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(iface));
                pushInteger(mv, i);
                mv.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", "(I)L" + INTEGER + ";", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, IDENTITY_HASHMAP, "put",
                        "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
                mv.visitInsn(POP);
            }
        }
        
        // NAME_INDICES = new HashMap<>()
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "NAME_INDICES", "L" + HASHMAP + ";");
        
        // Populate NAME_INDICES
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "NAME_INDICES", "L" + HASHMAP + ";");
            mv.visitLdcInsn(comp.getComponentName());
            pushInteger(mv, i);
            mv.visitMethodInsn(INVOKESTATIC, INTEGER, "valueOf", "(I)L" + INTEGER + ";", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, HASHMAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", false);
            mv.visitInsn(POP);
        }
        
        // SCOPES = new Scope[count]
        pushInteger(mv, components.size());
        mv.visitTypeInsn(ANEWARRAY, SCOPE);
        for (int i = 0; i < components.size(); i++) {
            mv.visitInsn(DUP);
            pushInteger(mv, i);
            String scopeName = components.get(i).getScope() == Scope.SINGLETON ? "SINGLETON" : "PROTOTYPE";
            mv.visitFieldInsn(GETSTATIC, SCOPE, scopeName, "L" + SCOPE + ";");
            mv.visitInsn(AASTORE);
        }
        mv.visitFieldInsn(PUTSTATIC, REGISTRY_NAME, "SCOPES", "[L" + SCOPE + ";");
        
        // LAZY_FLAGS = new boolean[count]
        pushInteger(mv, components.size());
        mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).isLazy()) {
                mv.visitInsn(DUP);
                pushInteger(mv, i);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(BASTORE);
            }
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
        
        // super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        
        // this.factories = new ComponentFactory[count]
        mv.visitVarInsn(ALOAD, 0);
        pushInteger(mv, components.size());
        mv.visitTypeInsn(ANEWARRAY, COMPONENT_FACTORY);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factories", "[L" + COMPONENT_FACTORY + ";");
        
        // Legacy maps
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factoriesByType", "L" + MAP + ";");
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factoriesByName", "L" + MAP + ";");
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        
        // Register all factories
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            String factoryInternal = comp.getFactoryInternalName();
            String componentInternal = comp.getInternalName();
            
            // Create factory
            mv.visitTypeInsn(NEW, factoryInternal);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, factoryInternal, "<init>", "()V", false);
            int localVar = i + 1;
            mv.visitVarInsn(ASTORE, localVar);
            
            // factories[i] = factory
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factories", "[L" + COMPONENT_FACTORY + ";");
            pushInteger(mv, i);
            mv.visitVarInsn(ALOAD, localVar);
            mv.visitInsn(AASTORE);
            
            // Legacy: factoriesByType.put(Component.class, factory)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByType", "L" + MAP + ";");
            mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(componentInternal));
            mv.visitVarInsn(ALOAD, localVar);
            mv.visitMethodInsn(INVOKEINTERFACE, MAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", true);
            mv.visitInsn(POP);
            
            // Legacy: factoriesByName.put("name", factory)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByName", "L" + MAP + ";");
            mv.visitLdcInsn(comp.getComponentName());
            mv.visitVarInsn(ALOAD, localVar);
            mv.visitMethodInsn(INVOKEINTERFACE, MAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", true);
            mv.visitInsn(POP);
            
            // Legacy: register by interfaces
            for (String iface : comp.getImplementedInterfacesInternal()) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByType", "L" + MAP + ";");
                mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(iface));
                mv.visitVarInsn(ALOAD, localVar);
                mv.visitMethodInsn(INVOKEINTERFACE, MAP, "put",
                        "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", true);
                mv.visitInsn(POP);
            }
            
            // Legacy: register in factoriesBySupertype
            registerInSupertypeMap(mv, localVar, componentInternal);
            for (String iface : comp.getImplementedInterfacesInternal()) {
                registerInSupertypeMap(mv, localVar, iface);
            }
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void registerInSupertypeMap(MethodVisitor mv, int factoryVar, String componentInternal) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(componentInternal));
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(componentInternal));
        mv.visitMethodInsn(INVOKEINTERFACE, MAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", true);
        
        Label hasListLabel = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, hasListLabel);
        
        mv.visitInsn(POP);
        mv.visitTypeInsn(NEW, ARRAYLIST);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST, "<init>", "()V", false);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        mv.visitInsn(SWAP);
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(componentInternal));
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEINTERFACE, MAP, "put",
                "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", true);
        mv.visitInsn(POP);
        Label addLabel = new Label();
        mv.visitJumpInsn(GOTO, addLabel);
        
        mv.visitLabel(hasListLabel);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{OBJECT});
        
        mv.visitLabel(addLabel);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{LIST});
        
        mv.visitTypeInsn(CHECKCAST, LIST);
        mv.visitVarInsn(ALOAD, factoryVar);
        mv.visitMethodInsn(INVOKEINTERFACE, LIST, "add", "(L" + OBJECT + ";)Z", true);
        mv.visitInsn(POP);
    }
    
    private void generateGetIndexByType(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getIndex",
                "(L" + CLASS + ";)I", null, null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "TYPE_INDICES", "L" + IDENTITY_HASHMAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, IDENTITY_HASHMAP, "get",
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
    
    private void generateGetComponentCount(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getComponentCount", "()I", null, null);
        mv.visitCode();
        pushInteger(mv, components.size());
        mv.visitInsn(IRETURN);
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
    
    private void generateCreate(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "create",
                "(I)L" + OBJECT + ";",
                "<T:L" + OBJECT + ";>(I)TT;", null);
        mv.visitCode();
        
        if (components.isEmpty()) {
            mv.visitTypeInsn(NEW, VELD_EXCEPTION);
            mv.visitInsn(DUP);
            mv.visitLdcInsn("No components registered");
            mv.visitMethodInsn(INVOKESPECIAL, VELD_EXCEPTION, "<init>",
                    "(L" + STRING + ";)V", false);
            mv.visitInsn(ATHROW);
        } else {
            // Use factories[index].create()
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factories", "[L" + COMPONENT_FACTORY + ";");
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(AALOAD);
            mv.visitMethodInsn(INVOKEINTERFACE, COMPONENT_FACTORY, "create",
                    "()L" + OBJECT + ";", true);
            mv.visitInsn(ARETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetIndicesForType(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getIndicesForType",
                "(L" + CLASS + ";)[I", null, null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, REGISTRY_NAME, "SUPERTYPE_INDICES", "L" + HASHMAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, HASHMAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", false);
        mv.visitTypeInsn(CHECKCAST, "[I");
        mv.visitVarInsn(ASTORE, 2);
        
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
    
    private void generateInvokePostConstruct(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invokePostConstruct",
                "(IL" + OBJECT + ";)V", null, null);
        mv.visitCode();
        
        if (!components.isEmpty()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factories", "[L" + COMPONENT_FACTORY + ";");
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(AALOAD);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEINTERFACE, COMPONENT_FACTORY, "invokePostConstruct",
                    "(L" + OBJECT + ";)V", true);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateInvokePreDestroy(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invokePreDestroy",
                "(IL" + OBJECT + ";)V", null, null);
        mv.visitCode();
        
        if (!components.isEmpty()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factories", "[L" + COMPONENT_FACTORY + ";");
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(AALOAD);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEINTERFACE, COMPONENT_FACTORY, "invokePreDestroy",
                    "(L" + OBJECT + ";)V", true);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetAllFactories(ClassWriter cw) {
        String signature = "()L" + LIST + "<L" + COMPONENT_FACTORY + "<*>;>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getAllFactories",
                "()L" + LIST + ";", signature, null);
        mv.visitCode();
        
        mv.visitTypeInsn(NEW, ARRAYLIST);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factories", "[L" + COMPONENT_FACTORY + ";");
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList",
                "([L" + OBJECT + ";)L" + LIST + ";", false);
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST, "<init>", "(Ljava/util/Collection;)V", false);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetFactoryByType(ClassWriter cw) {
        String signature = "<T:L" + OBJECT + ";>(L" + CLASS + "<TT;>;)L" + COMPONENT_FACTORY + "<TT;>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getFactory",
                "(L" + CLASS + ";)L" + COMPONENT_FACTORY + ";", signature, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByType", "L" + MAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, MAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", true);
        mv.visitTypeInsn(CHECKCAST, COMPONENT_FACTORY);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetFactoryByName(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getFactory",
                "(L" + STRING + ";)L" + COMPONENT_FACTORY + ";",
                "(L" + STRING + ";)L" + COMPONENT_FACTORY + "<*>;", null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByName", "L" + MAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, MAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", true);
        mv.visitTypeInsn(CHECKCAST, COMPONENT_FACTORY);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetFactoriesForType(ClassWriter cw) {
        String signature = "<T:L" + OBJECT + ";>(L" + CLASS + "<TT;>;)L" + LIST + 
                "<L" + COMPONENT_FACTORY + "<+TT;>;>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getFactoriesForType",
                "(L" + CLASS + ";)L" + LIST + ";", signature, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, MAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", true);
        mv.visitVarInsn(ASTORE, 2);
        
        mv.visitVarInsn(ALOAD, 2);
        Label notNullLabel = new Label();
        mv.visitJumpInsn(IFNONNULL, notNullLabel);
        
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptyList",
                "()L" + LIST + ";", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(notNullLabel);
        mv.visitFrame(F_APPEND, 1, new Object[]{OBJECT}, 0, null);
        
        mv.visitTypeInsn(NEW, ARRAYLIST);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, "java/util/Collection");
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST, "<init>", "(Ljava/util/Collection;)V", false);
        mv.visitInsn(ARETURN);
        
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
