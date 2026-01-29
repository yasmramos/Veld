package io.github.yasmramos.veld.aop.spi;

import io.github.yasmramos.veld.aop.AopClassGenerator;
import io.github.yasmramos.veld.aop.AopComponentNode;
import io.github.yasmramos.veld.aop.AopExtension;
import io.github.yasmramos.veld.aop.AopGenerationContext;
import io.github.yasmramos.veld.aop.AopGenerator;
import io.github.yasmramos.veld.spi.extension.ExtensionPhase;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Ejecutor de extensiones AOP que orquesta la generación de código AOP.
 * 
 * <p>Esta clase es responsable de:</p>
 * <ul>
 *   <li>Ejecutar extensiones AOP en el orden correcto según su fase</li>
 *   <li>Integrar la generación default (AopGenerator) con extensiones custom</li>
 *   <li>Manejar errores para que no afecten la compilación core</li>
 *   <li>Proveer el contexto de generación a cada extensión</li>
 * </ul>
 * 
 * <p>Este ejecutor está en {@code veld-aop} y usa {@link AopClassGenerator}
 * como implementación por defecto de {@link AopGenerator}.</p>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public final class SpiAopExtensionExecutor {
    
    private final SpiAopExtensionLoader extensionLoader;
    private final AopGenerator aopGenerator;
    private final boolean enabled;
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;
    private final javax.annotation.processing.Filer filer;
    private final List<String> executionErrors;
    
    /**
     * Crea un nuevo executor de extensiones AOP.
     * 
     * <p>Este constructor usa directamente {@link AopClassGenerator} como
     * implementación de {@link AopGenerator}. Ambas clases están en el mismo
     * módulo (veld-aop), evitando problemas de classloader que ocurrirían
     * con {@link ServiceLoader} en annotation processing.</p>
     * 
     * <p>Para verdadera extensibilidad SPI, las implementaciones adicionales
     * de {@link AopGenerator} pueden ser registradas en META-INF/services
     * y serán descubiertas en runtime (no en annotation processing).</p>
     * 
     * @param enabled si el sistema AOP está habilitado
     * @param messager el messager para reportes
     * @param elementUtils utilities de elementos
     * @param typeUtils utilities de tipos
     * @param filer el filer para escribir archivos
     */
    public SpiAopExtensionExecutor(
            boolean enabled,
            Messager messager,
            Elements elementUtils,
            Types typeUtils,
            javax.annotation.processing.Filer filer) {
        this.enabled = enabled;
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
        this.filer = filer;
        this.extensionLoader = enabled ? SpiAopExtensionLoader.loadExtensions() : null;
        this.executionErrors = new ArrayList<>();
        
        // Directly instantiate AopGenerator (both in veld-aop, no classloader issues)
        this.aopGenerator = enabled ? new AopClassGenerator() : null;
        
        if (enabled && extensionLoader.hasExtensions()) {
            reportNote("AOP Extensions loaded: " + extensionLoader.getExtensionCount());
        }
        
        if (aopGenerator != null) {
            reportNote("AopGenerator discovered via SPI: " + aopGenerator.getClass().getName());
        } else if (enabled) {
            reportWarning("No AopGenerator available via SPI, skipping AOP generation");
        }
    }
    
    /**
     * Convierte una lista de datos de componentes a una lista de AopComponentNode.
     * 
     * @param componentsData los datos de componentes (mapa de className -> datos)
     * @return lista de nodos AOP adaptados
     */
    private List<AopComponentNode> toAopComponentNodes(
            Map<String, ComponentData> componentsData) {
        
        List<AopComponentNode> nodes = new ArrayList<>();
        for (ComponentData data : componentsData.values()) {
            nodes.add(new AopComponentNodeImpl(data));
        }
        return nodes;
    }
    
    /**
     * Genera wrappers AOP para los componentes dados.
     * 
     * @param componentsData los datos de componentes a procesar
     * @return mapa de nombre de clase original -> nombre de clase wrapper AOP
     */
    public Map<String, String> generateAopClasses(Map<String, ComponentData> componentsData) {
        if (componentsData == null || componentsData.isEmpty()) {
            return Map.of();
        }
        
        // Crear contexto de generación
        AopGenerationContext context = AopGenerationContext.create(
            messager, elementUtils, typeUtils, filer);
        
        // Convertir a nodos AOP para las extensiones
        List<AopComponentNode> aopComponents = toAopComponentNodes(componentsData);
        
        // Fase INIT
        executePhase(ExtensionPhase.INIT, aopComponents, context);
        
        // beforeAopGeneration para todas las extensiones
        executeBeforeAopGeneration(aopComponents, context);
        
        Map<String, String> result;
        
        // Verificar si hay extensión que quiere sobrescribir la generación
        Optional<AopExtension> overridingExtension = extensionLoader != null 
            ? extensionLoader.findOverridingExtension() 
            : Optional.empty();
        
        if (overridingExtension.isPresent()) {
            // Usar generación de la extensión override
            reportNote("Using overriding AOP extension: " + 
                getExtensionId(overridingExtension.get()));
            result = executeOverridingGeneration(
                overridingExtension.get(), aopComponents, context, componentsData);
        } else if (enabled && extensionLoader != null && extensionLoader.hasExtensions() && aopGenerator != null) {
            // Usar generación default + extensiones
            reportNote("Using default AOP generation with " + 
                extensionLoader.getExtensionCount() + " extension(s)");
            result = executeDefaultGenerationWithExtensions(aopComponents, context, componentsData);
        } else if (aopGenerator != null) {
            // Solo generación default
            result = executeDefaultGeneration(componentsData, context);
        } else {
            // No hay generador disponible
            reportWarning("No AopGenerator available, skipping AOP generation");
            result = Map.of();
        }
        
        // afterAopGeneration para todas las extensiones
        executeAfterAopGeneration(result, context, aopComponents);
        
        return result;
    }
    
    private void executePhase(ExtensionPhase phase, List<AopComponentNode> components,
                              AopGenerationContext context) {
        if (extensionLoader == null) return;
        
        List<AopExtension> extensions = extensionLoader.getExtensionsForPhase(phase);
        for (AopExtension extension : extensions) {
            try {
                // Las extensiones AOP en INIT solo tienen beforeAopGeneration
                extension.beforeAopGeneration(components, context);
            } catch (Exception e) {
                recordError("AOP Extension", extension, phase.name(), e, context);
            }
        }
    }
    
    private void executeBeforeAopGeneration(List<AopComponentNode> components,
                                             AopGenerationContext context) {
        if (extensionLoader == null) return;
        
        for (AopExtension extension : extensionLoader.getExtensions()) {
            try {
                extension.beforeAopGeneration(components, context);
            } catch (Exception e) {
                recordError("AOP Extension", extension, "beforeAopGeneration", e, context);
            }
        }
    }
    
    private Map<String, String> executeOverridingGeneration(
            AopExtension extension,
            List<AopComponentNode> components,
            AopGenerationContext context,
            Map<String, ComponentData> componentsData) {
        try {
            return extension.generateAopWrappers(components, context);
        } catch (Exception e) {
            recordError("AOP Extension (override)", extension, "generateAopWrappers", e, context);
            // Fallback a generación default en caso de error
            return aopGenerator != null 
                ? aopGenerator.generateAopWrappers(components, context)
                : Map.of();
        }
    }
    
    private Map<String, String> executeDefaultGenerationWithExtensions(
            List<AopComponentNode> components,
            AopGenerationContext context,
            Map<String, ComponentData> componentsData) {
        if (aopGenerator == null) {
            return Map.of();
        }
        
        // Primero ejecutar generación default
        Map<String, String> result = aopGenerator.generateAopWrappers(components, context);
        
        // Luego permitir a extensiones agregar wrappers adicionales
        for (AopExtension extension : extensionLoader.getExtensions()) {
            if (!extension.overridesDefaultGeneration()) {
                try {
                    Map<String, String> additionalWrappers = 
                        extension.generateAopWrappers(components, context);
                    // Mergear resultados (extensiones pueden agregar, no sobrescribir)
                    for (Map.Entry<String, String> entry : additionalWrappers.entrySet()) {
                        if (!result.containsKey(entry.getKey())) {
                            result.put(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Exception e) {
                    recordError("AOP Extension", extension, "generateAopWrappers", e, context);
                }
            }
        }
        
        return result;
    }
    
    private Map<String, String> executeDefaultGeneration(
            Map<String, ComponentData> componentsData,
            AopGenerationContext context) {
        if (aopGenerator == null) {
            return Map.of();
        }
        
        try {
            List<AopComponentNode> components = toAopComponentNodes(componentsData);
            return aopGenerator.generateAopWrappers(components, context);
        } catch (Exception e) {
            context.reportError("Failed to generate AOP classes: " + e.getMessage(), null);
            executionErrors.add("AopGenerator failed: " + e.getMessage());
            return Map.of();
        }
    }
    
    private void executeAfterAopGeneration(Map<String, String> generatedWrappers,
                                            AopGenerationContext context,
                                            List<AopComponentNode> components) {
        if (extensionLoader == null) return;
        
        for (AopExtension extension : extensionLoader.getExtensions()) {
            try {
                extension.afterAopGeneration(generatedWrappers, context);
            } catch (Exception e) {
                recordError("AOP Extension", extension, "afterAopGeneration", e, context);
            }
        }
    }
    
    private void recordError(String type, AopExtension extension, String method, 
                             Exception e, AopGenerationContext context) {
        String errorMsg = type + " '" + getExtensionId(extension) + 
                          "' failed in " + method + ": " + e.getMessage();
        executionErrors.add(errorMsg);
        if (context != null) {
            context.reportWarning("Extension error: " + e.getMessage(), null);
        } else {
            messager.printMessage(Diagnostic.Kind.WARNING, "[Veld AOP] Extension error: " + e.getMessage());
        }
    }
    
    private String getExtensionId(AopExtension extension) {
        try {
            return extension.getDescriptor().getExtensionId();
        } catch (Exception e) {
            return extension.getClass().getName();
        }
    }
    
    private void reportNote(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[Veld AOP] " + message);
    }
    
    private void reportWarning(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, "[Veld AOP] " + message);
    }
    
    /**
     * Verifica si hay extensiones AOP cargadas.
     */
    public boolean hasExtensions() {
        return enabled && extensionLoader != null && extensionLoader.hasExtensions();
    }
    
    /**
     * Returns el número de extensiones cargadas.
     */
    public int getExtensionCount() {
        return enabled && extensionLoader != null ? extensionLoader.getExtensionCount() : 0;
    }
    
    /**
     * Returns información de las extensiones cargadas.
     */
    public String getExtensionsInfo() {
        if (!enabled || extensionLoader == null) {
            return "AOP Extensions disabled";
        }
        return extensionLoader.getExtensionsInfo();
    }
    
    /**
     * Returns errores de ejecución.
     */
    public List<String> getExecutionErrors() {
        return Collections.unmodifiableList(executionErrors);
    }
    
    /**
     * Indica si hubo errores durante la ejecución.
     */
    public boolean hasExecutionErrors() {
        return !executionErrors.isEmpty();
    }
    
    /**
     * Datos de componente para pasar entre processor y AOP.
     * 
     * <p>Esta clase contiene solo datos serializables, sin dependencias del processor.</p>
     */
    public static class ComponentData {
        private final String className;
        private final String internalName;
        private final List<String> interceptors;
        private final javax.lang.model.type.TypeMirror typeMirror;
        
        public ComponentData(
                String className,
                String internalName,
                List<String> interceptors,
                javax.lang.model.type.TypeMirror typeMirror) {
            this.className = className;
            this.internalName = internalName;
            this.interceptors = interceptors;
            this.typeMirror = typeMirror;
        }
        
        public String getClassName() { return className; }
        public String getInternalName() { return internalName; }
        public List<String> getInterceptors() { return interceptors; }
        public javax.lang.model.type.TypeMirror getTypeMirror() { return typeMirror; }
    }
}
