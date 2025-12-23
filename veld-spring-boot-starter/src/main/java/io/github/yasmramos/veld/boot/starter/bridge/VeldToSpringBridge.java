package io.github.yasmramos.veld.boot.starter.bridge;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.boot.starter.config.VeldProperties;
import io.github.yasmramos.veld.boot.starter.service.VeldSpringBootService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers Veld-managed beans into Spring ApplicationContext.
 * 
 * This enables:
 * - Using @Autowired/@Inject on Veld beans from Spring controllers
 * - Injecting Veld beans into Spring services
 * - Using Veld beans in Spring templates (Thymeleaf, etc.)
 * 
 * Usage:
 * {@code
 * @Autowired
 * private UserService veldUserService;  // Veld bean in Spring
 * }
 */
public class VeldToSpringBridge {

    private static final Logger logger = LoggerFactory.getLogger(VeldToSpringBridge.class);
    private static final String VELD_CLASS = "io.github.yasmramos.veld.generated.Veld";
    
    private final VeldProperties properties;
    private final ApplicationContext springContext;
    private final VeldSpringBootService veldService;
    
    public VeldToSpringBridge(VeldProperties properties, 
                             ApplicationContext springContext,
                             VeldSpringBootService veldService) {
        this.properties = properties;
        this.springContext = springContext;
        this.veldService = veldService;
    }
    
    /**
     * Bridge all Veld beans to Spring context
     */
    public void bridge() {
        if (!properties.getSpringIntegration().isEnabled()) {
            logger.debug("Spring integration is disabled");
            return;
        }
        
        if (!properties.getSpringIntegration().isBridgeBeans()) {
            logger.debug("Bean bridging is disabled");
            return;
        }
        
        if (!veldService.isInitialized()) {
            logger.warn("Veld is not initialized, cannot bridge beans");
            return;
        }
        
        try {
            SingletonBeanRegistry registry = (SingletonBeanRegistry) springContext.getAutowireCapableBeanFactory();
            
            // Get all registered component types from Veld
            Map<Class<?>, Object> veldBeans = getVeldBeans();
            
            int bridgedCount = 0;
            for (Map.Entry<Class<?>, Object> entry : veldBeans.entrySet()) {
                Class<?> beanType = entry.getKey();
                Object beanInstance = entry.getValue();
                
                if (beanInstance != null) {
                    // Register as singleton in Spring
                    String beanName = getBeanName(beanType);
                    String[] existingNames = springContext.getBeanNamesForType(beanType);
                    
                    // Only register if not already registered by Spring
                    boolean alreadyRegistered = false;
                    for (String existingName : existingNames) {
                        if (existingName.equals(beanName)) {
                            alreadyRegistered = true;
                            break;
                        }
                    }
                    
                    if (!alreadyRegistered) {
                        registry.registerSingleton(beanName, beanInstance);
                        bridgedCount++;
                        logger.debug("Bridged Veld bean: {} as {}", beanType.getSimpleName(), beanName);
                    }
                }
            }
            
            logger.info("Successfully bridged {} Veld beans to Spring context", bridgedCount);
            
        } catch (Exception e) {
            logger.error("Failed to bridge Veld beans to Spring", e);
        }
    }
    
    /**
     * Get all beans from Veld container
     */
    private Map<Class<?>, Object> getVeldBeans() {
        Map<Class<?>, Object> beans = new HashMap<>();
        
        try {
            Class<?> veldClass = Class.forName(VELD_CLASS);
            
            // Get all component types
            Method getComponentsMethod = veldClass.getMethod("getComponents");
            @SuppressWarnings("unchecked")
            java.util.Set<Class<?>> componentTypes = (java.util.Set<Class<?>>) getComponentsMethod.invoke(null);
            
            for (Class<?> componentType : componentTypes) {
                try {
                    Object bean = veldService.getBean(componentType);
                    if (bean != null) {
                        beans.put(componentType, bean);
                    }
                } catch (Exception e) {
                    logger.debug("Could not get bean of type: {}", componentType.getName());
                }
            }
            
        } catch (ClassNotFoundException e) {
            logger.warn("Veld class not found on classpath");
        } catch (Exception e) {
            logger.error("Failed to get Veld beans", e);
        }
        
        return beans;
    }
    
    /**
     * Generate bean name for Spring registration
     */
    private String getBeanName(Class<?> beanType) {
        // Use simple class name with veld prefix to avoid conflicts
        String simpleName = beanType.getSimpleName();
        if (simpleName == null || simpleName.isEmpty()) {
            return beanType.getName();
        }
        return String.valueOf(Character.toLowerCase(simpleName.charAt(0))).concat(simpleName.substring(1));
    }
}
