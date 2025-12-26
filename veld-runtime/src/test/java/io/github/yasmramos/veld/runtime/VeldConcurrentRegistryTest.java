package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VeldConcurrentRegistry - high-performance concurrent component registry.
 */
class VeldConcurrentRegistryTest {

    private VeldConcurrentRegistry registry;

    @BeforeEach
    void setUp() {
        VeldConcurrentRegistry.forceCleanup();
        registry = new VeldConcurrentRegistry(16);
    }

    @AfterEach
    void tearDown() {
        VeldConcurrentRegistry.clearThreadCache();
    }

    @Test
    void testRegisterAndGet() {
        String instance = "test-instance";
        registry.register(String.class, instance);
        
        String result = registry.get(String.class);
        
        assertSame(instance, result);
    }

    @Test
    void testGetNonExistentReturnsNull() {
        Object result = registry.get(Integer.class);
        
        assertNull(result);
    }

    @Test
    void testRegisterMultipleTypes() {
        String strInstance = "string";
        Integer intInstance = 42;
        Double doubleInstance = 3.14;
        
        registry.register(String.class, strInstance);
        registry.register(Integer.class, intInstance);
        registry.register(Double.class, doubleInstance);
        
        assertSame(strInstance, registry.get(String.class));
        assertSame(intInstance, registry.get(Integer.class));
        assertSame(doubleInstance, registry.get(Double.class));
    }

    @Test
    void testOverwriteExistingRegistration() {
        String first = "first";
        String second = "second";
        
        registry.register(String.class, first);
        registry.register(String.class, second);
        
        assertSame(second, registry.get(String.class));
    }

    @Test
    void testThreadLocalCacheHit() {
        String instance = "cached";
        registry.register(String.class, instance);
        
        // First access populates cache
        assertSame(instance, registry.get(String.class));
        // Second access should hit thread-local cache
        assertSame(instance, registry.get(String.class));
        // Third access - still cached
        assertSame(instance, registry.get(String.class));
    }

    @Test
    void testClearThreadCache() {
        String instance = "test";
        registry.register(String.class, instance);
        
        registry.get(String.class); // Populate cache
        VeldConcurrentRegistry.clearThreadCache();
        
        // Should still work after cache clear
        assertSame(instance, registry.get(String.class));
    }

    @Test
    void testForceCleanup() {
        String instance = "test";
        registry.register(String.class, instance);
        
        registry.get(String.class); // Populate cache
        VeldConcurrentRegistry.forceCleanup();
        
        // Should still work after force cleanup
        assertSame(instance, registry.get(String.class));
    }

    @Test
    void testGetLoadFactor() {
        assertEquals(0.0, registry.getLoadFactor(), 0.001);
        
        registry.register(String.class, "test");
        
        assertTrue(registry.getLoadFactor() > 0);
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String instance = "concurrent-test";
        registry.register(String.class, instance);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        String result = registry.get(String.class);
                        if (instance.equals(result)) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(threadCount * operationsPerThread, successCount.get());
    }

    @Test
    void testResizeOnHighLoad() {
        // Create a small registry that will need to resize
        VeldConcurrentRegistry smallRegistry = new VeldConcurrentRegistry(4);
        
        // Register many types to trigger resize
        for (int i = 0; i < 20; i++) {
            final int index = i;
            Class<?> type = getTestClass(index);
            if (type != null) {
                smallRegistry.register(type, "instance-" + index);
            }
        }
        
        // Verify all can be retrieved
        for (int i = 0; i < 20; i++) {
            Class<?> type = getTestClass(i);
            if (type != null) {
                Object result = smallRegistry.get(type);
                assertNotNull(result, "Should find instance for type " + type);
            }
        }
    }

    @Test
    void testLoadFactorAfterMultipleRegistrations() {
        VeldConcurrentRegistry smallRegistry = new VeldConcurrentRegistry(8);
        
        double initialLoad = smallRegistry.getLoadFactor();
        assertEquals(0.0, initialLoad, 0.001);
        
        smallRegistry.register(String.class, "a");
        smallRegistry.register(Integer.class, 1);
        smallRegistry.register(Double.class, 1.0);
        
        double loadAfter = smallRegistry.getLoadFactor();
        assertTrue(loadAfter > 0, "Load factor should be positive after registrations");
    }

    // Helper to get different classes for testing
    private Class<?> getTestClass(int index) {
        Class<?>[] classes = {
            String.class, Integer.class, Double.class, Float.class,
            Long.class, Short.class, Byte.class, Boolean.class,
            Character.class, Object.class, Number.class, Comparable.class,
            Runnable.class, Cloneable.class, AutoCloseable.class,
            Iterable.class, CharSequence.class, Appendable.class,
            Readable.class, Thread.class
        };
        return index < classes.length ? classes[index] : null;
    }
}
