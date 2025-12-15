package io.github.yasmramos.veld;

import io.github.yasmramos.veld.runtime.Provider;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.value.ValueResolver;

import java.util.List;

/**
 * Main entry point for Veld dependency injection.
 * 
 * This is a compile-time stub that gets replaced by the processor with the actual
 * generated implementation containing all registered components.
 *
 * Usage:
 * <pre>
 * MyService service = Veld.get(MyService.class);
 * EventBus bus = Veld.getEventBus();
 * String value = Veld.resolveValue("${app.name}");
 * Veld.setActiveProfiles("dev", "test");
 * </pre>
 */
public final class Veld {
    
    private Veld() {}
    
    /**
     * Gets a component instance by type.
     * 
     * @param type the component class or interface
     * @param <T> the component type
     * @return the component instance
     * @throws IllegalStateException if Veld was not properly initialized by the processor
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
    
    /**
     * Gets a component instance by type and name.
     * 
     * @param type the component class or interface
     * @param name the component name (for @Named injection)
     * @param <T> the component type
     * @return the component instance
     * @throws IllegalStateException if Veld was not properly initialized by the processor
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type, String name) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
    
    /**
     * Gets all components assignable to the given type.
     * 
     * @param type the base class or interface
     * @param <T> the component type
     * @return list of matching component instances
     */
    public static <T> List<T> getAll(Class<T> type) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
    
    /**
     * Gets a Provider for lazy access to a component.
     * 
     * @param type the component class or interface
     * @param <T> the component type
     * @return a Provider for the component
     * @throws IllegalStateException if Veld was not properly initialized by the processor
     */
    @SuppressWarnings("unchecked")
    public static <T> Provider<T> getProvider(Class<T> type) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
    
    /**
     * Checks if a component of the given type is registered.
     * 
     * @param type the component class or interface
     * @return true if a component is registered for this type
     */
    public static boolean contains(Class<?> type) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
    
    /**
     * Returns the total number of registered components.
     * 
     * @return component count
     */
    public static int componentCount() {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
    
    /**
     * Gets the EventBus instance for publishing and subscribing to events.
     * 
     * @return the EventBus instance
     */
    public static EventBus getEventBus() {
        return EventBus.getInstance();
    }
    
    /**
     * Resolves a value expression using the ValueResolver.
     * 
     * @param expression the value expression (e.g., "${app.name}" or "${app.name:default}")
     * @return the resolved string value
     * @throws IllegalStateException if Veld was not properly initialized by the processor
     */
    public static String resolveValue(String expression) {
        return ValueResolver.getInstance().resolve(expression);
    }
    
    /**
     * Resolves a value expression and converts it to the specified type.
     * 
     * @param expression the value expression
     * @param targetType the target type class
     * @param <T> the target type
     * @return the resolved and converted value
     * @throws IllegalStateException if Veld was not properly initialized by the processor
     */
    public static <T> T resolveValue(String expression, Class<T> targetType) {
        return ValueResolver.getInstance().resolve(expression, targetType);
    }
    
    /**
     * Sets the active profiles for conditional bean registration.
     * 
     * @param profiles the profiles to activate
     */
    public static void setActiveProfiles(String... profiles) {
        // TODO: Implement profile management
        System.out.println("[Veld] Setting active profiles: " + String.join(", ", profiles));
    }
    
    /**
     * Gets the currently active profiles.
     * 
     * @return array of active profile names
     */
    public static String[] getActiveProfiles() {
        // TODO: Implement profile management
        return new String[0];
    }
    
    /**
     * Checks if a specific profile is active.
     * 
     * @param profile the profile name to check
     * @return true if the profile is active
     */
    public static boolean isProfileActive(String profile) {
        // TODO: Implement profile management
        return false;
    }
    
    /**
     * Shuts down the container, calling @PreDestroy on all singleton components.
     */
    public static void shutdown() {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-processor is in annotation processor path.");
    }
}