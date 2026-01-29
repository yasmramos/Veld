package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.aop.spi.SpiAopExtensionExecutor;
import io.github.yasmramos.veld.processor.ComponentInfo;

import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad para convertir datos del processor a formatos SPI.
 * 
 * <p>Esta clase proporciona métodos para convertir los datos internos del processor
 * (ComponentInfo) a formatos que pueden ser usados por el sistema SPI en veld-aop
 * sin crear dependencias directas.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public final class ProcessorToSpiConverter {
    
    private ProcessorToSpiConverter() {
        // Utility class
    }
    
    /**
     * Convierte una lista de ComponentInfo a un mapa de ComponentData.
     * 
     * @param components la lista de componentes a convertir
     * @return mapa de nombre de clase -> datos de componente
     */
    public static Map<String, SpiAopExtensionExecutor.ComponentData> toComponentDataMap(
            List<ComponentInfo> components) {
        
        if (components == null || components.isEmpty()) {
            return Map.of();
        }
        
        Map<String, SpiAopExtensionExecutor.ComponentData> result = new LinkedHashMap<>();
        
        for (ComponentInfo component : components) {
            result.put(
                component.getClassName(),
                createComponentData(component)
            );
        }
        
        return result;
    }
    
    /**
     * Crea un ComponentData a partir de un ComponentInfo.
     * 
     * @param component el componente a convertir
     * @return los datos del componente para SPI
     */
    public static SpiAopExtensionExecutor.ComponentData createComponentData(
            ComponentInfo component) {
        
        return new SpiAopExtensionExecutor.ComponentData(
            component.getClassName(),
            component.getInternalName(),
            copyInterceptors(component.getAopInterceptors()),
            component.getTypeElement() != null ? component.getTypeElement().asType() : null
        );
    }
    
    /**
     * Copia la lista de interceptores de manera segura.
     * El ComponentInfo puede mutar su lista, así que necesitamos una copia.
     */
    private static List<String> copyInterceptors(List<String> original) {
        if (original == null) {
            return List.of();
        }
        return new ArrayList<>(original);
    }
}
