/**
 * Veld Runtime Module.
 * Provides the minimal runtime container for the Veld DI Framework.
 */
module io.github.yasmramos.veld.runtime {
    // Required JDK modules
    requires java.logging;
    
    // Required Veld modules
    requires transitive io.github.yasmramos.veld.annotation;
    
    // Export all runtime packages
    exports io.github.yasmramos.veld;
    exports io.github.yasmramos.veld.runtime;
    exports io.github.yasmramos.veld.runtime.condition;
    exports io.github.yasmramos.veld.runtime.event;
    exports io.github.yasmramos.veld.runtime.lifecycle;
    exports io.github.yasmramos.veld.runtime.lifecycle.event;
    exports io.github.yasmramos.veld.runtime.value;
    exports io.github.yasmramos.veld.runtime.async;
    
    // Open packages for reflection (needed for DI and lifecycle callbacks)
    opens io.github.yasmramos.veld.runtime;
    opens io.github.yasmramos.veld.runtime.lifecycle;
}
