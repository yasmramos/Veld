package io.github.yasmramos.veld.weaver;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * Generates AOP proxy classes at compile-time.
 * 
 * <p>Each proxy class:
 * <ul>
 *   <li>Extends the original class</li>
 *   <li>Overrides intercepted methods</li>
 *   <li>Uses DirectInvoker0-4 for zero-reflection invocation</li>
 *   <li>Registers method metadata in static initializer</li>
 * </ul>
 * 
 * <p>Generated class name: {OriginalClass}$AopProxy
 */
public class AopProxyGenerator implements Opcodes {
    
    private static final String PROXY_METHOD_HANDLER = "io/github/yasmramos/veld/aop/proxy/ProxyMethodHandler";
    private static final String METHOD_METADATA = PROXY_METHOD_HANDLER + "$MethodMetadata";
    private static final String DIRECT_INVOKER = PROXY_METHOD_HANDLER + "$DirectInvoker";
    private static final String DIRECT_INVOKER_0 = PROXY_METHOD_HANDLER + "$DirectInvoker0";
    private static final String DIRECT_INVOKER_1 = PROXY_METHOD_HANDLER + "$DirectInvoker1";
    private static final String DIRECT_INVOKER_2 = PROXY_METHOD_HANDLER + "$DirectInvoker2";
    private static final String DIRECT_INVOKER_3 = PROXY_METHOD_HANDLER + "$DirectInvoker3";
    private static final String DIRECT_INVOKER_4 = PROXY_METHOD_HANDLER + "$DirectInvoker4";
    
    private final List<ProxyMeta> proxyMetas;
    
    public AopProxyGenerator(List<ProxyMeta> proxyMetas) {
        this.proxyMetas = proxyMetas;
    }
    
    /**
     * Generates all proxy classes.
     * @return Map of internal class name to bytecode
     */
    public Map<String, byte[]> generateAll() {
        Map<String, byte[]> result = new HashMap<>();
        
        for (ProxyMeta meta : proxyMetas) {
            String proxyName = meta.targetInternal() + "$AopProxy";
            result.put(proxyName, generateProxy(meta));
        }
        
        return result;
    }
    
    /**
     * Generates a single proxy class.
     */
    private byte[] generateProxy(ProxyMeta meta) {
        String proxyInternal = meta.targetInternal() + "$AopProxy";
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, proxyInternal, null, 
                 meta.targetInternal(), null);
        
        // Generate static initializer to register methods
        generateStaticInit(cw, meta, proxyInternal);
        
        // Generate constructor that delegates to super
        generateConstructor(cw, meta);
        
        // Generate overridden methods
        for (MethodMeta method : meta.methods()) {
            generateProxyMethod(cw, meta, method, proxyInternal);
            generateSuperInvoker(cw, meta, method, proxyInternal);
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    /**
     * Generates static initializer that registers method metadata with ProxyMethodHandler.
     */
    private void generateStaticInit(ClassWriter cw, ProxyMeta meta, String proxyInternal) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        for (MethodMeta method : meta.methods()) {
            String methodKey = meta.targetInternal().replace('/', '.') + "#" + method.name() + "#" + method.descriptor();
            int arity = Type.getArgumentTypes(method.descriptor()).length;
            
            // Create MethodMetadata with appropriate invoker
            mv.visitLdcInsn(methodKey);
            
            // new MethodMetadata(className, methodName, paramTypes, returnType, invoker, interceptors)
            mv.visitTypeInsn(NEW, METHOD_METADATA);
            mv.visitInsn(DUP);
            
            // className
            mv.visitLdcInsn(meta.targetInternal().replace('/', '.'));
            
            // methodName
            mv.visitLdcInsn(method.name());
            
            // parameterTypes array
            Type[] argTypes = Type.getArgumentTypes(method.descriptor());
            pushInt(mv, argTypes.length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
            for (int i = 0; i < argTypes.length; i++) {
                mv.visitInsn(DUP);
                pushInt(mv, i);
                mv.visitLdcInsn(argTypes[i].getClassName());
                mv.visitInsn(AASTORE);
            }
            
            // returnType
            mv.visitLdcInsn(Type.getReturnType(method.descriptor()).getClassName());
            
            // Create DirectInvoker using lambda/method reference to super call
            generateInvokerLambda(mv, meta, method, proxyInternal, arity);
            
            // interceptors list (empty for now - will be populated at runtime registration)
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptyList", 
                              "()Ljava/util/List;", false);
            
            // Call appropriate constructor based on arity
            String invokerType = getInvokerType(arity);
            String ctorDesc = getMetadataConstructorDesc(arity);
            mv.visitMethodInsn(INVOKESPECIAL, METHOD_METADATA, "<init>", ctorDesc, false);
            
            // Register with ProxyMethodHandler
            mv.visitMethodInsn(INVOKESTATIC, PROXY_METHOD_HANDLER, "registerMethod",
                              "(Ljava/lang/String;L" + METHOD_METADATA + ";)V", false);
        }
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates the invoker lambda for a method.
     */
    private void generateInvokerLambda(MethodVisitor mv, ProxyMeta meta, MethodMeta method, 
                                        String proxyInternal, int arity) {
        String invokerType = getInvokerType(arity);
        String invokerMethod = getInvokerMethodName(arity);
        String invokerDesc = getInvokerMethodDesc(arity);
        String superMethod = "_super$" + method.name();
        
        // Use invokedynamic to create the lambda
        org.objectweb.asm.Handle bsmHandle = new org.objectweb.asm.Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
            "Ljava/lang/invoke/CallSite;", false);
        
        String samDesc = "()L" + invokerType + ";";
        String implDesc = getSuperInvokerDesc(method.descriptor(), arity);
        
        mv.visitInvokeDynamicInsn("invoke", samDesc, bsmHandle,
            Type.getType(invokerDesc),
            new org.objectweb.asm.Handle(H_INVOKESTATIC, proxyInternal, superMethod, implDesc, false),
            Type.getType(invokerDesc));
    }
    
    /**
     * Generates a static method that calls super.method() - used by the invoker lambda.
     */
    private void generateSuperInvoker(ClassWriter cw, ProxyMeta meta, MethodMeta method, String proxyInternal) {
        Type[] argTypes = Type.getArgumentTypes(method.descriptor());
        Type returnType = Type.getReturnType(method.descriptor());
        int arity = argTypes.length;
        
        String superMethod = "_super$" + method.name();
        String implDesc = getSuperInvokerDesc(method.descriptor(), arity);
        
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, superMethod, implDesc, null, 
                                          new String[]{"java/lang/Throwable"});
        mv.visitCode();
        
        // Load target and cast to proxy type
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(CHECKCAST, proxyInternal);
        
        // Load arguments based on arity
        if (arity <= 4) {
            for (int i = 0; i < arity; i++) {
                mv.visitVarInsn(ALOAD, i + 1);
                unbox(mv, argTypes[i]);
            }
        } else {
            // N>4: args come as Object[]
            for (int i = 0; i < arity; i++) {
                mv.visitVarInsn(ALOAD, 1);
                pushInt(mv, i);
                mv.visitInsn(AALOAD);
                unbox(mv, argTypes[i]);
            }
        }
        
        // Call super.method()
        mv.visitMethodInsn(INVOKESPECIAL, meta.targetInternal(), method.name(), method.descriptor(), false);
        
        // Box return value if needed
        box(mv, returnType);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates constructor that delegates to super.
     */
    private void generateConstructor(ClassWriter cw, ProxyMeta meta) {
        // Default no-arg constructor
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, meta.targetInternal(), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates a proxy method that delegates to ProxyMethodHandler.
     */
    private void generateProxyMethod(ClassWriter cw, ProxyMeta meta, MethodMeta method, String proxyInternal) {
        Type[] argTypes = Type.getArgumentTypes(method.descriptor());
        Type returnType = Type.getReturnType(method.descriptor());
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, method.name(), method.descriptor(), null, 
                                          new String[]{"java/lang/Throwable"});
        mv.visitCode();
        
        String methodKey = meta.targetInternal().replace('/', '.') + "#" + method.name() + "#" + method.descriptor();
        
        // ProxyMethodHandler.invoke(this, methodKey, args)
        mv.visitVarInsn(ALOAD, 0);  // this
        mv.visitLdcInsn(methodKey);  // methodKey
        
        // Create Object[] args
        pushInt(mv, argTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        
        int localIndex = 1;
        for (int i = 0; i < argTypes.length; i++) {
            mv.visitInsn(DUP);
            pushInt(mv, i);
            mv.visitVarInsn(argTypes[i].getOpcode(ILOAD), localIndex);
            box(mv, argTypes[i]);
            mv.visitInsn(AASTORE);
            localIndex += argTypes[i].getSize();
        }
        
        // Call ProxyMethodHandler.invoke
        mv.visitMethodInsn(INVOKESTATIC, PROXY_METHOD_HANDLER, "invoke",
                          "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        
        // Unbox and return
        unboxReturn(mv, returnType);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    // === Helper methods ===
    
    private String getInvokerType(int arity) {
        switch (arity) {
            case 0: return DIRECT_INVOKER_0;
            case 1: return DIRECT_INVOKER_1;
            case 2: return DIRECT_INVOKER_2;
            case 3: return DIRECT_INVOKER_3;
            case 4: return DIRECT_INVOKER_4;
            default: return DIRECT_INVOKER;
        }
    }
    
    private String getInvokerMethodName(int arity) {
        return "invoke";
    }
    
    private String getInvokerMethodDesc(int arity) {
        switch (arity) {
            case 0: return "(Ljava/lang/Object;)Ljava/lang/Object;";
            case 1: return "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
            case 2: return "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
            case 3: return "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
            case 4: return "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
            default: return "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
        }
    }
    
    private String getSuperInvokerDesc(String methodDesc, int arity) {
        if (arity <= 4) {
            StringBuilder sb = new StringBuilder("(Ljava/lang/Object;");
            for (int i = 0; i < arity; i++) {
                sb.append("Ljava/lang/Object;");
            }
            sb.append(")Ljava/lang/Object;");
            return sb.toString();
        } else {
            return "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
        }
    }
    
    private String getMetadataConstructorDesc(int arity) {
        String invokerType = getInvokerType(arity);
        if (arity == 0) {
            return "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;L" + invokerType + ";Ljava/util/List;)V";
        } else {
            return "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;L" + invokerType + ";Ljava/util/List;)V";
        }
    }
    
    private void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) mv.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) mv.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) mv.visitIntInsn(SIPUSH, value);
        else mv.visitLdcInsn(value);
    }
    
    private void box(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.BYTE:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case Type.INT:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case Type.VOID:
                mv.visitInsn(ACONST_NULL);
                break;
        }
    }
    
    private void unbox(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;
            case Type.BYTE:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                break;
            case Type.CHAR:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                break;
            case Type.SHORT:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                break;
            case Type.INT:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                break;
            case Type.LONG:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                break;
            case Type.FLOAT:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                break;
            case Type.DOUBLE:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                break;
        }
    }
    
    private void unboxReturn(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                mv.visitInsn(POP);
                mv.visitInsn(RETURN);
                break;
            case Type.BOOLEAN:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                mv.visitInsn(IRETURN);
                break;
            case Type.BYTE:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                mv.visitInsn(IRETURN);
                break;
            case Type.CHAR:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                mv.visitInsn(IRETURN);
                break;
            case Type.SHORT:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                mv.visitInsn(IRETURN);
                break;
            case Type.INT:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                mv.visitInsn(IRETURN);
                break;
            case Type.LONG:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                mv.visitInsn(LRETURN);
                break;
            case Type.FLOAT:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                mv.visitInsn(FRETURN);
                break;
            case Type.DOUBLE:
                mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                mv.visitInsn(DRETURN);
                break;
            default:
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                mv.visitInsn(ARETURN);
                break;
        }
    }
    
    // === Data classes (Java 17 records) ===
    
    public record MethodMeta(String name, String descriptor, List<String> interceptorBindings) {}
    
    public record ProxyMeta(String targetInternal, List<MethodMeta> methods, List<String> interceptorClasses) {}
}
