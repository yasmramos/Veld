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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero-reflection method handler for AOP proxies.
 *
 * <p>This class coordinates method invocations through the interceptor chain
 * without using reflection. All method dispatching is done through pre-registered
 * functional interfaces generated at compile-time.
 * 
 * <p><b>Zero-Reflection Principle:</b>
 * <ul>
 *   <li>No {@code Method.invoke()} calls in hot path</li>
 *   <li>All dispatching via functional interfaces</li>
 *   <li>Specialized invokers by arity (0-4 args) to avoid array allocation</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class ProxyMethodHandler {

    // ==================== Specialized Invokers by Arity ====================
    // These eliminate Object[] array creation for methods with 0-4 parameters
    
    /** Generic invoker for methods with N>4 parameters */
    @FunctionalInterface
    public interface DirectInvoker {
        Object invoke(Object target, Object[] args) throws Throwable;
    }
    
    /** Specialized invoker for 0-arg methods (no array allocation) */
    @FunctionalInterface
    public interface DirectInvoker0 {
        Object invoke(Object target) throws Throwable;
    }
    
    /** Specialized invoker for 1-arg methods (no array allocation) */
    @FunctionalInterface
    public interface DirectInvoker1 {
        Object invoke(Object target, Object arg0) throws Throwable;
    }
    
    /** Specialized invoker for 2-arg methods (no array allocation) */
    @FunctionalInterface
    public interface DirectInvoker2 {
        Object invoke(Object target, Object arg0, Object arg1) throws Throwable;
    }
    
    /** Specialized invoker for 3-arg methods (no array allocation) */
    @FunctionalInterface
    public interface DirectInvoker3 {
        Object invoke(Object target, Object arg0, Object arg1, Object arg2) throws Throwable;
    }
    
    /** Specialized invoker for 4-arg methods (no array allocation) */
    @FunctionalInterface
    public interface DirectInvoker4 {
        Object invoke(Object target, Object arg0, Object arg1, Object arg2, Object arg3) throws Throwable;
    }

    /**
     * Holder for pre-computed method metadata (generated at compile-time).
     * Supports specialized invokers by arity for optimal performance.
     */
    public static class MethodMetadata {
        private final String className;
        private final String methodName;
        private final String[] parameterTypes;
        private final String returnType;
        private final int arity;
        private final List<MethodInterceptor> interceptors;
        
        // Only one of these will be non-null based on arity
        private final DirectInvoker invoker;
        private final DirectInvoker0 invoker0;
        private final DirectInvoker1 invoker1;
        private final DirectInvoker2 invoker2;
        private final DirectInvoker3 invoker3;
        private final DirectInvoker4 invoker4;

        // Constructor for generic invoker (N>4 args or legacy)
        public MethodMetadata(String className, String methodName, 
                             String[] parameterTypes, String returnType,
                             DirectInvoker invoker, List<MethodInterceptor> interceptors) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.arity = parameterTypes.length;
            this.invoker = invoker;
            this.invoker0 = null;
            this.invoker1 = null;
            this.invoker2 = null;
            this.invoker3 = null;
            this.invoker4 = null;
            this.interceptors = interceptors;
        }
        
        // Constructor for 0-arg methods
        public MethodMetadata(String className, String methodName, 
                             String returnType, DirectInvoker0 invoker, 
                             List<MethodInterceptor> interceptors) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = new String[0];
            this.returnType = returnType;
            this.arity = 0;
            this.invoker = null;
            this.invoker0 = invoker;
            this.invoker1 = null;
            this.invoker2 = null;
            this.invoker3 = null;
            this.invoker4 = null;
            this.interceptors = interceptors;
        }
        
        // Constructor for 1-arg methods
        public MethodMetadata(String className, String methodName, 
                             String[] parameterTypes, String returnType,
                             DirectInvoker1 invoker, List<MethodInterceptor> interceptors) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.arity = 1;
            this.invoker = null;
            this.invoker0 = null;
            this.invoker1 = invoker;
            this.invoker2 = null;
            this.invoker3 = null;
            this.invoker4 = null;
            this.interceptors = interceptors;
        }
        
        // Constructor for 2-arg methods
        public MethodMetadata(String className, String methodName, 
                             String[] parameterTypes, String returnType,
                             DirectInvoker2 invoker, List<MethodInterceptor> interceptors) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.arity = 2;
            this.invoker = null;
            this.invoker0 = null;
            this.invoker1 = null;
            this.invoker2 = invoker;
            this.invoker3 = null;
            this.invoker4 = null;
            this.interceptors = interceptors;
        }
        
        // Constructor for 3-arg methods
        public MethodMetadata(String className, String methodName, 
                             String[] parameterTypes, String returnType,
                             DirectInvoker3 invoker, List<MethodInterceptor> interceptors) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.arity = 3;
            this.invoker = null;
            this.invoker0 = null;
            this.invoker1 = null;
            this.invoker2 = null;
            this.invoker3 = invoker;
            this.invoker4 = null;
            this.interceptors = interceptors;
        }
        
        // Constructor for 4-arg methods
        public MethodMetadata(String className, String methodName, 
                             String[] parameterTypes, String returnType,
                             DirectInvoker4 invoker, List<MethodInterceptor> interceptors) {
            this.className = className;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
            this.arity = 4;
            this.invoker = null;
            this.invoker0 = null;
            this.invoker1 = null;
            this.invoker2 = null;
            this.invoker3 = null;
            this.invoker4 = invoker;
            this.interceptors = interceptors;
        }

        public String getClassName() { return className; }
        public String getMethodName() { return methodName; }
        public String[] getParameterTypes() { return parameterTypes; }
        public String getReturnType() { return returnType; }
        public int getArity() { return arity; }
        public List<MethodInterceptor> getInterceptors() { return interceptors; }
        
        // Legacy getter - wraps specialized invoker if needed
        public DirectInvoker getInvoker() { 
            if (invoker != null) return invoker;
            // Wrap specialized invoker for backwards compatibility
            return (target, args) -> invokeDirect(target, args);
        }
        
        /**
         * Invokes the method using the optimal specialized invoker.
         * Avoids array creation for 0-4 arg methods.
         */
        public Object invokeDirect(Object target, Object[] args) throws Throwable {
            switch (arity) {
                case 0: return invoker0.invoke(target);
                case 1: return invoker1.invoke(target, args[0]);
                case 2: return invoker2.invoke(target, args[0], args[1]);
                case 3: return invoker3.invoke(target, args[0], args[1], args[2]);
                case 4: return invoker4.invoke(target, args[0], args[1], args[2], args[3]);
                default: return invoker.invoke(target, args);
            }
        }
        
        public String getSignature() {
            StringBuilder sb = new StringBuilder(returnType).append(" ")
                .append(className).append(".").append(methodName).append("(");
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameterTypes[i]);
            }
            return sb.append(")").toString();
        }
    }

    /** Registry of method metadata (populated at startup by generated code) */
    private static final Map<String, MethodMetadata> methodRegistry = new ConcurrentHashMap<>();
    
    /** Cache for Method objects (legacy support) */
    private static final Map<String, Method> legacyMethodCache = new ConcurrentHashMap<>();

    /**
     * Registers method metadata (called by generated code at startup).
     */
    public static void registerMethod(String key, MethodMetadata metadata) {
        methodRegistry.put(key, metadata);
    }

    /**
     * Invokes a method through the interceptor chain.
     * Zero-reflection: uses pre-registered DirectInvoker.
     */
    public static Object invoke(Object target, String methodKey, Object[] args) throws Throwable {
        MethodMetadata metadata = methodRegistry.get(methodKey);
        
        if (metadata == null) {
            throw new IllegalStateException("Method not registered: " + methodKey + 
                ". Ensure generated code has been initialized.");
        }

        List<MethodInterceptor> interceptors = metadata.getInterceptors();
        
        if (interceptors == null || interceptors.isEmpty()) {
            // No interceptors - direct invocation (fastest path)
            return metadata.invokeDirect(target, args);
        }

        // Create zero-reflection invocation context
        DirectInvocationContext ctx = new DirectInvocationContext(target, metadata, args, interceptors);
        return ctx.proceed();
    }

    /**
     * Zero-reflection invocation context.
     */
    private static class DirectInvocationContext implements InvocationContext {
        private final Object target;
        private final MethodMetadata metadata;
        private Object[] parameters;
        private final List<MethodInterceptor> interceptors;
        private int interceptorIndex;
        private Object currentInterceptor;
        private Map<String, Object> contextData;

        DirectInvocationContext(Object target, MethodMetadata metadata,
                               Object[] parameters, List<MethodInterceptor> interceptors) {
            this.target = target;
            this.metadata = metadata;
            this.parameters = parameters != null ? parameters : new Object[0];
            this.interceptors = interceptors;
            this.interceptorIndex = 0;
        }

        @Override
        public Object proceed() throws Throwable {
            if (interceptorIndex < interceptors.size()) {
                MethodInterceptor interceptor = interceptors.get(interceptorIndex++);
                currentInterceptor = interceptor;
                return interceptor.invoke(this);
            }
            return metadata.invokeDirect(target, parameters);
        }

        @Override
        public Object getTarget() { return target; }

        @Override
        public Method getMethod() { return null; }

        @Override
        public String getMethodName() { return metadata.getMethodName(); }

        @Override
        public Object[] getArgs() { 
            return parameters.length == 0 ? parameters : parameters.clone(); 
        }

        @Override
        public Object[] getParameters() { 
            return parameters.length == 0 ? parameters : parameters.clone(); 
        }

        @Override
        public void setParameters(Object[] params) {
            if (params == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }
            if (params.length != metadata.getParameterTypes().length) {
                throw new IllegalArgumentException(
                    "Expected " + metadata.getParameterTypes().length +
                    " parameters, but got " + params.length);
            }
            this.parameters = params.clone();
        }

        @Override
        public Class<?> getDeclaringClass() {
            try {
                return Class.forName(metadata.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getSignature() { return metadata.getSignature(); }

        @Override
        public String toShortString() {
            String simpleName = metadata.getClassName();
            int lastDot = simpleName.lastIndexOf('.');
            if (lastDot > 0) simpleName = simpleName.substring(lastDot + 1);
            int lastDollar = simpleName.lastIndexOf('$');
            if (lastDollar > 0) simpleName = simpleName.substring(lastDollar + 1);
            return simpleName + "." + metadata.getMethodName() + "()";
        }

        @Override
        public String toLongString() {
            return getSignature() + " with args " + java.util.Arrays.toString(parameters);
        }

        @Override
        public Map<String, Object> getContextData() {
            if (contextData == null) {
                contextData = new java.util.HashMap<>(4);
            }
            return contextData;
        }

        @Override
        public Object getTimer() { return null; }

        @Override
        public Object getInterceptor() { return currentInterceptor; }
    }

    public static void clearRegistry() {
        methodRegistry.clear();
    }
    
    public static int getRegisteredMethodCount() {
        return methodRegistry.size();
    }
    
    // ==================== Legacy API (backwards compatibility) ====================
    
    /**
     * Legacy invoke method for backwards compatibility.
     * @deprecated Use pre-registered DirectInvokers instead
     */
    @Deprecated
    public static Object invoke(Object target, String methodName,
                                 String methodDescriptor, Object[] args) throws Throwable {
        String cacheKey = target.getClass().getName() + "#" + methodName + "#" + methodDescriptor;
        
        MethodMetadata metadata = methodRegistry.get(cacheKey);
        if (metadata != null) {
            return invoke(target, cacheKey, args);
        }
        
        Method method = legacyMethodCache.computeIfAbsent(cacheKey, key -> {
            Method m = findMethodLegacy(target.getClass(), methodName, methodDescriptor);
            m.setAccessible(true);
            return m;
        });
        
        List<MethodInterceptor> interceptors = InterceptorRegistry.getInstance()
                .getInterceptors(method);

        if (interceptors == null || interceptors.isEmpty()) {
            return method.invoke(target, args);
        }

        MethodInvocation invocation = new MethodInvocation(target, method, args, interceptors);
        return invocation.proceed();
    }
    
    private static Method findMethodLegacy(Class<?> targetClass, String methodName, String descriptor) {
        Class<?>[] paramTypes = parseDescriptor(descriptor);
        try {
            return targetClass.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            try {
                Method m = targetClass.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException("Method not found: " + methodName, e2);
            }
        }
    }
    
    private static Class<?>[] parseDescriptor(String descriptor) {
        if (descriptor.startsWith("()")) {
            return new Class<?>[0];
        }
        
        List<Class<?>> params = new ArrayList<>(4);
        int i = 1;
        
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            char c = descriptor.charAt(i);
            
            if (c == 'L') {
                int end = descriptor.indexOf(';', i);
                String className = descriptor.substring(i + 1, end).replace('/', '.');
                params.add(loadClass(className));
                i = end + 1;
            } else if (c == '[') {
                int start = i;
                while (descriptor.charAt(i) == '[') i++;
                c = descriptor.charAt(i);
                if (c == 'L') {
                    int end = descriptor.indexOf(';', i);
                    params.add(loadClass(descriptor.substring(start, end + 1).replace('/', '.')));
                    i = end + 1;
                } else {
                    params.add(loadClass(descriptor.substring(start, i + 1).replace('/', '.')));
                    i++;
                }
            } else {
                params.add(getPrimitiveType(c));
                i++;
            }
        }
        
        return params.toArray(new Class<?>[0]);
    }
    
    private static Class<?> loadClass(String className) {
        try {
            return className.startsWith("[") ? Class.forName(className) : Class.forName(className);
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
