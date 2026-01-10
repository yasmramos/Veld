package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.ScopeType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Veld.java source code instead of bytecode.
 * Uses the Initialization-on-demand holder idiom for lock-free singleton access.
 */
public final class VeldSourceGenerator {
    
    private final List<ComponentInfo> components;
    private final Map<String, String> aopClassMap;
    
    public VeldSourceGenerator(List<ComponentInfo> components) {
        this(components, Collections.emptyMap());
    }
    
    public VeldSourceGenerator(List<ComponentInfo> components, Map<String, String> aopClassMap) {
        this.components = components;
        this.aopClassMap = aopClassMap;
    }

    public String getClassName() {
        return "io.github.yasmramos.veld.Veld";
    }

    public String generate() {
        StringBuilder sb = new StringBuilder();

        // Package declaration - generate in veld package to replace stub
        sb.append("package io.github.yasmramos.veld;\n\n");

        // Imports
        sb.append("import io.github.yasmramos.veld.VeldRegistry;\n");
        sb.append("import io.github.yasmramos.veld.VeldException;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentRegistry;\n");
        sb.append("import io.github.yasmramos.veld.runtime.Provider;\n");
        sb.append("import io.github.yasmramos.veld.annotation.ScopeType;\n");
        sb.append("import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ConditionalRegistry;\n");
        sb.append("import io.github.yasmramos.veld.runtime.event.EventBus;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentFactory;\n");
        sb.append("import io.github.yasmramos.veld.runtime.value.ValueResolver;\n");
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.concurrent.ConcurrentHashMap;\n");
        sb.append("import java.util.Set;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.Optional;\n");
        sb.append("import java.util.function.Supplier;\n");
        sb.append("import java.util.Comparator;\n");
        sb.append("import java.util.stream.Collectors;\n\n");
        
        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated service locator for Veld DI container.\n");
        sb.append(" * Uses Initialization-on-demand holder idiom for lock-free singleton access.\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class Veld {\n\n");
        
        // Registry and lifecycle processor (no volatile needed for final fields)
        sb.append("    private static final VeldRegistry _registry = new VeldRegistry();\n");
        sb.append("    private static final LifecycleProcessor _lifecycle;\n");
        sb.append("    private static final ConditionalRegistry _conditionalRegistry;\n");
        sb.append("    private static final EventBus _eventBus = EventBus.getInstance();\n");
        sb.append("    private static String[] _activeProfiles = new String[0];\n\n");
        
        // Static initializer
        sb.append("    static {\n");
        sb.append("        Set<String> initialProfiles = computeActiveProfiles();\n");
        sb.append("        _activeProfiles = initialProfiles.toArray(new String[0]);\n");
        sb.append("        _lifecycle = new LifecycleProcessor();\n");
        sb.append("        _lifecycle.setEventBus(_eventBus);\n");
        sb.append("        _conditionalRegistry = new ConditionalRegistry(_registry, initialProfiles);\n");
        sb.append("    }\n\n");
        
        // computeActiveProfiles
        sb.append("    private static Set<String> computeActiveProfiles() {\n");
        sb.append("        String profiles = System.getProperty(\"veld.profiles.active\", \n");
        sb.append("            System.getenv().getOrDefault(\"VELD_PROFILES_ACTIVE\", \"\"));\n");
        sb.append("        if (profiles.isEmpty()) {\n");
        sb.append("            return Set.of();\n");
        sb.append("        }\n");
        sb.append("        return Set.of(profiles.split(\",\"));\n");
        sb.append("    }\n\n");
        
        // Generate Holder classes for singletons
        generateHolderClasses(sb);
        
        // get() methods for each component
        generateGetMethods(sb);
        
        // Generic get by class
        generateGetByClass(sb);
        
        // Additional generic methods (getOptional, getProvider, getAll, etc.)
        generateAdditionalGenericMethods(sb);
        
        // Registry accessor
        sb.append("    public static ComponentRegistry getRegistry() {\n");
        sb.append("        return _registry;\n");
        sb.append("    }\n\n");
        
        // EventBus accessor
        sb.append("    public static EventBus getEventBus() {\n");
        sb.append("        return _eventBus;\n");
        sb.append("    }\n\n");
        
        // LifecycleProcessor accessor
        sb.append("    public static LifecycleProcessor getLifecycleProcessor() {\n");
        sb.append("        return _lifecycle;\n");
        sb.append("    }\n\n");
        
        // Shutdown method
        sb.append("    public static void shutdown() {\n");
        sb.append("        _lifecycle.destroy();\n");
        sb.append("    }\n\n");

        // Inject dependencies into an instance
        sb.append("    /**\n");
        sb.append("     * Injects dependencies into an instance using field injection.\n");
        sb.append("     * Used by generated factories to inject dependencies into factory class instances.\n");
        sb.append("     *\n");
        sb.append("     * @param instance the instance to inject dependencies into\n");
        sb.append("     */\n");
        sb.append("    public static void inject(Object instance) {\n");
        sb.append("        if (instance == null) return;\n");
        sb.append("        // Use reflection to inject fields - this is only called for factory instances\n");
        sb.append("        // which are created by the generated factory classes themselves\n");
        sb.append("        Class<?> clazz = instance.getClass();\n");
        sb.append("        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {\n");
        sb.append("            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&\n");
        sb.append("                !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {\n");
        sb.append("                io.github.yasmramos.veld.annotation.Inject injectAnn = field.getAnnotation(io.github.yasmramos.veld.annotation.Inject.class);\n");
        sb.append("                io.github.yasmramos.veld.annotation.Value valueAnn = field.getAnnotation(io.github.yasmramos.veld.annotation.Value.class);\n");
        sb.append("                if (injectAnn != null) {\n");
        sb.append("                    try {\n");
        sb.append("                        field.setAccessible(true);\n");
        sb.append("                        Class<?> fieldType = field.getType();\n");
        sb.append("                        Object value = get(fieldType);\n");
        sb.append("                        field.set(instance, value);\n");
        sb.append("                    } catch (Exception e) {\n");
        sb.append("                        throw new VeldException(\"Failed to inject field: \" + field.getName(), e);\n");
        sb.append("                    }\n");
        sb.append("                } else if (valueAnn != null) {\n");
        sb.append("                    try {\n");
        sb.append("                        field.setAccessible(true);\n");
        sb.append("                        Object value = resolveValue(valueAnn.value());\n");
        sb.append("                        field.set(instance, value);\n");
        sb.append("                    } catch (Exception e) {\n");
        sb.append("                        throw new VeldException(\"Failed to inject @Value field: \" + field.getName(), e);\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        // resolveValue method
        sb.append("    /**\n");
        sb.append("     * Resolves a value expression using the ValueResolver.\n");
        sb.append("     *\n");
        sb.append("     * @param expression the value expression to resolve\n");
        sb.append("     * @return the resolved value as a String\n");
        sb.append("     */\n");
        sb.append("    public static String resolveValue(String expression) {\n");
        sb.append("        return ValueResolver.getInstance().resolve(expression);\n");
        sb.append("    }\n\n");

        // Profile management methods
        sb.append("    // === PROFILE MANAGEMENT ===\n\n");
        sb.append("    public static void setActiveProfiles(String... profiles) {\n");
        sb.append("        _activeProfiles = profiles != null ? profiles : new String[0];\n");
        sb.append("    }\n\n");
        sb.append("    public static String[] getActiveProfiles() {\n");
        sb.append("        return _activeProfiles.clone();\n");
        sb.append("    }\n\n");
        sb.append("    public static boolean isProfileActive(String profile) {\n");
        sb.append("        if (profile == null) return false;\n");
        sb.append("        for (String p : _activeProfiles) {\n");
        sb.append("            if (profile.equals(p)) return true;\n");
        sb.append("        }\n");
        sb.append("        return false;\n");
        sb.append("    }\n\n");
        
        // Sort factories by order (lower values have higher priority)
        sb.append("    private static java.util.List<ComponentFactory<?>> sortByOrder(java.util.List<ComponentFactory<?>> factories) {\n");
        sb.append("        return factories.stream()\n");
        sb.append("            .sorted(Comparator.comparingInt(ComponentFactory::getOrder))\n");
        sb.append("            .collect(Collectors.toList());\n");
        sb.append("    }\n\n");
        
        // Close class
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private void generateHolderClasses(StringBuilder sb) {
        sb.append("    // === HOLDER CLASSES FOR LOCK-FREE SINGLETON ACCESS ===\n\n");
        for (ComponentInfo comp : components) {
            if (comp.getScope() == ScopeType.SINGLETON && comp.canUseHolderPattern()) {
                // Simple singleton - use true holder pattern with direct instantiation
                String holderName = getHolderClassName(comp);
                String returnType = comp.getClassName();
                String simpleName = getSimpleName(comp);
                
                sb.append("    private static final class ").append(holderName).append(" {\n");
                sb.append("        static final ").append(returnType).append(" INSTANCE = new ").append(simpleName).append("();\n");
                sb.append("    }\n\n");
            }
        }
    }
    
    private void generateGetMethods(StringBuilder sb) {
        sb.append("    // === GETTER METHODS ===\n\n");
        
        // Track generated method names to avoid duplicates
        Set<String> generatedMethodNames = new HashSet<>();
        
        for (ComponentInfo comp : components) {
            String methodName = getGetterMethodName(comp);
            String returnType = comp.getClassName();
            
            // Handle duplicate method names by adding a suffix
            String uniqueMethodName = methodName;
            int suffix = 1;
            while (generatedMethodNames.contains(uniqueMethodName)) {
                uniqueMethodName = methodName + "_" + suffix++;
            }
            generatedMethodNames.add(uniqueMethodName);
            
            sb.append("    public static ").append(returnType).append(" ").append(uniqueMethodName).append("() {\n");
            
            if (comp.getScope() == ScopeType.SINGLETON) {
                if (comp.canUseHolderPattern()) {
                    // Simple singleton - use holder pattern (lock-free, direct instantiation)
                    String holderName = getHolderClassName(comp);
                    sb.append("        return ").append(holderName).append(".INSTANCE;\n");
                } else {
                    // Complex singleton - use factory (supports DI, lifecycle, conditions, AOP)
                    sb.append("        return (").append(returnType).append(") _registry.getFactory(").append(returnType).append(".class).create();\n");
                }
            } else {
                // Prototype - always create new via factory
                sb.append("        return (").append(returnType).append(") _registry.getFactory(").append(returnType).append(".class).create();\n");
            }
            
            sb.append("    }\n\n");
        }
    }
    
    private void generateGetByClass(StringBuilder sb) {
        sb.append("    // === GENERIC GET BY CLASS ===\n\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> T get(Class<T> type) {\n");
        
        // Generate if-else chain for each component type
        for (ComponentInfo comp : components) {
            sb.append("        if (type == ").append(comp.getClassName()).append(".class) {\n");
            
            if (comp.getScope() == ScopeType.SINGLETON && comp.canUseHolderPattern()) {
                // Simple singleton - use holder pattern
                String holderName = getHolderClassName(comp);
                sb.append("            return (T) ").append(holderName).append(".INSTANCE;\n");
            } else {
                // Complex singleton or prototype - use factory
                sb.append("            return (T) _registry.getFactory(type).create();\n");
            }
            
            sb.append("        }\n");
        }
        
        // Also check interfaces
        for (ComponentInfo comp : components) {
            for (String iface : comp.getImplementedInterfaces()) {
                sb.append("        if (type == ").append(iface).append(".class) {\n");
                
                if (comp.getScope() == ScopeType.SINGLETON && comp.canUseHolderPattern()) {
                    String holderName = getHolderClassName(comp);
                    sb.append("            return (T) ").append(holderName).append(".INSTANCE;\n");
                } else {
                    sb.append("            return (T) _registry.getFactory(type).create();\n");
                }
                
                sb.append("        }\n");
            }
        }
        
        sb.append("        throw new VeldException(\"No component registered for type: \" + type.getName());\n");
        sb.append("    }\n\n");
    }
    
    private void generateAdditionalGenericMethods(StringBuilder sb) {
        sb.append("    // === ADDITIONAL GENERIC METHODS ===\n\n");
        
        // get(Class<T> type, String name)
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> T get(Class<T> type, String name) {\n");
        sb.append("        ComponentFactory<?> factory = _registry.getFactory(name);\n");
        sb.append("        if (factory != null) {\n");
        sb.append("            return (T) factory.create();\n");
        sb.append("        }\n");
        sb.append("        throw new VeldException(\"No component registered with name: \" + name);\n");
        sb.append("    }\n\n");
        
        // getAll(Class<T> type)
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> List<T> getAll(Class<T> type) {\n");
        sb.append("        List<ComponentFactory<? extends T>> factories = _registry.getFactoriesForType(type);\n");
        sb.append("        List<T> instances = new ArrayList<>();\n");
        sb.append("        for (ComponentFactory<? extends T> factory : factories) {\n");
        sb.append("            instances.add((T) factory.create());\n");
        sb.append("        }\n");
        sb.append("        return instances;\n");
        sb.append("    }\n\n");
        
        // getProvider(Class<T> type)
        sb.append("    public static <T> Provider<T> getProvider(Class<T> type) {\n");
        sb.append("        ComponentFactory<T> factory = _registry.getFactory(type);\n");
        sb.append("        if (factory != null) {\n");
        sb.append("            return () -> (T) factory.create();\n");
        sb.append("        }\n");
        sb.append("        return () -> { throw new VeldException(\"No component registered for type: \" + type.getName()); };\n");
        sb.append("    }\n\n");
        
        // getOptional(Class<T> type)
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> Optional<T> getOptional(Class<T> type) {\n");
        sb.append("        ComponentFactory<T> factory = _registry.getFactory(type);\n");
        sb.append("        if (factory != null) {\n");
        sb.append("            return Optional.of((T) factory.create());\n");
        sb.append("        }\n");
        sb.append("        return Optional.empty();\n");
        sb.append("    }\n\n");
        
        // contains(Class<?> type)
        sb.append("    public static boolean contains(Class<?> type) {\n");
        sb.append("        return _registry.getFactory(type) != null;\n");
        sb.append("    }\n\n");
        
        // componentCount()
        sb.append("    public static int componentCount() {\n");
        sb.append("        return _registry.getComponentCount();\n");
        sb.append("    }\n\n");
    }
    
    private String getHolderClassName(ComponentInfo comp) {
        return "Holder_" + getSimpleName(comp);
    }
    
    private String getGetterMethodName(ComponentInfo comp) {
        String simpleName = getSimpleName(comp);
        return decapitalize(simpleName);
    }
    
    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    private String getSimpleName(ComponentInfo comp) {
        String className = comp.getClassName();
        // For inner classes (Outer$Inner), get just the simple name (Inner)
        int lastDollar = className.lastIndexOf('$');
        if (lastDollar >= 0) {
            return className.substring(lastDollar + 1);
        }
        // For regular classes (package.Class), get the class name
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    /**
     * Gets the getter call for a dependency type.
     * For unresolved interface dependencies, returns null (to be resolved at runtime).
     * 
     * @param typeName the dependency type name
     * @param component the component being created (to check its unresolved deps)
     * @return the getter call code
     */
    private String getGetterCallForType(String typeName, ComponentInfo component) {
        // Check if this is an unresolved interface dependency
        if (component.hasUnresolvedInterfaceDependencies() && 
            component.getUnresolvedInterfaceDependencies().contains(typeName)) {
            // For unresolved interface dependencies, return null
            // These will be resolved at runtime (e.g., via mocks in tests)
            return "null /* unresolved interface: " + typeName + " */";
        }
        
        // Find the component that matches this type and get its unique method name
        String methodName = getGetterMethodNameForType(typeName);
        if (methodName != null) {
            return methodName;
        }
        
        return "get(" + typeName + ".class)";
    }
    
    /**
     * Gets the unique getter method name for a given type.
     * Handles duplicate names by adding suffixes.
     * 
     * @param typeName the dependency type name
     * @return the unique getter method name, or null if not found
     */
    private String getGetterMethodNameForType(String typeName) {
        // First pass: count occurrences to determine suffixes needed
        int occurrenceCount = 0;
        ComponentInfo matchedComponent = null;
        for (ComponentInfo comp : components) {
            if (comp.getClassName().equals(typeName)) {
                occurrenceCount++;
                matchedComponent = comp;
            }
            for (String iface : comp.getImplementedInterfaces()) {
                if (iface.equals(typeName)) {
                    occurrenceCount++;
                    matchedComponent = comp;
                }
            }
        }
        
        if (occurrenceCount == 0) {
            return null;
        }
        
        // Generate unique method name
        String baseMethodName = getGetterMethodName(matchedComponent);
        
        if (occurrenceCount == 1) {
            return baseMethodName + "()";
        }
        
        // Multiple components with same name - need to find the correct one
        // Use class name hash to make unique suffix
        int hash = typeName.hashCode() & 0xFFFF;
        return baseMethodName + "_" + hash + "()";
    }
}
