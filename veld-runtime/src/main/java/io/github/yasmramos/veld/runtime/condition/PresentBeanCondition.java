package io.github.yasmramos.veld.runtime.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Condition that checks for the PRESENCE of beans in the container.
 * The condition matches only if ALL of the specified beans are registered.
 * This is the inverse of {@link MissingBeanCondition}.
 * 
 * @since 1.0.0
 */
public final class PresentBeanCondition implements Condition {
    
    private final List<String> beanTypes;
    private final List<String> beanNames;
    
    /**
     * Creates a condition that checks for bean types being present.
     * 
     * @param beanTypes fully qualified class names of bean types that must be present
     * @return a new PresentBeanCondition
     */
    public static PresentBeanCondition forTypes(String... beanTypes) {
        return new PresentBeanCondition(Arrays.asList(beanTypes), Collections.emptyList());
    }
    
    /**
     * Creates a condition that checks for bean names being present.
     * 
     * @param beanNames bean names that must be present
     * @return a new PresentBeanCondition
     */
    public static PresentBeanCondition forNames(String... beanNames) {
        return new PresentBeanCondition(Collections.emptyList(), Arrays.asList(beanNames));
    }
    
    /**
     * Creates a new present bean condition.
     * 
     * @param beanTypes fully qualified class names of bean types to check
     * @param beanNames bean names to check
     */
    public PresentBeanCondition(List<String> beanTypes, List<String> beanNames) {
        this.beanTypes = beanTypes;
        this.beanNames = beanNames;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
        // Check types - fail if any type is missing
        for (String type : beanTypes) {
            if (!context.containsBeanType(type)) {
                return false;
            }
        }
        
        // Check names - fail if any name is missing
        for (String name : beanNames) {
            if (!context.containsBeanName(name)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("@ConditionalOnBean(");
        
        if (!beanTypes.isEmpty()) {
            sb.append("value={");
            for (int i = 0; i < beanTypes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(beanTypes.get(i)).append(".class");
            }
            sb.append("}");
        }
        
        if (!beanNames.isEmpty()) {
            if (!beanTypes.isEmpty()) sb.append(", ");
            sb.append("name={");
            for (int i = 0; i < beanNames.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(beanNames.get(i)).append("\"");
            }
            sb.append("}");
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    public List<String> getBeanTypes() {
        return beanTypes;
    }
    
    public List<String> getBeanNames() {
        return beanNames;
    }
}
