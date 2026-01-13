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
        @DisplayName("Should have REQUEST scope")
        void shouldHaveRequestScope() {
            assertNotNull(ScopeType.REQUEST);
        }
        
        @Test
        @DisplayName("Should have SESSION scope")
        void shouldHaveSessionScope() {
            assertNotNull(ScopeType.SESSION);
        }
        
        @Test
        @DisplayName("Should have correct number of scopes")
        void shouldHaveCorrectNumberOfScopes() {
            ScopeType[] scopes = ScopeType.values();
            
            assertEquals(4, scopes.length);
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
            assertEquals(ScopeType.REQUEST, ScopeType.valueOf("REQUEST"));
            assertEquals(ScopeType.SESSION, ScopeType.valueOf("SESSION"));
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
            assertEquals("REQUEST", ScopeType.REQUEST.name());
            assertEquals("SESSION", ScopeType.SESSION.name());
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
        
        @Test
        @DisplayName("REQUEST should return 'request' scope ID")
        void requestShouldReturnRequestScopeId() {
            assertEquals("request", ScopeType.REQUEST.getScopeId());
        }
        
        @Test
        @DisplayName("SESSION should return 'session' scope ID")
        void sessionShouldReturnSessionScopeId() {
            assertEquals("session", ScopeType.SESSION.getScopeId());
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
        @DisplayName("Should return REQUEST for 'request'")
        void shouldReturnRequestForRequest() {
            assertEquals(ScopeType.REQUEST, ScopeType.fromScopeId("request"));
        }
        
        @Test
        @DisplayName("Should return SESSION for 'session'")
        void shouldReturnSessionForSession() {
            assertEquals(ScopeType.SESSION, ScopeType.fromScopeId("session"));
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
            assertEquals(ScopeType.REQUEST, ScopeType.fromScopeId("REQUEST"));
            assertEquals(ScopeType.REQUEST, ScopeType.fromScopeId("Request"));
            assertEquals(ScopeType.SESSION, ScopeType.fromScopeId("SESSION"));
            assertEquals(ScopeType.SESSION, ScopeType.fromScopeId("Session"));
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
        @DisplayName("Should return true for 'request'")
        void shouldReturnTrueForRequest() {
            assertTrue(ScopeType.isBuiltInScope("request"));
        }
        
        @Test
        @DisplayName("Should return true for 'session'")
        void shouldReturnTrueForSession() {
            assertTrue(ScopeType.isBuiltInScope("session"));
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
    
    @Nested
    @DisplayName("Is Web Scope Tests")
    class IsWebScopeTests {
        
        @Test
        @DisplayName("REQUEST should be a web scope")
        void requestShouldBeWebScope() {
            assertTrue(ScopeType.REQUEST.isWebScope());
        }
        
        @Test
        @DisplayName("SESSION should be a web scope")
        void sessionShouldBeWebScope() {
            assertTrue(ScopeType.SESSION.isWebScope());
        }
        
        @Test
        @DisplayName("SINGLETON should not be a web scope")
        void singletonShouldNotBeWebScope() {
            assertFalse(ScopeType.SINGLETON.isWebScope());
        }
        
        @Test
        @DisplayName("PROTOTYPE should not be a web scope")
        void prototypeShouldNotBeWebScope() {
            assertFalse(ScopeType.PROTOTYPE.isWebScope());
        }
    }
}
