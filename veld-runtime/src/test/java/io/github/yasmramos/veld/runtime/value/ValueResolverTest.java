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
}
