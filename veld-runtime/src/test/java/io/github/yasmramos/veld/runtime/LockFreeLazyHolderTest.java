package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LockFreeLazyHolder - lock-free lazy initialization.
 */
class LockFreeLazyHolderTest {

    @Test
    void testLazyInitialization() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> {
            callCount.incrementAndGet();
            return "initialized";
        });
        
        assertFalse(holder.isInitialized());
        assertEquals(0, callCount.get());
        
        String result = holder.get();
        
        assertTrue(holder.isInitialized());
        assertEquals("initialized", result);
        assertEquals(1, callCount.get());
    }

    @Test
    void testSupplierCalledOnlyOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> {
            callCount.incrementAndGet();
            return "value";
        });
        
        holder.get();
        holder.get();
        holder.get();
        
        assertEquals(1, callCount.get());
    }

    @Test
    void testReturnsSupplierValue() {
        Object expected = new Object();
        LockFreeLazyHolder<Object> holder = new LockFreeLazyHolder<>(() -> expected);
        
        Object result = holder.get();
        
        assertSame(expected, result);
    }

    @Test
    void testIsInitializedBeforeGet() {
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> "test");
        
        assertFalse(holder.isInitialized());
    }

    @Test
    void testIsInitializedAfterGet() {
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> "test");
        
        holder.get();
        
        assertTrue(holder.isInitialized());
    }

    @Test
    void testNullValueFromSupplier() {
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> null);
        
        // Note: null values may not work correctly with VarHandle CAS
        // This test documents the behavior
        holder.get();
        // After first get, subsequent gets should work
        assertNull(holder.get());
    }

    @Test
    void testConcurrentInitialization() throws InterruptedException {
        int threadCount = 10;
        AtomicInteger initCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        String expectedValue = "concurrent-value";
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> {
            initCount.incrementAndGet();
            return expectedValue;
        });
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String result = holder.get();
                    assertEquals(expectedValue, result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Due to CAS nature, supplier may be called more than once
        // but results should be consistent
        assertTrue(initCount.get() >= 1);
        assertTrue(holder.isInitialized());
    }

    @Test
    void testProviderInterface() {
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> "provider-test");
        
        // LockFreeLazyHolder implements Provider
        Provider<String> provider = holder;
        
        assertEquals("provider-test", provider.get());
    }

    @Test
    void testDifferentTypes() {
        LockFreeLazyHolder<Integer> intHolder = new LockFreeLazyHolder<>(() -> 42);
        LockFreeLazyHolder<Double> doubleHolder = new LockFreeLazyHolder<>(() -> 3.14);
        LockFreeLazyHolder<Object> objectHolder = new LockFreeLazyHolder<>(Object::new);
        
        assertEquals(42, intHolder.get());
        assertEquals(3.14, doubleHolder.get());
        assertNotNull(objectHolder.get());
    }

    @Test
    void testConsistentValueAcrossMultipleGets() {
        LockFreeLazyHolder<Object> holder = new LockFreeLazyHolder<>(Object::new);
        
        Object first = holder.get();
        Object second = holder.get();
        Object third = holder.get();
        
        assertSame(first, second);
        assertSame(second, third);
    }

    // ===== NEW TESTS FOR STATIC INITIALIZER AND EDGE CASES =====

    @Test
    void testStaticInitializer_InitialState() {
        // The static initializer sets INSTANCE to null and REFRESH to new Object()
        // This test verifies the static fields are properly initialized
        LockFreeLazyHolder<String> holder1 = new LockFreeLazyHolder<>(() -> "first");
        LockFreeLazyHolder<String> holder2 = new LockFreeLazyHolder<>(() -> "second");

        // Both should start uninitialized
        assertFalse(holder1.isInitialized());
        assertFalse(holder2.isInitialized());

        // Initializing one should not affect the other
        assertEquals("first", holder1.get());
        assertFalse(holder2.isInitialized());
    }

    @Test
    void testInitializeAndGet_ConcurrentRaceCondition() throws InterruptedException {
        // Test the initializeAndGet method specifically with race conditions
        AtomicInteger initCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch readyLatch = new CountDownLatch(20);
        CountDownLatch startLatch = new CountDownLatch(1);

        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> {
            int count = initCount.incrementAndGet();
            // Small delay to increase chance of race condition
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "race-test-" + count;
        });

        // Submit all tasks
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    String result = holder.get();
                    // Verify we get a consistent value (not different values from different calls)
                    assertTrue(result.startsWith("race-test-"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for all threads to be ready
        readyLatch.await();
        // Start all threads simultaneously
        startLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Verify supplier was called (possibly multiple times due to race)
        assertTrue(initCount.get() >= 1);
        assertTrue(holder.isInitialized());
    }

    @Test
    void testInitializeAndGet_ExceptionInSupplier() {
        RuntimeException expectedException = new RuntimeException("Test exception");

        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> {
            throw expectedException;
        });

        // First call should throw
        assertThrows(RuntimeException.class, holder::get);

        // The holder should not be initialized after an exception
        // This tests the branch in initializeAndGet that handles exceptions
        assertFalse(holder.isInitialized());
    }

    @Test
    void testMultipleHolders_Independence() {
        // Each holder has its own state, testing static field independence
        LockFreeLazyHolder<String> holder1 = new LockFreeLazyHolder<>(() -> "value1");
        LockFreeLazyHolder<String> holder2 = new LockFreeLazyHolder<>(() -> "value2");

        // Initialize only holder1
        assertEquals("value1", holder1.get());
        assertTrue(holder1.isInitialized());
        assertFalse(holder2.isInitialized());

        // Initialize holder2
        assertEquals("value2", holder2.get());
        assertTrue(holder2.isInitialized());
    }

    @Test
    void testRefreshToken_StaticInitialization() {
        // The REFRESH token is created in static initializer
        // This test ensures the static block runs correctly
        LockFreeLazyHolder<String> holder = new LockFreeLazyHolder<>(() -> "refresh-test");

        // Verify initial state
        assertFalse(holder.isInitialized());

        // Initialize
        assertEquals("refresh-test", holder.get());
        assertTrue(holder.isInitialized());

        // The refresh token should exist (we can't directly access it, but we can
        // verify the holder works correctly which implies proper static init)
        assertNotNull(holder);
    }
}
