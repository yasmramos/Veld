package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.annotation.ScopeType;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScopeType enum.
 */
@DisplayName("ScopeType Tests")
class ScopeTest {
    
    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {
        
        @Test
        @DisplayName("Should have SINGLETON scope")
        void shouldHaveSingletonScope() {
            assertNotNull(ScopeType.SINGLETON);
        }
        
        @Test
        @DisplayName("Should have PROTOTYPE scope")
        void shouldHavePrototypeScope() {
            assertNotNull(ScopeType.PROTOTYPE);
        }
        
        @Test
        @DisplayName("Should have correct number of scopes")
        void shouldHaveCorrectNumberOfScopes() {
            ScopeType[] scopes = ScopeType.values();
            
            assertEquals(2, scopes.length);
        }
    }
    
    @Nested
    @DisplayName("Value Of Tests")
    class ValueOfTests {
        
        @Test
        @DisplayName("Should get scope by name")
        void shouldGetScopeByName() {
            assertEquals(ScopeType.SINGLETON, ScopeType.valueOf("SINGLETON"));
            assertEquals(ScopeType.PROTOTYPE, ScopeType.valueOf("PROTOTYPE"));
        }
        
        @Test
        @DisplayName("Should throw for invalid name")
        void shouldThrowForInvalidName() {
            assertThrows(IllegalArgumentException.class, () -> 
                ScopeType.valueOf("INVALID"));
        }
    }
    
    @Nested
    @DisplayName("Name Tests")
    class NameTests {
        
        @Test
        @DisplayName("Should return correct name")
        void shouldReturnCorrectName() {
            assertEquals("SINGLETON", ScopeType.SINGLETON.name());
            assertEquals("PROTOTYPE", ScopeType.PROTOTYPE.name());
        }
    }
    
    @Nested
    @DisplayName("Scope ID Tests")
    class ScopeIdTests {
        
        @Test
        @DisplayName("SINGLETON should return 'singleton' scope ID")
        void singletonShouldReturnSingletonScopeId() {
            assertEquals("singleton", ScopeType.SINGLETON.getScopeId());
        }
        
        @Test
        @DisplayName("PROTOTYPE should return 'prototype' scope ID")
        void prototypeShouldReturnPrototypeScopeId() {
            assertEquals("prototype", ScopeType.PROTOTYPE.getScopeId());
        }
    }
    
    @Nested
    @DisplayName("From Scope ID Tests")
    class FromScopeIdTests {
        
        @Test
        @DisplayName("Should return SINGLETON for 'singleton'")
        void shouldReturnSingletonForSingleton() {
            assertEquals(ScopeType.SINGLETON, ScopeType.fromScopeId("singleton"));
        }
        
        @Test
        @DisplayName("Should return PROTOTYPE for 'prototype'")
        void shouldReturnPrototypeForPrototype() {
            assertEquals(ScopeType.PROTOTYPE, ScopeType.fromScopeId("prototype"));
        }
        
        @Test
        @DisplayName("Should return null for unknown scope ID")
        void shouldReturnNullForUnknownScopeId() {
            assertNull(ScopeType.fromScopeId("unknown"));
        }
        
        @Test
        @DisplayName("Should handle case insensitivity")
        void shouldHandleCaseInsensitivity() {
            assertEquals(ScopeType.SINGLETON, ScopeType.fromScopeId("SINGLETON"));
            assertEquals(ScopeType.SINGLETON, ScopeType.fromScopeId("Singleton"));
            assertEquals(ScopeType.PROTOTYPE, ScopeType.fromScopeId("PROTOTYPE"));
            assertEquals(ScopeType.PROTOTYPE, ScopeType.fromScopeId("Prototype"));
        }
        
        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(ScopeType.fromScopeId(null));
        }
    }
    
    @Nested
    @DisplayName("Default Tests")
    class DefaultTests {
        
        @Test
        @DisplayName("getDefault should return SINGLETON")
        void getDefaultShouldReturnSingleton() {
            assertEquals(ScopeType.SINGLETON, ScopeType.getDefault());
        }
    }
    
    @Nested
    @DisplayName("Is BuiltIn Scope Tests")
    class IsBuiltInScopeTests {
        
        @Test
        @DisplayName("Should return true for 'singleton'")
        void shouldReturnTrueForSingleton() {
            assertTrue(ScopeType.isBuiltInScope("singleton"));
        }
        
        @Test
        @DisplayName("Should return true for 'prototype'")
        void shouldReturnTrueForPrototype() {
            assertTrue(ScopeType.isBuiltInScope("prototype"));
        }
        
        @Test
        @DisplayName("Should return false for unknown scope ID")
        void shouldReturnFalseForUnknownScopeId() {
            assertFalse(ScopeType.isBuiltInScope("unknown"));
        }
        
        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(ScopeType.isBuiltInScope(null));
        }
    }
}
