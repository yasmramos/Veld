package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.aop.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for TransactionInterceptor.
 */
class TransactionInterceptorTest {

    private TransactionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TransactionInterceptor();
    }

    @Nested
    class TransactionContextTests {
        
        @Test
        void testTransactionContextCreation() {
            TransactionInterceptor.TransactionContext ctx = 
                new TransactionInterceptor.TransactionContext(1L, "TestMethod");
            
            assertEquals(1L, ctx.getId());
            assertEquals("TestMethod", ctx.getMethodName());
            assertFalse(ctx.isCommitted());
            assertFalse(ctx.isRolledBack());
        }

        @Test
        void testTransactionContextCommit() {
            TransactionInterceptor.TransactionContext ctx = 
                new TransactionInterceptor.TransactionContext(1L, "TestMethod");
            
            ctx.commit();
            
            assertTrue(ctx.isCommitted());
            assertFalse(ctx.isRolledBack());
        }

        @Test
        void testTransactionContextRollback() {
            TransactionInterceptor.TransactionContext ctx = 
                new TransactionInterceptor.TransactionContext(1L, "TestMethod");
            
            ctx.rollback(new RuntimeException("Test error"));
            
            assertFalse(ctx.isCommitted());
            assertTrue(ctx.isRolledBack());
        }
    }

    @Nested
    class StaticMethodTests {
        
        @Test
        void testGetCurrentTransactionReturnsNullWhenNoTransaction() {
            assertNull(TransactionInterceptor.getCurrentTransaction());
        }

        @Test
        void testGetTransactionCount() {
            long count = TransactionInterceptor.getTransactionCount();
            assertTrue(count >= 0);
        }
    }

    @Nested
    class TransactionalAnnotationTests {
        
        @Test
        void testPropagationEnumValues() {
            Transactional.Propagation[] values = Transactional.Propagation.values();
            
            assertEquals(6, values.length);
            assertNotNull(Transactional.Propagation.valueOf("REQUIRED"));
            assertNotNull(Transactional.Propagation.valueOf("REQUIRES_NEW"));
            assertNotNull(Transactional.Propagation.valueOf("SUPPORTS"));
            assertNotNull(Transactional.Propagation.valueOf("NOT_SUPPORTED"));
            assertNotNull(Transactional.Propagation.valueOf("MANDATORY"));
            assertNotNull(Transactional.Propagation.valueOf("NEVER"));
        }

        @Test
        void testIsolationEnumValues() {
            Transactional.Isolation[] values = Transactional.Isolation.values();
            
            assertEquals(5, values.length);
            assertNotNull(Transactional.Isolation.valueOf("DEFAULT"));
            assertNotNull(Transactional.Isolation.valueOf("READ_UNCOMMITTED"));
            assertNotNull(Transactional.Isolation.valueOf("READ_COMMITTED"));
            assertNotNull(Transactional.Isolation.valueOf("REPEATABLE_READ"));
            assertNotNull(Transactional.Isolation.valueOf("SERIALIZABLE"));
        }
    }

    @Nested
    class ManageTransactionTests {

        @Test
        void testManageTransactionSuccess() throws Throwable {
            InvocationContext ctx = mock(InvocationContext.class);
            Method method = TestService.class.getMethod("transactionalMethod");
            when(ctx.getMethod()).thenReturn(method);
            when(ctx.proceed()).thenReturn("result");
            
            Object result = interceptor.manageTransaction(ctx);
            
            assertEquals("result", result);
            verify(ctx).proceed();
        }

        @Test
        void testManageTransactionRollbackOnException() throws Throwable {
            InvocationContext ctx = mock(InvocationContext.class);
            Method method = TestService.class.getMethod("transactionalMethod");
            when(ctx.getMethod()).thenReturn(method);
            when(ctx.proceed()).thenThrow(new RuntimeException("Test exception"));
            
            assertThrows(RuntimeException.class, () -> interceptor.manageTransaction(ctx));
            verify(ctx).proceed();
        }

        @Test
        void testManageTransactionWithReadOnly() throws Throwable {
            InvocationContext ctx = mock(InvocationContext.class);
            Method method = TestService.class.getMethod("readOnlyMethod");
            when(ctx.getMethod()).thenReturn(method);
            when(ctx.proceed()).thenReturn("readonly result");
            
            Object result = interceptor.manageTransaction(ctx);
            
            assertEquals("readonly result", result);
        }
    }

    // Test helper class
    static class TestService {
        @Transactional
        public String transactionalMethod() {
            return "result";
        }

        @Transactional(readOnly = true)
        public String readOnlyMethod() {
            return "readonly";
        }

        @Transactional(propagation = Transactional.Propagation.REQUIRES_NEW)
        public String requiresNewMethod() {
            return "new tx";
        }

        @Transactional(noRollbackFor = IllegalArgumentException.class)
        public String noRollbackMethod() {
            return "no rollback";
        }
    }
}
