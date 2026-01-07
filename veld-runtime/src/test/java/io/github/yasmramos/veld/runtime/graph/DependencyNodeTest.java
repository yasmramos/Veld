package io.github.yasmramos.veld.runtime.graph;

import io.github.yasmramos.veld.annotation.ScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests for DependencyNode - previously at 0% coverage.
 * These tests exercise the actual DependencyNode implementation.
 */
@DisplayName("DependencyNode Tests")
class DependencyNodeTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create node with class name, component name, and scope")
        void testConstructorWithAllParams() {
            DependencyNode node = new DependencyNode("com.example.MyClass", "myComponent", ScopeType.SINGLETON);

            assertEquals("com.example.MyClass", node.getClassName());
            assertEquals("myComponent", node.getComponentName());
            assertEquals(ScopeType.SINGLETON, node.getScope());
        }

        @Test
        @DisplayName("Should extract simple name from class name")
        void testSimpleNameExtraction() {
            DependencyNode node = new DependencyNode("com.example.outer.MyClass", "component", ScopeType.PROTOTYPE);

            assertEquals("MyClass", node.getSimpleName());
        }

        @Test
        @DisplayName("Should handle class name without package")
        void testSimpleClassName() {
            DependencyNode node = new DependencyNode("MyClass", "component", ScopeType.SINGLETON);

            assertEquals("MyClass", node.getClassName());
            assertEquals("MyClass", node.getSimpleName());
        }

        @Test
        @DisplayName("Should initialize with isPrimary as false")
        void testInitialPrimaryIsFalse() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertFalse(node.isPrimary());
        }
    }

    @Nested
    @DisplayName("Profile Management Tests")
    class ProfileTests {

        @Test
        @DisplayName("Should start with empty profiles")
        void testInitialProfilesEmpty() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertTrue(node.getProfiles().isEmpty());
        }

        @Test
        @DisplayName("Should add profile")
        void testAddProfile() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addProfile("dev");

            assertEquals(1, node.getProfiles().size());
            assertTrue(node.getProfiles().contains("dev"));
        }

        @Test
        @DisplayName("Should add multiple profiles")
        void testAddMultipleProfiles() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addProfile("dev");
            node.addProfile("test");

            assertEquals(2, node.getProfiles().size());
        }
    }

    @Nested
    @DisplayName("Dependency Management Tests")
    class DependencyTests {

        @Test
        @DisplayName("Should start with empty constructor dependencies")
        void testInitialConstructorDependenciesEmpty() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertTrue(node.getConstructorDependencies().isEmpty());
        }

        @Test
        @DisplayName("Should start with empty field dependencies")
        void testInitialFieldDependenciesEmpty() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertTrue(node.getFieldDependencies().isEmpty());
        }

        @Test
        @DisplayName("Should start with empty method dependencies")
        void testInitialMethodDependenciesEmpty() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertTrue(node.getMethodDependencies().isEmpty());
        }

        @Test
        @DisplayName("Should add constructor dependency")
        void testAddConstructorDependency() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addConstructorDependency("com.example.DependencyA");

            assertEquals(1, node.getConstructorDependencies().size());
            assertTrue(node.getConstructorDependencies().contains("com.example.DependencyA"));
        }

        @Test
        @DisplayName("Should add field dependency")
        void testAddFieldDependency() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addFieldDependency("com.example.DependencyB");

            assertEquals(1, node.getFieldDependencies().size());
            assertTrue(node.getFieldDependencies().contains("com.example.DependencyB"));
        }

        @Test
        @DisplayName("Should add method dependency")
        void testAddMethodDependency() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addMethodDependency("com.example.DependencyC");

            assertEquals(1, node.getMethodDependencies().size());
            assertTrue(node.getMethodDependencies().contains("com.example.DependencyC"));
        }

        @Test
        @DisplayName("Should return all dependencies combined")
        void testGetAllDependencies() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addConstructorDependency("com.example.ConstructorDep");
            node.addFieldDependency("com.example.FieldDep");
            node.addMethodDependency("com.example.MethodDep");

            List<String> allDeps = node.getAllDependencies();

            assertEquals(3, allDeps.size());
            assertTrue(allDeps.contains("com.example.ConstructorDep"));
            assertTrue(allDeps.contains("com.example.FieldDep"));
            assertTrue(allDeps.contains("com.example.MethodDep"));
        }
    }

    @Nested
    @DisplayName("Interface Management Tests")
    class InterfaceTests {

        @Test
        @DisplayName("Should start with empty interfaces")
        void testInitialInterfacesEmpty() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertTrue(node.getInterfaces().isEmpty());
        }

        @Test
        @DisplayName("Should add interface")
        void testAddInterface() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addInterface("com.example.MyInterface");

            assertEquals(1, node.getInterfaces().size());
            assertTrue(node.getInterfaces().contains("com.example.MyInterface"));
        }

        @Test
        @DisplayName("Should add multiple interfaces")
        void testAddMultipleInterfaces() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);
            node.addInterface("com.example.InterfaceA");
            node.addInterface("com.example.InterfaceB");

            assertEquals(2, node.getInterfaces().size());
        }
    }

    @Nested
    @DisplayName("Primary Status Tests")
    class PrimaryStatusTests {

        @Test
        @DisplayName("Should default to non-primary")
        void testDefaultNotPrimary() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            assertFalse(node.isPrimary());
        }

        @Test
        @DisplayName("Should be able to set primary status")
        void testSetPrimary() {
            DependencyNode node = new DependencyNode("com.example.Test", "test", ScopeType.SINGLETON);

            node.setPrimary(true);
            assertTrue(node.isPrimary());

            node.setPrimary(false);
            assertFalse(node.isPrimary());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should manage all properties together")
        void testAllProperties() {
            DependencyNode node = new DependencyNode("com.example.Component", "myComponent", ScopeType.PROTOTYPE);

            node.addProfile("dev");
            node.addConstructorDependency("com.example.Dep1");
            node.addFieldDependency("com.example.Dep2");
            node.addMethodDependency("com.example.Dep3");
            node.addInterface("com.example.Interface1");
            node.setPrimary(true);

            assertEquals("com.example.Component", node.getClassName());
            assertEquals("myComponent", node.getComponentName());
            assertEquals(ScopeType.PROTOTYPE, node.getScope());
            assertEquals(1, node.getProfiles().size());
            assertEquals(1, node.getConstructorDependencies().size());
            assertEquals(1, node.getFieldDependencies().size());
            assertEquals(1, node.getMethodDependencies().size());
            assertEquals(1, node.getInterfaces().size());
            assertTrue(node.isPrimary());
        }

        @Test
        @DisplayName("Should handle inner class names with $")
        void testInnerClassName() {
            DependencyNode node = new DependencyNode("com.example.Outer$Inner", "inner", ScopeType.SINGLETON);

            // The simple name extraction should handle $ signs
            assertEquals("Inner", node.getSimpleName());
        }
    }
}
