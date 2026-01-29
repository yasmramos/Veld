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
 * Veld extension executor that orchestrates execution in the correct phases.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Building the dependency graph from discovered components</li>
 *   <li>Executing extensions in the correct order according to their phase</li>
 *   <li>Handling errors so they don't affect core processing</li>
 *   <li>Providing the processing context to each extension</li>
 * </ul>
 *
 * <p>The execution flow follows the pattern:</p>
 * <ol>
 *   <li>INIT: Extensions can initialize resources</li>
 *   <li>VALIDATION: Extensions can validate the graph</li>
 *   <li>ANALYSIS: Extensions can analyze the graph</li>
 *   <li>GENERATION: Extensions can generate additional code</li>
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
     * Creates a new extension executor.
     *
     * @param enabled whether extensions are enabled
     */
    public SpiExtensionExecutor(boolean enabled) {
        this.enabled = enabled;
        this.extensionLoader = enabled ? SpiExtensionLoader.loadExtensions() : null;
        this.executionErrors = new ArrayList<>();
    }
    
    /**
     * Builds the dependency graph from discovered components.
     *
     * @param components list of discovered components
     * @param typeUtils types utility
     * @return the built graph, or null if there are no components
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
     * Executes extensions in the INIT phase.
     *
     * @param graph the dependency graph
     * @param context the processing context
     */
    public void executeInitPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.INIT, graph, context);
    }
    
    /**
     * Executes extensions in the VALIDATION phase.
     *
     * @param graph the dependency graph
     * @param context the processing context
     */
    public void executeValidationPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.VALIDATION, graph, context);
    }
    
    /**
     * Executes extensions in the ANALYSIS phase.
     *
     * @param graph the dependency graph
     * @param context the processing context
     */
    public void executeAnalysisPhase(VeldGraph graph, VeldProcessingContext context) {
        if (!enabled || graph == null) return;
        executeExtensionsForPhase(ExtensionPhase.ANALYSIS, graph, context);
    }
    
    /**
     * Executes extensions in the GENERATION phase.
     *
     * @param graph the dependency graph
     * @param context the processing context
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
     * Checks if extensions are loaded.
     *
     * @return true if extensions are available
     */
    public boolean hasExtensions() {
        return enabled && extensionLoader != null && extensionLoader.hasExtensions();
    }
    
    /**
     * Returns the number of loaded extensions.
     *
     * @return number of extensions
     */
    public int getExtensionCount() {
        return enabled && extensionLoader != null ? extensionLoader.getExtensionCount() : 0;
    }
    
    /**
     * Returns information about loaded extensions.
     *
     * @return string with extension information
     */
    public String getExtensionsInfo() {
        if (!enabled || extensionLoader == null) {
            return "Extensions disabled";
        }
        return extensionLoader.getExtensionsInfo();
    }
    
    /**
     * Returns extension execution errors.
     *
     * @return list of errors
     */
    public List<String> getExecutionErrors() {
        return Collections.unmodifiableList(executionErrors);
    }
    
    /**
     * Checks if there were errors during execution.
     *
     * @return true if there were errors
     */
    public boolean hasExecutionErrors() {
        return !executionErrors.isEmpty();
    }
    
    /**
     * Checks if there were errors during extension loading.
     *
     * @return true if there were load errors
     */
    public boolean hasLoadErrors() {
        return enabled && extensionLoader != null && extensionLoader.hasErrors();
    }
    
    /**
     * Returns extension loading errors.
     *
     * @return list of loading errors
     */
    public List<String> getLoadErrors() {
        return enabled && extensionLoader != null ? 
            extensionLoader.getErrors() : Collections.emptyList();
    }
    
    /**
     * Creates a processing context for extensions.
     *
     * @param messager the processor messager
     * @param elementUtils the Elements utility
     * @param typeUtils the Types utility
     * @param roundEnv the RoundEnvironment
     * @param filer the processor filer
     * @param supportedOptions supported processing options
     * @param debugEnabled whether debug mode is enabled
     * @return the processing context
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
