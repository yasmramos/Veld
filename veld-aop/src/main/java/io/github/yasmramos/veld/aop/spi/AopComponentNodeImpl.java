package io.github.yasmramos.veld.aop.spi;

import io.github.yasmramos.veld.aop.AopComponentNode;

import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Objects;

/**
 * Implementación del adapter que conecta el sistema SPI con los datos de componentes.
 * 
 * <p>Esta clase actúa como bridge entre:</p>
 * <ul>
 *   <li>El mundo interno de veld-processor (ComponentData)</li>
 *   <li>El mundo público del SPI (AopComponentNode)</li>
 * </ul>
 * 
 * <p>Gracias a este adapter, las extensiones AOP pueden trabajar con una abstracción
 * limpia sin conocer los detalles internos del processor.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
final class AopComponentNodeImpl implements AopComponentNode {
    
    private final SpiAopExtensionExecutor.ComponentData component;
    
    /**
     * Crea un nuevo adapter para el ComponentData dado.
     * 
     * @param component los datos del componente a adaptar
     */
    AopComponentNodeImpl(SpiAopExtensionExecutor.ComponentData component) {
        if (component == null) {
            throw new IllegalArgumentException("ComponentData cannot be null");
        }
        this.component = component;
    }
    
    @Override
    public String getClassName() {
        return component.getClassName();
    }
    
    @Override
    public String getInternalName() {
        return component.getInternalName();
    }
    
    @Override
    public String getSimpleName() {
        int lastDot = component.getClassName().lastIndexOf('.');
        return lastDot > 0 
            ? component.getClassName().substring(lastDot + 1) 
            : component.getClassName();
    }
    
    @Override
    public String getPackageName() {
        int lastDot = component.getClassName().lastIndexOf('.');
        return lastDot > 0 
            ? component.getClassName().substring(0, lastDot) 
            : "";
    }
    
    @Override
    public List<String> getInterceptors() {
        return component.getInterceptors();
    }
    
    @Override
    public boolean hasInterceptors() {
        return component.getInterceptors() != null && !component.getInterceptors().isEmpty();
    }
    
    @Override
    public TypeMirror getTypeMirror() {
        return component.getTypeMirror();
    }
    
    @Override
    public String toString() {
        return "AopComponentNodeImpl{" +
               "className='" + getClassName() + '\'' +
               ", simpleName='" + getSimpleName() + '\'' +
               ", packageName='" + getPackageName() + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AopComponentNodeImpl)) return false;
        AopComponentNodeImpl other = (AopComponentNodeImpl) obj;
        return getClassName().equals(other.getClassName());
    }
    
    @Override
    public int hashCode() {
        return getClassName().hashCode();
    }
}
