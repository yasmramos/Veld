package io.github.yasmramos.veld.boot.starter.condition;

import io.github.yasmramos.veld.boot.starter.condition.ConditionalOnVeldBean.SearchStrategy;
import io.github.yasmramos.veld.boot.starter.service.VeldSpringBootService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;

import java.util.Arrays;

/**
 * Condition implementation for {@link ConditionalOnVeldBean}.
 * 
 * Evaluates whether a Veld bean of the specified type or name exists
 * in the Veld container, Spring context, or both.
 * 
 * @author Veld Team
 */
public class VeldBeanCondition implements Condition {

    private static final Logger logger = LoggerFactory.getLogger(VeldBeanCondition.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Extract annotation attributes
        ConditionalOnVeldBean annotation = getAnnotation(metadata);
        if (annotation == null) {
            return true;
        }

        Class<?>[] beanTypes = annotation.value();
        String[] beanNames = annotation.name();
        SearchStrategy searchStrategy = annotation.search();

        VeldSpringBootService veldService = getVeldService(context);
        boolean veldAvailable = veldService != null && veldService.isInitialized();

        // Check by bean names
        if (beanNames.length > 0) {
            for (String beanName : beanNames) {
                if (matchesName(beanName, searchStrategy, veldService, veldAvailable, context)) {
                    return true;
                }
            }
            return false;
        }

        // Check by bean types
        if (beanTypes.length > 0) {
            for (Class<?> beanType : beanTypes) {
                if (matchesType(beanType, searchStrategy, veldService, veldAvailable, context)) {
                    return true;
                }
            }
            return false;
        }

        // No conditions specified, always match
        return true;
    }

    /**
     * Extract @ConditionalOnVeldBean annotation from metadata
     */
    private ConditionalOnVeldBean getAnnotation(AnnotatedTypeMetadata metadata) {
        if (metadata.isAnnotated(ConditionalOnVeldBean.class.getName())) {
            return metadata.getAnnotation(ConditionalOnVeldBean.class.getName());
        }
        if (metadata instanceof MethodMetadata methodMetadata 
                && methodMetadata.isAnnotated(ConditionalOnVeldBean.class.getName())) {
            return methodMetadata.getAnnotation(ConditionalOnVeldBean.class.getName());
        }
        return null;
    }

    /**
     * Get VeldSpringBootService from application context
     */
    private VeldSpringBootService getVeldService(ConditionContext context) {
        try {
            return context.getBeanFactory().getBean(VeldSpringBootService.class);
        } catch (NoSuchBeanDefinitionException e) {
            logger.debug("VeldSpringBootService not found in context");
            return null;
        }
    }

    /**
     * Check if bean with specified name exists
     */
    private boolean matchesName(String beanName, SearchStrategy searchStrategy,
                               VeldSpringBootService veldService, boolean veldAvailable,
                               ConditionContext context) {
        
        // Search in Veld
        if ((searchStrategy == SearchStrategy.ALL || searchStrategy == SearchStrategy.VELD_ONLY)
                && veldAvailable) {
            try {
                // Try getting bean by class name from Veld
                Class<?> beanType = Class.forName(beanName);
                if (veldService.contains(beanType)) {
                    logger.debug("Found Veld bean by name: {}", beanName);
                    return true;
                }
            } catch (ClassNotFoundException e) {
                // Not a class name, check if it's a simple name
                if (veldService.contains(getClassBySimpleName(beanName))) {
                    logger.debug("Found Veld bean by simple name: {}", beanName);
                    return true;
                }
            }
        }

        // Search in Spring
        if (searchStrategy == SearchStrategy.ALL || searchStrategy == SearchStrategy.SPRING_ONLY) {
            try {
                String[] beanNames = context.getBeanFactory().getBeanNamesForType(Object.class);
                for (String name : beanNames) {
                    if (name.equals(beanName)) {
                        logger.debug("Found Spring bean by name: {}", beanName);
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.debug("Error searching Spring beans by name: {}", beanName);
            }
        }

        return false;
    }

    /**
     * Check if bean of specified type exists
     */
    private boolean matchesType(Class<?> beanType, SearchStrategy searchStrategy,
                               VeldSpringBootService veldService, boolean veldAvailable,
                               ConditionContext context) {
        
        // Search in Veld
        if ((searchStrategy == SearchStrategy.ALL || searchStrategy == SearchStrategy.VELD_ONLY)
                && veldAvailable) {
            if (veldService.contains(beanType)) {
                logger.debug("Found Veld bean by type: {}", beanType.getName());
                return true;
            }
        }

        // Search in Spring
        if (searchStrategy == SearchStrategy.ALL || searchStrategy == SearchStrategy.SPRING_ONLY) {
            try {
                String[] beanNames = context.getBeanFactory().getBeanNamesForType(beanType);
                if (beanNames.length > 0) {
                    logger.debug("Found Spring bean by type: {} (names: {})", 
                            beanType.getName(), Arrays.toString(beanNames));
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Error searching Spring beans by type: {}", beanType.getName());
            }
        }

        return false;
    }

    /**
     * Try to get class by simple name (first character lowercase)
     */
    private Class<?> getClassBySimpleName(String simpleName) {
        // This is a simplified implementation
        // A full implementation would scan the classpath
        return null;
    }
}
