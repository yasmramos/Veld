package io.github.yasmramos.veld.runtime.condition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Condition that matches when one or more specified profiles are active.
 * 
 * <p>This condition supports:
 * <ul>
 *   <li>Simple profile matching: "dev" matches when "dev" is active</li>
 *   <li>Negation: "!prod" matches when "prod" is NOT active</li>
 *   <li>Multiple profiles: ["dev", "test"] matches when either is active (OR logic)</li>
 *   <li>Expression: SpEL-style expressions like "dev &amp;&amp; database.type == 'h2'"</li>
 * </ul>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public class ProfileCondition implements Condition {
    
    private final String[] profiles;
    private final String expression;
    private final MatchStrategy strategy;
    
    /**
     * Strategy for combining profile conditions.
     */
    public enum MatchStrategy {
        /**
         * All conditions (value + expression) must match (AND logic)
         */
        ALL,
        
        /**
         * At least one condition must match (OR logic)
         */
        ANY
    }
    
    /**
     * Creates a new profile condition.
     * 
     * @param profiles the profiles to check (any must match)
     */
    public ProfileCondition(String... profiles) {
        this(profiles, "", MatchStrategy.ALL);
    }
    
    /**
     * Creates a new profile condition with expression and strategy.
     * 
     * @param profiles the profiles to check
     * @param expression SpEL-style expression for complex conditions
     * @param strategy how to combine value and expression conditions
     */
    public ProfileCondition(String[] profiles, String expression, MatchStrategy strategy) {
        this.profiles = profiles != null ? profiles : new String[0];
        this.expression = expression != null ? expression : "";
        this.strategy = strategy != null ? strategy : MatchStrategy.ALL;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
        boolean profileMatches = matchesProfiles(context);
        boolean expressionMatches = matchesExpression(context);
        
        switch (strategy) {
            case ANY:
                return profileMatches || expressionMatches;
            case ALL:
            default:
                // If both are empty/default, consider it a match
                if (profiles.length == 0 && expression.isEmpty()) {
                    return true;
                }
                // If only one is specified, use its result
                if (profiles.length == 0) {
                    return expressionMatches;
                }
                if (expression.isEmpty()) {
                    return profileMatches;
                }
                // Both specified - both must match
                return profileMatches && expressionMatches;
        }
    }
    
    /**
     * Checks if the profile values match the active profiles.
     */
    private boolean matchesProfiles(ConditionContext context) {
        Set<String> activeProfiles = context.getActiveProfiles();
        
        // If no profiles specified, always match
        if (profiles == null || profiles.length == 0) {
            return true;
        }
        
        // Check if ANY of the specified profiles matches (OR logic)
        for (String profile : profiles) {
            if (matchesProfile(profile, activeProfiles)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the expression matches the current context.
     */
    private boolean matchesExpression(ConditionContext context) {
        if (expression == null || expression.isEmpty()) {
            return true;
        }
        
        return ExpressionEvaluator.evaluate(expression, context);
    }
    
    /**
     * Checks if a single profile expression matches.
     * 
     * @param profileExpression the profile expression (may include "!" for negation)
     * @param activeProfiles the set of active profiles
     * @return true if the expression matches
     */
    private boolean matchesProfile(String profileExpression, Set<String> activeProfiles) {
        if (profileExpression == null || profileExpression.isEmpty()) {
            return true;
        }
        
        // Check for negation
        if (profileExpression.startsWith("!")) {
            String negatedProfile = profileExpression.substring(1).trim();
            return !activeProfiles.contains(negatedProfile);
        }
        
        // Simple match
        return activeProfiles.contains(profileExpression.trim());
    }
    
    /**
     * Gets the profiles this condition checks.
     * 
     * @return the profiles array
     */
    public String[] getProfiles() {
        return profiles;
    }
    
    /**
     * Gets the expression for this condition.
     * 
     * @return the expression string
     */
    public String getExpression() {
        return expression;
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
        StringBuilder sb = new StringBuilder("@Profile({");
        for (int i = 0; i < profiles.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(profiles[i]).append("\"");
        }
        sb.append("})");
        
        if (!expression.isEmpty()) {
            sb.append(" with expression: ").append(expression);
        }
        
        if (strategy != MatchStrategy.ALL) {
            sb.append(" strategy: ").append(strategy);
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ProfileCondition{profiles=" + Arrays.toString(profiles) + 
               ", expression='" + expression + "', strategy=" + strategy + "}";
    }
}
