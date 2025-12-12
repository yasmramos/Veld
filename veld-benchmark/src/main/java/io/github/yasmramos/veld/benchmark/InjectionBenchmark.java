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
import io.github.yasmramos.veld.benchmark.common.ComplexService;
import io.github.yasmramos.veld.benchmark.common.DeepService;
import io.github.yasmramos.veld.benchmark.common.Logger;
import io.github.yasmramos.veld.benchmark.common.Service;
import io.github.yasmramos.veld.benchmark.dagger.BenchmarkComponent;
import io.github.yasmramos.veld.benchmark.dagger.DaggerBenchmarkComponent;
import io.github.yasmramos.veld.benchmark.guice.GuiceModule;
import io.github.yasmramos.veld.benchmark.spring.SpringConfig;
import io.github.yasmramos.veld.benchmark.veld.VeldComplexService;
import io.github.yasmramos.veld.benchmark.veld.VeldLogger;
import io.github.yasmramos.veld.benchmark.veld.VeldSimpleService;
import io.github.yasmramos.veld.generated.Veld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks measuring dependency injection/lookup time.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class InjectionBenchmark {
    
    private AnnotationConfigApplicationContext springContext;
    private Injector guiceInjector;
    private BenchmarkComponent daggerComponent;
    
    @Setup(Level.Trial)
    public void setup() {
        springContext = new AnnotationConfigApplicationContext(SpringConfig.class);
        guiceInjector = Guice.createInjector(new GuiceModule());
        daggerComponent = DaggerBenchmarkComponent.create();
    }
    
    @TearDown(Level.Trial)
    public void teardown() {
        if (springContext != null) springContext.close();
    }
    
    // ==================== SIMPLE INJECTION ====================
    
    @Benchmark
    public void veldSimpleInjection(Blackhole bh) {
        Service service = Veld.veldSimpleService();
        bh.consume(service);
    }
    
    @Benchmark
    public void springSimpleInjection(Blackhole bh) {
        Service service = springContext.getBean("simpleService", Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void guiceSimpleInjection(Blackhole bh) {
        Service service = guiceInjector.getInstance(Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerSimpleInjection(Blackhole bh) {
        Service service = daggerComponent.simpleService();
        bh.consume(service);
    }
    
    // ==================== COMPLEX INJECTION ====================
    
    @Benchmark
    public void veldComplexInjection(Blackhole bh) {
        Service service = Veld.veldComplexService();
        bh.consume(service);
    }
    
    @Benchmark
    public void springComplexInjection(Blackhole bh) {
        ComplexService service = springContext.getBean(ComplexService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void guiceComplexInjection(Blackhole bh) {
        ComplexService service = guiceInjector.getInstance(ComplexService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerComplexInjection(Blackhole bh) {
        ComplexService service = daggerComponent.complexService();
        bh.consume(service);
    }
    
    // ==================== DEEP INJECTION ====================
    
    @Benchmark
    public void springDeepInjection(Blackhole bh) {
        DeepService service = springContext.getBean(DeepService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void guiceDeepInjection(Blackhole bh) {
        DeepService service = guiceInjector.getInstance(DeepService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerDeepInjection(Blackhole bh) {
        DeepService service = daggerComponent.deepService();
        bh.consume(service);
    }
    
    // ==================== LOGGER LOOKUP ====================
    
    @Benchmark
    public void veldLoggerLookup(Blackhole bh) {
        Logger logger = Veld.veldLogger();
        bh.consume(logger);
    }
    
    @Benchmark
    public void springLoggerLookup(Blackhole bh) {
        Logger logger = springContext.getBean(Logger.class);
        bh.consume(logger);
    }
    
    @Benchmark
    public void guiceLoggerLookup(Blackhole bh) {
        Logger logger = guiceInjector.getInstance(Logger.class);
        bh.consume(logger);
    }
    
    @Benchmark
    public void daggerLoggerLookup(Blackhole bh) {
        Logger logger = daggerComponent.logger();
        bh.consume(logger);
    }
}
