/**
 * Veld Spring Boot Example Module.
 * Provides Spring Boot examples and demonstrations for Veld DI Framework.
 */
module io.github.yasmramos.veld.spring.boot.example {
    requires io.github.yasmramos.veld.annotation;
    
    // Spring Boot requirements for examples
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;
    requires spring.boot.actuator;

    
    // Export example packages
    exports io.github.yasmramos.veld.boot.example;
    exports io.github.yasmramos.veld.boot.example.controller;
    exports io.github.yasmramos.veld.boot.example.service;
}