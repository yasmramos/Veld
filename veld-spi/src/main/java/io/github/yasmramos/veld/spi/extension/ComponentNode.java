package io.github.yasmramos.veld.spi.extension;

import io.github.yasmramos.veld.annotation.ScopeType;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Representa un nodo de componente en el grafo de dependencias de Veld.
 * 
 * <p>Un ComponentNode encapsula toda la información relevante sobre un componente
 * registrado en el sistema, incluyendo sus dependencias, su ámbito de inyección,
 * su elemento de origen y metadatos adicionales.</p>
 * 
 * <p><strong>Identidad del componente:</strong></p>
 * <p>Cada componente tiene un nombre cualificado único que lo identifica dentro
 * del grafo. Este nombre se deriva del paquete y nombre de clase del componente.</p>
 * 
 * <p><strong>Dependencias:</strong></p>
 * <p>Las dependencias de un componente son las aristas que salen del nodo,
 * representando las inyecciones que el componente necesita para funcionar.</p>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * // Obtener información del componente
 * String name = component.getQualifiedName();
 * String simpleName = component.getSimpleName();
 * ScopeType scope = component.getScope();
 * 
 // Acceder al elemento de origen para reportes
 * Element element = component.getElement();
 * 
 // Obtener dependencias
 * List<DependencyEdge> dependencies = component.getDependencies();
 * for (DependencyEdge dep : dependencies) {
 *     ComponentNode target = dep.getTarget();
 *     System.out.println("Inyecta: " + target.getQualifiedName());
 * }
 * 
 // Obtener inyectores (componentes que dependen de este)
 * List<DependencyEdge> injectors = component.getInjectors();
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface ComponentNode {
    
    /**
     * Returns el nombre cualificado del componente (paquete + clase).
     * 
     * @return el nombre cualificado completo
     */
    String getQualifiedName();
    
    /**
     * Returns el nombre simple de la clase del componente.
     * 
     * @return el nombre simple sin paquete
     */
    String getSimpleName();
    
    /**
     * Returns el paquete donde está definida la clase del componente.
     * 
     * @return el nombre del paquete
     */
    String getPackageName();
    
    /**
     * Returns el ámbito de inyección del componente.
     * 
     * <p>El ámbito determina el ciclo de vida y la política de reutilización
     * de las instancias del componente. Los ámbitos comunes incluyen SINGLETON,
     * PROTOTYPE y APPLICATION.</p>
     * 
     * @return el tipo de ámbito
     */
    ScopeType getScope();
    
    /**
     * Returns el elemento de origen del componente.
     * 
     * <p>Este elemento es el AST node de Javac correspondiente a la clase del
     * componente. Se puede utilizar para obtener información adicional sobre el
     * elemento, como su posición en el código fuente para reportes de errores.</p>
     * 
     * @return el elemento de origen
     */
    Element getElement();
    
    /**
     * Returns el tipo mirror del componente.
     * 
     * <p>El TypeMirror proporciona información de tipos en tiempo de compilación,
     * incluyendo interfaces implementadas, superclase y tipo genérico si aplica.</p>
     * 
     * @return el tipo mirror del componente
     */
    TypeMirror getTypeMirror();
    
    /**
     * Returns la lista de dependencias (aristas salientes) del componente.
     * 
     * <p>Cada elemento de la lista representa una inyección que el componente
     * necesita para funcionar. El orden en la lista refleja el orden en que
     * las dependencias fueron declaradas en el código fuente.</p>
     * 
     * @return lista de aristas de dependencia salientes
     */
    List<DependencyEdge> getDependencies();
    
    /**
     * Returns la lista de inyectores (componentes que dependen de este).
     * 
     * <p>Esta es la vista inversa del grafo: todos los componentes que tienen
     * una arista hacia este nodo. Útil para análisis de impacto o para encontrar
     * todos los consumidores de un servicio.</p>
     * 
     * @return lista de aristas de dependencia entrantes
     */
    List<DependencyEdge> getInjectors();
    
    /**
     * Returns el número de dependencias directas del componente.
     * 
     * @return cantidad de dependencias
     */
    int getDependencyCount();
    
    /**
     * Returns el número de inyectores del componente.
     * 
     * @return cantidad de inyectores
     */
    int getInjectorCount();
    
    /**
     * Verifica si este componente tiene una dependencia hacia otro componente.
     * 
     * @param targetName el nombre cualificado del componente objetivo
     * @return true si existe una dependencia directa hacia el objetivo
     */
    boolean hasDependency(String targetName);
    
    /**
     * Busca una dependencia específica por el nombre del componente objetivo.
     * 
     * @param targetName el nombre cualificado del componente objetivo
     * @return un Optional conteniendo la arista si existe
     */
    Optional<DependencyEdge> findDependency(String targetName);
    
    /**
     * Verifica si el componente es un singleton.
     * 
     * <p>Shortcut para verificar si getScope() == ScopeType.SINGLETON.</p>
     * 
     * @return true si el componente es singleton
     */
    boolean isSingleton();
    
    /**
     * Verifica si el componente tiene dependencias.
     * 
     * @return true si tiene al menos una dependencia
     */
    boolean hasDependencies();
    
    /**
     * Verifica si el componente tiene inyectores.
     * 
     * @return true si al menos otro componente depende de este
     */
    boolean hasInjectors();
    
    /**
     * Returns una representación textual del componente para depuración.
     * 
     * @return string con formato "ComponentNode{name=...}"
     */
    @Override
    String toString();
}
