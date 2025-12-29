package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.Scope;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    
    public String generate() {
        StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package io.github.yasmramos.veld;\n\n");
        
        // Imports
        sb.append("import io.github.yasmramos.veld.generated.VeldRegistry;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentRegistry;\n");
        sb.append("import io.github.yasmramos.veld.runtime.Scope;\n");
        sb.append("import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ConditionalRegistry;\n");
        sb.append("import io.github.yasmramos.veld.runtime.event.EventBus;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentFactory;\n");
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.concurrent.ConcurrentHashMap;\n");
        sb.append("import java.util.Set;\n");
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
        sb.append("        Set<String> initialProfiles = getActiveProfiles();\n");
        sb.append("        _activeProfiles = initialProfiles.toArray(new String[0]);\n");
        sb.append("        _lifecycle = new LifecycleProcessor();\n");
        sb.append("        _lifecycle.setEventBus(_eventBus);\n");
        sb.append("        _conditionalRegistry = new ConditionalRegistry(_registry, initialProfiles);\n");
        sb.append("    }\n\n");
        
        // getActiveProfiles
        sb.append("    private static Set<String> getActiveProfiles() {\n");
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
        
        // createInstance methods
        generateCreateInstanceMethods(sb);
        
        // Generic get by class
        generateGetByClass(sb);
        
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
            if (comp.getScope() == Scope.SINGLETON) {
                String holderName = getHolderClassName(comp);
                String returnType = comp.getClassName();
                String simpleName = getSimpleName(comp);
                
                sb.append("    private static final class ").append(holderName).append(" {\n");
                sb.append("        static final ").append(returnType).append(" INSTANCE = createInstance_").append(simpleName).append("();\n");
                sb.append("    }\n\n");
            }
        }
    }
    
    private void generateGetMethods(StringBuilder sb) {
        sb.append("    // === GETTER METHODS ===\n\n");
        for (ComponentInfo comp : components) {
            String methodName = getGetterMethodName(comp);
            String returnType = comp.getClassName();
            
            sb.append("    public static ").append(returnType).append(" ").append(methodName).append("() {\n");
            
            if (comp.getScope() == Scope.SINGLETON) {
                // Singleton - use holder pattern (lock-free)
                String holderName = getHolderClassName(comp);
                sb.append("        return ").append(holderName).append(".INSTANCE;\n");
            } else {
                // Prototype - always create new
                sb.append("        return createInstance_").append(getSimpleName(comp)).append("();\n");
            }
            
            sb.append("    }\n\n");
        }
    }
    
    private void generateCreateInstanceMethods(StringBuilder sb) {
        sb.append("    // === INSTANCE CREATION METHODS ===\n\n");
        for (ComponentInfo comp : components) {
            generateCreateInstanceMethod(sb, comp);
        }
    }
    
    private void generateCreateInstanceMethod(StringBuilder sb, ComponentInfo comp) {
        String simpleName = getSimpleName(comp);
        String returnType = comp.getClassName();
        
        // Use AOP wrapper class if available
        String instantiationType = aopClassMap.getOrDefault(comp.getClassName(), comp.getClassName());
        
        sb.append("    private static ").append(returnType).append(" createInstance_").append(simpleName).append("() {\n");
        
        // Create instance with constructor dependencies (using AOP class if present)
        sb.append("        ").append(returnType).append(" instance = new ").append(instantiationType).append("(");
        
        InjectionPoint ctor = comp.getConstructorInjection();
        if (ctor != null && !ctor.getDependencies().isEmpty()) {
            boolean first = true;
            for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(getGetterCallForType(dep.getActualTypeName()));
            }
        }
        sb.append(");\n");
        
        // Field injections
        for (InjectionPoint field : comp.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                if (dep.isValueInjection()) {
                    continue;
                }
                String setterName = "set" + capitalize(field.getName());
                sb.append("        instance.").append(setterName).append("(")
                  .append(getGetterCallForType(dep.getActualTypeName())).append(");\n");
            }
        }
        
        // Method injections
        for (InjectionPoint method : comp.getMethodInjections()) {
            sb.append("        instance.").append(method.getName()).append("(");
            boolean first = true;
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(getGetterCallForType(dep.getActualTypeName()));
            }
            sb.append(");\n");
        }
        
        // PostConstruct
        if (comp.hasPostConstruct()) {
            sb.append("        instance.").append(comp.getPostConstructMethod()).append("();\n");
        }
        
        sb.append("        return instance;\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetByClass(StringBuilder sb) {
        sb.append("    // === GENERIC GET BY CLASS ===\n\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> T get(Class<T> type) {\n");
        
        // Generate if-else chain for each component type
        for (ComponentInfo comp : components) {
            sb.append("        if (type == ").append(comp.getClassName()).append(".class) {\n");
            sb.append("            return (T) ").append(getGetterMethodName(comp)).append("();\n");
            sb.append("        }\n");
        }
        
        // Also check interfaces
        for (ComponentInfo comp : components) {
            for (String iface : comp.getImplementedInterfaces()) {
                sb.append("        if (type == ").append(iface).append(".class) {\n");
                sb.append("            return (T) ").append(getGetterMethodName(comp)).append("();\n");
                sb.append("        }\n");
            }
        }
        
        sb.append("        throw new VeldException(\"No component registered for type: \" + type.getName());\n");
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
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    private String getGetterCallForType(String typeName) {
        for (ComponentInfo comp : components) {
            if (comp.getClassName().equals(typeName)) {
                return getGetterMethodName(comp) + "()";
            }
            for (String iface : comp.getImplementedInterfaces()) {
                if (iface.equals(typeName)) {
                    return getGetterMethodName(comp) + "()";
                }
            }
        }
        return "get(" + typeName + ".class)";
    }
}
