package io.github.yasmramos.veld.processor;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ASM bytecode generation utilities for Veld DI Framework.
 * 
 * Provides common patterns for generating Java bytecode:
 * - Constructor generation
 * - Static method generation
 * - Object instantiation
 * - Method invocation patterns
 * - Type descriptor handling
 * 
 * All methods follow ASM best practices and generate valid JVM bytecode.
 */
public final class AsmUtils implements Opcodes {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMMON INTERNAL NAMES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final String OBJECT = "java/lang/Object";
    public static final String STRING = "java/lang/String";
    public static final String CLASS = "java/lang/Class";
    public static final String THROWABLE = "java/lang/Throwable";
    public static final String EXCEPTION = "java/lang/Exception";
    public static final String RUNTIME_EXCEPTION = "java/lang/RuntimeException";
    
    // Veld specific
    public static final String COMPONENT_REGISTRY = "io/github/yasmramos/veld/runtime/ComponentRegistry";
    public static final String COMPONENT_FACTORY = "io/github/yasmramos/veld/runtime/ComponentFactory";
    public static final String VELD_EXCEPTION = "io/github/yasmramos/veld/VeldException";
    public static final String SCOPE = "io.github.yasmramos.veld.runtime.LegacyScope";
    
    // Generated classes
    public static final String VELD_REGISTRY = "io/github/yasmramos/veld/generated/VeldRegistry";
    public static final String VELD_BOOTSTRAP = "io/github/yasmramos/Veld";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMMON DESCRIPTORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final String VOID_DESCRIPTOR = "()V";
    public static final String CLASS_DESCRIPTOR = "()Ljava/lang/Class;";
    public static final String STRING_DESCRIPTOR = "()Ljava/lang/String;";
    public static final String OBJECT_DESCRIPTOR = "()Ljava/lang/Object;";
    
    private AsmUtils() {
        // Utility class - no instantiation
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLASS WRITER CREATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a ClassWriter with COMPUTE_FRAMES and COMPUTE_MAXS enabled.
     * This is the recommended configuration for most bytecode generation.
     * 
     * @return a new ClassWriter instance
     */
    public static ClassWriter createClassWriter() {
        return new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }
    
    /**
     * Starts a public class that extends Object.
     * 
     * @param cw the ClassWriter
     * @param internalName the internal name of the class (e.g., "com/example/MyClass")
     * @param interfaces optional interfaces to implement
     */
    public static void visitPublicClass(ClassWriter cw, String internalName, String... interfaces) {
        cw.visit(V11, ACC_PUBLIC | ACC_SUPER, internalName, null, OBJECT, 
                interfaces.length > 0 ? interfaces : null);
    }
    
    /**
     * Starts a public final class that extends Object.
     * 
     * @param cw the ClassWriter
     * @param internalName the internal name of the class
     * @param interfaces optional interfaces to implement
     */
    public static void visitPublicFinalClass(ClassWriter cw, String internalName, String... interfaces) {
        cw.visit(V11, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, internalName, null, OBJECT,
                interfaces.length > 0 ? interfaces : null);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR GENERATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates a default no-arg constructor that calls super().
     * 
     * @param cw the ClassWriter
     */
    public static void generateDefaultConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", VOID_DESCRIPTOR, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", VOID_DESCRIPTOR, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates a private no-arg constructor (for utility classes or singletons).
     * 
     * @param cw the ClassWriter
     */
    public static void generatePrivateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", VOID_DESCRIPTOR, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT, "<init>", VOID_DESCRIPTOR, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // OBJECT INSTANTIATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates bytecode to create a new instance using no-arg constructor.
     * Stack effect: pushes the new instance onto the stack.
     * 
     * Generated bytecode:
     *   NEW classInternalName
     *   DUP
     *   INVOKESPECIAL classInternalName.<init>()V
     * 
     * @param mv the MethodVisitor
     * @param classInternalName the internal name of the class to instantiate
     */
    public static void generateNewInstance(MethodVisitor mv, String classInternalName) {
        mv.visitTypeInsn(NEW, classInternalName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", VOID_DESCRIPTOR, false);
    }
    
    /**
     * Generates bytecode to create a new instance with constructor arguments.
     * The constructor arguments must already be on the stack.
     * Stack effect: replaces arguments with the new instance.
     * 
     * @param mv the MethodVisitor
     * @param classInternalName the internal name of the class
     * @param constructorDescriptor the constructor descriptor (e.g., "(Ljava/lang/String;I)V")
     */
    public static void generateNewInstanceWithArgs(MethodVisitor mv, String classInternalName, 
                                                    String constructorDescriptor) {
        mv.visitTypeInsn(NEW, classInternalName);
        mv.visitInsn(DUP);
        // Note: Arguments should be loaded before calling this, then:
        mv.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", constructorDescriptor, false);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // METHOD INVOCATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates a static method call.
     * 
     * @param mv the MethodVisitor
     * @param owner the internal name of the class containing the method
     * @param methodName the method name
     * @param descriptor the method descriptor
     */
    public static void invokeStatic(MethodVisitor mv, String owner, String methodName, String descriptor) {
        mv.visitMethodInsn(INVOKESTATIC, owner, methodName, descriptor, false);
    }
    
    /**
     * Generates a virtual method call (instance method).
     * 
     * @param mv the MethodVisitor
     * @param owner the internal name of the class
     * @param methodName the method name
     * @param descriptor the method descriptor
     */
    public static void invokeVirtual(MethodVisitor mv, String owner, String methodName, String descriptor) {
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, methodName, descriptor, false);
    }
    
    /**
     * Generates a special method call (constructor or super method).
     * 
     * @param mv the MethodVisitor
     * @param owner the internal name of the class
     * @param methodName the method name (usually "<init>")
     * @param descriptor the method descriptor
     */
    public static void invokeSpecial(MethodVisitor mv, String owner, String methodName, String descriptor) {
        mv.visitMethodInsn(INVOKESPECIAL, owner, methodName, descriptor, false);
    }
    
    /**
     * Generates an interface method call.
     * 
     * @param mv the MethodVisitor
     * @param owner the internal name of the interface
     * @param methodName the method name
     * @param descriptor the method descriptor
     */
    public static void invokeInterface(MethodVisitor mv, String owner, String methodName, String descriptor) {
        mv.visitMethodInsn(INVOKEINTERFACE, owner, methodName, descriptor, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEPTION HANDLING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates bytecode to throw a VeldException with a message.
     * 
     * Generated bytecode:
     *   NEW io/github/yasmramos/runtime/VeldException
     *   DUP
     *   LDC "message"
     *   INVOKESPECIAL VeldException.<init>(String)V
     *   ATHROW
     * 
     * @param mv the MethodVisitor
     * @param message the exception message
     */
    public static void throwVeldException(MethodVisitor mv, String message) {
        mv.visitTypeInsn(NEW, VELD_EXCEPTION);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(INVOKESPECIAL, VELD_EXCEPTION, "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
    }
    
    /**
     * Generates bytecode to throw a RuntimeException wrapping an existing exception.
     * Assumes the exception to wrap is on top of the stack.
     * 
     * @param mv the MethodVisitor
     */
    public static void throwRuntimeExceptionWrapping(MethodVisitor mv) {
        // Stack: [..., Throwable]
        mv.visitTypeInsn(NEW, RUNTIME_EXCEPTION);
        // Stack: [..., Throwable, RuntimeException]
        mv.visitInsn(DUP_X1);
        // Stack: [..., RuntimeException, Throwable, RuntimeException]
        mv.visitInsn(SWAP);
        // Stack: [..., RuntimeException, RuntimeException, Throwable]
        mv.visitMethodInsn(INVOKESPECIAL, RUNTIME_EXCEPTION, "<init>", "(Ljava/lang/Throwable;)V", false);
        // Stack: [..., RuntimeException]
        mv.visitInsn(ATHROW);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPE UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Converts a fully qualified class name to internal name.
     * Example: "com.example.MyClass" -> "com/example/MyClass"
     * 
     * @param className the fully qualified class name
     * @return the internal name
     */
    public static String toInternalName(String className) {
        return className.replace('.', '/');
    }
    
    /**
     * Converts an internal name to a type descriptor.
     * Example: "com/example/MyClass" -> "Lcom/example/MyClass;"
     * 
     * @param internalName the internal name
     * @return the type descriptor
     */
    public static String toDescriptor(String internalName) {
        return "L" + internalName + ";";
    }
    
    /**
     * Creates a method descriptor for a method returning an object type.
     * Example: toReturnDescriptor("com/example/MyClass") -> "()Lcom/example/MyClass;"
     * 
     * @param returnTypeInternal the internal name of the return type
     * @return the method descriptor
     */
    public static String toReturnDescriptor(String returnTypeInternal) {
        return "()" + toDescriptor(returnTypeInternal);
    }
    
    /**
     * Loads a Class constant onto the stack.
     * 
     * @param mv the MethodVisitor
     * @param internalName the internal name of the class
     */
    public static void loadClassConstant(MethodVisitor mv, String internalName) {
        mv.visitLdcInsn(Type.getObjectType(internalName));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RETURN INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates ARETURN (return object reference).
     */
    public static void returnObject(MethodVisitor mv) {
        mv.visitInsn(ARETURN);
    }
    
    /**
     * Generates RETURN (void return).
     */
    public static void returnVoid(MethodVisitor mv) {
        mv.visitInsn(RETURN);
    }
    
    /**
     * Completes a method by calling visitMaxs and visitEnd.
     * When using COMPUTE_MAXS, the parameters to visitMaxs are ignored.
     * 
     * @param mv the MethodVisitor
     */
    public static void endMethod(MethodVisitor mv) {
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
