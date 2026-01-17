package io.github.yasmramos.veld.processor.spi;

import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.InjectionPoint;
import io.github.yasmramos.veld.spi.extension.*;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.StringWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Ejecutor de extensiones de Veld que orquesta la ejecución en las fases correctas.
 * 
 * <p>Esta clase es responsable de:</p>
 * <ul>
 *   <li>Construir el grafo de dependencias desde los componentes descubiertos</li>
 *   <li>Ejecutar las extensiones en el orden correcto según su fase</li>
 *   <li>Manejar errores de manera que no afecten el procesamiento del núcleo</li>
 *   <li>Proveer el contexto de procesamiento a cada extensión</li>
 * </ul>
 * 
 * <p>El flujo de ejecución sigue el patrón:</p>
 * <ol>
 *   <li>INIT: Extensions pueden inicializar recursos</li>
 *   <li>VALIDATION: Extensions pueden validar el grafo</li>
 *   <li>ANALYSIS: Extensions pueden analizar el grafo</li>
 *   <li>GENERATION: Extensions pueden generar código adicional</li>
 * </ol>
 * 
 * @author Veld Team
 * @version 1.0.0
 */
public final class SpiExtensionExecutor {
    
    private final SpiExtensionLoader extensionLoader;
    private final boolean enabled;
    private final List<String> executionErrors;
    
    /**
     * Crea un nuevo ejecutor de extensiones.
     * 
     * @param enabled si las extensiones están habilitadas
     */
    public SpiExtensionExecutor(boolean enabled) {
        this.enabled = enabled;
        this.extensionLoader = enabled ? SpiExtensionLoader.loadExtensions() : null;
        this.executionErrors = new ArrayList<>();
    }
    
    /**
     * Construye el grafo de dependencias desde los componentes descubiertos.
     * 
     * @param components lista de componentes descubiertos
     * @param typeUtils utilitario de tipos
     * @return el grafo construido, o null si no hay componentes
     */
    public VeldGraph buildGraph(List<ComponentInfo> components, Types typeUtils) {
        if (components == null || components.isEmpty()) {
            return null;
        }
        
        SpiVeldGraph.Builder graphBuilder = SpiVeldGraph.builder();
        
        // Map for quick component lookup by class name
        Map<String, SpiComponentNode> nodesByName = new HashMap<>();
        
        // First pass: create all nodes
        for (ComponentInfo comp : components) {
            SpiComponentNode node = new SpiComponentNode(comp);
            nodesByName.put(comp.getClassName(), node);
            graphBuilder.addComponent(comp);
        }
        
        // Second pass: create dependency edges
        for (ComponentInfo comp : components) {
            SpiComponentNode source = nodesByName.get(comp.getClassName());
            if (source == null) continue;
            
            // Constructor dependencies
            if (comp.getConstructorInjection() != null) {
                for (InjectionPoint.Dependency dep : comp.getConstructorInjection().getDependencies()) {
                    SpiComponentNode target = nodesByName.get(dep.getActualTypeName());
                    if (target != null) {
                        graphBuilder.addDependency(source, target, 
                            InjectionPoint.Type.CONSTRUCTOR, dep, null);
                    }
                }
            }
            
            // Field dependencies
            for (InjectionPoint field : comp.getFieldInjections()) {
                if (!field.getDependencies().isEmpty()) {
                    InjectionPoint.Dependency dep = field.getDependencies().get(0);
                    SpiComponentNode target = nodesByName.get(dep.getActualTypeName());
                    if (target != null) {
                        graphBuilder.addDependency(source, target, 
                            InjectionPoint.Type.FIELD, dep, null);
                    }
                }
            }
            
            // Method dependencies
            for (InjectionPoint method : comp.getMethodInjections()) {
                for (InjectionPoint.Dependency dep : method.getDependencies()) {
                    SpiComponentNode target = nodesByName.get(dep.getActualTypeName());
                    if (target != null) {
                        graphBuilder.addDependency(source, target, 
                            InjectionPoint.Type.METHOD, dep, null);
                    }
                }
            }
        }
        
        return graphBuilder.build();
    }
    
    /**
     * Ejecuta las extensiones en la fase INIT.
     * 
     * @param graph el grafo de dependencias
     * @param context el contexto de procesamiento
     */
    public void executeInitPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.INIT, graph, context);
    }
    
    /**
     * Ejecuta las extensiones en la fase VALIDATION.
     * 
     * @param graph el grafo de dependencias
     * @param context el contexto de procesamiento
     */
    public void executeValidationPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.VALIDATION, graph, context);
    }
    
    /**
     * Ejecuta las extensiones en la fase ANALYSIS.
     * 
     * @param graph el grafo de dependencias
     * @param context el contexto de procesamiento
     */
    public void executeAnalysisPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.ANALYSIS, graph, context);
    }
    
    /**
     * Ejecuta las extensiones en la fase GENERATION.
     * 
     * @param graph el grafo de dependencias
     * @param context el contexto de procesamiento
     */
    public void executeGenerationPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.GENERATION, graph, context);
    }
    
    private void executeExtensionsForPhase(ExtensionPhase phase, VeldGraph graph, 
                                           VeldProcessingContext context) {
        if (extensionLoader == null) return;
        
        List<VeldExtension> extensions = extensionLoader.getExtensionsForPhase(phase);
        
        for (VeldExtension extension : extensions) {
            try {
                extension.execute(graph, context);
            } catch (Exception e) {
                String errorMsg = "Extension '" + getExtensionId(extension) + 
                                  "' failed in " + phase + " phase: " + e.getMessage();
                executionErrors.add(errorMsg);
                
                // Report as warning to not break compilation
                context.reportWarning("Extension error: " + e.getMessage(), null);
            }
        }
    }
    
    private String getExtensionId(VeldExtension extension) {
        try {
            return extension.getDescriptor().getExtensionId();
        } catch (Exception e) {
            return extension.getClass().getName();
        }
    }
    
    /**
     * Verifica si hay extensiones cargadas.
     * 
     * @return true si hay extensiones disponibles
     */
    public boolean hasExtensions() {
        return enabled && extensionLoader != null && extensionLoader.hasExtensions();
    }
    
    /**
     * Returns el número de extensiones cargadas.
     * 
     * @return cantidad de extensiones
     */
    public int getExtensionCount() {
        return enabled && extensionLoader != null ? extensionLoader.getExtensionCount() : 0;
    }
    
    /**
     * Returns información sobre las extensiones cargadas.
     * 
     * @return string con información de extensiones
     */
    public String getExtensionsInfo() {
        if (!enabled || extensionLoader == null) {
            return "Extensions disabled";
        }
        return extensionLoader.getExtensionsInfo();
    }
    
    /**
     * Returns los errores de ejecución de extensiones.
     * 
     * @return lista de errores
     */
    public List<String> getExecutionErrors() {
        return Collections.unmodifiableList(executionErrors);
    }
    
    /**
     * Verifica si hubo errores durante la ejecución.
     * 
     * @return true si hubo errores
     */
    public boolean hasExecutionErrors() {
        return !executionErrors.isEmpty();
    }
    
    /**
     * Verifica si hubo errores durante la carga de extensiones.
     * 
     * @return true si hubo errores de carga
     */
    public boolean hasLoadErrors() {
        return enabled && extensionLoader != null && extensionLoader.hasErrors();
    }
    
    /**
     * Returns los errores de carga de extensiones.
     * 
     * @return lista de errores de carga
     */
    public List<String> getLoadErrors() {
        return enabled && extensionLoader != null ? 
            extensionLoader.getErrors() : Collections.emptyList();
    }
    
    /**
     * Crea un contexto de procesamiento para las extensiones.
     * 
     * @param messager el messager del processor
     * @param elementUtils el Elements utility
     * @param typeUtils el Types utility
     * @param roundEnv el RoundEnvironment
     * @param filer el filer del processor
     * @param supportedOptions las opciones de procesamiento soportadas
     * @param debugEnabled si el modo debug está habilitado
     * @return el contexto de procesamiento
     */
    public static VeldProcessingContext createContext(
            Messager messager, Elements elementUtils, Types typeUtils,
            RoundEnvironment roundEnv, javax.annotation.processing.Filer filer,
            Set<String> supportedOptions, boolean debugEnabled) {
        return new SpiVeldProcessingContext(
            messager, elementUtils, typeUtils, roundEnv, filer,
            supportedOptions, debugEnabled);
    }
}
