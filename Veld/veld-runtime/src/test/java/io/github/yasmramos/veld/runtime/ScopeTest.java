package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LegacyScope enum.
 */
@DisplayName("LegacyScope Tests")
class ScopeTest {
    
    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {
        
        @Test
        @DisplayName("Should have SINGLETON scope")
        void shouldHaveSingletonScope() {
            assertNotNull(LegacyScope.SINGLETON);
        }
        
        @Test
        @DisplayName("Should have PROTOTYPE scope")
        void shouldHavePrototypeScope() {
            assertNotNull(LegacyScope.PROTOTYPE);
        }
        
        @Test
        @DisplayName("Should have correct number of scopes")
        void shouldHaveCorrectNumberOfScopes() {
            LegacyScope[] scopes = LegacyScope.values();
            
            assertTrue(scopes.length >= 2);
        }
    }
    
    @Nested
    @DisplayName("Value Of Tests")
    class ValueOfTests {
        
        @Test
        @DisplayName("Should get scope by name")
        void shouldGetScopeByName() {
            assertEquals(LegacyScope.SINGLETON, LegacyScope.valueOf("SINGLETON"));
            assertEquals(LegacyScope.PROTOTYPE, LegacyScope.valueOf("PROTOTYPE"));
        }
        
        @Test
        @DisplayName("Should throw for invalid name")
        void shouldThrowForInvalidName() {
            assertThrows(IllegalArgumentException.class, () -> 
                LegacyScope.valueOf("INVALID"));
        }
    }
    
    @Nested
    @DisplayName("Name Tests")
    class NameTests {
        
        @Test
        @DisplayName("Should return correct name")
        void shouldReturnCorrectName() {
            assertEquals("SINGLETON", LegacyScope.SINGLETON.name());
            assertEquals("PROTOTYPE", LegacyScope.PROTOTYPE.name());
        }
    }
}
