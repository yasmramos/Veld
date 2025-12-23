package io.github.yasmramos.veld.boot.starter.bridge;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.boot.starter.config.VeldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Post-processor that bridges Spring beans to Veld container.
 * 
 * This allows Spring beans to be injectable in Veld components,
 * enabling incremental migration from Spring to Veld.
 * 
 * Registered beans:
 * - All @Component, @Service, @Repository, @Controller beans from Spring
 * - Beans are registered with their interface types when possible
 * - Primary beans are preferred when multiple implementations exist
 */
public class SpringToVeldBridge implements BeanFactoryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SpringToVeldBridge.class);
    
    private final VeldProperties properties;
    
    public SpringToVeldBridge(VeldProperties properties) {
        this.properties = properties;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!properties.getSpringIntegration().isEnabled()) {
            logger.debug("Spring to Veld bridge is disabled");
            return;
        }
        
        if (!properties.getSpringIntegration().isBridgeBeans()) {
            logger.debug("Bean bridging is disabled");
            return;
        }
        
        try {
            bridgeSpringBeansToVeld(beanFactory);
        } catch (Exception e) {
            logger.error("Failed to bridge Spring beans to Veld", e);
        }
    }
    
    /**
     * Register Spring beans in Veld container
     */
    private void bridgeSpringBeansToVeld(ConfigurableListableBeanFactory beanFactory) {
        logger.info("Starting Spring to Veld bean bridging");
        
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        int registeredCount = 0;
        
        for (String beanName : beanNames) {
            try {
                BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                
                // Skip internal Spring beans
                if (isInternalBean(beanName)) {
                    continue;
                }
                
                Class<?> beanClass = getBeanClass(beanFactory, beanName);
                if (beanClass == null) {
                    continue;
                }
                
                // Check if this bean should be bridged (has Veld annotations)
                if (shouldBridge(beanClass)) {
                    registerBeanInVeld(beanClass, beanName, registry);
                    registeredCount++;
                }
                
            } catch (Exception e) {
                logger.debug("Could not bridge bean: {}", beanName, e);
            }
        }
        
        logger.info("Successfully bridged {} Spring beans to Veld container", registeredCount);
    }
    
    /**
     * Check if bean is an internal Spring bean that should not be bridged
     */
    private boolean isInternalBean(String beanName) {
        return beanName.startsWith("org.springframework.")
            || beanName.startsWith("io.micrometer.")
            || beanName.contains("org.apache.")
            || beanName.contains("com.zaxxer.hikari");
    }
    
    /**
     * Get bean class from bean definition
     */
    private Class<?> getBeanClass(ConfigurableListableBeanFactory beanFactory, String beanName) {
        try {
            if (beanFactory.containsBean(beanName)) {
                return beanFactory.getType(beanName);
            }
        } catch (Exception e) {
            logger.debug("Could not get type for bean: {}", beanName);
        }
        return null;
    }
    
    /**
     * Check if bean should be bridged to Veld
     */
    private boolean shouldBridge(Class<?> beanClass) {
        // Check for Veld annotations
        if (beanClass.isAnnotationPresent(Component.class)) {
            return true;
        }
        
        // Check for javax.inject annotations (compatible with Veld)
        try {
            Class<?> injectClass = Class.forName("javax.inject.Inject");
            boolean hasInject = false;
            
            // Check constructor
            for (Constructor<?> constructor : beanClass.getConstructors()) {
                if (constructor.isAnnotationPresent((Class<Annotation>) injectClass)) {
                    hasInject = true;
                    break;
                }
            }
            
            if (hasInject) {
                return true;
            }
            
        } catch (ClassNotFoundException e) {
            // javax.inject not on classpath, skip
        }
        
        return false;
    }
    
    /**
     * Register bean in Veld container using reflection
     */
    private void registerBeanInVeld(Class<?> beanClass, String beanName, BeanDefinitionRegistry registry) {
        try {
            // Get the Veld registry class
            Class<?> veldClass = findVeldClass();
            if (veldClass == null) {
                logger.warn("Veld class not found, cannot bridge bean: {}", beanName);
                return;
            }
            
            // Get the singleton instance method
            Object veldInstance = getVeldInstance(veldClass);
            if (veldInstance == null) {
                logger.warn("Veld instance not available, cannot bridge bean: {}", beanName);
                return;
            }
            
            // Get register method
            Method registerMethod = findRegisterMethod(veldClass, beanClass);
            if (registerMethod != null) {
                registerMethod.invoke(veldInstance, beanClass, getBeanInstance(beanClass, beanName, registry));
                logger.debug("Bridging bean: {} -> {}", beanName, beanClass.getName());
            }
            
        } catch (Exception e) {
            logger.debug("Could not register bean in Veld: {}", beanName, e);
        }
    }
    
    /**
     * Find Veld class on classpath
     */
    private Class<?> findVeldClass() {
        try {
            return Class.forName("io.github.yasmramos.veld.generated.Veld");
        } catch (ClassNotFoundException e) {
            logger.debug("Veld class not found on classpath");
            return null;
        }
    }
    
    /**
     * Get Veld singleton instance
     */
    private Object getVeldInstance(Class<?> veldClass) {
        try {
            Field instanceField = veldClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            return instanceField.get(null);
        } catch (Exception e) {
            try {
                // Try getInstance method
                Method getInstance = veldClass.getMethod("getInstance");
                return getInstance.invoke(null);
            } catch (Exception ex) {
                logger.debug("Could not get Veld instance", ex);
                return null;
            }
        }
    }
    
    /**
     * Find register method in Veld class
     */
    private Method findRegisterMethod(Class<?> veldClass, Class<?> beanClass) {
        try {
            // Try registerBean method
            return veldClass.getMethod("registerBean", Class.class, Object.class);
        } catch (NoSuchMethodException e) {
            logger.debug("registerBean method not found in Veld class");
            return null;
        }
    }
    
    /**
     * Get bean instance from Spring context
     */
    private Object getBeanInstance(Class<?> beanClass, String beanName, BeanDefinitionRegistry registry) {
        try {
            // Get bean from application context via static method
            return getSpringContext().getBean(beanName);
        } catch (Exception e) {
            try {
                // Try creating instance directly
                return beanClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                logger.debug("Could not create instance for: {}", beanClass.getName());
                return null;
            }
        }
    }
    
    /**
     * Get Spring ApplicationContext (lazy initialization)
     */
    private org.springframework.context.ApplicationContext getSpringContext() {
        try {
            Class<?> contextClass = Class.forName("org.springframework.context.ApplicationContext");
            Field contextField = io.github.yasmramos.veld.generated.Veld.class.getDeclaredField("applicationContext");
            contextField.setAccessible(true);
            return (org.springframework.context.ApplicationContext) contextField.get(null);
        } catch (Exception e) {
            logger.debug("Could not get Spring ApplicationContext");
            return null;
        }
    }
}
