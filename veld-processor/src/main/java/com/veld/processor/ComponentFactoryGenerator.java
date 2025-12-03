package com.veld.processor;

import com.veld.processor.InjectionPoint.Dependency;
import com.veld.runtime.Scope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * Generates bytecode for ComponentFactory implementations using ASM.
 * NO REFLECTION - pure bytecode generation.
 * 
 * Generated class structure:
 * <pre>
 * public class MyService$$VeldFactory implements ComponentFactory<MyService> {
 *     
 *     public MyService$$VeldFactory() { }
 *     
 *     public MyService create(VeldContainer container) {
 *         // Instantiate and inject dependencies
 *         // Dependencies resolved via container.get()
 *     }
 *     
 *     public Class<MyService> getComponentType() { return MyService.class; }
 *     public String getComponentName() { return "myService"; }
 *     public Scope getScope() { return Scope.SINGLETON; }
 *     public void invokePostConstruct(MyService instance) { instance.init(); }
 *     public void invokePreDestroy(MyService instance) { instance.cleanup(); }
 * }
 * </pre>
 */
public final class ComponentFactoryGenerator {
    
    private static final String COMPONENT_FACTORY = "com/veld/runtime/ComponentFactory";
    private static final String VELD_CONTAINER = "com/veld/runtime/VeldContainer";
    private static final String SCOPE = "com/veld/runtime/Scope";
    private static final String PROVIDER = "com/veld/runtime/Provider";
    private static final String OBJECT = "java/lang/Object";
    private static final String CLASS = "java/lang/Class";
    private static final String STRING = "java/lang/String";
    
    private final ComponentInfo component;
    
    public ComponentFactoryGenerator(ComponentInfo component) {
        this.component = component;
    }
    
    /**
     * Generates the complete bytecode for the factory class.
     * @return The bytecode as a byte array
     */
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        String factoryInternal = component.getFactoryInternalName();
        String componentInternal = component.getInternalName();
        
        // Class declaration: public class X$$VeldFactory implements ComponentFactory<X>
        String signature = "L" + OBJECT + ";L" + COMPONENT_FACTORY + "<L" + componentInternal + ";>;";
        cw.visit(V11, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                factoryInternal,
                signature,
                OBJECT,
                new String[]{COMPONENT_FACTORY});
        
        // Default constructor
        generateConstructor(cw);
        
        // create(VeldContainer) method
        generateCreateMethod(cw, factoryInternal, componentInternal);
        
        // getComponentType() method
        generateGetComponentType(cw, componentInternal);
        
        // getComponentName() method
        generateGetComponentName(cw);
        
        // getScope() method
        generateGetScope(cw);
        
        // isLazy() method (only if component is lazy)
        if (component.isLazy()) {
            generateIsLazy(cw);
        }
        
        // invokePostConstruct(T) method
        generateInvokePostConstruct(cw, componentInternal);
        
        // invokePreDestroy(T) method
        generateInvokePreDestroy(cw, componentInternal);
        
        // Bridge methods for type erasure
        generateBridgeMethods(cw, factoryInternal, componentInternal);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        
        // super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateCreateMethod(ClassWriter cw, String factoryInternal, String componentInternal) {
        // public T create(VeldContainer container)
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "create",
                "(L" + VELD_CONTAINER + ";)L" + componentInternal + ";", null, null);
        mv.visitCode();
        
        InjectionPoint constructor = component.getConstructorInjection();
        
        if (constructor == null || constructor.getDependencies().isEmpty()) {
            // new Component()
            mv.visitTypeInsn(NEW, componentInternal);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, componentInternal, "<init>", "()V", false);
        } else {
            // new Component(dep1, dep2, ...)
            mv.visitTypeInsn(NEW, componentInternal);
            mv.visitInsn(DUP);
            
            // Load each dependency from container
            for (Dependency dep : constructor.getDependencies()) {
                loadDependency(mv, dep);
            }
            
            mv.visitMethodInsn(INVOKESPECIAL, componentInternal, "<init>",
                    constructor.getDescriptor(), false);
        }
        
        // Store instance in local variable 2 (0=this, 1=container, 2=instance)
        mv.visitVarInsn(ASTORE, 2);
        
        // Field injections
        for (InjectionPoint field : component.getFieldInjections()) {
            mv.visitVarInsn(ALOAD, 2);
            Dependency dep = field.getDependencies().get(0);
            loadDependency(mv, dep);
            mv.visitFieldInsn(PUTFIELD, componentInternal, field.getName(), field.getDescriptor());
        }
        
        // Method injections
        for (InjectionPoint method : component.getMethodInjections()) {
            mv.visitVarInsn(ALOAD, 2);
            for (Dependency dep : method.getDependencies()) {
                loadDependency(mv, dep);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, componentInternal, 
                    method.getName(), method.getDescriptor(), false);
        }
        
        // Return instance
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void loadDependency(MethodVisitor mv, Dependency dep) {
        if (dep.isProvider()) {
            // For Provider<T>, call container.getProvider(T.class)
            loadProviderDependency(mv, dep);
        } else if (dep.isLazy()) {
            // For @Lazy, wrap in a LazyHolder that calls container.get() on first access
            loadLazyDependency(mv, dep);
        } else {
            // Regular dependency
            loadRegularDependency(mv, dep);
        }
    }
    
    private void loadRegularDependency(MethodVisitor mv, Dependency dep) {
        String depInternal = dep.getTypeName().replace('.', '/');
        
        // container.get(DependencyType.class)
        mv.visitVarInsn(ALOAD, 1); // Load container (parameter 1)
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(depInternal));
        mv.visitMethodInsn(INVOKEVIRTUAL, VELD_CONTAINER, "get",
                "(L" + CLASS + ";)L" + OBJECT + ";", false);
        
        // Cast to dependency type
        mv.visitTypeInsn(CHECKCAST, depInternal);
    }
    
    private void loadProviderDependency(MethodVisitor mv, Dependency dep) {
        String actualTypeInternal = dep.getActualTypeName().replace('.', '/');
        
        // container.getProvider(ActualType.class)
        mv.visitVarInsn(ALOAD, 1); // Load container (parameter 1)
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(actualTypeInternal));
        mv.visitMethodInsn(INVOKEVIRTUAL, VELD_CONTAINER, "getProvider",
                "(L" + CLASS + ";)L" + PROVIDER + ";", false);
        
        // No need to cast - Provider<T> is returned
    }
    
    private void loadLazyDependency(MethodVisitor mv, Dependency dep) {
        String actualTypeInternal = dep.getActualTypeName().replace('.', '/');
        
        // container.getLazy(ActualType.class)
        mv.visitVarInsn(ALOAD, 1); // Load container (parameter 1)
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(actualTypeInternal));
        mv.visitMethodInsn(INVOKEVIRTUAL, VELD_CONTAINER, "getLazy",
                "(L" + CLASS + ";)L" + OBJECT + ";", false);
        
        // Cast to dependency type
        mv.visitTypeInsn(CHECKCAST, actualTypeInternal);
    }
    
    private void generateGetComponentType(ClassWriter cw, String componentInternal) {
        String signature = "()L" + CLASS + "<L" + componentInternal + ";>;";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getComponentType",
                "()L" + CLASS + ";", signature, null);
        mv.visitCode();
        
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(componentInternal));
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetComponentName(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getComponentName",
                "()L" + STRING + ";", null, null);
        mv.visitCode();
        
        mv.visitLdcInsn(component.getComponentName());
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetScope(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getScope",
                "()L" + SCOPE + ";", null, null);
        mv.visitCode();
        
        String scopeName = component.getScope() == Scope.SINGLETON ? "SINGLETON" : "PROTOTYPE";
        mv.visitFieldInsn(GETSTATIC, SCOPE, scopeName, "L" + SCOPE + ";");
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateIsLazy(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isLazy",
                "()Z", null, null);
        mv.visitCode();
        
        mv.visitInsn(ICONST_1); // return true
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateInvokePostConstruct(ClassWriter cw, String componentInternal) {
        // invokePostConstruct(T instance) - with generic type
        String signature = "(L" + componentInternal + ";)V";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invokePostConstruct",
                "(L" + componentInternal + ";)V", null, null);
        mv.visitCode();
        
        if (component.hasPostConstruct()) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, componentInternal,
                    component.getPostConstructMethod(), 
                    component.getPostConstructDescriptor(), false);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        // Bridge method: invokePostConstruct(Object) -> invokePostConstruct(T)
        MethodVisitor bridge = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
                "invokePostConstruct", "(L" + OBJECT + ";)V", null, null);
        bridge.visitCode();
        
        bridge.visitVarInsn(ALOAD, 0);
        bridge.visitVarInsn(ALOAD, 1);
        bridge.visitTypeInsn(CHECKCAST, componentInternal);
        bridge.visitMethodInsn(INVOKEVIRTUAL, component.getFactoryInternalName(),
                "invokePostConstruct", "(L" + componentInternal + ";)V", false);
        bridge.visitInsn(RETURN);
        
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
    }
    
    private void generateInvokePreDestroy(ClassWriter cw, String componentInternal) {
        // invokePreDestroy(T instance) - with generic type
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invokePreDestroy",
                "(L" + componentInternal + ";)V", null, null);
        mv.visitCode();
        
        if (component.hasPreDestroy()) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, componentInternal,
                    component.getPreDestroyMethod(),
                    component.getPreDestroyDescriptor(), false);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        // Bridge method: invokePreDestroy(Object) -> invokePreDestroy(T)
        MethodVisitor bridge = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
                "invokePreDestroy", "(L" + OBJECT + ";)V", null, null);
        bridge.visitCode();
        
        bridge.visitVarInsn(ALOAD, 0);
        bridge.visitVarInsn(ALOAD, 1);
        bridge.visitTypeInsn(CHECKCAST, componentInternal);
        bridge.visitMethodInsn(INVOKEVIRTUAL, component.getFactoryInternalName(),
                "invokePreDestroy", "(L" + componentInternal + ";)V", false);
        bridge.visitInsn(RETURN);
        
        bridge.visitMaxs(0, 0);
        bridge.visitEnd();
    }
    
    private void generateBridgeMethods(ClassWriter cw, String factoryInternal, String componentInternal) {
        // Bridge method for create(VeldContainer): Object create(VeldContainer) calls T create(VeldContainer)
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, 
                "create", "(L" + VELD_CONTAINER + ";)L" + OBJECT + ";", null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, factoryInternal, "create",
                "(L" + VELD_CONTAINER + ";)L" + componentInternal + ";", false);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
