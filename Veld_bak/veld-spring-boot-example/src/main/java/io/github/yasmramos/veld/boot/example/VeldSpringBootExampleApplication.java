package io.github.yasmramos.veld.boot.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
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