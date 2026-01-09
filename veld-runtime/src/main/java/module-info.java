/**
 * Veld Runtime Module.
 * Provides the minimal runtime container for the Veld DI Framework.
 */
module io.github.yasmramos.veld.runtime {
    // Required JDK modules
    requires java.logging;
    
    // Required Veld modules
    requires transitive io.github.yasmramos.veld.annotation;
    
    // Export runtime packages
    // Note: io.github.yasmramos.veld contains the stub. The processor generates
    // classes in this package in the consumer's target/generated-sources directory,
    // which takes precedence at compile/runtime because it appears first in classpath.
    exports io.github.yasmramos.veld;
    exports io.github.yasmramos.veld.runtime;
    exports io.github.yasmramos.veld.runtime.condition;
    exports io.github.yasmramos.veld.runtime.event;
    exports io.github.yasmramos.veld.runtime.graph;
    exports io.github.yasmramos.veld.runtime.scope;
    exports io.github.yasmramos.veld.runtime.lifecycle;
    exports io.github.yasmramos.veld.runtime.lifecycle.event;
    exports io.github.yasmramos.veld.runtime.value;
    exports io.github.yasmramos.veld.runtime.async;
    
    // Open packages for reflection (needed for DI and lifecycle callbacks)
    opens io.github.yasmramos.veld.runtime;
    opens io.github.yasmramos.veld.runtime.graph;
    opens io.github.yasmramos.veld.runtime.lifecycle;
    opens io.github.yasmramos.veld.runtime.scope;
}
