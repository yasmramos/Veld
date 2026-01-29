/**
 * Paquete principal del Service Provider Interface de Veld.
 * 
 * <p>Este paquete contiene todas las interfaces y clases necesarias para crear
 * extensiones que observen y reaccionen al proceso de compilación de Veld.</p>
 * 
 * <p><strong>Uso básico:</strong></p>
 * <ol>
 *   <li>Implementar la interfaz {@link com.veld.spi.extension.VeldExtension}</li>
 *   <li>Definir el descriptor con {@link com.veld.spi.extension.ExtensionDescriptor}</li>
 *   <li>Seleccionar la fase de ejecución con {@link com.veld.spi.extension.ExtensionPhase}</li>
 *   <li>Registrar la extensión mediante ServiceLoader</li>
 * </ol>
 * 
 * <p><strong>Ejemplo completo:</strong></p>
 * <pre>{@code
 * package com.example.veld.extension;
 * 
 * import com.veld.spi.extension.*;
 * 
 * public class LoggingExtension implements VeldExtension {
 *     @Override
 *     public ExtensionDescriptor getDescriptor() {
 *         return new ExtensionDescriptor(
 *             "com.example/logging-extension",
 *             ExtensionPhase.VALIDATION,
 *             100
 *         );
 *     }
 *     
 *     @Override
 *     public void execute(VeldGraph graph, VeldProcessingContext context) {
 *         context.reportNote("Detectados " + graph.getComponentCount() + 
 *             " componentes", null);
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Registro con ServiceLoader:</strong></p>
 * <p>Crear el archivo {@code META-INF/services/com.veld.spi.extension.VeldExtension}
 * con el nombre completo de la implementación:</p>
 * <pre>com.example.veld.extension.LoggingExtension</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
package io.github.yasmramos.veld.spi.extension;
