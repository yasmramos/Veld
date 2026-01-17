package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.annotation.ScopeType;
import io.github.yasmramos.veld.runtime.condition.ConditionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConditionalRegistry class.
 */
@DisplayName("ConditionalRegistry Tests")
class ConditionalRegistryTest {

    // Test component classes
    static class ServiceA {}
    static class ServiceB {}
    static class ServiceC {}
    interface ServiceInterface {}
    static class ServiceWithInterface implements ServiceInterface {}

    // Simple factory implementation for testing
    static class TestFactory<T> implements ComponentFactory<T> {
        private final Class<T> type;
        private final String name;
        private final ScopeType scope;
        private final boolean hasConditions;
        private final boolean conditionResult;
        private final List<String> interfaces;

        TestFactory(Class<T> type, String name) {
            this(type, name, ScopeType.SINGLETON, false, true, Collections.emptyList());
        }

        TestFactory(Class<T> type, String name, boolean hasConditions, boolean conditionResult) {
            this(type, name, ScopeType.SINGLETON, hasConditions, conditionResult, Collections.emptyList());
        }

        TestFactory(Class<T> type, String name, ScopeType scope, boolean hasConditions,
                   boolean conditionResult, List<String> interfaces) {
            this.type = type;
            this.name = name;
            this.scope = scope;
            this.hasConditions = hasConditions;
            this.conditionResult = conditionResult;
            this.interfaces = interfaces;
        }

        @Override
        public T create() {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Class<T> getComponentType() {
            return type;
        }

        @Override
        public String getComponentName() {
            return name;
        }

        @Override
        public ScopeType getScope() {
            return scope;
        }

        @Override
        public void invokePostConstruct(T instance) {}

        @Override
        public void invokePreDestroy(T instance) {}

        @Override
        public boolean hasConditions() {
            return hasConditions;
        }

        @Override
        public boolean evaluateConditions(ConditionContext context) {
            return conditionResult;
        }

        @Override
        public List<String> getImplementedInterfaces() {
            return interfaces;
        }
    }

    // Simple registry implementation for testing
    static class TestRegistry implements ComponentRegistry {
        private final List<ComponentFactory<?>> factories = new ArrayList<>();

        void addFactory(ComponentFactory<?> factory) {
            factories.add(factory);
        }

        @Override
        public List<ComponentFactory<?>> getAllFactories() {
            return new ArrayList<>(factories);
        }

        @Override
        public <T> ComponentFactory<T> getFactory(Class<T> type) {
            return null;
        }

        @Override
        public ComponentFactory<?> getFactory(String name) {
            return null;
        }

        @Override
        public <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type) {
            return Collections.emptyList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getSingleton(Class<T> type) {
            ComponentFactory<T> factory = getFactory(type);
            if (factory == null) {
                return null;
            }
            return factory.create();
        }
    }

    private TestRegistry originalRegistry;

    @BeforeEach
    void setUp() {
        originalRegistry = new TestRegistry();
    }

    @Nested
    @DisplayName("Basic Registration Tests")
    class BasicRegistrationTests {

        @Test
        @DisplayName("Should register components without conditions")
        void shouldRegisterComponentsWithoutConditions() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));
            originalRegistry.addFactory(new TestFactory<>(ServiceB.class, "serviceB"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(2, registry.getRegisteredCount());
            assertEquals(0, registry.getExcludedCount());
            assertNotNull(registry.getFactory(ServiceA.class));
            assertNotNull(registry.getFactory(ServiceB.class));
        }

        @Test
        @DisplayName("Should retrieve factory by name")
        void shouldRetrieveFactoryByName() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "myService"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNotNull(registry.getFactory("myService"));
            assertEquals("myService", registry.getFactory("myService").getComponentName());
        }

        @Test
        @DisplayName("Should return null for unknown type")
        void shouldReturnNullForUnknownType() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNull(registry.getFactory(ServiceB.class));
        }

        @Test
        @DisplayName("Should return null for unknown name")
        void shouldReturnNullForUnknownName() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNull(registry.getFactory("unknownService"));
        }
    }

    @Nested
    @DisplayName("Conditional Registration Tests")
    class ConditionalRegistrationTests {

        @Test
        @DisplayName("Should register component when condition passes")
        void shouldRegisterWhenConditionPasses() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA", true, true));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(1, registry.getRegisteredCount());
            assertEquals(0, registry.getExcludedCount());
            assertNotNull(registry.getFactory(ServiceA.class));
        }

        @Test
        @DisplayName("Should exclude component when condition fails")
        void shouldExcludeWhenConditionFails() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA", true, false));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(0, registry.getRegisteredCount());
            assertEquals(1, registry.getExcludedCount());
            assertNull(registry.getFactory(ServiceA.class));
        }

        @Test
        @DisplayName("Should track excluded component names")
        void shouldTrackExcludedComponentNames() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "excludedService", true, false));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertTrue(registry.wasExcluded("excludedService"));
            assertFalse(registry.wasExcluded("otherService"));
            assertTrue(registry.getExcludedComponents().contains("excludedService"));
        }

        @Test
        @DisplayName("Should register mixed conditional and non-conditional")
        void shouldRegisterMixedComponents() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA")); // no condition
            originalRegistry.addFactory(new TestFactory<>(ServiceB.class, "serviceB", true, true)); // passes
            originalRegistry.addFactory(new TestFactory<>(ServiceC.class, "serviceC", true, false)); // fails

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(2, registry.getRegisteredCount());
            assertEquals(1, registry.getExcludedCount());
            assertNotNull(registry.getFactory(ServiceA.class));
            assertNotNull(registry.getFactory(ServiceB.class));
            assertNull(registry.getFactory(ServiceC.class));
        }
    }

    @Nested
    @DisplayName("Interface Registration Tests")
    class InterfaceRegistrationTests {

        @Test
        @DisplayName("Should register implemented interfaces")
        void shouldRegisterImplementedInterfaces() {
            List<String> interfaces = Collections.singletonList(ServiceInterface.class.getName());
            originalRegistry.addFactory(new TestFactory<>(
                ServiceWithInterface.class, "serviceWithInterface",
                ScopeType.SINGLETON, false, true, interfaces
            ));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNotNull(registry.getFactory(ServiceWithInterface.class));
            assertNotNull(registry.getFactory(ServiceInterface.class));
        }
    }

    @Nested
    @DisplayName("Factory List Tests")
    class FactoryListTests {

        @Test
        @DisplayName("Should return all factories")
        void shouldReturnAllFactories() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "a"));
            originalRegistry.addFactory(new TestFactory<>(ServiceB.class, "b"));
            originalRegistry.addFactory(new TestFactory<>(ServiceC.class, "c"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            List<ComponentFactory<?>> factories = registry.getAllFactories();
            assertEquals(3, factories.size());
        }

        @Test
        @DisplayName("Should return defensive copy of factories")
        void shouldReturnDefensiveCopyOfFactories() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "a"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);
            List<ComponentFactory<?>> factories = registry.getAllFactories();

            factories.clear();

            assertEquals(1, registry.getAllFactories().size());
        }

        @Test
        @DisplayName("Should get factories for type")
        void shouldGetFactoriesForType() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "a"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            List<ComponentFactory<? extends ServiceA>> factories =
                registry.getFactoriesForType(ServiceA.class);

            assertEquals(1, factories.size());
        }

        @Test
        @DisplayName("Should return empty list for unknown type")
        void shouldReturnEmptyListForUnknownType() {
            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            List<ComponentFactory<? extends ServiceB>> factories =
                registry.getFactoriesForType(ServiceB.class);

            assertTrue(factories.isEmpty());
        }
    }

    @Nested
    @DisplayName("Constructor Variants Tests")
    class ConstructorVariantsTests {

        @Test
        @DisplayName("Should create with class loader")
        void shouldCreateWithClassLoader() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry, classLoader);

            assertEquals(1, registry.getRegisteredCount());
        }
    }

    @Nested
    @DisplayName("Excluded Components Tests")
    class ExcludedComponentsTests {

        @Test
        @DisplayName("Should return defensive copy of excluded components")
        void shouldReturnDefensiveCopyOfExcludedComponents() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "excluded", true, false));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);
            List<String> excluded = registry.getExcludedComponents();

            excluded.clear();

            assertEquals(1, registry.getExcludedComponents().size());
        }
    }

    @Nested
    @DisplayName("Bean Existence Check Tests")
    class BeanExistenceTests {

        @Test
        @DisplayName("Should check if bean exists by type using getFactory")
        void shouldCheckIfBeanExistsByType() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));
            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNotNull(registry.getFactory(ServiceA.class));
            assertNull(registry.getFactory(ServiceB.class));
        }

        @Test
        @DisplayName("Should check if bean exists by name using getFactory")
        void shouldCheckIfBeanExistsByName() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "myBean"));
            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNotNull(registry.getFactory("myBean"));
            assertNull(registry.getFactory("otherBean"));
        }
    }

    @Nested
    @DisplayName("Was Excluded Tests")
    class WasExcludedTests {

        @Test
        @DisplayName("Should return true for excluded component")
        void shouldReturnTrueForExcludedComponent() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "excludedBean", true, false));
            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertTrue(registry.wasExcluded("excludedBean"));
        }

        @Test
        @DisplayName("Should return false for registered component")
        void shouldReturnFalseForRegisteredComponent() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "registeredBean"));
            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertFalse(registry.wasExcluded("registeredBean"));
        }
    }

    @Nested
    @DisplayName("ConditionalRegistry Constructor Edge Cases")
    class ConstructorEdgeCases {

        @Test
        @DisplayName("Should handle registry with no factories")
        void shouldHandleRegistryWithNoFactories() {
            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(0, registry.getRegisteredCount());
            assertEquals(0, registry.getExcludedCount());
        }

        @Test
        @DisplayName("Should handle all factories excluded")
        void shouldHandleAllFactoriesExcluded() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "a", true, false));
            originalRegistry.addFactory(new TestFactory<>(ServiceB.class, "b", true, false));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(0, registry.getRegisteredCount());
            assertEquals(2, registry.getExcludedCount());
        }
    }

    @Nested
    @DisplayName("Supertype Map Tests")
    class SupertypeMapTests {

        @Test
        @DisplayName("Should register factory for interface")
        void shouldRegisterFactoryForInterface() {
            originalRegistry.addFactory(new TestFactory<>(ServiceWithInterface.class, "serviceImpl",
                ScopeType.SINGLETON, false, true,
                Collections.singletonList(ServiceInterface.class.getName())));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNotNull(registry.getFactory(ServiceInterface.class));
        }
    }
}
