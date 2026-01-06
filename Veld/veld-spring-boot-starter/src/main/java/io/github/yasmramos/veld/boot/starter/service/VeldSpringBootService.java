package io.github.yasmramos.veld.boot.starter.service;

import io.github.yasmramos.veld.boot.starter.bridge.VeldToSpringBridge;
import io.github.yasmramos.veld.boot.starter.config.VeldProperties;
import io.github.yasmramos.veld.VeldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that manages Veld integration within Spring Boot applications.
 * 
 * This service:
 * <ul>
 *   <li>Provides access to Veld managed components</li>
 *   <li>Bridges Veld beans to Spring ApplicationContext if enabled</li>
 *   <li>Provides health indicators</li>
 * </ul>
 * 
 * @author Veld Team
 */
public class VeldSpringBootService implements InitializingBean, DisposableBean, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(VeldSpringBootService.class);
    private static final String VELD_CLASS = "io.github.yasmramos.veld.Veld";

    private final VeldProperties properties;
    private volatile ApplicationContext applicationContext;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    // Cached method handles for Veld class
    private volatile MethodHandle getByClassHandle;
    private volatile MethodHandle containsHandle;
    private volatile MethodHandle componentCountHandle;

    public VeldSpringBootService(VeldProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!properties.getSpringIntegration().isEnabled()) {
            logger.info("Veld Spring Boot integration is disabled");
            return;
        }

        initializeVeld();
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
     * Initialize Veld integration
     */
    private void initializeVeld() {
        try {
            if (initialized.get()) {
                logger.warn("Veld is already initialized");
                return;
            }

            // Verify Veld class exists
            Class.forName(VELD_CLASS);
            
            // Cache method handles
            cacheMethodHandles();

            if (properties.getLogging().isEnabled()) {
                logger.info("Veld initialized successfully with {} components", getComponentCount());
            }

            initialized.set(true);

            // Bridge beans to Spring context if enabled
            if (properties.getSpringIntegration().isBridgeBeans() && applicationContext != null) {
                bridgeBeansToSpring();
            }

        } catch (ClassNotFoundException e) {
            logger.warn("Veld generated class not found. Make sure @Component classes are annotated.");
        } catch (Exception e) {
            logger.error("Failed to initialize Veld", e);
            throw new VeldException("Failed to initialize Veld", e);
        }
    }
    
    private void cacheMethodHandles() throws Exception {
        Class<?> veldClass = Class.forName(VELD_CLASS);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        
        getByClassHandle = lookup.findStatic(veldClass, "get", 
            MethodType.methodType(Object.class, Class.class));
        containsHandle = lookup.findStatic(veldClass, "contains",
            MethodType.methodType(boolean.class, Class.class));
        componentCountHandle = lookup.findStatic(veldClass, "componentCount",
            MethodType.methodType(int.class));
    }

    /**
     * Bridge Veld beans to Spring ApplicationContext
     */
    private void bridgeBeansToSpring() {
        try {
            VeldToSpringBridge bridge = new VeldToSpringBridge(
                properties, 
                applicationContext, 
                this
            );
            bridge.bridge();
            logger.info("Successfully bridged Veld beans to Spring context");
        } catch (Exception e) {
            logger.warn("Failed to bridge Veld beans to Spring context: {}", e.getMessage());
        }
    }

    /**
     * Get the number of managed components
     */
    public int getComponentCount() {
        try {
            if (componentCountHandle != null) {
                return (int) componentCountHandle.invoke();
            }
        } catch (Throwable e) {
            logger.debug("Could not get component count", e);
        }
        return 0;
    }

    /**
     * Check if the service is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Get a bean from Veld by type
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        try {
            if (getByClassHandle != null) {
                return (T) getByClassHandle.invoke(type);
            }
        } catch (Throwable e) {
            throw new VeldException("Failed to get bean of type: " + type.getName(), e);
        }
        throw new IllegalStateException("Veld is not initialized");
    }

    /**
     * Check if Veld contains a bean of the given type
     */
    public boolean contains(Class<?> type) {
        try {
            if (containsHandle != null) {
                return (boolean) containsHandle.invoke(type);
            }
        } catch (Throwable e) {
            logger.debug("Could not check contains", e);
        }
        return false;
    }

    /**
     * Close the Veld integration gracefully
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
                logger.info("Closing Veld Spring Boot integration");
                closed.set(true);
            } catch (Exception e) {
                logger.error("Error closing Veld integration", e);
            }
        }
    }

    /**
     * Health check for Veld
     */
    public boolean isHealthy() {
        return initialized.get() && !closed.get();
    }
}
