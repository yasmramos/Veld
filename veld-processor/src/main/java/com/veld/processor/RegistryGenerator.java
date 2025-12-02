package com.veld.processor;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Generates bytecode for the VeldRegistry class that implements ComponentRegistry.
 * NO REFLECTION - pure bytecode generation.
 * 
 * Generated class structure:
 * <pre>
 * public final class VeldRegistry implements ComponentRegistry {
 *     private final Map<Class<?>, ComponentFactory<?>> factoriesByType = new HashMap<>();
 *     private final Map<String, ComponentFactory<?>> factoriesByName = new HashMap<>();
 *     private final List<ComponentFactory<?>> allFactories = new ArrayList<>();
 *     
 *     public VeldRegistry() {
 *         // Register all factories
 *         MyService$$VeldFactory f1 = new MyService$$VeldFactory(this);
 *         factoriesByType.put(MyService.class, f1);
 *         factoriesByName.put("myService", f1);
 *         allFactories.add(f1);
 *         // ... more registrations
 *     }
 *     
 *     public List<ComponentFactory<?>> getAllFactories() { ... }
 *     public <T> ComponentFactory<T> getFactory(Class<T> type) { ... }
 *     public ComponentFactory<?> getFactory(String name) { ... }
 *     public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) { ... }
 * }
 * </pre>
 */
public final class RegistryGenerator {
    
    private static final String REGISTRY_NAME = "com/veld/generated/VeldRegistry";
    private static final String COMPONENT_REGISTRY = "com/veld/runtime/ComponentRegistry";
    private static final String COMPONENT_FACTORY = "com/veld/runtime/ComponentFactory";
    private static final String OBJECT = "java/lang/Object";
    private static final String CLASS = "java/lang/Class";
    private static final String STRING = "java/lang/String";
    private static final String MAP = "java/util/Map";
    private static final String HASHMAP = "java/util/HashMap";
    private static final String LIST = "java/util/List";
    private static final String ARRAYLIST = "java/util/ArrayList";
    
    private final List<ComponentInfo> components;
    
    public RegistryGenerator(List<ComponentInfo> components) {
        this.components = components;
    }
    
    public String getRegistryClassName() {
        return "com.veld.generated.VeldRegistry";
    }
    
    public String getRegistryInternalName() {
        return REGISTRY_NAME;
    }
    
    /**
     * Generates the complete bytecode for the registry class.
     * @return The bytecode as a byte array
     */
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // Class declaration
        cw.visit(V11, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                REGISTRY_NAME,
                null,
                OBJECT,
                new String[]{COMPONENT_REGISTRY});
        
        // Fields
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factoriesByType",
                "L" + MAP + ";",
                "L" + MAP + "<L" + CLASS + "<*>;L" + COMPONENT_FACTORY + "<*>;>;",
                null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factoriesByName",
                "L" + MAP + ";",
                "L" + MAP + "<L" + STRING + ";L" + COMPONENT_FACTORY + "<*>;>;",
                null).visitEnd();
        
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "allFactories",
                "L" + LIST + ";",
                "L" + LIST + "<L" + COMPONENT_FACTORY + "<*>;>;",
                null).visitEnd();
        
        // For supertype lookups - maps interface/superclass to list of factories
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "factoriesBySupertype",
                "L" + MAP + ";",
                "L" + MAP + "<L" + CLASS + "<*>;L" + LIST + "<L" + COMPONENT_FACTORY + "<*>;>;>;",
                null).visitEnd();
        
        // Constructor
        generateConstructor(cw);
        
        // Interface methods - matching ComponentRegistry interface exactly
        generateGetAllFactories(cw);
        generateGetFactoryByType(cw);
        generateGetFactoryByName(cw);
        generateGetFactoriesForType(cw);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        
        // super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        
        // this.factoriesByType = new HashMap<>()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factoriesByType", "L" + MAP + ";");
        
        // this.factoriesByName = new HashMap<>()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factoriesByName", "L" + MAP + ";");
        
        // this.allFactories = new ArrayList<>()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, ARRAYLIST);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "allFactories", "L" + LIST + ";");
        
        // this.factoriesBySupertype = new HashMap<>()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASHMAP);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASHMAP, "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        
        // Register all factories
        int localVarIndex = 1;
        for (ComponentInfo comp : components) {
            String factoryInternal = comp.getFactoryInternalName();
            String componentInternal = comp.getInternalName();
            
            // FactoryClass factory = new FactoryClass()
            mv.visitTypeInsn(NEW, factoryInternal);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, factoryInternal, "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, localVarIndex);
            
            // factoriesByType.put(ComponentClass.class, factory)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByType", "L" + MAP + ";");
            mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(componentInternal));
            mv.visitVarInsn(ALOAD, localVarIndex);
            mv.visitMethodInsn(INVOKEINTERFACE, MAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", true);
            mv.visitInsn(POP);
            
            // factoriesByName.put("componentName", factory)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesByName", "L" + MAP + ";");
            mv.visitLdcInsn(comp.getComponentName());
            mv.visitVarInsn(ALOAD, localVarIndex);
            mv.visitMethodInsn(INVOKEINTERFACE, MAP, "put",
                    "(L" + OBJECT + ";L" + OBJECT + ";)L" + OBJECT + ";", true);
            mv.visitInsn(POP);
            
            // allFactories.add(factory)
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "allFactories", "L" + LIST + ";");
            mv.visitVarInsn(ALOAD, localVarIndex);
            mv.visitMethodInsn(INVOKEINTERFACE, LIST, "add", "(L" + OBJECT + ";)Z", true);
            mv.visitInsn(POP);
            
            // Also register by the component type itself in factoriesBySupertype
            registerInSupertypeMap(mv, localVarIndex, componentInternal);
            
            localVarIndex++;
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void registerInSupertypeMap(MethodVisitor mv, int factoryVar, String componentInternal) {
        // Get or create list for this type
        // List list = factoriesBySupertype.computeIfAbsent(Type.class, k -> new ArrayList<>())
        
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
        
        // Create new list if null
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
        
        // list.add(factory)
        mv.visitTypeInsn(CHECKCAST, LIST);
        mv.visitVarInsn(ALOAD, factoryVar);
        mv.visitMethodInsn(INVOKEINTERFACE, LIST, "add", "(L" + OBJECT + ";)Z", true);
        mv.visitInsn(POP);
    }
    
    private void generateGetAllFactories(ClassWriter cw) {
        // public List<ComponentFactory<?>> getAllFactories()
        String signature = "()L" + LIST + "<L" + COMPONENT_FACTORY + "<*>;>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getAllFactories",
                "()L" + LIST + ";", signature, null);
        mv.visitCode();
        
        // return new ArrayList<>(allFactories) - return a copy for safety
        mv.visitTypeInsn(NEW, ARRAYLIST);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "allFactories", "L" + LIST + ";");
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST, "<init>", "(Ljava/util/Collection;)V", false);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetFactoryByType(ClassWriter cw) {
        // public <T> ComponentFactory<T> getFactory(Class<T> type)
        String signature = "<T:L" + OBJECT + ";>(L" + CLASS + "<TT;>;)L" + COMPONENT_FACTORY + "<TT;>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getFactory",
                "(L" + CLASS + ";)L" + COMPONENT_FACTORY + ";", signature, null);
        mv.visitCode();
        
        // return (ComponentFactory<T>) factoriesByType.get(type)
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
        // public ComponentFactory<?> getFactory(String name)
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
        // public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type)
        String signature = "<T:L" + OBJECT + ";>(L" + CLASS + "<TT;>;)L" + LIST + 
                "<L" + COMPONENT_FACTORY + "<+TT;>;>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getFactoriesForType",
                "(L" + CLASS + ";)L" + LIST + ";", signature, null);
        mv.visitCode();
        
        // List result = factoriesBySupertype.get(type)
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, REGISTRY_NAME, "factoriesBySupertype", "L" + MAP + ";");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, MAP, "get",
                "(L" + OBJECT + ";)L" + OBJECT + ";", true);
        mv.visitVarInsn(ASTORE, 2);
        
        // if (result == null) return Collections.emptyList()
        mv.visitVarInsn(ALOAD, 2);
        Label notNullLabel = new Label();
        mv.visitJumpInsn(IFNONNULL, notNullLabel);
        
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptyList",
                "()L" + LIST + ";", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(notNullLabel);
        mv.visitFrame(F_APPEND, 1, new Object[]{OBJECT}, 0, null);
        
        // return new ArrayList<>(result) - return a copy for safety
        mv.visitTypeInsn(NEW, ARRAYLIST);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, "java/util/Collection");
        mv.visitMethodInsn(INVOKESPECIAL, ARRAYLIST, "<init>", "(Ljava/util/Collection;)V", false);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
