package io.github.yasmramos.veld;

import io.github.yasmramos.veld.runtime.ComponentRegistry;
import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.graph.*;
import io.github.yasmramos.veld.runtime.value.ValueResolver;
import io.github.yasmramos.veld.runtime.Provider;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Stub class for compile-time API reference.
 * The actual implementation is generated at compile-time by veld-processor.
 */
public class Veld {
    
    private Veld() {}
    
    public static <T> T get(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> T get(Class<T> type, String name) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> List<T> getAll(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> Provider<T> getProvider(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static <T> Optional<T> getOptional(Class<T> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static boolean contains(Class<?> type) {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static int componentCount() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static LifecycleProcessor getLifecycleProcessor() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static ComponentRegistry getRegistry() {
        throw new VeldException("Stub - use generated Veld class");
    }
    
    public static EventBus getEventBus() {
        return EventBus.getInstance();
    }
    
    public static ValueResolver getValueResolver() {
        return ValueResolver.getInstance();
    }
    
    public static String resolveValue(String expression) {
        return ValueResolver.getInstance().resolve(expression);
    }
    
    public static <T> T resolveValue(String expression, Class<T> type) {
        return ValueResolver.getInstance().resolve(expression, type);
    }
    
    public static void setActiveProfiles(String... profiles) {
        // No-op in stub
    }
    
    public static boolean isProfileActive(String profile) {
        return false;
    }
    
    public static String[] getActiveProfiles() {
        return new String[0];
    }
    
    public static void shutdown() {
        throw new VeldException("Stub - use generated Veld class");
    }

    /**
     * Injects dependencies into an instance using the Veld registry.
     * This method is used by generated factories to inject dependencies
     * into factory class instances.
     *
     * @param instance the instance to inject dependencies into
     */
    public static void inject(Object instance) {
        throw new VeldException("Stub - use generated Veld class");
    }

    // ==================== Dependency Graph Export ====================
    
    /**
     * Exports the dependency graph in DOT (Graphviz) format.
     * The DOT format can be visualized using Graphviz or compatible tools.
     *
     * @param writer the writer to write the DOT output to
     * @throws IOException if an I/O error occurs
     */
    public static void exportDependencyGraphDot(Writer writer) throws IOException {
        ComponentRegistry registry = getRegistry();
        if (registry != null) {
            DotExporter exporter = new DotExporter();
            exporter.export(registry.buildDependencyGraph(), writer);
        }
    }
    
    /**
     * Exports the dependency graph in DOT (Graphviz) format.
     *
     * @return the DOT representation as a string
     * @throws IOException if an I/O error occurs
     */
    public static String exportDependencyGraphDot() throws IOException {
        StringWriter writer = new StringWriter();
        exportDependencyGraphDot(writer);
        return writer.toString();
    }
    
    /**
     * Exports the dependency graph in JSON format.
     * JSON is ideal for programmatic access and integration with other tools.
     *
     * @param writer the writer to write the JSON output to
     * @throws IOException if an I/O error occurs
     */
    public static void exportDependencyGraphJson(Writer writer) throws IOException {
        ComponentRegistry registry = getRegistry();
        if (registry != null) {
            JsonExporter exporter = new JsonExporter();
            exporter.export(registry.buildDependencyGraph(), writer);
        }
    }
    
    /**
     * Exports the dependency graph in JSON format.
     *
     * @return the JSON representation as a string
     * @throws IOException if an I/O error occurs
     */
    public static String exportDependencyGraphJson() throws IOException {
        StringWriter writer = new StringWriter();
        exportDependencyGraphJson(writer);
        return writer.toString();
    }
    
    /**
     * Exports the dependency graph using the specified exporter.
     *
     * @param exporter the exporter to use
     * @param writer the writer to write the output to
     * @throws IOException if an I/O error occurs
     */
    public static void exportDependencyGraph(GraphExporter exporter, Writer writer) throws IOException {
        ComponentRegistry registry = getRegistry();
        if (registry != null) {
            exporter.export(registry.buildDependencyGraph(), writer);
        }
    }
    
    /**
     * Exports the dependency graph using the specified exporter.
     *
     * @param exporter the exporter to use
     * @return the exported representation as a string
     * @throws IOException if an I/O error occurs
     */
    public static String exportDependencyGraph(GraphExporter exporter) throws IOException {
        StringWriter writer = new StringWriter();
        exportDependencyGraph(exporter, writer);
        return writer.toString();
    }
    
    /**
     * Gets the dependency graph for visualization.
     *
     * @return the dependency graph, or null if the registry is not available
     */
    public static DependencyGraph getDependencyGraph() {
        ComponentRegistry registry = getRegistry();
        return registry != null ? registry.buildDependencyGraph() : null;
    }
    
    public static class Annotations {
        private Annotations() {}
    }
}
