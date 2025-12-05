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

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Main runner for all Veld benchmarks.
 * 
 * Usage:
 *   java -jar veld-benchmark.jar                    # Run all benchmarks
 *   java -jar veld-benchmark.jar Startup            # Run startup benchmarks only
 *   java -jar veld-benchmark.jar -h                 # Show help
 * 
 * Profiles:
 *   Quick: -f 1 -wi 1 -i 2
 *   Normal: -f 2 -wi 3 -i 5
 *   Full: -f 5 -wi 5 -i 10
 */
public class BenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException {
        String include = ".*Benchmark.*";
        if (args.length > 0 && !args[0].startsWith("-")) {
            include = ".*" + args[0] + ".*";
        }
        
        Options opts = new OptionsBuilder()
                .include(include)
                .forks(2)
                .warmupIterations(3)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark-results.json")
                .build();
        
        System.out.println("=".repeat(60));
        System.out.println("  Veld Framework Benchmark Suite");
        System.out.println("  Comparing: Veld vs Spring vs Guice vs Dagger");
        System.out.println("=".repeat(60));
        System.out.println();
        
        new Runner(opts).run();
        
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  Results saved to: benchmark-results.json");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Run quick benchmarks for development testing.
     */
    public static void runQuick() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(".*Benchmark.*")
                .forks(1)
                .warmupIterations(1)
                .warmupTime(TimeValue.milliseconds(500))
                .measurementIterations(2)
                .measurementTime(TimeValue.milliseconds(500))
                .build();
        
        new Runner(opts).run();
    }
    
    /**
     * Run full comprehensive benchmarks.
     */
    public static void runFull() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(".*Benchmark.*")
                .forks(5)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(2))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(2))
                .resultFormat(ResultFormatType.JSON)
                .result("benchmark-results-full.json")
                .build();
        
        new Runner(opts).run();
    }
}
