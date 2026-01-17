package io.github.yasmramos.veld.processor.spi;
import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.InjectionPoint;
import io.github.yasmramos.veld.spi.extension.ComponentNode;
import io.github.yasmramos.veld.spi.extension.DependencyEdge;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * Implementación de {@link VeldGraph} que construye y gestiona el grafo de dependencias.
 * 
 * <p>Esta clase es inmutable después de construida y proporciona acceso de solo lectura
 * a todos los componentes y sus relaciones de dependencia para las extensiones de Veld.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
final class SpiVeldGraph implements VeldGraph {
    private final Map<String, SpiComponentNode> nodesByName;
    private final List<ComponentNode> components;
    private final List<DependencyEdge> allDependencies;
    private final boolean isImmutable;
    private SpiVeldGraph(Map<String, SpiComponentNode> nodesByName, 
                         List<ComponentNode> components,
                         List<DependencyEdge> allDependencies) {
        this.nodesByName = Collections.unmodifiableMap(new HashMap<>(nodesByName));
        this.components = Collections.unmodifiableList(new ArrayList<>(components));
        this.allDependencies = Collections.unmodifiableList(new ArrayList<>(allDependencies));
        this.isImmutable = true;
    }
    
    /**
     * Builder para construir el grafo de manera incremental.
     */
    static Builder builder() {
        return new Builder();
    }
    
    @Override
    public Collection<ComponentNode> getComponents() {
        return components;
    }
    
    @Override
    public int getComponentCount() {
        return components.size();
    }
    
    @Override
    public Optional<ComponentNode> findComponent(String qualifiedName) {
        return Optional.ofNullable(nodesByName.get(qualifiedName));
    }
    
    @Override
    public Collection<ComponentNode> findComponentsBySuperType(String typeName) {
        return components.stream()
            .filter(node -> {
                TypeMirror typeMirror = node.getTypeMirror();
                if (typeMirror == null) return false;
                Element element = node.getElement();
                if (element == null) return false;
                // Check direct interfaces - cast to TypeElement to access getInterfaces()
                if (!(element instanceof TypeElement)) return false;
                TypeElement typeElement = (TypeElement) element;
                for (TypeMirror iface : typeElement.getInterfaces()) {
                    if (iface.toString().equals(typeName)) {
                        return true;
                    }
                }
                return false;
            })
            .toList();
    }
    
    @Override
    public Collection<ComponentNode> findComponentsWithAnnotation(String annotationName) {
        return components.stream()
            .filter(node -> {
                Element element = node.getElement();
                if (element == null) return false;
                for (AnnotationMirror ann : element.getAnnotationMirrors()) {
                    String annType = ann.getAnnotationType().asElement().toString();
                    if (annType.equals(annotationName)) {
                        return true;
                    }
                }
                return false;
            })
            .toList();
    }
    
    @Override
    public Set<ComponentNode> findCycle(ComponentNode start) {
        if (!(start instanceof SpiComponentNode spiStart)) {
            return Collections.emptySet();
        }
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Map<String, ComponentNode> parent = new HashMap<>();
        Optional<List<ComponentNode>> cycle = dfsCycle(spiStart, visited, recursionStack, parent);
        if (cycle.isPresent()) {
            return new HashSet<>(cycle.get());
        }
        return Collections.emptySet();
    }
    
    private Optional<List<ComponentNode>> dfsCycle(SpiComponentNode current,
                                                     Set<String> visited,
                                                     Set<String> recursionStack,
                                                     Map<String, ComponentNode> parent) {
        visited.add(current.getQualifiedName());
        recursionStack.add(current.getQualifiedName());
        for (DependencyEdge edge : current.getDependencies()) {
            ComponentNode targetNode = edge.getTarget();
            if (!(targetNode instanceof SpiComponentNode spiTarget)) {
                continue;
            }
            String targetName = spiTarget.getQualifiedName();
            if (!visited.contains(targetName)) {
                parent.put(targetName, current);
                Optional<List<ComponentNode>> cycle = dfsCycle(spiTarget, visited, recursionStack, parent);
                if (cycle.isPresent()) {
                    return cycle;
                }
            } else if (recursionStack.contains(targetName)) {
                // Cycle detected - build the cycle path
                List<ComponentNode> cyclePath = new ArrayList<>();
                cyclePath.add(spiTarget);
                ComponentNode node = current;
                while (node != null && !node.getQualifiedName().equals(targetName)) {
                    cyclePath.add(node);
                    if (node instanceof SpiComponentNode spiNode) {
                        node = parent.get(spiNode.getQualifiedName());
                    } else {
                        break;
                    }
                }
                cyclePath.add(spiTarget); // Close the cycle
                Collections.reverse(cyclePath);
                return Optional.of(cyclePath);
            }
        }
        recursionStack.remove(current.getQualifiedName());
        return Optional.empty();
    }
    
    @Override
    public Collection<DependencyEdge> getAllDependencies() {
        return allDependencies;
    }
    
    @Override
    public boolean isEmpty() {
        return components.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("VeldGraph{components=%d, dependencies=%d}", 
                components.size(), allDependencies.size());
    }
    
    /**
     * Builder para construir el grafo de dependencias.
     */
    static final class Builder {
        private final Map<String, SpiComponentNode> nodesByName = new HashMap<>();
        private final List<DependencyEdge> allDependencies = new ArrayList<>();
        private Builder() {
        }
        
        /**
         * Añade un componente al grafo.
         */
        Builder addComponent(ComponentInfo componentInfo) {
            String name = componentInfo.getClassName();
            if (!nodesByName.containsKey(name)) {
                nodesByName.put(name, new SpiComponentNode(componentInfo));
            }
            return this;
        }
        
        /**
         * Añade una dependencia al grafo.
         */
        Builder addDependency(SpiComponentNode source, SpiComponentNode target,
                             InjectionPoint.Type injectionType,
                             InjectionPoint.Dependency dependency,
                             Element injectionElement) {
            SpiDependencyEdge edge = new SpiDependencyEdge(
                source, target, injectionType, dependency, injectionElement);
            source.addDependency(edge);
            target.addInjector(edge);
            allDependencies.add(edge);
            return this;
        }
        
        /**
         * Construye el grafo inmutable.
         */
        SpiVeldGraph build() {
            List<ComponentNode> componentList = new ArrayList<>(nodesByName.values());
            return new SpiVeldGraph(nodesByName, componentList, allDependencies);
        }
    }
}
