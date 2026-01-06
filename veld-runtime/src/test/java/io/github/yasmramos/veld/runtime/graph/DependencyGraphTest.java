package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.runtime.LegacyScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for DependencyGraph - previously at 0% coverage.
 * These tests exercise the actual DependencyGraph implementation.
 */
@DisplayName("DependencyGraph Tests")
class DependencyGraphTest {

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty graph")
        void testEmptyGraph() {
            assertEquals(0, graph.nodeCount());
            assertEquals(0, graph.edgeCount());
            assertTrue(graph.getNodes().isEmpty());
            assertTrue(graph.getEdges().isEmpty());
        }
    }

    @Nested
    @DisplayName("Node Operations")
    class NodeOperationsTests {

        @Test
        @DisplayName("Should add node and return it by class name")
        void testAddAndGetNode() {
            DependencyNode node = new DependencyNode("com.example.Service", "service", LegacyScope.SINGLETON);
            graph.addNode(node);

            Optional<DependencyNode> result = graph.getNode("com.example.Service");
            assertTrue(result.isPresent());
            assertEquals("com.example.Service", result.get().getClassName());
        }

        @Test
        @DisplayName("Should return empty optional for non-existent node")
        void testGetNonExistentNode() {
            Optional<DependencyNode> result = graph.getNode("com.example.NonExistent");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return all nodes")
        void testGetAllNodes() {
            DependencyNode node1 = new DependencyNode("com.example.Service1", "service1", LegacyScope.SINGLETON);
            DependencyNode node2 = new DependencyNode("com.example.Service2", "service2", LegacyScope.PROTOTYPE);
            graph.addNode(node1);
            graph.addNode(node2);

            Collection<DependencyNode> nodes = graph.getNodes();
            assertEquals(2, nodes.size());
            assertTrue(nodes.contains(node1));
            assertTrue(nodes.contains(node2));
        }

        @Test
        @DisplayName("Should return correct node count")
        void testNodeCount() {
            assertEquals(0, graph.nodeCount());

            graph.addNode(new DependencyNode("com.example.Service1", "service1", LegacyScope.SINGLETON));
            assertEquals(1, graph.nodeCount());

            graph.addNode(new DependencyNode("com.example.Service2", "service2", LegacyScope.PROTOTYPE));
            assertEquals(2, graph.nodeCount());
        }

        @Test
        @DisplayName("Should replace existing node with same class name")
        void testReplaceNode() {
            DependencyNode node1 = new DependencyNode("com.example.Service", "service1", LegacyScope.SINGLETON);
            DependencyNode node2 = new DependencyNode("com.example.Service", "service2", LegacyScope.PROTOTYPE);

            graph.addNode(node1);
            graph.addNode(node2);

            assertEquals(1, graph.nodeCount());
            assertEquals("service2", graph.getNode("com.example.Service").get().getComponentName());
        }
    }

    @Nested
    @DisplayName("Edge Operations")
    class EdgeOperationsTests {

        @Test
        @DisplayName("Should add edge and return it")
        void testAddAndGetEdge() {
            graph.addEdge("com.example.ServiceA", "com.example.ServiceB", "dependsOn");

            List<DependencyGraph.DependencyEdge> edges = graph.getEdges();
            assertEquals(1, edges.size());

            DependencyGraph.DependencyEdge edge = edges.get(0);
            assertEquals("com.example.ServiceA", edge.getFrom());
            assertEquals("com.example.ServiceB", edge.getTo());
            assertEquals("dependsOn", edge.getRelationship());
        }

        @Test
        @DisplayName("Should return correct edge count")
        void testEdgeCount() {
            assertEquals(0, graph.edgeCount());

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            assertEquals(1, graph.edgeCount());

            graph.addEdge("com.example.B", "com.example.C", "dependsOn");
            assertEquals(2, graph.edgeCount());
        }

        @Test
        @DisplayName("Should return unmodifiable edges list")
        void testEdgesAreUnmodifiable() {
            graph.addEdge("com.example.A", "com.example.B", "dependsOn");

            List<DependencyGraph.DependencyEdge> edges = graph.getEdges();
            assertThrows(UnsupportedOperationException.class, () ->
                edges.add(new DependencyGraph.DependencyEdge("x", "y", "z")));
        }

        @Test
        @DisplayName("Should return unmodifiable nodes collection")
        void testNodesAreUnmodifiable() {
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));

            Collection<DependencyNode> nodes = graph.getNodes();
            assertThrows(UnsupportedOperationException.class, () ->
                nodes.add(new DependencyNode("com.example.B", "b", LegacyScope.SINGLETON)));
        }
    }

    @Nested
    @DisplayName("Root and Leaf Node Detection")
    class RootLeafNodeTests {

        @BeforeEach
        void setUpComplexGraph() {
            // A -> B -> C
            // D -> B
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.C", "c", LegacyScope.PROTOTYPE));
            graph.addNode(new DependencyNode("com.example.D", "d", LegacyScope.SINGLETON));

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.B", "com.example.C", "dependsOn");
            graph.addEdge("com.example.D", "com.example.B", "dependsOn");
        }

        @Test
        @DisplayName("Should identify root nodes (not depended upon by anyone)")
        void testGetRootNodes() {
            List<DependencyNode> roots = graph.getRootNodes();

            // Roots are nodes that don't appear as "from" in any edge
            // A appears as "from" -> not a root
            // B appears as "from" -> not a root
            // D appears as "from" -> not a root
            // C does NOT appear as "from" in any edge -> IS the only root
            assertEquals(1, roots.size());
            Set<String> rootNames = new HashSet<>();
            for (DependencyNode node : roots) {
                rootNames.add(node.getClassName());
            }
            assertTrue(rootNames.contains("com.example.C"));
        }

        @Test
        @DisplayName("Should identify leaf nodes (no dependencies)")
        void testGetLeafNodes() {
            List<DependencyNode> leaves = graph.getLeafNodes();

            // Leaves are nodes that don't appear as "to" in any edge
            // A and D do NOT appear as "to" edges, so they ARE leaves
            // B and C appear as "to" edges, so they are not leaves
            assertEquals(2, leaves.size());
            Set<String> leafNames = new HashSet<>();
            for (DependencyNode node : leaves) {
                leafNames.add(node.getClassName());
            }
            assertTrue(leafNames.contains("com.example.A"));
            assertTrue(leafNames.contains("com.example.D"));
        }

        @Test
        @DisplayName("Should handle empty graph for roots and leaves")
        void testEmptyGraphRootsLeaves() {
            DependencyGraph emptyGraph = new DependencyGraph();
            assertTrue(emptyGraph.getRootNodes().isEmpty());
            assertTrue(emptyGraph.getLeafNodes().isEmpty());
        }
    }

    @Nested
    @DisplayName("Cycle Detection Tests")
    class CycleDetectionTests {

        @Test
        @DisplayName("Should detect cycle in graph")
        void testFindCyclesWithCycle() {
            // A -> B -> C -> A
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.C", "c", LegacyScope.PROTOTYPE));

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.B", "com.example.C", "dependsOn");
            graph.addEdge("com.example.C", "com.example.A", "dependsOn");

            List<List<String>> cycles = graph.findCycles();
            assertFalse(cycles.isEmpty());
        }

        @Test
        @DisplayName("Should not detect cycles in acyclic graph")
        void testFindCyclesWithoutCycle() {
            // A -> B -> C
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.C", "c", LegacyScope.PROTOTYPE));

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.B", "com.example.C", "dependsOn");

            List<List<String>> cycles = graph.findCycles();
            assertTrue(cycles.isEmpty());
        }

        @Test
        @DisplayName("Should not detect cycles in empty graph")
        void testFindCyclesEmptyGraph() {
            List<List<String>> cycles = graph.findCycles();
            assertTrue(cycles.isEmpty());
        }

        @Test
        @DisplayName("Should detect multiple cycles")
        void testFindMultipleCycles() {
            // A -> B -> A
            // C -> D -> C
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.C", "c", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.D", "d", LegacyScope.SINGLETON));

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.B", "com.example.A", "dependsOn");
            graph.addEdge("com.example.C", "com.example.D", "dependsOn");
            graph.addEdge("com.example.D", "com.example.C", "dependsOn");

            List<List<String>> cycles = graph.findCycles();
            assertEquals(2, cycles.size());
        }
    }

    @Nested
    @DisplayName("Complex Graph Scenarios")
    class ComplexGraphTests {

        @Test
        @DisplayName("Should handle disconnected nodes")
        void testDisconnectedNodes() {
            graph.addNode(new DependencyNode("com.example.ComponentA", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.ComponentB", "b", LegacyScope.PROTOTYPE));
            // No edges between them

            assertEquals(2, graph.nodeCount());
            assertEquals(0, graph.edgeCount());
        }

        @Test
        @DisplayName("Should handle self-referencing edges")
        void testSelfReferencingEdge() {
            graph.addNode(new DependencyNode("com.example.Self", "self", LegacyScope.SINGLETON));
            graph.addEdge("com.example.Self", "com.example.Self", "selfDependsOn");

            assertEquals(1, graph.edgeCount());

            // Self-reference is a cycle
            List<List<String>> cycles = graph.findCycles();
            assertFalse(cycles.isEmpty());
        }

        @Test
        @DisplayName("Should handle diamond dependency pattern")
        void testDiamondDependency() {
            // A -> B
            // A -> C
            // B -> D
            // C -> D
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.C", "c", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.D", "d", LegacyScope.PROTOTYPE));

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.A", "com.example.C", "dependsOn");
            graph.addEdge("com.example.B", "com.example.D", "dependsOn");
            graph.addEdge("com.example.C", "com.example.D", "dependsOn");

            assertEquals(4, graph.nodeCount());
            assertEquals(4, graph.edgeCount());

            // No cycles in a diamond
            List<List<String>> cycles = graph.findCycles();
            assertTrue(cycles.isEmpty());

            // Roots: nodes not appearing as "from" in any edge = {D}
            List<DependencyNode> roots = graph.getRootNodes();
            assertEquals(1, roots.size());
            assertEquals("com.example.D", roots.get(0).getClassName());

            // Leaves: nodes not appearing as "to" in any edge = {A}
            List<DependencyNode> leaves = graph.getLeafNodes();
            assertEquals(1, leaves.size());
            assertEquals("com.example.A", leaves.get(0).getClassName());
        }
    }

    @Nested
    @DisplayName("DependencyEdge Tests")
    class DependencyEdgeTests {

        @Test
        @DisplayName("Should create edge with all properties")
        void testEdgeCreation() {
            DependencyGraph.DependencyEdge edge = new DependencyGraph.DependencyEdge(
                "fromClass", "toClass", "relationship"
            );

            assertEquals("fromClass", edge.getFrom());
            assertEquals("toClass", edge.getTo());
            assertEquals("relationship", edge.getRelationship());
        }
    }
}
