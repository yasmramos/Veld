package io.github.yasmramos.veld.runtime.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassCondition class.
 */
@DisplayName("ClassCondition Tests")
class ClassConditionTest {

    private ConditionContext context;

    @BeforeEach
    void setUp() {
        context = new ConditionContext(Thread.currentThread().getContextClassLoader());
    }

    @Nested
    @DisplayName("Single Class Tests")
    class SingleClassTests {

        @Test
        @DisplayName("Should match when class is present")
        void shouldMatchWhenClassIsPresent() {
            ClassCondition condition = new ClassCondition("java.lang.String");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when class is missing")
        void shouldNotMatchWhenClassIsMissing() {
            ClassCondition condition = new ClassCondition("com.nonexistent.FakeClass");
            
            assertFalse(condition.matches(context));
        }

        @Test
        @DisplayName("Should match standard library classes")
        void shouldMatchStandardLibraryClasses() {
            ClassCondition condition = new ClassCondition("java.util.ArrayList");
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Multiple Classes Tests")
    class MultipleClassesTests {

        @Test
        @DisplayName("Should match when all classes are present")
        void shouldMatchWhenAllClassesPresent() {
            ClassCondition condition = new ClassCondition(
                "java.lang.String",
                "java.util.List",
                "java.util.Map"
            );
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when one class is missing")
        void shouldNotMatchWhenOneClassMissing() {
            ClassCondition condition = new ClassCondition(
                "java.lang.String",
                "com.nonexistent.FakeClass",
                "java.util.List"
            );
            
            assertFalse(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when all classes are missing")
        void shouldNotMatchWhenAllClassesMissing() {
            ClassCondition condition = new ClassCondition(
                "com.fake.ClassA",
                "com.fake.ClassB"
            );
            
            assertFalse(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should accept varargs array")
        void shouldAcceptVarargsArray() {
            ClassCondition condition = new ClassCondition("java.lang.String", "java.lang.Integer");
            
            List<String> classes = condition.getRequiredClasses();
            assertEquals(2, classes.size());
            assertTrue(classes.contains("java.lang.String"));
            assertTrue(classes.contains("java.lang.Integer"));
        }

        @Test
        @DisplayName("Should accept list of classes")
        void shouldAcceptListOfClasses() {
            List<String> classList = Arrays.asList("java.lang.String", "java.lang.Integer");
            ClassCondition condition = new ClassCondition(classList);
            
            assertEquals(classList, condition.getRequiredClasses());
        }

        @Test
        @DisplayName("Should handle empty list")
        void shouldHandleEmptyList() {
            ClassCondition condition = new ClassCondition(Collections.emptyList());
            
            assertTrue(condition.matches(context));
            assertTrue(condition.getRequiredClasses().isEmpty());
        }

        @Test
        @DisplayName("Should handle no arguments")
        void shouldHandleNoArguments() {
            ClassCondition condition = new ClassCondition();
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return required classes")
        void shouldReturnRequiredClasses() {
            ClassCondition condition = new ClassCondition("java.lang.Object", "java.lang.Class");
            
            List<String> classes = condition.getRequiredClasses();
            
            assertEquals(2, classes.size());
            assertTrue(classes.contains("java.lang.Object"));
            assertTrue(classes.contains("java.lang.Class"));
        }
    }

    @Nested
    @DisplayName("Description Tests")
    class DescriptionTests {

        @Test
        @DisplayName("Should generate description for single class")
        void shouldGenerateDescriptionForSingleClass() {
            ClassCondition condition = new ClassCondition("java.lang.String");
            
            String description = condition.getDescription();
            
            assertTrue(description.contains("@ConditionalOnClass"));
            assertTrue(description.contains("java.lang.String"));
        }

        @Test
        @DisplayName("Should generate description for multiple classes")
        void shouldGenerateDescriptionForMultipleClasses() {
            ClassCondition condition = new ClassCondition("java.lang.String", "java.util.List");
            
            String description = condition.getDescription();
            
            assertTrue(description.contains("@ConditionalOnClass"));
            assertTrue(description.contains("java.lang.String"));
            assertTrue(description.contains("java.util.List"));
        }
    }

    @Nested
    @DisplayName("Framework Class Tests")
    class FrameworkClassTests {

        @Test
        @DisplayName("Should detect JUnit classes when present")
        void shouldDetectJUnitClasses() {
            ClassCondition condition = new ClassCondition("org.junit.jupiter.api.Test");
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should detect Veld classes")
        void shouldDetectVeldClasses() {
            ClassCondition condition = new ClassCondition(
                "io.github.yasmramos.veld.runtime.condition.Condition",
                "io.github.yasmramos.veld.runtime.condition.ConditionContext"
            );
            
            assertTrue(condition.matches(context));
        }
    }
}
