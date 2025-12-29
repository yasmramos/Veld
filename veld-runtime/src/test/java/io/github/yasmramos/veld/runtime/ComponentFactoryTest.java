package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.runtime.condition.ConditionContext;
import io.github.yasmramos.veld.runtime.graph.DependencyGraph;
import io.github.yasmramos.veld.runtime.graph.DependencyNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests that exercise the ACTUAL default methods in ComponentFactory interface.
 * These tests intentionally do NOT override the methods being tested,
 * allowing the default implementations to execute and be covered.
 */
@DisplayName("ComponentFactory Default Methods Tests")
class ComponentFactoryTest {

    @Nested
    @DisplayName("Default getIndex() Tests")
    class DefaultGetIndexTests {

        @Test
        @DisplayName("Should return -1 when getIndex is not overridden")
        void testDefaultGetIndex() {
            // Create factory without overriding getIndex()
            ComponentFactory<String> factory = new TestComponentFactoryWithoutIndex(
                0, "test", String.class, LegacyScope.SINGLETON
            );

            // This exercises the default getIndex() method in ComponentFactory
            assertEquals(-1, factory.getIndex());
        }

        @Test
        @DisplayName("Should return custom index when overridden")
        void testCustomGetIndex() {
            ComponentFactory<String> factory = new TestComponentFactoryWithIndex(
                0, "test", String.class, LegacyScope.SINGLETON, 42
            );

            assertEquals(42, factory.getIndex());
        }
    }

    @Nested
    @DisplayName("Default hasConditions() Tests")
    class DefaultHasConditionsTests {

        @Test
        @DisplayName("Should return false when conditions are not set")
        void testDefaultHasConditions() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutConditions(
                0, "test", String.class, LegacyScope.SINGLETON
            );

            // This exercises the default hasConditions() method
            assertFalse(factory.hasConditions());
        }
    }

    @Nested
    @DisplayName("Default evaluateConditions() Tests")
    class DefaultEvaluateConditionsTests {

        @Test
        @DisplayName("Should return true when no conditions are set")
        void testDefaultEvaluateConditions() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutConditions(
                0, "test", String.class, LegacyScope.SINGLETON
            );

            // This exercises the default evaluateConditions() method
            assertTrue(factory.evaluateConditions(null));
            assertTrue(factory.evaluateConditions(new ConditionContext(Thread.currentThread().getContextClassLoader())));
        }
    }

    // Helper classes that expose the default methods

    static class TestComponentFactory<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final LegacyScope scope;

        TestComponentFactory(int index, String name, Class<T> type, LegacyScope scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public LegacyScope getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override:
        // - getScopeId() -> tests ComponentFactory default
        // - hasConditions() -> tests ComponentFactory default
        // - evaluateConditions() -> tests ComponentFactory default
    }

    static class TestComponentFactoryWithoutIndex<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final LegacyScope scope;

        TestComponentFactoryWithoutIndex(int index, String name, Class<T> type, LegacyScope scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public LegacyScope getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getIndex()
    }

    static class TestComponentFactoryWithIndex<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final LegacyScope scope;
        private final int customIndex;

        TestComponentFactoryWithIndex(int index, String name, Class<T> type, LegacyScope scope, int customIndex) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.customIndex = customIndex;
        }

        @Override public int getIndex() { return customIndex; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public LegacyScope getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}
    }

    static class TestComponentFactoryWithoutConditions<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final LegacyScope scope;

        TestComponentFactoryWithoutConditions(int index, String name, Class<T> type, LegacyScope scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public LegacyScope getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override:
        // - hasConditions() -> tests ComponentFactory default
        // - evaluateConditions() -> tests ComponentFactory default
    }
}
