package com.veld.benchmark;

import com.veld.benchmark.common.Service;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.veld.VeldBenchmarkHelper;
import com.veld.benchmark.veld.VeldSimpleService;
import com.veld.runtime.VeldContainer;

/**
 * Quick performance comparison test (not using JMH).
 * Runs lightweight loops to compare raw performance.
 */
public class QuickPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 500_000;
    private static final int MEASURE_ITERATIONS = 10_000_000;
    
    public static void main(String[] args) {
        System.out.println("=== Quick Performance Test ===\n");
        
        // Initialize containers
        System.out.println("Initializing containers...");
        
        long startVeld = System.nanoTime();
        VeldContainer veldContainer = VeldBenchmarkHelper.createSimpleContainer();
        long veldInitTime = System.nanoTime() - startVeld;
        
        long startDagger = System.nanoTime();
        BenchmarkComponent daggerComponent = DaggerBenchmarkComponent.create();
        long daggerInitTime = System.nanoTime() - startDagger;
        
        System.out.printf("Veld init: %.2f ms%n", veldInitTime / 1_000_000.0);
        System.out.printf("Dagger init: %.2f ms%n%n", daggerInitTime / 1_000_000.0);
        
        // Get index for ultra-fast access
        int serviceIndex = veldContainer.indexFor(VeldSimpleService.class);
        System.out.println("Veld service index: " + serviceIndex);
        
        // Warmup all paths
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            veldContainer.get(VeldSimpleService.class);
            veldContainer.fastGet(serviceIndex);
            daggerComponent.simpleService();
        }
        
        // Measure Veld standard get()
        System.out.println("Measuring Veld get(Class) (" + MEASURE_ITERATIONS + " iterations)...");
        long veldStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = veldContainer.get(VeldSimpleService.class);
        }
        long veldTime = System.nanoTime() - veldStart;
        
        // Measure Veld fastGet(index)
        System.out.println("Measuring Veld fastGet(index) (" + MEASURE_ITERATIONS + " iterations)...");
        long veldFastStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = veldContainer.fastGet(serviceIndex);
        }
        long veldFastTime = System.nanoTime() - veldFastStart;
        
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
        double veldFastNsPerOp = (double) veldFastTime / MEASURE_ITERATIONS;
        double daggerNsPerOp = (double) daggerTime / MEASURE_ITERATIONS;
        
        System.out.printf("Veld get(Class):     %.2f ns/op (%.0f K ops/ms)%n", 
            veldNsPerOp, 1_000_000.0 / veldNsPerOp / 1000.0);
        System.out.printf("Veld fastGet(index): %.2f ns/op (%.0f K ops/ms)%n", 
            veldFastNsPerOp, 1_000_000.0 / veldFastNsPerOp / 1000.0);
        System.out.printf("Dagger:              %.2f ns/op (%.0f K ops/ms)%n", 
            daggerNsPerOp, 1_000_000.0 / daggerNsPerOp / 1000.0);
        
        System.out.println("\n=== Comparison ===");
        double ratioStd = veldNsPerOp / daggerNsPerOp;
        double ratioFast = veldFastNsPerOp / daggerNsPerOp;
        
        if (ratioStd > 1.0) {
            System.out.printf("Veld get(Class): Dagger is %.2fx faster%n", ratioStd);
        } else {
            System.out.printf("Veld get(Class): %.2fx FASTER than Dagger!%n", 1/ratioStd);
        }
        
        if (ratioFast > 1.0) {
            System.out.printf("Veld fastGet():  Dagger is %.2fx faster%n", ratioFast);
        } else {
            System.out.printf("Veld fastGet():  %.2fx FASTER than Dagger!%n", 1/ratioFast);
        }
        
        // Cleanup
        veldContainer.close();
    }
}
