package io.github.yasmramos.veld.runtime.condition;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a set of conditions for a component.
 * A component is only registered if ALL conditions pass.
 * 
 * @since 1.0.0
 */
public final class ConditionEvaluator {
    
    private final List<Condition> conditions;
    private final String componentName;
    
    /**
     * Creates a new condition evaluator.
     * 
     * @param componentName name of the component being evaluated
     */
    public ConditionEvaluator(String componentName) {
        this.componentName = componentName;
        this.conditions = new ArrayList<>();
    }
    
    /**
     * Adds a condition to be evaluated.
     * 
     * @param condition the condition to add
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addCondition(Condition condition) {
        conditions.add(condition);
        return this;
    }
    
    /**
     * Adds a property condition.
     * 
     * @param propertyName the property name
     * @param expectedValue the expected value (empty for any value)
     * @param matchIfMissing whether to match when property is missing
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addPropertyCondition(String propertyName, String expectedValue, boolean matchIfMissing) {
        return addCondition(new PropertyCondition(propertyName, expectedValue, matchIfMissing));
    }
    
    /**
     * Adds a class presence condition.
     * 
     * @param classNames fully qualified class names that must be present
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addClassCondition(String... classNames) {
        return addCondition(new ClassCondition(classNames));
    }
    
    /**
     * Adds a missing bean condition by types.
     * 
     * @param beanTypes fully qualified class names that must NOT be present
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addMissingBeanCondition(String... beanTypes) {
        return addCondition(MissingBeanCondition.forTypes(beanTypes));
    }
    
    /**
     * Adds a missing bean condition by names.
     * 
     * @param beanNames bean names that must NOT be present
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addMissingBeanNameCondition(String... beanNames) {
        return addCondition(MissingBeanCondition.forNames(beanNames));
    }
    
    /**
     * Adds a profile condition.
     * 
     * @param profiles the profiles that must be active (any one of them)
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addProfileCondition(String... profiles) {
        return addCondition(new ProfileCondition(profiles));
    }
    
    /**
     * Checks if all conditions are satisfied.
     * 
     * @param context the condition context
     * @return true if ALL conditions pass, false otherwise
     */
    public boolean evaluate(ConditionContext context) {
        for (Condition condition : conditions) {
            if (!condition.matches(context)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if any conditions are defined.
     * 
     * @return true if this evaluator has conditions
     */
    public boolean hasConditions() {
        return !conditions.isEmpty();
    }
    
    /**
     * Gets a description of all failing conditions.
     * 
     * @param context the condition context
     * @return description of failing conditions, or empty string if all pass
     */
    public String getFailureMessage(ConditionContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Component '").append(componentName).append("' excluded due to:\n");
        
        for (Condition condition : conditions) {
            if (!condition.matches(context)) {
                sb.append("  - ").append(condition.getDescription()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the component name being evaluated.
     * 
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }
    
    /**
     * Gets all conditions.
     * 
     * @return list of conditions
     */
    public List<Condition> getConditions() {
        return conditions;
    }
}
