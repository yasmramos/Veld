/**
 * Veld Benchmark Module.
 * Provides performance benchmarks for Veld DI Framework.
 */
module io.github.yasmramos.veld.benchmark {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    requires io.github.yasmramos.veld.processor;
    
    // Jakarta and JavaX Inject for benchmarks
    requires jakarta.inject;
    requires javax.inject;
    
    // JMH requirements
    requires jmh.core;
    requires jmh.generator.annotations;
    
    // Spring Framework for comparison benchmarks
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    
    // Guice for comparison benchmarks
    requires com.google.inject;
    
    // Dagger for comparison benchmarks  
    requires dagger;
    requires dagger.compiler;
    
    // Export benchmark package
    exports io.github.yasmramos.veld.benchmark;
    exports io.github.yasmramos.veld.benchmark.config;
    exports io.github.yasmramos.veld.benchmark.framework;
    exports io.github.yasmramos.veld.benchmark.result;
    exports io.github.yasmramos.veld.benchmark.util;
}