package io.github.yasmramos.veld.aop.runtime.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link AsyncExecutor}.
 * 
 * @author Veld Team
 * @version 1.0.0
 */
@DisplayName("AsyncExecutor Tests")
class AsyncExecutorTest {

    @BeforeEach
    void setUp() {
        // Reset singleton state before each test
        AsyncExecutor.reset();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        AsyncExecutor.reset();
    }

    @Test
    @DisplayName("Should create singleton instance")
    void shouldCreateSingletonInstance() {
        // When
        AsyncExecutor executor = AsyncExecutor.getInstance();

        // Then
        assertNotNull(executor);
        assertSame(executor, AsyncExecutor.getInstance());
    }

    @Test
    @DisplayName("Should submit runnable task successfully")
    void shouldSubmitRunnableTaskSuccessfully() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable task = latch::countDown;

        // When
        CompletableFuture<Void> future = executor.submit(task);

        // Then
        assertNotNull(future);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task should complete within timeout");
        // Wait for future to complete (it should be done after latch)
        future.get(5, TimeUnit.SECONDS);
        assertTrue(future.isDone(), "Future should be completed");
    }

    @Test
    @DisplayName("Should submit callable task successfully")
    void shouldSubmitCallableTaskSuccessfully() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        Callable<String> callable = () -> "result";

        // When
        CompletableFuture<String> future = executor.submit(callable);

        // Then
        assertNotNull(future);
        assertEquals("result", future.get(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should submit task with empty executor name")
    void shouldSubmitTaskWithEmptyExecutorName() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable task = latch::countDown;

        // When
        CompletableFuture<Void> future = executor.submit(task, "");

        // Then
        assertNotNull(future);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task should complete within timeout");
    }

    @Test
    @DisplayName("Should submit task with null executor name")
    void shouldSubmitTaskWithNullExecutorName() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable task = latch::countDown;

        // When
        CompletableFuture<Void> future = executor.submit(task, null);

        // Then
        assertNotNull(future);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task should complete within timeout");
    }

    @Test
    @DisplayName("Should register named executor")
    void shouldRegisterNamedExecutor() {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        ExecutorService namedExecutor = Executors.newFixedThreadPool(2);

        // When
        executor.registerExecutor("testExecutor", namedExecutor);

        // Then - no exception should be thrown
        assertNotNull(executor.getDefaultExecutor());
        
        // Cleanup
        namedExecutor.shutdown();
    }

    @Test
    @DisplayName("Should get default executor")
    void shouldGetDefaultExecutor() {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();

        // When
        ExecutorService defaultExecutor = executor.getDefaultExecutor();

        // Then
        assertNotNull(defaultExecutor);
        assertFalse(defaultExecutor.isShutdown());
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();

        // When
        executor.shutdown();

        // Then
        assertTrue(executor.getDefaultExecutor().isShutdown());
    }

    @Test
    @DisplayName("Should throw RejectedExecutionException after shutdown")
    void shouldThrowRejectedExecutionExceptionAfterShutdown() {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        executor.shutdown();
        Runnable task = () -> {};

        // When & Then
        assertThrows(RejectedExecutionException.class, () -> {
            executor.submit(task);
        });
    }

    @Test
    @DisplayName("Should handle task that throws exception")
    void shouldHandleTaskThatThrowsException() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        Callable<String> failingCallable = () -> {
            throw new RuntimeException("Test exception");
        };

        // When
        CompletableFuture<String> future = executor.submit(failingCallable);

        // Then
        assertNotNull(future);
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle multiple concurrent submissions")
    void shouldHandleMultipleConcurrentSubmissions() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        // When
        for (int i = 0; i < taskCount; i++) {
            executor.submit(latch::countDown);
        }

        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All tasks should complete within timeout");
    }

    @Test
    @DisplayName("Should reset singleton correctly")
    void shouldResetSingletonCorrectly() {
        // Given
        AsyncExecutor executor1 = AsyncExecutor.getInstance();

        // When
        AsyncExecutor.reset();
        AsyncExecutor executor2 = AsyncExecutor.getInstance();

        // Then
        assertNotSame(executor1, executor2);
    }

    @Test
    @DisplayName("Should handle callable returning null")
    void shouldHandleCallableReturningNull() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        Callable<String> nullCallable = () -> null;

        // When
        CompletableFuture<String> future = executor.submit(nullCallable);

        // Then
        assertNotNull(future);
        assertNull(future.get(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle runnable with long execution time")
    void shouldHandleRunnableWithLongExecutionTime() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable slowTask = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        };

        // When
        CompletableFuture<Void> future = executor.submit(slowTask);

        // Then
        assertNotNull(future);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Slow task should complete within timeout");
    }

    @Test
    @DisplayName("Should handle submit with callable and executor name")
    void shouldHandleSubmitWithCallableAndExecutorName() throws Exception {
        // Given
        AsyncExecutor executor = AsyncExecutor.getInstance();
        Callable<Integer> callable = () -> 42;

        // When
        CompletableFuture<Integer> future = executor.submit(callable, "nonExistentExecutor");

        // Then
        assertNotNull(future);
        assertEquals(42, future.get(5, TimeUnit.SECONDS).intValue());
    }
}
