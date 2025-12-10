/**
 * Veld Runtime Module.
 * Provides the minimal runtime container for the Veld DI Framework.
 */
module io.github.yasmramos.runtime {
    // Required JDK modules
    requires java.logging;
    
    // Required Veld modules
    requires transitive io.github.yasmramos.annotation;
    
    // Export all runtime packages
    exports io.github.yasmramos;
    exports io.github.yasmramos.runtime;
    exports io.github.yasmramos.runtime.condition;
    exports io.github.yasmramos.runtime.event;
    exports io.github.yasmramos.runtime.lifecycle;
    exports io.github.yasmramos.runtime.lifecycle.event;
    exports io.github.yasmramos.runtime.value;
    
    // Open packages for reflection (needed for DI and lifecycle callbacks)
    opens io.github.yasmramos.runtime;
    opens io.github.yasmramos.runtime.lifecycle;
}
