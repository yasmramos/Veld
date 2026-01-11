package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VeldType - ultra-optimized type holder with thread-local caching.
 */
class VeldTypeTest {

    @BeforeEach
    void setUp() {
        VeldType.clearThreadCache();
    }

    @AfterEach
    void tearDown() {
        VeldType.clearThreadCache();
    }

    @Test
    void testInitialState() {
        VeldType type = new VeldType();
        
        assertFalse(type.isInitialized());
        assertNull(type.getThreadLocal());
    }

    @Test
    void testCompareAndSet() {
        VeldType type = new VeldType();
        String value = "test-value";
        
        boolean success = type.compareAndSet(null, value);
        
        assertTrue(success);
        assertTrue(type.isInitialized());
    }

    @Test
    void testCompareAndSetFailsWithWrongExpected() {
        VeldType type = new VeldType();
        type.compareAndSet(null, "first");
        
        boolean success = type.compareAndSet(null, "second");
        
        assertFalse(success);
    }

    @Test
    void testGetThreadLocal() {
        VeldType type = new VeldType();
        String value = "thread-local-value";
        type.compareAndSet(null, value);
        
        Object result = type.getThreadLocal();
        
        assertEquals(value, result);
    }

    @Test
    void testGetOptimized() {
        VeldType type = new VeldType();
        String value = "optimized-value";
        type.compareAndSet(null, value);
        
        Object result = type.getOptimized();
        
        assertEquals(value, result);
    }

    @Test
    void testGetConcurrent() {
        VeldType type = new VeldType();
        String value = "concurrent-value";
        type.compareAndSet(null, value);
        
        Object result = type.getConcurrent();
        
        assertEquals(value, result);
    }

    @Test
    void testGetCached() {
        VeldType type = new VeldType();
        String value = "cached-value";
        type.compareAndSet(null, value);
        
        // First access populates cache
        type.getThreadLocal();
        
        // getCached should return the cached value
        Object cached = type.getCached();
        assertEquals(value, cached);
    }

    @Test
    void testGetCachedReturnsNullIfNotCached() {
        VeldType type = new VeldType();
        
        Object cached = type.getCached();
        
        assertNull(cached);
    }

    @Test
    void testConcurrentContext() {
        VeldType type = new VeldType();
        type.compareAndSet(null, "value");
        
        try (VeldType.ConcurrentContext ctx = VeldType.concurrent()) {
            Object result = type.getOptimized();
            assertNotNull(result);
        }
    }

    @Test
    void testClearThreadCache() {
        VeldType type = new VeldType();
        type.compareAndSet(null, "value");
        type.getThreadLocal(); // Populate cache
        
        VeldType.clearThreadCache();
        
        // Cache should be cleared, but value still accessible
        assertEquals("value", type.getThreadLocal());
    }

    @Test
    void testForceCleanupAll() {
        VeldType type = new VeldType();
        type.compareAndSet(null, "value");
        type.getThreadLocal();
        
        VeldType.forceCleanupAll();
        
        // Should still work after cleanup
        assertEquals("value", type.getThreadLocal());
    }

    @Test
    void testGetTLLoadFactor() {
        VeldType.clearThreadCache();
        
        double loadBefore = VeldType.getTLLoadFactor();
        assertEquals(0.0, loadBefore, 0.001);
        
        VeldType type = new VeldType();
        type.compareAndSet(null, "value");
        type.getThreadLocal(); // Populate cache
        
        double loadAfter = VeldType.getTLLoadFactor();
        assertTrue(loadAfter > 0);
    }

    @Test
    void testToString() {
        VeldType type = new VeldType();
        
        String uninitializedString = type.toString();
        assertTrue(uninitializedString.contains("null"));
        
        type.compareAndSet(null, "test");
        String initializedString = type.toString();
        assertTrue(initializedString.contains("String"));
    }

    @Test
    void testMultipleTypes() {
        VeldType type1 = new VeldType();
        VeldType type2 = new VeldType();
        VeldType type3 = new VeldType();
        
        type1.compareAndSet(null, "value1");
        type2.compareAndSet(null, 42);
        type3.compareAndSet(null, 3.14);
        
        assertEquals("value1", type1.getThreadLocal());
        assertEquals(42, type2.getThreadLocal());
        assertEquals(3.14, type3.getThreadLocal());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        VeldType type = new VeldType();
        type.compareAndSet(null, "concurrent-test");
        
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    try (VeldType.ConcurrentContext ctx = VeldType.concurrent()) {
                        Object result = type.getOptimized();
                        assertEquals("concurrent-test", result);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testCacheUpdateOnCompareAndSet() {
        VeldType type = new VeldType();
        type.compareAndSet(null, "initial");
        type.getThreadLocal(); // Cache it
        
        Object cached = type.getCached();
        assertEquals("initial", cached);
        
        // Update should clear cache
        type.compareAndSet("initial", "updated");
        
        // Re-access to populate cache again
        type.getThreadLocal();
        Object newCached = type.getCached();
        assertEquals("updated", newCached);
    }
}
