/**
 * Veld AOP Module.
 * Provides Aspect-Oriented Programming support for the Veld DI Framework.
 */
module io.github.yasmramos.aop {
    // Required Veld modules
    requires transitive io.github.yasmramos.annotation;
    requires transitive io.github.yasmramos.runtime;
    
    // ASM modules for bytecode generation
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.util;
    
    // Export all AOP packages
    exports io.github.yasmramos.aop;
    exports io.github.yasmramos.aop.interceptor;
    exports io.github.yasmramos.aop.pointcut;
    exports io.github.yasmramos.aop.proxy;
}
