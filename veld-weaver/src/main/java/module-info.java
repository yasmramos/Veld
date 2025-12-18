/**
 * Veld Weaver Module.
 * Provides bytecode weaving capabilities for the Veld DI Framework.
 */
module veld.weaver {
    // Required JDK modules
    requires java.instrument;
    requires java.logging;
    
    // Required Veld modules
    requires io.github.yasmramos.veld.annotation;
    
    // Export weaver packages
    exports io.github.yasmramos.veld.weaver;
    
    // Open packages for bytecode manipulation
    opens io.github.yasmramos.veld.weaver to java.instrument;
}