/**
 * Veld Maven Plugin Module.
 * Provides Maven plugin integration for Veld DI Framework.
 */
module io.github.yasmramos.veld.maven.plugin {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.processor;
    requires io.github.yasmramos.veld.weaver;
    
    // Maven Plugin API requirements
    requires org.apache.maven.api.plugin;
    requires org.apache.maven.api.core;
    requires org.apache.maven.api.annotations;
    
    // ASM requirements for bytecode manipulation
    requires org.objectweb.asm;
    
    // Export plugin package
    exports io.github.yasmramos.veld.maven.plugin;
    exports io.github.yasmramos.veld.maven.plugin.config;
    exports io.github.yasmramos.veld.maven.plugin.exception;
    exports io.github.yasmramos.veld.maven.plugin.goal;
    exports io.github.yasmramos.veld.maven.plugin.handler;
    exports io.github.yasmramos.veld.maven.plugin.util;
}