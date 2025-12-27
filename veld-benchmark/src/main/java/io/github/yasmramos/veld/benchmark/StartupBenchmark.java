/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.yasmramos.veld.benchmark.dagger.BenchmarkComponent;
import io.github.yasmramos.veld.benchmark.dagger.DaggerBenchmarkComponent;
import io.github.yasmramos.veld.benchmark.guice.GuiceModule;
import io.github.yasmramos.veld.benchmark.spring.SpringConfig;
import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.benchmark.veld.VeldSimpleService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks measuring container startup time.
 * 
 * For Veld, "startup" is just accessing the static class (class loading).
 * All initialization happens in the static initializer block.
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
        // Veld startup = accessing static methods (class already loaded)
        // In real app, first access triggers class loading + static init
        bh.consume(Veld.get(VeldSimpleService.class));
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
