package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.InjectionPoint;
import io.github.yasmramos.veld.spi.extension.ComponentNode;
import io.github.yasmramos.veld.spi.extension.DependencyEdge;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

/**
 * Implementación de {@link DependencyEdge} que envuelve un {@link InjectionPoint.Dependency}.
 * 
 * <p>Esta clase representa una relación de inyección desde un componente origen hacia
 * un componente objetivo, incluyendo información sobre el tipo de inyección y metadatos.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
final class SpiDependencyEdge implements DependencyEdge {
    
    private final SpiComponentNode source;
    private final SpiComponentNode target;
    private final InjectionPoint.Type injectionType;
    private final InjectionPoint.Dependency dependency;
    private final Element injectionElement;
    private final DependencyKind dependencyKind;
    
    SpiDependencyEdge(SpiComponentNode source, SpiComponentNode target, 
                      InjectionPoint.Type injectionType, 
                      InjectionPoint.Dependency dependency,
                      Element injectionElement) {
        this.source = source;
        this.target = target;
        this.injectionType = injectionType;
        this.dependency = dependency;
        this.injectionElement = injectionElement;
        this.dependencyKind = determineDependencyKind(dependency);
    }
    
    private static DependencyKind determineDependencyKind(InjectionPoint.Dependency dep) {
        if (dep.isOptionalWrapper() || dep.isOptional()) {
            return DependencyKind.OPTIONAL;
        }
        if (dep.isProvider()) {
            return DependencyKind.PROVIDER;
        }
        return DependencyKind.NORMAL;
    }
    
    @Override
    public ComponentNode getSource() {
        return source;
    }
    
    @Override
    public ComponentNode getTarget() {
        return target;
    }
    
    @Override
    public InjectionType getInjectionType() {
        return mapInjectionType(injectionType);
    }
    
    private static InjectionType mapInjectionType(InjectionPoint.Type type) {
        return switch (type) {
            case CONSTRUCTOR -> InjectionType.CONSTRUCTOR;
            case FIELD -> InjectionType.FIELD;
            case METHOD -> InjectionType.METHOD;
        };
    }
    
    @Override
    public Element getInjectionElement() {
        return injectionElement;
    }
    
    @Override
    public Optional<String> getFieldName() {
        if (injectionType == InjectionPoint.Type.FIELD) {
            return Optional.of(dependency.getTypeName().substring(
                dependency.getTypeName().lastIndexOf('.') + 1).toLowerCase());
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<String> getMethodName() {
        if (injectionType == InjectionPoint.Type.METHOD) {
            // Return a reasonable default method name
            String simpleName = dependency.getTypeName().substring(
                dependency.getTypeName().lastIndexOf('.') + 1);
            return Optional.of("set" + capitalize(simpleName));
        }
        return Optional.empty();
    }
    
    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    @Override
    public DependencyKind getDependencyKind() {
        return dependencyKind;
    }
    
    @Override
    public TypeMirror getInjectionTypeMirror() {
        // We don't have direct access to the TypeMirror from InjectionPoint.Dependency
        // Return null as this is a compile-time only interface
        return null;
    }
    
    @Override
    public boolean isRequired() {
        return !dependency.allowsMissing() && !dependency.isValueInjection();
    }
    
    @Override
    public boolean isCollection() {
        // Check if the type suggests a collection injection
        String typeName = dependency.getTypeName();
        return typeName.startsWith("java.util.List") || 
               typeName.startsWith("java.util.Set") ||
               typeName.startsWith("java.util.Collection");
    }
    
    @Override
    public Optional<String> getQualifierName() {
        String qualifier = dependency.getQualifierName();
        if (qualifier != null && !qualifier.isEmpty()) {
            return Optional.of(qualifier);
        }
        return Optional.empty();
    }
    
    @Override
    public String toString() {
        return String.format("DependencyEdge{%s -> %s [%s]}", 
                source.getSimpleName(), target.getSimpleName(), injectionType);
    }
}
