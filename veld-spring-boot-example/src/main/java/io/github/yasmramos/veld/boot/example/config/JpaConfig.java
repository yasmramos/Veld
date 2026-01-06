package io.github.yasmramos.veld.boot.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for the application.
 * Demonstrates Spring Boot configuration for data access.
 * Note: In Spring Boot 3.x, @EntityScan is not required as entity detection is automatic.
 */
@Configuration
@EnableJpaRepositories(basePackages = "io.github.yasmramos.veld.boot.example.repository")
@EnableTransactionManagement
public class JpaConfig {
    // Configuration is handled by Spring Boot auto-configuration
}
