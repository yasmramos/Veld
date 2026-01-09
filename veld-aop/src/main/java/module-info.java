/**
 * Veld AOP Module.
 * Provides Aspect-Oriented Programming support for the Veld DI Framework.
 */
module io.github.yasmramos.veld.aop {
    // Required Veld modules
    requires transitive io.github.yasmramos.veld.annotation;
    requires transitive io.github.yasmramos.veld.runtime;
    
    // ASM modules for bytecode generation
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.util;
    
    // Export all AOP packages
    exports io.github.yasmramos.veld.aop;
    exports io.github.yasmramos.veld.aop.interceptor;
    exports io.github.yasmramos.veld.aop.pointcut;
}
