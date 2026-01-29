package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.InjectionPoint;
import io.github.yasmramos.veld.spi.extension.ComponentNode;
import io.github.yasmramos.veld.spi.extension.DependencyEdge;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ComponentNode} que envuelve un {@link ComponentInfo}.
 * 
 * <p>Esta clase proporciona una vista SPI de un componente descubierto por el processor,
 * exponiendo toda la información relevante para las extensiones de Veld.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
final class SpiComponentNode implements ComponentNode {
    
    private static final String SCOPE_SINGLETON = "singleton";
    
    private final ComponentInfo componentInfo;
    private final List<DependencyEdge> dependencies;
    private final List<DependencyEdge> injectors;
    private final String packageName;
    private final String simpleName;
    
    SpiComponentNode(ComponentInfo componentInfo) {
        this.componentInfo = componentInfo;
        this.packageName = componentInfo.getPackageName();
        this.simpleName = componentInfo.getSimpleName();
        this.dependencies = new ArrayList<>();
        this.injectors = new ArrayList<>();
    }
    
    /**
     * Añade una arista de dependencia saliente.
     */
    void addDependency(DependencyEdge edge) {
        dependencies.add(edge);
    }
    
    /**
     * Añade una arista de dependencia entrante (inyector).
     */
    void addInjector(DependencyEdge edge) {
        injectors.add(edge);
    }
    
    @Override
    public String getQualifiedName() {
        return componentInfo.getClassName();
    }
    
    @Override
    public String getSimpleName() {
        return simpleName;
    }
    
    @Override
    public String getPackageName() {
        return packageName;
    }
    
    @Override
    public String getScope() {
        return componentInfo.getScope();
    }
    
    @Override
    public Element getElement() {
        return componentInfo.getTypeElement();
    }
    
    @Override
    public TypeMirror getTypeMirror() {
        return componentInfo.getTypeElement() != null 
            ? componentInfo.getTypeElement().asType() 
            : null;
    }
    
    @Override
    public List<DependencyEdge> getDependencies() {
        return List.copyOf(dependencies);
    }
    
    @Override
    public List<DependencyEdge> getInjectors() {
        return List.copyOf(injectors);
    }
    
    @Override
    public int getDependencyCount() {
        return dependencies.size();
    }
    
    @Override
    public int getInjectorCount() {
        return injectors.size();
    }
    
    @Override
    public boolean hasDependency(String targetName) {
        return dependencies.stream()
            .anyMatch(edge -> edge.getTarget().getQualifiedName().equals(targetName));
    }
    
    @Override
    public Optional<DependencyEdge> findDependency(String targetName) {
        return dependencies.stream()
            .filter(edge -> edge.getTarget().getQualifiedName().equals(targetName))
            .findFirst();
    }
    
    @Override
    public boolean isSingleton() {
        return SCOPE_SINGLETON.equals(componentInfo.getScope());
    }
    
    @Override
    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }
    
    @Override
    public boolean hasInjectors() {
        return !injectors.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("ComponentNode{name=%s, scope=%s, deps=%d}", 
                simpleName, componentInfo.getScope(), dependencies.size());
    }
    
    // Package-private getters for use by SpiDependencyEdge
    
    ComponentInfo getComponentInfo() {
        return componentInfo;
    }
}
