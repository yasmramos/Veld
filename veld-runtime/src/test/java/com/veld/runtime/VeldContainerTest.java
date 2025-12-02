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
}
