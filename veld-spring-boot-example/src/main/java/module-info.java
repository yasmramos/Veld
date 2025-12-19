/**
 * Veld Spring Boot Example Module.
 * Provides Spring Boot examples and demonstrations for Veld DI Framework.
 */
module io.github.yasmramos.veld.spring.boot.example {
    requires io.github.yasmramos.veld.annotation;
    requires io.github.yasmramos.veld.spring.boot.starter;
    
    // Spring Boot requirements for examples
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.boot.web;
    requires spring.boot.actuator;
    requires spring.test;

    
    // Export example packages
    exports io.github.yasmramos.veld.spring.boot.example;
    exports io.github.yasmramos.veld.spring.boot.example.controller;
    exports io.github.yasmramos.veld.spring.boot.example.config;
    exports io.github.yasmramos.veld.spring.boot.example.service;
    exports io.github.yasmramos.veld.spring.boot.example.repository;
}