/**
 * Veld Benchmark Module.
 * Provides performance benchmarks for Veld DI Framework.
 */
module io.github.yasmramos.veld.benchmark {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    
    // Jakarta and JavaX Inject for benchmarks
    requires jakarta.inject;
    requires javax.inject;
    
    // Dagger for comparison benchmarks
    requires dagger;
    
    // Google Guice is not a JPMS module - will be handled via classpath
    
    // JMH requirements
    requires jmh.core;
    requires jmh.generator.annprocess;
    
    // Spring Framework for comparison benchmarks
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    
    // Export benchmark packages
    exports io.github.yasmramos.veld.benchmark;
    exports io.github.yasmramos.veld.benchmark.common;
    exports io.github.yasmramos.veld.benchmark.dagger;
    exports io.github.yasmramos.veld.benchmark.guice;
    exports io.github.yasmramos.veld.benchmark.spring;
    exports io.github.yasmramos.veld.benchmark.strategic;
    exports io.github.yasmramos.veld.benchmark.veld;
}