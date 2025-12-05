/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Veld Benchmark Module.
 * 
 * This module provides performance benchmarks comparing Veld with other
 * popular DI frameworks: Spring, Guice, and Dagger.
 * 
 * <h2>Benchmarks Included</h2>
 * <ul>
 *   <li>StartupBenchmark - Container initialization time</li>
 *   <li>InjectionBenchmark - Dependency lookup time</li>
 *   <li>PrototypeBenchmark - New instance creation time</li>
 *   <li>ThroughputBenchmark - Operations per second</li>
 *   <li>MemoryBenchmark - Memory footprint</li>
 * </ul>
 * 
 * <h2>Running Benchmarks</h2>
 * <pre>
 * # Build
 * mvn clean package -pl veld-benchmark -am
 * 
 * # Run all benchmarks
 * java -jar veld-benchmark/target/veld-benchmark.jar
 * 
 * # Run specific benchmark
 * java -jar veld-benchmark/target/veld-benchmark.jar Startup
 * </pre>
 */
module com.veld.benchmark {
    // Veld modules
    requires com.veld.annotations;
    requires com.veld.runtime;
    
    // JMH
    requires jmh.core;
    
    // Spring
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    
    // Guice
    requires com.google.guice;
    
    // Dagger
    requires dagger;
    requires javax.inject;
    
    // Export packages
    exports com.veld.benchmark;
    exports com.veld.benchmark.common;
    exports com.veld.benchmark.veld;
    exports com.veld.benchmark.spring;
    exports com.veld.benchmark.guice;
    exports com.veld.benchmark.dagger;
    
    // Open for JMH reflection
    opens com.veld.benchmark to jmh.core;
    opens com.veld.benchmark.common to spring.core, spring.beans, com.google.guice;
    opens com.veld.benchmark.spring to spring.core, spring.beans, spring.context;
    opens com.veld.benchmark.guice to com.google.guice;
    opens com.veld.benchmark.veld to com.veld.runtime;
}
