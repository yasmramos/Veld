/**
 * Veld Runtime Module.
 * Provides the minimal runtime container for the Veld DI Framework.
 */
module com.veld.runtime {
    // Required JDK modules
    requires java.logging;
    
    // Required Veld modules
    requires transitive com.veld.annotation;
    
    // Export all runtime packages
    exports com.veld;
    exports com.veld.runtime;
    exports com.veld.runtime.condition;
    exports com.veld.runtime.event;
    exports com.veld.runtime.lifecycle;
    exports com.veld.runtime.lifecycle.event;
    exports com.veld.runtime.value;
    
    // Open packages for reflection (needed for DI and lifecycle callbacks)
    opens com.veld.runtime;
    opens com.veld.runtime.lifecycle;
}
