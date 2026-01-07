package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.annotation.ScopeType;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Exports the dependency graph in DOT (Graphviz) format.
 * The DOT format is widely supported by graph visualization tools.
 * 
 * Example output:
 * <pre>
 * digraph G {
 *   rankdir=TB;
 *   node [shape=box, style=rounded];
 *   "com.example.Service" [label="Service"];
 *   "com.example.Repository" [label="Repository"];
 *   "com.example.Service" -> "com.example.Repository" [label="depends on"];
 * }
 * </pre>
 */
public final class DotExporter implements GraphExporter {
    
    private static final String INDENT = "  ";
    
    @Override
    public void export(DependencyGraph graph, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("digraph G {\n");
        sb.append(INDENT).append("rankdir=TB;\n");
        sb.append(INDENT).append("node [shape=box, style=rounded, fontname=\"Arial\"];\n");
        sb.append(INDENT).append("edge [fontname=\"Arial\"];\n\n");
        
        // Add rank constraint for leaf nodes (optional, can improve layout)
        List<DependencyNode> leafNodes = graph.getLeafNodes();
        if (!leafNodes.isEmpty()) {
            sb.append(INDENT).append("{ rank=same; ");
            for (int i = 0; i < leafNodes.size(); i++) {
                if (i > 0) sb.append("; ");
                sb.append("\"").append(escape(leafNodes.get(i).getClassName())).append("\"");
            }
            sb.append(" }\n\n");
        }
        
        // Add nodes with attributes based on scope and other properties
        for (DependencyNode node : graph.getNodes()) {
            sb.append(INDENT).append("\"").append(escape(node.getClassName())).append("\"");
            sb.append(" [label=\"").append(escape(node.getSimpleName())).append("\"");
            
            // Add visual attributes based on node properties
            if (node.isPrimary()) {
                sb.append(", style=\"filled\", fillcolor=\"lightyellow\"");
            }
            
            String shape = getShapeForScope(node.getScope());
            if (shape != null) {
                sb.append(", shape=\"").append(shape).append("\"");
            }
            
            // Color based on profiles
            if (!node.getProfiles().isEmpty()) {
                sb.append(", color=\"blue\"");
            }
            
            sb.append("];\n");
        }
        
        sb.append("\n");
        
        // Add edges with labels
        Set<String> addedEdges = new HashSet<>();
        for (DependencyGraph.DependencyEdge edge : graph.getEdges()) {
            String edgeKey = edge.getFrom() + "->" + edge.getTo();
            if (!addedEdges.contains(edgeKey)) {
                sb.append(INDENT).append("\"").append(escape(edge.getFrom())).append("\"");
                sb.append(" -> ");
                sb.append("\"").append(escape(edge.getTo())).append("\"");
                
                if (edge.getRelationship() != null && !edge.getRelationship().isEmpty()) {
                    sb.append(" [label=\"").append(escape(edge.getRelationship())).append("\"]");
                }
                
                sb.append(";\n");
                addedEdges.add(edgeKey);
            }
        }
        
        // Footer
        sb.append("}\n");
        
        writer.write(sb.toString());
    }
    
    /**
     * Gets the DOT shape for a given scope.
     */
    private String getShapeForScope(ScopeType scope) {
        if (scope == null) {
            return "box";
        }
        return switch (scope) {
            case SINGLETON -> "box";
            case PROTOTYPE -> "oval";
        };
    }
    
    /**
     * Escapes special characters for DOT format.
     */
    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    @Override
    public String getFileExtension() {
        return "dot";
    }
    
    @Override
    public String getFormatName() {
        return "DOT (Graphviz)";
    }
}
