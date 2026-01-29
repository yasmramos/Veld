package io.github.yasmramos.veld.aop;

import io.github.yasmramos.veld.spi.extension.ExtensionDescriptor;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;
import io.github.yasmramos.veld.spi.extension.VeldExtension;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;

import java.util.List;
import java.util.Map;

/**
 * Clase base abstracta para extensiones AOP en Veld.
 * 
 * <p>Una extensión AOP puede observar y modificar cómo se genera el código AOP
 * para componentes con interceptores. Esto permite a los usuarios personalizar
 * la generación de wrappers AOP o agregar comportamiento adicional.</p>
 * 
 * <p><strong>Diferencia con VeldExtension:</strong></p>
 * <ul>
 *   <li>{@code VeldExtension} es para observadores genéricos del grafo de dependencias</li>
 *   <li>{@code AopExtension} es específica para la generación de código AOP</li>
 * </ul>
 * 
 * <p><strong>Implementación SPI:</strong></p>
 * <p>Para que una extensión AOP sea descubierta automáticamente, debe:</p>
 * <ol>
 *   <li>Extender esta clase {@code AopExtension}</li>
 *   <li>Implementar todos los métodos abstractos requeridos</li>
 *   <li>Registrarse en {@code META-INF/services/io.github.yasmramos.veld.aop.AopExtension}</li>
 * </ol>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * public class CustomAopExtension extends AopExtension {
 *     @Override
 *     public ExtensionDescriptor getDescriptor() {
 *         return new ExtensionDescriptor(
 *             "com.example/custom-aop",
 *             ExtensionPhase.GENERATION,
 *             100 // orden de ejecución
 *         );
 *     }
 *     
 *     @Override
 *     public void beforeAopGeneration(List<AopComponentNode> components, AopGenerationContext context) {
 *         // Modificar componentes antes de generar AOP
 *         for (AopComponentNode component : components) {
 *             // Agregar interceptores custom
 *         }
 *     }
 *     
 *     @Override
 *     public Map<String, String> generateAopWrappers(List<AopComponentNode> components, AopGenerationContext context) {
 *         // Generar wrappers AOP customizados
 *         return Map.of();
 *     }
 *     
 *     @Override
 *     public void afterAopGeneration(Map<String, String> generatedWrappers, AopGenerationContext context) {
 *         // Post-procesamiento después de generar AOP
 *     }
 * }
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public abstract class AopExtension implements VeldExtension {
    
    /**
     * Proporciona los metadatos de la extensión AOP.
     * 
     * <p>Las extensiones AOP típicamente se ejecutan en la fase GENERATION
     * para generar código auxiliar junto con el resto del processor.</p>
     * 
     * @return el descriptor de la extensión
     */
    @Override
    public abstract ExtensionDescriptor getDescriptor();
    
    /**
     * Se ejecuta antes de la generación de wrappers AOP.
     * 
     * <p>Útil para modificar la lista de componentes que necesitan AOP,
     * agregar interceptores personalizados, o realizar validaciones previas.</p>
     * 
     * @param components la lista de componentes descubiertos
     * @param context el contexto de generación AOP
     */
    public void beforeAopGeneration(List<AopComponentNode> components, AopGenerationContext context) {
        // Default: no-op
    }
    
    /**
     * Genera wrappers AOP para los componentes dados.
     * 
     * <p>Este método es llamado para permitir a las extensiones generar
     * código AOP adicional. El contexto proporciona acceso al filer para
     * escribir archivos fuente.</p>
     * 
     * <p><strong>Nota:</strong> Si una extensión retorna un mapa vacío, se usa
     * la generación default. Si retorna un mapa con entradas, esas entradas
     * se usarán en lugar de la generación default para esos componentes.</p>
     * 
     * @param components la lista de componentes que pueden necesitar wrappers AOP
     * @param context el contexto de generación AOP
     * @return mapa de nombre de clase original -> nombre de clase wrapper AOP
     */
    public Map<String, String> generateAopWrappers(List<AopComponentNode> components, AopGenerationContext context) {
        // Default: usar generación del processor (AopClassGenerator)
        return Map.of();
    }
    
    /**
     * Se ejecuta después de la generación de wrappers AOP.
     * 
     * <p>Útil para post-procesamiento, logging, o validación de wrappers generados.</p>
     * 
     * @param generatedWrappers mapa de wrappers generados (original -> wrapper)
     * @param context el contexto de generación AOP
     */
    public void afterAopGeneration(Map<String, String> generatedWrappers, AopGenerationContext context) {
        // Default: no-op
    }
    
    /**
     * Indica si esta extensión quiere generar wrappers AOP completamente customizados.
     *
     * <p>Si retorna {@code true}, {@link #generateAopWrappers} debe generar TODOS los wrappers
     * y la generación default (AopClassGenerator) será ignorada.</p>
     *
     * <p>Si retorna {@code false} (default), la generación default se ejecutará y las
     * extensiones pueden agregar comportamiento adicional.</p>
     *
     * @return true si quiere control total de la generación AOP
     */
    public boolean overridesDefaultGeneration() {
        return false;
    }

    /**
     * Implementación por defecto de {@link VeldExtension#execute}.
     *
     * <p>Para extensiones AOP, la lógica principal se maneja a través de los métodos
     * específicos del ciclo de vida AOP:
     * {@link #beforeAopGeneration}, {@link #generateAopWrappers}, y {@link #afterAopGeneration}.
     * Este método proporciona una implementación vacía que las extensiones AOP pueden
     * sobrescribir si necesitan acceso directo al grafo de dependencias.</p>
     *
     * @param graph el grafo de dependencias inmutable
     * @param context el contexto de procesamiento
     */
    @Override
    public void execute(VeldGraph graph, VeldProcessingContext context) {
        // Default: no-op para extensiones AOP
        // La lógica AOP se maneja en beforeAopGeneration, generateAopWrappers, afterAopGeneration
    }
}
