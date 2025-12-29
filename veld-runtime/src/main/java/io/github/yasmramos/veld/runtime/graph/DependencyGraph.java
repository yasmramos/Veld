package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.runtime.ComponentRegistry;
import io.github.yasmramos.veld.runtime.Scope;

import java.util.*;

/**
 * Represents the complete dependency graph of Veld components.
 * Provides methods to export the graph in DOT and JSON formats.
 */
public final class DependencyGraph {
    
    private final Map<String, DependencyNode> nodes = new LinkedHashMap<>();
    private final List<DependencyEdge> edges = new ArrayList<>();
    
    public DependencyGraph() {}
    
    /**
     * Adds a node to the graph.
     */
    public void addNode(DependencyNode node) {
        nodes.put(node.getClassName(), node);
    }
    
    /**
     * Adds an edge from source to target with a label describing the relationship.
     */
    public void addEdge(String fromClass, String toClass, String relationship) {
        edges.add(new DependencyEdge(fromClass, toClass, relationship));
    }
    
    /**
     * Gets a node by class name.
     */
    public Optional<DependencyNode> getNode(String className) {
        return Optional.ofNullable(nodes.get(className));
    }
    
    /**
     * Gets all nodes in the graph.
     */
    public Collection<DependencyNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }
    
    /**
     * Gets all edges in the graph.
     */
    public List<DependencyEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }
    
    /**
     * Gets the total number of nodes.
     */
    public int nodeCount() {
        return nodes.size();
    }
    
    /**
     * Gets the total number of edges.
     */
    public int edgeCount() {
        return edges.size();
    }
    
    /**
     * Finds all nodes that have no dependencies (root components).
     */
    public List<DependencyNode> getRootNodes() {
        Set<String> dependents = new HashSet<>();
        for (DependencyEdge edge : edges) {
            dependents.add(edge.from);
        }
        
        List<DependencyNode> roots = new ArrayList<>();
        for (DependencyNode node : nodes.values()) {
            if (!dependents.contains(node.getClassName())) {
                roots.add(node);
            }
        }
        return roots;
    }
    
    /**
     * Finds all nodes that are not depended upon by any other node.
     */
    public List<DependencyNode> getLeafNodes() {
        Set<String> dependencies = new HashSet<>();
        for (DependencyEdge edge : edges) {
            dependencies.add(edge.to);
        }
        
        List<DependencyNode> leaves = new ArrayList<>();
        for (DependencyNode node : nodes.values()) {
            if (!dependencies.contains(node.getClassName())) {
                leaves.add(node);
            }
        }
        return leaves;
    }
    
    /**
     * Finds cycles in the dependency graph using DFS.
     */
    public List<List<String>> findCycles() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        Map<String, List<String>> path = new HashMap<>();
        
        for (String node : nodes.keySet()) {
            if (!visited.contains(node)) {
                dfs(node, visited, recursionStack, path, cycles);
            }
        }
        
        return cycles;
    }
    
    private void dfs(String node, Set<String> visited, Set<String> recursionStack,
                     Map<String, List<String>> path, List<List<String>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        path.computeIfAbsent(node, k -> new ArrayList<>()).add(node);
        
        for (DependencyEdge edge : edges) {
            if (edge.from.equals(node)) {
                if (!visited.contains(edge.to)) {
                    path.computeIfAbsent(edge.to, k -> new ArrayList<>()).addAll(path.get(node));
                    dfs(edge.to, visited, recursionStack, path, cycles);
                } else if (recursionStack.contains(edge.to)) {
                    // Found a cycle
                    List<String> cycle = new ArrayList<>(path.get(node));
                    cycle.add(edge.to);
                    cycles.add(cycle);
                }
            }
        }
        
        recursionStack.remove(node);
    }
    
    /**
     * Represents an edge in the dependency graph.
     */
    public static final class DependencyEdge {
        private final String from;
        private final String to;
        private final String relationship;
        
        public DependencyEdge(String from, String to, String relationship) {
            this.from = from;
            this.to = to;
            this.relationship = relationship;
        }
        
        public String getFrom() { return from; }
        public String getTo() { return to; }
        public String getRelationship() { return relationship; }
    }
}
