package io.github.yasmramos.veld.runtime.condition;

import java.util.Arrays;
import java.util.List;

/**
 * Condition that checks for the presence of classes on the classpath.
 * ALL specified classes must be present for the condition to match.
 * 
 * @since 1.0.0
 */
public final class ClassCondition implements Condition {
    
    private final List<String> requiredClasses;
    
    /**
     * Creates a new class condition.
     * 
     * @param requiredClasses fully qualified class names that must be present
     */
    public ClassCondition(String... requiredClasses) {
        this.requiredClasses = Arrays.asList(requiredClasses);
    }
    
    /**
     * Creates a new class condition.
     * 
     * @param requiredClasses list of fully qualified class names
     */
    public ClassCondition(List<String> requiredClasses) {
        this.requiredClasses = requiredClasses;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
        for (String className : requiredClasses) {
            if (!context.isClassPresent(className)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("@ConditionalOnClass(name={");
        for (int i = 0; i < requiredClasses.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(requiredClasses.get(i)).append("\"");
        }
        sb.append("})");
        return sb.toString();
    }
    
    public List<String> getRequiredClasses() {
        return requiredClasses;
    }
}
