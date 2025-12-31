package io.github.yasmramos.veld.benchmark.features.async;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Singleton;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark para medir el rendimiento del código GENERADO con @Async.
 * Este benchmark usa el wrapper AOP generado, no llamadas directas al AsyncExecutor.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
public class GeneratedAsyncBenchmark {

    private BenchmarkAsyncService asyncService;
    private AtomicInteger counter;

    @Setup
    public void setup() {
        asyncService = Veld.get(BenchmarkAsyncService.class);
        counter = new AtomicInteger(0);
    }

    /**
     * Benchmark 1: Método @Async vacío (mide overhead del wrapper AOP generado)
     */
    @Benchmark
    public void measureGeneratedAsyncVoid(Blackhole bh) {
        asyncService.executeAsync();
        bh.consume(counter.incrementAndGet());
    }

    /**
     * Benchmark 2: Baseline - llamada a método síncrono vacío
     */
    @Benchmark
    public void measureSyncVoidCall(Blackhole bh) {
        asyncService.syncMethod();
        bh.consume(counter.incrementAndGet());
    }
}
