package com.veld.processor;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * Generates the VeldFast bootstrap class that provides ultra-fast container creation.
 * 
 * <p>Generated class:
 * <pre>
 * public final class VeldFast {
 *     private VeldFast() {}
 *     
 *     public static FastRegistry createFastRegistry() {
 *         return new VeldFastRegistry();
 *     }
 * }
 * </pre>
 */
public final class FastBootstrapGenerator {
    
    private static final String BOOTSTRAP_NAME = "com/veld/generated/VeldFast";
    private static final String FAST_REGISTRY = "com/veld/runtime/fast/FastRegistry";
    private static final String REGISTRY_NAME = "com/veld/generated/VeldFastRegistry";
    private static final String OBJECT = "java/lang/Object";
    
    public String getBootstrapClassName() {
        return "com.veld.generated.VeldFast";
    }
    
    public String getBootstrapInternalName() {
        return BOOTSTRAP_NAME;
    }
    
    /**
     * Generates the bootstrap class bytecode.
     */
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        // public final class VeldFast
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                BOOTSTRAP_NAME,
                null,
                OBJECT,
                null);
        
        // Private constructor
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        // public static FastRegistry createFastRegistry()
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "createFastRegistry",
                "()L" + FAST_REGISTRY + ";", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, REGISTRY_NAME);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, REGISTRY_NAME, "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        cw.visitEnd();
        return cw.toByteArray();
    }
}
