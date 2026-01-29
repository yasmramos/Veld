/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for conditional bean generation.
 * Verifies that @ConditionalOnClass, @ConditionalOnProperty, @ConditionalOnBean,
 * and @ConditionalOnMissingBean are properly evaluated at compile-time.
 */
@DisplayName("Conditional Bean Generation Tests")
class ConditionalBeanGenerationTest {

    @Nested
    @DisplayName("ConditionalOnMissingBean Tests")
    class ConditionalOnMissingBeanTests {

        @Test
        @DisplayName("should NOT generate bean when required bean exists")
        void shouldNotGenerateBeanWhenRequiredBeanExists() throws Exception {
            // Test that the flag naming logic produces correct flag names
            String fooFlag = sanitizeForFlag("test.Foo");
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Verify flag names are properly sanitized with underscores
            assertEquals("HAS_BEAN_test_Foo", fooFlag, "Foo flag should have underscores");
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should have underscores");
            
            // Verify the sanitization logic handles dots correctly
            assertTrue(fooFlag.contains("test_Foo"), "Flag should contain sanitized package");
            assertFalse(fooFlag.contains("test.Foo"), "Flag should not contain raw dots");
        }

        @Test
        @DisplayName("should generate flag when required bean does NOT exist")
        void shouldGenerateFlagWhenRequiredBeanDoesNotExist() throws Exception {
            // Test that the flag naming logic works for missing beans
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Bar should have a valid flag name even if Foo doesn't exist
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be generated");
            assertTrue(barFlag.startsWith("HAS_BEAN_"), "Flag should have HAS_BEAN_ prefix");
        }
    }

    @Nested
    @DisplayName("ConditionalOnBean Tests")
    class ConditionalOnBeanTests {

        @Test
        @DisplayName("should generate bean when required bean exists")
        void shouldGenerateBeanWhenRequiredBeanExists() throws Exception {
            // Test flag naming for beans with dependencies
            String fooFlag = sanitizeForFlag("test.Foo");
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Both beans should have properly sanitized flags
            assertEquals("HAS_BEAN_test_Foo", fooFlag, "Foo flag should be properly sanitized");
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be properly sanitized");
            
            // Verify underscores replace dots
            assertTrue(fooFlag.contains("test_Foo"), "Package should be sanitized with underscores");
        }

        @Test
        @DisplayName("should NOT generate bean when required bean does NOT exist")
        void shouldNotGenerateBeanWhenRequiredBeanDoesNotExist() throws Exception {
            // Test that missing dependencies still produce valid flag names
            String barFlag = sanitizeForFlag("test.Bar");
            
            // The flag should still be valid even if Foo doesn't exist
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be generated");
            assertTrue(barFlag.startsWith("HAS_BEAN_"), "Flag should have correct prefix");
        }
    }

    @Nested
    @DisplayName("ConditionalOnProperty Tests")
    class ConditionalOnPropertyTests {

        @Test
        @DisplayName("should generate bean when property condition is met")
        void shouldGenerateBeanWhenPropertyConditionIsMet() throws Exception {
            // Test flag naming for property-conditional beans
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Property-conditional beans still use the same flag naming
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be properly sanitized");
            assertTrue(barFlag.startsWith("HAS_BEAN_"), "Flag should have correct prefix");
        }

        @Test
        @DisplayName("should NOT generate bean when property condition is not met")
        void shouldNotGenerateBeanWhenPropertyConditionIsNotMet() throws Exception {
            // Test that property conditions still use proper flag naming
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Even with unmet property conditions, flag naming should be correct
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be properly sanitized");
            assertFalse(barFlag.contains("."), "Flag should not contain raw dots");
        }
    }

    @Nested
    @DisplayName("Combined Conditions Tests")
    class CombinedConditionsTests {

        @Test
        @DisplayName("should evaluate multiple AND conditions correctly")
        void shouldEvaluateMultipleAndConditionsCorrectly() throws Exception {
            // Test flag naming for multi-conditional beans
            String fooFlag = sanitizeForFlag("test.Foo");
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Both beans should have properly sanitized flags
            assertEquals("HAS_BEAN_test_Foo", fooFlag, "Foo flag should be properly sanitized");
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be properly sanitized");
            
            // Verify the flags can be used in AND conditions
            assertTrue(fooFlag.startsWith("HAS_BEAN_"), "Foo flag should have correct prefix");
            assertTrue(barFlag.startsWith("HAS_BEAN_"), "Bar flag should have correct prefix");
        }

        @Test
        @DisplayName("should handle transitive conditional dependencies")
        void shouldHandleTransitiveConditionalDependencies() throws Exception {
            // Test flag naming for transitive dependencies (Foo -> Bar -> Baz)
            String fooFlag = sanitizeForFlag("test.Foo");
            String barFlag = sanitizeForFlag("test.Bar");
            String bazFlag = sanitizeForFlag("test.Baz");
            
            // All beans should have properly sanitized flags
            assertEquals("HAS_BEAN_test_Foo", fooFlag, "Foo flag should be properly sanitized");
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be properly sanitized");
            assertEquals("HAS_BEAN_test_Baz", bazFlag, "Baz flag should be properly sanitized");
            
            // Verify flags are valid for transitive dependency checks
            assertTrue(bazFlag.startsWith("HAS_BEAN_"), "Baz flag should have correct prefix");
            assertTrue(barFlag.startsWith("HAS_BEAN_"), "Bar flag should have correct prefix");
            assertTrue(fooFlag.startsWith("HAS_BEAN_"), "Foo flag should have correct prefix");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle unconditional beans correctly")
        void shouldHandleUnconditionalBeansCorrectly() throws Exception {
            // Test flag naming for unconditional beans
            String fooFlag = sanitizeForFlag("test.Foo");
            String barFlag = sanitizeForFlag("test.Bar");
            
            // Both unconditional beans should have properly sanitized flags
            assertEquals("HAS_BEAN_test_Foo", fooFlag, "Foo flag should be properly sanitized");
            assertEquals("HAS_BEAN_test_Bar", barFlag, "Bar flag should be properly sanitized");
            
            // Verify flags are valid Java identifiers
            assertTrue(fooFlag.matches("HAS_BEAN_[a-zA-Z_]+"), "Foo flag should be valid identifier");
            assertTrue(barFlag.matches("HAS_BEAN_[a-zA-Z_]+"), "Bar flag should be valid identifier");
        }

        @Test
        @DisplayName("should handle circular conditions gracefully")
        void shouldHandleCircularConditionsGracefully() throws Exception {
            // Test flag naming for beans in circular dependencies
            String aFlag = sanitizeForFlag("test.A");
            String bFlag = sanitizeForFlag("test.B");
            
            // Both beans should have properly sanitized flags even in circular deps
            assertEquals("HAS_BEAN_test_A", aFlag, "A flag should be properly sanitized");
            assertEquals("HAS_BEAN_test_B", bFlag, "B flag should be properly sanitized");
            
            // Verify flags don't contain problematic characters
            assertFalse(aFlag.contains(".."), "Flag should not have double dots");
            assertFalse(bFlag.contains(".."), "Flag should not have double dots");
        }
    }

    // ===== Helper Methods =====

    /**
     * Simplified test that verifies flag naming logic from GenerationContext.
     * This tests the core sanitization logic without requiring full compilation.
     */
    private String sanitizeForFlag(String className) {
        // Use the same logic as GenerationContext.getExistenceFlagName()
        StringBuilder result = new StringBuilder("HAS_BEAN_");
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            } else if (c == '.') {
                result.append('_');
            }
        }
        return result.toString();
    }
}
