package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.runtime.LegacyScope;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Exports the dependency graph in JSON format.
 * The JSON format is ideal for programmatic access and integration with other tools.
 * 
 * Example output:
 * <pre>
 * {
 *   "graph": {
 *     "directed": true,
 *     "nodes": [
 *       {
 *         "id": "com.example.Service",
 *         "label": "Service",
 *         "scope": "singleton",
 *         "profiles": [],
 *         "dependencies": ["com.example.Repository"]
 *       }
 *     ],
 *     "edges": [
 *       {
 *         "from": "com.example.Service",
 *         "to": "com.example.Repository",
 *         "relationship": "depends on"
 *       }
 *     ]
 *   },
 *   "metadata": {
 *     "nodeCount": 1,
 *     "edgeCount": 1,
 *     "cycles": []
 *   }
 * }
 * </pre>
 */
public final class JsonExporter implements GraphExporter {
    
    private final boolean includeMetadata;
    private final boolean prettyPrint;
    
    public JsonExporter() {
        this(true, true);
    }
    
    public JsonExporter(boolean includeMetadata, boolean prettyPrint) {
        this.includeMetadata = includeMetadata;
        this.prettyPrint = prettyPrint;
    }
    
    @Override
    public void export(DependencyGraph graph, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{");
        if (prettyPrint) sb.append("\n");
        
        // Graph section
        sb.append(indent(1)).append("\"graph\": {");
        if (prettyPrint) sb.append("\n");
        sb.append(indent(2)).append("\"directed\": true,");
        if (prettyPrint) sb.append("\n");
        
        // Nodes
        sb.append(indent(2)).append("\"nodes\": [");
        if (prettyPrint) sb.append("\n");
        
        List<DependencyNode> nodes = new ArrayList<>(graph.getNodes());
        for (int i = 0; i < nodes.size(); i++) {
            DependencyNode node = nodes.get(i);
            sb.append(indent(3)).append("{");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(4)).append("\"id\": \"").append(escapeJson(node.getClassName())).append("\",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"label\": \"").append(escapeJson(node.getSimpleName())).append("\",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"scope\": \"").append(node.getScope()).append("\",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"isPrimary\": ").append(node.isPrimary()).append(",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"profiles\": ").append(toJsonArray(node.getProfiles())).append(",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"constructorDependencies\": ")
              .append(toJsonArray(node.getConstructorDependencies())).append(",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"fieldDependencies\": ")
              .append(toJsonArray(node.getFieldDependencies())).append(",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"methodDependencies\": ")
              .append(toJsonArray(node.getMethodDependencies()));
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(3)).append("}");
            if (i < nodes.size() - 1) sb.append(",");
            if (prettyPrint) sb.append("\n");
        }
        
        sb.append(indent(2)).append("],");
        if (prettyPrint) sb.append("\n");
        
        // Edges
        sb.append(indent(2)).append("\"edges\": [");
        if (prettyPrint) sb.append("\n");
        
        List<DependencyGraph.DependencyEdge> edges = graph.getEdges();
        for (int i = 0; i < edges.size(); i++) {
            DependencyGraph.DependencyEdge edge = edges.get(i);
            sb.append(indent(3)).append("{");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(4)).append("\"from\": \"").append(escapeJson(edge.getFrom())).append("\",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"to\": \"").append(escapeJson(edge.getTo())).append("\",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(4)).append("\"relationship\": \"")
              .append(escapeJson(edge.getRelationship() != null ? edge.getRelationship() : "")).append("\"");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(3)).append("}");
            if (i < edges.size() - 1) sb.append(",");
            if (prettyPrint) sb.append("\n");
        }
        
        sb.append(indent(2)).append("]");
        if (prettyPrint) sb.append("\n");
        
        sb.append(indent(1)).append("}");
        
        // Metadata section
        if (includeMetadata) {
            sb.append(",");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(1)).append("\"metadata\": {");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(2)).append("\"nodeCount\": ").append(graph.nodeCount()).append(",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(2)).append("\"edgeCount\": ").append(graph.edgeCount()).append(",");
            if (prettyPrint) sb.append("\n");
            
            // Find cycles
            List<List<String>> cycles = graph.findCycles();
            sb.append(indent(2)).append("\"hasCycles\": ").append(!cycles.isEmpty()).append(",");
            if (prettyPrint) sb.append("\n");
            sb.append(indent(2)).append("\"cycleCount\": ").append(cycles.size()).append(",");
            if (prettyPrint) sb.append("\n");
            
            // Root and leaf nodes
            sb.append(indent(2)).append("\"rootNodes\": [");
            List<DependencyNode> roots = graph.getRootNodes();
            for (int i = 0; i < roots.size(); i++) {
                sb.append("\"").append(escapeJson(roots.get(i).getClassName())).append("\"");
                if (i < roots.size() - 1) sb.append(", ");
            }
            sb.append("],");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(2)).append("\"leafNodes\": [");
            List<DependencyNode> leaves = graph.getLeafNodes();
            for (int i = 0; i < leaves.size(); i++) {
                sb.append("\"").append(escapeJson(leaves.get(i).getClassName())).append("\"");
                if (i < leaves.size() - 1) sb.append(", ");
            }
            sb.append("]");
            if (prettyPrint) sb.append("\n");
            
            sb.append(indent(1)).append("}");
        }
        
        sb.append("}\n");
        
        writer.write(sb.toString());
    }
    
    private String indent(int level) {
        if (!prettyPrint) {
            return "";
        }
        return "  ".repeat(level);
    }
    
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
    }
    
    private String toJsonArray(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String item : collection) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(item)).append("\"");
            i++;
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public String getFileExtension() {
        return "json";
    }
    
    @Override
    public String getFormatName() {
        return "JSON";
    }
}
