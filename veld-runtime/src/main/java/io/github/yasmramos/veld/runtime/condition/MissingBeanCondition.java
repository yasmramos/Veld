package io.github.yasmramos.veld.runtime.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Condition that checks for the ABSENCE of beans in the container.
 * The condition matches only if NONE of the specified beans are registered.
 * 
 * @since 1.0.0
 */
public final class MissingBeanCondition implements Condition {
    
    private final List<String> beanTypes;
    private final List<String> beanNames;
    
    /**
     * Creates a condition that checks for missing bean types.
     * 
     * @param beanTypes fully qualified class names of bean types
     * @return a new MissingBeanCondition
     */
    public static MissingBeanCondition forTypes(String... beanTypes) {
        return new MissingBeanCondition(Arrays.asList(beanTypes), Collections.emptyList());
    }
    
    /**
     * Creates a condition that checks for missing bean names.
     * 
     * @param beanNames bean names to check
     * @return a new MissingBeanCondition
     */
    public static MissingBeanCondition forNames(String... beanNames) {
        return new MissingBeanCondition(Collections.emptyList(), Arrays.asList(beanNames));
    }
    
    /**
     * Creates a new missing bean condition.
     * 
     * @param beanTypes fully qualified class names of bean types to check
     * @param beanNames bean names to check
     */
    public MissingBeanCondition(List<String> beanTypes, List<String> beanNames) {
        this.beanTypes = beanTypes;
        this.beanNames = beanNames;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
        // Check types - fail if any type is present
        for (String type : beanTypes) {
            if (context.containsBeanType(type)) {
                return false;
            }
        }
        
        // Check names - fail if any name is present
        for (String name : beanNames) {
            if (context.containsBeanName(name)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("@ConditionalOnMissingBean(");
        
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

    @Override
    public String getFailureReason(ConditionContext context) {
        List<String> foundTypes = new ArrayList<>();
        List<String> foundNames = new ArrayList<>();

        // Check which types are actually present (shouldn't be)
        for (String type : beanTypes) {
            if (context.containsBeanType(type)) {
                foundTypes.add(type);
            }
        }

        // Check which names are actually present (shouldn't be)
        for (String name : beanNames) {
            if (context.containsBeanName(name)) {
                foundNames.add(name);
            }
        }

        // If nothing was found, condition passed
        if (foundTypes.isEmpty() && foundNames.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found beans that should be absent:\n");

        // Add found types
        if (!foundTypes.isEmpty()) {
            sb.append("  Found bean types:\n");
            for (String type : foundTypes) {
                sb.append("    - ").append(type).append("\n");
            }
        }

        // Add found names
        if (!foundNames.isEmpty()) {
            sb.append("  Found bean names:\n");
            for (String name : foundNames) {
                sb.append("    - \"").append(name).append("\"\n");
            }
        }

        return sb.toString();
    }

    public List<String> getBeanTypes() {
        return beanTypes;
    }
    
    public List<String> getBeanNames() {
        return beanNames;
    }
}
