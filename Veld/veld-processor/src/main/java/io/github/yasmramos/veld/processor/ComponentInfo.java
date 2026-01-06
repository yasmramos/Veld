package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.LegacyScope;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds all metadata about a component discovered during annotation processing.
 * This information is used by ASM generators to create bytecode.
 */
public final class ComponentInfo {
    
    private String className;              // Fully qualified: com.example.MyService
    private String internalName;           // ASM internal: com/example/MyService
    private final String componentName;          // @Component value or simple class name
    private final LegacyScope scope;                   // SINGLETON or PROTOTYPE (for backwards compat)
    private final String scopeId;                // Custom scope ID (null for built-in scopes)
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
    private int order;                            // @Order - bean initialization order
    
    // TypeElement for AOP processing (transient - not serialized)
    private transient TypeElement typeElement;
    
    public ComponentInfo(String className, String componentName, LegacyScope scope) {
        this(className, componentName, scope, null, false);
    }
    
    public ComponentInfo(String className, String componentName, LegacyScope scope, String scopeId) {
        this(className, componentName, scope, scopeId, false);
    }
    
    public ComponentInfo(String className, String componentName, LegacyScope scope, String scopeId, boolean lazy) {
        this(className, componentName, scope, scopeId, lazy, false);
    }
    
    public ComponentInfo(String className, String componentName, LegacyScope scope, String scopeId, boolean lazy, boolean isPrimary) {
        this.className = className;
        this.internalName = className.replace('.', '/');
        this.componentName = componentName;
        this.scope = scope;
        this.scopeId = scopeId;
        this.lazy = lazy;
        this.isPrimary = isPrimary;
    }
    
    public String getClassName() {
        return className;
    }
    
    /**
     * Sets the class name and updates internal name accordingly.
     * 
     * @param className the new fully qualified class name
     */
    public void setClassName(String className) {
        this.className = className;
        this.internalName = className.replace('.', '/');
    }
    
    public String getInternalName() {
        return internalName;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public LegacyScope getScope() {
        return scope;
    }
    
    /**
     * Returns the scope ID for this component.
     * For built-in scopes (singleton, prototype), returns the scope enum name.
     * For custom scopes, returns the custom scope ID.
     * 
     * @return the scope identifier string
     */
    public String getScopeId() {
        if (scopeId != null && !scopeId.isEmpty()) {
            return scopeId;
        }
        return scope != null ? scope.name().toLowerCase() : "singleton";
    }
    
    /**
     * Sets a custom scope ID for this component.
     * 
     * @param scopeId the custom scope identifier
     */
    public void setScopeId(String scopeId) {
        // Intentionally blank - scopeId is final
    }
    
    /**
     * Returns whether this component uses a custom scope.
     * 
     * @return true if a custom scope is specified
     */
    public boolean hasCustomScope() {
        return scopeId != null && !scopeId.isEmpty();
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
    
    // Destruction order support (@DependsOn destroyOrder)
    
    private final List<String> explicitDestructionDependencies = new ArrayList<>();
    private int destroyOrderValue = 0;
    
    /**
     * Gets the explicit destruction dependencies declared via @DependsOn.
     * These beans will be destroyed AFTER this bean.
     * 
     * @return list of bean names this component must outlive
     */
    public List<String> getExplicitDestructionDependencies() {
        return explicitDestructionDependencies;
    }
    
    /**
     * Adds an explicit destruction dependency.
     * 
     * @param dependencyBeanName the name of the bean this component must outlive
     */
    public void addExplicitDestructionDependency(String dependencyBeanName) {
        this.explicitDestructionDependencies.add(dependencyBeanName);
    }
    
    /**
     * Sets all explicit destruction dependencies at once.
     * 
     * @param dependencies list of bean names
     */
    public void setExplicitDestructionDependencies(List<String> dependencies) {
        this.explicitDestructionDependencies.clear();
        this.explicitDestructionDependencies.addAll(dependencies);
    }
    
    /**
     * Checks if this component has explicit destruction dependencies.
     * 
     * @return true if @DependsOn destroyOrder is specified
     */
    public boolean hasExplicitDestructionDependencies() {
        return !explicitDestructionDependencies.isEmpty();
    }
    
    /**
     * Gets the destruction order value for this component.
     * Lower values are destroyed first, higher values are destroyed last.
     * 
     * @return the destruction order value
     */
    public int getDestroyOrderValue() {
        return destroyOrderValue;
    }
    
    /**
     * Sets the destruction order value for this component.
     * 
     * @param value the destruction order value
     */
    public void setDestroyOrderValue(int value) {
        this.destroyOrderValue = value;
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
     * Gets the order value for this component.
     * Lower values have higher priority.
     * 
     * @return the order value, defaults to 0
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * Sets the order value for this component.
     * 
     * @param order the order value, lower values have higher priority
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /**
     * Sets whether this component is primary.
     * 
     * @param isPrimary true if this bean should be primary
     */
    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    /**
     * Gets the TypeElement for this component (used for AOP processing).
     */
    public TypeElement getTypeElement() {
        return typeElement;
    }
    
    /**
     * Sets the TypeElement for this component.
     */
    public void setTypeElement(TypeElement typeElement) {
        this.typeElement = typeElement;
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
    
    // === UNRESOLVED INTERFACE DEPENDENCIES ===
    // Dependencies that are interfaces without @Component implementations
    
    private final List<String> unresolvedInterfaceDependencies = new ArrayList<>();
    
    /**
     * Adds an interface dependency that has no implementing component.
     * These can be resolved at runtime via mock injection or other mechanisms.
     * 
     * @param interfaceName fully qualified interface name
     */
    public void addUnresolvedInterfaceDependency(String interfaceName) {
        this.unresolvedInterfaceDependencies.add(interfaceName);
    }
    
    /**
     * Gets all interface dependencies that have no implementing component.
     * 
     * @return list of interface names that need external resolution
     */
    public List<String> getUnresolvedInterfaceDependencies() {
        return unresolvedInterfaceDependencies;
    }
    
    /**
     * Checks if this component has any unresolved interface dependencies.
     * 
     * @return true if there are interfaces without implementations
     */
    public boolean hasUnresolvedInterfaceDependencies() {
        return !unresolvedInterfaceDependencies.isEmpty();
    }
}
