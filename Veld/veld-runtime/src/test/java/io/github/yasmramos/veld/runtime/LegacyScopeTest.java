package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.runtime.scope.Scope;
import io.github.yasmramos.veld.runtime.scope.SingletonScope;
import io.github.yasmramos.veld.runtime.scope.PrototypeScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LegacyScope enum.
 */
@DisplayName("LegacyScope Tests")
class LegacyScopeTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValuesTests {

        @Test
        @DisplayName("Should have SINGLETON enum value")
        void shouldHaveSingletonEnumValue() {
            LegacyScope scope = LegacyScope.SINGLETON;
            assertNotNull(scope);
            assertEquals("SINGLETON", scope.name());
        }

        @Test
        @DisplayName("Should have PROTOTYPE enum value")
        void shouldHavePrototypeEnumValue() {
            LegacyScope scope = LegacyScope.PROTOTYPE;
            assertNotNull(scope);
            assertEquals("PROTOTYPE", scope.name());
        }

        @Test
        @DisplayName("Should have two enum values")
        void shouldHaveTwoEnumValues() {
            assertEquals(2, LegacyScope.values().length);
        }
    }

    @Nested
    @DisplayName("toScope Method")
    class ToScopeTests {

        @Test
        @DisplayName("SINGLETON should convert to SingletonScope")
        void singletonShouldConvertToSingletonScope() {
            Scope scope = LegacyScope.SINGLETON.toScope();

            assertInstanceOf(SingletonScope.class, scope);
            assertEquals("singleton", scope.getId());
        }

        @Test
        @DisplayName("PROTOTYPE should convert to PrototypeScope")
        void prototypeShouldConvertToPrototypeScope() {
            Scope scope = LegacyScope.PROTOTYPE.toScope();

            assertInstanceOf(PrototypeScope.class, scope);
            assertEquals("prototype", scope.getId());
        }

        @Test
        @DisplayName("Each call should create new scope instance")
        void eachCallShouldCreateNewScopeInstance() {
            Scope scope1 = LegacyScope.SINGLETON.toScope();
            Scope scope2 = LegacyScope.SINGLETON.toScope();

            assertNotSame(scope1, scope2);
        }
    }

    @Nested
    @DisplayName("getScopeId Method")
    class GetScopeIdTests {

        @Test
        @DisplayName("SINGLETON should return 'singleton'")
        void singletonShouldReturnSingleton() {
            assertEquals("singleton", LegacyScope.SINGLETON.getScopeId());
        }

        @Test
        @DisplayName("PROTOTYPE should return 'prototype'")
        void prototypeShouldReturnPrototype() {
            assertEquals("prototype", LegacyScope.PROTOTYPE.getScopeId());
        }
    }

    @Nested
    @DisplayName("getDefault Method")
    class GetDefaultTests {

        @Test
        @DisplayName("getDefault should return SINGLETON")
        void getDefaultShouldReturnSingleton() {
            assertEquals(LegacyScope.SINGLETON, LegacyScope.getDefault());
        }
    }

    @Nested
    @DisplayName("fromId Method")
    class FromIdTests {

        @Test
        @DisplayName("Should return SINGLETON for 'singleton'")
        void shouldReturnSingletonForSingleton() {
            assertEquals(LegacyScope.SINGLETON, LegacyScope.fromId("singleton"));
        }

        @Test
        @DisplayName("Should return PROTOTYPE for 'prototype'")
        void shouldReturnPrototypeForPrototype() {
            assertEquals(LegacyScope.PROTOTYPE, LegacyScope.fromId("prototype"));
        }

        @Test
        @DisplayName("Should return null for unknown scope ID")
        void shouldReturnNullForUnknownScopeId() {
            assertNull(LegacyScope.fromId("unknown"));
        }

        @Test
        @DisplayName("Should handle case insensitivity")
        void shouldHandleCaseInsensitivity() {
            assertEquals(LegacyScope.SINGLETON, LegacyScope.fromId("SINGLETON"));
            assertEquals(LegacyScope.SINGLETON, LegacyScope.fromId("Singleton"));
            assertEquals(LegacyScope.PROTOTYPE, LegacyScope.fromId("PROTOTYPE"));
            assertEquals(LegacyScope.PROTOTYPE, LegacyScope.fromId("Prototype"));
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(LegacyScope.fromId(null));
        }
    }

    @Nested
    @DisplayName("isValidScopeId Method")
    class IsValidScopeIdTests {

        @Test
        @DisplayName("Should return true for 'singleton'")
        void shouldReturnTrueForSingleton() {
            assertTrue(LegacyScope.isValidScopeId("singleton"));
        }

        @Test
        @DisplayName("Should return true for 'prototype'")
        void shouldReturnTrueForPrototype() {
            assertTrue(LegacyScope.isValidScopeId("prototype"));
        }

        @Test
        @DisplayName("Should return false for unknown scope ID")
        void shouldReturnFalseForUnknownScopeId() {
            assertFalse(LegacyScope.isValidScopeId("unknown"));
        }

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(LegacyScope.isValidScopeId(null));
        }

        @Test
        @DisplayName("Should handle case insensitivity")
        void shouldHandleCaseInsensitivity() {
            assertTrue(LegacyScope.isValidScopeId("SINGLETON"));
            assertTrue(LegacyScope.isValidScopeId("Singleton"));
            assertTrue(LegacyScope.isValidScopeId("PROTOTYPE"));
            assertTrue(LegacyScope.isValidScopeId("Prototype"));
        }
    }
}
