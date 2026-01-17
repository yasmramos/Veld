package io.github.yasmramos.veld.runtime.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConditionContext class.
 */
@DisplayName("ConditionContext Tests")
class ConditionContextTest {

    private ConditionContext context;

    @BeforeEach
    void setUp() {
        context = new ConditionContext(Thread.currentThread().getContextClassLoader());
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create context with class loader")
        void shouldCreateContextWithClassLoader() {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            ConditionContext ctx = new ConditionContext(loader);

            assertNotNull(ctx);
            assertEquals(loader, ctx.getClassLoader());
        }

        @Test
        @DisplayName("Should use context class loader when null provided")
        void shouldUseContextClassLoaderWhenNull() {
            ConditionContext ctx = new ConditionContext(null);

            assertNotNull(ctx.getClassLoader());
        }
    }

    @Nested
    @DisplayName("Bean Registration Tests")
    class BeanRegistrationTests {

        @Test
        @DisplayName("Should register and find bean by name")
        void shouldRegisterAndFindBeanByName() {
            assertFalse(context.containsBeanName("myBean"));

            context.registerBeanName("myBean");

            assertTrue(context.containsBeanName("myBean"));
        }

        @Test
        @DisplayName("Should register and find bean by type")
        void shouldRegisterAndFindBeanByType() {
            String type = "com.example.MyService";

            assertFalse(context.containsBeanType(type));

            context.registerBeanType(type);

            assertTrue(context.containsBeanType(type));
        }

        @Test
        @DisplayName("Should register multiple bean interfaces")
        void shouldRegisterMultipleBeanInterfaces() {
            Iterable<String> interfaces = Arrays.asList(
                "com.example.ServiceA",
                "com.example.ServiceB",
                "com.example.ServiceC"
            );

            context.registerBeanInterfaces(interfaces);

            assertTrue(context.containsBeanType("com.example.ServiceA"));
            assertTrue(context.containsBeanType("com.example.ServiceB"));
            assertTrue(context.containsBeanType("com.example.ServiceC"));
        }
    }

    @Nested
    @DisplayName("Class Presence Tests")
    class ClassPresenceTests {

        @Test
        @DisplayName("Should detect present class")
        void shouldDetectPresentClass() {
            assertTrue(context.isClassPresent("java.lang.String"));
            assertTrue(context.isClassPresent("java.util.List"));
        }

        @Test
        @DisplayName("Should detect missing class")
        void shouldDetectMissingClass() {
            assertFalse(context.isClassPresent("com.nonexistent.FakeClass"));
            assertFalse(context.isClassPresent("org.imaginary.Service"));
        }
    }

    @Nested
    @DisplayName("Property Tests")
    class PropertyTests {

        @Test
        @DisplayName("Should get system property")
        void shouldGetSystemProperty() {
            String key = "test.property." + System.currentTimeMillis();
            String value = "testValue";

            System.setProperty(key, value);
            try {
                assertEquals(value, context.getProperty(key));
            } finally {
                System.clearProperty(key);
            }
        }

        @Test
        @DisplayName("Should return null for missing property")
        void shouldReturnNullForMissingProperty() {
            assertNull(context.getProperty("nonexistent.property.key." + System.currentTimeMillis()));
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            context.registerBeanName("bean1");
            context.registerBeanName("bean2");

            String str = context.toString();

            assertTrue(str.contains("ConditionContext"));
            assertTrue(str.contains("2")); // 2 beans
        }
    }
}
