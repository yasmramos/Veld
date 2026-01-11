package io.github.yasmramos.veld.processor;

import java.util.*;

/**
 * Directed graph for detecting circular dependencies at compile-time.
 * 
 * Uses Depth-First Search (DFS) to detect cycles in the dependency graph.
 * When a cycle is detected, it provides the full cycle path for clear error messages.
 * 
 * Example cycle: UserService → OrderService → PaymentService → UserService
 */
public class DependencyGraph {
    
    // Adjacency list: component -> list of dependencies
    private final Map<String, Set<String>> adjacencyList = new LinkedHashMap<>();
    
    // For cycle detection
    private final Set<String> visited = new HashSet<>();
    private final Set<String> recursionStack = new HashSet<>();
    private final Map<String, String> parent = new HashMap<>();

    public DependencyGraph() {
    }
    
    /**
     * Adds a component to the graph.
     * 
     * @param componentName the fully qualified class name
     */
    public void addComponent(String componentName) {
        adjacencyList.putIfAbsent(componentName, new LinkedHashSet<>());
    }
    
    /**
     * Adds a dependency edge: from depends on to.
     * 
     * @param from the component that has the dependency
     * @param to the component being depended upon
     */
    public void addDependency(String from, String to) {
        adjacencyList.putIfAbsent(from, new LinkedHashSet<>());
        adjacencyList.putIfAbsent(to, new LinkedHashSet<>());
        adjacencyList.get(from).add(to);
    }
    
    /**
     * Detects if there are any circular dependencies in the graph.
     * 
     * @return Optional containing the cycle path if found, empty otherwise
     */
    public Optional<List<String>> detectCycle() {
        visited.clear();
        recursionStack.clear();
        parent.clear();
        
        for (String component : adjacencyList.keySet()) {
            if (!visited.contains(component)) {
                Optional<List<String>> cycle = dfs(component);
                if (cycle.isPresent()) {
                    return cycle;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Depth-First Search for cycle detection.
     */
    private Optional<List<String>> dfs(String current) {
        visited.add(current);
        recursionStack.add(current);
        
        Set<String> dependencies = adjacencyList.getOrDefault(current, Collections.emptySet());
        
        for (String dependency : dependencies) {
            if (!visited.contains(dependency)) {
                parent.put(dependency, current);
                Optional<List<String>> cycle = dfs(dependency);
                if (cycle.isPresent()) {
                    return cycle;
                }
            } else if (recursionStack.contains(dependency)) {
                // Cycle detected! Build the cycle path
                return Optional.of(buildCyclePath(current, dependency));
            }
        }
        
        recursionStack.remove(current);
        return Optional.empty();
    }
    
    /**
     * Builds the cycle path from the detected cycle.
     * 
     * @param current the current node where cycle was detected
     * @param cycleStart the node that starts the cycle
     * @return list representing the cycle path
     */
    private List<String> buildCyclePath(String current, String cycleStart) {
        List<String> cycle = new ArrayList<>();
        
        // Add the edge that closes the cycle
        cycle.add(cycleStart);
        
        // Trace back from current to cycleStart
        String node = current;
        List<String> path = new ArrayList<>();
        path.add(current);
        
        while (node != null && !node.equals(cycleStart)) {
            node = parent.get(node);
            if (node != null) {
                path.add(node);
            }
        }
        
        // Reverse to get correct order
        Collections.reverse(path);
        cycle.clear();
        cycle.addAll(path);
        cycle.add(cycleStart); // Close the cycle
        
        return cycle;
    }
    
    /**
     * Formats a cycle path as a readable string.
     * 
     * @param cyclePath the cycle path
     * @return formatted string like "A → B → C → A"
     */
    public static String formatCycle(List<String> cyclePath) {
        if (cyclePath == null || cyclePath.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cyclePath.size(); i++) {
            // Use simple class name for readability
            String fullName = cyclePath.get(i);
            String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
            sb.append(simpleName);
            
            if (i < cyclePath.size() - 1) {
                sb.append(" → ");
            }
        }
        return sb.toString();
    }
    
    /**
     * Gets all components in the graph.
     * 
     * @return set of component names
     */
    public Set<String> getComponents() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }
    
    /**
     * Gets dependencies for a component.
     * 
     * @param component the component name
     * @return set of dependency names
     */
    public Set<String> getDependencies(String component) {
        return Collections.unmodifiableSet(
            adjacencyList.getOrDefault(component, Collections.emptySet()));
    }
    
    /**
     * Returns a topological sort of the components (if no cycles exist).
     * Useful for determining initialization order.
     * 
     * @return list of components in topological order
     * @throws IllegalStateException if a cycle exists
     */
    public List<String> topologicalSort() {
        Optional<List<String>> cycle = detectCycle();
        if (cycle.isPresent()) {
            throw new IllegalStateException(
                "Cannot perform topological sort: circular dependency detected: " + 
                formatCycle(cycle.get()));
        }
        
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        
        for (String component : adjacencyList.keySet()) {
            if (!visited.contains(component)) {
                topologicalSortDfs(component, visited, stack);
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }
    
    private void topologicalSortDfs(String current, Set<String> visited, Deque<String> stack) {
        visited.add(current);
        
        for (String dependency : adjacencyList.getOrDefault(current, Collections.emptySet())) {
            if (!visited.contains(dependency)) {
                topologicalSortDfs(dependency, visited, stack);
            }
        }
        
        stack.push(current);
    }
    
    /**
     * Returns a string representation of the graph for debugging.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DependencyGraph {\n");
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            String simpleName = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
            sb.append("  ").append(simpleName).append(" → [");
            
            StringJoiner joiner = new StringJoiner(", ");
            for (String dep : entry.getValue()) {
                joiner.add(dep.substring(dep.lastIndexOf('.') + 1));
            }
            sb.append(joiner).append("]\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
