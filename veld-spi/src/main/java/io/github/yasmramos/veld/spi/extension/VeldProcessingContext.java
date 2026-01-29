package io.github.yasmramos.veld.spi.extension;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

/**
 * Contexto de procesamiento que proporciona acceso a las herramientas del annotation
 * processor y utilities adicionales para las extensiones.
 * 
 * <p>Este contexto es el puente entre las extensiones y las capacidades del
 * processor de Veld. Proporciona acceso al messager para reportar errores,
 * al filer para generar código, y a utilities de procesamiento de tipos.</p>
 * 
 * <p><strong>Gestión de errores:</strong></p>
 * <p>El contexto proporciona métodos convenientes para reportar errores, advertencias
 * y notas. Los mensajes se direccionan al compilador de Java y aparecen junto con
 * los mensajes de Veld, facilitando la integración.</p>
 * 
 * <p><strong>Generación de código:</strong></p>
 * <p>Las extensiones pueden generar código adicional a través del filer. El código
 * generado se compila junto con el resto del proyecto y se incluye en el JAR final.</p>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * public void execute(VeldGraph graph, VeldProcessingContext context) {
 *     // Reportar un error
 *     context.reportError("Componente sin dependencias válidas", 
 *         component.getElement());
 *     
 *     // Reportar una advertencia
 *     context.reportWarning("Considera usar @Singleton para componentes únicos",
 *         component.getElement());
 *     
 *     // Generar código
 *     try (CodeBuilder builder = context.createSourceFile(
 *             "com.example.generated.MetricsRegistry")) {
 *         builder.append("package com.example.generated;");
 *         builder.append("public class MetricsRegistry {");
 *         builder.append("    public static void register() { }");
 *         builder.append("}");
 *     } catch (IOException e) {
 *         context.reportError("No se pudo generar código", component.getElement());
 *     }
 *     
 *     // Acceder a utilities del processor
 *     Elements elementUtils = context.getElementUtils();
 *     Types typeUtils = context.getTypeUtils();
 * }
 * }</pre>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public interface VeldProcessingContext {
    
    /**
     * Reporta un error al compilador.
     * 
     * <p>Los errores detienen la compilación y deben utilizarse solo para
     * condiciones que impidan la generación correcta del código.</p>
     * 
     * @param message el mensaje de error
     * @param element el elemento donde ocurrió el error (puede ser null)
     */
    void reportError(String message, Element element);
    
    /**
     * Reporta un error con información adicional de diagnóstico.
     * 
     * @param message el mensaje de error
     * @param element el elemento donde ocurrió el error
     * @param kind el tipo de mensaje de diagnóstico
     */
    void reportError(String message, Element element, Diagnostic.Kind kind);
    
    /**
     * Reporta una advertencia al compilador.
     * 
     * <p>Las advertencias no detienen la compilación pero indican condiciones
     * que podrían ser problemáticas.</p>
     * 
     * @param message el mensaje de advertencia
     * @param element el elemento donde ocurrió la advertencia (puede ser null)
     */
    void reportWarning(String message, Element element);
    
    /**
     * Reporta una nota informacional al compilador.
     * 
     * <p>Las notas proporcionan información adicional sin indicar problemas.</p>
     * 
     * @param message el mensaje de nota
     * @param element el elemento asociado (puede ser null)
     */
    void reportNote(String message, Element element);
    
    /**
     * Crea un nuevo archivo fuente para generación de código.
     * 
     * <p>El nombre del archivo debe incluir el paquete completo. Por ejemplo,
     * "com/example/generated/MyClass.java". El archivo se creará en el directorio
     * de fuentes generados y se compilará automáticamente.</p>
     * 
     * <p><strong>Nota:</strong> El archivo debe ser cerrado explícitamente
     * después de escribir. Se recomienda usar try-with-resources.</p>
     * 
     * @param qualifiedFileName el nombre cualificado del archivo a crear
     * @return un builder para escribir el contenido del archivo
     * @throws java.io.IOException si no se puede crear el archivo
     */
    CodeBuilder createSourceFile(String qualifiedFileName) throws java.io.IOException;
    
    /**
     * Crea un nuevo archivo de recursos.
     * 
     * <p>Similar a createSourceFile pero para archivos de recursos (no Java).
     * El archivo se copiará al directorio de clases generado.</p>
     * 
     * @param resourcePath la ruta del recurso dentro del classpath
     * @return un PrintWriter para escribir el recurso
     * @throws java.io.IOException si no se puede crear el recurso
     */
    PrintWriter createResourceFile(String resourcePath) throws java.io.IOException;
    
    /**
     * Returns el utilitario de elementos del processor.
     * 
     * <p>Elements proporciona métodos para manipular elementos del código fuente,
     * incluyendo obtención de nombres, paquetes, y metadatos de anotaciones.</p>
     * 
     * @return el Elements utility
     */
    Elements getElementUtils();
    
    /**
     * Returns el utilitario de tipos del processor.
     * 
     * <p>Types proporciona métodos para manipular tipos de tiempo de compilación,
     * incluyendo comparaciones de tipos, casting y obtención de tipos primitivos.</p>
     * 
     * @return el Types utility
     */
    Types getTypeUtils();
    
    /**
     * Returns el entorno de redondeo actual.
     * 
     * <p>El RoundEnvironment proporciona acceso a los elementos anotados que fueron
     * procesados en el round actual. Útil para extensiones que necesitan iterar
     * sobre todos los elementos.</p>
     * 
     * @return el RoundEnvironment
     */
    javax.annotation.processing.RoundEnvironment getRoundEnvironment();
    
    /**
     * Verifica si el código debe generar trazas de depuración.
     * 
     * @return true si el modo de depuración está habilitado
     */
    boolean isDebugEnabled();
    
    /**
     * Returns el conjunto de opciones de procesamiento activas.
     * 
     * <p>Las opciones pueden incluir flags como "-Aveld.debug=true" o
     * "-Aveld.strict=true" que modifican el comportamiento del processor.</p>
     * 
     * @return conjunto de nombres de opciones
     */
    Set<String> getSupportedOptions();
    
    /**
     * Returns el valor de una opción de procesamiento específica.
     * 
     * @param optionName el nombre de la opción (sin el prefijo -A)
     * @return el valor de la opción, o null si no está definida
     */
    String getOption(String optionName);
    
    /**
     * Builder para generación de código fuente.
     * 
     * <p>Proporciona una interfazfluida para construir archivos Java de manera
     * programática, con soporte para indentación automática y manejo de paquetes.</p>
     */
    interface CodeBuilder extends AutoCloseable {
        
        /**
         * Append una línea de código.
         * 
         * @param line la línea de código a agregar
         * @return este builder para encadenamiento
         */
        CodeBuilder append(String line);
        
        /**
         * Append una línea con salto de línea automático.
         * 
         * @param line la línea de código
         * @return este builder
         */
        CodeBuilder appendLine(String line);
        
        /**
         * Append múltiples líneas.
         * 
         * @param lines las líneas a agregar
         * @return este builder
         */
        CodeBuilder appendLines(String... lines);
        
        /**
         * Agrega una declaración de paquete.
         * 
         * @param packageName el nombre del paquete
         * @return este builder
         */
        CodeBuilder packageDeclaration(String packageName);
        
        /**
         * Agrega una declaración de importación.
         * 
         * @param className el nombre completo de la clase a importar
         * @return este builder
         */
        CodeBuilder importDeclaration(String className);
        
        /**
         * Inicia un bloque de código (llaves).
         * 
         * @return este builder
         */
        CodeBuilder beginBlock();
        
        /**
         * Finaliza un bloque de código.
         * 
         * @return este builder
         */
        CodeBuilder endBlock();
        
        /**
         * Agrega una línea en blanco.
         * 
         * @return este builder
         */
        CodeBuilder blankLine();
        
        /**
         * Returns el código generado como string.
         * 
         * @return el contenido generado
         */
        String toString();
        
        /**
         * Cierra el builder y libera recursos.
         * 
         * <p>Debe ser llamado después de terminar de escribir para asegurar
         * que el archivo se escriba correctamente.</p>
         */
        @Override
        void close();
    }
}
