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
 * <p>Optimizations implemented in Phase 1.1:
 * <ul>
 *   <li>Lazy ThreadLocal initialization to avoid overhead when not used</li>
 *   <li>Inlined executor access for common cases</li>
 *   <li>Reduced allocation in hot path</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
public final class AsyncExecutor {

    private static volatile AsyncExecutor instance;

    private final ExecutorService defaultExecutor;
    private final Map<String, ExecutorService> namedExecutors;
    private volatile boolean shutdown = false;

    // Lazy ThreadLocal - only initialized when actually needed
    private static final ThreadLocal<ExecutorService> EXECUTOR_CACHE =
            ThreadLocal.withInitial(() -> null);

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
     */
    public static AsyncExecutor getInstance() {
        AsyncExecutor result = instance;
        if (result == null) {
            synchronized (AsyncExecutor.class) {
                result = instance;
                if (result == null) {
                    instance = result = new AsyncExecutor();
                }
            }
        }
        return result;
    }

    /**
     * Gets the cached executor for the current thread.
     * Uses lazy initialization - only creates the cache entry when first accessed.
     */
    private ExecutorService getCachedExecutor() {
        ExecutorService cached = EXECUTOR_CACHE.get();
        if (cached == null) {
            // Only set if still null (avoid race condition overhead)
            EXECUTOR_CACHE.set(defaultExecutor);
            return defaultExecutor;
        }
        return cached;
    }

    /**
     * Submits a task for async execution.
     */
    public CompletableFuture<Void> submit(Runnable task) {
        return submit(task, "");
    }

    /**
     * Submits a task for async execution with a named executor.
     */
    public CompletableFuture<Void> submit(Runnable task, String executorName) {
        if (shutdown) {
            throw new RejectedExecutionException("AsyncExecutor has been shut down");
        }

        // Fast path: default executor
        if (executorName == null || executorName.isEmpty()) {
            return CompletableFuture.runAsync(task, getCachedExecutor());
        }

        // Slow path: need to look up named executor
        ExecutorService executor = namedExecutors.get(executorName);
        if (executor == null) {
            executor = defaultExecutor;
        }
        return CompletableFuture.runAsync(task, executor);
    }

    /**
     * Submits a callable for async execution.
     */
    public <T> CompletableFuture<T> submit(Callable<T> callable) {
        return submit(callable, "");
    }

    /**
     * Submits a callable for async execution with a named executor.
     */
    public <T> CompletableFuture<T> submit(Callable<T> callable, String executorName) {
        if (shutdown) {
            throw new RejectedExecutionException("AsyncExecutor has been shut down");
        }

        // Fast path: default executor
        if (executorName == null || executorName.isEmpty()) {
            ExecutorService executor = getCachedExecutor();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, executor);
        }

        // Slow path: need to look up named executor
        ExecutorService executor = namedExecutors.get(executorName);
        if (executor == null) {
            executor = defaultExecutor;
        }
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
     */
    public void registerExecutor(String name, ExecutorService executor) {
        namedExecutors.put(name, executor);
    }

    /**
     * Gets the default executor for direct use in generated code.
     * This allows compile-time constant folding for @Async methods.
     */
    public ExecutorService getDefaultExecutor() {
        return defaultExecutor;
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
            EXECUTOR_CACHE.remove();
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
