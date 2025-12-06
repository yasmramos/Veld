package com.veld.benchmark.staticbench;

import com.veld.generated.Veld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing DIRECT static method calls:
 * - Veld: Veld.staticService() - generated static accessor
 * - Dagger: component.staticService() - component method
 * 
 * NO reflection, NO MethodHandle - pure direct calls.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StaticAccessBenchmark {

    private BenchmarkComponent daggerComponent;

    @Setup
    public void setup() {
        // Initialize Dagger component
        daggerComponent = DaggerBenchmarkComponent.create();
        
        // Warm up Veld's static accessor (first call initializes singleton)
        Veld.staticService();
    }

    /**
     * Veld: Direct static method call - no container lookup
     * This is equivalent to Dagger's generated code pattern
     */
    @Benchmark
    public void veldStaticAccess(Blackhole bh) {
        bh.consume(Veld.staticService());
    }

    /**
     * Dagger: Component method call
     * Standard Dagger access pattern
     */
    @Benchmark
    public void daggerAccess(Blackhole bh) {
        bh.consume(daggerComponent.staticService());
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
