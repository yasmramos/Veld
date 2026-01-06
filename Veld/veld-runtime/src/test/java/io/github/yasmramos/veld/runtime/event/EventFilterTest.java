package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for EventFilter expression evaluation.
 */
@DisplayName("EventFilter Tests")
class EventFilterTest {

    // Test event with various property types
    static class TestEvent extends Event {
        private final String type;
        private final int priority;
        private final double amount;
        private final boolean active;

        TestEvent(String type, int priority, double amount, boolean active) {
            super();
            this.type = type;
            this.priority = priority;
            this.amount = amount;
            this.active = active;
        }

        public String getType() {
            return type;
        }

        public int getPriority() {
            return priority;
        }

        public double getAmount() {
            return amount;
        }

        public boolean isActive() {
            return active;
        }
    }

    // Event with null property
    static class NullPropertyEvent extends Event {
        private final String value;

        NullPropertyEvent(String value) {
            super();
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Nested
    @DisplayName("Empty Filter Tests")
    class EmptyFilterTests {

        @Test
        @DisplayName("Should return true for null expression")
        void shouldReturnTrueForNullExpression() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate(null, event));
        }

        @Test
        @DisplayName("Should return true for empty expression")
        void shouldReturnTrueForEmptyExpression() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("", event));
        }

        @Test
        @DisplayName("Should return true for whitespace expression")
        void shouldReturnTrueForWhitespaceExpression() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("   ", event));
        }
    }

    @Nested
    @DisplayName("String Comparison Tests")
    class StringComparisonTests {

        @Test
        @DisplayName("Should match string with single quotes")
        void shouldMatchStringWithSingleQuotes() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.type == 'PREMIUM'", event));
        }

        @Test
        @DisplayName("Should match string with double quotes")
        void shouldMatchStringWithDoubleQuotes() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.type == \"PREMIUM\"", event));
        }

        @Test
        @DisplayName("Should not match different string")
        void shouldNotMatchDifferentString() {
            TestEvent event = new TestEvent("STANDARD", 5, 100.0, true);

            assertFalse(EventFilter.evaluate("event.type == 'PREMIUM'", event));
        }

        @Test
        @DisplayName("Should match not equals")
        void shouldMatchNotEquals() {
            TestEvent event = new TestEvent("STANDARD", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.type != 'PREMIUM'", event));
        }
    }

    @Nested
    @DisplayName("Numeric Comparison Tests")
    class NumericComparisonTests {

        @Test
        @DisplayName("Should match equals for integer")
        void shouldMatchEqualsForInteger() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority == 5", event));
        }

        @Test
        @DisplayName("Should match greater than")
        void shouldMatchGreaterThan() {
            TestEvent event = new TestEvent("TEST", 10, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority > 5", event));
        }

        @Test
        @DisplayName("Should not match greater than when equal")
        void shouldNotMatchGreaterThanWhenEqual() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertFalse(EventFilter.evaluate("event.priority > 5", event));
        }

        @Test
        @DisplayName("Should match greater than or equal")
        void shouldMatchGreaterThanOrEqual() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority >= 5", event));
        }

        @Test
        @DisplayName("Should match less than")
        void shouldMatchLessThan() {
            TestEvent event = new TestEvent("TEST", 3, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority < 5", event));
        }

        @Test
        @DisplayName("Should match less than or equal")
        void shouldMatchLessThanOrEqual() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority <= 5", event));
        }

        @Test
        @DisplayName("Should match double comparison")
        void shouldMatchDoubleComparison() {
            TestEvent event = new TestEvent("TEST", 5, 150.50, true);

            assertTrue(EventFilter.evaluate("event.amount > 100.0", event));
        }
    }

    @Nested
    @DisplayName("Boolean Comparison Tests")
    class BooleanComparisonTests {

        @Test
        @DisplayName("Should match boolean true")
        void shouldMatchBooleanTrue() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.active == true", event));
        }

        @Test
        @DisplayName("Should match boolean false")
        void shouldMatchBooleanFalse() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, false);

            assertTrue(EventFilter.evaluate("event.active == false", event));
        }

        @Test
        @DisplayName("Should match boolean case insensitive")
        void shouldMatchBooleanCaseInsensitive() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.active == TRUE", event));
        }
    }

    @Nested
    @DisplayName("Invalid Expression Tests")
    class InvalidExpressionTests {

        @Test
        @DisplayName("Should return true for invalid syntax")
        void shouldReturnTrueForInvalidSyntax() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            // Invalid syntax - should return true (accept event)
            assertTrue(EventFilter.evaluate("invalid expression", event));
        }

        @Test
        @DisplayName("Should return false for non-existent property")
        void shouldReturnFalseForNonExistentProperty() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            // Property doesn't exist
            assertFalse(EventFilter.evaluate("event.nonExistent == 'value'", event));
        }
    }

    @Nested
    @DisplayName("Null Value Tests")
    class NullValueTests {

        @Test
        @DisplayName("Should handle null property value")
        void shouldHandleNullPropertyValue() {
            NullPropertyEvent event = new NullPropertyEvent(null);

            // Null property - should return false for comparison
            assertFalse(EventFilter.evaluate("event.value == 'test'", event));
        }

        @Test
        @DisplayName("Should handle non-null property value")
        void shouldHandleNonNullPropertyValue() {
            NullPropertyEvent event = new NullPropertyEvent("test");

            assertTrue(EventFilter.evaluate("event.value == 'test'", event));
        }
    }

    @Nested
    @DisplayName("Whitespace Handling Tests")
    class WhitespaceHandlingTests {

        @Test
        @DisplayName("Should handle whitespace around operator")
        void shouldHandleWhitespaceAroundOperator() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.type   ==   'PREMIUM'", event));
        }

        @Test
        @DisplayName("Should handle leading and trailing whitespace")
        void shouldHandleLeadingAndTrailingWhitespace() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("  event.type == 'PREMIUM'  ", event));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string value")
        void shouldHandleEmptyStringValue() {
            TestEvent event = new TestEvent("", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.type == ''", event));
        }

        @Test
        @DisplayName("Should handle zero numeric value")
        void shouldHandleZeroNumericValue() {
            TestEvent event = new TestEvent("TEST", 0, 0.0, true);

            assertTrue(EventFilter.evaluate("event.priority == 0", event));
            assertTrue(EventFilter.evaluate("event.amount == 0.0", event));
        }

        @Test
        @DisplayName("Should handle negative numeric value")
        void shouldHandleNegativeNumericValue() {
            TestEvent event = new TestEvent("TEST", -5, -100.0, true);

            assertTrue(EventFilter.evaluate("event.priority < 0", event));
        }
        
        @Test
        @DisplayName("Should handle property without event prefix")
        void shouldHandlePropertyWithoutEventPrefix() {
            TestEvent event = new TestEvent("PREMIUM", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("type == 'PREMIUM'", event));
        }
        
        @Test
        @DisplayName("Should handle integer not equals")
        void shouldHandleIntegerNotEquals() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.priority != 10", event));
        }
        
        @Test
        @DisplayName("Should handle double less than")
        void shouldHandleDoubleLessThan() {
            TestEvent event = new TestEvent("TEST", 5, 50.0, true);

            assertTrue(EventFilter.evaluate("event.amount < 100.0", event));
        }
        
        @Test
        @DisplayName("Should handle double less than or equal")
        void shouldHandleDoubleLessThanOrEqual() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.amount <= 100.0", event));
        }
        
        @Test
        @DisplayName("Should handle double greater than or equal")
        void shouldHandleDoubleGreaterThanOrEqual() {
            TestEvent event = new TestEvent("TEST", 5, 100.0, true);

            assertTrue(EventFilter.evaluate("event.amount >= 100.0", event));
        }
    }
}
