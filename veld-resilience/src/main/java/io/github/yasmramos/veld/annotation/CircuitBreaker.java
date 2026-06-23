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
 * Marks a method to be protected by a circuit breaker pattern.
 *
 * <p>The circuit breaker prevents cascading failures by stopping calls to a
 * failing service and allowing it time to recover.
 *
 * <p>States:
 * <ul>
 *   <li>CLOSED - Normal operation, calls pass through</li>
 *   <li>OPEN - Failure threshold exceeded, calls fail fast</li>
 *   <li>HALF_OPEN - Testing if service recovered</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class PaymentService {
 *     
 *     @CircuitBreaker(failureThreshold = 5, resetTimeout = 30000)
 *     public PaymentResult processPayment(Payment payment) {
 *         return paymentGateway.process(payment);
 *     }
 *     
 *     @CircuitBreaker(failureThreshold = 3, successThreshold = 2, fallbackMethod = "fallbackCall")
 *     public Response callExternalApi(Request request) {
 *         return httpClient.execute(request);
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
public @interface CircuitBreaker {
    
    /**
     * Name of the circuit breaker instance.
     * Methods with the same name share the same circuit breaker state.
     *
     * @return circuit breaker name, default uses method name
     */
    String name() default "";
    
    /**
     * Number of failures before opening the circuit.
     *
     * @return failure threshold, default 5
     */
    int failureThreshold() default 5;
    
    /**
     * Number of successful calls in HALF_OPEN state before closing the circuit.
     *
     * @return success threshold, default 3
     */
    int successThreshold() default 3;
    
    /**
     * Time in milliseconds to wait before transitioning from OPEN to HALF_OPEN.
     *
     * @return reset timeout in milliseconds, default 60000 (1 minute)
     */
    long resetTimeout() default 60000;
    
    /**
     * Time window in milliseconds for counting failures.
     *
     * @return sliding window duration, default 60000 (1 minute)
     */
    long slidingWindowDuration() default 60000;
    
    /**
     * Fallback method name to call when circuit is open.
     * Must have same signature as the annotated method.
     *
     * @return fallback method name, empty for no fallback
     */
    String fallbackMethod() default "";
    
    /**
     * Exception types that should count as failures.
     * If empty, all exceptions count as failures.
     *
     * @return exception classes to record as failures
     */
    Class<? extends Throwable>[] recordExceptions() default {};
    
    /**
     * Exception types that should NOT count as failures.
     *
     * @return exception classes to ignore
     */
    Class<? extends Throwable>[] ignoreExceptions() default {};
}
