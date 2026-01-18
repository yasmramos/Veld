package io.github.yasmramos.veld.runtime.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Condition that checks for the PRESENCE of beans in the container.
 * This is the inverse of {@link MissingBeanCondition}.
 * 
 * @since 1.0.0
 */
public final class PresentBeanCondition implements Condition {
    
    private final List<String> beanTypes;
    private final List<String> beanNames;
    private final MatchStrategy strategy;
    
    /**
     * Strategy for combining multiple bean conditions.
     */
    public enum MatchStrategy {
        /**
         * All conditions must match (AND logic)
         */
        ALL,
        
        /**
         * At least one condition must match (OR logic)
         */
        ANY
    }
    
    /**
     * Creates a condition that checks for bean types being present.
     * Uses ALL strategy (all types must be present).
     * 
     * @param beanTypes fully qualified class names of bean types that must be present
     * @return a new PresentBeanCondition
     */
    public static PresentBeanCondition forTypes(String... beanTypes) {
        return new PresentBeanCondition(Arrays.asList(beanTypes), Collections.emptyList(), MatchStrategy.ALL);
    }
    
    /**
     * Creates a condition that checks for bean names being present.
     * Uses ALL strategy (all names must be present).
     * 
     * @param beanNames bean names that must be present
     * @return a new PresentBeanCondition
     */
    public static PresentBeanCondition forNames(String... beanNames) {
        return new PresentBeanCondition(Collections.emptyList(), Arrays.asList(beanNames), MatchStrategy.ALL);
    }
    
    /**
     * Creates a new present bean condition with default ALL strategy.
     * 
     * @param beanTypes fully qualified class names of bean types to check
     * @param beanNames bean names to check
     */
    public PresentBeanCondition(List<String> beanTypes, List<String> beanNames) {
        this(beanTypes, beanNames, MatchStrategy.ALL);
    }
    
    /**
     * Creates a new present bean condition with custom strategy.
     * 
     * @param beanTypes fully qualified class names of bean types to check
     * @param beanNames bean names to check
     * @param strategy how to combine multiple conditions
     */
    public PresentBeanCondition(List<String> beanTypes, List<String> beanNames, MatchStrategy strategy) {
        this.beanTypes = beanTypes != null ? beanTypes : Collections.emptyList();
        this.beanNames = beanNames != null ? beanNames : Collections.emptyList();
        this.strategy = strategy != null ? strategy : MatchStrategy.ALL;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
        boolean typesMatch = matchTypes(context);
        boolean namesMatch = matchNames(context);
        
        switch (strategy) {
            case ANY:
                // If both are empty, consider it a match
                if (beanTypes.isEmpty() && beanNames.isEmpty()) {
                    return true;
                }
                // If only one type is specified, use its result
                if (beanTypes.size() == 1 && beanNames.isEmpty()) {
                    return typesMatch;
                }
                // If only one name is specified, use its result
                if (beanNames.size() == 1 && beanTypes.isEmpty()) {
                    return namesMatch;
                }
                // At least one must match
                return typesMatch || namesMatch;
            case ALL:
            default:
                // All specified conditions must match
                // If both are empty, consider it a match
                if (beanTypes.isEmpty() && beanNames.isEmpty()) {
                    return true;
                }
                // All non-empty conditions must match
                boolean allTypesPass = beanTypes.isEmpty() || typesMatch;
                boolean allNamesPass = beanNames.isEmpty() || namesMatch;
                return allTypesPass && allNamesPass;
        }
    }
    
    /**
     * Checks if all specified bean types are present.
     */
    private boolean matchTypes(ConditionContext context) {
        for (String type : beanTypes) {
            if (!context.containsBeanType(type)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if all specified bean names are present.
     */
    private boolean matchNames(ConditionContext context) {
        for (String name : beanNames) {
            if (!context.containsBeanName(name)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets the bean types this condition checks.
     * 
     * @return the bean types list
     */
    public List<String> getBeanTypes() {
        return beanTypes;
    }
    
    /**
     * Gets the bean names this condition checks.
     * 
     * @return the bean names list
     */
    public List<String> getBeanNames() {
        return beanNames;
    }
    
    /**
     * Gets the match strategy.
     * 
     * @return the match strategy
     */
    public MatchStrategy getStrategy() {
        return strategy;
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
        
        if (strategy != MatchStrategy.ALL) {
            sb.append(", strategy=").append(strategy);
        }
        
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getFailureReason(ConditionContext context) {
        List<String> missingTypes = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();

        // Check which types are missing
        for (String type : beanTypes) {
            if (!context.containsBeanType(type)) {
                missingTypes.add(type);
            }
        }

        // Check which names are missing
        for (String name : beanNames) {
            if (!context.containsBeanName(name)) {
                missingNames.add(name);
            }
        }

        // If nothing is missing, condition passed
        if (missingTypes.isEmpty() && missingNames.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (strategy == MatchStrategy.ALL) {
            sb.append("Required beans not found in container:\n");
        } else {
            sb.append("No matching beans found in container (strategy=ANY):\n");
        }

        // Add missing types
        if (!missingTypes.isEmpty()) {
            sb.append("  Missing bean types:\n");
            for (String type : missingTypes) {
                sb.append("    - ").append(type).append("\n");
            }
        }

        // Add missing names
        if (!missingNames.isEmpty()) {
            sb.append("  Missing bean names:\n");
            for (String name : missingNames) {
                sb.append("    - \"").append(name).append("\"\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "PresentBeanCondition{beanTypes=" + beanTypes + 
               ", beanNames=" + beanNames + 
               ", strategy=" + strategy + "}";
    }
}
