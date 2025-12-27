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

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages thread pools for @Async method execution.
 *
 * <p>Provides a default executor and support for named executors
 * for different async workloads.
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
public final class AsyncExecutor {
    
    private static volatile AsyncExecutor instance;
    
    private final ExecutorService defaultExecutor;
    private final Map<String, ExecutorService> namedExecutors;
    private volatile boolean shutdown = false;
    
    private AsyncExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        this.defaultExecutor = new ThreadPoolExecutor(
            cores,
            cores * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new VeldThreadFactory("veld-async"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.namedExecutors = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return the AsyncExecutor instance
     */
    public static AsyncExecutor getInstance() {
        if (instance == null) {
            synchronized (AsyncExecutor.class) {
                if (instance == null) {
                    instance = new AsyncExecutor();
                }
            }
        }
        return instance;
    }
    
    /**
     * Submits a task for async execution.
     *
     * @param task the task to execute
     * @return a CompletableFuture representing the task
     */
    public CompletableFuture<Void> submit(Runnable task) {
        return submit(task, "");
    }
    
    /**
     * Submits a task for async execution with a named executor.
     *
     * @param task the task to execute
     * @param executorName the name of the executor to use
     * @return a CompletableFuture representing the task
     */
    public CompletableFuture<Void> submit(Runnable task, String executorName) {
        if (shutdown) {
            throw new RejectedExecutionException("AsyncExecutor has been shut down");
        }
        ExecutorService executor = getExecutor(executorName);
        return CompletableFuture.runAsync(task, executor);
    }
    
    /**
     * Submits a callable for async execution.
     *
     * @param callable the callable to execute
     * @param <T> the result type
     * @return a CompletableFuture representing the result
     */
    public <T> CompletableFuture<T> submit(Callable<T> callable) {
        return submit(callable, "");
    }
    
    /**
     * Submits a callable for async execution with a named executor.
     *
     * @param callable the callable to execute
     * @param executorName the name of the executor to use
     * @param <T> the result type
     * @return a CompletableFuture representing the result
     */
    public <T> CompletableFuture<T> submit(Callable<T> callable, String executorName) {
        if (shutdown) {
            throw new RejectedExecutionException("AsyncExecutor has been shut down");
        }
        ExecutorService executor = getExecutor(executorName);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Registers a named executor.
     *
     * @param name the executor name
     * @param executor the executor service
     */
    public void registerExecutor(String name, ExecutorService executor) {
        namedExecutors.put(name, executor);
    }
    
    /**
     * Gets an executor by name, or default if name is empty.
     */
    private ExecutorService getExecutor(String name) {
        if (name == null || name.isEmpty()) {
            return defaultExecutor;
        }
        return namedExecutors.getOrDefault(name, defaultExecutor);
    }
    
    /**
     * Shuts down all executors gracefully.
     */
    public void shutdown() {
        shutdown = true;
        defaultExecutor.shutdown();
        namedExecutors.values().forEach(ExecutorService::shutdown);
        
        try {
            if (!defaultExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                defaultExecutor.shutdownNow();
            }
            for (ExecutorService executor : namedExecutors.values()) {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            defaultExecutor.shutdownNow();
            namedExecutors.values().forEach(ExecutorService::shutdownNow);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Resets the singleton (for testing).
     */
    public static void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
    
    /**
     * Thread factory that creates named daemon threads.
     */
    private static class VeldThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;
        
        VeldThreadFactory(String prefix) {
            this.prefix = prefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
