/**
 * Veld Maven Plugin Module.
 * Provides Maven plugin capabilities for the Veld DI Framework.
 */
module veld.maven.plugin {
    // Required JDK modules
    requires java.compiler;
    
    // Required Veld modules
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.processor;
    
    // Export plugin packages
    exports io.github.yasmramos.veld.maven;
}