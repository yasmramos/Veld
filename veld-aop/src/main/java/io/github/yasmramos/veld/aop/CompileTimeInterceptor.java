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

/**
 * Interface for compile-time interceptors.
 * 
 * <p>Unlike {@link MethodInterceptor} which uses runtime proxies,
 * this interface is designed for code generation at compile-time.
 * The generated AOP wrapper classes call these methods directly.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class LoggingInterceptor implements CompileTimeInterceptor {
 *     @Override
 *     public void beforeMethod(String methodName, Object[] args) {
 *         System.out.println("Entering: " + methodName);
 *     }
 *     
 *     @Override
 *     public void afterMethod(String methodName, Object result) {
 *         System.out.println("Exiting: " + methodName);
 *     }
 *     
 *     @Override
 *     public void afterThrowing(String methodName, Throwable ex) {
 *         System.out.println("Exception in: " + methodName + " - " + ex);
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.4
 */
public interface CompileTimeInterceptor {

    /**
     * Called before the method execution.
     *
     * @param methodName the name of the method being called
     * @param args the method arguments
     */
    void beforeMethod(String methodName, Object[] args);

    /**
     * Called after the method returns successfully.
     *
     * @param methodName the name of the method that returned
     * @param result the return value (null for void methods)
     */
    void afterMethod(String methodName, Object result);

    /**
     * Called when the method throws an exception.
     *
     * @param methodName the name of the method that threw
     * @param ex the exception that was thrown
     */
    void afterThrowing(String methodName, Throwable ex);
}
