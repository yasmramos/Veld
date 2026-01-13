/*
 * Copyright (c) 2025. Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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
package io.github.yasmramos.veld.runtime.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EventFilter methods that require property accessors.
 * These tests increase coverage for methods with 0% initial coverage.
 */
class EventFilterAccessorTest {

    @BeforeEach
    void setUp() {
        EventFilter.clearCache();
    }

    @Test
    void testEvaluateWithReflectionFallback() throws Throwable {
        // Arrange
        TestEvent event = new TestEvent("TEST", 1, 100.0, true);

        // Act & Assert - should use reflection to access properties
        assertTrue(EventFilter.evaluate("type == 'TEST'", event));
        assertFalse(EventFilter.evaluate("type == 'WRONG'", event));
        assertTrue(EventFilter.evaluate("priority == 1", event));
        assertTrue(EventFilter.evaluate("amount == 100.0", event));
        assertTrue(EventFilter.evaluate("active == true", event));
    }

    @Test
    void testEvaluateWithComparisonOperators() throws Throwable {
        // Arrange
        TestEvent event = new TestEvent("COMPARE", 50, 500.0, false);

        // Act & Assert
        assertTrue(EventFilter.evaluate("priority >= 50", event));
        assertTrue(EventFilter.evaluate("priority > 49", event));
        assertTrue(EventFilter.evaluate("priority <= 50", event));
        assertTrue(EventFilter.evaluate("priority < 51", event));
        assertFalse(EventFilter.evaluate("priority > 50", event));
        assertFalse(EventFilter.evaluate("priority < 50", event));
    }

    @Test
    void testClearCache() throws Throwable {
        // Arrange
        TestEvent event1 = new TestEvent("CACHE1", 1, 100.0, true);
        TestEvent event2 = new TestEvent("CACHE2", 2, 200.0, false);

        // Use the filter multiple times to populate cache
        EventFilter.evaluate("type == 'CACHE1'", event1);
        EventFilter.evaluate("priority == 2", event2);

        // Act - clear the cache
        EventFilter.clearCache();

        // Assert - should still work after clearing
        assertTrue(EventFilter.evaluate("type == 'CACHE2'", event2));
    }

    @Test
    void testRegisterPropertyAccessor() throws Throwable {
        // Arrange
        TestEvent event = new TestEvent("CUSTOM", 10, 1000.0, true);

        EventFilter.EventPropertyAccessor accessor = new EventFilter.EventPropertyAccessor() {
            @Override
            public Object getProperty(String propertyName) {
                if ("customProperty".equals(propertyName)) {
                    return "customValue";
                }
                return null;
            }

            @Override
            public Class<?> getEventClass() {
                return TestEvent.class;
            }
        };

        // Act - register the accessor
        EventFilter.registerPropertyAccessor(accessor);

        // Assert - should use the registered accessor
        assertTrue(EventFilter.evaluate("customProperty == 'customValue'", event));
    }

    @Test
    void testRegisterPropertyGetter() throws Throwable {
        // Arrange
        TestEvent event = new TestEvent("HANDLE", 20, 2000.0, false);

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = lookup.findVirtual(
            TestEvent.class, "getType", java.lang.invoke.MethodType.methodType(String.class)
        );

        // Act - register the property getter
        EventFilter.registerPropertyGetter(TestEvent.class, "type", handle, false);

        // Assert - should use the registered MethodHandle
        assertTrue(EventFilter.evaluate("type == 'HANDLE'", event));
    }

    @Test
    void testEvaluateWithNullExpression() {
        // Arrange
        TestEvent event = new TestEvent("NULL_TEST", 99, 999.0, true);

        // Act & Assert - null expression should accept all events
        assertTrue(EventFilter.evaluate(null, event));
    }

    @Test
    void testEvaluateWithEmptyExpression() {
        // Arrange
        TestEvent event = new TestEvent("EMPTY_TEST", 100, 1000.0, false);

        // Act & Assert - empty expression should accept all events
        assertTrue(EventFilter.evaluate("", event));
        assertTrue(EventFilter.evaluate("   ", event));
    }

    @Test
    void testEvaluateWithWhitespaceExpression() {
        // Arrange
        TestEvent event = new TestEvent("WS_TEST", 5, 50.0, true);

        // Act & Assert - whitespace expression should accept all events
        assertTrue(EventFilter.evaluate("  ", event));
    }

    @Test
    void testEvaluateWithBooleanValues() {
        TestEvent event = new TestEvent("TEST", 1, 100.0, true);
        // Test that boolean parsing works correctly through evaluate
        assertTrue(EventFilter.evaluate("active == true", event));
        assertTrue(EventFilter.evaluate("active == true", event));
    }

    @Test
    void testEvaluateWithNumericValues() {
        TestEvent event = new TestEvent("TEST", 42, 3.14, true);
        // Test that numeric parsing works correctly through evaluate
        assertTrue(EventFilter.evaluate("priority == 42", event));
        assertTrue(EventFilter.evaluate("amount == 3.14", event));
    }

    @Test
    void testEvaluateWithStringValues() {
        TestEvent event = new TestEvent("hello", 1, 1.0, true);
        // Test that string parsing works correctly through evaluate
        assertTrue(EventFilter.evaluate("type == 'hello'", event));
    }

    @Test
    void testCompareNullValues() {
        // Arrange
        NullTestEvent event = new NullTestEvent(null);

        // Act & Assert - null value should work with == null
        // The filter returns false when property value is null
        assertFalse(EventFilter.evaluate("value != null", event));
    }

    @Test
    void testEvaluateInvalidExpression() {
        // Arrange
        TestEvent event = new TestEvent("INVALID", 1, 1.0, true);

        // Act & Assert - invalid expression should accept all events
        assertTrue(EventFilter.evaluate("invalid syntax here", event));
    }

    @Test
    void testEvaluateNonExistentProperty() {
        // Arrange
        TestEvent event = new TestEvent("NONEXISTENT", 1, 1.0, true);

        // Act & Assert - non-existent property should return false
        assertFalse(EventFilter.evaluate("nonExistent == 'value'", event));
    }

    @Test
    void testToDoubleConversion() {
        TestEvent event = new TestEvent("CONVERT", 5, 3.14, true);
        assertTrue(EventFilter.evaluate("amount >= 3", event));
        assertTrue(EventFilter.evaluate("amount < 4", event));
    }

    @Test
    void testBooleanProperty() {
        TestEvent event = new TestEvent("BOOL_TEST", 1, 1.0, true);
        assertTrue(EventFilter.evaluate("active == true", event));
        assertFalse(EventFilter.evaluate("active == false", event));
    }

    @Test
    void testCaseInsensitiveStringComparison() {
        TestEvent event = new TestEvent("case_test", 1, 1.0, true);
        // String comparison is case-sensitive by default
        assertTrue(EventFilter.evaluate("type == 'case_test'", event));
        assertFalse(EventFilter.evaluate("type == 'CASE_TEST'", event));
    }

    /**
     * Helper method to test parseValue through reflection
     */
    private Object parseValueTest(String valueStr) {
        // parseValue is private, so we test it indirectly through evaluate
        return null;
    }

    /**
     * Test event class with standard properties
     */
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

    /**
     * Test event class with null property
     */
    static class NullTestEvent extends Event {
        private final String value;

        NullTestEvent(String value) {
            super();
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
