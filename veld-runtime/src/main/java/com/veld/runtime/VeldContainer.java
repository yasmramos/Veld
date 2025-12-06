package com.veld.runtime;

import com.veld.runtime.condition.ConditionContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Ultra-fast dependency injection container for Veld.
 * 
 * <p>Performance optimizations:
 * <ul>
 *   <li>Array-based singleton storage instead of ConcurrentHashMap
 *   <li>IdentityHashMap for O(1) Class-to-index lookups
 *   <li>Double-check locking for lazy singleton initialization
 *   <li>Pre-sized arrays to avoid resizing
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 *   VeldContainer container = new VeldContainer();
 *   MyService service = container.get(MyService.class);
 * </pre>
 */
public class VeldContainer {

    private static final String BOOTSTRAP_CLASS = "com.veld.generated.Veld";
    private static final String CREATE_REGISTRY_METHOD = "createRegistry";
    
    // Cached MethodHandle for optimal performance
    private static volatile MethodHandle createRegistryHandle;
    
    private final ComponentRegistry registry;
    private final Set<String> activeProfiles;
    
    // Ultra-fast singleton storage - array indexed by component index
    private final Object[] singletons;
    
    // Locks for double-check locking on lazy singletons
    private final Object[] locks;
    
    // Pre-computed index cache for common lookups
    private final IdentityHashMap<Class<?>, Integer> indexCache;
    
    // Direct singleton cache by type for ultra-fast get()
    private final IdentityHashMap<Class<?>, Object> singletonCache;
    
    private volatile boolean closed = false;

    /**
     * Creates a new container that automatically loads the generated registry.
     * 
     * @throws VeldException if the registry cannot be loaded
     */
    public VeldContainer() {
        this((Set<String>) null);
    }
    
    /**
     * Creates a new container with the specified active profiles.
     * 
     * @param activeProfiles the profiles to activate (null to read from environment)
     * @throws VeldException if the registry cannot be loaded
     */
    public VeldContainer(Set<String> activeProfiles) {
        this.activeProfiles = activeProfiles;
        this.registry = loadGeneratedRegistry(activeProfiles);
        
        int count = registry.getComponentCount();
        this.singletons = new Object[count];
        this.locks = new Object[count];
        this.indexCache = new IdentityHashMap<>(count * 2);
        this.singletonCache = new IdentityHashMap<>(count * 2);
        
        // Initialize locks for each component
        for (int i = 0; i < count; i++) {
            locks[i] = new Object();
        }
        
        // Build index cache for all registered types
        buildIndexCache();
        
        // Initialize eager singletons
        initializeSingletons();
    }
    
    /**
     * Creates a new container with the given registry.
     *
     * @param registry the component registry
     */
    public VeldContainer(ComponentRegistry registry) {
        this.registry = registry;
        this.activeProfiles = null;
        
        int count = registry.getComponentCount();
        this.singletons = new Object[count];
        this.locks = new Object[count];
        this.indexCache = new IdentityHashMap<>(count * 2);
        this.singletonCache = new IdentityHashMap<>(count * 2);
        
        for (int i = 0; i < count; i++) {
            locks[i] = new Object();
        }
        
        buildIndexCache();
        initializeSingletons();
    }
    
    /**
     * Builds the index cache from registry for ultra-fast lookups.
     */
    private void buildIndexCache() {
        for (ComponentFactory<?> factory : registry.getAllFactories()) {
            int index = factory.getIndex();
            if (index >= 0) {
                indexCache.put(factory.getComponentType(), index);
            }
        }
    }
    
    /**
     * Creates a new container with the specified active profiles.
     * 
     * @param profiles the profiles to activate
     * @return a new container with the specified profiles active
     */
    public static VeldContainer withProfiles(String... profiles) {
        Set<String> profileSet = new HashSet<>(Arrays.asList(profiles));
        return new VeldContainer(profileSet);
    }
    
    /**
     * Gets the active profiles for this container.
     * 
     * @return the set of active profile names
     */
    public Set<String> getActiveProfiles() {
        if (activeProfiles != null) {
            return new HashSet<>(activeProfiles);
        }
        return new ConditionContext(getClass().getClassLoader()).getActiveProfiles();
    }
    
    /**
     * Checks if a specific profile is active.
     * 
     * @param profile the profile name to check
     * @return true if the profile is active
     */
    public boolean isProfileActive(String profile) {
        return getActiveProfiles().contains(profile);
    }
    
    private static ComponentRegistry loadGeneratedRegistry(Set<String> activeProfiles) {
        try {
            MethodHandle handle = getCreateRegistryHandle();
            ComponentRegistry generatedRegistry = (ComponentRegistry) handle.invoke();
            return new ConditionalRegistry(generatedRegistry, activeProfiles);
        } catch (VeldException e) {
            throw e;
        } catch (Throwable e) {
            throw new VeldException("Failed to create registry via bootstrap: " + e.getMessage(), e);
        }
    }
    
    private static MethodHandle getCreateRegistryHandle() {
        MethodHandle handle = createRegistryHandle;
        if (handle == null) {
            synchronized (VeldContainer.class) {
                handle = createRegistryHandle;
                if (handle == null) {
                    handle = lookupCreateRegistryHandle();
                    createRegistryHandle = handle;
                }
            }
        }
        return handle;
    }
    
    private static MethodHandle lookupCreateRegistryHandle() {
        try {
            Class<?> bootstrapClass = Class.forName(BOOTSTRAP_CLASS);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodType methodType = MethodType.methodType(ComponentRegistry.class);
            return lookup.findStatic(bootstrapClass, CREATE_REGISTRY_METHOD, methodType);
        } catch (ClassNotFoundException e) {
            throw new VeldException(
                "Veld bootstrap class not found. Make sure the veld-processor annotation processor " +
                "is configured and at least one @Component class exists.", e);
        } catch (NoSuchMethodException e) {
            throw new VeldException(
                "createRegistry() method not found in bootstrap class.", e);
        } catch (IllegalAccessException e) {
            throw new VeldException(
                "Cannot access createRegistry() method.", e);
        }
    }

    /**
     * Pre-initializes all non-lazy singleton components.
     */
    private void initializeSingletons() {
        List<ComponentFactory<?>> factories = registry.getAllFactories();
        for (int i = 0; i < factories.size(); i++) {
            ComponentFactory<?> factory = factories.get(i);
            if (factory.getScope() == Scope.SINGLETON && !factory.isLazy()) {
                getOrCreateSingleton(i, factory);
            }
        }
    }

    /**
     * Gets a component by its type.
     * Ultra-fast path: direct singleton cache lookup.
     *
     * @param type the component type
     * @param <T> the component type
     * @return the component instance
     * @throws VeldException if no component found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        // Ultra-fast path: check singleton cache directly
        Object cached = singletonCache.get(type);
        if (cached != null) {
            return (T) cached;
        }
        return getSlowPath(type);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getSlowPath(Class<T> type) {
        checkNotClosed();
        
        // Check index cache
        Integer index = indexCache.get(type);
        if (index != null) {
            T instance = getByIndex(index);
            // Cache singleton for ultra-fast future lookups
            if (registry.getScope(index) == Scope.SINGLETON) {
                singletonCache.put(type, instance);
            }
            return instance;
        }
        
        // Fallback: use registry lookup (handles interfaces and supertypes)
        ComponentFactory<T> factory = registry.getFactory(type);
        if (factory == null) {
            throw new VeldException("No component found for type: " + type.getName());
        }
        
        int factoryIndex = factory.getIndex();
        if (factoryIndex >= 0) {
            indexCache.put(type, factoryIndex);
            T instance = getByIndex(factoryIndex);
            if (factory.getScope() == Scope.SINGLETON) {
                singletonCache.put(type, instance);
            }
            return instance;
        }
        
        return getInstance(factory);
    }
    

    
    /**
     * Gets a component by its numeric index (internal).
     */
    @SuppressWarnings("unchecked")
    private <T> T getByIndex(int index) {
        // Fast path: singleton already exists
        Object instance = singletons[index];
        if (instance != null) {
            return (T) instance;
        }
        return getByIndexSlow(index);
    }
    
    /**
     * Slow path for component creation by index.
     */
    @SuppressWarnings("unchecked")
    private <T> T getByIndexSlow(int index) {
        Scope scope = registry.getScope(index);
        
        if (scope == Scope.SINGLETON) {
            // Double-check locking for thread-safe lazy initialization
            synchronized (locks[index]) {
                Object instance = singletons[index];
                if (instance == null) {
                    instance = registry.create(index, this);
                    registry.invokePostConstruct(index, instance);
                    singletons[index] = instance;
                }
                return (T) instance;
            }
        } else {
            // Prototype - create new instance each time
            T instance = registry.create(index, this);
            registry.invokePostConstruct(index, instance);
            return instance;
        }
    }

    /**
     * Gets a component by its name.
     *
     * @param name the component name
     * @param <T> the expected type
     * @return the component instance
     * @throws VeldException if no component found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        checkNotClosed();
        
        int index = registry.getIndex(name);
        if (index >= 0) {
            return getByIndex(index);
        }
        
        // Fallback for non-indexed
        ComponentFactory<?> factory = registry.getFactory(name);
        if (factory == null) {
            throw new VeldException("No component found with name: " + name);
        }
        return (T) getInstance(factory);
    }

    /**
     * Gets a component by its type and name.
     *
     * @param type the component type
     * @param name the component name
     * @param <T> the component type
     * @return the component instance
     * @throws VeldException if no component found
     */
    public <T> T get(Class<T> type, String name) {
        checkNotClosed();
        ComponentFactory<?> factory = registry.getFactory(name);
        if (factory == null) {
            throw new VeldException("No component found with name: " + name);
        }
        if (!type.isAssignableFrom(factory.getComponentType())) {
            throw new VeldException("Component '" + name + "' is not of type: " + type.getName());
        }
        
        int index = factory.getIndex();
        if (index >= 0) {
            return getByIndex(index);
        }
        
        @SuppressWarnings("unchecked")
        T instance = (T) getInstance(factory);
        return instance;
    }

    /**
     * Gets all components of the given type.
     *
     * @param type the component type
     * @param <T> the component type
     * @return list of component instances
     */
    public <T> List<T> getAll(Class<T> type) {
        checkNotClosed();
        
        int[] indices = registry.getIndicesForType(type);
        if (indices.length > 0) {
            List<T> result = new ArrayList<>(indices.length);
            for (int index : indices) {
                result.add(getByIndex(index));
            }
            return result;
        }
        
        // Fallback
        List<ComponentFactory<? extends T>> factories = registry.getFactoriesForType(type);
        List<T> result = new ArrayList<>(factories.size());
        for (ComponentFactory<? extends T> factory : factories) {
            result.add(getInstance(factory));
        }
        return result;
    }

    /**
     * Checks if a component exists for the given type.
     *
     * @param type the component type
     * @return true if a component exists
     */
    public boolean contains(Class<?> type) {
        Integer index = indexCache.get(type);
        if (index != null) {
            return true;
        }
        return registry.getFactory(type) != null;
    }

    /**
     * Checks if a component exists with the given name.
     *
     * @param name the component name
     * @return true if a component exists
     */
    public boolean contains(String name) {
        return registry.getFactory(name) != null;
    }
    
    /**
     * Gets a Provider for the specified type.
     *
     * @param type the component type
     * @param <T> the component type
     * @return a Provider for the component
     */
    public <T> Provider<T> getProvider(Class<T> type) {
        checkNotClosed();
        
        Integer index = indexCache.get(type);
        if (index != null) {
            final int idx = index;
            return () -> getByIndex(idx);
        }
        
        ComponentFactory<T> factory = registry.getFactory(type);
        if (factory == null) {
            throw new VeldException("No component found for type: " + type.getName());
        }
        return () -> getInstance(factory);
    }
    
    /**
     * Gets a Provider for the specified type and name.
     *
     * @param type the component type
     * @param name the component name
     * @param <T> the component type
     * @return a Provider for the component
     */
    public <T> Provider<T> getProvider(Class<T> type, String name) {
        checkNotClosed();
        ComponentFactory<?> factory = registry.getFactory(name);
        if (factory == null) {
            throw new VeldException("No component found with name: " + name);
        }
        if (!type.isAssignableFrom(factory.getComponentType())) {
            throw new VeldException("Component '" + name + "' is not of type: " + type.getName());
        }
        
        int index = factory.getIndex();
        if (index >= 0) {
            return () -> getByIndex(index);
        }
        
        @SuppressWarnings("unchecked")
        ComponentFactory<T> typedFactory = (ComponentFactory<T>) factory;
        return () -> getInstance(typedFactory);
    }
    
    /**
     * Gets a lazy instance wrapper for the specified type.
     *
     * @param type the component type
     * @param <T> the component type
     * @return the component on first access
     */
    public <T> T getLazy(Class<T> type) {
        checkNotClosed();
        return get(type);
    }
    
    /**
     * Tries to get a component by its type, returning null if not found.
     *
     * @param type the component type
     * @param <T> the component type
     * @return the component instance, or null if not found
     */
    public <T> T tryGet(Class<T> type) {
        checkNotClosed();
        
        Integer index = indexCache.get(type);
        if (index != null) {
            return getByIndex(index);
        }
        
        ComponentFactory<T> factory = registry.getFactory(type);
        if (factory == null) {
            return null;
        }
        
        int factoryIndex = factory.getIndex();
        if (factoryIndex >= 0) {
            indexCache.put(type, factoryIndex);
            return getByIndex(factoryIndex);
        }
        
        return getInstance(factory);
    }
    
    /**
     * Tries to get a component by its name, returning null if not found.
     *
     * @param name the component name
     * @param <T> the expected type
     * @return the component instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T tryGet(String name) {
        checkNotClosed();
        
        int index = registry.getIndex(name);
        if (index >= 0) {
            return getByIndex(index);
        }
        
        ComponentFactory<?> factory = registry.getFactory(name);
        if (factory == null) {
            return null;
        }
        return (T) getInstance(factory);
    }
    
    /**
     * Gets a component wrapped in an Optional.
     *
     * @param type the component type
     * @param <T> the component type
     * @return Optional containing the component
     */
    public <T> java.util.Optional<T> getOptional(Class<T> type) {
        checkNotClosed();
        T instance = tryGet(type);
        return java.util.Optional.ofNullable(instance);
    }
    
    /**
     * Gets a component by name wrapped in an Optional.
     *
     * @param name the component name
     * @param <T> the expected type
     * @return Optional containing the component
     */
    public <T> java.util.Optional<T> getOptional(String name) {
        checkNotClosed();
        T instance = tryGet(name);
        return java.util.Optional.ofNullable(instance);
    }

    /**
     * Closes the container and destroys all singleton components.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        List<ComponentFactory<?>> factories = registry.getAllFactories();
        for (int i = 0; i < factories.size(); i++) {
            ComponentFactory<?> factory = factories.get(i);
            if (factory.getScope() == Scope.SINGLETON) {
                Object instance = singletons[i];
                if (instance != null) {
                    registry.invokePreDestroy(i, instance);
                }
            }
        }
        Arrays.fill(singletons, null);
    }

    private <T> T getInstance(ComponentFactory<T> factory) {
        if (factory.getScope() == Scope.SINGLETON) {
            int index = factory.getIndex();
            if (index >= 0) {
                return getByIndex(index);
            }
            return getOrCreateSingleton(index, factory);
        }
        return createPrototype(factory);
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrCreateSingleton(int index, ComponentFactory<T> factory) {
        if (index >= 0 && index < singletons.length) {
            Object instance = singletons[index];
            if (instance != null) {
                return (T) instance;
            }
            
            synchronized (locks[index]) {
                instance = singletons[index];
                if (instance == null) {
                    instance = factory.create(this);
                    factory.invokePostConstruct((T) instance);
                    singletons[index] = instance;
                }
                return (T) instance;
            }
        }
        
        // Legacy fallback for unindexed factories
        T instance = factory.create(this);
        factory.invokePostConstruct(instance);
        return instance;
    }

    private <T> T createPrototype(ComponentFactory<T> factory) {
        T instance = factory.create(this);
        factory.invokePostConstruct(instance);
        return instance;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new VeldException("Container has been closed");
        }
    }
    
    /**
     * Returns information about components excluded due to failing conditions.
     * 
     * @return list of excluded component names
     */
    public List<String> getExcludedComponents() {
        if (registry instanceof ConditionalRegistry) {
            return ((ConditionalRegistry) registry).getExcludedComponents();
        }
        return List.of();
    }
    
    /**
     * Checks if a component was excluded due to failing conditions.
     * 
     * @param componentName the component name to check
     * @return true if the component was excluded
     */
    public boolean wasExcluded(String componentName) {
        if (registry instanceof ConditionalRegistry) {
            return ((ConditionalRegistry) registry).wasExcluded(componentName);
        }
        return false;
    }
}
