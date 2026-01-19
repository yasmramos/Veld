package io.github.yasmramos.veld.processor;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the Veld dependency graph.
 * This is the core model for static dependency injection.
 * 
 * <p>Each node corresponds to a component that will be instantiated:
 * <ul>
 *   <li>SINGLETON → becomes a static final field in Veld.java</li>
 *   <li>PROTOTYPE → becomes a static factory method in Veld.java</li>
 * </ul>
 */
public final class VeldNode {

    private static final String SCOPE_SINGLETON = "singleton";
    private static final String SCOPE_PROTOTYPE = "prototype";

    /**
     * The fully qualified class name of the component.
     * e.g., "com.example.service.UserService"
     */
    private final String className;

    /**
     * The name used to access this component from Veld.java.
     * From @Component("name") or @Singleton("name"), or derived from class name.
     * e.g., "userService" or "mainUserService"
     */
    private final String veldName;

    /**
     * The scope of this component.
     * "singleton" → static final field
     * "prototype" → static factory method
     */
    private final String scope;

    /**
     * Constructor injection information - null if no @Inject constructor.
     */
    private ConstructorInfo constructorInfo;

    /**
     * Field injections (requires AOP or weaver for private fields).
     */
    private final List<FieldInjection> fieldInjections = new ArrayList<>();

    /**
     * Method injections.
     */
    private final List<MethodInjection> methodInjections = new ArrayList<>();

    /**
     * Lifecycle methods.
     */
    private String postConstructMethod;
    private String preDestroyMethod;

    /**
     * The TypeElement for additional metadata (may be null for external beans).
     */
    private transient TypeElement typeElement;
    
    /**
     * Condition information for profile-based filtering at compile-time.
     */
    private ConditionInfo conditionInfo;

    /**
     * Whether this component should use lazy initialization.
     * Lazy components are not created until first accessed.
     */
    private boolean lazy;

    /**
     * List of AOP interceptor classes applied to this component.
     * Used for generating AOP wrapper classes.
     */
    private final List<String> aopInterceptors = new ArrayList<>();

    /**
     * Whether this component has an AOP wrapper class.
     * If true, use the AOP wrapper class name for instantiation.
     */
    private boolean hasAopWrapper;

    /**
     * Creates a new VeldNode.
     * 
     * @param className the fully qualified class name
     * @param veldName the name used to access from Veld.java
     * @param scope the scope string ("singleton" or "prototype")
     */
    public VeldNode(String className, String veldName, String scope) {
        this.className = className;
        this.veldName = veldName;
        this.scope = scope != null ? scope : SCOPE_SINGLETON;
    }

    /**
     * Gets the fully qualified class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the package name of the class.
     */
    public String getPackageName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Gets the simple class name (without package).
     */
    public String getSimpleName() {
        int lastDollar = className.lastIndexOf('$');
        int lastDot = className.lastIndexOf('.');
        String baseName = lastDollar > 0 ? className.substring(lastDollar + 1) : className;
        return lastDot > 0 ? baseName.substring(lastDot + 1) : baseName;
    }

    /**
     * Gets the Veld access name.
     */
    public String getVeldName() {
        return veldName;
    }

    /**
     * Gets the scope type.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Checks if this is a singleton.
     */
    public boolean isSingleton() {
        return SCOPE_SINGLETON.equals(scope);
    }

    /**
     * Checks if this is a prototype.
     */
    public boolean isPrototype() {
        return SCOPE_PROTOTYPE.equals(scope);
    }

    /**
     * Checks if this component has constructor injection.
     */
    public boolean hasConstructorInjection() {
        return constructorInfo != null;
    }

    /**
     * Gets the constructor info.
     */
    public ConstructorInfo getConstructorInfo() {
        return constructorInfo;
    }

    /**
     * Sets the constructor info.
     */
    public void setConstructorInfo(ConstructorInfo constructorInfo) {
        this.constructorInfo = constructorInfo;
    }

    /**
     * Gets all field injections.
     */
    public List<FieldInjection> getFieldInjections() {
        return new ArrayList<>(fieldInjections);
    }

    /**
     * Adds a field injection.
     */
    public void addFieldInjection(FieldInjection injection) {
        fieldInjections.add(injection);
    }

    /**
     * Checks if this component has field injections that require an accessor.
     * Private fields require an accessor.
     */
    public boolean hasPrivateFieldInjections() {
        for (FieldInjection injection : fieldInjections) {
            if (injection.isPrivate()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this component has field injections that are not public.
     * Non-public (private or package-private) fields require an accessor
     * since Veld.java is in a different package.
     */
    public boolean hasNonPublicFieldInjections() {
        for (FieldInjection injection : fieldInjections) {
            if (!injection.isPublic()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this component has any field injections.
     */
    public boolean hasFieldInjections() {
        return !fieldInjections.isEmpty();
    }

    /**
     * Checks if this component has method injections that require an accessor.
     * Private methods require an accessor.
     */
    public boolean hasPrivateMethodInjections() {
        for (MethodInjection injection : methodInjections) {
            if (injection.isPrivate()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this component needs an accessor class.
     * Accessor is needed if there are non-public field injections or private method injections.
     * Non-public fields (private or package-private) cannot be accessed from Veld.java
     * which is in a different package.
     */
    public boolean needsAccessorClass() {
        return hasNonPublicFieldInjections() || hasPrivateMethodInjections();
    }

    /**
     * Gets the accessor class name for this component.
     * Format: com.example.Service$$Accessor
     */
    public String getAccessorClassName() {
        return className + "$$Accessor";
    }

    /**
     * Gets the accessor class simple name.
     * Format: Service$$Accessor
     */
    public String getAccessorSimpleName() {
        return getSimpleName() + "$$Accessor";
    }

    /**
     * Gets all method injections.
     */
    public List<MethodInjection> getMethodInjections() {
        return new ArrayList<>(methodInjections);
    }

    /**
     * Adds a method injection.
     */
    public void addMethodInjection(MethodInjection injection) {
        methodInjections.add(injection);
    }

    /**
     * Checks if this component has method injections.
     */
    public boolean hasMethodInjections() {
        return !methodInjections.isEmpty();
    }

    /**
     * Gets the @PostConstruct method name.
     */
    public String getPostConstructMethod() {
        return postConstructMethod;
    }

    /**
     * Sets the @PostConstruct method name.
     */
    public void setPostConstruct(String methodName) {
        this.postConstructMethod = methodName;
    }

    /**
     * Checks if this component has @PostConstruct.
     */
    public boolean hasPostConstruct() {
        return postConstructMethod != null;
    }

    /**
     * Gets the @PreDestroy method name.
     */
    public String getPreDestroyMethod() {
        return preDestroyMethod;
    }

    /**
     * Sets the @PreDestroy method name.
     */
    public void setPreDestroy(String methodName) {
        this.preDestroyMethod = methodName;
    }

    /**
     * Checks if this component has @PreDestroy.
     */
    public boolean hasPreDestroy() {
        return preDestroyMethod != null;
    }

    /**
     * Gets the TypeElement.
     */
    public TypeElement getTypeElement() {
        return typeElement;
    }

    /**
     * Gets the Element for use with Messager.
     */
    public javax.lang.model.element.Element getElement() {
        return typeElement;
    }

    /**
     * Sets the TypeElement.
     */
    public void setTypeElement(TypeElement typeElement) {
        this.typeElement = typeElement;
    }
    
    /**
     * Gets the condition information for this component.
     */
    public ConditionInfo getConditionInfo() {
        return conditionInfo;
    }
    
    /**
     * Sets the condition information for this component.
     */
    public void setConditionInfo(ConditionInfo conditionInfo) {
        this.conditionInfo = conditionInfo;
    }

    /**
     * Checks if this component should be lazily initialized.
     */
    public boolean isLazy() {
        return lazy;
    }

    /**
     * Sets whether this component should be lazily initialized.
     */
    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Checks if this component has AOP interceptors.
     */
    public boolean hasAopInterceptors() {
        return !aopInterceptors.isEmpty();
    }

    /**
     * Gets the list of AOP interceptor classes.
     */
    public List<String> getAopInterceptors() {
        return new ArrayList<>(aopInterceptors);
    }

    /**
     * Adds an AOP interceptor class.
     */
    public void addAopInterceptor(String interceptorClass) {
        aopInterceptors.add(interceptorClass);
    }

    /**
     * Checks if this component has an AOP wrapper class.
     */
    public boolean hasAopWrapper() {
        return hasAopWrapper;
    }

    /**
     * Sets whether this component has an AOP wrapper class.
     */
    public void setHasAopWrapper(boolean hasAopWrapper) {
        this.hasAopWrapper = hasAopWrapper;
    }

    /**
     * Gets the actual class name to use for instantiation.
     * If hasAopWrapper is true, returns the AOP wrapper class name.
     */
    public String getActualClassName() {
        if (hasAopWrapper) {
            return className + "$$Aop";
        }
        return className;
    }

    /**
     * Checks if this component needs factory pattern (complex initialization).
     */
    public boolean needsFactoryPattern() {
        // Needs factory if has field injections (especially private)
        // or method injections, or lifecycle callbacks
        return hasFieldInjections() || hasMethodInjections() 
            || hasPostConstruct() || hasPreDestroy();
    }

    @Override
    public String toString() {
        return "VeldNode{" +
                "className='" + className + '\'' +
                ", veldName='" + veldName + '\'' +
                ", scope=" + scope +
                '}';
    }

    /**
     * Constructor information for creating instances.
     */
    public static final class ConstructorInfo {
        private final List<ParameterInfo> parameters = new ArrayList<>();

        public void addParameter(ParameterInfo param) {
            parameters.add(param);
        }

        public List<ParameterInfo> getParameters() {
            return new ArrayList<>(parameters);
        }

        public int getParameterCount() {
            return parameters.size();
        }

        public boolean hasParameters() {
            return !parameters.isEmpty();
        }
    }

    /**
     * Parameter information for constructor or method.
     */
    public static final class ParameterInfo {
        private final String typeName;
        private final String parameterName;
        private final boolean isProvider;
        private final boolean isOptional;
        private final boolean isOptionalWrapper;
        private final String actualTypeName;  // For Provider<T>/Optional<T>, the T
        private final String qualifierName;
        private final String valueExpression;

        public ParameterInfo(String typeName, String parameterName) {
            this(typeName, parameterName, false, false, false, typeName, null, null);
        }

        public ParameterInfo(String typeName, String parameterName, boolean isProvider,
                            boolean isOptional, boolean isOptionalWrapper, String actualTypeName,
                            String qualifierName, String valueExpression) {
            this.typeName = typeName;
            this.parameterName = parameterName;
            this.isProvider = isProvider;
            this.isOptional = isOptional;
            this.isOptionalWrapper = isOptionalWrapper;
            this.actualTypeName = actualTypeName;
            this.qualifierName = qualifierName;
            this.valueExpression = valueExpression;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getParameterName() {
            return parameterName;
        }

        public boolean isProvider() {
            return isProvider;
        }

        public boolean isOptional() {
            return isOptional;
        }

        public boolean isOptionalWrapper() {
            return isOptionalWrapper;
        }

        public String getActualTypeName() {
            return actualTypeName;
        }

        public String getQualifierName() {
            return qualifierName;
        }

        public boolean hasQualifier() {
            return qualifierName != null && !qualifierName.isEmpty();
        }

        public String getValueExpression() {
            return valueExpression;
        }

        public boolean isValueInjection() {
            return valueExpression != null;
        }

        public boolean allowsMissing() {
            return isOptional || isOptionalWrapper;
        }

        public boolean needsProviderWrapper() {
            return isProvider;
        }

        public boolean needsOptionalWrapper() {
            return isOptionalWrapper;
        }
    }

    /**
     * Field injection information.
     */
    public static final class FieldInjection {
        private final String fieldName;
        private final String typeName;
        private final boolean isProvider;
        private final boolean isOptional;
        private final boolean isOptionalWrapper;
        private final String actualTypeName;
        private final String qualifierName;
        private final boolean isPrivate;
        private final boolean isPublic;
        private final boolean isValueInjection;

        public FieldInjection(String fieldName, String typeName) {
            this(fieldName, typeName, false, false, false, typeName, null, false, false, false);
        }

        public FieldInjection(String fieldName, String typeName, boolean isProvider,
                            boolean isOptional, boolean isOptionalWrapper, String actualTypeName,
                            String qualifierName) {
            this(fieldName, typeName, isProvider, isOptional, isOptionalWrapper, actualTypeName, qualifierName, false, false, false);
        }

        public FieldInjection(String fieldName, String typeName, boolean isProvider,
                            boolean isOptional, boolean isOptionalWrapper, String actualTypeName,
                            String qualifierName, boolean isPrivate) {
            this(fieldName, typeName, isProvider, isOptional, isOptionalWrapper, actualTypeName, qualifierName, isPrivate, false, false);
        }

        public FieldInjection(String fieldName, String typeName, boolean isProvider,
                            boolean isOptional, boolean isOptionalWrapper, String actualTypeName,
                            String qualifierName, boolean isPrivate, boolean isPublic) {
            this(fieldName, typeName, isProvider, isOptional, isOptionalWrapper, actualTypeName, qualifierName, isPrivate, isPublic, false);
        }

        public FieldInjection(String fieldName, String typeName, boolean isProvider,
                            boolean isOptional, boolean isOptionalWrapper, String actualTypeName,
                            String qualifierName, boolean isPrivate, boolean isPublic, boolean isValueInjection) {
            this.fieldName = fieldName;
            this.typeName = typeName;
            this.isProvider = isProvider;
            this.isOptional = isOptional;
            this.isOptionalWrapper = isOptionalWrapper;
            this.actualTypeName = actualTypeName;
            this.qualifierName = qualifierName;
            this.isPrivate = isPrivate;
            this.isPublic = isPublic;
            this.isValueInjection = isValueInjection;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getTypeName() {
            return typeName;
        }

        public boolean isProvider() {
            return isProvider;
        }

        public boolean isOptional() {
            return isOptional;
        }

        public boolean isOptionalWrapper() {
            return isOptionalWrapper;
        }

        public String getActualTypeName() {
            return actualTypeName;
        }

        public String getQualifierName() {
            return qualifierName;
        }

        public boolean hasQualifier() {
            return qualifierName != null && !qualifierName.isEmpty();
        }

        /**
         * Checks if this field is private.
         * Private fields require an accessor class for injection.
         */
        public boolean isPrivate() {
            return isPrivate;
        }

        /**
         * Checks if this is a @Value injection.
         */
        public boolean isValueInjection() {
            return isValueInjection;
        }

        /**
         * Checks if this field is public.
         * Public fields can be accessed directly from Veld.java.
         */
        public boolean isPublic() {
            // If not marked as private and has no qualifier, it's package-private by default
            // We need to track visibility explicitly
            return false; // Default assumption: fields are package-private unless marked public
        }

        /**
         * Checks if this field requires an accessor class.
         * Non-public (private or package-private) fields require an accessor
         * since Veld.java is in a different package.
         */
        public boolean requiresAccessor() {
            return !isPublic();
        }
    }

    /**
     * Method injection information.
     */
    public static final class MethodInjection {
        private final String methodName;
        private final List<ParameterInfo> parameters = new ArrayList<>();
        private final boolean isPrivate;

        public MethodInjection(String methodName) {
            this(methodName, false);
        }

        public MethodInjection(String methodName, boolean isPrivate) {
            this.methodName = methodName;
            this.isPrivate = isPrivate;
        }

        public void addParameter(ParameterInfo param) {
            parameters.add(param);
        }

        public String getMethodName() {
            return methodName;
        }

        public List<ParameterInfo> getParameters() {
            return new ArrayList<>(parameters);
        }

        /**
         * Checks if this method is private.
         * Private methods require an accessor class for invocation.
         */
        public boolean isPrivate() {
            return isPrivate;
        }

        /**
         * Checks if this method requires an accessor class.
         * Private methods always require an accessor.
         */
        public boolean requiresAccessor() {
            return isPrivate;
        }
    }
}
