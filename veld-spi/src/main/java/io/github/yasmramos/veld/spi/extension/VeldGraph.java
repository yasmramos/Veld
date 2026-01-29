package io.github.yasmramos.veld.spi.extension;

import io.github.yasmramos.veld.annotation.Component;

import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Vista inmutable del grafo de dependencias de Veld.
 * 
 * <p>Esta interfaz proporciona acceso de solo lectura a toda la información sobre
 * los componentes registrados en el proyecto y sus relaciones de dependencia.
 * El grafo es inmutable por diseño: las extensiones pueden observarlo y analizarlo,
 * pero nunca modificarlo.</p>
 * 
 * <p><strong>Características del grafo:</strong></p>
 * <ul>
 *   <li><strong>Completo:</strong> Contiene todos los componentes descubiertos</li>
 *   <li><strong>Inmutable:</strong> No se puede modificar después de construido</li>
 *   <li><strong>Conexo:</strong> Incluye todas las aristas de dependencia</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * public void execute(VeldGraph graph, VeldProcessingContext context) {
 *     // Obtener todos los componentes
 *     Collection<ComponentNode> components = graph.getComponents();
 *     
 *     // Buscar un componente específico
 *     Optional<ComponentNode> myComponent = graph.findComponent("com.example/MyComponent");
 *     
 *     // Obtener dependencias de un componente
 *     myComponent.ifPresent(component -> {
 *         Collection<DependencyEdge> deps = component.getDependencies();
 *         for (DependencyEdge dep : deps) {
 *             ComponentNode target = dep.getTarget();
 *             System.out.println("Depende de: " + target.getQualifiedName());
 *         }
 *     });
 *     
 *     // Verificar ciclos
 *     Set<ComponentNode> cycle = graph.findCycle(myComponent.get());
 *     if (!cycle.isEmpty()) {
 *         context.reportError("Ciclo detectado", component.getElement());
 *     }
 * }
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface VeldGraph {
    
    /**
     * Returns una colección con todos los componentes registrados en el grafo.
     * 
     * <p>La colección devuelta es una vista del grafo y no puede ser modificada.
     * Los componentes están ordenados por su nombre cualificado para facilitar
     * la reproducibilidad de los resultados.</p>
     * 
     * @return colección inmutable de todos los componentes
     */
    Collection<ComponentNode> getComponents();
    
    /**
     * Returns el número total de componentes en el grafo.
     * 
     * @return cantidad de componentes
     */
    int getComponentCount();
    
    /**
     * Busca un componente por su nombre cualificado.
     * 
     * <p>Esta operación permite localizar rápidamente un componente específico
     * sin necesidad de iterar sobre toda la colección.</p>
     * 
     * @param qualifiedName el nombre cualificado del componente (paquete + clase)
     * @return un Optional conteniendo el componente si existe, o vacío si no
     */
    Optional<ComponentNode> findComponent(String qualifiedName);
    
    /**
     * Busca todos los componentes que implementan o extienden un tipo dado.
     * 
     * <p>Esta operación es útil para encontrar todos los componentes que son
     * subtipos de una interfaz o clase base específica, lo que permite implementar
     * patrones de descubrimiento basados en tipos.</p>
     * 
     * @param typeName el nombre cualificado del tipo a buscar
     * @return colección de componentes que extienden o implementan el tipo
     */
    Collection<ComponentNode> findComponentsBySuperType(String typeName);
    
    /**
     * Busca todos los componentes anotados con una anotación específica.
     * 
     * <p>Esta operación permite encontrar componentes basados en las anotaciones
     * que portan, facilitando el descubrimiento de componentes por características
     * o metadata.</p>
     * 
     * @param annotationName el nombre cualificado de la anotación
     * @return colección de componentes con la anotación
     */
    Collection<ComponentNode> findComponentsWithAnnotation(String annotationName);
    
    /**
     * Verifica si existe un ciclo de dependencias comenzando desde un componente.
     * 
     * <p>Esta operación realiza una búsqueda en profundidad para detectar si
     * el componente dado participa en un ciclo de dependencias. Los ciclos son
     * problemáticos porque pueden causar stack overflow o comportamiento indefinido
     * en tiempo de ejecución.</p>
     * 
     * @param start el componente desde el cual comenzar la búsqueda
     * @return un conjunto con todos los componentes que forman parte del ciclo,
     *         o un conjunto vacío si no hay ciclo
     */
    Set<ComponentNode> findCycle(ComponentNode start);
    
    /**
     * Returns todas las dependencias del sistema como aristas.
     * 
     * <p>Esta operación devuelve una vista completa de todas las relaciones de
     * inyección en el sistema, lo que puede ser útil para análisis globales o
     * generación de documentación.</p>
     * 
     * @return colección de todas las aristas de dependencia
     */
    Collection<DependencyEdge> getAllDependencies();
    
    /**
     * Verifica si el grafo está vacío.
     * 
     * @return true si no hay componentes registrados
     */
    boolean isEmpty();
}
