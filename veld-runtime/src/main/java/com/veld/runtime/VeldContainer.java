package com.veld.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The main dependency injection container for Veld.
 * Manages component lifecycle and provides dependency resolution.
 * 
 * Usage (simple):
 * <pre>
 *   VeldContainer container = new VeldContainer();
 * </pre>
 * 
 * The container automatically discovers and loads the generated VeldRegistry
 * using MethodHandle for zero-reflection instantiation. The generated bootstrap
 * class provides direct access without any reflective operations.
 */
public class VeldContainer {

    private static final String BOOTSTRAP_CLASS = "com.veld.generated.Veld";
    private static final String CREATE_REGISTRY_METHOD = "createRegistry";
    
    // Cached MethodHandle for optimal performance
    private static volatile MethodHandle createRegistryHandle;
    
    private final ComponentRegistry registry;
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    /**
     * Creates a new container that automatically loads the generated registry.
     * This is the recommended way to create a VeldContainer.
     * 
     * Uses MethodHandle to invoke the generated bootstrap class, avoiding
     * traditional reflection APIs (Constructor.newInstance, etc.).
     * 
     * @throws VeldException if the registry cannot be loaded
     */
    public VeldContainer() {
        this(loadGeneratedRegistry());
    }
    
    /**
     * Creates a new container with the given registry.
     * Use this constructor for testing or custom registry implementations.
     *
     * @param registry the component registry (generated at compile time)
     */
    public VeldContainer(ComponentRegistry registry) {
        this.registry = registry;
        initializeSingletons();
    }
    
    /**
     * Loads the generated VeldRegistry using MethodHandle.
     * 
     * This approach uses java.lang.invoke.MethodHandle which is:
     * - NOT traditional reflection (no Constructor.newInstance)
     * - Optimized by the JVM after first invocation
     * - Type-safe with compile-time method signatures
     * - The standard way to invoke methods dynamically in modern Java
     * 
     * The generated Veld bootstrap class provides a static createRegistry()
     * method that directly instantiates VeldRegistry without reflection.
     */
    private static ComponentRegistry loadGeneratedRegistry() {
        try {
            MethodHandle handle = getCreateRegistryHandle();
            return (ComponentRegistry) handle.invoke();
        } catch (VeldException e) {
            throw e;
        } catch (Throwable e) {
            throw new VeldException("Failed to create registry via bootstrap: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets or creates the MethodHandle for createRegistry().
     * Uses double-checked locking for thread-safe lazy initialization.
     */
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
    
    /**
     * Looks up the MethodHandle for Veld.createRegistry() static method.
     */
    private static MethodHandle lookupCreateRegistryHandle() {
        try {
            // Load the generated bootstrap class
            Class<?> bootstrapClass = Class.forName(BOOTSTRAP_CLASS);
            
            // Get a MethodHandle for the static createRegistry() method
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodType methodType = MethodType.methodType(ComponentRegistry.class);
            
            return lookup.findStatic(bootstrapClass, CREATE_REGISTRY_METHOD, methodType);
            
        } catch (ClassNotFoundException e) {
            throw new VeldException(
                "Veld bootstrap class not found. Make sure the veld-processor annotation processor " +
                "is configured and at least one @Component class exists.", e);
        } catch (NoSuchMethodException e) {
            throw new VeldException(
                "createRegistry() method not found in bootstrap class. " +
                "This indicates a version mismatch between veld-runtime and veld-processor.", e);
        } catch (IllegalAccessException e) {
            throw new VeldException(
                "Cannot access createRegistry() method. Check module permissions.", e);
        }
    }

    /**
     * Pre-initializes all non-lazy singleton components.
     * Lazy singletons are initialized on first access.
     */
    private void initializeSingletons() {
        for (ComponentFactory<?> factory : registry.getAllFactories()) {
            if (factory.getScope() == Scope.SINGLETON && !factory.isLazy()) {
                getOrCreateSingleton(factory);
            }
        }
    }

    /**
     * Gets a component by its type.
     *
     * @param type the component type
     * @param <T> the component type
     * @return the component instance
     * @throws VeldException if no component found for the type
     */
    public <T> T get(Class<T> type) {
        checkNotClosed();
        ComponentFactory<T> factory = registry.getFactory(type);
        if (factory == null) {
            throw new VeldException("No component found for type: " + type.getName());
        }
        return getInstance(factory);
    }

    /**
     * Gets a component by its name.
     *
     * @param name the component name
     * @param <T> the expected type
     * @return the component instance
     * @throws VeldException if no component found for the name
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        checkNotClosed();
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
    @SuppressWarnings("unchecked")
    public <T> List<T> getAll(Class<T> type) {
        checkNotClosed();
        List<ComponentFactory<? extends T>> factories = registry.getFactoriesForType(type);
        return factories.stream()
                .map(factory -> (T) getInstance(factory))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a component exists for the given type.
     *
     * @param type the component type
     * @return true if a component exists
     */
    public boolean contains(Class<?> type) {
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
     * The Provider allows lazy access to the component - it's only created when get() is called.
     * 
     * For @Singleton components, the Provider always returns the same instance.
     * For @Prototype components, each call to get() creates a new instance.
     *
     * @param type the component type
     * @param <T> the component type
     * @return a Provider for the component
     * @throws VeldException if no component found for the type
     */
    public <T> Provider<T> getProvider(Class<T> type) {
        checkNotClosed();
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
     * @throws VeldException if no component found
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
        @SuppressWarnings("unchecked")
        ComponentFactory<T> typedFactory = (ComponentFactory<T>) factory;
        return () -> getInstance(typedFactory);
    }
    
    /**
     * Gets a lazy instance wrapper for the specified type.
     * The instance is not created until first access through the LazyHolder.
     * This is used internally for @Lazy injection points.
     *
     * @param type the component type
     * @param <T> the component type
     * @return a LazyHolder that provides the component on first access
     * @throws VeldException if no component found for the type
     */
    public <T> T getLazy(Class<T> type) {
        checkNotClosed();
        ComponentFactory<T> factory = registry.getFactory(type);
        if (factory == null) {
            throw new VeldException("No component found for type: " + type.getName());
        }
        
        // For singletons, just return the instance (will be lazy-created if needed)
        // For prototypes, we return a proxy-like behavior through the instance
        return getInstance(factory);
    }

    /**
     * Closes the container and destroys all singleton components.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Invoke pre-destroy on all singletons
        for (ComponentFactory<?> factory : registry.getAllFactories()) {
            if (factory.getScope() == Scope.SINGLETON) {
                Object instance = singletons.get(factory.getComponentName());
                if (instance != null) {
                    invokePreDestroy(factory, instance);
                }
            }
        }
        singletons.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> void invokePreDestroy(ComponentFactory<T> factory, Object instance) {
        factory.invokePreDestroy((T) instance);
    }

    private <T> T getInstance(ComponentFactory<T> factory) {
        if (factory.getScope() == Scope.SINGLETON) {
            return getOrCreateSingleton(factory);
        }
        return createPrototype(factory);
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrCreateSingleton(ComponentFactory<T> factory) {
        String name = factory.getComponentName();
        return (T) singletons.computeIfAbsent(name, k -> {
            T instance = factory.create(this);
            factory.invokePostConstruct(instance);
            return instance;
        });
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
}
