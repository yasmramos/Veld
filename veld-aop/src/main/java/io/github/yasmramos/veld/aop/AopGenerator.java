package io.github.yasmramos.veld.aop;

import java.util.List;
import java.util.Map;

/**
 * Interfaz para la generación de clases wrapper AOP.
 * 
 * <p>Esta interfaz permite que el sistema SPI en {@code veld-aop} pueda invocar
 * la generación de clases AOP sin conocer los detalles de implementación del
 * processor.</p>
 * 
 * <p>El processor implementa esta interfaz y la registra via SPI, permitiendo
 * que veld-aop la descubra y utilice.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface AopGenerator {
    
    /**
     * Genera las clases wrapper AOP para los componentes dados.
     * 
     * @param components los nodos de componentes para generar wrappers
     * @param context el contexto de generación
     * @return mapa de nombre de clase original -> nombre de clase wrapper AOP
     */
    Map<String, String> generateAopWrappers(
            List<? extends AopComponentNode> components,
            AopGenerationContext context);
    
    /**
     * Verifica si este generador está disponible.
     * 
     * @return true si el generador puede ejecutar
     */
    default boolean isAvailable() {
        return true;
    }
}
