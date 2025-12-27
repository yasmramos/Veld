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
package io.github.yasmramos.veld.annotation;

import java.lang.annotation.*;

/**
 * Marks a method for asynchronous execution.
 *
 * <p>When applied to a method, the method will be executed in a separate thread
 * from a managed executor service, allowing the caller to continue without waiting
 * for the method to complete.
 *
 * <p>Methods annotated with @Async should return either:
 * <ul>
 *   <li>{@code void} - fire and forget execution</li>
 *   <li>{@code CompletableFuture<T>} - allows caller to handle result asynchronously</li>
 *   <li>{@code Future<T>} - allows caller to wait for result</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class EmailService {
 *     
 *     @Async
 *     public void sendEmail(String to, String subject, String body) {
 *         // This runs in a background thread
 *         emailClient.send(to, subject, body);
 *     }
 *     
 *     @Async
 *     public CompletableFuture<Boolean> sendEmailWithResult(String to, String subject) {
 *         boolean sent = emailClient.send(to, subject, "Hello");
 *         return CompletableFuture.completedFuture(sent);
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {
    
    /**
     * The name of the executor to use for this async method.
     * If empty, the default executor will be used.
     *
     * @return the executor name
     */
    String value() default "";
    
    /**
     * Timeout in milliseconds for the async operation.
     * A value of 0 means no timeout.
     *
     * @return timeout in milliseconds
     */
    long timeout() default 0;
}
