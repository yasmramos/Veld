/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.features.injection;

import io.github.yasmramos.veld.Veld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark for measuring the performance overhead of different injection types
 * in the Veld framework. This benchmark compares constructor injection, field injection,
 * and method injection to understand their relative performance characteristics.
 *
 * <p>The goal is to measure the raw overhead of each injection mechanism during
 * bean creation and dependency resolution.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class InjectionTypeBenchmark {

    /**
     * Measures the baseline performance of getting a bean with no dependencies.
     * This establishes the baseline overhead of the bean retrieval mechanism.
     */
    @Benchmark
    public void baselineNoInjection(Blackhole bh) {
        NoInjectionTestBean bean = Veld.get(NoInjectionTestBean.class);
        bh.consume(bean);
    }

    /**
     * Measures the performance of field injection (default injection type in Veld).
     * Field injection requires the framework to resolve dependencies and set
     * the field values via reflection.
     */
    @Benchmark
    public void fieldInjection(Blackhole bh) {
        FieldInjectionTestBean bean = Veld.get(FieldInjectionTestBean.class);
        bh.consume(bean);
    }

    /**
     * Measures the performance of constructor injection.
     * Constructor injection requires the framework to resolve dependencies
     * and invoke the constructor with the resolved values.
     */
    @Benchmark
    public void constructorInjection(Blackhole bh) {
        ConstructorInjectionTestBean bean = Veld.get(ConstructorInjectionTestBean.class);
        bh.consume(bean);
    }

    /**
     * Measures the performance of method injection.
     * Method injection requires the framework to resolve dependencies
     * and invoke the annotated method with the resolved values.
     */
    @Benchmark
    public void methodInjection(Blackhole bh) {
        MethodInjectionTestBean bean = Veld.get(MethodInjectionTestBean.class);
        bh.consume(bean);
    }
}
