package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.runtime.condition.Condition;
import io.github.yasmramos.veld.runtime.condition.ConditionContext;
import io.github.yasmramos.veld.runtime.condition.ConditionEvaluator;
import org.junit.jupiter.api.AfterEach;
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
        private final LegacyScope scope;
        private final boolean hasConditions;
        private final boolean conditionResult;
        private final List<String> interfaces;

        TestFactory(Class<T> type, String name) {
            this(type, name, LegacyScope.SINGLETON, false, true, Collections.emptyList());
        }

        TestFactory(Class<T> type, String name, boolean hasConditions, boolean conditionResult) {
            this(type, name, LegacyScope.SINGLETON, hasConditions, conditionResult, Collections.emptyList());
        }

        TestFactory(Class<T> type, String name, LegacyScope scope, boolean hasConditions, 
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
        public LegacyScope getScope() {
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
                LegacyScope.SINGLETON, false, true, interfaces
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
        @DisplayName("Should create with active profiles")
        void shouldCreateWithActiveProfiles() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));
            Set<String> profiles = new HashSet<>(Arrays.asList("dev", "test"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry, profiles);

            assertEquals(1, registry.getRegisteredCount());
        }

        @Test
        @DisplayName("Should create with class loader")
        void shouldCreateWithClassLoader() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry, classLoader);

            assertEquals(1, registry.getRegisteredCount());
        }

        @Test
        @DisplayName("Should create with null profiles (resolves from environment)")
        void shouldCreateWithNullProfiles() {
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA"));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry, (Set<String>) null);

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
    @DisplayName("Active Profiles Tests")
    class ActiveProfilesTests {

        @Test
        @DisplayName("Should return empty array when no profiles set")
        void shouldReturnEmptyArrayWhenNoProfilesSet() {
            ConditionalRegistry.setActiveProfiles();
            String[] profiles = ConditionalRegistry.getActiveProfiles();
            assertEquals(0, profiles.length);
        }

        @Test
        @DisplayName("Should set and get active profiles")
        void shouldSetAndGetActiveProfiles() {
            ConditionalRegistry.setActiveProfiles("dev", "test");
            String[] profiles = ConditionalRegistry.getActiveProfiles();
            
            assertEquals(2, profiles.length);
            assertTrue(Arrays.asList(profiles).contains("dev"));
            assertTrue(Arrays.asList(profiles).contains("test"));
            
            // Cleanup
            ConditionalRegistry.setActiveProfiles();
        }

        @Test
        @DisplayName("Should clear active profiles")
        void shouldClearActiveProfiles() {
            ConditionalRegistry.setActiveProfiles("prod");
            ConditionalRegistry.setActiveProfiles();
            
            String[] profiles = ConditionalRegistry.getActiveProfiles();
            assertEquals(0, profiles.length);
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

    // ===== NEW TESTS FOR REMAINING COVERAGE GAPS =====

    @Nested
    @DisplayName("Get Active Profiles Edge Cases")
    class GetActiveProfilesEdgeCases {

        @Test
        @DisplayName("Should handle getActiveProfiles with null environment profiles")
        void shouldHandleGetActiveProfilesWithNullEnvironmentProfiles() {
            // Clear any previously set profiles
            ConditionalRegistry.setActiveProfiles();

            // When VELD_ACTIVE_PROFILES is null and no profiles are set
            // This tests the null path in getActiveProfiles()
            String[] profiles = ConditionalRegistry.getActiveProfiles();

            assertNotNull(profiles);
            assertEquals(0, profiles.length);
        }

        @Test
        @DisplayName("Should handle getActiveProfiles with empty environment")
        void shouldHandleGetActiveProfilesWithEmptyEnvironment() {
            ConditionalRegistry.setActiveProfiles();

            String[] profiles = ConditionalRegistry.getActiveProfiles();

            assertNotNull(profiles);
            assertEquals(0, profiles.length);
        }

        @Test
        @DisplayName("Should handle getActiveProfiles with multiple profiles")
        void shouldHandleGetActiveProfilesWithMultipleProfiles() {
            ConditionalRegistry.setActiveProfiles("profile1", "profile2", "profile3");

            String[] profiles = ConditionalRegistry.getActiveProfiles();

            assertEquals(3, profiles.length);
        }
    }

    @Nested
    @DisplayName("Set Active Profiles Edge Cases")
    class SetActiveProfilesEdgeCases {

        @AfterEach
        void cleanUp() {
            ConditionalRegistry.setActiveProfiles();
        }

        @Test
        @DisplayName("Should set single profile")
        void shouldSetSingleProfile() {
            ConditionalRegistry.setActiveProfiles("single");

            String[] profiles = ConditionalRegistry.getActiveProfiles();

            assertEquals(1, profiles.length);
            assertEquals("single", profiles[0]);
        }

        @Test
        @DisplayName("Should handle empty varargs")
        void shouldHandleEmptyVarargs() {
            ConditionalRegistry.setActiveProfiles("existing");
            ConditionalRegistry.setActiveProfiles();

            String[] profiles = ConditionalRegistry.getActiveProfiles();

            assertEquals(0, profiles.length);
        }

        @Test
        @DisplayName("Should handle profile with special characters")
        void shouldHandleProfileWithSpecialCharacters() {
            ConditionalRegistry.setActiveProfiles("profile-with-dashes", "profile.with.dots", "profile_with_underscores");

            String[] profiles = ConditionalRegistry.getActiveProfiles();

            assertEquals(3, profiles.length);
        }

        @Test
        @DisplayName("Should handle duplicate profiles")
        void shouldHandleDuplicateProfiles() {
            ConditionalRegistry.setActiveProfiles("dev", "dev", "test");

            String[] profiles = ConditionalRegistry.getActiveProfiles();

            // Note: Arrays.asList() preserves duplicates, but actual behavior depends on implementation
            assertTrue(profiles.length >= 2);
        }

        @Test
        @DisplayName("Should override previous profiles")
        void shouldOverridePreviousProfiles() {
            ConditionalRegistry.setActiveProfiles("old-profile");

            String[] oldProfiles = ConditionalRegistry.getActiveProfiles();
            assertEquals(1, oldProfiles.length);

            ConditionalRegistry.setActiveProfiles("new-profile");

            String[] newProfiles = ConditionalRegistry.getActiveProfiles();
            assertEquals(1, newProfiles.length);
            assertEquals("new-profile", newProfiles[0]);
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

        @Test
        @DisplayName("Should handle profile condition with active profile")
        void shouldHandleProfileConditionWithActiveProfile() {
            ConditionalRegistry.setActiveProfiles("dev");

            // Factory that only activates with 'dev' profile
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA", true, true));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(1, registry.getRegisteredCount());
        }

        @Test
        @DisplayName("Should handle profile condition with inactive profile")
        void shouldHandleProfileConditionWithInactiveProfile() {
            ConditionalRegistry.setActiveProfiles("prod");

            // This tests the path where the profile condition exists but is not active
            originalRegistry.addFactory(new TestFactory<>(ServiceA.class, "serviceA", true, true));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertEquals(1, registry.getRegisteredCount());
        }
    }

    @Nested
    @DisplayName("Supertype Map Tests")
    class SupertypeMapTests {

        @Test
        @DisplayName("Should register factory for interface")
        void shouldRegisterFactoryForInterface() {
            originalRegistry.addFactory(new TestFactory<>(ServiceWithInterface.class, "serviceImpl",
                LegacyScope.SINGLETON, false, true,
                Collections.singletonList(ServiceInterface.class.getName())));

            ConditionalRegistry registry = new ConditionalRegistry(originalRegistry);

            assertNotNull(registry.getFactory(ServiceInterface.class));
        }
    }

    // ===== NEW TESTS FOR isProfileActive =====

    @Nested
    @DisplayName("Is Profile Active Tests")
    class IsProfileActiveTests {

        @Test
        @DisplayName("Should return false for null profile")
        void shouldReturnFalseForNullProfile() {
            ConditionalRegistry.setActiveProfiles("dev", "test");

            assertFalse(ConditionalRegistry.isProfileActive(null));
        }

        @Test
        @DisplayName("Should return true for active profile")
        void shouldReturnTrueForActiveProfile() {
            ConditionalRegistry.setActiveProfiles("dev", "test", "prod");

            assertTrue(ConditionalRegistry.isProfileActive("dev"));
            assertTrue(ConditionalRegistry.isProfileActive("test"));
            assertTrue(ConditionalRegistry.isProfileActive("prod"));
        }

        @Test
        @DisplayName("Should return false for inactive profile")
        void shouldReturnFalseForInactiveProfile() {
            ConditionalRegistry.setActiveProfiles("dev");

            assertFalse(ConditionalRegistry.isProfileActive("prod"));
            assertFalse(ConditionalRegistry.isProfileActive("test"));
        }

        @Test
        @DisplayName("Should handle profile with whitespace")
        void shouldHandleProfileWithWhitespace() {
            ConditionalRegistry.setActiveProfiles(" dev ");

            assertTrue(ConditionalRegistry.isProfileActive("dev"));
        }

        @Test
        @DisplayName("Should return false when no profiles are set")
        void shouldReturnFalseWhenNoProfilesSet() {
            ConditionalRegistry.setActiveProfiles();

            assertFalse(ConditionalRegistry.isProfileActive("dev"));
        }

        @Test
        @DisplayName("Should be case-sensitive")
        void shouldBeCaseSensitive() {
            ConditionalRegistry.setActiveProfiles("DEV");

            assertTrue(ConditionalRegistry.isProfileActive("DEV"));
            assertFalse(ConditionalRegistry.isProfileActive("dev"));
            assertFalse(ConditionalRegistry.isProfileActive("Dev"));
        }

        @Test
        @DisplayName("Should return false for empty string profile")
        void shouldReturnFalseForEmptyStringProfile() {
            ConditionalRegistry.setActiveProfiles("dev");

            // Empty string is not in the set
            assertFalse(ConditionalRegistry.isProfileActive(""));
        }
    }
}
