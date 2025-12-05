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
import com.veld.benchmark.common.Service;
import com.veld.benchmark.dagger.BenchmarkComponent;
import com.veld.benchmark.dagger.DaggerBenchmarkComponent;
import com.veld.benchmark.guice.GuiceModule;
import com.veld.benchmark.spring.SpringConfig;
import com.veld.benchmark.veld.VeldSimpleService;
import com.veld.runtime.VeldContainer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks measuring throughput (operations per second).
 * 
 * This measures how many dependency lookups can be performed per second,
 * which is important for high-throughput applications.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ThroughputBenchmark {
    
    private VeldContainer veldContainer;
    private AnnotationConfigApplicationContext springContext;
    private Injector guiceInjector;
    private BenchmarkComponent daggerComponent;
    
    @Setup(Level.Trial)
    public void setup() {
        veldContainer = VeldContainer.create();
        springContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        guiceInjector = Guice.createInjector(new GuiceModule());
        daggerComponent = DaggerBenchmarkComponent.create();
    }
    
    @TearDown(Level.Trial)
    public void teardown() {
        if (springContext != null) {
            springContext.close();
        }
    }
    
    // ==================== THROUGHPUT BENCHMARKS ====================
    
    @Benchmark
    public void veldThroughput(Blackhole bh) {
        Service service = veldContainer.get(VeldSimpleService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void springThroughput(Blackhole bh) {
        Service service = springContext.getBean(Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void guiceThroughput(Blackhole bh) {
        Service service = guiceInjector.getInstance(Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerThroughput(Blackhole bh) {
        Service service = daggerComponent.simpleService();
        bh.consume(service);
    }
    
    // ==================== CONCURRENT THROUGHPUT ====================
    
    @Benchmark
    @Threads(4)
    public void veldConcurrentThroughput(Blackhole bh) {
        Service service = veldContainer.get(VeldSimpleService.class);
        bh.consume(service);
    }
    
    @Benchmark
    @Threads(4)
    public void springConcurrentThroughput(Blackhole bh) {
        Service service = springContext.getBean(Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    @Threads(4)
    public void guiceConcurrentThroughput(Blackhole bh) {
        Service service = guiceInjector.getInstance(Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    @Threads(4)
    public void daggerConcurrentThroughput(Blackhole bh) {
        Service service = daggerComponent.simpleService();
        bh.consume(service);
    }
}
