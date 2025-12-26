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
}
