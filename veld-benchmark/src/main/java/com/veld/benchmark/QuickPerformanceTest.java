package com.veld.benchmark;

import com.veld.benchmark.common.Service;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.veld.VeldSimpleService;
import com.veld.runtime.VeldContainer;

/**
 * Quick performance comparison test (not using JMH).
 */
public class QuickPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 500_000;
    private static final int MEASURE_ITERATIONS = 10_000_000;
    
    public static void main(String[] args) {
        System.out.println("=== Quick Performance Test ===\n");
        
        // Initialize containers
        System.out.println("Initializing containers...");
        
        long startVeld = System.nanoTime();
        VeldContainer veldContainer = new VeldContainer();
        long veldInitTime = System.nanoTime() - startVeld;
        
        long startDagger = System.nanoTime();
        BenchmarkComponent daggerComponent = DaggerBenchmarkComponent.create();
        long daggerInitTime = System.nanoTime() - startDagger;
        
        System.out.printf("Veld init: %.2f ms%n", veldInitTime / 1_000_000.0);
        System.out.printf("Dagger init: %.2f ms%n%n", daggerInitTime / 1_000_000.0);
        
        // Warmup
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            veldContainer.get(VeldSimpleService.class);
            daggerComponent.simpleService();
        }
        
        // Measure Veld
        System.out.println("Measuring Veld get() (" + MEASURE_ITERATIONS + " iterations)...");
        long veldStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = veldContainer.get(VeldSimpleService.class);
        }
        long veldTime = System.nanoTime() - veldStart;
        
        // Measure Dagger  
        System.out.println("Measuring Dagger (" + MEASURE_ITERATIONS + " iterations)...");
        long daggerStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = daggerComponent.simpleService();
        }
        long daggerTime = System.nanoTime() - daggerStart;
        
        // Results
        System.out.println("\n=== Results ===");
        double veldNsPerOp = (double) veldTime / MEASURE_ITERATIONS;
        double daggerNsPerOp = (double) daggerTime / MEASURE_ITERATIONS;
        
        System.out.printf("Veld get():  %.2f ns/op (%.0f K ops/ms)%n", 
            veldNsPerOp, 1_000_000.0 / veldNsPerOp / 1000.0);
        System.out.printf("Dagger:      %.2f ns/op (%.0f K ops/ms)%n", 
            daggerNsPerOp, 1_000_000.0 / daggerNsPerOp / 1000.0);
        
        System.out.println("\n=== Comparison ===");
        double ratio = veldNsPerOp / daggerNsPerOp;
        
        if (ratio > 1.0) {
            System.out.printf("Dagger is %.2fx faster%n", ratio);
        } else {
            System.out.printf("Veld is %.2fx FASTER than Dagger!%n", 1/ratio);
        }
        
        veldContainer.close();
    }
}
