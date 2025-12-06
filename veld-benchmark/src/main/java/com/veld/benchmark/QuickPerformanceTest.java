package com.veld.benchmark;

import com.veld.benchmark.common.Service;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.veld.VeldSimpleService;
import com.veld.runtime.VeldContainer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Quick performance comparison test.
 * Compares Veld static access (like Dagger) vs Veld container vs Dagger.
 */
public class QuickPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 500_000;
    private static final int MEASURE_ITERATIONS = 10_000_000;
    
    public static void main(String[] args) throws Throwable {
        System.out.println("=== Quick Performance Test ===\n");
        
        // Initialize
        System.out.println("Initializing...");
        
        VeldContainer veldContainer = new VeldContainer();
        BenchmarkComponent daggerComponent = DaggerBenchmarkComponent.create();
        
        // Get Veld static accessor via MethodHandle
        MethodHandle veldStaticGetter = null;
        try {
            Class<?> veldClass = Class.forName("com.veld.generated.Veld");
            veldStaticGetter = MethodHandles.lookup()
                .findStatic(veldClass, "veldSimpleService", 
                    MethodType.methodType(VeldSimpleService.class));
            veldStaticGetter.invoke(); // Warmup
            System.out.println("Veld static method found: Veld.veldSimpleService()");
        } catch (Exception e) {
            System.out.println("Veld static accessor not available: " + e.getMessage());
        }
        
        // Warmup
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            veldContainer.get(VeldSimpleService.class);
            if (veldStaticGetter != null) veldStaticGetter.invoke();
            daggerComponent.simpleService();
        }
        
        // Measure Veld container.get()
        System.out.println("Measuring Veld container.get() (" + MEASURE_ITERATIONS + " iterations)...");
        long containerStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = veldContainer.get(VeldSimpleService.class);
        }
        long containerTime = System.nanoTime() - containerStart;
        
        // Measure Veld static access
        long staticTime = 0;
        if (veldStaticGetter != null) {
            System.out.println("Measuring Veld.veldSimpleService() (" + MEASURE_ITERATIONS + " iterations)...");
            long staticStart = System.nanoTime();
            for (int i = 0; i < MEASURE_ITERATIONS; i++) {
                VeldSimpleService s = (VeldSimpleService) veldStaticGetter.invokeExact();
            }
            staticTime = System.nanoTime() - staticStart;
        }
        
        // Measure Dagger  
        System.out.println("Measuring Dagger (" + MEASURE_ITERATIONS + " iterations)...");
        long daggerStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = daggerComponent.simpleService();
        }
        long daggerTime = System.nanoTime() - daggerStart;
        
        // Results
        System.out.println("\n=== Results ===");
        double containerNsPerOp = (double) containerTime / MEASURE_ITERATIONS;
        double staticNsPerOp = staticTime > 0 ? (double) staticTime / MEASURE_ITERATIONS : 0;
        double daggerNsPerOp = (double) daggerTime / MEASURE_ITERATIONS;
        
        System.out.printf("Veld container.get(): %.2f ns/op (%.0f K ops/ms)%n", 
            containerNsPerOp, 1_000_000.0 / containerNsPerOp / 1000.0);
        if (staticNsPerOp > 0) {
            System.out.printf("Veld static access:   %.2f ns/op (%.0f K ops/ms)%n", 
                staticNsPerOp, 1_000_000.0 / staticNsPerOp / 1000.0);
        }
        System.out.printf("Dagger:               %.2f ns/op (%.0f K ops/ms)%n", 
            daggerNsPerOp, 1_000_000.0 / daggerNsPerOp / 1000.0);
        
        System.out.println("\n=== Comparison vs Dagger ===");
        double ratioContainer = containerNsPerOp / daggerNsPerOp;
        
        if (ratioContainer > 1.0) {
            System.out.printf("Veld container.get(): Dagger is %.2fx faster%n", ratioContainer);
        } else {
            System.out.printf("Veld container.get(): %.2fx FASTER than Dagger!%n", 1/ratioContainer);
        }
        
        if (staticNsPerOp > 0) {
            double ratioStatic = staticNsPerOp / daggerNsPerOp;
            if (ratioStatic > 1.0) {
                System.out.printf("Veld static access:   Dagger is %.2fx faster%n", ratioStatic);
            } else {
                System.out.printf("Veld static access:   %.2fx FASTER than Dagger!%n", 1/ratioStatic);
            }
        }
        
        veldContainer.close();
    }
}
