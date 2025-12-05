/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.guice.GuiceModule;
import com.veld.benchmark.spring.SpringConfig;
import com.veld.runtime.VeldContainer;
import org.openjdk.jmh.annotations.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring memory footprint.
 * 
 * This creates multiple container instances to measure memory consumption.
 * Run with: -prof gc to see GC statistics.
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 3, jvmArgs = {"-Xms256m", "-Xmx256m", "-XX:+UseG1GC"})
@Warmup(iterations = 0)
@Measurement(iterations = 1)
public class MemoryBenchmark {
    
    @Param({"10", "100", "500"})
    private int containerCount;
    
    // ==================== VELD ====================
    
    @Benchmark
    public Object[] veldMemory() {
        Object[] containers = new Object[containerCount];
        for (int i = 0; i < containerCount; i++) {
            containers[i] = VeldContainer.create();
        }
        return containers;
    }
    
    // ==================== SPRING ====================
    
    @Benchmark
    public Object[] springMemory() {
        Object[] contexts = new Object[containerCount];
        for (int i = 0; i < containerCount; i++) {
            contexts[i] = new AnnotationConfigApplicationContext(SpringConfig.class);
        }
        // Clean up
        for (Object ctx : contexts) {
            ((AnnotationConfigApplicationContext) ctx).close();
        }
        return contexts;
    }
    
    // ==================== GUICE ====================
    
    @Benchmark
    public Object[] guiceMemory() {
        Object[] injectors = new Object[containerCount];
        for (int i = 0; i < containerCount; i++) {
            injectors[i] = Guice.createInjector(new GuiceModule());
        }
        return injectors;
    }
    
    // ==================== DAGGER ====================
    
    @Benchmark
    public Object[] daggerMemory() {
        Object[] components = new Object[containerCount];
        for (int i = 0; i < containerCount; i++) {
            components[i] = DaggerBenchmarkComponent.create();
        }
        return components;
    }
}
