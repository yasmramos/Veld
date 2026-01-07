package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.annotation.ScopeType;
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
                0, "test", String.class, ScopeType.SINGLETON
            );

            // This exercises the default getIndex() method in ComponentFactory
            assertEquals(-1, factory.getIndex());
        }

        @Test
        @DisplayName("Should return custom index when overridden")
        void testCustomGetIndex() {
            ComponentFactory<String> factory = new TestComponentFactoryWithIndex(
                0, "test", String.class, ScopeType.SINGLETON, 42
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
                0, "test", String.class, ScopeType.SINGLETON
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
                0, "test", String.class, ScopeType.SINGLETON
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
        private final ScopeType scope;

        TestComponentFactory(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
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
        private final ScopeType scope;

        TestComponentFactoryWithoutIndex(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getIndex()
    }

    static class TestComponentFactoryWithIndex<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;
        private final int customIndex;

        TestComponentFactoryWithIndex(int index, String name, Class<T> type, ScopeType scope, int customIndex) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.customIndex = customIndex;
        }

        @Override public int getIndex() { return customIndex; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}
    }

    static class TestComponentFactoryWithoutConditions<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutConditions(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override:
        // - hasConditions() -> tests ComponentFactory default
        // - evaluateConditions() -> tests ComponentFactory default
    }

    // ===== NEW TESTS FOR PREVIOUSLY UNCOVERED DEFAULT METHODS =====

    @Nested
    @DisplayName("Default getScopeId() Tests")
    class DefaultGetScopeIdTests {

        @Test
        @DisplayName("Should return lowercase scope name for singleton")
        void testDefaultGetScopeId_singleton() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutScopeId(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertEquals("singleton", factory.getScopeId());
        }

        @Test
        @DisplayName("Should return lowercase scope name for prototype")
        void testDefaultGetScopeId_prototype() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutScopeId(
                0, "test", String.class, ScopeType.PROTOTYPE
            );
            assertEquals("prototype", factory.getScopeId());
        }

        @Test
        @DisplayName("Should return custom scope when overridden")
        void testCustomGetScopeId() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public String getScopeId() {
                    return "custom-scope";
                }
            };
            assertEquals("custom-scope", factory.getScopeId());
        }
    }

    @Nested
    @DisplayName("Default isLazy() Tests")
    class DefaultIsLazyTests {

        @Test
        @DisplayName("Should return false by default")
        void testDefaultIsLazy() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutIsLazy(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertFalse(factory.isLazy());
        }

        @Test
        @DisplayName("Should return true when overridden")
        void testCustomIsLazy() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public boolean isLazy() {
                    return true;
                }
            };
            assertTrue(factory.isLazy());
        }
    }

    @Nested
    @DisplayName("Default createConditionEvaluator() Tests")
    class DefaultCreateConditionEvaluatorTests {

        @Test
        @DisplayName("Should return null by default")
        void testDefaultCreateConditionEvaluator() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutConditionEvaluator(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertNull(factory.createConditionEvaluator());
        }
    }

    @Nested
    @DisplayName("Default getImplementedInterfaces() Tests")
    class DefaultGetImplementedInterfacesTests {

        @Test
        @DisplayName("Should return empty list by default")
        void testDefaultGetImplementedInterfaces() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutImplementedInterfaces(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertTrue(factory.getImplementedInterfaces().isEmpty());
        }
    }

    @Nested
    @DisplayName("Default isPrimary() Tests")
    class DefaultIsPrimaryTests {

        @Test
        @DisplayName("Should return false by default")
        void testDefaultIsPrimary() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutPrimary(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertFalse(factory.isPrimary());
        }

        @Test
        @DisplayName("Should return true when overridden")
        void testCustomIsPrimary() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public boolean isPrimary() {
                    return true;
                }
            };
            assertTrue(factory.isPrimary());
        }
    }

    @Nested
    @DisplayName("Default getOrder() Tests")
    class DefaultGetOrderTests {

        @Test
        @DisplayName("Should return 0 by default")
        void testDefaultGetOrder() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutOrder(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertEquals(0, factory.getOrder());
        }

        @Test
        @DisplayName("Should return custom order when overridden")
        void testCustomGetOrder() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public int getOrder() {
                    return 42;
                }
            };
            assertEquals(42, factory.getOrder());
        }
    }

    @Nested
    @DisplayName("Default getQualifier() Tests")
    class DefaultGetQualifierTests {

        @Test
        @DisplayName("Should return null by default")
        void testDefaultGetQualifier() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutQualifier(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertNull(factory.getQualifier());
        }

        @Test
        @DisplayName("Should return qualifier when overridden")
        void testCustomGetQualifier() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public String getQualifier() {
                    return "primary";
                }
            };
            assertEquals("primary", factory.getQualifier());
        }
    }

    @Nested
    @DisplayName("Default getFactoryClass() Tests")
    class DefaultGetFactoryClassTests {

        @Test
        @DisplayName("Should return null by default")
        void testDefaultGetFactoryClass() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutFactoryClass(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertNull(factory.getFactoryClass());
        }

        @Test
        @DisplayName("Should return factory class when overridden")
        void testCustomGetFactoryClass() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public Class<?> getFactoryClass() {
                    return ComponentFactoryTest.class;
                }
            };
            assertEquals(ComponentFactoryTest.class, factory.getFactoryClass());
        }
    }

    @Nested
    @DisplayName("Default getBeanMethodName() Tests")
    class DefaultGetBeanMethodNameTests {

        @Test
        @DisplayName("Should return null by default")
        void testDefaultGetBeanMethodName() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutBeanMethodName(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertNull(factory.getBeanMethodName());
        }

        @Test
        @DisplayName("Should return bean method name when overridden")
        void testCustomGetBeanMethodName() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public String getBeanMethodName() {
                    return "createMyBean";
                }
            };
            assertEquals("createMyBean", factory.getBeanMethodName());
        }
    }

    @Nested
    @DisplayName("Default getFactoryMethodParameters() Tests")
    class DefaultGetFactoryMethodParametersTests {

        @Test
        @DisplayName("Should return empty list by default")
        void testDefaultGetFactoryMethodParameters() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutFactoryMethodParameters(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertTrue(factory.getFactoryMethodParameters().isEmpty());
        }
    }

    @Nested
    @DisplayName("Default getDestructionDependencies() Tests")
    class DefaultGetDestructionDependenciesTests {

        @Test
        @DisplayName("Should return empty list by default")
        void testDefaultGetDestructionDependencies() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutDestructionDependencies(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertTrue(factory.getDestructionDependencies().isEmpty());
        }
    }

    @Nested
    @DisplayName("Default getDestroyOrder() Tests")
    class DefaultGetDestroyOrderTests {

        @Test
        @DisplayName("Should return 0 by default")
        void testDefaultGetDestroyOrder() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutDestroyOrder(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertEquals(0, factory.getDestroyOrder());
        }

        @Test
        @DisplayName("Should return custom order when overridden")
        void testCustomGetDestroyOrder() {
            ComponentFactory<String> factory = new ComponentFactory<String>() {
                @Override public String getComponentName() { return "test"; }
                @Override public Class<String> getComponentType() { return String.class; }
                @Override public ScopeType getScope() { return ScopeType.SINGLETON; }
                @Override public String create() { return null; }
                @Override public void invokePostConstruct(String instance) {}
                @Override public void invokePreDestroy(String instance) {}

                @Override
                public int getDestroyOrder() {
                    return 100;
                }
            };
            assertEquals(100, factory.getDestroyOrder());
        }
    }

    @Nested
    @DisplayName("Default getDependencyTypes() Tests")
    class DefaultGetDependencyTypesTests {

        @Test
        @DisplayName("Should return empty list by default")
        void testDefaultGetDependencyTypes() {
            ComponentFactory<String> factory = new TestComponentFactoryWithoutDependencyTypes(
                0, "test", String.class, ScopeType.SINGLETON
            );
            assertTrue(factory.getDependencyTypes().isEmpty());
        }
    }

    // Helper classes for testing default methods (intentionally don't override tested methods)

    static class TestComponentFactoryWithoutScopeId<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutScopeId(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getScopeId()
    }

    static class TestComponentFactoryWithoutIsLazy<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutIsLazy(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override isLazy()
    }

    static class TestComponentFactoryWithoutConditionEvaluator<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutConditionEvaluator(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override createConditionEvaluator()
    }

    static class TestComponentFactoryWithoutImplementedInterfaces<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutImplementedInterfaces(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getImplementedInterfaces()
    }

    static class TestComponentFactoryWithoutPrimary<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutPrimary(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override isPrimary()
    }

    static class TestComponentFactoryWithoutOrder<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutOrder(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getOrder()
    }

    static class TestComponentFactoryWithoutQualifier<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutQualifier(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getQualifier()
    }

    static class TestComponentFactoryWithoutFactoryClass<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutFactoryClass(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getFactoryClass()
    }

    static class TestComponentFactoryWithoutBeanMethodName<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutBeanMethodName(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getBeanMethodName()
    }

    static class TestComponentFactoryWithoutFactoryMethodParameters<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutFactoryMethodParameters(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getFactoryMethodParameters()
    }

    static class TestComponentFactoryWithoutDestructionDependencies<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutDestructionDependencies(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getDestructionDependencies()
    }

    static class TestComponentFactoryWithoutDestroyOrder<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutDestroyOrder(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getDestroyOrder()
    }

    static class TestComponentFactoryWithoutDependencyTypes<T> implements ComponentFactory<T> {
        private final int index;
        private final String name;
        private final Class<T> type;
        private final ScopeType scope;

        TestComponentFactoryWithoutDependencyTypes(int index, String name, Class<T> type, ScopeType scope) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.scope = scope;
        }

        @Override public int getIndex() { return index; }
        @Override public String getComponentName() { return name; }
        @Override public Class<T> getComponentType() { return type; }
        @Override public ScopeType getScope() { return scope; }
        @Override public String getScopeId() { return scope.name().toLowerCase(); }
        @Override public T create() { return null; }
        @Override public void invokePostConstruct(T instance) {}
        @Override public void invokePreDestroy(T instance) {}

        // Note: We intentionally do NOT override getDependencyTypes()
    }
}
