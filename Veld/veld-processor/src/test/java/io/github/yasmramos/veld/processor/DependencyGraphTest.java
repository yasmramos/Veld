package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyGraph class.
 * Tests cycle detection and topological sorting.
 */
@DisplayName("DependencyGraph Tests")
class DependencyGraphTest {
    
    private DependencyGraph graph;
    
    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }
    
    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {
        
        @Test
        @DisplayName("Should add component to graph")
        void shouldAddComponent() {
            graph.addComponent("com.example.ServiceA");
            
            assertTrue(graph.getComponents().contains("com.example.ServiceA"));
        }
        
        @Test
        @DisplayName("Should add dependency between components")
        void shouldAddDependency() {
            graph.addDependency("com.example.ServiceA", "com.example.ServiceB");
            
            assertTrue(graph.getComponents().contains("com.example.ServiceA"));
            assertTrue(graph.getComponents().contains("com.example.ServiceB"));
            assertTrue(graph.getDependencies("com.example.ServiceA").contains("com.example.ServiceB"));
        }
        
        @Test
        @DisplayName("Should handle empty graph")
        void shouldHandleEmptyGraph() {
            assertTrue(graph.getComponents().isEmpty());
            assertFalse(graph.detectCycle().isPresent());
        }
    }
    
    @Nested
    @DisplayName("Cycle Detection")
    class CycleDetection {
        
        @Test
        @DisplayName("Should detect simple cycle A -> B -> A")
        void shouldDetectSimpleCycle() {
            graph.addDependency("A", "B");
            graph.addDependency("B", "A");
            
            Optional<List<String>> cycle = graph.detectCycle();
            
            assertTrue(cycle.isPresent());
        }
        
        @Test
        @DisplayName("Should detect cycle A -> B -> C -> A")
        void shouldDetectThreeNodeCycle() {
            graph.addDependency("A", "B");
            graph.addDependency("B", "C");
            graph.addDependency("C", "A");
            
            Optional<List<String>> cycle = graph.detectCycle();
            
            assertTrue(cycle.isPresent());
            List<String> cyclePath = cycle.get();
            assertEquals("A", cyclePath.get(cyclePath.size() - 1)); // Cycle ends at A
        }
        
        @Test
        @DisplayName("Should not detect cycle in linear graph")
        void shouldNotDetectCycleInLinearGraph() {
            graph.addDependency("A", "B");
            graph.addDependency("B", "C");
            graph.addDependency("C", "D");
            
            Optional<List<String>> cycle = graph.detectCycle();
            
            assertFalse(cycle.isPresent());
        }
        
        @Test
        @DisplayName("Should not detect cycle in diamond graph")
        void shouldNotDetectCycleInDiamondGraph() {
            // Diamond: A -> B, A -> C, B -> D, C -> D
            graph.addDependency("A", "B");
            graph.addDependency("A", "C");
            graph.addDependency("B", "D");
            graph.addDependency("C", "D");
            
            Optional<List<String>> cycle = graph.detectCycle();
            
            assertFalse(cycle.isPresent());
        }
        
        @Test
        @DisplayName("Should detect self-loop")
        void shouldDetectSelfLoop() {
            graph.addDependency("A", "A");
            
            Optional<List<String>> cycle = graph.detectCycle();
            
            assertTrue(cycle.isPresent());
        }
    }
    
    @Nested
    @DisplayName("Cycle Formatting")
    class CycleFormatting {
        
        @Test
        @DisplayName("Should format cycle path correctly")
        void shouldFormatCyclePath() {
            List<String> path = List.of("com.example.A", "com.example.B", "com.example.A");
            
            String formatted = DependencyGraph.formatCycle(path);
            
            // Uses Unicode arrow character
            assertEquals("A → B → A", formatted);
        }
        
        @Test
        @DisplayName("Should handle empty path")
        void shouldHandleEmptyPath() {
            String formatted = DependencyGraph.formatCycle(List.of());
            
            assertEquals("", formatted);
        }
        
        @Test
        @DisplayName("Should handle null path")
        void shouldHandleNullPath() {
            String formatted = DependencyGraph.formatCycle(null);
            
            assertEquals("", formatted);
        }
    }
    
    @Nested
    @DisplayName("Topological Sort")
    class TopologicalSortTests {
        
        @Test
        @DisplayName("Should return correct topological order")
        void shouldReturnCorrectTopologicalOrder() {
            graph.addDependency("A", "B");
            graph.addDependency("A", "C");
            graph.addDependency("B", "D");
            graph.addDependency("C", "D");
            
            List<String> sorted = graph.topologicalSort();
            
            // A must come before B and C
            assertTrue(sorted.indexOf("A") < sorted.indexOf("B"));
            assertTrue(sorted.indexOf("A") < sorted.indexOf("C"));
            // B and C must come before D
            assertTrue(sorted.indexOf("B") < sorted.indexOf("D"));
            assertTrue(sorted.indexOf("C") < sorted.indexOf("D"));
        }
        
        @Test
        @DisplayName("Should throw exception for cyclic graph")
        void shouldThrowExceptionForCyclicGraph() {
            graph.addDependency("A", "B");
            graph.addDependency("B", "A");
            
            assertThrows(IllegalStateException.class, () -> graph.topologicalSort());
        }
        
        @Test
        @DisplayName("Should handle single component")
        void shouldHandleSingleComponent() {
            graph.addComponent("A");
            
            List<String> sorted = graph.topologicalSort();
            
            assertEquals(1, sorted.size());
            assertEquals("A", sorted.get(0));
        }
    }
    
    @Nested
    @DisplayName("Graph String Representation")
    class ToStringTests {

        @Test
        @DisplayName("Should produce readable toString output")
        void shouldProduceReadableToString() {
            graph.addDependency("com.example.ServiceA", "com.example.ServiceB");

            String output = graph.toString();

            assertTrue(output.contains("ServiceA"));
            assertTrue(output.contains("ServiceB"));
            // Uses Unicode arrow character
            assertTrue(output.contains("→"));
        }
    }

    @Nested
    @DisplayName("Factory Bean Cycle Detection")
    class FactoryBeanCycleDetection {

        @Test
        @DisplayName("Should detect cycle between two factory beans")
        void shouldDetectCycleBetweenFactoryBeans() {
            // BeanA returns type A and depends on B (parameter)
            // BeanB returns type B and depends on A (parameter)
            // This creates: A -> B -> A
            graph.addComponent("com.example.BeanA");
            graph.addComponent("com.example.BeanB");

            // BeanA depends on BeanB
            graph.addDependency("com.example.BeanA", "com.example.BeanB");
            // BeanB depends on BeanA
            graph.addDependency("com.example.BeanB", "com.example.BeanA");

            Optional<List<String>> cycle = graph.detectCycle();

            assertTrue(cycle.isPresent(), "Should detect cycle between BeanA and BeanB");
        }

        @Test
        @DisplayName("Should detect cycle in factory chain A -> B -> C -> A")
        void shouldDetectCycleInFactoryChain() {
            // Three factory beans in a cycle
            graph.addDependency("com.example.BeanA", "com.example.BeanB");
            graph.addDependency("com.example.BeanB", "com.example.BeanC");
            graph.addDependency("com.example.BeanC", "com.example.BeanA");

            Optional<List<String>> cycle = graph.detectCycle();

            assertTrue(cycle.isPresent(), "Should detect cycle A -> B -> C -> A");
        }

        @Test
        @DisplayName("Should not detect cycle when factory beans have no circular dependencies")
        void shouldNotDetectCycleWithNoCircularDependencies() {
            // BeanA depends on BeanB
            // BeanB depends on BeanC
            // BeanC has no dependencies
            graph.addDependency("com.example.BeanA", "com.example.BeanB");
            graph.addDependency("com.example.BeanB", "com.example.BeanC");

            Optional<List<String>> cycle = graph.detectCycle();

            assertFalse(cycle.isPresent(), "Should not detect cycle in linear dependency chain");
        }

        @Test
        @DisplayName("Should detect cycle involving component and factory bean")
        void shouldDetectCycleBetweenComponentAndFactoryBean() {
            // Regular component depends on factory bean
            graph.addDependency("com.example.RegularComponent", "com.example.FactoryBeanA");
            // Factory bean depends on regular component
            graph.addDependency("com.example.FactoryBeanA", "com.example.RegularComponent");

            Optional<List<String>> cycle = graph.detectCycle();

            assertTrue(cycle.isPresent(), "Should detect cycle between component and factory bean");
        }

        @Test
        @DisplayName("Should handle multiple factory beans with shared dependencies")
        void shouldHandleMultipleFactoriesWithSharedDeps() {
            // BeanA and BeanB both depend on SharedService
            graph.addDependency("com.example.BeanA", "com.example.SharedService");
            graph.addDependency("com.example.BeanB", "com.example.SharedService");

            Optional<List<String>> cycle = graph.detectCycle();

            assertFalse(cycle.isPresent(), "Should not detect cycle with shared dependencies");
        }

        @Test
        @DisplayName("Should detect self-referencing factory bean")
        void shouldDetectSelfReferencingBean() {
            // A factory bean that depends on itself
            graph.addDependency("com.example.SelfRefBean", "com.example.SelfRefBean");

            Optional<List<String>> cycle = graph.detectCycle();

            assertTrue(cycle.isPresent(), "Should detect self-referencing bean cycle");
        }
    }
}
