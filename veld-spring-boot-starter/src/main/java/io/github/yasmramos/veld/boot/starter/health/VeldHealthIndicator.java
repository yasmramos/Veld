package io.github.yasmramos.veld.boot.starter.health;

import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import io.github.yasmramos.veld.boot.starter.service.VeldSpringBootService;

/**
 * Spring Boot Health Indicator for Veld Container.
 * 
 * Provides health status for monitoring tools and Spring Boot Actuator.
 * 
 * Access via: GET /actuator/health/veld
 */
@Component("veld")
public class VeldHealthIndicator implements HealthIndicator {

    private final VeldSpringBootService veldService;

    public VeldHealthIndicator(VeldSpringBootService veldService) {
        this.veldService = veldService;
    }

    @Override
    public Health health() {
        try {
            if (veldService.isHealthy()) {
                return Health.up()
                        .withDetail("container", "running")
                        .withDetail("initialized", veldService.isInitialized())
                        .withDetail("version", "1.0.0-alpha.6")
                        .withDetail("framework", "Veld DI - Zero Reflection")
                        .build();
            } else {
                return Health.down()
                        .withDetail("container", "not running")
                        .withDetail("initialized", veldService.isInitialized())
                        .withDetail("reason", "Container is not healthy")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("container", "error")
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}