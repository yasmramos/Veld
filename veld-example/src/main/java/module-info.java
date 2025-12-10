/**
 * Veld Example Module.
 * Demonstrates usage of the Veld DI Framework.
 */
module io.github.yasmramos.example {
    // Required JDK modules
    requires java.logging;
    
    // Required Veld modules
    requires io.github.yasmramos.annotation;
    requires io.github.yasmramos.runtime;
    requires io.github.yasmramos.aop;
    
    // JSR-330 and Jakarta inject support
    requires javax.inject;
    requires jakarta.inject;
    
    // Export example packages
    exports io.github.yasmramos.example;
    exports io.github.yasmramos.example.aop;
    exports io.github.yasmramos.example.events;
    exports io.github.yasmramos.example.lifecycle;
    
    // Open packages for dependency injection and lifecycle callbacks
    opens io.github.yasmramos.example to io.github.yasmramos.runtime;
    opens io.github.yasmramos.example.aop to io.github.yasmramos.runtime;
    opens io.github.yasmramos.example.events to io.github.yasmramos.runtime;
    opens io.github.yasmramos.example.lifecycle to io.github.yasmramos.runtime;
}
