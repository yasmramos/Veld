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
package io.github.yasmramos.veld.aop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zero-reflection implementation of {@link InvocationContext}.
 *
 * <p>Manages the interceptor chain and provides method invocation context
 * without using reflection for the actual method invocation.
 * 
 * <p><b>Zero-Reflection Principle:</b>
 * <ul>
 *   <li>Uses {@link DirectInvoker} for method invocation</li>
 *   <li>No {@code Method.invoke()} calls</li>
 *   <li>Method metadata provided as strings (pre-computed)</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class MethodInvocation implements InvocationContext {

    /**
     * Functional interface for direct method invocation without reflection.
     */
    @FunctionalInterface
    public interface DirectInvoker {
        Object invoke(Object target, Object[] args) throws Throwable;
    }

    private final Object target;
    private final String className;
    private final String methodName;
    private final String[] parameterTypes;
    private final String returnType;
    private final DirectInvoker invoker;
    private Object[] parameters;
    private Map<String, Object> contextData;
    private final List<MethodInterceptor> interceptors;
    private int interceptorIndex;
    private Object currentInterceptor;
    
    // Cached values
    private String signature;
    private String shortString;
    
    // Legacy support - only populated when using deprecated constructor
    private Method legacyMethod;

    /**
     * Creates a new zero-reflection method invocation context.
     *
     * @param target         the target object
     * @param className      fully qualified class name
     * @param methodName     method name
     * @param parameterTypes parameter type names
     * @param returnType     return type name
     * @param invoker        direct invoker (no reflection)
     * @param parameters     method parameters
     * @param interceptors   the interceptor chain
     */
    public MethodInvocation(Object target, String className, String methodName,
                            String[] parameterTypes, String returnType,
                            DirectInvoker invoker, Object[] parameters,
                            List<MethodInterceptor> interceptors) {
        this.target = target;
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.invoker = invoker;
        this.parameters = (parameters == null || parameters.length == 0) 
            ? new Object[0] 
            : parameters;
        this.interceptors = interceptors;
        this.interceptorIndex = 0;
    }

    /**
     * Legacy constructor for backwards compatibility.
     * @deprecated Use the zero-reflection constructor instead.
     */
    @Deprecated
    public MethodInvocation(Object target, Method method, Object[] parameters,
                            List<MethodInterceptor> interceptors) {
        this.target = target;
        this.className = method.getDeclaringClass().getName();
        this.methodName = method.getName();
        
        Class<?>[] paramTypes = method.getParameterTypes();
        this.parameterTypes = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            this.parameterTypes[i] = paramTypes[i].getSimpleName();
        }
        this.returnType = method.getReturnType().getSimpleName();
        
        // Store method for legacy compatibility
        this.legacyMethod = method;
        
        // Create invoker that uses the method (reflection fallback)
        final Method m = method;
        this.invoker = (t, args) -> {
            try {
                m.setAccessible(true);
                return m.invoke(t, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        };
        
        this.parameters = (parameters == null || parameters.length == 0) 
            ? new Object[0] 
            : parameters;
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
        // End of chain - direct invocation (zero-reflection)
        return invoker.invoke(target, parameters);
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        // Returns legacy method if available (for backward compatibility)
        // Zero-reflection path returns null
        return legacyMethod;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

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
        if (params.length != parameterTypes.length) {
            throw new IllegalArgumentException(
                    "Expected " + parameterTypes.length +
                            " parameters, but got " + params.length);
        }
        this.parameters = params.clone();
    }

    @Override
    public Class<?> getDeclaringClass() {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    @Override
    public String getSignature() {
        if (signature == null) {
            StringBuilder sb = new StringBuilder(64);
            sb.append(returnType).append(" ");
            sb.append(className).append(".");
            sb.append(methodName).append("(");
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameterTypes[i]);
            }
            sb.append(")");
            signature = sb.toString();
        }
        return signature;
    }

    @Override
    public String toShortString() {
        if (shortString == null) {
            String simpleName = className;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                simpleName = className.substring(lastDot + 1);
            }
            // Handle inner classes - take only the last part after $
            int lastDollar = simpleName.lastIndexOf('$');
            if (lastDollar > 0) {
                simpleName = simpleName.substring(lastDollar + 1);
            }
            shortString = simpleName + "." + methodName + "()";
        }
        return shortString;
    }

    @Override
    public String toLongString() {
        return getSignature() + " with args " + Arrays.toString(parameters);
    }

    @Override
    public Map<String, Object> getContextData() {
        if (contextData == null) {
            contextData = new HashMap<>(4);
        }
        return contextData;
    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Object getInterceptor() {
        return currentInterceptor;
    }

    @Override
    public String toString() {
        return "MethodInvocation{" +
                "method=" + toShortString() +
                ", interceptorIndex=" + interceptorIndex +
                "/" + interceptors.size() +
                '}';
    }
}
