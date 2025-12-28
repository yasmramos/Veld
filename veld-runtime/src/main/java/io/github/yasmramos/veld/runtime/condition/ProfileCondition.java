package io.github.yasmramos.veld.runtime.condition;

import java.util.Arrays;
import java.util.Set;

/**
 * Condition that matches when one or more specified profiles are active.
 * 
 * <p>This condition supports:
 * <ul>
 *   <li>Simple profile matching: "dev" matches when "dev" is active</li>
 *   <li>Negation: "!prod" matches when "prod" is NOT active</li>
 *   <li>Multiple profiles: ["dev", "test"] matches when either is active (OR logic)</li>
 * </ul>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public class ProfileCondition implements Condition {
    
    private final String[] profiles;
    
    /**
     * Creates a new profile condition.
     * 
     * @param profiles the profiles to check (any must match)
     */
    public ProfileCondition(String... profiles) {
        this.profiles = profiles;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
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
    
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("@Profile({");
        for (int i = 0; i < profiles.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(profiles[i]).append("\"");
        }
        sb.append("})");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ProfileCondition{profiles=" + Arrays.toString(profiles) + "}";
    }
}
