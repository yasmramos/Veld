package io.github.yasmramos.veld.runtime.condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PropertyCondition class.
 */
@DisplayName("PropertyCondition Tests")
class PropertyConditionTest {

    private static final String TEST_PROPERTY = "veld.test.property";
    private ConditionContext context;

    @BeforeEach
    void setUp() {
        context = new ConditionContext(null);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(TEST_PROPERTY);
    }

    @Nested
    @DisplayName("Property Existence Tests")
    class PropertyExistenceTests {

        @Test
        @DisplayName("Should match when property exists with any value")
        void shouldMatchWhenPropertyExistsWithAnyValue() {
            System.setProperty(TEST_PROPERTY, "someValue");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "", false);
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when property is missing and matchIfMissing is false")
        void shouldNotMatchWhenPropertyMissingAndMatchIfMissingFalse() {
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "", false);
            
            assertFalse(condition.matches(context));
        }

        @Test
        @DisplayName("Should match when property is missing and matchIfMissing is true")
        void shouldMatchWhenPropertyMissingAndMatchIfMissingTrue() {
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "", true);
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Property Value Matching Tests")
    class PropertyValueMatchingTests {

        @Test
        @DisplayName("Should match when property value equals expected")
        void shouldMatchWhenPropertyValueEqualsExpected() {
            System.setProperty(TEST_PROPERTY, "expectedValue");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "expectedValue", false);
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should not match when property value differs from expected")
        void shouldNotMatchWhenPropertyValueDiffers() {
            System.setProperty(TEST_PROPERTY, "actualValue");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "expectedValue", false);
            
            assertFalse(condition.matches(context));
        }

        @Test
        @DisplayName("Should match any value when expected value is null")
        void shouldMatchAnyValueWhenExpectedIsNull() {
            System.setProperty(TEST_PROPERTY, "anyValue");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, null, false);
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should match any value when expected value is empty")
        void shouldMatchAnyValueWhenExpectedIsEmpty() {
            System.setProperty(TEST_PROPERTY, "anyValue");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "", false);
            
            assertTrue(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Boolean Property Tests")
    class BooleanPropertyTests {

        @Test
        @DisplayName("Should match true value")
        void shouldMatchTrueValue() {
            System.setProperty(TEST_PROPERTY, "true");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "true", false);
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should match false value")
        void shouldMatchFalseValue() {
            System.setProperty(TEST_PROPERTY, "false");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "false", false);
            
            assertTrue(condition.matches(context));
        }

        @Test
        @DisplayName("Should be case sensitive")
        void shouldBeCaseSensitive() {
            System.setProperty(TEST_PROPERTY, "TRUE");
            PropertyCondition condition = new PropertyCondition(TEST_PROPERTY, "true", false);
            
            assertFalse(condition.matches(context));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return property name")
        void shouldReturnPropertyName() {
            PropertyCondition condition = new PropertyCondition("my.property", "value", true);
            
            assertEquals("my.property", condition.getPropertyName());
        }

        @Test
        @DisplayName("Should return expected value")
        void shouldReturnExpectedValue() {
            PropertyCondition condition = new PropertyCondition("my.property", "expectedVal", false);
            
            assertEquals("expectedVal", condition.getExpectedValue());
        }

        @Test
        @DisplayName("Should return matchIfMissing flag")
        void shouldReturnMatchIfMissingFlag() {
            PropertyCondition conditionTrue = new PropertyCondition("prop", "", true);
            PropertyCondition conditionFalse = new PropertyCondition("prop", "", false);
            
            assertTrue(conditionTrue.isMatchIfMissing());
            assertFalse(conditionFalse.isMatchIfMissing());
        }
    }

    @Nested
    @DisplayName("Description Tests")
    class DescriptionTests {

        @Test
        @DisplayName("Should generate description with property name")
        void shouldGenerateDescriptionWithPropertyName() {
            PropertyCondition condition = new PropertyCondition("app.feature.enabled", "", false);
            
            String description = condition.getDescription();
            
            assertTrue(description.contains("@ConditionalOnProperty"));
            assertTrue(description.contains("app.feature.enabled"));
        }

        @Test
        @DisplayName("Should include havingValue in description when specified")
        void shouldIncludeHavingValueInDescription() {
            PropertyCondition condition = new PropertyCondition("feature", "active", false);
            
            String description = condition.getDescription();
            
            assertTrue(description.contains("havingValue=\"active\""));
        }

        @Test
        @DisplayName("Should include matchIfMissing in description when true")
        void shouldIncludeMatchIfMissingInDescription() {
            PropertyCondition condition = new PropertyCondition("feature", "", true);
            
            String description = condition.getDescription();
            
            assertTrue(description.contains("matchIfMissing=true"));
        }

        @Test
        @DisplayName("Should not include matchIfMissing in description when false")
        void shouldNotIncludeMatchIfMissingWhenFalse() {
            PropertyCondition condition = new PropertyCondition("feature", "", false);
            
            String description = condition.getDescription();
            
            assertFalse(description.contains("matchIfMissing"));
        }
    }
}
