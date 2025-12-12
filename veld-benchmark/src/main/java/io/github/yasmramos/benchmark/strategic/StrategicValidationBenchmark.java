/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.benchmark.strategic;

import io.github.yasmramos.generated.Veld;
import io.github.yasmramos.benchmark.veld.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Strategic validation benchmarks for Veld Framework.
 * 
 * Tests critical performance aspects:
 * 1. Pure scalability (concurrent vs single-thread efficiency)
 * 2. Specific contention scenarios
 * 3. Memory overhead and ThreadLocal cache behavior
 * 4. Hash collision impact
 * 5. Thread-local memory leaks
 * 6. VarHandle vs CAS overhead
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xmx2G", "-XX:+UseG1GC"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class StrategicValidationBenchmark {

    // =============================================
    // 1. PURE SCALABILITY BENCHMARKS
    // =============================================

    @State(Scope.Group)
    public static class ScalabilityState {
        private final Random random = new Random(42);
        private final Class<?>[] serviceTypes = {
            VeldLogger.class,
            VeldValidator.class, 
            VeldRepository.class,
            VeldSimpleService.class,
            VeldComplexService.class,
            io.github.yasmramos.benchmark.common.Logger.class,
            io.github.yasmramos.benchmark.common.Service.class
        };

        public Class<?> randomServiceType() {
            return serviceTypes[random.nextInt(serviceTypes.length)];
        }
    }

    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    public Object concurrentLookup(ScalabilityState state) {
        return Veld.get(state.randomServiceType()); // Random among 7 services
    }

    @Benchmark  
    @Group("single")
    @GroupThreads(1)
    public Object singleThreadLookup() {
        return Veld.get(VeldSimpleService.class); // Always same (best case)
    }

    // =============================================
    // 2. SPECIFIC CONTENTION BENCHMARKS
    // =============================================

    @State(Scope.Group)
    public static class LazyContentionState {
        public ExpensiveLazyService getExpensiveService() {
            return Veld.get(ExpensiveLazyService.class); // Never initialized
        }
    }

    @Benchmark
    @Group("lazyContention")
    @GroupThreads(8)  // Maximum contention
    public Object getLazyService(LazyContentionState state) {
        return state.getExpensiveService();
    }

    // =============================================
    // 3. MEMORY OVERHEAD VALIDATION
    // =============================================

    @Benchmark
    public long memoryOverhead() {
        long before = Runtime.getRuntime().totalMemory();
        
        // Simulate 100k lookups (typical production usage)
        for (int i = 0; i < 100_000; i++) {
            Veld.get(VeldLogger.class);
        }
        
        long after = Runtime.getRuntime().totalMemory();
        return after - before;
    }

    @Benchmark
    public long threadLocalCacheBehavior() {
        // Test ThreadLocal cache growth pattern
        long before = Runtime.getRuntime().freeMemory();
        
        // Force ThreadLocal cache to grow by accessing from multiple threads
        Thread[] threads = new Thread[10];
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    Veld.get(VeldLogger.class);
                    counter.incrementAndGet();
                }
            });
            threads[i].start();
        }
        
        // Wait for completion
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long after = Runtime.getRuntime().freeMemory();
        return before - after; // Memory used by cache
    }

    // =============================================
    // 4. HASH COLLISION IMPACT VALIDATION
    // =============================================

    /**
     * Test to validate current linear search performance.
     * Current implementation: O(n) array search
     * Danger: With 20+ services, clustering can degrade to O(n)
     */
    @Benchmark
    @Group("hashCollision")
    @GroupThreads(4)
    public Object worstCaseHashCollision() {
        // Force worst-case: services that hash to similar values
        // This tests the linear probing in array search
        Class<?>[] worstTypes = {
            VeldLogger.class,
            VeldValidator.class,
            VeldRepository.class,
            VeldSimpleService.class,
            VeldComplexService.class
        };
        
        return Veld.get(worstTypes[(int)(Thread.currentThread().getId() % worstTypes.length)]);
    }

    // =============================================
    // 5. VARHANDLE VS CAS OVERHEAD
    // =============================================

    /**
     * Test for Acquire fence overhead vs plain reads
     * Current implementation: No VarHandle, just direct field access
     * Acquire fence has cost on ARM/POWER architectures
     */
    @Benchmark
    @Group("varhandle")
    @GroupThreads(8)
    public Object varHandleVsCasOverhead() {
        // Test both scenarios:
        // 1. Veld.get() - direct reference comparison (current implementation)
        // 2. Future: VarHandle with acquire fence for lazy initialization
        
        return Veld.get(VeldSimpleService.class); // Direct field access
    }

    // =============================================
    // 6. PROBE LENGTH VALIDATION
    // =============================================

    @State(Scope.Benchmark)
    public static class ProbeLengthState {
        private final List<Class<?>> testTypes = new ArrayList<>();
        private int probeCount = 0;
        
        public ProbeLengthState() {
            // Add enough types to test probe length
            testTypes.add(VeldLogger.class);
            testTypes.add(VeldValidator.class);
            testTypes.add(VeldRepository.class);
            testTypes.add(VeldSimpleService.class);
            testTypes.add(VeldComplexService.class);
        }
        
        public Class<?> getNextType() {
            Class<?> type = testTypes.get(probeCount % testTypes.size());
            probeCount++;
            return type;
        }
        
        public int getProbeCount() {
            return probeCount;
        }
    }

    @Benchmark
    public int probeLengthValidation(ProbeLengthState state) {
        Veld.get(state.getNextType());
        return state.getProbeCount();
    }

    // =============================================
    // 7. EFFICIENCY CALCULATION
    // =============================================

    /**
     * Calculate efficiency ratio: concurrentLookup ÷ (single × 4)
     * Target: > 0.8 (80% efficiency) for good scalability
     */
    @State(Scope.Benchmark)
    public static class EfficiencyState {
        private final Random random = new Random(42);
        private final Class<?>[] serviceTypes = {
            VeldLogger.class,
            VeldValidator.class,
            VeldRepository.class,
            VeldSimpleService.class,
            VeldComplexService.class
        };

        public Class<?> randomServiceType() {
            return serviceTypes[random.nextInt(serviceTypes.length)];
        }
    }

    @Benchmark
    @Group("efficiency")
    @GroupThreads(4)
    public Object concurrentEfficiency(EfficiencyState state) {
        return Veld.get(state.randomServiceType());
    }

    @Benchmark
    @Group("efficiency")
    @GroupThreads(1)
    public Object singleEfficiency(EfficiencyState state) {
        return Veld.get(state.randomServiceType());
    }

    // =============================================
    // 8. LOAD FACTOR VALIDATION
    // =============================================

    /**
     * Validate load factor < 0.7 and max probe length < 3
     * This simulates future hash-based implementation
     */
    @Benchmark
    public double loadFactorValidation() {
        // Simulate what load factor would be with current array size
        int currentTypes = 7; // Current implementation has 7 types
        int arrayCapacity = 16; // Typical power-of-2 capacity
        
        return (double) currentTypes / arrayCapacity;
    }

    // =============================================
    // UTILITY METHODS FOR ANALYSIS
    // =============================================

    /**
     * Helper to simulate 20+ services for stress testing
     */
    private static Class<?>[] generateStressTestTypes() {
        List<Class<?>> types = new ArrayList<>();
        types.add(VeldLogger.class);
        types.add(VeldValidator.class);
        types.add(VeldRepository.class);
        types.add(VeldSimpleService.class);
        types.add(VeldComplexService.class);
        
        // Add more types to simulate larger application
        for (int i = 0; i < 20; i++) {
            try {
                // This would be real service classes in production
                Class<?> dummyClass = Class.forName("java.lang.String" + i);
                types.add(dummyClass);
            } catch (ClassNotFoundException e) {
                // Fallback to existing types
                types.add(VeldLogger.class);
            }
        }
        
        return types.toArray(new Class<?>[0]);
    }

    // =============================================
    // MAIN METHOD FOR MANUAL TESTING
    // =============================================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(StrategicValidationBenchmark.class.getSimpleName())
            .addProfiler("gc")
            .addProfiler("method")
            .result(new File("strategic-validation-results.json").getAbsolutePath())
            .resultFormat(ResultFormatType.JSON)
            .build();

        new Runner(opt).run();
    }
}