/**
 * Veld Spring Boot Starter Module.
 * Provides Spring Boot integration for Veld DI Framework.
 */
module io.github.yasmramos.veld.spring.boot.starter {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.runtime;
    requires io.github.yasmramos.veld.aop;
    
    // Spring Boot requirements
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires spring.boot.actuator;
    
    // Jakarta Inject for Spring compatibility
    requires jakarta.inject;
    
    // SLF4J Logging
    requires org.slf4j;
    
    // Export starter package
    exports io.github.yasmramos.veld.spring.boot;
    exports io.github.yasmramos.veld.spring.boot.autoconfigure;
    exports io.github.yasmramos.veld.spring.boot.config;
    exports io.github.yasmramos.veld.spring.boot.exception;
    exports io.github.yasmramos.veld.spring.boot.handler;
    exports io.github.yasmramos.veld.spring.boot.util;
}