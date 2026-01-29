package io.github.yasmramos.veld.spi.extension;

import io.github.yasmramos.veld.annotation.Component;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

/**
 * Representa una arista de dependencia entre dos componentes en el grafo de Veld.
 * 
 * <p>Una DependencyEdge captura la relación de inyección de un componente (origen)
 * hacia otro componente (objetivo). Cada arista contiene información sobre el tipo
 * de inyección, el punto de inyección específico y metadatos adicionales.</p>
 * 
 * <p><strong>Dirección de la arista:</strong></p>
 * <p>Las aristas van desde el componente que necesita la dependencia hacia el
 * componente que la proporciona. El componente origen depende del componente objetivo.</p>
 * 
 * <p><strong>Tipos de inyección:</strong></p>
 * <p>La arista distingue entre diferentes tipos de relaciones de inyección:</p>
 * <ul>
 *   <li><strong>FIELD:</strong> Inyección directamente en un campo</li>
 *   <li><strong>CONSTRUCTOR:</strong> Inyección como parámetro del constructor</li>
 *   <li><strong>METHOD:</strong> Inyección como parámetro de un método setter</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * public void analyzeDependencies(VeldGraph graph) {
 *     for (ComponentNode component : graph.getComponents()) {
 *         for (DependencyEdge edge : component.getDependencies()) {
 *             ComponentNode target = edge.getTarget();
 *             ComponentNode source = edge.getSource();
 *             
 *             System.out.printf("%s -> %s (tipo: %s, campo: %s)%n",
 *                 source.getSimpleName(),
 *                 target.getSimpleName(),
 *                 edge.getInjectionType(),
 *                 edge.getFieldName().orElse("N/A")
 *             );
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface DependencyEdge {
    
    /**
     * Returns el componente origen de la dependencia (el que necesita).
     * 
     * @return el nodo de origen
     */
    ComponentNode getSource();
    
    /**
     * Returns el componente objetivo de la dependencia (el que proporciona).
     * 
     * @return el nodo objetivo
     */
    ComponentNode getTarget();
    
    /**
     * Returns el tipo de inyección para esta dependencia.
     * 
     * <p>El tipo indica cómo se realiza la inyección: directamente en un campo,
     * como parámetro del constructor, o a través de un método setter.</p>
     * 
     * @return el tipo de inyección
     */
    InjectionType getInjectionType();
    
    /**
     * Returns el elemento del campo o método donde se realiza la inyección.
     * 
     * <p>Para inyecciones FIELD, devuelve el elemento del campo. Para inyecciones
     * METHOD, devuelve el elemento del método setter. Para inyecciones CONSTRUCTOR,
     * devuelve el elemento del parámetro del constructor.</p>
     * 
     * @return el elemento de inyección
     */
    Element getInjectionElement();
    
    /**
     * Returns el nombre del campo si la inyección es de tipo FIELD.
     * 
     * @return un Optional conteniendo el nombre del campo, o vacío si no aplica
     */
    java.util.Optional<String> getFieldName();
    
    /**
     * Returns el nombre del método si la inyección es de tipo METHOD.
     * 
     * @return un Optional conteniendo el nombre del método, o vacío si no aplica
     */
    java.util.Optional<String> getMethodName();
    
    /**
     * Returns el tipo de dependencia desde la perspectiva del objetivo.
     * 
     * <p>Indica si el objetivo es el componente principal, una dependencia opcional,
     * o una colección de dependencias del mismo tipo.</p>
     * 
     * @return el tipo de relación
     */
    DependencyKind getDependencyKind();
    
    /**
     * Returns el tipo mirror del punto de inyección.
     * 
     * <p>Proporciona el tipo declarado en el punto de inyección, que puede diferir
     * del tipo del componente objetivo si hay conversiones o tipos genéricos.</p>
     * 
     * @return el tipo mirror del punto de inyección
     */
    TypeMirror getInjectionTypeMirror();
    
    /**
     * Verifica si esta dependencia es requerida (no opcional).
     * 
     * @return true si la dependencia es obligatoria
     */
    boolean isRequired();
    
    /**
     * Verifica si esta dependencia es una colección.
     * 
     * @return true si es una inyección de tipo List/Set/Collection
     */
    boolean isCollection();
    
    /**
     * Returns el tipo de cualificador si existe.
     * 
     * <p>Las inyecciones pueden tener qualificadores que distinguen entre múltiples
     * implementaciones del mismo tipo de interfaz.</p>
     * 
     * @return un Optional con el nombre del cualificador, o vacío si no existe
     */
    java.util.Optional<String> getQualifierName();
    
    /**
     * Returns una representación textual de la arista para depuración.
     * 
     * @return string con formato "DependencyEdge{source -> target [type]}"
     */
    @Override
    String toString();
    
    /**
     * Enumera los tipos posibles de inyección.
     */
    enum InjectionType {
        /**
         * Inyección directa en un campo.
         */
        FIELD,
        
        /**
         * Inyección como parámetro del constructor.
         */
        CONSTRUCTOR,
        
        /**
         * Inyección como parámetro de un método setter.
         */
        METHOD
    }
    
    /**
     * Enumera los tipos de relación de dependencia.
     */
    enum DependencyKind {
        /**
         * El objetivo es el componente principal y único.
         */
        PRIMARY,
        
        /**
         * El objetivo es una dependencia normal.
         */
        NORMAL,
        
        /**
         * El objetivo es opcional (puede ser null).
         */
        OPTIONAL,
        
        /**
         * El objetivo es parte de una colección.
         */
        COLLECTION_ELEMENT,
        
        /**
         * El objetivo es un proveedor de instancias.
         */
        PROVIDER
    }
}
