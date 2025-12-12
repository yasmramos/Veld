package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Scope enum.
 */
@DisplayName("Scope Tests")
class ScopeTest {
    
    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {
        
        @Test
        @DisplayName("Should have SINGLETON scope")
        void shouldHaveSingletonScope() {
            assertNotNull(Scope.SINGLETON);
        }
        
        @Test
        @DisplayName("Should have PROTOTYPE scope")
        void shouldHavePrototypeScope() {
            assertNotNull(Scope.PROTOTYPE);
        }
        
        @Test
        @DisplayName("Should have correct number of scopes")
        void shouldHaveCorrectNumberOfScopes() {
            Scope[] scopes = Scope.values();
            
            assertTrue(scopes.length >= 2);
        }
    }
    
    @Nested
    @DisplayName("Value Of Tests")
    class ValueOfTests {
        
        @Test
        @DisplayName("Should get scope by name")
        void shouldGetScopeByName() {
            assertEquals(Scope.SINGLETON, Scope.valueOf("SINGLETON"));
            assertEquals(Scope.PROTOTYPE, Scope.valueOf("PROTOTYPE"));
        }
        
        @Test
        @DisplayName("Should throw for invalid name")
        void shouldThrowForInvalidName() {
            assertThrows(IllegalArgumentException.class, () -> 
                Scope.valueOf("INVALID"));
        }
    }
    
    @Nested
    @DisplayName("Name Tests")
    class NameTests {
        
        @Test
        @DisplayName("Should return correct name")
        void shouldReturnCorrectName() {
            assertEquals("SINGLETON", Scope.SINGLETON.name());
            assertEquals("PROTOTYPE", Scope.PROTOTYPE.name());
        }
    }
}
