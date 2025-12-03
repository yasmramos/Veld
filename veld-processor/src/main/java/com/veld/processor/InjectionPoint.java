package com.veld.processor;

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
    
    private final Type type;
    private final String name;
    private final String descriptor;
    private final List<Dependency> dependencies;
    
    public InjectionPoint(Type type, String name, String descriptor, List<Dependency> dependencies) {
        this.type = type;
        this.name = name;
        this.descriptor = descriptor;
        this.dependencies = List.copyOf(dependencies);
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
    
    /**
     * Represents a single dependency at an injection point.
     */
    public static final class Dependency {
        private final String typeName;           // Fully qualified class name
        private final String typeDescriptor;     // ASM type descriptor
        private final String qualifierName;      // @Named value, or null
        private final boolean isProvider;        // true if Provider<T>
        private final boolean isLazy;            // true if @Lazy
        private final String actualTypeName;     // For Provider<T>, the T type
        private final String actualTypeDescriptor; // For Provider<T>, the T descriptor
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName) {
            this(typeName, typeDescriptor, qualifierName, false, false, typeName, typeDescriptor);
        }
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName,
                          boolean isProvider, boolean isLazy, 
                          String actualTypeName, String actualTypeDescriptor) {
            this.typeName = typeName;
            this.typeDescriptor = typeDescriptor;
            this.qualifierName = qualifierName;
            this.isProvider = isProvider;
            this.isLazy = isLazy;
            this.actualTypeName = actualTypeName;
            this.actualTypeDescriptor = actualTypeDescriptor;
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
         * Returns true if this dependency needs special handling (Provider or Lazy).
         */
        public boolean needsSpecialHandling() {
            return isProvider || isLazy;
        }
    }
}
