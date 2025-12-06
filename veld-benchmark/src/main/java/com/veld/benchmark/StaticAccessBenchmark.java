package com.veld.benchmark;

import com.veld.benchmark.common.Service;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.veld.VeldSimpleService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing Veld static access vs Dagger.
 * Uses invokeExact for minimal reflection overhead.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class StaticAccessBenchmark {
    
    private BenchmarkComponent daggerComponent;
    private MethodHandle veldGetter;
    
    @Setup(Level.Trial)
    public void setup() throws Exception {
        daggerComponent = DaggerBenchmarkComponent.create();
        
        Class<?> veldClass = Class.forName("com.veld.generated.Veld");
        veldGetter = MethodHandles.lookup()
            .findStatic(veldClass, "veldSimpleService", 
                MethodType.methodType(VeldSimpleService.class));
        
        // Warmup to initialize singleton
        veldGetter.invoke();
    }
    
    @Benchmark
    public void veldStaticAccess(Blackhole bh) throws Throwable {
        VeldSimpleService service = (VeldSimpleService) veldGetter.invokeExact();
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerAccess(Blackhole bh) {
        Service service = daggerComponent.simpleService();
        bh.consume(service);
    }
}
