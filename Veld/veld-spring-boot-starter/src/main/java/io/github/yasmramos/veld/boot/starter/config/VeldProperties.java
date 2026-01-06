package io.github.yasmramos.veld.boot.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Veld Framework integration with Spring Boot.
 * 
 * Allows users to customize Veld behavior through application.properties:
 * 
 * <pre>
 * veld:
 *   profiles:
 *     active: dev,test
 *   container:
 *     auto-start: true
 *   logging:
 *     enabled: true
 *     level: INFO
 *   spring-integration:
 *     enabled: true
 *     bridge-beans: true
 *     health-indicator: true
 * </pre>
 */
@ConfigurationProperties(prefix = "veld")
public class VeldProperties {

    /**
     * Active Veld profiles to load components for.
     * Can be specified as comma-separated values.
     * If empty, defaults to "default" profile.
     */
    private String[] profiles = {};

    /**
     * Configuration for the Veld container
     */
    private final Container container = new Container();

    /**
     * Configuration for logging
     */
    private final Logging logging = new Logging();

    /**
     * Configuration for Spring Boot integration features
     */
    private final SpringIntegration springIntegration = new SpringIntegration();

    public String[] getProfiles() {
        return profiles;
    }

    public void setProfiles(String[] profiles) {
        this.profiles = profiles != null ? profiles : new String[0];
    }

    public Container getContainer() {
        return container;
    }

    public Logging getLogging() {
        return logging;
    }

    public SpringIntegration getSpringIntegration() {
        return springIntegration;
    }

    /**
     * Container-specific configuration
     */
    public static class Container {
        /**
         * Whether to auto-start Veld container on application startup
         */
        private boolean autoStart = true;

        /**
         * Whether to close Veld container on Spring context shutdown
         */
        private boolean autoClose = true;

        public boolean isAutoStart() {
            return autoStart;
        }

        public void setAutoStart(boolean autoStart) {
            this.autoStart = autoStart;
        }

        public boolean isAutoClose() {
            return autoClose;
        }

        public void setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
        }
    }

    /**
     * Logging configuration
     */
    public static class Logging {
        /**
         * Enable Veld logging
         */
        private boolean enabled = true;

        /**
         * Log level for Veld framework
         */
        private String level = "INFO";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    /**
     * Spring Boot integration configuration
     */
    public static class SpringIntegration {
        /**
         * Enable Spring Boot integration features
         */
        private boolean enabled = true;

        /**
         * Bridge Veld beans into Spring ApplicationContext
         */
        private boolean bridgeBeans = true;

        /**
         * Create Spring Health indicator for Veld
         */
        private boolean healthIndicator = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBridgeBeans() {
            return bridgeBeans;
        }

        public void setBridgeBeans(boolean bridgeBeans) {
            this.bridgeBeans = bridgeBeans;
        }

        public boolean isHealthIndicator() {
            return healthIndicator;
        }

        public void setHealthIndicator(boolean healthIndicator) {
            this.healthIndicator = healthIndicator;
        }
    }
}