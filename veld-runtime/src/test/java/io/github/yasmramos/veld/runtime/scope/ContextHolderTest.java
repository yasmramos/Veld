package io.github.yasmramos.veld.runtime.scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContextHolder thread-safety and concurrent access.
 */
@DisplayName("ContextHolder Tests")
class ContextHolderTest {

    @Nested
    @DisplayName("Basic Operations Tests")
    class BasicOperationsTests {

        @Test
        @DisplayName("Should store and retrieve value")
        void shouldStoreAndRetrieveValue() {
            ContextHolder<String> holder = new ContextHolder<>();
            assertNull(holder.get());

            holder.set("test-value");
            assertEquals("test-value", holder.get());
        }

        @Test
        @DisplayName("Should return default value when null")
        void shouldReturnDefaultWhenNull() {
            ContextHolder<String> holder = new ContextHolder<>();
            assertEquals("default", holder.getOrDefault("default"));
        }

        @Test
        @DisplayName("Should return actual value when not null")
        void shouldReturnActualWhenNotNull() {
            ContextHolder<String> holder = new ContextHolder<>();
            holder.set("actual");
            assertEquals("actual", holder.getOrDefault("default"));
        }

        @Test
        @DisplayName("Should clear value")
        void shouldClearValue() {
            ContextHolder<String> holder = new ContextHolder<>();
            holder.set("test");
            assertTrue(holder.hasValue());

            holder.clear();
            assertNull(holder.get());
            assertFalse(holder.hasValue());
        }

        @Test
        @DisplayName("Should indicate if value exists")
        void shouldIndicateValueExists() {
            ContextHolder<String> holder = new ContextHolder<>();
            assertFalse(holder.hasValue());

            holder.set("value");
            assertTrue(holder.hasValue());

            holder.clear();
            assertFalse(holder.hasValue());
        }
    }

    @Nested
    @DisplayName("Atomic Operations Tests")
    class AtomicOperationsTests {

        @Test
        @DisplayName("Should get and set atomically")
        void shouldGetAndSetAtomically() {
            ContextHolder<Integer> holder = new ContextHolder<>(0);

            int oldValue = holder.getAndSet(1);
            assertEquals(0, oldValue);
            assertEquals(1, holder.get());
        }

        @Test
        @DisplayName("Should update and get atomically")
        void shouldUpdateAndGetAtomically() {
            ContextHolder<Integer> holder = new ContextHolder<>(5);

            int result = holder.updateAndGet(v -> v * 2);
            assertEquals(10, result);
            assertEquals(10, holder.get());
        }

        @Test
        @DisplayName("Should throw on null from update function")
        void shouldThrowOnNullFromUpdateFunction() {
            ContextHolder<String> holder = new ContextHolder<>("value");

            assertThrows(NullPointerException.class, () ->
                holder.updateAndGet(v -> null)
            );
        }

        @Test
        @DisplayName("Should compare and set successfully")
        void shouldCompareAndSetSuccessfully() {
            ContextHolder<String> holder = new ContextHolder<>("old");

            boolean success = holder.compareAndSet("old", "new");
            assertTrue(success);
            assertEquals("new", holder.get());
        }

        @Test
        @DisplayName("Should fail compare and set when values don't match")
        void shouldFailCompareAndSetWhenValuesDiffer() {
            ContextHolder<String> holder = new ContextHolder<>("old");

            boolean success = holder.compareAndSet("wrong", "new");
            assertFalse(success);
            assertEquals("old", holder.get());
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent getAndSet from multiple threads")
        void shouldHandleConcurrentGetAndSet() throws InterruptedException {
            ContextHolder<Integer> holder = new ContextHolder<>(0);
            int threadCount = 10;
            int iterationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger expectedTotal = new AtomicInteger(threadCount * iterationsPerThread);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            // Use updateAndGet for atomic increment - handles retries internally
                            holder.updateAndGet(v -> v + 1);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            // All increments should be applied atomically
            assertEquals(expectedTotal.get(), holder.get(),
                "Value should equal total number of increments");
        }

        @Test
        @DisplayName("Should handle concurrent set and get from multiple threads")
        void shouldHandleConcurrentSetAndGet() throws InterruptedException {
            ContextHolder<String> holder = new ContextHolder<>();
            int threadCount = 10;
            int iterationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadNum = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            holder.set("thread-" + threadNum + "-value-" + j);
                            String value = holder.get();
                            assertNotNull(value,
                                "Value should never be null after set");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertNotNull(holder.get(),
                "Final value should not be null");
        }

        @Test
        @DisplayName("Should handle concurrent clear operations")
        void shouldHandleConcurrentClear() throws InterruptedException {
            ContextHolder<String> holder = new ContextHolder<>("initial");
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger nullCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 100; j++) {
                            holder.set("value-" + j);
                            holder.clear();
                            if (holder.get() == null) {
                                nullCount.incrementAndGet();
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
            doneLatch.await();
            executor.shutdown();

            assertTrue(nullCount.get() > 0,
                "Some gets should have seen null after clear");
        }

        @Test
        @DisplayName("Should maintain consistency under high contention")
        void shouldMaintainConsistencyUnderHighContention() throws InterruptedException {
            ContextHolder<String> holder = new ContextHolder<>("initial");
            int threadCount = 20;
            int iterationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            // Various operations that should all be thread-safe
                            holder.getAndSet("value-" + j);
                            holder.updateAndGet(v -> v + "-updated");
                            holder.hasValue();
                            holder.getOrDefault("default");
                        }
                    } catch (Exception e) {
                        exceptionOccurred.set(true);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertFalse(exceptionOccurred.get(),
                "No exceptions should occur during concurrent access");
            assertNotNull(holder.get(),
                "Final value should not be null");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle initial value in constructor")
        void shouldHandleInitialValueInConstructor() {
            ContextHolder<Integer> holder = new ContextHolder<>(42);
            assertEquals(42, holder.get());
            assertTrue(holder.hasValue());
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            ContextHolder<String> holder = new ContextHolder<>("non-null");
            holder.set(null);
            assertNull(holder.get());
            assertFalse(holder.hasValue());
        }

        @Test
        @DisplayName("Should handle different generic types")
        void shouldHandleDifferentGenericTypes() {
            ContextHolder<Integer> intHolder = new ContextHolder<>(1);
            ContextHolder<String> strHolder = new ContextHolder<>("str");
            ContextHolder<Object> objHolder = new ContextHolder<>(new Object());

            assertEquals(1, intHolder.get());
            assertEquals("str", strHolder.get());
            assertNotNull(objHolder.get());
        }

        @Test
        @DisplayName("Should handle rapid alternating operations")
        void shouldHandleRapidAlternatingOperations() throws InterruptedException {
            ContextHolder<Integer> holder = new ContextHolder<>(0);
            int iterations = 1000;

            for (int i = 0; i < iterations; i++) {
                int oldVal = holder.get();
                holder.set(oldVal + 1);
                if (holder.get() > oldVal + 1) {
                    fail("Value increased by more than 1");
                }
            }

            assertEquals(iterations, holder.get());
        }
    }
}
