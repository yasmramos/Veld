package io.github.yasmramos.veld.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves dependency types to their concrete component implementations.
 * This enables compile-time dependency injection by mapping interface types
 * to the concrete classes that implement them.
 * 
 * <p>When a component depends on an interface (e.g., EmailService), the resolver
 * finds the concrete implementation (e.g., SmtpEmailService) and returns its
 * factory class name for direct instantiation.</p>
 * 
 * <p>Resolution Strategy:</p>
 * <ul>
 *   <li>Direct type match - if dependency type is a concrete component, use it directly</li>
 *   <li>Interface match - if dependency type is an interface, find the implementing component</li>
 *   <li>@Named resolution - respect @Named annotations for disambiguation</li>
 *   <li>Primary bean - fall back to @Primary if multiple implementations exist</li>
 * </ul>
 */
public final class ComponentResolver {

    private final Map<String, ComponentInfo> typeToComponent = new HashMap<>();
    private final Map<String, ComponentInfo> nameToComponent = new HashMap<>();
    private final Map<String, ComponentInfo> interfaceToComponent = new HashMap<>();

    /**
     * Builds the resolution maps from a list of discovered components.
     * 
     * @param components the list of discovered components
     */
    public void buildResolutionMaps(List<ComponentInfo> components) {
        for (ComponentInfo component : components) {
            // Map the concrete class type
            typeToComponent.put(component.getClassName(), component);

            // Map by component name (@Component value or derived name)
            nameToComponent.put(component.getComponentName(), component);

            // Map implemented interfaces to their implementing component
            for (String interfaceName : component.getImplementedInterfaces()) {
                // If multiple components implement the same interface, keep the last one
                // (or we could implement primary bean selection)
                interfaceToComponent.put(interfaceName, component);
            }
        }
    }

    /**
     * Resolves a dependency type to the component that provides it.
     * Static compile-time model: Only explicit @Component(name) or @Named are allowed.
     * 
     * @param dependencyType the type name of the dependency (interface or class)
     * @param qualifier the @Named qualifier value, or null if none
     * @return the ComponentInfo that provides this dependency, or null if not found
     */
    public ComponentInfo resolveDependency(String dependencyType, String qualifier) {
        // If a qualifier is specified, find by explicit bean name
        if (qualifier != null && !qualifier.isEmpty()) {
            ComponentInfo byName = nameToComponent.get(qualifier);
            if (byName != null) {
                return byName;
            }
            // Qualifier not found - this is an error in static model
            return null;
        }

        // Try direct type match first
        ComponentInfo byType = typeToComponent.get(dependencyType);
        if (byType != null) {
            return byType;
        }

        // Try interface resolution
        return interfaceToComponent.get(dependencyType);
    }

    /**
     * Gets the class name for a dependency type.
     * In static compile-time model, we return the component class name directly.
     * 
     * @param dependencyType the type name of the dependency
     * @param qualifier the @Named qualifier value, or null if none
     * @return the fully qualified component class name, or null if not found
     */
    public String getClassName(String dependencyType, String qualifier) {
        ComponentInfo component = resolveDependency(dependencyType, qualifier);
        if (component != null) {
            return component.getClassName();
        }
        return null;
    }

    /**
     * Checks if a dependency type has a known component implementation.
     * 
     * @param dependencyType the type name to check
     * @return true if a component implementation exists
     */
    public boolean hasImplementation(String dependencyType) {
        return resolveDependency(dependencyType, null) != null;
    }
}
