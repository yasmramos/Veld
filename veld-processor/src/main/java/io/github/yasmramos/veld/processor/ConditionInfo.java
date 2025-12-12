package io.github.yasmramos.veld.processor;

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
     * Adds a profile condition.
     */
    public void addProfileCondition(List<String> profiles) {
        profileConditions.add(new ProfileConditionInfo(profiles));
    }
    
    /**
     * Checks if any conditions are defined.
     */
    public boolean hasConditions() {
        return !propertyConditions.isEmpty() || 
               !classConditions.isEmpty() || 
               !missingBeanConditions.isEmpty() ||
               !profileConditions.isEmpty();
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
    
    public List<ProfileConditionInfo> getProfileConditions() {
        return profileConditions;
    }
    
    /**
     * Property condition info.
     */
    public static final class PropertyConditionInfo {
        private final String name;
        private final String havingValue;
        private final boolean matchIfMissing;
        
        public PropertyConditionInfo(String name, String havingValue, boolean matchIfMissing) {
            this.name = name;
            this.havingValue = havingValue;
            this.matchIfMissing = matchIfMissing;
        }
        
        public String getName() {
            return name;
        }
        
        public String getHavingValue() {
            return havingValue;
        }
        
        public boolean isMatchIfMissing() {
            return matchIfMissing;
        }
    }
    
    /**
     * Class condition info.
     */
    public static final class ClassConditionInfo {
        private final List<String> classNames;
        
        public ClassConditionInfo(List<String> classNames) {
            this.classNames = classNames;
        }
        
        public List<String> getClassNames() {
            return classNames;
        }
    }
    
    /**
     * Missing bean condition info.
     */
    public static final class MissingBeanConditionInfo {
        private final List<String> beanTypes;
        private final List<String> beanNames;
        
        public MissingBeanConditionInfo(List<String> beanTypes, List<String> beanNames) {
            this.beanTypes = beanTypes;
            this.beanNames = beanNames;
        }
        
        public List<String> getBeanTypes() {
            return beanTypes;
        }
        
        public List<String> getBeanNames() {
            return beanNames;
        }
    }
    
    /**
     * Profile condition info.
     */
    public static final class ProfileConditionInfo {
        private final List<String> profiles;
        
        public ProfileConditionInfo(List<String> profiles) {
            this.profiles = profiles;
        }
        
        public List<String> getProfiles() {
            return profiles;
        }
    }
}
