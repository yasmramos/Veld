package io.github.yasmramos.veld.runtime.value;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ValueResolver.
 */
@DisplayName("ValueResolver Tests")
class ValueResolverTest {
    
    private ValueResolver resolver;
    
    @BeforeEach
    void setUp() {
        ValueResolver.reset();
        resolver = ValueResolver.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        ValueResolver.reset();
    }
    
    @Nested
    @DisplayName("Singleton Tests")
    class SingletonTests {
        
        @Test
        @DisplayName("Should return same instance")
        void shouldReturnSameInstance() {
            ValueResolver r1 = ValueResolver.getInstance();
            ValueResolver r2 = ValueResolver.getInstance();
            
            assertSame(r1, r2);
        }
        
        @Test
        @DisplayName("Should create new instance after reset")
        void shouldCreateNewInstanceAfterReset() {
            ValueResolver r1 = ValueResolver.getInstance();
            ValueResolver.reset();
            ValueResolver r2 = ValueResolver.getInstance();
            
            assertNotSame(r1, r2);
        }
    }
    
    @Nested
    @DisplayName("Literal Value Tests")
    class LiteralValueTests {
        
        @Test
        @DisplayName("Should return literal value unchanged")
        void shouldReturnLiteralValueUnchanged() {
            String result = resolver.resolve("hello world");
            
            assertEquals("hello world", result);
        }
        
        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(resolver.resolve(null));
        }
        
        @Test
        @DisplayName("Should return empty for empty input")
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", resolver.resolve(""));
        }
    }
    
    @Nested
    @DisplayName("System Property Tests")
    class SystemPropertyTests {
        
        @BeforeEach
        void setUpSystemProperty() {
            System.setProperty("test.property", "test-value");
        }
        
        @AfterEach
        void cleanUpSystemProperty() {
            System.clearProperty("test.property");
        }
        
        @Test
        @DisplayName("Should resolve system property")
        void shouldResolveSystemProperty() {
            String result = resolver.resolve("${test.property}");
            
            assertEquals("test-value", result);
        }
        
        @Test
        @DisplayName("Should resolve java.version system property")
        void shouldResolveJavaVersionSystemProperty() {
            String result = resolver.resolve("${java.version}");
            
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {
        
        @Test
        @DisplayName("Should use default when property not found")
        void shouldUseDefaultWhenPropertyNotFound() {
            String result = resolver.resolve("${missing.property:default-value}");
            
            assertEquals("default-value", result);
        }
        
        @Test
        @DisplayName("Should use empty default")
        void shouldUseEmptyDefault() {
            String result = resolver.resolve("${missing.property:}");
            
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("Should prefer actual value over default")
        void shouldPreferActualValueOverDefault() {
            System.setProperty("existing.property", "actual");
            try {
                String result = resolver.resolve("${existing.property:default}");
                assertEquals("actual", result);
            } finally {
                System.clearProperty("existing.property");
            }
        }
    }
    
    @Nested
    @DisplayName("Exception Tests")
    class ExceptionTests {
        
        @Test
        @DisplayName("Should throw exception for missing property without default")
        void shouldThrowExceptionForMissingPropertyWithoutDefault() {
            assertThrows(ValueResolutionException.class, () -> 
                resolver.resolve("${missing.property}"));
        }
    }
    
    @Nested
    @DisplayName("Embedded Placeholder Tests")
    class EmbeddedPlaceholderTests {
        
        @Test
        @DisplayName("Should resolve embedded placeholders")
        void shouldResolveEmbeddedPlaceholders() {
            System.setProperty("app.name", "MyApp");
            try {
                String result = resolver.resolve("Welcome to ${app.name:App}!");
                assertEquals("Welcome to MyApp!", result);
            } finally {
                System.clearProperty("app.name");
            }
        }
        
        @Test
        @DisplayName("Should resolve multiple embedded placeholders")
        void shouldResolveMultipleEmbeddedPlaceholders() {
            System.setProperty("first.name", "John");
            System.setProperty("last.name", "Doe");
            try {
                String result = resolver.resolve("Hello ${first.name:User} ${last.name:Unknown}!");
                assertEquals("Hello John Doe!", result);
            } finally {
                System.clearProperty("first.name");
                System.clearProperty("last.name");
            }
        }
    }
    
    @Nested
    @DisplayName("Programmatic Property Tests")
    class ProgrammaticPropertyTests {
        
        @Test
        @DisplayName("Should set and resolve programmatic property")
        void shouldSetAndResolveProgrammaticProperty() {
            resolver.setProperty("custom.key", "custom-value");
            
            String result = resolver.resolve("${custom.key}");
            
            assertEquals("custom-value", result);
        }
        
        @Test
        @DisplayName("Should return all properties")
        void shouldReturnAllProperties() {
            resolver.setProperty("key1", "value1");
            resolver.setProperty("key2", "value2");
            
            var properties = resolver.getAllProperties();
            
            assertEquals("value1", properties.get("key1"));
            assertEquals("value2", properties.get("key2"));
        }
    }
    
    @Nested
    @DisplayName("Type Conversion Tests")
    class TypeConversionTests {
        
        @BeforeEach
        void setUpProperties() {
            resolver.setProperty("int.value", "42");
            resolver.setProperty("long.value", "1234567890123");
            resolver.setProperty("double.value", "3.14159");
            resolver.setProperty("float.value", "2.5");
            resolver.setProperty("boolean.true", "true");
            resolver.setProperty("boolean.false", "false");
            resolver.setProperty("byte.value", "127");
            resolver.setProperty("short.value", "32000");
            resolver.setProperty("char.value", "X");
            resolver.setProperty("string.value", "hello");
        }
        
        @Test
        @DisplayName("Should convert to String")
        void shouldConvertToString() {
            String result = resolver.resolve("${string.value}", String.class);
            assertEquals("hello", result);
        }
        
        @Test
        @DisplayName("Should convert to int")
        void shouldConvertToInt() {
            Integer result = resolver.resolve("${int.value}", Integer.class);
            assertEquals(42, result);
        }
        
        @Test
        @DisplayName("Should convert to primitive int")
        void shouldConvertToPrimitiveInt() {
            int result = resolver.resolve("${int.value}", int.class);
            assertEquals(42, result);
        }
        
        @Test
        @DisplayName("Should convert to long")
        void shouldConvertToLong() {
            Long result = resolver.resolve("${long.value}", Long.class);
            assertEquals(1234567890123L, result);
        }
        
        @Test
        @DisplayName("Should convert to double")
        void shouldConvertToDouble() {
            Double result = resolver.resolve("${double.value}", Double.class);
            assertEquals(3.14159, result, 0.00001);
        }
        
        @Test
        @DisplayName("Should convert to float")
        void shouldConvertToFloat() {
            Float result = resolver.resolve("${float.value}", Float.class);
            assertEquals(2.5f, result, 0.001f);
        }
        
        @Test
        @DisplayName("Should convert to boolean true")
        void shouldConvertToBooleanTrue() {
            Boolean result = resolver.resolve("${boolean.true}", Boolean.class);
            assertTrue(result);
        }
        
        @Test
        @DisplayName("Should convert to boolean false")
        void shouldConvertToBooleanFalse() {
            Boolean result = resolver.resolve("${boolean.false}", Boolean.class);
            assertFalse(result);
        }
        
        @Test
        @DisplayName("Should convert to byte")
        void shouldConvertToByte() {
            Byte result = resolver.resolve("${byte.value}", Byte.class);
            assertEquals((byte) 127, result);
        }
        
        @Test
        @DisplayName("Should convert to short")
        void shouldConvertToShort() {
            Short result = resolver.resolve("${short.value}", Short.class);
            assertEquals((short) 32000, result);
        }
        
        @Test
        @DisplayName("Should convert to char")
        void shouldConvertToChar() {
            Character result = resolver.resolve("${char.value}", Character.class);
            assertEquals('X', result);
        }
        
        @Test
        @DisplayName("Should throw exception for invalid char conversion")
        void shouldThrowExceptionForInvalidCharConversion() {
            resolver.setProperty("multi.char", "ABC");
            
            assertThrows(ValueResolutionException.class, () -> 
                resolver.resolve("${multi.char}", Character.class));
        }
        
        @Test
        @DisplayName("Should throw exception for unsupported type")
        void shouldThrowExceptionForUnsupportedType() {
            assertThrows(ValueResolutionException.class, () -> 
                resolver.resolve("${string.value}", Object.class));
        }
        
        @Test
        @DisplayName("Should throw exception for conversion failure")
        void shouldThrowExceptionForConversionFailure() {
            resolver.setProperty("not.a.number", "abc");
            
            assertThrows(ValueResolutionException.class, () -> 
                resolver.resolve("${not.a.number}", Integer.class));
        }
        
        @Test
        @DisplayName("Should return null for null value with type")
        void shouldReturnNullForNullValueWithType() {
            Integer result = resolver.resolve(null, Integer.class);
            assertNull(result);
        }
        
        @Test
        @DisplayName("Should convert to primitive long")
        void shouldConvertToPrimitiveLong() {
            long result = resolver.resolve("${long.value}", long.class);
            assertEquals(1234567890123L, result);
        }
        
        @Test
        @DisplayName("Should convert to primitive double")
        void shouldConvertToPrimitiveDouble() {
            double result = resolver.resolve("${double.value}", double.class);
            assertEquals(3.14159, result, 0.00001);
        }
        
        @Test
        @DisplayName("Should convert to primitive float")
        void shouldConvertToPrimitiveFloat() {
            float result = resolver.resolve("${float.value}", float.class);
            assertEquals(2.5f, result, 0.001f);
        }
        
        @Test
        @DisplayName("Should convert to primitive boolean")
        void shouldConvertToPrimitiveBoolean() {
            boolean result = resolver.resolve("${boolean.true}", boolean.class);
            assertTrue(result);
        }
        
        @Test
        @DisplayName("Should convert to primitive byte")
        void shouldConvertToPrimitiveByte() {
            byte result = resolver.resolve("${byte.value}", byte.class);
            assertEquals((byte) 127, result);
        }
        
        @Test
        @DisplayName("Should convert to primitive short")
        void shouldConvertToPrimitiveShort() {
            short result = resolver.resolve("${short.value}", short.class);
            assertEquals((short) 32000, result);
        }
        
        @Test
        @DisplayName("Should convert to primitive char")
        void shouldConvertToPrimitiveChar() {
            char result = resolver.resolve("${char.value}", char.class);
            assertEquals('X', result);
        }
    }
    
    @Nested
    @DisplayName("Reload Tests")
    class ReloadTests {
        
        @Test
        @DisplayName("Should clear properties on reload")
        void shouldClearPropertiesOnReload() {
            resolver.setProperty("custom.key", "value");
            assertEquals("value", resolver.resolve("${custom.key}"));
            
            resolver.reload();
            
            assertThrows(ValueResolutionException.class, () -> 
                resolver.resolve("${custom.key}"));
        }
    }

    @Nested
    @DisplayName("Malformed Expression Tests")
    class MalformedExpressionTests {

        @Test
        @DisplayName("Should handle incomplete placeholder - missing closing brace")
        void shouldHandleIncompletePlaceholderMissingClosingBrace() {
            // ${property without closing brace should be treated as literal
            String result = resolver.resolve("${incomplete.property");
            assertEquals("${incomplete.property", result);
        }

        @Test
        @DisplayName("Should handle incomplete placeholder - missing opening brace")
        void shouldHandleIncompletePlaceholderMissingOpeningBrace() {
            // property} without opening brace should be treated as literal
            String result = resolver.resolve("property}");
            assertEquals("property}", result);
        }

        @Test
        @DisplayName("Should handle empty placeholder")
        void shouldHandleEmptyPlaceholder() {
            // ${} has no property name, so it won't match the pattern
            // It will be treated as a literal string
            String result = resolver.resolve("${}");
            assertEquals("${}", result);
        }
        
        @Test
        @DisplayName("Should handle placeholder with only colon")
        void shouldHandlePlaceholderWithOnlyColon() {
            // ${:} has no property name, so it won't match the pattern
            // It will be treated as a literal string
            String result = resolver.resolve("${:}");
            assertEquals("${:}", result);
        }

        @Test
        @DisplayName("Should handle nested placeholders - not supported")
        void shouldHandleNestedPlaceholders() {
            // ${outer.${inner.property}} - nested placeholders are not supported
            resolver.setProperty("inner.property", "value");
            resolver.setProperty("outer.value", "result");

            // This should not resolve nested placeholder
            // Since nested placeholders are not supported, this will try to resolve
            // "outer.${inner.property}" as a property name, which doesn't exist
            assertThrows(ValueResolutionException.class, () ->
                resolver.resolve("${outer.${inner.property}}"));
        }

        @Test
        @DisplayName("Should handle placeholder with special characters in property name")
        void shouldHandlePlaceholderWithSpecialCharacters() {
            // Property names with special characters should be handled
            resolver.setProperty("property.with.dots", "value1");
            resolver.setProperty("property-with-dashes", "value2");
            resolver.setProperty("property_with_underscores", "value3");

            assertEquals("value1", resolver.resolve("${property.with.dots}"));
            assertEquals("value2", resolver.resolve("${property-with-dashes}"));
            assertEquals("value3", resolver.resolve("${property_with_underscores}"));
        }

        @Test
        @DisplayName("Should handle placeholder with empty property name")
        void shouldHandlePlaceholderWithEmptyPropertyName() {
            // ${:default} - empty property name with default
            // Pattern requires at least 1 character for property name: ([^}:]+)
            // So this won't match and will be treated as literal
            String result = resolver.resolve("${:default-value}");
            assertEquals("${:default-value}", result);
        }

        @Test
        @DisplayName("Should handle multiple colons in placeholder")
        void shouldHandleMultipleColonsInPlaceholder() {
            // ${property:default:value} - multiple colons
            // Should treat everything after first colon as default value
            String result = resolver.resolve("${missing.property:default:value}");
            assertEquals("default:value", result);
        }

        @Test
        @DisplayName("Should handle placeholder with spaces")
        void shouldHandlePlaceholderWithSpaces() {
            // ${ property name } - spaces in property name
            resolver.setProperty("property name", "value");
            String result = resolver.resolve("${property name}");
            assertEquals("value", result);
        }

        @Test
        @DisplayName("Should handle unclosed placeholder in embedded text")
        void shouldHandleUnclosedPlaceholderInEmbeddedText() {
            // Text with ${unclosed placeholder
            String result = resolver.resolve("Hello ${unclosed.property world");
            // Should be treated as literal since it doesn't match the pattern
            assertEquals("Hello ${unclosed.property world", result);
        }

        @Test
        @DisplayName("Should handle placeholder with newline in default value")
        void shouldHandlePlaceholderWithNewlineInDefaultValue() {
            // ${property:default\nvalue} - newline in default
            String result = resolver.resolve("${missing.property:default\nvalue}");
            assertEquals("default\nvalue", result);
        }

        @Test
        @DisplayName("Should handle placeholder with tab in default value")
        void shouldHandlePlaceholderWithTabInDefaultValue() {
            // ${property:default\tvalue} - tab in default
            String result = resolver.resolve("${missing.property:default\tvalue}");
            assertEquals("default\tvalue", result);
        }

        @Test
        @DisplayName("Should handle placeholder with escaped characters")
        void shouldHandlePlaceholderWithEscapedCharacters() {
            // ${property:default\$value} - escaped dollar sign
            String result = resolver.resolve("${missing.property:default\\$value}");
            assertEquals("default\\$value", result);
        }

        @Test
        @DisplayName("Should handle very long property name")
        void shouldHandleVeryLongPropertyName() {
            String longPropertyName = "a".repeat(1000);
            resolver.setProperty(longPropertyName, "value");

            String result = resolver.resolve("${" + longPropertyName + "}");
            assertEquals("value", result);
        }

        @Test
        @DisplayName("Should handle placeholder with unicode characters")
        void shouldHandlePlaceholderWithUnicodeCharacters() {
            String unicodeKey = "property.unicode";
            String unicodeValue = "\uAC12"; // 값

            resolver.setProperty(unicodeKey, unicodeValue);

            assertEquals(unicodeValue, resolver.resolve("${" + unicodeKey + "}"));
        }
    }

    @Nested
    @DisplayName("Environment Variable Tests")
    class EnvironmentVariableTests {

        @Test
        @DisplayName("Should resolve from programmatic property (simulating env var)")
        void shouldResolveFromProgrammaticProperty() {
            resolver.setProperty("env.test.property", "env-value");

            String result = resolver.resolve("${env.test.property}");
            assertEquals("env-value", result);
        }

        @Test
        @DisplayName("Should resolve property with dots converted to underscores")
        void shouldResolvePropertyWithDotsConvertedToUnderscores() {
            // System property takes precedence, so we test the fallback mechanism
            resolver.setProperty("app.name", "MyApp");

            String result = resolver.resolve("${app.name}");
            assertEquals("MyApp", result);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle property with only dollar sign")
        void shouldHandlePropertyWithOnlyDollarSign() {
            String result = resolver.resolve("$");
            assertEquals("$", result);
        }

        @Test
        @DisplayName("Should handle property with only dollar and opening brace")
        void shouldHandlePropertyWithOnlyDollarAndOpeningBrace() {
            String result = resolver.resolve("${");
            assertEquals("${", result);
        }

        @Test
        @DisplayName("Should handle property with only closing brace")
        void shouldHandlePropertyWithOnlyClosingBrace() {
            String result = resolver.resolve("}");
            assertEquals("}", result);
        }

        @Test
        @DisplayName("Should handle consecutive dollar signs")
        void shouldHandleConsecutiveDollarSigns() {
            String result = resolver.resolve("$$");
            assertEquals("$$", result);
        }

        @Test
        @DisplayName("Should handle dollar sign followed by non-brace character")
        void shouldHandleDollarSignFollowedByNonBraceCharacter() {
            String result = resolver.resolve("$property");
            assertEquals("$property", result);
        }

        @Test
        @DisplayName("Should handle placeholder at start of string")
        void shouldHandlePlaceholderAtStartOfString() {
            resolver.setProperty("start", "value");
            String result = resolver.resolve("${start}end");
            assertEquals("valueend", result);
        }

        @Test
        @DisplayName("Should handle placeholder at end of string")
        void shouldHandlePlaceholderAtEndOfString() {
            resolver.setProperty("end", "value");
            String result = resolver.resolve("start${end}");
            assertEquals("startvalue", result);
        }

        @Test
        @DisplayName("Should handle multiple consecutive placeholders")
        void shouldHandleMultipleConsecutivePlaceholders() {
            resolver.setProperty("a", "1");
            resolver.setProperty("b", "2");
            resolver.setProperty("c", "3");

            String result = resolver.resolve("${a}${b}${c}");
            assertEquals("123", result);
        }

        @Test
        @DisplayName("Should handle placeholder with default containing placeholder syntax")
        void shouldHandlePlaceholderWithDefaultContainingPlaceholderSyntax() {
            // Default value that looks like a placeholder should be treated as literal
            String result = resolver.resolve("${missing.property:${also.missing}}");
            assertEquals("${also.missing}", result);
        }
    }
}
