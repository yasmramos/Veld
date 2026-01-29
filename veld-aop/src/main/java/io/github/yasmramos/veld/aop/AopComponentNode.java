package io.github.yasmramos.veld.aop;

import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Nodo de componente especializado para procesamiento AOP.
 * 
 * <p>Esta interfaz proporciona acceso a la información necesaria para la generación
 * de wrappers AOP. Se diseñó como una abstracción ligera que no depende de clases
 * internas del processor, permitiendo que las extensiones AOP definidas en veld-aop
 * no creen dependencias circulares.</p>
 * 
 * <p><strong>Información proporcionada:</strong></p>
 * <ul>
 *   <li>Identidad del componente (nombre de clase, nombre interno ASM)</li>
 *   <li>Interceptores AOP asociados</li>
 *   <li>Referencia al TypeMirror para manipulación de tipos</li>
 * </ul>
 * 
 * <p><strong>Implementación:</strong></p>
 * <p>La implementación concreta es {@code AopComponentNodeImpl} en veld-aop,
 * que adapta el {@code ComponentInfo} interno a esta interfaz SPI.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface AopComponentNode {
    
    /**
     * Returns el nombre cualificado de la clase del componente.
     * 
     * @return nombre cualificado (ej: "com.example.MyService")
     */
    String getClassName();
    
    /**
     * Returns el nombre interno del componente en formato ASM.
     * 
     * <p>Este formato utiliza barras en lugar de puntos, necesario para
     * manipulación de bytecode con ASM.</p>
     * 
     * @return nombre interno ASM (ej: "com/example/MyService")
     */
    String getInternalName();
    
    /**
     * Returns el nombre simple de la clase del componente.
     * 
     * @return nombre simple sin paquete (ej: "MyService")
     */
    String getSimpleName();
    
    /**
     * Returns el nombre del paquete del componente.
     * 
     * @return nombre del paquete (ej: "com.example"), o cadena vacía si está en paquete por defecto
     */
    String getPackageName();
    
    /**
     * Returns la lista de interceptores AOP asociados a este componente.
     * 
     * <p>Cada cadena representa el nombre cualificado de una clase interceptor.
     * La lista está ordenada según el orden de declaración en las anotaciones.</p>
     * 
     * @return lista de nombres de clases interceptor
     */
    List<String> getInterceptors();
    
    /**
     * Verifica si este componente tiene interceptores AOP.
     * 
     * @return true si tiene al menos un interceptor
     */
    boolean hasInterceptors();
    
    /**
     * Returns el TypeMirror del componente.
     * 
     * <p>Proporciona acceso a la información de tipos en tiempo de compilación,
     * incluyendo interfaces implementadas, superclase y tipos genéricos.</p>
     * 
     * @return el TypeMirror del componente
     */
    TypeMirror getTypeMirror();
    
    /**
     * Returns una representación textual del nodo para depuración.
     * 
     * @return string con formato "AopComponentNode{className=...}"
     */
    @Override
    String toString();
}
