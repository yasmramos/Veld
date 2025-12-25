package io.github.yasmramos.veld.boot.example.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for the application.
 * Demonstrates Spring Boot configuration for data access.
 */
@Configuration
@EnableJpaRepositories(basePackages = "io.github.yasmramos.veld.boot.example.repository")
@EntityScan(basePackages = "io.github.yasmramos.veld.boot.example.domain")
@EnableTransactionManagement
public class JpaConfig {
    // Configuration is handled by Spring Boot auto-configuration
}
