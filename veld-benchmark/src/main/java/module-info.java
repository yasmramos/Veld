/**
 * Veld Benchmark Module.
 * Provides performance benchmarks for Veld DI Framework.
 */
module io.github.yasmramos.veld.benchmark {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    
    // Jakarta and JavaX Inject for benchmarks
    requires jakarta.inject;
    
    // JMH requirements (will be provided in runtime)
    // requires jmh.core;
    // requires jmh.generator.annotations;
    
    // Spring Framework for comparison benchmarks
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    
    // Export benchmark package
    exports io.github.yasmramos.veld.benchmark;
    exports io.github.yasmramos.veld.benchmark.config;
    exports io.github.yasmramos.veld.benchmark.framework;
    exports io.github.yasmramos.veld.benchmark.result;
    exports io.github.yasmramos.veld.benchmark.util;
}