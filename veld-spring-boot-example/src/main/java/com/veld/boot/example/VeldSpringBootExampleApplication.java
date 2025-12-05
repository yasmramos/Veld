package com.veld.boot.example;

import com.veld.annotations.Component;
import com.veld.annotations.Inject;
import com.veld.annotations.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application demonstrating Veld Framework integration.
 * 
 * This application shows:
 * - Veld components (using @Component, @Inject annotations)
 * - Spring Boot integration (Veld container auto-starts)
 * - Health checks via Actuator
 * - REST endpoints accessing Veld beans
 * 
 * Run with: mvn spring-boot:run
 * 
 * Access health check: http://localhost:8080/actuator/health/veld
 */
@SpringBootApplication
public class VeldSpringBootExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeldSpringBootExampleApplication.class, args);
    }
}