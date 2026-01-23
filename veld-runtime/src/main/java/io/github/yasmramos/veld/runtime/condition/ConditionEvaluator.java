package io.github.yasmramos.veld.runtime.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
     * Adds a present bean condition by types.
     * The component will only be registered if all specified bean types exist.
     * 
     * @param beanTypes fully qualified class names that MUST be present
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addPresentBeanCondition(String... beanTypes) {
        return addCondition(PresentBeanCondition.forTypes(beanTypes));
    }
    
    /**
     * Adds a present bean condition by names.
     * The component will only be registered if all specified bean names exist.
     * 
     * @param beanNames bean names that MUST be present
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addPresentBeanNameCondition(String... beanNames) {
        return addCondition(PresentBeanCondition.forNames(beanNames));
    }
    
    /**
     * Adds a profile condition.
     * 
     * @param profiles the profiles that must be active (any one of them)
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addProfileCondition(String... profiles) {
        return addProfileCondition(profiles, "", ProfileCondition.MatchStrategy.ALL);
    }
    
    /**
     * Adds a profile condition with expression and strategy.
     * 
     * @param profiles the profiles that must be active
     * @param expression SpEL-style expression for complex conditions
     * @param strategy how to combine value and expression conditions
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addProfileCondition(String[] profiles, String expression, ProfileCondition.MatchStrategy strategy) {
        return addCondition(new ProfileCondition(profiles, expression, strategy));
    }
    
    /**
     * Adds a present bean condition by types with custom strategy.
     * 
     * @param beanTypes fully qualified class names that MUST be present
     * @param strategy how to combine conditions (ALL or ANY)
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addPresentBeanCondition(String[] beanTypes, PresentBeanCondition.MatchStrategy strategy) {
        return addCondition(new PresentBeanCondition(Arrays.asList(beanTypes), Collections.emptyList(), strategy));
    }
    
    /**
     * Adds a present bean condition by names with custom strategy.
     * 
     * @param beanNames bean names that MUST be present
     * @param strategy how to combine conditions (ALL or ANY)
     * @return this evaluator for chaining
     */
    public ConditionEvaluator addPresentBeanNameCondition(String[] beanNames, PresentBeanCondition.MatchStrategy strategy) {
        return addCondition(new PresentBeanCondition(Collections.emptyList(), Arrays.asList(beanNames), strategy));
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
     * Gets a detailed description of all failing conditions.
     *
     * @param context the condition context
     * @return detailed description of failing conditions, or empty string if all pass
     */
    public String getFailureMessage(ConditionContext context) {
        StringBuilder sb = new StringBuilder();
        boolean hasFailures = false;

        for (Condition condition : conditions) {
            if (!condition.matches(context)) {
                if (!hasFailures) {
                    sb.append("Component '").append(componentName).append("' excluded due to:\n\n");
                    hasFailures = true;
                }

                // Get detailed failure reason
                String reason = condition.getFailureReason(context);
                if (reason != null && !reason.isEmpty()) {
                    sb.append(reason);
                } else {
                    // Fallback to description if getFailureReason not implemented
                    sb.append(condition.getDescription());
                }
                sb.append("\n");
            }
        }

        return hasFailures ? sb.toString() : "";
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
