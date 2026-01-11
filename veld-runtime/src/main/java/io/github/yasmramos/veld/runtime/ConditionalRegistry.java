package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.runtime.condition.ConditionContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry wrapper that evaluates conditional annotations at initialization time.
 * Components with failing conditions are excluded from registration.
 * 
 * <p>This class wraps the generated VeldRegistry and filters out components
 * whose conditions are not satisfied.</p>
 * 
 * <h3>Condition Evaluation Order:</h3>
 * <ol>
 *   <li>Components without conditions are registered first</li>
 *   <li>Components with @ConditionalOnClass are evaluated next</li>
 *   <li>Components with @ConditionalOnProperty are evaluated</li>
 *   <li>Components with @ConditionalOnMissingBean are evaluated last</li>
 * </ol>
 * 
 * @since 1.0.0
 */
public final class ConditionalRegistry implements ComponentRegistry {
    
    private final Map<Class<?>, ComponentFactory<?>> factoriesByType = new ConcurrentHashMap<>();
    private final Map<String, ComponentFactory<?>> factoriesByName = new ConcurrentHashMap<>();
    private final List<ComponentFactory<?>> allFactories = new ArrayList<>();
    private final Map<Class<?>, List<ComponentFactory<?>>> factoriesBySupertype = new ConcurrentHashMap<>();
    
    private final ConditionContext conditionContext;
    private final List<String> excludedComponents = new ArrayList<>();
    private static final Set<String> activeProfiles = new HashSet<>();
    
    /**
     * Gets active profiles from the programmatic set or system properties/environment variables.
     * 
     * @return array of active profile names
     */
    public static String[] getActiveProfiles() {
        // If profiles were set programmatically, use those
        if (!activeProfiles.isEmpty()) {
            return activeProfiles.toArray(new String[0]);
        }
        
        // Try system property first: veld.profiles.active
        String profiles = System.getProperty("veld.profiles.active");
        if (profiles != null && !profiles.trim().isEmpty()) {
            return profiles.split(",");
        }
        
        // Try environment variable: VELD_PROFILES_ACTIVE
        profiles = System.getenv("VELD_PROFILES_ACTIVE");
        if (profiles != null && !profiles.trim().isEmpty()) {
            return profiles.split(",");
        }
        
        // Try Spring-style property as fallback
        profiles = System.getProperty("spring.profiles.active");
        if (profiles != null && !profiles.trim().isEmpty()) {
            return profiles.split(",");
        }
        
        // No profiles active
        return new String[0];
    }
    
    /**
     * Creates a conditional registry from the original generated registry.
     * Evaluates all conditions and only registers components that pass.
     * 
     * @param originalRegistry the generated registry with all components
     */
    public ConditionalRegistry(ComponentRegistry originalRegistry) {
        this(originalRegistry, (Set<String>) null);
    }
    
    /**
     * Creates a conditional registry with specific active profiles.
     * 
     * @param originalRegistry the generated registry with all components
     * @param activeProfiles the profiles to activate, or null to resolve from environment
     */
    public ConditionalRegistry(ComponentRegistry originalRegistry, Set<String> activeProfiles) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        this.conditionContext = new ConditionContext(classLoader, activeProfiles);
        evaluateAndRegister(originalRegistry.getAllFactories());
    }
    
    /**
     * Creates a conditional registry with a specific class loader.
     * 
     * @param originalRegistry the generated registry with all components
     * @param classLoader the class loader for class presence checks
     */
    public ConditionalRegistry(ComponentRegistry originalRegistry, ClassLoader classLoader) {
        this.conditionContext = new ConditionContext(classLoader, null);
        evaluateAndRegister(originalRegistry.getAllFactories());
    }
    
    /**
     * Evaluates conditions and registers components in the correct order.
     */
    private void evaluateAndRegister(List<ComponentFactory<?>> factories) {
        // Separate factories by their condition types for ordered evaluation
        List<ComponentFactory<?>> noConditions = new ArrayList<>();
        List<ComponentFactory<?>> withConditions = new ArrayList<>();
        
        for (ComponentFactory<?> factory : factories) {
            if (factory.hasConditions()) {
                withConditions.add(factory);
            } else {
                noConditions.add(factory);
            }
        }
        
        // Register components without conditions first
        for (ComponentFactory<?> factory : noConditions) {
            registerFactory(factory);
        }
        
        // Evaluate and register components with conditions
        // @ConditionalOnMissingBean depends on other registrations, so order matters
        for (ComponentFactory<?> factory : withConditions) {
            if (factory.evaluateConditions(conditionContext)) {
                registerFactory(factory);
            } else {
                excludedComponents.add(factory.getComponentName());
            }
        }
    }
    
    /**
     * Registers a factory and updates the condition context.
     */
    private void registerFactory(ComponentFactory<?> factory) {
        Class<?> componentType = factory.getComponentType();
        String componentName = factory.getComponentName();
        
        // Register by type
        factoriesByType.put(componentType, factory);
        
        // Register by name
        factoriesByName.put(componentName, factory);
        
        // Add to all factories list
        allFactories.add(factory);
        
        // Register in supertype map
        registerInSupertypeMap(componentType, factory);
        
        // Register implemented interfaces
        for (String interfaceName : factory.getImplementedInterfaces()) {
            try {
                Class<?> interfaceClass = Class.forName(interfaceName, false, 
                        conditionContext.getClassLoader());
                factoriesByType.put(interfaceClass, factory);
                registerInSupertypeMap(interfaceClass, factory);
            } catch (ClassNotFoundException e) {
                // Interface not available, skip
            }
        }
        
        // Update condition context for @ConditionalOnMissingBean evaluation
        conditionContext.registerBeanName(componentName);
        conditionContext.registerBeanType(componentType.getName());
        conditionContext.registerBeanInterfaces(factory.getImplementedInterfaces());
    }
    
    private void registerInSupertypeMap(Class<?> type, ComponentFactory<?> factory) {
        factoriesBySupertype
                .computeIfAbsent(type, k -> new ArrayList<>())
                .add(factory);
    }
    
    @Override
    public List<ComponentFactory<?>> getAllFactories() {
        return new ArrayList<>(allFactories);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> ComponentFactory<T> getFactory(Class<T> type) {
        return (ComponentFactory<T>) factoriesByType.get(type);
    }
    
    @Override
    public ComponentFactory<?> getFactory(String name) {
        return factoriesByName.get(name);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) {
        List<ComponentFactory<?>> factories = factoriesBySupertype.get(type);
        if (factories == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>((List<ComponentFactory<? extends T>>) (List<?>) factories);
    }
    
    /**
     * Returns a list of component names that were excluded due to failing conditions.
     * Useful for debugging and logging.
     * 
     * @return list of excluded component names
     */
    public List<String> getExcludedComponents() {
        return new ArrayList<>(excludedComponents);
    }
    
    /**
     * Checks if a component was excluded due to conditions.
     * 
     * @param componentName the component name
     * @return true if the component was excluded
     */
    public boolean wasExcluded(String componentName) {
        return excludedComponents.contains(componentName);
    }
    
    /**
     * Returns the total number of registered components.
     * 
     * @return number of registered components
     */
    public int getRegisteredCount() {
        return allFactories.size();
    }
    
    /**
     * Returns the number of excluded components.
     * 
     * @return number of excluded components
     */
    public int getExcludedCount() {
        return excludedComponents.size();
    }
    
    /**
     * Sets the active profiles programmatically.
     * 
     * @param profiles the profile names to set as active
     */
    public static void setActiveProfiles(String... profiles) {
        activeProfiles.clear();
        if (profiles != null) {
            for (String profile : profiles) {
                if (profile != null && !profile.trim().isEmpty()) {
                    activeProfiles.add(profile.trim());
                }
            }
        }
    }
    
    /**
     * Checks if a specific profile is active.
     * 
     * @param profile the profile name to check
     * @return true if the profile is active
     */
    public static boolean isProfileActive(String profile) {
        if (profile == null) {
            return false;
        }
        return activeProfiles.contains(profile.trim());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSingleton(Class<T> type) {
        ComponentFactory<T> factory = getFactory(type);
        if (factory == null) {
            return null;
        }

        // For singleton scope, use the cache
        if (factory.getScope() == io.github.yasmramos.veld.annotation.ScopeType.SINGLETON) {
            // We need to maintain a separate singleton cache for ConditionalRegistry
            // since the original registry's cache may contain excluded components
            return getOrCreateSingleton(type, factory);
        } else {
            // For prototype scope, create new instance
            return factory.create();
        }
    }

    // Separate cache for ConditionalRegistry singletons
    private final Map<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private <T> T getOrCreateSingleton(Class<T> type, ComponentFactory<T> factory) {
        synchronized (singletonCache) {
            Object cached = singletonCache.get(type);
            if (cached != null) {
                return (T) cached;
            }
            T instance = factory.create();
            singletonCache.put(type, instance);
            return instance;
        }
    }
}
