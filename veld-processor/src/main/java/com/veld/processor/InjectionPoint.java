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
        
        public Dependency(String typeName, String typeDescriptor, String qualifierName) {
            this.typeName = typeName;
            this.typeDescriptor = typeDescriptor;
            this.qualifierName = qualifierName;
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
    }
}
