package io.github.yasmramos.veld.benchmark.features.async;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.runtime.async.AsyncExecutor;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, warmups = 0)
public class AsyncBenchmark {

    private AsyncExecutor asyncExecutor;
    private AtomicInteger sharedCounter;

    @Setup
    public void setup() {
        asyncExecutor = AsyncExecutor.getInstance();
        sharedCounter = new AtomicInteger(0);
    }

    /**
     * Measures the overhead of submitting an async task (non-blocking).
     * This measures the time to submit + CompletableFuture creation.
     */
    @Benchmark
    public CompletableFuture<?> measureAsyncSubmit(Blackhole bh) {
        return asyncExecutor.submit(() -> {
            // Minimal work to avoid empty task optimization
            sharedCounter.incrementAndGet();
        });
    }

    /**
     * Measures async execution with work (non-blocking).
     */
    @Benchmark
    public CompletableFuture<Integer> measureAsyncWithWork(Blackhole bh) {
        return asyncExecutor.submit(() -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;
            }
            return sum;
        });
    }

    /**
     * Baseline: synchronous execution with same work.
     */
    @Benchmark
    public int measureSyncWithWork(Blackhole bh) {
        int sum = 0;
        for (int i = 0; i < 100; i++) {
            sum += i;
        }
        bh.consume(sum);
        return sum;
    }

    /**
     * Baseline: pure sync method call.
     */
    @Benchmark
    public void measureSyncBaseline(Blackhole bh) {
        SyncService syncService = Veld.get(SyncService.class);
        syncService.execute();
        bh.consume(syncService);
    }

    /**
     * Measure complete async roundtrip (submit + wait).
     * This shows the real-world cost of async with thread dispatch.
     */
    @Benchmark
    public long measureAsyncRoundtrip(Blackhole bh) throws Exception {
        long start = System.nanoTime();
        asyncExecutor.submit(() -> {
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;
            }
            return sum;
        }).get();
        long elapsed = System.nanoTime() - start;
        bh.consume(elapsed);
        return elapsed;
    }
}
