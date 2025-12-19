/**
 * Veld Weaver Module.
 * Provides bytecode weaving capabilities for Veld DI Framework.
 */
module io.github.yasmramos.veld.weaver {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    
    // ASM requirements for bytecode manipulation
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.util;
    
    // Export weaver package for use by other modules
    exports io.github.yasmramos.veld.weaver;
    exports io.github.yasmramos.veld.weaver.config;
    exports io.github.yasmramos.veld.weaver.exception;
    exports io.github.yasmramos.veld.weaver.handler;
    exports io.github.yasmramos.veld.weaver.transform;
    exports io.github.yasmramos.veld.weaver.util;
}