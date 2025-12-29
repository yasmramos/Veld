package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.Profile;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds condition metadata discovered during annotation processing.
 * This information is used to generate conditional registration code.
 */
public final class ConditionInfo {
    
    // Property conditions
    private final List<PropertyConditionInfo> propertyConditions = new ArrayList<>();
    
    // Class conditions
    private final List<ClassConditionInfo> classConditions = new ArrayList<>();
    
    // Missing bean conditions
    private final List<MissingBeanConditionInfo> missingBeanConditions = new ArrayList<>();
    
    // Present bean conditions (beans that must exist)
    private final List<PresentBeanConditionInfo> presentBeanConditions = new ArrayList<>();
    
    // Profile conditions
    private final List<ProfileConditionInfo> profileConditions = new ArrayList<>();
    
    /**
     * Adds a property condition.
     */
    public void addPropertyCondition(String name, String havingValue, boolean matchIfMissing) {
        propertyConditions.add(new PropertyConditionInfo(name, havingValue, matchIfMissing));
    }
    
    /**
     * Adds a class presence condition.
     */
    public void addClassCondition(List<String> classNames) {
        classConditions.add(new ClassConditionInfo(classNames));
    }
    
    /**
     * Adds a missing bean condition by types.
     */
    public void addMissingBeanTypeCondition(List<String> beanTypes) {
        missingBeanConditions.add(new MissingBeanConditionInfo(beanTypes, new ArrayList<>()));
    }
    
    /**
     * Adds a missing bean condition by names.
     */
    public void addMissingBeanNameCondition(List<String> beanNames) {
        missingBeanConditions.add(new MissingBeanConditionInfo(new ArrayList<>(), beanNames));
    }
    
    /**
     * Adds a present bean condition by types.
     * The bean will only be registered if all specified bean types exist.
     */
    public void addPresentBeanTypeCondition(List<String> beanTypes) {
        presentBeanConditions.add(new PresentBeanConditionInfo(beanTypes, new ArrayList<>(), true, false));
    }
    
    /**
     * Adds a present bean condition by types with strategy.
     * 
     * @param beanTypes bean types that must be present
     * @param matchAll if true, all types must match; if false, any type matches
     */
    public void addPresentBeanTypeCondition(List<String> beanTypes, boolean matchAll) {
        presentBeanConditions.add(new PresentBeanConditionInfo(beanTypes, new ArrayList<>(), matchAll, false));
    }
    
    /**
     * Adds a present bean condition by names.
     * The bean will only be registered if all specified bean names exist.
     */
    public void addPresentBeanNameCondition(List<String> beanNames) {
        presentBeanConditions.add(new PresentBeanConditionInfo(new ArrayList<>(), beanNames, true, false));
    }
    
    /**
     * Adds a present bean condition by names with strategy.
     * 
     * @param beanNames bean names that must be present
     * @param matchAll if true, all names must match; if false, any name matches
     */
    public void addPresentBeanNameCondition(List<String> beanNames, boolean matchAll) {
        presentBeanConditions.add(new PresentBeanConditionInfo(new ArrayList<>(), beanNames, matchAll, false));
    }
    
    /**
     * Adds a present bean condition with custom strategy.
     * 
     * @param beanTypes bean types that must be present
     * @param beanNames bean names that must be present
     * @param matchAll if true, all conditions must match; if false, any condition matches
     */
    public void addPresentBeanCondition(List<String> beanTypes, List<String> beanNames, boolean matchAll) {
        presentBeanConditions.add(new PresentBeanConditionInfo(beanTypes, beanNames, matchAll, false));
    }
    
    /**
     * Adds a profile condition.
     */
    public void addProfileCondition(List<String> profiles) {
        profileConditions.add(new ProfileConditionInfo(profiles, "", Profile.MatchStrategy.ALL));
    }
    
    /**
     * Adds a profile condition with expression.
     */
    public void addProfileCondition(List<String> profiles, String expression, Profile.MatchStrategy strategy) {
        profileConditions.add(new ProfileConditionInfo(profiles, expression, strategy));
    }
    
    /**
     * Checks if any conditions are defined.
     */
    public boolean hasConditions() {
        return !propertyConditions.isEmpty() || 
               !classConditions.isEmpty() || 
               !missingBeanConditions.isEmpty() ||
               !presentBeanConditions.isEmpty() ||
               !profileConditions.isEmpty();
    }
    
    /**
     * Checks if any bean presence conditions are defined.
     * This includes both present and missing bean conditions.
     */
    public boolean hasBeanConditions() {
        return !missingBeanConditions.isEmpty() || !presentBeanConditions.isEmpty();
    }
    
    public List<PropertyConditionInfo> getPropertyConditions() {
        return propertyConditions;
    }
    
    public List<ClassConditionInfo> getClassConditions() {
        return classConditions;
    }
    
    public List<MissingBeanConditionInfo> getMissingBeanConditions() {
        return missingBeanConditions;
    }
    
    public List<PresentBeanConditionInfo> getPresentBeanConditions() {
        return presentBeanConditions;
    }
    
    public List<ProfileConditionInfo> getProfileConditions() {
        return profileConditions;
    }
    
    /** Property condition info (Java 17 record) */
    public record PropertyConditionInfo(String name, String havingValue, boolean matchIfMissing) {}
    
    /** Class condition info (Java 17 record) */
    public record ClassConditionInfo(List<String> classNames) {}
    
    /** Missing bean condition info (Java 17 record) */
    public record MissingBeanConditionInfo(List<String> beanTypes, List<String> beanNames) {}
    
    /** Present bean condition info (Java 17 record) */
    public record PresentBeanConditionInfo(
        List<String> beanTypes, 
        List<String> beanNames,
        boolean matchAll,
        boolean considerHierarchy
    ) {}
    
    /** Profile condition info (Java 17 record) */
    public record ProfileConditionInfo(
        List<String> profiles,
        String expression,
        io.github.yasmramos.veld.annotation.Profile.MatchStrategy strategy
    ) {}
}
