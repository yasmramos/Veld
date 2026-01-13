package io.github.yasmramos.veld.runtime.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import java.lang.invoke.MethodHandles;

/**
 * Context object passed to conditions during evaluation.
 * Provides access to the environment, active profiles, and already-registered beans.
 * 
 * @since 1.0.0
 */
public final class ConditionContext {
    
    /** System property for active profiles */
    public static final String PROFILES_PROPERTY = "veld.profiles.active";
    
    /** Environment variable for active profiles */
    public static final String PROFILES_ENV_VAR = "VELD_PROFILES_ACTIVE";
    
    /** Default profile name when no profiles are explicitly active */
    public static final String DEFAULT_PROFILE = "default";
    
    private final Set<String> registeredBeanNames;
    private final Set<String> registeredBeanTypes;
    private final Set<String> activeProfiles;
    private final ClassLoader classLoader;
    
    /**
     * Creates a new condition context.
     * 
     * @param classLoader the class loader to use for class checks
     */
    public ConditionContext(ClassLoader classLoader) {
        this(classLoader, null);
    }
    
    /**
     * Creates a new condition context with specified active profiles.
     * 
     * @param classLoader the class loader to use for class checks
     * @param activeProfiles the active profiles, or null to read from environment
     */
    public ConditionContext(ClassLoader classLoader, Set<String> activeProfiles) {
        this.registeredBeanNames = new HashSet<>();
        this.registeredBeanTypes = new HashSet<>();
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        this.activeProfiles = activeProfiles != null ? new HashSet<>(activeProfiles) : resolveActiveProfiles();
    }
    
    /**
     * Registers a bean name as present.
     * Called after a bean passes its condition check.
     * 
     * @param name the bean name
     */
    public void registerBeanName(String name) {
        registeredBeanNames.add(name);
    }
    
    /**
     * Registers a bean type as present.
     * Called after a bean passes its condition check.
     * 
     * @param type the fully qualified class name
     */
    public void registerBeanType(String type) {
        registeredBeanTypes.add(type);
    }
    
    /**
     * Registers interface types that a bean implements.
     * 
     * @param interfaces the fully qualified interface names
     */
    public void registerBeanInterfaces(Iterable<String> interfaces) {
        for (String iface : interfaces) {
            registeredBeanTypes.add(iface);
        }
    }
    
    /**
     * Checks if a bean with the given name is registered.
     * 
     * @param name the bean name
     * @return true if a bean with this name exists
     */
    public boolean containsBeanName(String name) {
        return registeredBeanNames.contains(name);
    }
    
    /**
     * Checks if a bean of the given type is registered.
     * 
     * @param type the fully qualified class name
     * @return true if a bean of this type exists
     */
    public boolean containsBeanType(String type) {
        return registeredBeanTypes.contains(type);
    }
    
    /**
     * Gets a system property or environment variable.
     * Checks system properties first, then environment variables.
     * 
     * @param name the property name
     * @return the property value, or null if not found
     */
    public String getProperty(String name) {
        // Check system properties first
        String value = System.getProperty(name);
        if (value != null) {
            return value;
        }
        
        // Check environment variables
        value = System.getenv(name);
        if (value != null) {
            return value;
        }
        
        // Try with underscores instead of dots (common for env vars)
        String envName = name.replace('.', '_').toUpperCase();
        return System.getenv(envName);
    }
    
    /**
     * Checks if a class is present on the classpath.
     * 
     * @param className the fully qualified class name
     * @return true if the class is present
     */
    public boolean isClassPresent(String className) {
        try {
            classLoader.loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Gets the class loader used for class checks.
     * 
     * @return the class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    /**
     * Gets the set of active profiles.
     * 
     * @return an unmodifiable set of active profile names
     */
    public Set<String> getActiveProfiles() {
        return Collections.unmodifiableSet(activeProfiles);
    }
    
    /**
     * Checks if a specific profile is active.
     * 
     * @param profile the profile name to check
     * @return true if the profile is active
     */
    public boolean isProfileActive(String profile) {
        return activeProfiles.contains(profile);
    }
    
    /**
     * Checks if the default profile is active.
     * The default profile is active when no explicit profiles are set.
     * 
     * @return true if the default profile is active
     */
    public boolean isDefaultProfileActive() {
        return activeProfiles.contains(DEFAULT_PROFILE);
    }
    
    /**
     * Resolves active profiles from system properties and environment variables.
     * 
     * @return the set of active profiles
     */
    private Set<String> resolveActiveProfiles() {
        Set<String> profiles = new HashSet<>();
        
        // Check system property first
        String profilesValue = System.getProperty(PROFILES_PROPERTY);
        
        // Then check environment variable
        if (profilesValue == null || profilesValue.isEmpty()) {
            profilesValue = System.getenv(PROFILES_ENV_VAR);
        }
        
        // Parse profiles (comma-separated)
        if (profilesValue != null && !profilesValue.isEmpty()) {
            String[] parts = profilesValue.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    profiles.add(trimmed);
                }
            }
        }
        
        // If no profiles are active, add the default profile
        if (profiles.isEmpty()) {
            profiles.add(DEFAULT_PROFILE);
        }
        
        return profiles;
    }
    
    @Override
    public String toString() {
        return "ConditionContext{activeProfiles=" + activeProfiles + 
               ", registeredBeans=" + registeredBeanNames.size() + "}";
    }
}
