package com.veld.boot.starter.service;

import com.veld.boot.starter.config.VeldProperties;
import com.veld.runtime.VeldContainer;
import com.veld.runtime.VeldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that manages the lifecycle of Veld Container within Spring Boot applications.
 * 
 * This service:
 * <ul>
 *   <li>Initializes Veld Container on startup</li>
 *   <li>Provides integration hooks for Spring Boot features</li>
 *   <li>Manages container shutdown gracefully</li>
 *   <li>Bridges Veld beans to Spring ApplicationContext if enabled</li>
 *   <li>Provides health indicators</li>
 * </ul>
 * 
 * @author Veld Team
 */
public class VeldSpringBootService implements InitializingBean, DisposableBean, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(VeldSpringBootService.class);

    private final VeldProperties properties;
    private volatile VeldContainer container;
    private volatile ApplicationContext applicationContext;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public VeldSpringBootService(VeldProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!properties.getSpringIntegration().isEnabled()) {
            logger.info("Veld Spring Boot integration is disabled");
            return;
        }

        if (!properties.getContainer().isAutoStart()) {
            logger.info("Veld auto-start is disabled");
            return;
        }

        initializeVeldContainer();
    }

    @Override
    public void destroy() throws Exception {
        close();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize the Veld Container with configured properties
     */
    private void initializeVeldContainer() {
        try {
            if (initialized.get()) {
                logger.warn("Veld Container is already initialized");
                return;
            }

            Set<String> profiles = resolveActiveProfiles();
            
            logger.info("Initializing Veld Container with profiles: {}", profiles);
            
            if (profiles.isEmpty()) {
                this.container = new VeldContainer();
            } else {
                this.container = new VeldContainer(profiles);
            }

            if (properties.getLogging().isEnabled()) {
                logger.info("Veld Container initialized successfully with {} active components", 
                           getComponentCount());
            }

            initialized.set(true);

            // Bridge beans to Spring context if enabled
            if (properties.getSpringIntegration().isBridgeBeans() && applicationContext != null) {
                bridgeBeansToSpring();
            }

        } catch (Exception e) {
            logger.error("Failed to initialize Veld Container", e);
            throw new VeldException("Failed to initialize Veld Container", e);
        }
    }

    /**
     * Resolve active profiles from configuration
     */
    private Set<String> resolveActiveProfiles() {
        String[] configuredProfiles = properties.getProfiles();
        
        if (configuredProfiles != null && configuredProfiles.length > 0) {
            return new HashSet<>(Arrays.asList(configuredProfiles));
        }
        
        // Fallback to default profile
        return Set.of("default");
    }

    /**
     * Bridge Veld beans to Spring ApplicationContext
     * This allows Spring components to autowire Veld beans
     */
    private void bridgeBeansToSpring() {
        // TODO: Implement bean bridging logic
        // This would involve registering Veld beans as Spring beans
        // or creating proxy beans that delegate to Veld container
        logger.debug("Bean bridging to Spring context is not yet implemented");
    }

    /**
     * Get the number of managed components
     */
    private int getComponentCount() {
        // TODO: Implement component counting
        // This would require access to the ComponentRegistry
        return 0;
    }

    /**
     * Get the Veld Container instance
     */
    public VeldContainer getContainer() {
        return container;
    }

    /**
     * Check if the service is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Get a bean from the Veld container
     */
    public Object getBean(String name) {
        if (container == null) {
            throw new IllegalStateException("Veld Container is not initialized");
        }
        return container.getBean(name);
    }

    /**
     * Get a bean from the Veld container by type
     */
    public <T> T getBean(Class<T> type) {
        if (container == null) {
            throw new IllegalStateException("Veld Container is not initialized");
        }
        return container.getBean(type);
    }

    /**
     * Get a bean from the Veld container by name and type
     */
    public <T> T getBean(String name, Class<T> type) {
        if (container == null) {
            throw new IllegalStateException("Veld Container is not initialized");
        }
        return container.getBean(name, type);
    }

    /**
     * Close the Veld Container gracefully
     */
    public void close() {
        if (closed.get()) {
            return;
        }

        synchronized (this) {
            if (closed.get()) {
                return;
            }

            try {
                if (properties.getContainer().isAutoClose() && container != null) {
                    logger.info("Closing Veld Container");
                    container.close();
                }
                closed.set(true);
            } catch (Exception e) {
                logger.error("Error closing Veld Container", e);
            }
        }
    }

    /**
     * Health check for Veld Container
     */
    public boolean isHealthy() {
        return initialized.get() && !closed.get() && container != null;
    }
}