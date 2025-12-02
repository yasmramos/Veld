package com.veld.processor;

import com.veld.runtime.Scope;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all metadata about a component discovered during annotation processing.
 * This information is used by ASM generators to create bytecode.
 */
public final class ComponentInfo {
    
    private final String className;              // Fully qualified: com.example.MyService
    private final String internalName;           // ASM internal: com/example/MyService
    private final String componentName;          // @Component value or simple class name
    private final Scope scope;                   // SINGLETON or PROTOTYPE
    
    private InjectionPoint constructorInjection; // Constructor with @Inject (or default)
    private final List<InjectionPoint> fieldInjections = new ArrayList<>();
    private final List<InjectionPoint> methodInjections = new ArrayList<>();
    
    private String postConstructMethod;          // Method name with @PostConstruct
    private String postConstructDescriptor;
    private String preDestroyMethod;             // Method name with @PreDestroy
    private String preDestroyDescriptor;
    
    public ComponentInfo(String className, String componentName, Scope scope) {
        this.className = className;
        this.internalName = className.replace('.', '/');
        this.componentName = componentName;
        this.scope = scope;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getInternalName() {
        return internalName;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public Scope getScope() {
        return scope;
    }
    
    public String getFactoryClassName() {
        return className + "$$VeldFactory";
    }
    
    public String getFactoryInternalName() {
        return internalName + "$$VeldFactory";
    }
    
    public InjectionPoint getConstructorInjection() {
        return constructorInjection;
    }
    
    public void setConstructorInjection(InjectionPoint constructorInjection) {
        this.constructorInjection = constructorInjection;
    }
    
    public List<InjectionPoint> getFieldInjections() {
        return fieldInjections;
    }
    
    public void addFieldInjection(InjectionPoint injection) {
        this.fieldInjections.add(injection);
    }
    
    public List<InjectionPoint> getMethodInjections() {
        return methodInjections;
    }
    
    public void addMethodInjection(InjectionPoint injection) {
        this.methodInjections.add(injection);
    }
    
    public String getPostConstructMethod() {
        return postConstructMethod;
    }
    
    public String getPostConstructDescriptor() {
        return postConstructDescriptor;
    }
    
    public void setPostConstruct(String methodName, String descriptor) {
        this.postConstructMethod = methodName;
        this.postConstructDescriptor = descriptor;
    }
    
    public boolean hasPostConstruct() {
        return postConstructMethod != null;
    }
    
    public String getPreDestroyMethod() {
        return preDestroyMethod;
    }
    
    public String getPreDestroyDescriptor() {
        return preDestroyDescriptor;
    }
    
    public void setPreDestroy(String methodName, String descriptor) {
        this.preDestroyMethod = methodName;
        this.preDestroyDescriptor = descriptor;
    }
    
    public boolean hasPreDestroy() {
        return preDestroyMethod != null;
    }
    
    public boolean hasFieldInjections() {
        return !fieldInjections.isEmpty();
    }
    
    public boolean hasMethodInjections() {
        return !methodInjections.isEmpty();
    }
}
