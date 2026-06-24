package io.github.yasmramos.veld.processor;

import java.util.*;
import javax.lang.model.element.TypeElement;

/**
 * Encapsulates all mutable state for VeldProcessor to ensure proper
 * lifecycle management across multiple processing rounds.
 * 
 * <p>This class addresses the issue of global state management in annotation
 * processors, which can cause memory leaks or unexpected behavior when the
 * compiler invokes the processor multiple times or across different modules.</p>
 * 
 * <h2>State Categories:</h2>
 * <ul>
 *   <li><b>Accumulated State:</b> Built incrementally across rounds (NOT cleared)
 *     <ul>
 *       <li>discoveredComponents</li>
 *       <li>veldNodes</li>
 *       <li>processedClasses</li>
 *       <li>interfaceImplementors</li>
 *       <li>discoveredProfiles</li>
 *     </ul>
 *   </li>
 *   <li><b>Per-Round State:</b> Cleared at the start of each round
 *     <ul>
 *       <li>registeredScopes</li>
 *     </ul>
 *   </li>
 *   <li><b>Finalization State:</b> Used only in the last round
 *     <ul>
 *       <li>eventSubscriptions</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>The processor is invoked sequentially by the compiler, so this class
 * does not require synchronization. However, all collections are initialized
 * with predictable iteration order where needed (LinkedHashSet, LinkedHashMap).</p>
 * 
 * @author yasmramos
 * @since 1.0.0
 */
public final class ProcessorState {
    
    // ===== Accumulated State (NOT cleared between rounds) =====
    
    /**
     * All discovered components with their metadata.
     * Accumulated across rounds as new types are discovered.
     */
    private final List<ComponentInfo> discoveredComponents = new ArrayList<>();
    
    /**
     * Static dependency graph nodes for code generation.
     * Built incrementally as components are analyzed.
     */
    private final List<VeldNode> veldNodes = new ArrayList<>();
    
    /**
     * Tracks already processed classes to avoid duplicates across rounds.
     * Uses fully qualified class names as keys.
     */
    private final Set<String> processedClasses = new HashSet<>();
    
    /**
     * Maps interface -> list of implementing components for conflict detection.
     * Key: interface FQN, Value: list of component FQNs implementing it.
     */
    private final Map<String, List<String>> interfaceImplementors = new HashMap<>();
    
    /**
     * Discovered profiles from @Profile annotations.
     * Used for compile-time class generation (VeldDev, VeldProd, etc.).
     * Maintains insertion order for deterministic code generation.
     */
    private final Set<String> discoveredProfiles = new LinkedHashSet<>();
    
    // ===== Per-Round State (cleared each round) =====
    
    /**
     * Registered custom scopes discovered in current round.
     * Cleared at the start of each processing round.
     */
    private final Set<String> registeredScopes = new HashSet<>();
    
    // ===== Finalization State (used in last round) =====
    
    /**
     * Event subscriptions for zero-reflection event registration.
     * Populated during component analysis, used in final round for registry generation.
     */
    private final List<EventRegistryGenerator.SubscriptionInfo> eventSubscriptions = new ArrayList<>();
    
    /**
     * Dependency graph for cycle detection and validation.
     */
    private final DependencyGraph dependencyGraph = new DependencyGraph();
    
    // ===== Constructor =====
    
    /**
     * Creates a new ProcessorState with empty collections.
     */
    public ProcessorState() {
        // Initialize with default capacities
    }
    
    // ===== Accumulated State Accessors =====
    
    public List<ComponentInfo> getDiscoveredComponents() {
        return discoveredComponents;
    }
    
    public void addDiscoveredComponent(ComponentInfo info) {
        discoveredComponents.add(info);
    }
    
    public List<VeldNode> getVeldNodes() {
        return veldNodes;
    }
    
    public void addVeldNode(VeldNode node) {
        veldNodes.add(node);
    }
    
    public boolean isClassProcessed(String className) {
        return processedClasses.contains(className);
    }
    
    public void markClassAsProcessed(String className) {
        processedClasses.add(className);
    }
    
    public Map<String, List<String>> getInterfaceImplementors() {
        return interfaceImplementors;
    }
    
    public void addImplementor(String interfaceName, String implementorName) {
        interfaceImplementors.computeIfAbsent(interfaceName, k -> new ArrayList<>())
                            .add(implementorName);
    }
    
    public Set<String> getDiscoveredProfiles() {
        return discoveredProfiles;
    }
    
    public void addProfile(String profile) {
        discoveredProfiles.add(profile);
    }
    
    // ===== Per-Round State Accessors =====
    
    public Set<String> getRegisteredScopes() {
        return registeredScopes;
    }
    
    /**
     * Clears per-round state at the start of each processing round.
     * This should be called at the beginning of process().
     */
    public void clearPerRoundState() {
        registeredScopes.clear();
    }
    
    public void addRegisteredScope(String scopeId) {
        registeredScopes.add(scopeId);
    }
    
    // ===== Finalization State Accessors =====
    
    public List<EventRegistryGenerator.SubscriptionInfo> getEventSubscriptions() {
        return eventSubscriptions;
    }
    
    public void addEventSubscription(EventRegistryGenerator.SubscriptionInfo subscription) {
        eventSubscriptions.add(subscription);
    }
    
    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }
    
    // ===== Utility Methods =====
    
    /**
     * Returns the total number of discovered components.
     */
    public int getComponentCount() {
        return discoveredComponents.size();
    }
    
    /**
     * Returns the total number of veld nodes.
     */
    public int getNodeCount() {
        return veldNodes.size();
    }
    
    /**
     * Checks if any profiles have been discovered.
     */
    public boolean hasProfiles() {
        return !discoveredProfiles.isEmpty();
    }
    
    /**
     * Resets all accumulated state.
     * This should only be called if the processor needs to be reinitialized.
     */
    public void resetAll() {
        discoveredComponents.clear();
        veldNodes.clear();
        processedClasses.clear();
        interfaceImplementors.clear();
        discoveredProfiles.clear();
        eventSubscriptions.clear();
        dependencyGraph.clear();
        clearPerRoundState();
    }
    
    /**
     * Returns a summary of the current state for debugging.
     */
    public String getSummary() {
        return String.format(
            "ProcessorState[components=%d, nodes=%d, profiles=%d, processed=%d]",
            discoveredComponents.size(),
            veldNodes.size(),
            discoveredProfiles.size(),
            processedClasses.size()
        );
    }
}
