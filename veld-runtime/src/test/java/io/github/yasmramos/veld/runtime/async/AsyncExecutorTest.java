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
package io.github.yasmramos.veld.runtime.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AsyncExecutor}.
 */
class AsyncExecutorTest {

    @BeforeEach
    void setUp() {
        AsyncExecutor.reset();
    }

    @AfterEach
    void tearDown() {
        AsyncExecutor.reset();
    }

    @Test
    void getInstance_returnsSameInstance() {
        AsyncExecutor first = AsyncExecutor.getInstance();
        AsyncExecutor second = AsyncExecutor.getInstance();
        assertSame(first, second);
    }

    @Test
    void submit_runnable_executesTask() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = AsyncExecutor.getInstance()
            .submit(() -> executed.set(true));
        
        future.get(5, TimeUnit.SECONDS);
        assertTrue(executed.get());
    }

    @Test
    void submit_callable_returnsResult() throws Exception {
        CompletableFuture<String> future = AsyncExecutor.getInstance()
            .submit(() -> "hello");
        
        assertEquals("hello", future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void submit_callable_propagatesException() {
        CompletableFuture<String> future = AsyncExecutor.getInstance()
            .submit(() -> {
                throw new RuntimeException("test error");
            });
        
        ExecutionException ex = assertThrows(ExecutionException.class, 
            () -> future.get(5, TimeUnit.SECONDS));
        assertNotNull(ex.getCause());
    }

    @Test
    void submit_withNamedExecutor_usesRegisteredExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newSingleThreadExecutor();
        AtomicBoolean usedCustom = new AtomicBoolean(false);
        
        AsyncExecutor.getInstance().registerExecutor("custom", customExecutor);
        
        CompletableFuture<Void> future = AsyncExecutor.getInstance()
            .submit(() -> {
                usedCustom.set(Thread.currentThread().getName().startsWith("pool"));
            }, "custom");
        
        future.get(5, TimeUnit.SECONDS);
        assertTrue(usedCustom.get());
        customExecutor.shutdown();
    }

    @Test
    void submit_withUnknownExecutor_usesDefault() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = AsyncExecutor.getInstance()
            .submit(() -> executed.set(true), "unknown");
        
        future.get(5, TimeUnit.SECONDS);
        assertTrue(executed.get());
    }

    @Test
    void submit_afterShutdown_throwsException() {
        AsyncExecutor executor = AsyncExecutor.getInstance();
        executor.shutdown();
        
        assertThrows(RejectedExecutionException.class, 
            () -> executor.submit(() -> {}));
    }

    @Test
    void submit_callable_afterShutdown_throwsException() {
        AsyncExecutor executor = AsyncExecutor.getInstance();
        executor.shutdown();
        
        assertThrows(RejectedExecutionException.class, 
            () -> executor.submit(() -> "test"));
    }

    @Test
    void submit_runnable_withNullExecutorName_usesDefault() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = AsyncExecutor.getInstance()
            .submit(() -> executed.set(true), null);
        
        future.get(5, TimeUnit.SECONDS);
        assertTrue(executed.get());
    }

    @Test
    void submit_callable_withEmptyExecutorName_usesDefault() throws Exception {
        CompletableFuture<Integer> future = AsyncExecutor.getInstance()
            .submit(() -> 42, "");
        
        assertEquals(42, future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void multipleTasks_executesConcurrently() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            AsyncExecutor.getInstance().submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, counter.get());
    }

    @Test
    void reset_shutsDownAndClearsInstance() throws Exception {
        AsyncExecutor first = AsyncExecutor.getInstance();
        first.submit(() -> {}).get(1, TimeUnit.SECONDS);
        
        AsyncExecutor.reset();
        
        AsyncExecutor second = AsyncExecutor.getInstance();
        assertNotSame(first, second);
    }

    @Test
    void submit_callable_withNamedExecutor_returnsResult() throws Exception {
        ExecutorService customExecutor = Executors.newSingleThreadExecutor();
        AsyncExecutor.getInstance().registerExecutor("calc", customExecutor);
        
        CompletableFuture<Integer> future = AsyncExecutor.getInstance()
            .submit(() -> 100, "calc");
        
        assertEquals(100, future.get(5, TimeUnit.SECONDS));
        customExecutor.shutdown();
    }

    @Test
    void submit_callable_withNullExecutorName_usesDefault() throws Exception {
        CompletableFuture<String> future = AsyncExecutor.getInstance()
            .submit(() -> "default", null);
        
        assertEquals("default", future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void registerExecutor_multipleExecutors_worksCorrectly() throws Exception {
        ExecutorService exec1 = Executors.newSingleThreadExecutor();
        ExecutorService exec2 = Executors.newSingleThreadExecutor();
        
        AsyncExecutor.getInstance().registerExecutor("exec1", exec1);
        AsyncExecutor.getInstance().registerExecutor("exec2", exec2);
        
        CompletableFuture<Integer> f1 = AsyncExecutor.getInstance().submit(() -> 1, "exec1");
        CompletableFuture<Integer> f2 = AsyncExecutor.getInstance().submit(() -> 2, "exec2");
        
        assertEquals(1, f1.get(5, TimeUnit.SECONDS));
        assertEquals(2, f2.get(5, TimeUnit.SECONDS));
        
        exec1.shutdown();
        exec2.shutdown();
    }

    @Test
    void submit_runnable_noArgs_usesDefaultExecutor() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = AsyncExecutor.getInstance()
            .submit(() -> executed.set(true));
        
        future.get(5, TimeUnit.SECONDS);
        assertTrue(executed.get());
    }

    @Test
    void submit_callable_noArgs_usesDefaultExecutor() throws Exception {
        CompletableFuture<String> future = AsyncExecutor.getInstance()
            .submit(() -> "noArgs");
        
        assertEquals("noArgs", future.get(5, TimeUnit.SECONDS));
    }
}
