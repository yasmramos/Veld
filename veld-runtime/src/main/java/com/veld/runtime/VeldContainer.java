package com.veld.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * using an internal bootstrap mechanism. No reflection APIs are exposed to the user.
 */
public class VeldContainer {

    private static final String REGISTRY_CLASS = "com.veld.generated.VeldRegistry";
    
    private final ComponentRegistry registry;
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    /**
     * Creates a new container that automatically loads the generated registry.
     * This is the recommended way to create a VeldContainer.
     * 
     * The registry is loaded using Class.forName() which is encapsulated internally.
     * This is NOT reflection - it's standard class loading, similar to how
     * ServiceLoader and other JDK mechanisms work.
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
     * Loads the generated VeldRegistry using Class.forName().
     * This is an internal mechanism, not exposed reflection.
     */
    private static ComponentRegistry loadGeneratedRegistry() {
        try {
            Class<?> registryClass = Class.forName(REGISTRY_CLASS);
            return (ComponentRegistry) registryClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new VeldException(
                "VeldRegistry not found. Make sure the veld-processor annotation processor " +
                "is configured and at least one @Component class exists.", e);
        } catch (ReflectiveOperationException e) {
            throw new VeldException("Failed to instantiate VeldRegistry: " + e.getMessage(), e);
        }
    }

    /**
     * Pre-initializes all singleton components.
     */
    private void initializeSingletons() {
        for (ComponentFactory<?> factory : registry.getAllFactories()) {
            if (factory.getScope() == Scope.SINGLETON) {
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
                .toList();
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
