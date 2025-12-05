package com.veld.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for VeldContainer.
 */
@DisplayName("VeldContainer Tests")
class VeldContainerTest {
    
    @Mock
    private ComponentRegistry mockRegistry;
    
    @Mock
    private ComponentFactory<TestService> mockFactory;
    
    private VeldContainer container;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockRegistry.getAllFactories()).thenReturn(List.of());
    }
    
    // Test service for mocking
    static class TestService {
        private String value = "test";
        
        public String getValue() {
            return value;
        }
    }
    
    @Nested
    @DisplayName("Component Retrieval")
    class ComponentRetrieval {
        
        @Test
        @DisplayName("Should get component by type")
        void shouldGetComponentByType() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            TestService result = container.get(TestService.class);
            
            assertNotNull(result);
            assertEquals("test", result.getValue());
        }
        
        @Test
        @DisplayName("Should throw exception for unknown type")
        void shouldThrowExceptionForUnknownType() {
            when(mockRegistry.getFactory(TestService.class)).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> container.get(TestService.class));
        }
        
        @Test
        @DisplayName("Should get component by name")
        void shouldGetComponentByName() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory("testService");
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            Object result = container.get("testService");
            
            assertNotNull(result);
            assertTrue(result instanceof TestService);
        }
        
        @Test
        @DisplayName("Should throw exception for unknown name")
        void shouldThrowExceptionForUnknownName() {
            when(mockRegistry.getFactory("unknown")).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> container.get("unknown"));
        }
    }
    
    @Nested
    @DisplayName("Scope Handling")
    class ScopeHandling {
        
        @Test
        @DisplayName("Singleton should return same instance")
        void singletonShouldReturnSameInstance() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            
            TestService first = container.get(TestService.class);
            TestService second = container.get(TestService.class);
            
            assertSame(first, second);
        }
        
        @Test
        @DisplayName("Prototype should return different instances")
        void prototypeShouldReturnDifferentInstances() {
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.PROTOTYPE);
            when(mockFactory.create(any())).thenAnswer(inv -> new TestService());
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of());
            
            container = new VeldContainer(mockRegistry);
            
            TestService first = container.get(TestService.class);
            TestService second = container.get(TestService.class);
            
            assertNotSame(first, second);
        }
    }
    
    @Nested
    @DisplayName("Container Lifecycle")
    class ContainerLifecycle {
        
        @Test
        @DisplayName("Should call postConstruct on singleton creation")
        void shouldCallPostConstructOnSingletonCreation() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            container.get(TestService.class);
            
            verify(mockFactory).invokePostConstruct(testService);
        }
        
        @Test
        @DisplayName("Should call preDestroy on container close")
        void shouldCallPreDestroyOnContainerClose() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            container.close();
            
            verify(mockFactory).invokePreDestroy(testService);
        }
        
        @Test
        @DisplayName("Should throw exception when accessing closed container")
        void shouldThrowExceptionWhenAccessingClosedContainer() {
            container = new VeldContainer(mockRegistry);
            container.close();
            
            assertThrows(VeldException.class, () -> container.get(TestService.class));
        }
        
        @Test
        @DisplayName("Double close should be safe")
        void doubleCloseShouldBeSafe() {
            container = new VeldContainer(mockRegistry);
            container.close();
            container.close(); // Should not throw
        }
    }
    
    @Nested
    @DisplayName("Contains Check")
    class ContainsCheck {
        
        @Test
        @DisplayName("Should return true for existing type")
        void shouldReturnTrueForExistingType() {
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            container = new VeldContainer(mockRegistry);
            
            assertTrue(container.contains(TestService.class));
        }
        
        @Test
        @DisplayName("Should return false for unknown type")
        void shouldReturnFalseForUnknownType() {
            when(mockRegistry.getFactory(TestService.class)).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertFalse(container.contains(TestService.class));
        }
        
        @Test
        @DisplayName("Should return true for existing name")
        void shouldReturnTrueForExistingName() {
            doReturn(mockFactory).when(mockRegistry).getFactory("testService");
            container = new VeldContainer(mockRegistry);
            
            assertTrue(container.contains("testService"));
        }
        
        @Test
        @DisplayName("Should return false for unknown name")
        void shouldReturnFalseForUnknownName() {
            when(mockRegistry.getFactory("unknown")).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertFalse(container.contains("unknown"));
        }
    }
    
    @Nested
    @DisplayName("Get By Type And Name")
    class GetByTypeAndName {
        
        @Test
        @DisplayName("Should get component by type and name")
        void shouldGetComponentByTypeAndName() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory("testService");
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            TestService result = container.get(TestService.class, "testService");
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should throw when name not found")
        void shouldThrowWhenNameNotFound() {
            when(mockRegistry.getFactory("unknown")).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> 
                container.get(TestService.class, "unknown"));
        }
        
        @Test
        @DisplayName("Should throw when type mismatch")
        void shouldThrowWhenTypeMismatch() {
            when(mockFactory.getComponentType()).thenReturn((Class) String.class);
            doReturn(mockFactory).when(mockRegistry).getFactory("stringService");
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> 
                container.get(TestService.class, "stringService"));
        }
    }
    
    @Nested
    @DisplayName("GetAll Tests")
    class GetAllTests {
        
        @Test
        @DisplayName("Should get all components of type")
        void shouldGetAllComponentsOfType() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            when(mockRegistry.getFactoriesForType(TestService.class)).thenReturn(List.of(mockFactory));
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            List<TestService> services = container.getAll(TestService.class);
            
            assertEquals(1, services.size());
        }
        
        @Test
        @DisplayName("Should return empty list when no components")
        void shouldReturnEmptyListWhenNoComponents() {
            when(mockRegistry.getFactoriesForType(TestService.class)).thenReturn(List.of());
            container = new VeldContainer(mockRegistry);
            
            List<TestService> services = container.getAll(TestService.class);
            
            assertTrue(services.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Provider Tests")
    class ProviderTests {
        
        @Test
        @DisplayName("Should get provider for type")
        void shouldGetProviderForType() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            Provider<TestService> provider = container.getProvider(TestService.class);
            
            assertNotNull(provider);
            assertNotNull(provider.get());
        }
        
        @Test
        @DisplayName("Should throw when provider for unknown type")
        void shouldThrowWhenProviderForUnknownType() {
            when(mockRegistry.getFactory(TestService.class)).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> 
                container.getProvider(TestService.class));
        }
        
        @Test
        @DisplayName("Should get provider by type and name")
        void shouldGetProviderByTypeAndName() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory("testService");
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            Provider<TestService> provider = container.getProvider(TestService.class, "testService");
            
            assertNotNull(provider.get());
        }
        
        @Test
        @DisplayName("Should throw when provider name not found")
        void shouldThrowWhenProviderNameNotFound() {
            when(mockRegistry.getFactory("unknown")).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> 
                container.getProvider(TestService.class, "unknown"));
        }
        
        @Test
        @DisplayName("Should throw when provider type mismatch")
        void shouldThrowWhenProviderTypeMismatch() {
            when(mockFactory.getComponentType()).thenReturn((Class) String.class);
            doReturn(mockFactory).when(mockRegistry).getFactory("stringService");
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> 
                container.getProvider(TestService.class, "stringService"));
        }
    }
    
    @Nested
    @DisplayName("TryGet Tests")
    class TryGetTests {
        
        @Test
        @DisplayName("Should return component when exists by type")
        void shouldReturnComponentWhenExistsByType() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            TestService result = container.tryGet(TestService.class);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should return null when not exists by type")
        void shouldReturnNullWhenNotExistsByType() {
            when(mockRegistry.getFactory(TestService.class)).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            TestService result = container.tryGet(TestService.class);
            
            assertNull(result);
        }
        
        @Test
        @DisplayName("Should return component when exists by name")
        void shouldReturnComponentWhenExistsByName() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory("testService");
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            TestService result = container.tryGet("testService");
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should return null when not exists by name")
        void shouldReturnNullWhenNotExistsByName() {
            when(mockRegistry.getFactory("unknown")).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            TestService result = container.tryGet("unknown");
            
            assertNull(result);
        }
    }
    
    @Nested
    @DisplayName("GetOptional Tests")
    class GetOptionalTests {
        
        @Test
        @DisplayName("Should return Optional with component when exists by type")
        void shouldReturnOptionalWithComponentByType() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            java.util.Optional<TestService> result = container.getOptional(TestService.class);
            
            assertTrue(result.isPresent());
        }
        
        @Test
        @DisplayName("Should return empty Optional when not exists by type")
        void shouldReturnEmptyOptionalByType() {
            when(mockRegistry.getFactory(TestService.class)).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            java.util.Optional<TestService> result = container.getOptional(TestService.class);
            
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should return Optional with component when exists by name")
        void shouldReturnOptionalWithComponentByName() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory("testService");
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            java.util.Optional<TestService> result = container.getOptional("testService");
            
            assertTrue(result.isPresent());
        }
        
        @Test
        @DisplayName("Should return empty Optional when not exists by name")
        void shouldReturnEmptyOptionalByName() {
            when(mockRegistry.getFactory("unknown")).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            java.util.Optional<TestService> result = container.getOptional("unknown");
            
            assertTrue(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("GetLazy Tests")
    class GetLazyTests {
        
        @Test
        @DisplayName("Should get lazy instance")
        void shouldGetLazyInstance() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            TestService result = container.getLazy(TestService.class);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should throw when lazy type not found")
        void shouldThrowWhenLazyTypeNotFound() {
            when(mockRegistry.getFactory(TestService.class)).thenReturn(null);
            container = new VeldContainer(mockRegistry);
            
            assertThrows(VeldException.class, () -> 
                container.getLazy(TestService.class));
        }
    }
    
    @Nested
    @DisplayName("Lazy Initialization Tests")
    class LazyInitializationTests {
        
        @Test
        @DisplayName("Should not initialize lazy singleton on container creation")
        void shouldNotInitializeLazySingletonOnCreation() {
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.isLazy()).thenReturn(true);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            
            verify(mockFactory, never()).create(any());
        }
        
        @Test
        @DisplayName("Should initialize lazy singleton on first access")
        void shouldInitializeLazySingletonOnFirstAccess() {
            TestService testService = new TestService();
            
            when(mockFactory.getComponentType()).thenReturn((Class) TestService.class);
            when(mockFactory.getComponentName()).thenReturn("testService");
            when(mockFactory.getScope()).thenReturn(Scope.SINGLETON);
            when(mockFactory.isLazy()).thenReturn(true);
            when(mockFactory.create(any())).thenReturn(testService);
            doReturn(mockFactory).when(mockRegistry).getFactory(TestService.class);
            when(mockRegistry.getAllFactories()).thenReturn(List.of(mockFactory));
            
            container = new VeldContainer(mockRegistry);
            container.get(TestService.class);
            
            verify(mockFactory).create(any());
        }
    }
    
    @Nested
    @DisplayName("Excluded Components Tests")
    class ExcludedComponentsTests {
        
        @Test
        @DisplayName("Should return empty list for regular registry")
        void shouldReturnEmptyListForRegularRegistry() {
            container = new VeldContainer(mockRegistry);
            
            List<String> excluded = container.getExcludedComponents();
            
            assertTrue(excluded.isEmpty());
        }
        
        @Test
        @DisplayName("Should return false for wasExcluded with regular registry")
        void shouldReturnFalseForWasExcludedWithRegularRegistry() {
            container = new VeldContainer(mockRegistry);
            
            assertFalse(container.wasExcluded("anyComponent"));
        }
    }
}
