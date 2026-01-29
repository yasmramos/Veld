package io.github.yasmramos.veld.spi.extension;

import io.github.yasmramos.veld.annotation.Component;

/**
 * Representa el contrato base que todas las extensiones de Veld deben implementar.
 * 
 * <p>Una extensión de Veld es un componente que puede observar el grafo de dependencias
 * durante el proceso de compilación y realizar acciones como validaciones personalizadas,
 * generación de código auxiliar o análisis del grafo.</p>
 * 
 * <p><strong>Principios fundamentales:</strong></p>
 * <ul>
 *   <li>Las extensiones operan exclusivamente en tiempo de compilación</li>
 *   <li>Las extensiones reciben una vista de solo lectura del grafo</li>
 *   <li>Las extensiones no pueden modificar el grafo de dependencias</li>
 *   <li>Las extensiones se ejecutan en un orden determinista</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * public class MyExtension implements VeldExtension {
 *     @Override
 *     public ExtensionDescriptor getDescriptor() {
 *         return new ExtensionDescriptor(
 *             "com.example/my-extension",
 *             ExtensionPhase.VALIDATION,
 *             100 // orden de ejecución
 *         );
 *     }
 *     
 *     @Override
 *     public void execute(VeldGraph graph, VeldProcessingContext context) {
 *         // Analizar el grafo y realizar acciones
 *         for (ComponentNode component : graph.getComponents()) {
 *             // Validar reglas específicas del dominio
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface VeldExtension {
    
    /**
     * Proporciona los metadatos de la extensión.
     * 
     * <p>Este método debe devolver un objeto {@link ExtensionDescriptor} que contiene
     * la información necesaria para que Veld pueda gestionar correctamente la extensión,
     * incluyendo su identificador único, la fase en la que debe ejecutarse y su orden
     * de ejecución.</p>
     * 
     * @return el descriptor de la extensión con los metadatos de configuración
     */
    ExtensionDescriptor getDescriptor();
    
    /**
     * Ejecuta la lógica de la extensión.
     * 
     * <p>Este método es invocado por VeldProcessor después de que el grafo de
     * dependencias ha sido completamente construido. La extensión puede realizar
     * validaciones, análisis, o generar código adicional basándose en la estructura
     * del grafo.</p>
     * 
     * <p><strong>Nota importante:</strong> Este método solo debe realizar operaciones
     * que no requieran interacción con elementos del grafo que aún no han sido
     * procesados. El grafo pasado a este método está completamente construido y es
     * inmutable.</p>
     * 
     * @param graph el grafo de dependencias inmutable
     * @param context el contexto de procesamiento con acceso a herramientas del processor
     */
    void execute(VeldGraph graph, VeldProcessingContext context);
}
