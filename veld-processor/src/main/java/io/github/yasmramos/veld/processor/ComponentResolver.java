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
     * 
     * @param dependencyType the type name of the dependency (interface or class)
     * @param qualifier the @Named qualifier value, or null if none
     * @return the ComponentInfo that provides this dependency, or null if not found
     */
    public ComponentInfo resolveDependency(String dependencyType, String qualifier) {
        // If a qualifier is specified, try to find by name first
        if (qualifier != null && !qualifier.isEmpty()) {
            ComponentInfo byName = nameToComponent.get(qualifier);
            if (byName != null) {
                return byName;
            }
            // Qualifier might be a class name, try that too
            ComponentInfo byType = typeToComponent.get(qualifier);
            if (byType != null) {
                return byType;
            }
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
     * Gets the factory class name for a dependency type.
     * This is the main method used by ComponentFactorySourceGenerator.
     * 
     * @param dependencyType the type name of the dependency
     * @param qualifier the @Named qualifier value, or null if none
     * @return the fully qualified factory class name
     */
    public String getFactoryClassName(String dependencyType, String qualifier) {
        ComponentInfo component = resolveDependency(dependencyType, qualifier);
        if (component != null) {
            return component.getFactoryClassName();
        }
        // Fall back to generating factory for the dependency type directly
        // This handles cases where the dependency is not a @Component
        return generateFactoryClassNameFromType(dependencyType);
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

    /**
     * Generates a factory class name from a type name.
     * This is the fallback when no component is registered for the type.
     * 
     * @param typeName the fully qualified type name
     * @return the factory class name
     */
    private String generateFactoryClassNameFromType(String typeName) {
        // Handle nested classes - find the last $ for nested class, or last . for package
        int lastDollar = typeName.lastIndexOf('$');
        int lastDot = typeName.lastIndexOf('.');

        String packageName;
        String simpleName;

        if (lastDollar > lastDot) {
            // Nested class like Outer$Inner
            String outerClass = typeName.substring(0, lastDollar);
            packageName = getPackageFromClassName(outerClass);
            simpleName = typeName.substring(lastDollar + 1);
        } else {
            // Regular class
            packageName = getPackageFromClassName(typeName);
            simpleName = typeName.substring(lastDot + 1);
        }

        return packageName.isEmpty() 
            ? "veld." + simpleName + "$VeldFactory" 
            : packageName + ".veld." + simpleName + "$VeldFactory";
    }

    /**
     * Gets the package name from a fully qualified class name.
     */
    private String getPackageFromClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
