package com.veld;

import java.util.List;

/**
 * Main entry point for Veld dependency injection.
 * 
 * This is a compile-time stub that gets replaced by the weaver with the actual
 * generated implementation containing all registered components.
 * 
 * Usage:
 * <pre>
 * MyService service = Veld.get(MyService.class);
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
     * @throws IllegalStateException if Veld was not properly initialized by the weaver
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-maven-plugin is configured with <extensions>true</extensions>");
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
            "Veld not initialized. Ensure veld-maven-plugin is configured with <extensions>true</extensions>");
    }
    
    /**
     * Checks if a component of the given type is registered.
     * 
     * @param type the component class or interface
     * @return true if a component is registered for this type
     */
    public static boolean contains(Class<?> type) {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-maven-plugin is configured with <extensions>true</extensions>");
    }
    
    /**
     * Returns the total number of registered components.
     * 
     * @return component count
     */
    public static int componentCount() {
        throw new IllegalStateException(
            "Veld not initialized. Ensure veld-maven-plugin is configured with <extensions>true</extensions>");
    }
}
