package io.github.yasmramos.veld.runtime.graph;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface for exporting the dependency graph in various formats.
 * Implementations of this interface can serialize the graph to DOT, JSON, or other formats.
 */
public interface GraphExporter {
    
    /**
     * Exports the dependency graph to the provided writer.
     * 
     * @param graph the dependency graph to export
     * @param writer the writer to write the export data to
     * @throws IOException if an I/O error occurs
     */
    void export(DependencyGraph graph, Writer writer) throws IOException;
    
    /**
     * Gets the file extension for this export format.
     * 
     * @return the file extension (e.g., "dot", "json")
     */
    String getFileExtension();
    
    /**
     * Gets a human-readable name for this export format.
     * 
     * @return the format name
     */
    String getFormatName();
}
