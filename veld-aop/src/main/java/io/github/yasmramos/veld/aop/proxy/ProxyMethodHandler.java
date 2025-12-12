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
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles method invocations on proxy objects.
 *
 * <p>This class is called by generated proxy bytecode to delegate
 * to the interceptor chain.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class ProxyMethodHandler {

    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * Invokes a method through the interceptor chain.
     *
     * <p>This method is called from generated proxy code.
     *
     * @param target         the target object
     * @param methodName     the method name
     * @param methodDescriptor the method descriptor
     * @param args           the method arguments
     * @return the result of the invocation
     * @throws Throwable if the method or interceptor throws an exception
     */
    public static Object invoke(Object target, String methodName,
                                 String methodDescriptor, Object[] args) throws Throwable {
        // Find the method
        Method method = findMethod(target.getClass(), methodName, methodDescriptor);
        
        // Get interceptors
        List<MethodInterceptor> interceptors = InterceptorRegistry.getInstance()
                .getInterceptors(method);

        if (interceptors.isEmpty()) {
            // No interceptors, call directly
            method.setAccessible(true);
            return method.invoke(target, args);
        }

        // Create invocation context and proceed through chain
        MethodInvocation invocation = new MethodInvocation(target, method, args, interceptors);
        return invocation.proceed();
    }

    private static Method findMethod(Class<?> targetClass, String methodName,
                                      String descriptor) {
        String cacheKey = targetClass.getName() + "#" + methodName + descriptor;
        
        return methodCache.computeIfAbsent(cacheKey, key -> {
            Class<?>[] paramTypes = parseDescriptor(descriptor);
            try {
                return targetClass.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                // Try declared methods
                try {
                    Method method = targetClass.getDeclaredMethod(methodName, paramTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e2) {
                    throw new RuntimeException("Method not found: " + methodName, e2);
                }
            }
        });
    }

    /**
     * Parses a method descriptor to get parameter types.
     */
    private static Class<?>[] parseDescriptor(String descriptor) {
        // Format: (param1param2...)returnType
        // Examples: ()V, (I)V, (Ljava/lang/String;)I
        
        java.util.List<Class<?>> params = new java.util.ArrayList<>();
        int i = 1; // Skip opening '('
        
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            int start = i;
            char c = descriptor.charAt(i);
            
            if (c == 'L') {
                // Object type: Lpackage/Class;
                int end = descriptor.indexOf(';', i);
                String className = descriptor.substring(i + 1, end).replace('/', '.');
                params.add(loadClass(className));
                i = end + 1;
            } else if (c == '[') {
                // Array type
                int arrayDepth = 0;
                while (descriptor.charAt(i) == '[') {
                    arrayDepth++;
                    i++;
                }
                c = descriptor.charAt(i);
                if (c == 'L') {
                    int end = descriptor.indexOf(';', i);
                    String className = descriptor.substring(start, end + 1).replace('/', '.');
                    params.add(loadClass(toArrayClassName(descriptor.substring(start, end + 1))));
                    i = end + 1;
                } else {
                    // Primitive array
                    params.add(loadClass(toArrayClassName(descriptor.substring(start, i + 1))));
                    i++;
                }
            } else {
                // Primitive type
                params.add(getPrimitiveType(c));
                i++;
            }
        }
        
        return params.toArray(new Class<?>[0]);
    }

    private static String toArrayClassName(String descriptor) {
        return descriptor.replace('/', '.');
    }

    private static Class<?> loadClass(String className) {
        try {
            // Handle array notation
            if (className.startsWith("[")) {
                return Class.forName(className);
            }
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    private static Class<?> getPrimitiveType(char c) {
        switch (c) {
            case 'I': return int.class;
            case 'J': return long.class;
            case 'D': return double.class;
            case 'F': return float.class;
            case 'Z': return boolean.class;
            case 'B': return byte.class;
            case 'C': return char.class;
            case 'S': return short.class;
            case 'V': return void.class;
            default: throw new IllegalArgumentException("Unknown primitive: " + c);
        }
    }
}
