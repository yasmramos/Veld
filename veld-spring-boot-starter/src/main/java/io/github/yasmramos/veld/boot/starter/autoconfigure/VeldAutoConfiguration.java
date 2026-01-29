package io.github.yasmramos.veld.boot.starter.autoconfigure;

import io.github.yasmramos.veld.boot.starter.bridge.VeldToSpringBridge;
import io.github.yasmramos.veld.boot.starter.config.VeldProperties;
import io.github.yasmramos.veld.boot.starter.health.VeldHealthIndicator;
import io.github.yasmramos.veld.boot.starter.service.VeldSpringBootService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Auto-configuration for Veld Framework integration with Spring Boot.
 *
 * This configuration is automatically applied when:
 * 1. Veld classes are on the classpath
 * 2. VeldProperties is available
 * 3. Spring Boot Actuator is available (for health indicator)
 *
 * Provides:
 * - VeldProperties configuration bean
 * - VeldSpringBootService for container management
 * - VeldToSpringBridge for bridging Veld beans to Spring
 * - VeldHealthIndicator for Spring Boot Actuator
 *
 * Disable via: veld.spring-integration.enabled=false
 */
@Configuration
@ConditionalOnClass({VeldSpringBootService.class})
@EnableConfigurationProperties(VeldProperties.class)
public class VeldAutoConfiguration {

    /**
     * Configuration properties for Veld
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "veld", name = "spring-integration.enabled", havingValue = "true", matchIfMissing = true)
    public VeldProperties veldProperties() {
        return new VeldProperties();
    }

    /**
     * Main service for managing Veld Container lifecycle
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "veld", name = "container.auto-start", havingValue = "true", matchIfMissing = true)
    public VeldSpringBootService veldSpringBootService(VeldProperties properties) {
        return new VeldSpringBootService(properties);
    }

    /**
     * Bridge for registering Veld beans in Spring ApplicationContext.
     * This bean is created after VeldSpringBootService is initialized.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "veld", name = "spring-integration.bridge-beans", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(VeldSpringBootService.class)
    @DependsOn("veldSpringBootService")
    public VeldToSpringBridge veldToSpringBridge(VeldProperties properties,
                                                  VeldSpringBootService veldService) {
        return new VeldToSpringBridge(properties, null, veldService);
    }

    /**
     * Health indicator for Veld Container
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "veld", name = "spring-integration.health-indicator", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(VeldSpringBootService.class)
    public VeldHealthIndicator veldHealthIndicator(VeldSpringBootService veldService) {
        return new VeldHealthIndicator(veldService);
    }
}
