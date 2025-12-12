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

import io.github.yasmramos.veld.annotation.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor binding for transaction management.
 *
 * <p>When applied to a method, wraps the execution in a transaction.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @Component
 * public class OrderService {
 *
 *     @Transactional
 *     public Order createOrder(OrderRequest request) {
 *         // Method runs within a transaction
 *         // Commits on success, rollback on exception
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@InterceptorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional {

    /**
     * Propagation behavior.
     */
    enum Propagation {
        /** Create new transaction, suspend existing if any */
        REQUIRED,
        /** Support current transaction, create new if none */
        REQUIRES_NEW,
        /** Support current transaction, execute non-transactionally if none */
        SUPPORTS,
        /** Execute non-transactionally, suspend current if any */
        NOT_SUPPORTED,
        /** Support current transaction, throw if none */
        MANDATORY,
        /** Execute non-transactionally, throw if current exists */
        NEVER
    }

    /**
     * Isolation level.
     */
    enum Isolation {
        DEFAULT,
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }

    /**
     * The transaction propagation type.
     *
     * @return the propagation
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * The transaction isolation level.
     *
     * @return the isolation level
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * The timeout in seconds.
     *
     * @return the timeout
     */
    int timeout() default -1;

    /**
     * Whether to mark the transaction as read-only.
     *
     * @return true for read-only
     */
    boolean readOnly() default false;

    /**
     * Exception classes that should cause rollback.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception classes that should NOT cause rollback.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] noRollbackFor() default {};
}
