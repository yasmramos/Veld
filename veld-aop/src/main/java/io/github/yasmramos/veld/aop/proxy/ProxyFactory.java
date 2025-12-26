/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.aop.proxy;

import io.github.yasmramos.veld.aop.InterceptorRegistry;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import org.objectweb.asm.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.objectweb.asm.Opcodes.*;

/**
 * Factory for creating AOP proxy classes using optimized ASM bytecode generation.
 *
 * <p>Generates subclass proxies with fast-path/slow-path optimization:
 * <ul>
 *   <li>Fast-path: direct method call when no interceptors present</li>
 *   <li>Slow-path: interceptor chain invocation when interceptors exist</li>
 *   <li>Pre-computed MethodHandles for optimal performance</li>
 *   <li>GraalVM Native Image compatible</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class ProxyFactory {

    private static final ProxyFactory INSTANCE = new ProxyFactory();
    private static final AtomicLong PROXY_COUNTER = new AtomicLong(0);
    private static final String PROXY_SUFFIX = "$$VeldProxy$$";
    private static final String TARGET_FIELD = "$$target$$";
    private static final String INTERCEPTORS_FIELD = "$$interceptors$$";

    private final Map<Class<?>, ProxyClassInfo> proxyCache = new ConcurrentHashMap<>();
    private final ProxyClassLoader classLoader = new ProxyClassLoader();

    private ProxyFactory() {}

    /**
     * Returns the singleton instance.
     *
     * @return the factory instance
     */
    public static ProxyFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a proxy for the given target object.
     *
     * @param target the target object
     * @param <T>    the target type
     * @return the proxied instance
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T target) {
        Class<?> targetClass = target.getClass();

        // Check if we need a proxy
        if (!InterceptorRegistry.getInstance().hasAdvicesFor(targetClass)) {
            return target; // No advices, return original
        }

        try {
            ProxyClassInfo proxyInfo = getOrCreateProxyClass(targetClass);
            Constructor<?> constructor = proxyInfo.proxyClass.getConstructor(targetClass);
            T proxy = (T) constructor.newInstance(target);

            // Set interceptors into the proxy instance
            proxyInfo.setInterceptors(proxy);

            return proxy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy for " + targetClass.getName(), e);
        }
    }

    /**
     * Creates a proxy for a class (using default constructor).
     *
     * @param targetClass the target class
     * @param <T>         the target type
     * @return the proxied instance
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> targetClass) {
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            return createProxy(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy for " + targetClass.getName(), e);
        }
    }

    private ProxyClassInfo getOrCreateProxyClass(Class<?> targetClass) {
        return proxyCache.computeIfAbsent(targetClass, this::generateProxyClass);
    }

    /**
     * Generates a proxy class using ASM with optimized bytecode.
     */
    private ProxyClassInfo generateProxyClass(Class<?> targetClass) {
        String targetInternalName = Type.getInternalName(targetClass);
        String proxyClassName = targetClass.getName() + PROXY_SUFFIX + PROXY_COUNTER.incrementAndGet();
        String proxyInternalName = proxyClassName.replace('.', '/');

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Define the proxy class
        cw.visit(V17, ACC_PUBLIC | ACC_SUPER,
                proxyInternalName,
                null,
                targetInternalName,
                new String[]{Type.getInternalName(AopProxy.class)});

        // Add target field
        cw.visitField(ACC_PRIVATE | ACC_FINAL, TARGET_FIELD,
                Type.getDescriptor(targetClass), null, null).visitEnd();

        // Add interceptors field
        cw.visitField(ACC_PRIVATE | ACC_FINAL, INTERCEPTORS_FIELD,
                "[Ljava/lang/invoke/MethodHandle;", null, null).visitEnd();

        // Generate constructor
        generateConstructor(cw, targetClass, proxyInternalName, targetInternalName);

        // Generate AopProxy interface methods
        generateAopProxyMethods(cw, targetClass, proxyInternalName);

        // Generate optimized proxy methods
        List<Method> proxiedMethods = collectProxiedMethods(targetClass);
        for (int i = 0; i < proxiedMethods.size(); i++) {
            Method method = proxiedMethods.get(i);
            generateOptimizedProxyMethod(cw, method, targetClass, proxyInternalName, targetInternalName, i);
        }

        cw.visitEnd();
        byte[] bytecode = cw.toByteArray();

        Class<?> proxyClass = classLoader.defineClass(proxyClassName, bytecode);
        List<MethodHandle> methodHandles = buildMethodHandles(targetClass, proxiedMethods);

        return new ProxyClassInfo(proxyClass, methodHandles, proxiedMethods);
    }

    /**
     * Collects all methods that should be proxied.
     */
    private List<Method> collectProxiedMethods(Class<?> targetClass) {
        List<Method> methods = new ArrayList<>();
        for (Method method : targetClass.getMethods()) {
            if (shouldProxy(method)) {
                methods.add(method);
            }
        }
        return methods;
    }

    /**
     * Determines whether a method should be proxied.
     */
    private boolean shouldProxy(Method method) {
        int mods = method.getModifiers();

        // Skip static, final, private methods
        if (Modifier.isStatic(mods) || Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
            return false;
        }

        // Skip Object methods (except toString, equals, hashCode)
        if (method.getDeclaringClass() == Object.class) {
            String name = method.getName();
            return "toString".equals(name) || "equals".equals(name) || "hashCode".equals(name);
        }

        // Skip synthetic and bridge methods
        if (method.isSynthetic() || method.isBridge()) {
            return false;
        }

        return true;
    }

    /**
     * Generates the constructor for the proxy class.
     */
    private void generateConstructor(ClassWriter cw, Class<?> targetClass,
                                      String proxyInternalName, String targetInternalName) {
        String targetDescriptor = Type.getDescriptor(targetClass);

        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
        Constructor<?> targetConstructor = findCompatibleConstructor(constructors);
        String constructorDesc = Type.getConstructorDescriptor(targetConstructor);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", constructorDesc, null, null);
        mv.visitCode();

        // Call super()
        mv.visitVarInsn(ALOAD, 0);
        loadConstructorArgs(mv, targetConstructor, 1);
        mv.visitMethodInsn(INVOKESPECIAL, targetInternalName, "<init>", constructorDesc, false);

        // Initialize target field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitFieldInsn(PUTFIELD, proxyInternalName, TARGET_FIELD, targetDescriptor);

        // Initialize interceptors field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitFieldInsn(PUTFIELD, proxyInternalName, INTERCEPTORS_FIELD, "[Ljava/lang/invoke/MethodHandle;");

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Finds a constructor compatible with proxy instantiation.
     */
    private Constructor<?> findCompatibleConstructor(Constructor<?>[] constructors) {
        for (Constructor<?> c : constructors) {
            if (c.getParameterCount() == 0) {
                return c;
            }
        }
        return constructors[0];
    }

    /**
     * Loads constructor arguments onto the stack.
     */
    private void loadConstructorArgs(MethodVisitor mv, Constructor<?> constructor, int startLocal) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        int localIndex = startLocal;

        for (Class<?> paramType : paramTypes) {
            loadType(mv, paramType, localIndex);
            localIndex += getSlotSize(paramType);
        }
    }

    /**
     * Generates an optimized proxy method with fast-path/slow-path.
     */
    private void generateOptimizedProxyMethod(ClassWriter cw, Method method,
                                                Class<?> targetClass, String proxyInternalName,
                                                String targetInternalName, int methodIndex) {
        String methodDescriptor = Type.getMethodDescriptor(method);
        String[] exceptions = generateExceptionArray(method);
        Class<?> returnType = method.getReturnType();
        Class<?>[] paramTypes = method.getParameterTypes();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, method.getName(),
                methodDescriptor, null, exceptions);
        mv.visitCode();

        // Load this reference
        mv.visitVarInsn(ALOAD, 0);

        // Check if interceptors array is null (no interceptors case)
        Label noInterceptors = new Label();
        mv.visitFieldInsn(GETFIELD, proxyInternalName, INTERCEPTORS_FIELD, "[Ljava/lang/invoke/MethodHandle;");
        mv.visitInsn(ACONST_NULL);
        mv.visitJumpInsn(IF_ACMPNE, noInterceptors);

        // Fast-path: no interceptors, call target directly
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyInternalName, TARGET_FIELD, Type.getDescriptor(targetClass));

        // Load arguments directly
        int localIndex = 1;
        for (Class<?> paramType : paramTypes) {
            loadType(mv, paramType, localIndex);
            localIndex += getSlotSize(paramType);
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, targetInternalName, method.getName(), methodDescriptor, false);

        // Handle return
        generateReturn(mv, returnType);
        Label endLabel = new Label();
        mv.visitJumpInsn(GOTO, endLabel);

        // Slow-path: call interceptor chain
        mv.visitLabel(noInterceptors);

        // Prepare arguments array
        mv.visitIntInsn(BIPUSH, paramTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

        localIndex = 1;
        for (int i = 0; i < paramTypes.length; i++) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            boxAndStore(mv, paramTypes[i], localIndex);
            mv.visitInsn(AASTORE);
            localIndex += getSlotSize(paramTypes[i]);
        }

        // Call optimized handler with method index
        mv.visitIntInsn(BIPUSH, methodIndex);
        mv.visitMethodInsn(INVOKESTATIC,
                Type.getInternalName(ProxyMethodHandler.class),
                "invokeOptimized",
                "(ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                false);

        // Unbox return value
        if (returnType != void.class) {
            unboxAndReturn(mv, returnType);
        } else {
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
        }

        mv.visitLabel(endLabel);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generates AopProxy interface methods.
     */
    private void generateAopProxyMethods(ClassWriter cw, Class<?> targetClass, String proxyInternalName) {
        String targetDescriptor = Type.getDescriptor(targetClass);

        // getTargetObject()
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getTargetObject",
                "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyInternalName, TARGET_FIELD, targetDescriptor);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // getTargetClass()
        mv = cw.visitMethod(ACC_PUBLIC, "getTargetClass",
                "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getType(targetClass));
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Builds MethodHandles for all proxied methods at generation time.
     */
    private List<MethodHandle> buildMethodHandles(Class<?> targetClass, List<Method> methods) {
        List<MethodHandle> handles = new ArrayList<>(methods.size());
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Method method : methods) {
            try {
                MethodHandle handle = lookup.unreflect(method);
                handles.add(handle.asType(handle.type().changeParameterType(0, Object.class)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to create MethodHandle for " + method, e);
            }
        }

        return handles;
    }

    /**
     * Loads a local variable of the specified type onto the stack.
     */
    private void loadType(MethodVisitor mv, Class<?> type, int localIndex) {
        if (type == int.class || type == boolean.class || type == byte.class ||
            type == char.class || type == short.class) {
            mv.visitVarInsn(ILOAD, localIndex);
        } else if (type == long.class) {
            mv.visitVarInsn(LLOAD, localIndex);
        } else if (type == double.class) {
            mv.visitVarInsn(DLOAD, localIndex);
        } else if (type == float.class) {
            mv.visitVarInsn(FLOAD, localIndex);
        } else {
            mv.visitVarInsn(ALOAD, localIndex);
        }
    }

    /**
     * Boxes a primitive value and stores it in an array.
     */
    private void boxAndStore(MethodVisitor mv, Class<?> type, int localIndex) {
        if (type == int.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
                    "(I)Ljava/lang/Integer;", false);
        } else if (type == long.class) {
            mv.visitVarInsn(LLOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf",
                    "(J)Ljava/lang/Long;", false);
        } else if (type == double.class) {
            mv.visitVarInsn(DLOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf",
                    "(D)Ljava/lang/Double;", false);
        } else if (type == float.class) {
            mv.visitVarInsn(FLOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf",
                    "(F)Ljava/lang/Float;", false);
        } else if (type == boolean.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
                    "(Z)Ljava/lang/Boolean;", false);
        } else if (type == byte.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf",
                    "(B)Ljava/lang/Byte;", false);
        } else if (type == char.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf",
                    "(C)Ljava/lang/Character;", false);
        } else if (type == short.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf",
                    "(S)Ljava/lang/Short;", false);
        } else {
            mv.visitVarInsn(ALOAD, localIndex);
        }
    }

    /**
     * Generates the return instruction for the specified type.
     */
    private void generateReturn(MethodVisitor mv, Class<?> type) {
        if (type == void.class) {
            mv.visitInsn(RETURN);
        } else if (type == int.class || type == boolean.class || type == byte.class ||
                   type == char.class || type == short.class) {
            mv.visitInsn(IRETURN);
        } else if (type == long.class) {
            mv.visitInsn(LRETURN);
        } else if (type == double.class) {
            mv.visitInsn(DRETURN);
        } else if (type == float.class) {
            mv.visitInsn(FRETURN);
        } else {
            mv.visitInsn(ARETURN);
        }
    }

    /**
     * Unboxes a return value and generates the appropriate return instruction.
     */
    private void unboxAndReturn(MethodVisitor mv, Class<?> type) {
        if (type.isPrimitive() && type != void.class) {
            String wrapperClass = getWrapperClassName(type);
            String unboxMethod = getUnboxMethod(type);
            mv.visitTypeInsn(CHECKCAST, wrapperClass);
            mv.visitMethodInsn(INVOKEVIRTUAL, wrapperClass, unboxMethod,
                    "()" + getTypeDescriptor(type), false);
            generateReturn(mv, type);
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(type));
            mv.visitInsn(ARETURN);
        }
    }

    /**
     * Returns the JVM type descriptor for a class.
     */
    private String getTypeDescriptor(Class<?> type) {
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == double.class) return "D";
        if (type == float.class) return "F";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == void.class) return "V";
        return Type.getDescriptor(type);
    }

    /**
     * Returns the wrapper class name for a primitive type.
     */
    private String getWrapperClassName(Class<?> type) {
        if (type == int.class) return "java/lang/Integer";
        if (type == long.class) return "java/lang/Long";
        if (type == double.class) return "java/lang/Double";
        if (type == float.class) return "java/lang/Float";
        if (type == boolean.class) return "java/lang/Boolean";
        if (type == byte.class) return "java/lang/Byte";
        if (type == char.class) return "java/lang/Character";
        if (type == short.class) return "java/lang/Short";
        throw new IllegalArgumentException("Not a primitive type: " + type);
    }

    /**
     * Returns the unbox method name for a primitive type.
     */
    private String getUnboxMethod(Class<?> type) {
        if (type == int.class) return "intValue";
        if (type == long.class) return "longValue";
        if (type == double.class) return "doubleValue";
        if (type == float.class) return "floatValue";
        if (type == boolean.class) return "booleanValue";
        if (type == byte.class) return "byteValue";
        if (type == char.class) return "charValue";
        if (type == short.class) return "shortValue";
        throw new IllegalArgumentException("Not a primitive type: " + type);
    }

    /**
     * Returns the JVM slot size for a type.
     */
    private int getSlotSize(Class<?> type) {
        if (type == long.class || type == double.class) {
            return 2;
        }
        return 1;
    }

    /**
     * Generates an array of internal names for method exceptions.
     */
    private String[] generateExceptionArray(Method method) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        String[] exceptions = new String[exceptionTypes.length];
        for (int i = 0; i < exceptionTypes.length; i++) {
            exceptions[i] = Type.getInternalName(exceptionTypes[i]);
        }
        return exceptions;
    }

    /**
     * Custom class loader for proxy classes.
     */
    private static class ProxyClassLoader extends ClassLoader {
        ProxyClassLoader() {
            super(ProxyFactory.class.getClassLoader());
        }

        Class<?> defineClass(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }

    /**
     * Holds information about a generated proxy class.
     */
    private static class ProxyClassInfo {
        final Class<?> proxyClass;
        final List<MethodHandle> methodHandles;
        final List<Method> methods;

        ProxyClassInfo(Class<?> proxyClass, List<MethodHandle> methodHandles, List<Method> methods) {
            this.proxyClass = proxyClass;
            this.methodHandles = methodHandles;
            this.methods = methods;
        }

        @SuppressWarnings("unchecked")
        void setInterceptors(Object proxy) {
            try {
                java.lang.reflect.Field interceptorsField = proxy.getClass().getDeclaredField(INTERCEPTORS_FIELD);
                interceptorsField.setAccessible(true);

                // Collect all interceptors for methods
                List<MethodInterceptor> interceptors = new ArrayList<>();
                for (Method method : methods) {
                    List<MethodInterceptor> methodInterceptors = InterceptorRegistry.getInstance().getInterceptors(method);
                    interceptors.addAll(methodInterceptors);
                }

                if (!interceptors.isEmpty()) {
                    MethodHandle[] handles = new MethodHandle[interceptors.size()];
                    MethodHandles.Lookup lookup = MethodHandles.lookup();

                    for (int i = 0; i < interceptors.size(); i++) {
                        MethodInterceptor interceptor = interceptors.get(i);
                        handles[i] = lookup.bind(interceptor, "invoke",
                                java.lang.invoke.MethodType.methodType(Object.class, io.github.yasmramos.veld.aop.InvocationContext.class));
                    }

                    interceptorsField.set(proxy, handles);
                }
            } catch (Exception e) {
                // Silently fail - interceptors will be looked up at runtime
            }
        }
    }
}
