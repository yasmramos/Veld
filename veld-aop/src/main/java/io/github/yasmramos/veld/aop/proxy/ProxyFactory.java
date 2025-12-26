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
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;
import io.github.yasmramos.veld.aop.MethodInvocation;

import org.objectweb.asm.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.objectweb.asm.Opcodes.*;

/**
 * Factory for creating AOP proxy classes using ASM bytecode generation.
 *
 * <p>Generates subclass proxies that intercept method calls and delegate
 * to the interceptor chain.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class ProxyFactory {

    private static final ProxyFactory INSTANCE = new ProxyFactory();
    private static final AtomicLong PROXY_COUNTER = new AtomicLong(0);
    private static final String PROXY_SUFFIX = "$$VeldProxy$$";

    private final Map<Class<?>, Class<?>> proxyCache = new ConcurrentHashMap<>();
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
            Class<?> proxyClass = getOrCreateProxyClass(targetClass);
            Constructor<?> constructor = proxyClass.getConstructor(targetClass);
            return (T) constructor.newInstance(target);
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

    private Class<?> getOrCreateProxyClass(Class<?> targetClass) {
        return proxyCache.computeIfAbsent(targetClass, this::generateProxyClass);
    }

    /**
     * Generates a proxy class using ASM.
     */
    private Class<?> generateProxyClass(Class<?> targetClass) {
        String targetInternalName = Type.getInternalName(targetClass);
        String proxyClassName = targetClass.getName() + PROXY_SUFFIX + PROXY_COUNTER.incrementAndGet();
        String proxyInternalName = proxyClassName.replace('.', '/');

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Define the proxy class
        cw.visit(V11, ACC_PUBLIC | ACC_SUPER,
                proxyInternalName,
                null,
                targetInternalName,
                new String[]{Type.getInternalName(AopProxy.class)});

        // Add field for target
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "$$target$$", 
                Type.getDescriptor(targetClass), null, null).visitEnd();

        // Generate constructor
        generateConstructor(cw, targetClass, proxyInternalName, targetInternalName);

        // Generate AopProxy interface methods
        generateAopProxyMethods(cw, targetClass, proxyInternalName);

        // Generate proxy methods for all public methods
        for (Method method : targetClass.getMethods()) {
            if (shouldProxy(method)) {
                generateProxyMethod(cw, method, proxyInternalName, targetInternalName);
            }
        }

        cw.visitEnd();
        byte[] bytecode = cw.toByteArray();

        // Debug: Print bytecode info
        System.out.println("[AOP] Generated proxy class: " + proxyClassName + 
                " (" + bytecode.length + " bytes)");

        return classLoader.defineClass(proxyClassName, bytecode);
    }

    private void generateConstructor(ClassWriter cw, Class<?> targetClass,
                                      String proxyInternalName, String targetInternalName) {
        String targetDescriptor = Type.getDescriptor(targetClass);
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>",
                "(" + targetDescriptor + ")V", null, null);
        mv.visitCode();

        // Call super()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, targetInternalName, "<init>", "()V", false);

        // Store target
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, proxyInternalName, "$$target$$", targetDescriptor);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void generateAopProxyMethods(ClassWriter cw, Class<?> targetClass,
                                          String proxyInternalName) {
        String targetDescriptor = Type.getDescriptor(targetClass);

        // getTargetObject()
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getTargetObject",
                "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyInternalName, "$$target$$", targetDescriptor);
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

    private void generateProxyMethod(ClassWriter cw, Method method,
                                      String proxyInternalName, String targetInternalName) {
        String methodDescriptor = Type.getMethodDescriptor(method);
        String[] exceptions = new String[method.getExceptionTypes().length];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i] = Type.getInternalName(method.getExceptionTypes()[i]);
        }

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                method.getName(),
                methodDescriptor,
                null,
                exceptions);
        mv.visitCode();

        // Generate code that invokes ProxyMethodHandler.invoke()
        // This is the runtime delegation point

        // Load this (proxy)
        mv.visitVarInsn(ALOAD, 0);
        
        // Load target field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyInternalName, "$$target$$",
                Type.getDescriptor(method.getDeclaringClass()));

        // Load method name
        mv.visitLdcInsn(method.getName());

        // Load method descriptor
        mv.visitLdcInsn(methodDescriptor);

        // Create args array
        Class<?>[] paramTypes = method.getParameterTypes();
        mv.visitIntInsn(BIPUSH, paramTypes.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

        int localIndex = 1;
        for (int i = 0; i < paramTypes.length; i++) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            localIndex = loadAndBox(mv, paramTypes[i], localIndex);
            mv.visitInsn(AASTORE);
        }

        // Call ProxyMethodHandler.invoke(target, methodName, descriptor, args)
        mv.visitMethodInsn(INVOKESTATIC,
                Type.getInternalName(ProxyMethodHandler.class),
                "invoke",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                false);

        // Unbox return value if necessary
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
        } else if (returnType.isPrimitive()) {
            unbox(mv, returnType);
            mv.visitInsn(getReturnOpcode(returnType));
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
            mv.visitInsn(ARETURN);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private int loadAndBox(MethodVisitor mv, Class<?> type, int localIndex) {
        if (type == int.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
                    "(I)Ljava/lang/Integer;", false);
            return localIndex + 1;
        } else if (type == long.class) {
            mv.visitVarInsn(LLOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf",
                    "(J)Ljava/lang/Long;", false);
            return localIndex + 2;
        } else if (type == double.class) {
            mv.visitVarInsn(DLOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf",
                    "(D)Ljava/lang/Double;", false);
            return localIndex + 2;
        } else if (type == float.class) {
            mv.visitVarInsn(FLOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf",
                    "(F)Ljava/lang/Float;", false);
            return localIndex + 1;
        } else if (type == boolean.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
                    "(Z)Ljava/lang/Boolean;", false);
            return localIndex + 1;
        } else if (type == byte.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf",
                    "(B)Ljava/lang/Byte;", false);
            return localIndex + 1;
        } else if (type == char.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf",
                    "(C)Ljava/lang/Character;", false);
            return localIndex + 1;
        } else if (type == short.class) {
            mv.visitVarInsn(ILOAD, localIndex);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf",
                    "(S)Ljava/lang/Short;", false);
            return localIndex + 1;
        } else {
            mv.visitVarInsn(ALOAD, localIndex);
            return localIndex + 1;
        }
    }

    private void unbox(MethodVisitor mv, Class<?> type) {
        if (type == int.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        } else if (type == long.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        } else if (type == double.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        } else if (type == float.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
        } else if (type == boolean.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        } else if (type == byte.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
        } else if (type == char.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
        } else if (type == short.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
        }
    }

    private int getReturnOpcode(Class<?> type) {
        if (type == int.class || type == boolean.class || type == byte.class ||
            type == char.class || type == short.class) {
            return IRETURN;
        } else if (type == long.class) {
            return LRETURN;
        } else if (type == float.class) {
            return FRETURN;
        } else if (type == double.class) {
            return DRETURN;
        } else {
            return ARETURN;
        }
    }

    private boolean shouldProxy(Method method) {
        int mods = method.getModifiers();
        
        // Skip static, final, private methods
        if (Modifier.isStatic(mods) || Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
            return false;
        }

        // Skip Object methods
        if (method.getDeclaringClass() == Object.class) {
            return false;
        }

        // Skip synthetic and bridge methods
        if (method.isSynthetic() || method.isBridge()) {
            return false;
        }

        return true;
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
}
