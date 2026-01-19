package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.Lookup;
import java.util.List;

/**
 * Represents an injection point in a component.
 * Can be a constructor, field, or method.
 */
public final class InjectionPoint {
    
    public enum Type {
        CONSTRUCTOR,
        FIELD,
        METHOD
    }
    
    /**
     * Represents the visibility/access level of a field or method.
     */
    public enum Visibility {
        PRIVATE,
        PACKAGE_PRIVATE,
        PROTECTED,
        PUBLIC
    }
    
    private final Type type;
    private final String name;
    private final String descriptor;
    private final List<Dependency> dependencies;
    private final Visibility visibility;
    
    public InjectionPoint(Type type, String name, String descriptor, List<Dependency> dependencies) {
        this(type, name, descriptor, dependencies, Visibility.PACKAGE_PRIVATE);
    }
    
    public InjectionPoint(Type type, String name, String descriptor, List<Dependency> dependencies, Visibility visibility) {
        this.type = type;
        this.name = name;
        this.descriptor = descriptor;
        this.dependencies = List.copyOf(dependencies);
        this.visibility = visibility;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public List<Dependency> getDependencies() {
        return dependencies;
    }
    
    public Visibility getVisibility() {
        return visibility;
    }
    
    /**
     * Returns true if this field is private and requires a synthetic setter.
     * Only applicable for FIELD type injection points.
     */
    public boolean requiresSyntheticSetter() {
        return type == Type.FIELD && visibility == Visibility.PRIVATE;
    }
    
    /**
     * Represents a single dependency at an injection point.
     */
    public static final class Dependency {
        private final String typeName;           // Fully qualified class name
        private final String typeDescriptor;     // ASM type descriptor
        private final String qualifierName;      // @Named value, or null
        private final boolean isProvider;        // true if Provider<T>
        private final boolean isLazy;            // true if @Lazy
        private final boolean isOptional;        // true if @Optional annotation present
        private final boolean isOptionalWrapper; // true if type is java.util.Optional<T>
        private final String actualTypeName;     // For Provider<T>/Optional<T>, the T type
        private final String actualTypeDescriptor; // For Provider<T>/Optional<T>, the T descriptor
        private final String valueExpression;    // @Value expression, or null
        private final Lookup lookupAnnotation;   // @Lookup annotation, or null
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName) {
            this(typeName, typeDescriptor, qualifierName, false, false, false, false, typeName, typeDescriptor, null, null);
        }
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName,
                          boolean isProvider, boolean isLazy, 
                          String actualTypeName, String actualTypeDescriptor) {
            this(typeName, typeDescriptor, qualifierName, isProvider, isLazy, false, false, actualTypeName, actualTypeDescriptor, null, null);
        }
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName,
                          boolean isProvider, boolean isLazy, boolean isOptional, boolean isOptionalWrapper,
                          String actualTypeName, String actualTypeDescriptor) {
            this(typeName, typeDescriptor, qualifierName, isProvider, isLazy, isOptional, isOptionalWrapper, actualTypeName, actualTypeDescriptor, null, null);
        }
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName,
                          boolean isProvider, boolean isLazy, boolean isOptional, boolean isOptionalWrapper,
                          String actualTypeName, String actualTypeDescriptor, String valueExpression) {
            this(typeName, typeDescriptor, qualifierName, isProvider, isLazy, isOptional, isOptionalWrapper, actualTypeName, actualTypeDescriptor, valueExpression, null);
        }
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName,
                          boolean isProvider, boolean isLazy, boolean isOptional, boolean isOptionalWrapper,
                          String actualTypeName, String actualTypeDescriptor, String valueExpression, Lookup lookupAnnotation) {
            this.typeName = typeName;
            this.typeDescriptor = typeDescriptor;
            this.qualifierName = qualifierName;
            this.isProvider = isProvider;
            this.isLazy = isLazy;
            this.isOptional = isOptional;
            this.isOptionalWrapper = isOptionalWrapper;
            this.actualTypeName = actualTypeName;
            this.actualTypeDescriptor = actualTypeDescriptor;
            this.valueExpression = valueExpression;
            this.lookupAnnotation = lookupAnnotation;
        }
        
        /**
         * Creates a @Value dependency.
         */
        public static Dependency forValue(String typeName, String typeDescriptor, String valueExpression) {
            return new Dependency(typeName, typeDescriptor, null, false, false, false, false, typeName, typeDescriptor, valueExpression, null);
        }
        
        /**
         * Creates a @Lookup dependency.
         */
        public static Dependency forLookup(String typeName, String typeDescriptor, Lookup lookupAnnotation) {
            return new Dependency(typeName, typeDescriptor, null, false, false, lookupAnnotation.optional(), false, typeName, typeDescriptor, null, lookupAnnotation);
        }
        
        public String getTypeName() {
            return typeName;
        }
        
        public String getTypeDescriptor() {
            return typeDescriptor;
        }
        
        public String getQualifierName() {
            return qualifierName;
        }
        
        public boolean hasQualifier() {
            return qualifierName != null && !qualifierName.isEmpty();
        }
        
        /**
         * Returns true if this dependency is a Provider&lt;T&gt;.
         */
        public boolean isProvider() {
            return isProvider;
        }
        
        /**
         * Returns true if this dependency is marked with @Lazy.
         */
        public boolean isLazy() {
            return isLazy;
        }
        
        /**
         * For Provider&lt;T&gt; dependencies, returns the actual type T.
         * For regular dependencies, returns the same as getTypeName().
         */
        public String getActualTypeName() {
            return actualTypeName;
        }
        
        /**
         * For Provider&lt;T&gt; dependencies, returns the actual type descriptor of T.
         * For regular dependencies, returns the same as getTypeDescriptor().
         */
        public String getActualTypeDescriptor() {
            return actualTypeDescriptor;
        }
        
        /**
         * Returns true if this dependency is marked with @Optional.
         */
        public boolean isOptional() {
            return isOptional;
        }
        
        /**
         * Returns true if this dependency is of type java.util.Optional&lt;T&gt;.
         */
        public boolean isOptionalWrapper() {
            return isOptionalWrapper;
        }
        
        /**
         * Returns true if this dependency should allow missing beans.
         * Either @Optional annotation or Optional&lt;T&gt; type.
         */
        public boolean allowsMissing() {
            return isOptional || isOptionalWrapper;
        }
        
        /**
         * Returns true if this dependency needs special handling (Provider, Lazy, or Optional).
         */
        public boolean needsSpecialHandling() {
            return isProvider || isLazy || isOptional || isOptionalWrapper;
        }
        
        /**
         * Returns the @Value expression, or null if not a value injection.
         */
        public String getValueExpression() {
            return valueExpression;
        }
        
        /**
         * Returns true if this is a @Value injection.
         */
        public boolean isValueInjection() {
            return valueExpression != null;
        }
        
        /**
         * Returns the @Lookup annotation, or null if not a lookup injection.
         */
        public Lookup getLookupAnnotation() {
            return lookupAnnotation;
        }
        
        /**
         * Returns true if this is a @Lookup injection.
         */
        public boolean isLookupInjection() {
            return lookupAnnotation != null;
        }
    }
}
