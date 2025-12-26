package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds all metadata about a component discovered during annotation processing.
 * This information is used by ASM generators to create bytecode.
 */
public final class ComponentInfo {
    
    private final String className;              // Fully qualified: com.example.MyService
    private final String internalName;           // ASM internal: com/example/MyService
    private final String componentName;          // @Component value or simple class name
    private final Scope scope;                   // SINGLETON or PROTOTYPE
    private final boolean lazy;                  // @Lazy - deferred initialization
    
    private InjectionPoint constructorInjection; // Constructor with @Inject (or default)
    private final List<InjectionPoint> fieldInjections = new ArrayList<>();
    private final List<InjectionPoint> methodInjections = new ArrayList<>();
    
    // Interfaces implemented by this component (for interface-based injection)
    private final List<String> implementedInterfaces = new ArrayList<>();
    
    // Conditional registration info
    private ConditionInfo conditionInfo;
    
    // Explicit dependencies from @DependsOn annotation
    private final List<String> explicitDependencies = new ArrayList<>();
    
    private String postConstructMethod;          // Method name with @PostConstruct
    private String postConstructDescriptor;
    private String preDestroyMethod;             // Method name with @PreDestroy
    private String preDestroyDescriptor;
    
    private boolean hasSubscribeMethods;          // Has @Subscribe methods (EventBus)
    private boolean isPrimary;                    // @Primary - preferred bean for type
    
    public ComponentInfo(String className, String componentName, Scope scope) {
        this(className, componentName, scope, false);
    }
    
    public ComponentInfo(String className, String componentName, Scope scope, boolean lazy) {
        this(className, componentName, scope, lazy, false);
    }
    
    public ComponentInfo(String className, String componentName, Scope scope, boolean lazy, boolean isPrimary) {
        this.className = className;
        this.internalName = className.replace('.', '/');
        this.componentName = componentName;
        this.scope = scope;
        this.lazy = lazy;
        this.isPrimary = isPrimary;
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
    
    public boolean isLazy() {
        return lazy;
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
    
    public boolean hasSubscribeMethods() {
        return hasSubscribeMethods;
    }
    
    public void setHasSubscribeMethods(boolean hasSubscribeMethods) {
        this.hasSubscribeMethods = hasSubscribeMethods;
    }
    
    public boolean hasFieldInjections() {
        return !fieldInjections.isEmpty();
    }
    
    public boolean hasMethodInjections() {
        return !methodInjections.isEmpty();
    }
    
    // Interface-based injection support
    
    /**
     * Returns all interfaces implemented by this component.
     * Used for interface-based injection.
     * 
     * @return list of fully qualified interface names
     */
    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }
    
    /**
     * Returns all interfaces in ASM internal name format.
     * 
     * @return list of internal names (e.g., "com/example/UserRepository")
     */
    public List<String> getImplementedInterfacesInternal() {
        return implementedInterfaces.stream()
                .map(name -> name.replace('.', '/'))
                .collect(Collectors.toList());
    }
    
    /**
     * Adds an interface that this component implements.
     * 
     * @param interfaceName fully qualified interface name
     */
    public void addImplementedInterface(String interfaceName) {
        this.implementedInterfaces.add(interfaceName);
    }
    
    /**
     * Checks if this component implements any interfaces.
     * 
     * @return true if component implements at least one interface
     */
    public boolean hasImplementedInterfaces() {
        return !implementedInterfaces.isEmpty();
    }
    
    // Conditional registration support
    
    /**
     * Gets the condition info for this component.
     * 
     * @return condition info, or null if no conditions
     */
    public ConditionInfo getConditionInfo() {
        return conditionInfo;
    }
    
    /**
     * Sets the condition info for this component.
     * 
     * @param conditionInfo the conditions to apply
     */
    public void setConditionInfo(ConditionInfo conditionInfo) {
        this.conditionInfo = conditionInfo;
    }
    
    /**
     * Checks if this component has conditional registration.
     * 
     * @return true if conditions are defined
     */
    public boolean hasConditions() {
        return conditionInfo != null && conditionInfo.hasConditions();
    }
    
    // Explicit dependencies support (@DependsOn)
    
    /**
     * Gets the explicit dependencies declared via @DependsOn.
     * 
     * @return list of bean names this component depends on
     */
    public List<String> getExplicitDependencies() {
        return explicitDependencies;
    }
    
    /**
     * Adds an explicit dependency.
     * 
     * @param dependencyBeanName the name of the bean this component depends on
     */
    public void addExplicitDependency(String dependencyBeanName) {
        this.explicitDependencies.add(dependencyBeanName);
    }
    
    /**
     * Sets all explicit dependencies at once.
     * 
     * @param dependencies list of bean names
     */
    public void setExplicitDependencies(List<String> dependencies) {
        this.explicitDependencies.clear();
        this.explicitDependencies.addAll(dependencies);
    }
    
    /**
     * Checks if this component has explicit dependencies.
     * 
     * @return true if @DependsOn is specified
     */
    public boolean hasExplicitDependencies() {
        return !explicitDependencies.isEmpty();
    }
    
    // Primary bean support (@Primary)
    
    /**
     * Checks if this component is marked as @Primary.
     * 
     * @return true if @Primary annotation is present
     */
    public boolean isPrimary() {
        return isPrimary;
    }
    
    /**
     * Sets whether this component is primary.
     * 
     * @param isPrimary true if this bean should be primary
     */
    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    // === AOP SUPPORT ===
    private final List<String> aopInterceptors = new ArrayList<>();
    
    public void addAopInterceptor(String interceptorClass) {
        aopInterceptors.add(interceptorClass);
    }
    
    public boolean hasAopInterceptors() {
        return !aopInterceptors.isEmpty();
    }
    
    public List<String> getAopInterceptors() {
        return aopInterceptors;
    }
    
    /**
     * Gets the dependency types from constructor injection.
     */
    public List<String> getDependencyTypes() {
        if (constructorInjection == null) {
            return List.of();
        }
        return constructorInjection.getDependencies().stream()
            .map(InjectionPoint.Dependency::getTypeName)
            .collect(Collectors.toList());
    }
}
