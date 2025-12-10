package com.veld.benchmark;

import com.veld.benchmark.common.Service;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.veld.VeldSimpleService;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Quick performance comparison test.
 * Compares Veld static access vs Dagger.
 */
public class QuickPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 500_000;
    private static final int MEASURE_ITERATIONS = 10_000_000;
    
    public static void main(String[] args) throws Throwable {
        System.out.println("=== Quick Performance Test ===\n");
        
        // Initialize
        System.out.println("Initializing...");
        
        BenchmarkComponent daggerComponent = DaggerBenchmarkComponent.create();
        
        // Get Veld static accessor via MethodHandle
        MethodHandle veldStaticGetter = null;
        MethodHandle veldGetByClass = null;
        try {
            Class<?> veldClass = Class.forName("com.veld.generated.Veld");
            veldStaticGetter = MethodHandles.lookup()
                .findStatic(veldClass, "veldSimpleService", 
                    MethodType.methodType(VeldSimpleService.class));
            veldGetByClass = MethodHandles.lookup()
                .findStatic(veldClass, "get",
                    MethodType.methodType(Object.class, Class.class));
            veldStaticGetter.invoke(); // Warmup
            System.out.println("Veld static method found: Veld.veldSimpleService()");
        } catch (Exception e) {
            System.out.println("Veld static accessor not available: " + e.getMessage());
        }
        
        if (veldStaticGetter == null) {
            System.out.println("ERROR: Veld class not found. Cannot run benchmark.");
            return;
        }
        
        // Warmup
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            veldStaticGetter.invoke();
            veldGetByClass.invoke(VeldSimpleService.class);
            daggerComponent.simpleService();
        }
        
        // Measure Veld.get(Class)
        System.out.println("Measuring Veld.get(Class) (" + MEASURE_ITERATIONS + " iterations)...");
        long getByClassStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = (Service) veldGetByClass.invoke(VeldSimpleService.class);
        }
        long getByClassTime = System.nanoTime() - getByClassStart;
        
        // Measure Veld static access
        System.out.println("Measuring Veld.veldSimpleService() (" + MEASURE_ITERATIONS + " iterations)...");
        long staticStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            VeldSimpleService s = (VeldSimpleService) veldStaticGetter.invokeExact();
        }
        long staticTime = System.nanoTime() - staticStart;
        
        // Measure Dagger  
        System.out.println("Measuring Dagger (" + MEASURE_ITERATIONS + " iterations)...");
        long daggerStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Service s = daggerComponent.simpleService();
        }
        long daggerTime = System.nanoTime() - daggerStart;
        
        // Results
        System.out.println("\n=== Results ===");
        double getByClassNsPerOp = (double) getByClassTime / MEASURE_ITERATIONS;
        double staticNsPerOp = (double) staticTime / MEASURE_ITERATIONS;
        double daggerNsPerOp = (double) daggerTime / MEASURE_ITERATIONS;
        
        System.out.printf("Veld.get(Class):    %.2f ns/op (%.0f K ops/ms)%n", 
            getByClassNsPerOp, 1_000_000.0 / getByClassNsPerOp / 1000.0);
        System.out.printf("Veld static access: %.2f ns/op (%.0f K ops/ms)%n", 
            staticNsPerOp, 1_000_000.0 / staticNsPerOp / 1000.0);
        System.out.printf("Dagger:             %.2f ns/op (%.0f K ops/ms)%n", 
            daggerNsPerOp, 1_000_000.0 / daggerNsPerOp / 1000.0);
        
        System.out.println("\n=== Comparison vs Dagger ===");
        double ratioGetByClass = getByClassNsPerOp / daggerNsPerOp;
        double ratioStatic = staticNsPerOp / daggerNsPerOp;
        
        if (ratioGetByClass > 1.0) {
            System.out.printf("Veld.get(Class):    Dagger is %.2fx faster%n", ratioGetByClass);
        } else {
            System.out.printf("Veld.get(Class):    %.2fx FASTER than Dagger!%n", 1/ratioGetByClass);
        }
        
        if (ratioStatic > 1.0) {
            System.out.printf("Veld static access: Dagger is %.2fx faster%n", ratioStatic);
        } else {
            System.out.printf("Veld static access: %.2fx FASTER than Dagger!%n", 1/ratioStatic);
        }
    }
}
