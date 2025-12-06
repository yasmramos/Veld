package com.veld.processor;

import com.veld.runtime.Scope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;

/**
 * Generates the Veld bootstrap class as bytecode using ASM.
 * This class provides ULTRA-FAST static access to singletons - as fast as Dagger.
 * 
 * Generated class: com.veld.generated.Veld
 * 
 * Usage:
 *   // Ultra-fast static access (as fast as Dagger):
 *   MyService service = Veld.myService();
 *   
 *   // Or use container API:
 *   VeldContainer container = Veld.createContainer();
 */
public class VeldBootstrapGenerator implements Opcodes {
    
    private static final String VELD_CLASS = "com/veld/generated/Veld";
    private static final String REGISTRY_CLASS = "com/veld/generated/VeldRegistry";
    private static final String CONTAINER_CLASS = "com/veld/runtime/VeldContainer";
    private static final String COMPONENT_REGISTRY_CLASS = "com/veld/runtime/ComponentRegistry";
    
    private final List<ComponentInfo> components;
    
    public VeldBootstrapGenerator(List<ComponentInfo> components) {
        this.components = components;
    }
    
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // public final class Veld
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, VELD_CLASS, null, "java/lang/Object", null);
        
        // Generate static volatile fields for each singleton
        for (ComponentInfo comp : components) {
            if (comp.getScope() == Scope.SINGLETON) {
                cw.visitField(
                    ACC_PRIVATE | ACC_STATIC | ACC_VOLATILE,
                    getFieldName(comp),
                    "L" + comp.getInternalName() + ";",
                    null,
                    null
                ).visitEnd();
            }
        }
        
        // Lock object for synchronization
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "LOCK", 
            "Ljava/lang/Object;", null, null).visitEnd();
        
        // Static initializer for LOCK
        generateStaticInit(cw);
        
        // Private constructor
        generatePrivateConstructor(cw);
        
        // Generate getter method for each component
        for (ComponentInfo comp : components) {
            if (comp.getScope() == Scope.SINGLETON) {
                generateSingletonGetter(cw, comp);
            } else {
                generatePrototypeGetter(cw, comp);
            }
        }
        
        // Container factory methods
        generateCreateContainer(cw);
        generateCreateRegistry(cw);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateStaticInit(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "LOCK", "Ljava/lang/Object;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
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
     * Generates ultra-fast singleton getter with double-check locking.
     * 
     * Generated code equivalent:
     * public static MyService myService() {
     *     MyService local = myService;
     *     if (local == null) {
     *         synchronized (LOCK) {
     *             local = myService;
     *             if (local == null) {
     *                 myService = local = new MyService(dependency1(), dependency2());
     *             }
     *         }
     *     }
     *     return local;
     * }
     */
    private void generateSingletonGetter(ClassWriter cw, ComponentInfo comp) {
        String methodName = getMethodName(comp);
        String fieldName = getFieldName(comp);
        String returnType = "L" + comp.getInternalName() + ";";
        
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            methodName,
            "()" + returnType,
            null,
            null
        );
        mv.visitCode();
        
        // local = field
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, returnType);
        mv.visitVarInsn(ASTORE, 0);
        
        // if (local == null)
        mv.visitVarInsn(ALOAD, 0);
        Label notNullLabel = new Label();
        mv.visitJumpInsn(IFNONNULL, notNullLabel);
        
        // synchronized (LOCK) {
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "LOCK", "Ljava/lang/Object;");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitInsn(MONITORENTER);
        
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, null);
        
        mv.visitLabel(tryStart);
        
        // local = field (double-check)
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, returnType);
        mv.visitVarInsn(ASTORE, 0);
        
        // if (local == null)
        mv.visitVarInsn(ALOAD, 0);
        Label alreadyCreated = new Label();
        mv.visitJumpInsn(IFNONNULL, alreadyCreated);
        
        // Create instance: new Component(dependencies...)
        mv.visitTypeInsn(NEW, comp.getInternalName());
        mv.visitInsn(DUP);
        
        // Push constructor arguments
        StringBuilder constructorDesc = new StringBuilder("(");
        InjectionPoint constructor = comp.getConstructorInjection();
        List<InjectionPoint.Dependency> deps = constructor != null ? constructor.getDependencies() : Collections.emptyList();
        for (InjectionPoint.Dependency dep : deps) {
            String depType = dep.getTypeName().replace('.', '/');
            constructorDesc.append("L").append(depType).append(";");
            
            // Call the getter for this dependency
            ComponentInfo depComp = findComponentByType(depType);
            if (depComp != null) {
                String depMethod = getMethodName(depComp);
                String depReturnType = "L" + depComp.getInternalName() + ";";
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, depMethod, "()" + depReturnType, false);
            } else {
                // Dependency not found - this would be a compile error
                mv.visitInsn(ACONST_NULL);
            }
        }
        constructorDesc.append(")V");
        
        mv.visitMethodInsn(INVOKESPECIAL, comp.getInternalName(), "<init>", 
            constructorDesc.toString(), false);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 0);
        
        // field = local
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, fieldName, returnType);
        
        mv.visitLabel(alreadyCreated);
        mv.visitFrame(F_FULL, 2, new Object[]{comp.getInternalName(), "java/lang/Object"}, 0, new Object[]{});
        
        // MONITOREXIT
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(MONITOREXIT);
        mv.visitLabel(tryEnd);
        Label afterSync = new Label();
        mv.visitJumpInsn(GOTO, afterSync);
        
        // Exception handler
        mv.visitLabel(catchHandler);
        mv.visitFrame(F_FULL, 2, new Object[]{comp.getInternalName(), "java/lang/Object"}, 1, new Object[]{"java/lang/Throwable"});
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(MONITOREXIT);
        mv.visitInsn(ATHROW);
        
        mv.visitLabel(afterSync);
        mv.visitFrame(F_FULL, 1, new Object[]{comp.getInternalName()}, 0, new Object[]{});
        
        mv.visitLabel(notNullLabel);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        
        // return local
        mv.visitVarInsn(ALOAD, 0);
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
        
        // return new Component(dependencies...)
        mv.visitTypeInsn(NEW, comp.getInternalName());
        mv.visitInsn(DUP);
        
        StringBuilder constructorDesc = new StringBuilder("(");
        InjectionPoint constructor = comp.getConstructorInjection();
        List<InjectionPoint.Dependency> deps = constructor != null ? constructor.getDependencies() : Collections.emptyList();
        for (InjectionPoint.Dependency dep : deps) {
            String depType = dep.getTypeName().replace('.', '/');
            constructorDesc.append("L").append(depType).append(";");
            
            ComponentInfo depComp = findComponentByType(depType);
            if (depComp != null) {
                String depMethod = getMethodName(depComp);
                String depReturnType = "L" + depComp.getInternalName() + ";";
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, depMethod, "()" + depReturnType, false);
            } else {
                mv.visitInsn(ACONST_NULL);
            }
        }
        constructorDesc.append(")V");
        
        mv.visitMethodInsn(INVOKESPECIAL, comp.getInternalName(), "<init>", 
            constructorDesc.toString(), false);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private ComponentInfo findComponentByType(String internalName) {
        for (ComponentInfo comp : components) {
            if (comp.getInternalName().equals(internalName)) {
                return comp;
            }
            // Check interfaces
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
        // Decapitalize
        if (simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(1))) {
            return simpleName;
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
    
    private String getFieldName(ComponentInfo comp) {
        return "_" + getMethodName(comp);
    }
    
    private void generateCreateContainer(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "createContainer",
            "()L" + CONTAINER_CLASS + ";",
            null,
            null
        );
        mv.visitCode();
        mv.visitTypeInsn(NEW, CONTAINER_CLASS);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "createRegistry", 
            "()L" + COMPONENT_REGISTRY_CLASS + ";", false);
        mv.visitMethodInsn(INVOKESPECIAL, CONTAINER_CLASS, "<init>", 
            "(L" + COMPONENT_REGISTRY_CLASS + ";)V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateCreateRegistry(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "createRegistry",
            "()L" + COMPONENT_REGISTRY_CLASS + ";",
            null,
            null
        );
        mv.visitCode();
        mv.visitTypeInsn(NEW, REGISTRY_CLASS);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, REGISTRY_CLASS, "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    public String getClassName() {
        return VELD_CLASS.replace('/', '.');
    }
}
