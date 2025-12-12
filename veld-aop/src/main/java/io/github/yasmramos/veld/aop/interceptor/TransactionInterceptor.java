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

import io.github.yasmramos.veld.annotation.AroundInvoke;
import io.github.yasmramos.veld.annotation.Interceptor;
import io.github.yasmramos.veld.aop.InvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interceptor that manages transactions.
 *
 * <p>This is a demonstration implementation that simulates transaction
 * behavior. In a real application, this would integrate with a database
 * transaction manager.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Interceptor(priority = 300)
@Transactional
public class TransactionInterceptor {

    private static final AtomicLong transactionCounter = new AtomicLong(0);
    private static final ThreadLocal<TransactionContext> currentTransaction = new ThreadLocal<>();

    /**
     * Represents a transaction context.
     */
    public static class TransactionContext {
        private final long id;
        private final String methodName;
        private final long startTime;
        private boolean committed;
        private boolean rolledBack;

        TransactionContext(long id, String methodName) {
            this.id = id;
            this.methodName = methodName;
            this.startTime = System.currentTimeMillis();
        }

        public long getId() {
            return id;
        }

        public String getMethodName() {
            return methodName;
        }

        public boolean isCommitted() {
            return committed;
        }

        public boolean isRolledBack() {
            return rolledBack;
        }

        void commit() {
            this.committed = true;
            long duration = System.currentTimeMillis() - startTime;
            System.out.printf("[TX] Transaction %d committed (took %d ms)%n", id, duration);
        }

        void rollback(Throwable cause) {
            this.rolledBack = true;
            long duration = System.currentTimeMillis() - startTime;
            System.out.printf("[TX] Transaction %d rolled back after %d ms: %s%n", 
                    id, duration, cause.getMessage());
        }
    }

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Throwable {
        Method method = ctx.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // Get annotation configuration
        Transactional config = method.getAnnotation(Transactional.class);
        if (config == null) {
            config = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        // Check if already in transaction
        TransactionContext existing = currentTransaction.get();
        if (existing != null) {
            Transactional.Propagation propagation = config != null ? 
                    config.propagation() : Transactional.Propagation.REQUIRED;
            
            if (propagation == Transactional.Propagation.REQUIRES_NEW) {
                // Suspend current and start new
                return executeInNewTransaction(ctx, methodName, config);
            } else if (propagation == Transactional.Propagation.NOT_SUPPORTED) {
                // Execute without transaction
                return ctx.proceed();
            } else {
                // Join existing transaction
                System.out.printf("[TX] Joining existing transaction %d for %s%n", 
                        existing.getId(), methodName);
                return ctx.proceed();
            }
        }

        // Start new transaction
        return executeInNewTransaction(ctx, methodName, config);
    }

    private Object executeInNewTransaction(InvocationContext ctx, String methodName,
                                            Transactional config) throws Throwable {
        long txId = transactionCounter.incrementAndGet();
        TransactionContext tx = new TransactionContext(txId, methodName);
        
        boolean readOnly = config != null && config.readOnly();
        String mode = readOnly ? " (read-only)" : "";
        
        System.out.printf("[TX] Starting transaction %d for %s%s%n", txId, methodName, mode);
        
        currentTransaction.set(tx);
        try {
            Object result = ctx.proceed();
            tx.commit();
            return result;
        } catch (Throwable t) {
            if (shouldRollback(t, config)) {
                tx.rollback(t);
            } else {
                tx.commit();
            }
            throw t;
        } finally {
            currentTransaction.remove();
        }
    }

    private boolean shouldRollback(Throwable t, Transactional config) {
        if (config == null) {
            return true; // Default: rollback on any exception
        }

        // Check noRollbackFor
        for (Class<? extends Throwable> noRollback : config.noRollbackFor()) {
            if (noRollback.isInstance(t)) {
                return false;
            }
        }

        // Check rollbackFor (if specified)
        Class<? extends Throwable>[] rollbackFor = config.rollbackFor();
        if (rollbackFor.length > 0) {
            for (Class<? extends Throwable> rollback : rollbackFor) {
                if (rollback.isInstance(t)) {
                    return true;
                }
            }
            return false; // Not in rollbackFor list
        }

        return true; // Default: rollback
    }

    /**
     * Gets the current transaction context.
     *
     * @return the current transaction, or null if none
     */
    public static TransactionContext getCurrentTransaction() {
        return currentTransaction.get();
    }

    /**
     * Gets the total number of transactions created.
     *
     * @return the transaction count
     */
    public static long getTransactionCount() {
        return transactionCounter.get();
    }
}
