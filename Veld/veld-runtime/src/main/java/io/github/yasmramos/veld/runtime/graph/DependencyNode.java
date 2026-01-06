package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.runtime.LegacyScope;

import java.util.*;

/**
 * Represents a node in the dependency graph.
 */
@SuppressWarnings("deprecation")
public final class DependencyNode {
    private final String className;
    private final String simpleName;
    private final String componentName;
    private final LegacyScope scope;
    private boolean isPrimary;
    private final Set<String> profiles = new HashSet<>();
    private final List<String> constructorDependencies = new ArrayList<>();
    private final List<String> fieldDependencies = new ArrayList<>();
    private final List<String> methodDependencies = new ArrayList<>();
    private final List<String> interfaces = new ArrayList<>();
    
    public DependencyNode(String className, String componentName, LegacyScope scope) {
        this.className = className;
        this.simpleName = extractSimpleName(className);
        this.componentName = componentName;
        this.scope = scope;
        this.isPrimary = false;
    }
    
    private static String extractSimpleName(String className) {
        int lastDollar = className.lastIndexOf('$');
        if (lastDollar >= 0) {
            return className.substring(lastDollar + 1);
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    // Getters
    public String getClassName() { return className; }
    public String getSimpleName() { return simpleName; }
    public String getComponentName() { return componentName; }
    public LegacyScope getScope() { return scope; }
    public boolean isPrimary() { return isPrimary; }
    public Set<String> getProfiles() { return profiles; }
    public List<String> getConstructorDependencies() { return constructorDependencies; }
    public List<String> getFieldDependencies() { return fieldDependencies; }
    public List<String> getMethodDependencies() { return methodDependencies; }
    public List<String> getInterfaces() { return interfaces; }
    
    // Setters
    public void setPrimary(boolean isPrimary) { this.isPrimary = isPrimary; }
    public void addProfile(String profile) { this.profiles.add(profile); }
    public void addConstructorDependency(String dep) { this.constructorDependencies.add(dep); }
    public void addFieldDependency(String dep) { this.fieldDependencies.add(dep); }
    public void addMethodDependency(String dep) { this.methodDependencies.add(dep); }
    public void addInterface(String iface) { this.interfaces.add(iface); }
    
    public List<String> getAllDependencies() {
        List<String> all = new ArrayList<>();
        all.addAll(constructorDependencies);
        all.addAll(fieldDependencies);
        all.addAll(methodDependencies);
        return all;
    }
}
