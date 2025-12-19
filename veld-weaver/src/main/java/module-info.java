/**
 * Veld Weaver Module.
 * Provides bytecode weaving capabilities for Veld DI Framework.
 */
module io.github.yasmramos.veld.weaver {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    
    // Maven Plugin API requirements
    requires org.apache.maven.plugin.api;
    requires org.apache.maven.core;
    
    // ASM requirements for bytecode manipulation
    requires org.ow2.asm;
    requires org.ow2.asm.commons;
    requires org.ow2.asm.tree;
    requires org.ow2.asm.util;
    
    // Export weaver package for use by other modules
    exports io.github.yasmramos.veld.weaver;
    exports io.github.yasmramos.veld.weaver.config;
    exports io.github.yasmramos.veld.weaver.exception;
    exports io.github.yasmramos.veld.weaver.handler;
    exports io.github.yasmramos.veld.weaver.transform;
    exports io.github.yasmramos.veld.weaver.util;
}