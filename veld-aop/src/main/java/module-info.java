/**
 * Veld AOP Module.
 * Provides Aspect-Oriented Programming support for the Veld DI Framework.
 */
module com.veld.aop {
    // Required Veld modules
    requires transitive com.veld.annotation;
    requires transitive com.veld.runtime;
    
    // ASM modules for bytecode generation
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.util;
    
    // Export all AOP packages
    exports com.veld.aop;
    exports com.veld.aop.interceptor;
    exports com.veld.aop.pointcut;
    exports com.veld.aop.proxy;
}
