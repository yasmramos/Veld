/**
 * Veld Example Module.
 * Provides examples and demonstrations for Veld DI Framework.
 */
module io.github.yasmramos.veld.example {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    requires io.github.yasmramos.veld.aop;
    requires io.github.yasmramos.veld.processor;
    
    // Jakarta and JavaX Inject for examples
    requires jakarta.inject;
    requires javax.inject;
    
    // Export example packages
    exports io.github.yasmramos.veld.example;
    exports io.github.yasmramos.veld.example.aop;
    exports io.github.yasmramos.veld.example.events;
    exports io.github.yasmramos.veld.example.lifecycle;
    exports io.github.yasmramos.veld.example.dependsOn;
}