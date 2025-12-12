package io.github.yasmramos.veld.benchmark;

import io.github.yasmramos.veld.benchmark.common.Service;
import io.github.yasmramos.veld.benchmark.dagger.BenchmarkComponent;
import io.github.yasmramos.veld.benchmark.dagger.DaggerBenchmarkComponent;
import io.github.yasmramos.veld.generated.Veld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing Veld DIRECT static access vs Dagger.
 * Both use direct static method calls - no reflection, no MethodHandle.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class StaticAccessBenchmark {
    
    private BenchmarkComponent daggerComponent;
    
    @Setup(Level.Trial)
    public void setup() {
        daggerComponent = DaggerBenchmarkComponent.create();
        // Warmup - trigger <clinit>
        Veld.veldSimpleService();
    }
    
    @Benchmark
    public void veldStaticAccess(Blackhole bh) {
        // DIRECT static call - same as Dagger, no MethodHandle
        Object service = Veld.veldSimpleService();
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerAccess(Blackhole bh) {
        Service service = daggerComponent.simpleService();
        bh.consume(service);
    }
}
