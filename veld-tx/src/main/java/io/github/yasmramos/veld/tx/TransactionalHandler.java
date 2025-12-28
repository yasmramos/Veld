package io.github.yasmramos.veld.tx;

import io.github.yasmramos.veld.annotation.Transactional;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

public class TransactionalHandler implements MethodInterceptor {
    private static TransactionManager transactionManager;
    public static void setTransactionManager(TransactionManager manager) { transactionManager = manager; }

    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (transactionManager == null) return ctx.proceed();
        Transactional tx = ctx.getMethod().getAnnotation(Transactional.class);
        if (tx == null) return ctx.proceed();
        boolean newTx = false;
        try {
            switch (tx.propagation()) {
                case REQUIRED: if (!transactionManager.isActive()) { transactionManager.begin(); newTx = true; } break;
                case REQUIRES_NEW: transactionManager.begin(); newTx = true; break;
                case MANDATORY: if (!transactionManager.isActive()) throw new TransactionRequiredException("Transaction required"); break;
                case NEVER: if (transactionManager.isActive()) throw new TransactionNotAllowedException("Transaction not allowed"); break;
                default: break;
            }
            Object result = ctx.proceed();
            if (newTx) transactionManager.commit();
            return result;
        } catch (Throwable t) {
            if (newTx && shouldRollback(tx, t)) transactionManager.rollback();
            throw t;
        }
    }

    private boolean shouldRollback(Transactional tx, Throwable t) {
        for (Class<? extends Throwable> nr : tx.noRollbackFor()) if (nr.isInstance(t)) return false;
        for (Class<? extends Throwable> r : tx.rollbackFor()) if (r.isInstance(t)) return true;
        return t instanceof RuntimeException || t instanceof Error;
    }

    public interface TransactionManager { void begin(); void commit(); void rollback(); boolean isActive(); }
    public static class TransactionRequiredException extends RuntimeException { public TransactionRequiredException(String m) { super(m); } }
    public static class TransactionNotAllowedException extends RuntimeException { public TransactionNotAllowedException(String m) { super(m); } }
}
