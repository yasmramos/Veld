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
import io.github.yasmramos.veld.benchmark.common.Service;
import io.github.yasmramos.veld.benchmark.dagger.DaggerPrototypeComponent;
import io.github.yasmramos.veld.benchmark.dagger.PrototypeComponent;
import io.github.yasmramos.veld.benchmark.guice.GuicePrototypeModule;
import io.github.yasmramos.veld.benchmark.spring.SpringPrototypeConfig;
import io.github.yasmramos.veld.benchmark.veld.VeldPrototypeService;
import io.github.yasmramos.veld.generated.Veld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks measuring prototype (new instance) creation time.
 * 
 * Note: Veld prototype creates new instance each time via static method.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class PrototypeBenchmark {
    
    private AnnotationConfigApplicationContext springContext;
    private Injector guiceInjector;
    private PrototypeComponent daggerComponent;
    
    @Setup(Level.Trial)
    public void setup() {
        springContext = new AnnotationConfigApplicationContext(SpringPrototypeConfig.class);
        guiceInjector = Guice.createInjector(new GuicePrototypeModule());
        daggerComponent = DaggerPrototypeComponent.create();
    }
    
    @TearDown(Level.Trial)
    public void teardown() {
        if (springContext != null) springContext.close();
    }
    
    // ==================== SIMPLE PROTOTYPE ====================
    
    @Benchmark
    public void veldPrototypeSimple(Blackhole bh) {
        // For prototypes, Veld generates a factory method
        // Using direct instantiation to match prototype behavior
        Service service = new VeldPrototypeService(Veld.veldLogger());
        bh.consume(service);
    }
    
    @Benchmark
    public void springPrototypeSimple(Blackhole bh) {
        Service service = springContext.getBean("prototypeSimpleService", Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void guicePrototypeSimple(Blackhole bh) {
        Service service = guiceInjector.getInstance(Service.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerPrototypeSimple(Blackhole bh) {
        Service service = daggerComponent.simpleService();
        bh.consume(service);
    }
    
    // ==================== COMPLEX PROTOTYPE ====================
    
    @Benchmark
    public void springPrototypeComplex(Blackhole bh) {
        ComplexService service = springContext.getBean("prototypeComplexService", ComplexService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void guicePrototypeComplex(Blackhole bh) {
        ComplexService service = guiceInjector.getInstance(ComplexService.class);
        bh.consume(service);
    }
    
    @Benchmark
    public void daggerPrototypeComplex(Blackhole bh) {
        ComplexService service = daggerComponent.complexService();
        bh.consume(service);
    }
}
