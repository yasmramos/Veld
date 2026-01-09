package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.ScopeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for a bean exported by a module.
 * This metadata is generated during compilation and used by dependent modules
 * to resolve bean dependencies across module boundaries.
 * 
 * Format is designed to be lightweight and serializable to JSON for storage
 * in META-INF/veld/{module-id}.json files.
 */
public final class BeanMetadata {

    private String moduleId;
    private String beanName;
    private String beanType;
    private String factoryClassName;
    private String factoryMethodName;
    private String factoryMethodDescriptor;
    private ScopeType scope;
    private String qualifier;
    private boolean isPrimary;
    private List<String> dependencies = new ArrayList<>();

    public BeanMetadata() {
    }

    public BeanMetadata(String moduleId, String beanName, String beanType) {
        this.moduleId = moduleId;
        this.beanName = beanName;
        this.beanType = beanType;
    }

    // Builder-style methods
    public BeanMetadata withFactory(String factoryClassName, String methodName, String descriptor) {
        this.factoryClassName = factoryClassName;
        this.factoryMethodName = methodName;
        this.factoryMethodDescriptor = descriptor;
        return this;
    }

    public BeanMetadata withScope(ScopeType scope) {
        this.scope = scope;
        return this;
    }

    public BeanMetadata withQualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    public BeanMetadata asPrimary() {
        this.isPrimary = true;
        return this;
    }

    public BeanMetadata addDependency(String dependencyType) {
        this.dependencies.add(dependencyType);
        return this;
    }

    // Getters
    public String getModuleId() {
        return moduleId;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getBeanType() {
        return beanType;
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }

    public String getFactoryMethodName() {
        return factoryMethodName;
    }

    public String getFactoryMethodDescriptor() {
        return factoryMethodDescriptor;
    }

    public ScopeType getScope() {
        return scope;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean hasQualifier() {
        return qualifier != null && !qualifier.isEmpty();
    }

    @Override
    public String toString() {
        return "BeanMetadata{" +
                "moduleId='" + moduleId + '\'' +
                ", beanName='" + beanName + '\'' +
                ", beanType='" + beanType + '\'' +
                ", factoryClassName='" + factoryClassName + '\'' +
                ", scope=" + scope +
                '}';
    }
}
