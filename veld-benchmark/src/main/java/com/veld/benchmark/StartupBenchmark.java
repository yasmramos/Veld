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
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks measuring container startup time.
 * 
 * This measures the time to create and initialize a DI container,
 * which is critical for application startup performance and serverless functions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class StartupBenchmark {
    
    // ==================== VELD ====================
    
    @Benchmark
    public void veldStartup(Blackhole bh) {
        VeldContainer container = VeldContainer.create();
        bh.consume(container);
    }
    
    // ==================== SPRING ====================
    
    @Benchmark
    public void springStartup(Blackhole bh) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
        bh.consume(context);
        context.close();
    }
    
    // ==================== GUICE ====================
    
    @Benchmark
    public void guiceStartup(Blackhole bh) {
        Injector injector = Guice.createInjector(new GuiceModule());
        bh.consume(injector);
    }
    
    // ==================== DAGGER ====================
    
    @Benchmark
    public void daggerStartup(Blackhole bh) {
        BenchmarkComponent component = DaggerBenchmarkComponent.create();
        bh.consume(component);
    }
}
