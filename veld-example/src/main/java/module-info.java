/**
 * Veld Example Module.
 * Demonstrates usage of the Veld DI Framework.
 */
module com.veld.example {
    // Required JDK modules
    requires java.logging;
    
    // Required Veld modules
    requires com.veld.annotation;
    requires com.veld.runtime;
    requires com.veld.aop;
    
    // JSR-330 and Jakarta inject support
    requires javax.inject;
    requires jakarta.inject;
    
    // Export example packages
    exports com.veld.example;
    exports com.veld.example.aop;
    exports com.veld.example.events;
    exports com.veld.example.lifecycle;
    
    // Open packages for dependency injection and lifecycle callbacks
    opens com.veld.example to com.veld.runtime;
    opens com.veld.example.aop to com.veld.runtime;
    opens com.veld.example.events to com.veld.runtime;
    opens com.veld.example.lifecycle to com.veld.runtime;
}
