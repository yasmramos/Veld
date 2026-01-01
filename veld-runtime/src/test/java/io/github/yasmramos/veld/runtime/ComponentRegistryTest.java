package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.VeldException;
import io.github.yasmramos.veld.runtime.graph.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class ComponentRegistryTest {

    private TestRegistry registry;
    private ComponentFactory<String> stringFactory;
    private ComponentFactory<Integer> intFactory;

    @BeforeEach
    void setUp() {
        stringFactory = new TestFactory<>(0, "stringBean", String.class, LegacyScope.SINGLETON, false, () -> "Hello");
        intFactory = new TestFactory<>(1, "intBean", Integer.class, LegacyScope.PROTOTYPE, true, () -> 42);
        
        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(stringFactory);
        factories.add(intFactory);
        
        registry = new TestRegistry(factories);
    }

    @Test
    void testGetIndex() {
        assertEquals(0, registry.getIndex(String.class));
        assertEquals(1, registry.getIndex(Integer.class));
        assertEquals(-1, registry.getIndex(Double.class));
    }

    @Test
    void testGetIndexByName() {
        assertEquals(0, registry.getIndex("stringBean"));
        assertEquals(1, registry.getIndex("intBean"));
        assertEquals(-1, registry.getIndex("unknown"));
    }

    @Test
    void testGetComponentCount() {
        assertEquals(2, registry.getComponentCount());
    }

    @Test
    void testGetScope() {
        assertEquals(LegacyScope.SINGLETON, registry.getScope(0));
        assertEquals(LegacyScope.PROTOTYPE, registry.getScope(1));
        assertEquals(LegacyScope.SINGLETON, registry.getScope(-1));
        assertEquals(LegacyScope.SINGLETON, registry.getScope(100));
    }

    @Test
    void testIsLazy() {
        assertFalse(registry.isLazy(0));
        assertTrue(registry.isLazy(1));
        assertFalse(registry.isLazy(-1));
        assertFalse(registry.isLazy(100));
    }

    @Test
    void testCreate() {
        assertEquals("Hello", registry.<String>create(0));
        assertEquals(42, registry.<Integer>create(1));
    }

    @Test
    void testCreateInvalidIndex() {
        assertThrows(VeldException.class, () -> registry.create(-1));
        assertThrows(VeldException.class, () -> registry.create(100));
    }

    @Test
    void testGetIndicesForType() {
        int[] indices = registry.getIndicesForType(String.class);
        assertEquals(1, indices.length);
        assertEquals(0, indices[0]);
    }

    @Test
    void testGetPrimaryFactory_SingleFactory() {
        ComponentFactory<String> result = registry.getPrimaryFactory(String.class);
        assertNotNull(result);
        assertEquals("stringBean", result.getComponentName());
    }

    @Test
    void testGetPrimaryFactory_NotFound() {
        ComponentFactory<Double> result = registry.getPrimaryFactory(Double.class);
        assertNull(result);
    }

    @Test
    void testGetPrimaryFactory_WithPrimary() {
        ComponentFactory<String> primaryFactory = new TestFactory<>(2, "primaryString", String.class, LegacyScope.SINGLETON, false, () -> "Primary", true);
        ComponentFactory<String> nonPrimaryFactory = new TestFactory<>(3, "otherString", String.class, LegacyScope.SINGLETON, false, () -> "Other", false);
        
        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(primaryFactory);
        factories.add(nonPrimaryFactory);
        TestRegistry multiRegistry = new TestRegistry(factories);
        
        ComponentFactory<String> result = multiRegistry.getPrimaryFactory(String.class);
        assertNotNull(result);
        assertEquals("primaryString", result.getComponentName());
    }

    @Test
    void testGetPrimaryFactory_MultiplePrimaryThrows() {
        ComponentFactory<String> primary1 = new TestFactory<>(2, "primary1", String.class, LegacyScope.SINGLETON, false, () -> "P1", true);
        ComponentFactory<String> primary2 = new TestFactory<>(3, "primary2", String.class, LegacyScope.SINGLETON, false, () -> "P2", true);
        
        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(primary1);
        factories.add(primary2);
        TestRegistry multiRegistry = new TestRegistry(factories);
        
        assertThrows(VeldException.class, () -> multiRegistry.getPrimaryFactory(String.class));
    }

    @Test
    void testGetPrimaryFactory_MultipleNoPrimary() {
        ComponentFactory<String> f1 = new TestFactory<>(2, "s1", String.class, LegacyScope.SINGLETON, false, () -> "S1", false);
        ComponentFactory<String> f2 = new TestFactory<>(3, "s2", String.class, LegacyScope.SINGLETON, false, () -> "S2", false);
        
        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(f1);
        factories.add(f2);
        TestRegistry multiRegistry = new TestRegistry(factories);
        
        ComponentFactory<String> result = multiRegistry.getPrimaryFactory(String.class);
        assertNull(result);
    }

    @Test
    void testInvokePostConstruct() {
        registry.invokePostConstruct(0, "test");
        // Should not throw, just verify it can be called
    }

    @Test
    void testInvokePreDestroy() {
        registry.invokePreDestroy(0, "test");
        // Should not throw, just verify it can be called
    }

    @Test
    void testInvokePostConstruct_InvalidIndex() {
        registry.invokePostConstruct(-1, "test");
        registry.invokePostConstruct(100, "test");
        // Should not throw for invalid indices
    }

    @Test
    void testInvokePreDestroy_InvalidIndex() {
        registry.invokePreDestroy(-1, "test");
        registry.invokePreDestroy(100, "test");
        // Should not throw for invalid indices
    }

    // ===== NEW TESTS FOR PREVIOUSLY UNCOVERED METHODS =====

    @Test
    void testGetScopeId_ValidIndex() {
        assertEquals("singleton", registry.getScopeId(0));
        assertEquals("prototype", registry.getScopeId(1));
    }

    @Test
    void testGetScopeId_InvalidIndex_ReturnsSingleton() {
        assertEquals("singleton", registry.getScopeId(-1));
        assertEquals("singleton", registry.getScopeId(100));
    }

    @Test
    void testGetScopeId_CustomScope() {
        ComponentFactory<String> customScopeFactory = new TestFactory<>(2, "customScope", String.class, LegacyScope.SINGLETON, false, () -> "custom", false) {
            @Override
            public String getScopeId() {
                return "custom-scope-id";
            }
        };

        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(customScopeFactory);
        TestRegistry customRegistry = new TestRegistry(factories);

        assertEquals("custom-scope-id", customRegistry.getScopeId(0));
    }

    @Test
    void testBuildDependencyGraph_WithComponents() {
        DependencyGraph graph = registry.buildDependencyGraph();

        assertNotNull(graph);
        // Should have nodes for both components
        assertEquals(2, graph.getNodes().size());
    }

    @Test
    void testBuildDependencyGraph_EmptyRegistry() {
        List<ComponentFactory<?>> emptyFactories = new ArrayList<>();
        TestRegistry emptyRegistry = new TestRegistry(emptyFactories);

        DependencyGraph graph = emptyRegistry.buildDependencyGraph();

        assertNotNull(graph);
        assertEquals(0, graph.getNodes().size());
    }

    @Test
    void testBuildDependencyGraph_WithDependencies() {
        // Create factories with dependency types
        ComponentFactory<String> dependentFactory = new TestFactory<>(2, "dependent", String.class, LegacyScope.SINGLETON, false, () -> "dep", false) {
            @Override
            public List<String> getDependencyTypes() {
                return java.util.Arrays.asList("java.lang.String");
            }
        };

        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(dependentFactory);
        TestRegistry depRegistry = new TestRegistry(factories);

        DependencyGraph graph = depRegistry.buildDependencyGraph();

        assertNotNull(graph);
        assertEquals(1, graph.getNodes().size());
    }

    @Test
    void testBuildDependencyGraph_WithPrimaryFlag() {
        ComponentFactory<String> primaryFactory = new TestFactory<>(2, "primary", String.class, LegacyScope.SINGLETON, false, () -> "primary", true);

        List<ComponentFactory<?>> factories = new ArrayList<>();
        factories.add(primaryFactory);
        TestRegistry primaryRegistry = new TestRegistry(factories);

        DependencyGraph graph = primaryRegistry.buildDependencyGraph();

        assertNotNull(graph);
        assertEquals(1, graph.getNodes().size());
        // The node should be marked as primary
        assertTrue(graph.getNodes().iterator().next().isPrimary());
    }

    static class TestRegistry implements ComponentRegistry {
        private final List<ComponentFactory<?>> factories;

        TestRegistry(List<ComponentFactory<?>> factories) {
            this.factories = factories;
        }

        @Override
        public List<ComponentFactory<?>> getAllFactories() {
            return factories;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ComponentFactory<T> getFactory(Class<T> type) {
            for (ComponentFactory<?> f : factories) {
                if (f.getComponentType() == type) {
                    return (ComponentFactory<T>) f;
                }
            }
            return null;
        }

        @Override
        public ComponentFactory<?> getFactory(String name) {
            for (ComponentFactory<?> f : factories) {
                if (f.getComponentName().equals(name)) {
                    return f;
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) {
            List<ComponentFactory<? extends T>> result = new ArrayList<>();
            for (ComponentFactory<?> f : factories) {
                if (type.isAssignableFrom(f.getComponentType())) {
                    result.add((ComponentFactory<? extends T>) f);
                }
            }
            return result;
        }
    }

    static class TestFactory<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final LegacyScope scope;
        private final boolean lazy;
        private final Provider<T> supplier;
        private final boolean primary;

        TestFactory(int index, String name, Class<T> type, LegacyScope scope, boolean lazy, Provider<T> supplier) {
            this(index, name, type, scope, lazy, supplier, false);
        }

        TestFactory(int index, String name, Class<T> type, LegacyScope scope, boolean lazy, Provider<T> supplier, boolean primary) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.lazy = lazy;
            this.supplier = supplier;
            this.primary = primary;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public LegacyScope getScope() { return scope; }
        @Override public boolean isLazy() { return lazy; }
        @Override public boolean isPrimary() { return primary; }
        @Override public T create() { return supplier.get(); }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}
    }
}
