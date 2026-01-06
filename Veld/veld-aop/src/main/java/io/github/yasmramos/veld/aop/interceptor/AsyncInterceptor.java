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
package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.aop.CompileTimeInterceptor;
import io.github.yasmramos.veld.runtime.async.AsyncExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Compile-time interceptor for @Async methods.
 *
 * <p>Wraps method execution in an async task submitted to the executor.
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
public class AsyncInterceptor implements CompileTimeInterceptor {

    /** Private constructor to prevent instantiation. */
    private AsyncInterceptor() {}

    @Override
    public void beforeMethod(String methodName, Object[] args) {
        // No-op: async execution is handled by wrapAsync
    }
    
    @Override
    public void afterMethod(String methodName, Object result) {
        // No-op
    }
    
    @Override
    public void afterThrowing(String methodName, Throwable ex) {
        System.err.println("[Veld] Async method failed: " + methodName + " - " + ex.getMessage());
    }
    
    /**
     * Wraps method execution for async processing.
     * Called by generated AOP wrapper code.
     *
     * @param target the target object
     * @param methodName the method name
     * @param args the method arguments
     * @param executorName the executor name (empty for default)
     * @param directInvoker the direct method invoker
     * @return CompletableFuture for the result, or null for void methods
     */
    public static Object wrapAsync(Object target, String methodName, Object[] args,
                                   String executorName, DirectInvoker directInvoker) {
        Class<?> returnType = directInvoker.getReturnType();
        
        if (returnType == void.class || returnType == Void.class) {
            // Fire and forget
            AsyncExecutor.getInstance().submit(() -> {
                try {
                    directInvoker.invoke();
                } catch (Exception e) {
                    System.err.println("[Veld] Async method failed: " + methodName + " - " + e.getMessage());
                }
            }, executorName);
            return null;
        } else if (CompletableFuture.class.isAssignableFrom(returnType)) {
            // Return CompletableFuture
            return AsyncExecutor.getInstance().submit(() -> {
                try {
                    CompletableFuture<?> result = (CompletableFuture<?>) directInvoker.invoke();
                    return result != null ? result.join() : null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorName);
        } else if (Future.class.isAssignableFrom(returnType)) {
            // Return Future wrapped in CompletableFuture
            return AsyncExecutor.getInstance().submit(() -> {
                try {
                    return directInvoker.invoke();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorName);
        } else {
            // Non-void, non-Future return type - wrap in CompletableFuture
            return AsyncExecutor.getInstance().submit(() -> {
                try {
                    return directInvoker.invoke();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorName);
        }
    }
    
    /**
     * Functional interface for direct method invocation.
     */
    @FunctionalInterface
    public interface DirectInvoker {
        Object invoke() throws Exception;
        
        default Class<?> getReturnType() {
            return Object.class;
        }
    }
}
