package io.github.yasmramos.veld.processor;

import java.util.List;

/**
 * Generates the Veld.java source file with typed getters and AOP support.
 */
public final class VeldClassGenerator {
    
    private final String packageName;
    private final List<ComponentInfo> components;
    
    public VeldClassGenerator(String packageName, List<ComponentInfo> components) {
        this.packageName = packageName;
        this.components = components;
    }
    
    public String getClassName() {
        return packageName + ".Veld";
    }
    
    public String generate() {
        StringBuilder sb = new StringBuilder();
        
        // Package and imports
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.List;\n");
        sb.append("import io.github.yasmramos.veld.runtime.aop.*;\n\n");
        
        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated Veld container - ultra-fast DI with compile-time optimization.\n");
        sb.append(" * All singletons initialized in static block (thread-safe, zero runtime overhead).\n");
        sb.append(" */\n");
        sb.append("public final class Veld {\n\n");
        
        // Singleton fields
        sb.append("    // === SINGLETON FIELDS ===\n");
        for (ComponentInfo comp : components) {
            sb.append("    private static final ").append(comp.getClassName())
              .append(" _").append(fieldName(comp)).append(";\n");
        }
        sb.append("\n");
        
        // Lookup arrays
        sb.append("    // === LOOKUP ARRAYS ===\n");
        sb.append("    private static final Class<?>[] _types;\n");
        sb.append("    private static final Object[] _instances;\n\n");
        
        // Static initializer
        sb.append("    // === STATIC INITIALIZER ===\n");
        sb.append("    static {\n");
        for (ComponentInfo comp : components) {
            sb.append("        _").append(fieldName(comp)).append(" = new ")
              .append(comp.getClassName()).append("(");
            // Add constructor args based on dependencies
            List<String> deps = comp.getDependencyTypes();
            for (int i = 0; i < deps.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("_").append(decapitalize(simpleName(deps.get(i))));
            }
            sb.append(");\n");
        }
        sb.append("\n");
        
        // Types array
        sb.append("        _types = new Class<?>[] {\n");
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            sb.append("            ").append(comp.getClassName()).append(".class");
            if (i < components.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("        };\n\n");
        
        // Instances array
        sb.append("        _instances = new Object[] {\n");
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            sb.append("            _").append(fieldName(comp));
            if (i < components.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("        };\n");
        sb.append("    }\n\n");
        
        // Private constructor
        sb.append("    private Veld() {}\n\n");
        
        // Typed getters
        sb.append("    // === TYPED GETTERS ===\n");
        for (ComponentInfo comp : components) {
            String field = fieldName(comp);
            if (comp.hasAopInterceptors()) {
                // With AOP
                sb.append("    private static final MethodInterceptor[] _").append(field).append("Interceptors = {\n");
                for (String interceptor : comp.getAopInterceptors()) {
                    sb.append("        ").append(interceptor).append(".INSTANCE\n");
                }
                sb.append("    };\n\n");
                
                sb.append("    @SuppressWarnings(\"unchecked\")\n");
                sb.append("    public static ").append(comp.getClassName()).append(" ").append(field).append("() {\n");
                sb.append("        if (_").append(field).append("Interceptors.length == 0) {\n");
                sb.append("            return _").append(field).append(";\n");
                sb.append("        }\n");
                sb.append("        try {\n");
                sb.append("            MethodInvocation invocation = new MethodInvocation(\n");
                sb.append("                \"").append(field).append("\",\n");
                sb.append("                ").append(comp.getClassName()).append(".class,\n");
                sb.append("                () -> _").append(field).append("\n");
                sb.append("            );\n");
                sb.append("            return (").append(comp.getClassName()).append(") _").append(field).append("Interceptors[0].invoke(invocation);\n");
                sb.append("        } catch (Throwable t) {\n");
                sb.append("            throw new RuntimeException(\"AOP interceptor failed\", t);\n");
                sb.append("        }\n");
                sb.append("    }\n\n");
            } else {
                // Direct access
                sb.append("    public static ").append(comp.getClassName()).append(" ").append(field).append("() {\n");
                sb.append("        return _").append(field).append(";\n");
                sb.append("    }\n\n");
            }
        }
        
        // get(Class) method
        sb.append("    // === CONTAINER API ===\n");
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> T get(Class<T> type) {\n");
        for (ComponentInfo comp : components) {
            sb.append("        if (type == ").append(comp.getClassName()).append(".class) {\n");
            sb.append("            return (T) _").append(fieldName(comp)).append(";\n");
            sb.append("        }\n");
        }
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        
        // getAll method
        sb.append("    @SuppressWarnings(\"unchecked\")\n");
        sb.append("    public static <T> List<T> getAll(Class<T> type) {\n");
        sb.append("        List<T> result = new ArrayList<>();\n");
        sb.append("        for (int i = 0; i < _types.length; i++) {\n");
        sb.append("            if (type.isAssignableFrom(_types[i])) {\n");
        sb.append("                result.add((T) _instances[i]);\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return result;\n");
        sb.append("    }\n\n");
        
        // contains method
        sb.append("    public static boolean contains(Class<?> type) {\n");
        sb.append("        return get(type) != null;\n");
        sb.append("    }\n\n");
        
        // componentCount method
        sb.append("    public static int componentCount() {\n");
        sb.append("        return ").append(components.size()).append(";\n");
        sb.append("    }\n");
        
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private String fieldName(ComponentInfo comp) {
        return decapitalize(simpleName(comp.getClassName()));
    }
    
    private String simpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }
    
    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
