package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.annotation.ScopeType;
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

    /**
     * Creates a conditional registry from the original generated registry.
     * Evaluates all conditions and only registers components that pass.
     *
     * @param originalRegistry the generated registry with all components
     */
    public ConditionalRegistry(ComponentRegistry originalRegistry) {
        this.conditionContext = new ConditionContext();
        evaluateAndRegister(originalRegistry.getAllFactories());
    }

    /**
     * Creates a conditional registry with a specific class loader.
     *
     * @param originalRegistry the generated registry with all components
     * @param classLoader the class loader for class presence checks
     */
    public ConditionalRegistry(ComponentRegistry originalRegistry, ClassLoader classLoader) {
        this.conditionContext = new ConditionContext(classLoader);
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
            if (!factory.hasConditions()) {
                noConditions.add(factory);
            } else {
                withConditions.add(factory);
            }
        }

        // First, register all components without conditions
        for (ComponentFactory<?> factory : noConditions) {
            registerInternal(factory);
        }

        // Then, evaluate and register components with conditions
        for (ComponentFactory<?> factory : withConditions) {
            try {
                if (factory.evaluateConditions(conditionContext)) {
                    registerInternal(factory);
                } else {
                    String name = factory.getComponentName() != null ? factory.getComponentName() : factory.getComponentType().getName();
                    excludedComponents.add(name);
                }
            } catch (Exception e) {
                String name = factory.getComponentName() != null ? factory.getComponentName() : factory.getComponentType().getName();
                excludedComponents.add(name);
            }
        }
    }

    /**
     * Registers a factory internally without condition checking.
     */
    private void registerInternal(ComponentFactory<?> factory) {
        allFactories.add(factory);

        // Register by type
        Class<?> componentType = factory.getComponentType();
        factoriesByType.put(componentType, factory);

        // Register supertypes
        for (Class<?> iface : getAllInterfaces(componentType)) {
            factoriesBySupertype.computeIfAbsent(iface, k -> new ArrayList<>()).add(factory);
        }

        // Register by name if component has a name
        if (factory.getComponentName() != null) {
            factoriesByName.put(factory.getComponentName(), factory);
        }
    }

    /**
     * Gets all interfaces implemented by a class (including inherited ones).
     */
    private Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<>();
        while (clazz != null) {
            for (Class<?> iface : clazz.getInterfaces()) {
                interfaces.add(iface);
                interfaces.addAll(getAllInterfaces(iface));
            }
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ComponentFactory<T> getFactory(Class<T> type) {
        ComponentFactory<?> factory = factoriesByType.get(type);
        if (factory != null) {
            return (ComponentFactory<T>) factory;
        }

        // Check supertypes
        List<ComponentFactory<?>> factories = factoriesBySupertype.get(type);
        if (factories != null && !factories.isEmpty()) {
            return (ComponentFactory<T>) factories.get(0);
        }

        return null;
    }

    @Override
    public ComponentFactory<?> getFactory(String name) {
        return factoriesByName.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) {
        List<ComponentFactory<? extends T>> result = new ArrayList<>();

        // Check exact type
        ComponentFactory<?> factory = factoriesByType.get(type);
        if (factory != null) {
            result.add((ComponentFactory<? extends T>) factory);
        }

        // Check supertypes
        List<ComponentFactory<?>> superFactories = factoriesBySupertype.get(type);
        if (superFactories != null) {
            for (ComponentFactory<?> sf : superFactories) {
                result.add((ComponentFactory<? extends T>) sf);
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSingleton(Class<T> type) {
        ComponentFactory<T> factory = getFactory(type);
        if (factory == null) {
            return null;
        }

        // For singleton scope, use the cache
        if (factory.getScope() == ScopeType.SINGLETON) {
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

    @SuppressWarnings("unchecked")
    public <T> T getPrototype(Class<T> type) {
        ComponentFactory<T> factory = getFactory(type);
        if (factory == null) {
            return null;
        }
        return factory.create();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type, String... qualifiers) {
        ComponentFactory<T> factory = getFactory(type);
        if (factory == null) {
            return null;
        }
        return factory.create();
    }

    @Override
    public List<ComponentFactory<?>> getAllFactories() {
        return new ArrayList<>(allFactories);
    }

    public List<String> getExcludedComponents() {
        return new ArrayList<>(excludedComponents);
    }

    public boolean isTypePresent(String typeName) {
        try {
            return conditionContext.isClassPresent(typeName);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getComponentCount() {
        return factoriesByType.size();
    }

    /**
     * Returns the number of registered components.
     *
     * @return the count
     */
    public int getRegisteredCount() {
        return factoriesByType.size();
    }

    /**
     * Returns the number of excluded components.
     *
     * @return the count
     */
    public int getExcludedCount() {
        return excludedComponents.size();
    }

    /**
     * Checks if a component was excluded from registration.
     *
     * @param name the component name
     * @return true if excluded
     */
    public boolean wasExcluded(String name) {
        return excludedComponents.contains(name);
    }

    public void close() {
        // Clean up singleton cache
        singletonCache.clear();

        // Clear all maps
        factoriesByType.clear();
        factoriesByName.clear();
        allFactories.clear();
        factoriesBySupertype.clear();
        excludedComponents.clear();
    }
}
