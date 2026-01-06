/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.features;

import io.github.yasmramos.veld.Veld;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Base benchmark class for Veld feature benchmarks.
 * Provides common configuration and lifecycle management.
 *
 * <p>Note: Veld uses compile-time bytecode generation, so all beans are
 * automatically registered. No manual registration is required.</p>
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public abstract class BaseFeatureBenchmark {

    @Setup
    public void setup() {
        // Veld is initialized automatically at compile-time
        // Just verify it's available
    }

    @TearDown
    public void tearDown() {
        // Veld shutdown is handled automatically
    }
}
