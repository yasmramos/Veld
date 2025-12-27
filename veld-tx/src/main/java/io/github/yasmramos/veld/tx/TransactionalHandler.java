package io.github.yasmramos.veld.tx;

import io.github.yasmramos.veld.annotation.Transactional;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;

/**
 * Manages transactional boundaries for methods.
 */
public class TransactionalHandler implements AspectHandler {

    private static TransactionManager transactionManager;

    public static void setTransactionManager(TransactionManager manager) {
        transactionManager = manager;
    }

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Transactional.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        if (transactionManager == null) {
            // No transaction manager configured, proceed without transaction
            return invocation.proceed();
        }

        Transactional tx = invocation.getMethod().getAnnotation(Transactional.class);
        
        boolean newTransaction = false;
        try {
            switch (tx.propagation()) {
                case REQUIRED:
                    if (!transactionManager.isActive()) {
                        transactionManager.begin();
                        newTransaction = true;
                    }
                    break;
                case REQUIRES_NEW:
                    transactionManager.begin();
                    newTransaction = true;
                    break;
                case MANDATORY:
                    if (!transactionManager.isActive()) {
                        throw new TransactionRequiredException("Transaction required but none active");
                    }
                    break;
                case NEVER:
                    if (transactionManager.isActive()) {
                        throw new TransactionNotAllowedException("Transaction not allowed");
                    }
                    break;
                case SUPPORTS:
                default:
                    // Proceed with or without transaction
                    break;
            }

            Object result = invocation.proceed();
            
            if (newTransaction) {
                transactionManager.commit();
            }
            
            return result;
            
        } catch (Throwable t) {
            if (newTransaction && shouldRollback(tx, t)) {
                transactionManager.rollback();
            }
            throw t;
        }
    }

    private boolean shouldRollback(Transactional tx, Throwable t) {
        for (Class<? extends Throwable> noRollback : tx.noRollbackFor()) {
            if (noRollback.isInstance(t)) return false;
        }
        for (Class<? extends Throwable> rollback : tx.rollbackFor()) {
            if (rollback.isInstance(t)) return true;
        }
        return t instanceof RuntimeException || t instanceof Error;
    }

    public interface TransactionManager {
        void begin();
        void commit();
        void rollback();
        boolean isActive();
    }

    public static class TransactionRequiredException extends RuntimeException {
        public TransactionRequiredException(String message) { super(message); }
    }

    public static class TransactionNotAllowedException extends RuntimeException {
        public TransactionNotAllowedException(String message) { super(message); }
    }
}
