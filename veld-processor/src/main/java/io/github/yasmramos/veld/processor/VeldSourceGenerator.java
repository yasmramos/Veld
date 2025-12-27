package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.Scope;
import java.util.List;

/**
 * Generates Veld.java source code instead of bytecode.
 * The generated class is the main service locator for dependency injection.
 */
public final class VeldSourceGenerator {
    
    private final List<ComponentInfo> components;
    
    public VeldSourceGenerator(List<ComponentInfo> components) {
        this.components = components;
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
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.concurrent.ConcurrentHashMap;\n");
        sb.append("import java.util.Set;\n");
        sb.append("import java.util.function.Supplier;\n\n");
        
        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated service locator for Veld DI container.\n");
        sb.append(" * This class is auto-generated - do not modify.\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class Veld {\n\n");
        
        // Static fields for singletons
        for (ComponentInfo comp : components) {
            if (comp.getScope() == Scope.SINGLETON) {
                String fieldName = getFieldName(comp);
                sb.append("    private static volatile ").append(comp.getClassName())
                  .append(" ").append(fieldName).append(";\n");
            }
        }
        sb.append("\n");
        
        // Registry and lifecycle processor
        sb.append("    private static final VeldRegistry _registry = new VeldRegistry();\n");
        sb.append("    private static final LifecycleProcessor _lifecycle;\n");
        sb.append("    private static final ConditionalRegistry _conditionalRegistry;\n");
        sb.append("    private static final EventBus _eventBus = EventBus.getInstance();\n");
        sb.append("    private static final Map<Class<?>, Object> _singletons = new ConcurrentHashMap<>();\n");
        sb.append("    private static volatile boolean _initialized = false;\n\n");
        
        // Static initializer
        sb.append("    static {\n");
        sb.append("        _lifecycle = new LifecycleProcessor();\n");
        sb.append("        _lifecycle.setEventBus(_eventBus);\n");
        sb.append("        _conditionalRegistry = new ConditionalRegistry(_registry, getActiveProfiles());\n");
        sb.append("        initialize();\n");
        sb.append("    }\n\n");
        
        // Initialize method
        generateInitializeMethod(sb);
        
        // getActiveProfiles
        sb.append("    private static Set<String> getActiveProfiles() {\n");
        sb.append("        String profiles = System.getProperty(\"veld.profiles.active\", \n");
        sb.append("            System.getenv().getOrDefault(\"VELD_PROFILES_ACTIVE\", \"\"));\n");
        sb.append("        if (profiles.isEmpty()) {\n");
        sb.append("            return Set.of();\n");
        sb.append("        }\n");
        sb.append("        return Set.of(profiles.split(\",\"));\n");
        sb.append("    }\n\n");
        
        // get() methods for each component
        generateGetMethods(sb);
        
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
        
        // Shutdown method
        sb.append("    public static void shutdown() {\n");
        sb.append("        _lifecycle.destroy();\n");
        sb.append("        _singletons.clear();\n");
        sb.append("        _initialized = false;\n");
        sb.append("    }\n\n");
        
        // Close class
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private void generateInitializeMethod(StringBuilder sb) {
        sb.append("    private static void initialize() {\n");
        sb.append("        if (_initialized) return;\n");
        sb.append("        synchronized (Veld.class) {\n");
        sb.append("            if (_initialized) return;\n");
        
        // Create singleton instances
        for (ComponentInfo comp : components) {
            if (comp.getScope() == Scope.SINGLETON && !comp.isLazy()) {
                String fieldName = getFieldName(comp);
                String getterName = getGetterMethodName(comp);
                sb.append("            ").append(fieldName).append(" = ").append(getterName).append("();\n");
                sb.append("            _lifecycle.registerBean(\"").append(comp.getComponentName())
                  .append("\", ").append(fieldName).append(");\n");
            }
        }
        
        sb.append("            _initialized = true;\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetMethods(StringBuilder sb) {
        for (ComponentInfo comp : components) {
            String methodName = getGetterMethodName(comp);
            String returnType = comp.getClassName();
            String fieldName = getFieldName(comp);
            
            sb.append("    public static ").append(returnType).append(" ").append(methodName).append("() {\n");
            
            if (comp.getScope() == Scope.SINGLETON) {
                // Singleton - double-checked locking
                sb.append("        ").append(returnType).append(" result = ").append(fieldName).append(";\n");
                sb.append("        if (result == null) {\n");
                sb.append("            synchronized (Veld.class) {\n");
                sb.append("                result = ").append(fieldName).append(";\n");
                sb.append("                if (result == null) {\n");
                sb.append("                    result = createInstance_").append(getSimpleName(comp)).append("();\n");
                sb.append("                    ").append(fieldName).append(" = result;\n");
                sb.append("                }\n");
                sb.append("            }\n");
                sb.append("        }\n");
                sb.append("        return result;\n");
            } else {
                // Prototype - always create new
                sb.append("        return createInstance_").append(getSimpleName(comp)).append("();\n");
            }
            
            sb.append("    }\n\n");
            
            // Generate createInstance method
            generateCreateInstanceMethod(sb, comp);
        }
    }
    
    private void generateCreateInstanceMethod(StringBuilder sb, ComponentInfo comp) {
        String simpleName = getSimpleName(comp);
        String returnType = comp.getClassName();
        
        sb.append("    private static ").append(returnType).append(" createInstance_").append(simpleName).append("() {\n");
        
        // Create instance with constructor dependencies
        sb.append("        ").append(returnType).append(" instance = new ").append(returnType).append("(");
        
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
                    // @Value injection - skip for now, handle at runtime
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
    
    private String getFieldName(ComponentInfo comp) {
        return "_veld" + getSimpleName(comp);
    }
    
    private String getGetterMethodName(ComponentInfo comp) {
        // Use decapitalized simple name (e.g., VeldSimpleService -> veldSimpleService)
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
        // Find component by type
        for (ComponentInfo comp : components) {
            if (comp.getClassName().equals(typeName)) {
                return getGetterMethodName(comp) + "()";
            }
            // Check interfaces
            for (String iface : comp.getImplementedInterfaces()) {
                if (iface.equals(typeName)) {
                    return getGetterMethodName(comp) + "()";
                }
            }
        }
        // Fallback to get(Class)
        return "get(" + typeName + ".class)";
    }
}
