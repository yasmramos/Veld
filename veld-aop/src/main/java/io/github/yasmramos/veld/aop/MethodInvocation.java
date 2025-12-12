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
 * Default implementation of {@link InvocationContext}.
 *
 * <p>Manages the interceptor chain and provides method invocation context.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class MethodInvocation implements InvocationContext {

    private final Object target;
    private final Method method;
    private Object[] parameters;
    private final Map<String, Object> contextData;
    private final List<MethodInterceptor> interceptors;
    private int interceptorIndex;
    private Object currentInterceptor;

    /**
     * Creates a new method invocation context.
     *
     * @param target       the target object
     * @param method       the method being invoked
     * @param parameters   the method parameters
     * @param interceptors the interceptor chain
     */
    public MethodInvocation(Object target, Method method, Object[] parameters,
                            List<MethodInterceptor> interceptors) {
        this.target = target;
        this.method = method;
        this.parameters = parameters != null ? parameters.clone() : new Object[0];
        this.interceptors = interceptors;
        this.contextData = new HashMap<>();
        this.interceptorIndex = 0;
    }

    @Override
    public Object proceed() throws Throwable {
        if (interceptorIndex < interceptors.size()) {
            MethodInterceptor interceptor = interceptors.get(interceptorIndex++);
            currentInterceptor = interceptor;
            return interceptor.invoke(this);
        } else {
            // End of chain, invoke the actual method
            return invokeTarget();
        }
    }

    /**
     * Invokes the target method directly.
     *
     * @return the method return value
     * @throws Throwable if the method throws an exception
     */
    protected Object invokeTarget() throws Throwable {
        try {
            method.setAccessible(true);
            return method.invoke(target, parameters);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public String getMethodName() {
        return method.getName();
    }

    @Override
    public Object[] getArgs() {
        return parameters.clone();
    }

    @Override
    public Object[] getParameters() {
        return parameters.clone();
    }

    @Override
    public void setParameters(Object[] params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        if (params.length != method.getParameterCount()) {
            throw new IllegalArgumentException(
                    "Expected " + method.getParameterCount() +
                            " parameters, but got " + params.length);
        }
        this.parameters = params.clone();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    @Override
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType().getSimpleName()).append(" ");
        sb.append(method.getDeclaringClass().getName()).append(".");
        sb.append(method.getName()).append("(");
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i].getSimpleName());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toShortString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()";
    }

    @Override
    public String toLongString() {
        return getSignature() + " with args " + Arrays.toString(parameters);
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    @Override
    public Object getTimer() {
        return null; // Timer support can be added later
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
