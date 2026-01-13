package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.runtime.ComponentFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScopeRegistry implementation.
 */
@DisplayName("ScopeRegistry Tests")
class ScopeRegistryTest {
    
    @BeforeEach
    void setUp() {
        // Reset the registry before each test
        ScopeRegistry.reset();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        ScopeRegistry.reset();
    }
    
    @Nested
    @DisplayName("Built-in Scopes Tests")
    class BuiltInScopesTests {
        
        @Test
        @DisplayName("Should contain singleton scope")
        void shouldContainSingletonScope() {
            assertTrue(ScopeRegistry.contains(SingletonScope.SCOPE_ID));
        }
        
        @Test
        @DisplayName("Should contain prototype scope")
        void shouldContainPrototypeScope() {
            assertTrue(ScopeRegistry.contains(PrototypeScope.SCOPE_ID));
        }
        
        @Test
        @DisplayName("Should return singleton scope instance")
        void shouldReturnSingletonScopeInstance() {
            Scope scope = ScopeRegistry.get(SingletonScope.SCOPE_ID);
            
            assertNotNull(scope);
            assertEquals(SingletonScope.SCOPE_ID, scope.getId());
        }
        
        @Test
        @DisplayName("Should return prototype scope instance")
        void shouldReturnPrototypeScopeInstance() {
            Scope scope = ScopeRegistry.get(PrototypeScope.SCOPE_ID);
            
            assertNotNull(scope);
            assertEquals(PrototypeScope.SCOPE_ID, scope.getId());
        }
        
        @Test
        @DisplayName("Should return same singleton instance on multiple calls")
        void shouldReturnSameSingletonInstanceOnMultipleCalls() {
            Scope scope1 = ScopeRegistry.get(SingletonScope.SCOPE_ID);
            Scope scope2 = ScopeRegistry.get(SingletonScope.SCOPE_ID);
            
            assertSame(scope1, scope2);
        }
        
        @Test
        @DisplayName("Should return different prototype instances")
        void shouldReturnDifferentPrototypeInstances() {
            Scope scope1 = ScopeRegistry.get(PrototypeScope.SCOPE_ID);
            Scope scope2 = ScopeRegistry.get(PrototypeScope.SCOPE_ID);
            
            // Prototype scopes should be created fresh each time
            assertNotSame(scope1, scope2);
        }
    }
    
    @Nested
    @DisplayName("Get Or Null Tests")
    class GetOrNullTests {
        
        @Test
        @DisplayName("Should return scope when exists")
        void shouldReturnScopeWhenExists() {
            Scope scope = ScopeRegistry.getOrNull(SingletonScope.SCOPE_ID);
            
            assertNotNull(scope);
        }
        
        @Test
        @DisplayName("Should return null for non-existent scope")
        void shouldReturnNullForNonExistentScope() {
            Scope scope = ScopeRegistry.getOrNull("non-existent");
            
            assertNull(scope);
        }
    }
    
    @Nested
    @DisplayName("No Such Scope Exception Tests")
    class NoSuchScopeExceptionTests {
        
        @Test
        @DisplayName("Should throw exception for unknown scope")
        void shouldThrowExceptionForUnknownScope() {
            assertThrows(ScopeRegistry.NoSuchScopeException.class, () -> {
                ScopeRegistry.get("unknown-scope");
            });
        }
        
        @Test
        @DisplayName("Exception should contain helpful message")
        void exceptionShouldContainHelpfulMessage() {
            try {
                ScopeRegistry.get("unknown-scope");
                fail("Expected exception");
            } catch (ScopeRegistry.NoSuchScopeException e) {
                assertTrue(e.getMessage().contains("unknown-scope"));
                assertTrue(e.getMessage().contains("Available scopes"));
            }
        }
    }
    
    @Nested
    @DisplayName("Default Scope Tests")
    class DefaultScopeTests {
        
        @Test
        @DisplayName("Should return singleton as default")
        void shouldReturnSingletonAsDefault() {
            String defaultScope = ScopeRegistry.getDefaultScopeId();
            
            assertEquals(SingletonScope.SCOPE_ID, defaultScope);
        }
        
        @Test
        @DisplayName("Should set default scope")
        void shouldSetDefaultScope() {
            ScopeRegistry.setDefaultScope(PrototypeScope.SCOPE_ID);
            
            assertEquals(PrototypeScope.SCOPE_ID, ScopeRegistry.getDefaultScopeId());
        }
        
        @Test
        @DisplayName("Should throw when setting default to unknown scope")
        void shouldThrowWhenSettingDefaultToUnknownScope() {
            assertThrows(ScopeRegistry.NoSuchScopeException.class, () -> {
                ScopeRegistry.setDefaultScope("unknown");
            });
        }
    }
    
    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {
        
        @Test
        @DisplayName("Should return metadata for singleton scope")
        void shouldReturnMetadataForSingletonScope() {
            ScopeRegistry.ScopeMetadata metadata = ScopeRegistry.getMetadata(SingletonScope.SCOPE_ID);
            
            assertNotNull(metadata);
            assertEquals(SingletonScope.SCOPE_ID, metadata.getId());
            assertNotNull(metadata.getDisplayName());
            assertNotNull(metadata.getDescription());
        }
        
        @Test
        @DisplayName("Should return null metadata for unknown scope")
        void shouldReturnNullMetadataForUnknownScope() {
            ScopeRegistry.ScopeMetadata metadata = ScopeRegistry.getMetadata("unknown");
            
            assertNull(metadata);
        }
    }
    
    @Nested
    @DisplayName("Registered Scope IDs Tests")
    class RegisteredScopeIdsTests {
        
        @Test
        @DisplayName("Should return all registered scope IDs")
        void shouldReturnAllRegisteredScopeIds() {
            java.util.Set<String> scopeIds = ScopeRegistry.getRegisteredScopeIds();
            
            assertTrue(scopeIds.contains(SingletonScope.SCOPE_ID));
            assertTrue(scopeIds.contains(PrototypeScope.SCOPE_ID));
        }
        
        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSet() {
            java.util.Set<String> scopeIds = ScopeRegistry.getRegisteredScopeIds();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                scopeIds.add("new-scope");
            });
        }
    }
    
    @Nested
    @DisplayName("Custom Scope Registration Tests")
    class CustomScopeRegistrationTests {
        
        @Test
        @DisplayName("Should register custom scope")
        void shouldRegisterCustomScope() {
            String customScopeId = "custom";
            Scope customScope = new CustomTestScope();
            
            ScopeRegistry.register(customScopeId, () -> customScope, "Custom", "A custom test scope");
            
            assertTrue(ScopeRegistry.contains(customScopeId));
        }
        
        @Test
        @DisplayName("Should retrieve custom scope")
        void shouldRetrieveCustomScope() {
            String customScopeId = "custom";
            Scope customScope = new CustomTestScope();
            
            ScopeRegistry.register(customScopeId, () -> customScope, "Custom", "A custom test scope");
            
            Scope retrieved = ScopeRegistry.get(customScopeId);
            assertSame(customScope, retrieved);
        }
        
        @Test
        @DisplayName("Should throw when registering duplicate scope")
        void shouldThrowWhenRegisteringDuplicateScope() {
            assertThrows(IllegalArgumentException.class, () -> {
                ScopeRegistry.register(SingletonScope.SCOPE_ID, () -> new SingletonScope());
            });
        }
        
        @Test
        @DisplayName("Should throw for null scope ID")
        void shouldThrowForNullScopeId() {
            assertThrows(IllegalArgumentException.class, () -> {
                ScopeRegistry.register(null, () -> new SingletonScope());
            });
        }
        
        @Test
        @DisplayName("Should throw for empty scope ID")
        void shouldThrowForEmptyScopeId() {
            assertThrows(IllegalArgumentException.class, () -> {
                ScopeRegistry.register("", () -> new SingletonScope());
            });
        }
        
        @Test
        @DisplayName("Should throw for null factory")
        void shouldThrowForNullFactory() {
            assertThrows(IllegalArgumentException.class, () -> {
                ScopeRegistry.register("custom", null);
            });
        }
    }
    
    @Nested
    @DisplayName("Direct Scope Registration Tests")
    class DirectScopeRegistrationTests {
        
        @Test
        @DisplayName("Should register scope instance directly")
        void shouldRegisterScopeInstanceDirectly() {
            String customScopeId = "direct-custom";
            CustomTestScope customScope = new CustomTestScope();
            
            ScopeRegistry.register(customScope);
            
            assertTrue(ScopeRegistry.contains(customScope.getId()));
        }
        
        @Test
        @DisplayName("Should throw when registering null scope")
        void shouldThrowWhenRegisteringNullScope() {
            assertThrows(IllegalArgumentException.class, () -> {
                ScopeRegistry.register((Scope) null);
            });
        }
        
        @Test
        @DisplayName("Should throw when registering duplicate scope instance")
        void shouldThrowWhenRegisteringDuplicateScopeInstance() {
            CustomTestScope scope = new CustomTestScope();
            
            ScopeRegistry.register(scope);
            
            assertThrows(IllegalArgumentException.class, () -> {
                ScopeRegistry.register(scope);
            });
        }
    }
    
    @Nested
    @DisplayName("Destroy Tests")
    class DestroyTests {
        
        @Test
        @DisplayName("Should destroy all scopes")
        void shouldDestroyAllScopes() {
            // Get some scopes first
            ScopeRegistry.get(SingletonScope.SCOPE_ID);
            ScopeRegistry.get(PrototypeScope.SCOPE_ID);
            
            // Destroy
            ScopeRegistry.destroy();
            
            // Should not contain scopes after destroy
            assertFalse(ScopeRegistry.contains(SingletonScope.SCOPE_ID));
            assertFalse(ScopeRegistry.contains(PrototypeScope.SCOPE_ID));
        }
        
        @Test
        @DisplayName("Should recreate scopes after destroy")
        void shouldRecreateScopesAfterDestroy() {
            Scope scope1 = ScopeRegistry.get(SingletonScope.SCOPE_ID);
            
            ScopeRegistry.destroy();
            
            Scope scope2 = ScopeRegistry.get(SingletonScope.SCOPE_ID);
            
            assertNotSame(scope1, scope2);
        }
    }
    
    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {
        
        @Test
        @DisplayName("Should reset registry to initial state")
        void shouldResetRegistryToInitialState() {
            // Register custom scope
            ScopeRegistry.register("custom", () -> new CustomTestScope());
            
            // Reset
            ScopeRegistry.reset();
            
            // Custom scope should not exist
            assertFalse(ScopeRegistry.contains("custom"));
            
            // Built-in scopes should still exist
            assertTrue(ScopeRegistry.contains(SingletonScope.SCOPE_ID));
            assertTrue(ScopeRegistry.contains(PrototypeScope.SCOPE_ID));
        }
        
        @Test
        @DisplayName("Should reset default scope to singleton after reset")
        void shouldResetDefaultScopeToSingletonAfterReset() {
            ScopeRegistry.setDefaultScope(PrototypeScope.SCOPE_ID);
            
            ScopeRegistry.reset();
            
            assertEquals(SingletonScope.SCOPE_ID, ScopeRegistry.getDefaultScopeId());
        }
    }
    
    @Nested
    @DisplayName("Contains Tests")
    class ContainsTests {
        
        @Test
        @DisplayName("Should return true for built-in scopes")
        void shouldReturnTrueForBuiltInScopes() {
            assertTrue(ScopeRegistry.contains(SingletonScope.SCOPE_ID));
            assertTrue(ScopeRegistry.contains(PrototypeScope.SCOPE_ID));
        }
        
        @Test
        @DisplayName("Should return false for unknown scope")
        void shouldReturnFalseForUnknownScope() {
            assertFalse(ScopeRegistry.contains("unknown"));
        }
    }
    
    /**
     * Custom scope implementation for testing.
     */
    private static class CustomTestScope implements Scope {
        private final String id = "custom-test";
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public String getDisplayName() {
            return "Custom Test";
        }
        
        @Override
        public <T> T get(String name, ComponentFactory<T> factory) {
            return factory.create();
        }
        
        @Override
        public Object remove(String name) {
            return null;
        }
    }
}
