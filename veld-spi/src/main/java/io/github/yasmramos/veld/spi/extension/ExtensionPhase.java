package io.github.yasmramos.veld.spi.extension;

/**
 * Define las fases en las que una extensión puede ejecutarse durante el procesamiento
 * de anotaciones de Veld.
 * 
 * <p>Las fases determinan cuándo se ejecutará una extensión en relación con otras
 * operaciones del processor. Esto permite a las extensiones cooperar de manera
 * ordenada y predecible.</p>
 * 
 * <p><strong>Flujo de procesamiento:</strong></p>
 * <ol>
 *   <li><strong>INIT:</strong> Primera fase, ejecutada antes que cualquier otra.
 *       Útil para inicialización o registro de métricas.</li>
 *   <li><strong>VALIDATION:</strong> Después de INIT. Las extensiones pueden validar
 *       el grafo y reportar errores o advertencias.</li>
 *   <li><strong>ANALYSIS:</strong> Después de VALIDATION. Para análisis profundo del
 *       grafo que requiere que todas las validaciones hayan completado.</li>
 *   <li><strong>GENERATION:</strong> Fase final. Las extensiones pueden generar código
 *       auxiliar que se incluirá en la compilación.</li>
 * </ol>
 * 
 * <p><strong>Nota sobre el orden:</strong></p>
 * <p>Dentro de cada fase, las extensiones se ordenan por su valor de orden ascendente.
 * Si necesita ejecutar código después de otra extensión específica, asegúrese de que
 * su valor de orden sea mayor.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public enum ExtensionPhase {
    
    /**
     * Fase de inicialización.
     * 
     * <p>Primera fase en ejecutarse. Útil para extensiones que necesitan realizar
     * operaciones de setup o registro antes de que cualquier validación o análisis
     * comience. Esta fase se ejecuta una sola vez al inicio del procesamiento.</p>
     * 
     * <p>Valor numérico interno: 0</p>
     */
    INIT(0),
    
    /**
     * Fase de validación.
     * 
     * <p>Segunda fase del procesamiento. Las extensiones pueden inspeccionar el grafo
     * de dependencias y reportar errores si encuentran violaciones de reglas de negocio
     * o configuraciones incorrectas.</p>
     * 
     * <p>Valor numérico interno: 1000</p>
     */
    VALIDATION(1000),
    
    /**
     * Fase de análisis.
     * 
     * <p>Tercera fase. Ejecutada después de que todas las validaciones hayan completado.
     * Útil para extensiones que necesitan realizar análisis complejos o generar métricas
     * que dependen de que todas las validaciones sean exitosas.</p>
     * 
     * <p>Valor numérico interno: 2000</p>
     */
    ANALYSIS(2000),
    
    /**
     * Fase de generación.
     * 
     * <p>Última fase del procesamiento. Las extensiones pueden generar código adicional
     * que se incluirá en la compilación. El código generado se escribe a través del
     * {@link VeldProcessingContext#createSourceFile(String)} y se compila junto con
     * el resto del proyecto.</p>
     * 
     * <p>Valor numérico interno: 3000</p>
     */
    GENERATION(3000);
    
    private final int internalOrder;
    
    ExtensionPhase(int internalOrder) {
        this.internalOrder = internalOrder;
    }
    
    /**
     * Returns el valor numérico interno de esta fase.
     * 
     * <p>Este valor se utiliza internamente para ordenar las fases cuando se procesan
     * colecciones que pueden contener elementos de diferentes fases. No debe utilizarse
     * directamente en el código de usuario.</p>
     * 
     * @return el valor numérico de la fase
     */
    int getInternalOrder() {
        return internalOrder;
    }
}
