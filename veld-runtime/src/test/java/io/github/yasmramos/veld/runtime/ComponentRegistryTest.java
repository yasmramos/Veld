package io.github.yasmramos.veld.runtime;

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
        stringFactory = new TestFactory<>(0, "stringBean", String.class, Scope.SINGLETON, false, () -> "Hello");
        intFactory = new TestFactory<>(1, "intBean", Integer.class, Scope.PROTOTYPE, true, () -> 42);
        
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
        assertEquals(Scope.SINGLETON, registry.getScope(0));
        assertEquals(Scope.PROTOTYPE, registry.getScope(1));
        assertEquals(Scope.SINGLETON, registry.getScope(-1));
        assertEquals(Scope.SINGLETON, registry.getScope(100));
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
        private final Scope scope;
        private final boolean lazy;
        private final Provider<T> supplier;

        TestFactory(int index, String name, Class<T> type, Scope scope, boolean lazy, Provider<T> supplier) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.lazy = lazy;
            this.supplier = supplier;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public Scope getScope() { return scope; }
        @Override public boolean isLazy() { return lazy; }
        @Override public T create() { return supplier.get(); }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}
    }
}
