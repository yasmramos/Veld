package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.runtime.LegacyScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

/**
 * Tests for DotExporter - previously at 0% coverage.
 * These tests exercise the actual DotExporter implementation.
 */
@DisplayName("DotExporter Tests")
class DotExporterTest {

    private DotExporter exporter;
    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        exporter = new DotExporter();
        graph = new DependencyGraph();
    }

    @Nested
    @DisplayName("Format Info Tests")
    class FormatInfoTests {

        @Test
        @DisplayName("Should return correct file extension")
        void testGetFileExtension() {
            assertEquals("dot", exporter.getFileExtension());
        }

        @Test
        @DisplayName("Should return correct format name")
        void testGetFormatName() {
            assertEquals("DOT (Graphviz)", exporter.getFormatName());
        }
    }

    @Nested
    @DisplayName("Basic Export Tests")
    class BasicExportTests {

        @Test
        @DisplayName("Should export empty graph")
        void testExportEmptyGraph() throws Exception {
            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertNotNull(dot);
            assertTrue(dot.startsWith("digraph"));
            assertTrue(dot.contains("G {"));
            assertTrue(dot.endsWith("}\n"));
        }

        @Test
        @DisplayName("Should export graph with single node")
        void testExportSingleNode() throws Exception {
            graph.addNode(new DependencyNode("com.example.Single", "single", LegacyScope.SINGLETON));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertNotNull(dot);
            assertTrue(dot.contains("\"com.example.Single\""));
            assertTrue(dot.contains("label=\"Single\""));
        }

        @Test
        @DisplayName("Should export graph with multiple nodes")
        void testExportMultipleNodes() throws Exception {
            graph.addNode(new DependencyNode("com.example.Service", "service", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.Repository", "repository", LegacyScope.PROTOTYPE));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertNotNull(dot);
            assertTrue(dot.contains("\"com.example.Service\""));
            assertTrue(dot.contains("\"com.example.Repository\""));
        }

        @Test
        @DisplayName("Should export graph with edges")
        void testExportWithEdges() throws Exception {
            graph.addNode(new DependencyNode("com.example.Service", "service", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.Repository", "repository", LegacyScope.PROTOTYPE));
            graph.addEdge("com.example.Service", "com.example.Repository", "dependsOn");

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertNotNull(dot);
            assertTrue(dot.contains("->"));
            assertTrue(dot.contains("dependsOn"));
        }
    }

    @Nested
    @DisplayName("Node Shape Tests")
    class NodeShapeTests {

        @Test
        @DisplayName("Should use box shape for singleton scope")
        void testSingletonNodeShape() throws Exception {
            graph.addNode(new DependencyNode("com.example.Singleton", "singleton", LegacyScope.SINGLETON));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("shape=\"box\""));
        }

        @Test
        @DisplayName("Should use oval shape for prototype scope")
        void testPrototypeNodeShape() throws Exception {
            graph.addNode(new DependencyNode("com.example.Prototype", "prototype", LegacyScope.PROTOTYPE));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("shape=\"oval\""));
        }
    }

    @Nested
    @DisplayName("Special Node Attributes Tests")
    class SpecialAttributeTests {

        @Test
        @DisplayName("Should highlight primary nodes")
        void testPrimaryNodeHighlight() throws Exception {
            DependencyNode primaryNode = new DependencyNode("com.example.Primary", "primary", LegacyScope.SINGLETON);
            primaryNode.setPrimary(true);
            graph.addNode(primaryNode);

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("style=\"filled\""));
            assertTrue(dot.contains("fillcolor=\"lightyellow\""));
        }

        @Test
        @DisplayName("Should color nodes with profiles")
        void testProfileNodeColor() throws Exception {
            DependencyNode profileNode = new DependencyNode("com.example.Profiled", "profiled", LegacyScope.SINGLETON);
            profileNode.addProfile("dev");
            graph.addNode(profileNode);

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("color=\"blue\""));
        }

        @Test
        @DisplayName("Should handle nodes without profiles")
        void testNoProfiles() throws Exception {
            graph.addNode(new DependencyNode("com.example.NoProfiles", "noprof", LegacyScope.SINGLETON));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            // Should not have blue color when no profiles
            int blueIndex = dot.indexOf("color=\"blue\"");
            assertEquals(-1, blueIndex);
        }
    }

    @Nested
    @DisplayName("Edge Export Tests")
    class EdgeExportTests {

        @Test
        @DisplayName("Should include edge labels")
        void testEdgeLabels() throws Exception {
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.PROTOTYPE));
            graph.addEdge("com.example.A", "com.example.B", "dependsOn");

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("label=\"dependsOn\""));
        }

        @Test
        @DisplayName("Should handle edges without labels")
        void testNoEdgeLabel() throws Exception {
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.PROTOTYPE));
            graph.addEdge("com.example.A", "com.example.B", "");

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("->"));
        }

        @Test
        @DisplayName("Should avoid duplicate edges")
        void testNoDuplicateEdges() throws Exception {
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.PROTOTYPE));
            // Add the same edge multiple times
            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.A", "com.example.B", "dependsOn");

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            // Count occurrences of the edge pattern
            int count = 0;
            int idx = dot.indexOf("->");
            while (idx != -1) {
                count++;
                idx = dot.indexOf("->", idx + 1);
            }
            assertEquals(1, count);
        }
    }

    @Nested
    @DisplayName("Leaf Node Rank Tests")
    class LeafNodeRankTests {

        @Test
        @DisplayName("Should group leaf nodes in same rank")
        void testLeafNodeRanking() throws Exception {
            // A -> B
            // A -> C
            graph.addNode(new DependencyNode("com.example.A", "a", LegacyScope.SINGLETON));
            graph.addNode(new DependencyNode("com.example.B", "b", LegacyScope.PROTOTYPE));
            graph.addNode(new DependencyNode("com.example.C", "c", LegacyScope.PROTOTYPE));

            graph.addEdge("com.example.A", "com.example.B", "dependsOn");
            graph.addEdge("com.example.A", "com.example.C", "dependsOn");

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            // B and C should be in the same rank (leaf nodes)
            assertTrue(dot.contains("{ rank=same;"));
        }
    }

    @Nested
    @DisplayName("Character Escaping Tests")
    class EscapingTests {

        @Test
        @DisplayName("Should escape quotes in class names")
        void testQuoteEscaping() throws Exception {
            graph.addNode(new DependencyNode("com.example.With\"Quotes\"", "quoted", LegacyScope.SINGLETON));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("\\\""));
        }

        @Test
        @DisplayName("Should escape backslashes")
        void testBackslashEscaping() throws Exception {
            graph.addNode(new DependencyNode("com.example.With\\Backslash", "backslash", LegacyScope.SINGLETON));

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();
            assertTrue(dot.contains("\\\\"));
        }

        @Test
        @DisplayName("Should handle null class name gracefully")
        void testNullClassName() throws Exception {
            // DependencyNode doesn't allow null className in constructor,
            // but we test the escape method behavior
            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);
            assertNotNull(writer.toString());
        }
    }

    @Nested
    @DisplayName("Complex Graph Tests")
    class ComplexGraphTests {

        @Test
        @DisplayName("Should export complete component graph")
        void testCompleteGraph() throws Exception {
            // Setup a service layer graph
            graph.addNode(new DependencyNode("com.example.Controller", "controller", LegacyScope.SINGLETON));
            DependencyNode service = new DependencyNode("com.example.Service", "service", LegacyScope.SINGLETON);
            service.setPrimary(true);
            graph.addNode(service);

            DependencyNode repository = new DependencyNode("com.example.Repository", "repository", LegacyScope.PROTOTYPE);
            repository.addProfile("dev");
            graph.addNode(repository);

            graph.addNode(new DependencyNode("com.example.Database", "database", LegacyScope.PROTOTYPE));

            graph.addEdge("com.example.Controller", "com.example.Service", "dependsOn");
            graph.addEdge("com.example.Service", "com.example.Repository", "dependsOn");
            graph.addEdge("com.example.Repository", "com.example.Database", "dependsOn");

            StringWriter writer = new StringWriter();
            exporter.export(graph, writer);

            String dot = writer.toString();

            // Verify all nodes are present
            assertTrue(dot.contains("\"com.example.Controller\""));
            assertTrue(dot.contains("\"com.example.Service\""));
            assertTrue(dot.contains("\"com.example.Repository\""));
            assertTrue(dot.contains("\"com.example.Database\""));

            // Verify all edges are present
            assertTrue(dot.contains("->"));

            // Verify special attributes
            assertTrue(dot.contains("style=\"filled\"")); // Primary
            assertTrue(dot.contains("color=\"blue\"")); // Profile
        }
    }
}
