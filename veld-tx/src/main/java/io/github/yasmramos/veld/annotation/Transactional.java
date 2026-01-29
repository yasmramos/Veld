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
 * Marks a method or class for transaction management.
 *
 * <p>When applied, the method executes within a transaction context.
 * The transaction is committed if the method completes successfully,
 * or rolled back if an exception is thrown.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class OrderService {
 *     
 *     @Transactional
 *     public void createOrder(Order order) {
 *         orderRepository.save(order);
 *         inventoryService.reserve(order.getItems());
 *         paymentService.charge(order.getPayment());
 *     }
 *     
 *     @Transactional(readOnly = true)
 *     public List<Order> findAllOrders() {
 *         return orderRepository.findAll();
 *     }
 *     
 *     @Transactional(rollbackFor = PaymentException.class)
 *     public void processPayment(Payment payment) {
 *         paymentGateway.process(payment);
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {
    
    /**
     * Transaction propagation type.
     *
     * @return propagation behavior
     */
    Propagation propagation() default Propagation.REQUIRED;
    
    /**
     * Transaction isolation level.
     *
     * @return isolation level
     */
    Isolation isolation() default Isolation.DEFAULT;
    
    /**
     * Timeout in seconds. -1 means use default.
     *
     * @return timeout in seconds
     */
    int timeout() default -1;
    
    /**
     * Whether transaction is read-only.
     *
     * @return true for read-only
     */
    boolean readOnly() default false;
    
    /**
     * Exception types that trigger rollback.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] rollbackFor() default {};
    
    /**
     * Exception type names that trigger rollback.
     *
     * @return exception class names
     */
    String[] rollbackForClassName() default {};
    
    /**
     * Exception types that should NOT trigger rollback.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] noRollbackFor() default {};
    
    /**
     * Exception type names that should NOT trigger rollback.
     *
     * @return exception class names
     */
    String[] noRollbackForClassName() default {};
    
    /**
     * Transaction propagation behaviors.
     */
    enum Propagation {
        REQUIRED,
        REQUIRES_NEW,
        SUPPORTS,
        NOT_SUPPORTED,
        MANDATORY,
        NEVER,
        NESTED
    }
    
    /**
     * Transaction isolation levels.
     */
    enum Isolation {
        DEFAULT,
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }
}
