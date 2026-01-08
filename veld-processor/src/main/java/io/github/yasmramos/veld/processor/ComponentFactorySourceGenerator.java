package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.ScopeType;
import java.util.List;

/**
 * Generates ComponentFactory source code instead of bytecode.
 */
public final class ComponentFactorySourceGenerator {
    
    private final ComponentInfo component;
    private final int componentIndex;
    
    public ComponentFactorySourceGenerator(ComponentInfo component) {
        this(component, -1);
    }
    
    public ComponentFactorySourceGenerator(ComponentInfo component, int componentIndex) {
        this.component = component;
        this.componentIndex = componentIndex;
    }
    
    public String getFactoryClassName() {
        return component.getFactoryClassName();
    }
    
    public String generate() {
        StringBuilder sb = new StringBuilder();
        
        String packageName = getPackageName(component.getClassName());
        String simpleName = getSimpleName(component.getClassName());
        String factorySimpleName = simpleName + "$$VeldFactory";
        
        // Package declaration
        sb.append("package ").append(packageName).append(";\n\n");
        
        // Imports
        sb.append("import io.github.yasmramos.veld.Veld;\n");
        sb.append("import io.github.yasmramos.veld.runtime.ComponentFactory;\n");
        sb.append("import io.github.yasmramos.veld.annotation.ScopeType;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Arrays;\n\n");
        
        // Class declaration
        sb.append("/**\n");
        sb.append(" * Generated factory for ").append(component.getClassName()).append(".\n");
        sb.append(" */\n");
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
        sb.append("public final class ").append(factorySimpleName);
        sb.append(" implements ComponentFactory<").append(component.getClassName()).append("> {\n\n");
        
        // Constructor
        sb.append("    public ").append(factorySimpleName).append("() {}\n\n");
        
        // create() method
        generateCreateMethod(sb);
        
        // getComponentType()
        sb.append("    @Override\n");
        sb.append("    public Class<").append(component.getClassName()).append("> getComponentType() {\n");
        sb.append("        return ").append(component.getClassName()).append(".class;\n");
        sb.append("    }\n\n");
        
        // getComponentName()
        sb.append("    @Override\n");
        sb.append("    public String getComponentName() {\n");
        sb.append("        return \"").append(component.getComponentName()).append("\";\n");
        sb.append("    }\n\n");
        
        // getScope()
        sb.append("    @Override\n");
        sb.append("    public ScopeType getScope() {\n");
        String scopeName = component.getScope() == ScopeType.SINGLETON ? "SINGLETON" : "PROTOTYPE";
        sb.append("        return ScopeType.").append(scopeName).append(";\n");
        sb.append("    }\n\n");
        
        // getScopeId() - supports custom scopes
        sb.append("    @Override\n");
        sb.append("    public String getScopeId() {\n");
        sb.append("        return \"").append(component.getScopeId()).append("\";\n");
        sb.append("    }\n\n");
        
        // isLazy()
        sb.append("    @Override\n");
        sb.append("    public boolean isLazy() {\n");
        sb.append("        return ").append(component.isLazy()).append(";\n");
        sb.append("    }\n\n");
        
        // invokePostConstruct()
        sb.append("    @Override\n");
        sb.append("    public void invokePostConstruct(").append(component.getClassName()).append(" instance) {\n");
        if (component.hasPostConstruct()) {
            sb.append("        instance.").append(component.getPostConstructMethod()).append("();\n");
        }
        sb.append("    }\n\n");
        
        // invokePreDestroy()
        sb.append("    @Override\n");
        sb.append("    public void invokePreDestroy(").append(component.getClassName()).append(" instance) {\n");
        if (component.hasPreDestroy()) {
            sb.append("        instance.").append(component.getPreDestroyMethod()).append("();\n");
        }
        sb.append("    }\n\n");
        
        // getIndex() method - for ultra-fast array-based lookups
        sb.append("    @Override\n");
        sb.append("    public int getIndex() {\n");
        sb.append("        return ").append(componentIndex).append(";\n");
        sb.append("    }\n\n");
        
        // getDependencyTypes() - for dependency graph visualization
        generateGetDependencyTypes(sb);
        
        // Close class
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private void generateCreateMethod(StringBuilder sb) {
        String componentType = component.getClassName();
        
        sb.append("    @Override\n");
        sb.append("    public ").append(componentType).append(" create() {\n");
        
        // Create instance with constructor dependencies
        sb.append("        ").append(componentType).append(" instance = new ").append(componentType).append("(");
        
        InjectionPoint ctor = component.getConstructorInjection();
        if (ctor != null && !ctor.getDependencies().isEmpty()) {
            boolean first = true;
            for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("Veld.get(").append(dep.getActualTypeName()).append(".class)");
            }
        }
        sb.append(");\n");
        
        // Field injections - use synthetic setters for private fields
        for (InjectionPoint field : component.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                if (dep.isValueInjection()) {
                    // @Value injection - skip for now
                    continue;
                }
                
                String fieldName = field.getName();
                if (field.getVisibility() == InjectionPoint.Visibility.PRIVATE) {
                    // Use synthetic setter generated by weaver
                    sb.append("        instance.__di_set_").append(fieldName).append("(");
                } else {
                    // Use normal setter
                    sb.append("        instance.set").append(capitalize(fieldName)).append("(");
                }
                sb.append("Veld.get(").append(dep.getActualTypeName()).append(".class));\n");
            }
        }
        
        // Method injections
        for (InjectionPoint method : component.getMethodInjections()) {
            sb.append("        instance.").append(method.getName()).append("(");
            boolean first = true;
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("Veld.get(").append(dep.getActualTypeName()).append(".class)");
            }
            sb.append(");\n");
        }
        
        sb.append("        return instance;\n");
        sb.append("    }\n\n");
    }
    
    private void generateGetDependencyTypes(StringBuilder sb) {
        sb.append("    @Override\n");
        sb.append("    public List<String> getDependencyTypes() {\n");
        
        // Collect all dependencies
        sb.append("        return Arrays.asList(\n");
        
        boolean first = true;
        
        // Constructor dependencies
        InjectionPoint ctor = component.getConstructorInjection();
        if (ctor != null) {
            for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
                if (!first) sb.append(",\n            ");
                first = false;
                sb.append("\"").append(dep.getActualTypeName()).append("\"");
            }
        }
        
        // Field dependencies
        for (InjectionPoint field : component.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                if (dep.isValueInjection()) {
                    continue;
                }
                if (!first) sb.append(",\n            ");
                first = false;
                sb.append("\"").append(dep.getActualTypeName()).append("\"");
            }
        }
        
        // Method dependencies
        for (InjectionPoint method : component.getMethodInjections()) {
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                if (!first) sb.append(",\n            ");
                first = false;
                sb.append("\"").append(dep.getActualTypeName()).append("\"");
            }
        }
        
        sb.append("\n        );\n");
        sb.append("    }\n\n");
    }
    
    private String getPackageName(String className) {
        // For inner classes (Outer$Inner), find the package by looking for $ first
        // Then find the last dot before the $
        int dollarSign = className.lastIndexOf('$');
        if (dollarSign > 0) {
            // It's an inner class, find the last dot before the $
            int lastDot = className.lastIndexOf('.', dollarSign - 1);
            return lastDot >= 0 ? className.substring(0, lastDot) : "";
        }
        // Regular class - find the last dot
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(0, lastDot) : "";
    }
    
    private String getSimpleName(String className) {
        // For inner classes (Outer$Inner), get just the inner class name
        int lastDollar = className.lastIndexOf('$');
        if (lastDollar >= 0) {
            return className.substring(lastDollar + 1);
        }
        // For regular classes, get the class name after the last dot
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
