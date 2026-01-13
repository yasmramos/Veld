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
package io.github.yasmramos.veld.runtime.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates filter expressions for event subscribers.
 * <p>
 * This class is designed for zero-reflection mode, using pre-computed
 * MethodHandles for property access instead of reflection.
 * <p>
 * For property access to work without reflection, events should:
 * <ul>
 *   <li>Implement the {@link EventPropertyAccessor} interface</li>
 *   <li>Or have their MethodHandles pre-registered via {@link #registerPropertyGetter}</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class EventFilter {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * Interface for zero-reflection property access.
     */
    public interface EventPropertyAccessor {
        Object getProperty(String propertyName);
        Class<?> getEventClass();
    }

    /**
     * Container for pre-computed property getter MethodHandles.
     */
    private static class PropertyGetter {
        final MethodHandle getter;
        final boolean isBoolean;

        PropertyGetter(MethodHandle getter, boolean isBoolean) {
            this.getter = getter;
            this.isBoolean = isBoolean;
        }
    }

    private static final Map<Class<?>, EventPropertyAccessor> propertyAccessors = new ConcurrentHashMap<>();
    private static final Map<String, PropertyGetter> methodHandleCache = new ConcurrentHashMap<>();

    /**
     * Registers a property accessor for a specific event class.
     *
     * @param accessor the property accessor
     */
    public static void registerPropertyAccessor(EventPropertyAccessor accessor) {
        propertyAccessors.put(accessor.getEventClass(), accessor);
    }

    /**
     * Registers a pre-computed MethodHandle for a property getter.
     * <p>
     * This method is used by code generators to register property access
     * without reflection, enabling zero-reflection mode for GraalVM Native Image.
     *
     * @param eventClass the event class
     * @param propertyName the property name
     * @param getter the pre-computed MethodHandle
     * @param isBoolean true if the property is boolean (uses "is" getter)
     */
    public static void registerPropertyGetter(Class<?> eventClass, String propertyName,
                                               MethodHandle getter, boolean isBoolean) {
        String cacheKey = eventClass.getName() + "." + propertyName;
        methodHandleCache.put(cacheKey, new PropertyGetter(getter, isBoolean));
    }

    /**
     * Creates a MethodHandle for a property getter.
     * <p>
     * This method can be used by code generators to create MethodHandles
     * for property access, eliminating the need for reflection.
     *
     * @param eventClass the event class
     * @param propertyName the property name
     * @param returnType the return type of the getter
     * @param isBoolean true if the property is boolean (uses "is" getter)
     * @return a MethodHandle for the property getter
     * @throws NoSuchMethodException if the getter method doesn't exist
     * @throws IllegalAccessException if the method is not accessible
     */
    public static MethodHandle createPropertyGetter(Class<?> eventClass, String propertyName,
                                                    Class<?> returnType, boolean isBoolean)
            throws NoSuchMethodException, IllegalAccessException {
        String methodName = isBoolean ? "is" + capitalize(propertyName) : "get" + capitalize(propertyName);
        return LOOKUP.findVirtual(eventClass, methodName, MethodType.methodType(returnType));
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Gets a property value from an event using zero-reflection accessor.
     */
    private static Object getPropertyValue(Event event, String propertyName) {
        // Try registered property accessor first
        EventPropertyAccessor accessor = propertyAccessors.get(event.getClass());
        if (accessor != null) {
            return accessor.getProperty(propertyName);
        }

        // Try pre-registered MethodHandle
        String cacheKey = event.getClass().getName() + "." + propertyName;
        PropertyGetter propertyGetter = methodHandleCache.get(cacheKey);
        if (propertyGetter != null) {
            try {
                return propertyGetter.getter.invoke(event);
            } catch (Throwable e) {
                System.err.println("[EventFilter] Error invoking MethodHandle for property '" +
                        propertyName + "': " + e.getMessage());
                return null;
            }
        }

        System.err.println("[EventFilter] No property accessor found for: " + propertyName);
        return null;
    }

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "event\\.([a-zA-Z][a-zA-Z0-9]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+)"
    );

    /**
     * Private constructor to prevent instantiation.
     */
    private EventFilter() {
        throw new UnsupportedOperationException("EventFilter is a utility class");
    }

    /**
     * Evaluates a filter expression against an event.
     *
     * @param expression the filter expression
     * @param event      the event to evaluate against
     * @return true if the event matches the filter, false otherwise
     */
    public static boolean evaluate(String expression, Event event) {
        if (expression == null || expression.trim().isEmpty()) {
            return true; // No filter means accept all
        }

        try {
            Matcher matcher = EXPRESSION_PATTERN.matcher(expression.trim());
            if (!matcher.matches()) {
                System.err.println("[EventFilter] Invalid expression syntax: " + expression);
                return true; // Invalid expression, accept event
            }

            String propertyName = matcher.group(1);
            String operator = matcher.group(2);
            String valueStr = matcher.group(3).trim();

            // Get property value from event
            Object propertyValue = getPropertyValue(event, propertyName);
            if (propertyValue == null) {
                return false;
            }

            // Parse the comparison value
            Object comparisonValue = parseValue(valueStr);

            // Perform comparison
            return compare(propertyValue, operator, comparisonValue);

        } catch (Exception e) {
            System.err.println("[EventFilter] Error evaluating expression '" + expression + "': " + e.getMessage());
            return true; // On error, accept the event
        }
    }

    /**
     * Parses a value string into an appropriate type.
     */
    private static Object parseValue(String valueStr) {
        // Boolean
        if ("true".equalsIgnoreCase(valueStr)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return Boolean.FALSE;
        }

        // String literal (single or double quotes)
        if ((valueStr.startsWith("'") && valueStr.endsWith("'")) ||
            (valueStr.startsWith("\"") && valueStr.endsWith("\""))) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        // Try numeric
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            // Return as string
            return valueStr;
        }
    }

    /**
     * Compares two values using the specified operator.
     */
    @SuppressWarnings("unchecked")
    private static boolean compare(Object left, String operator, Object right) {
        // Handle null
        if (left == null || right == null) {
            if ("==".equals(operator)) {
                return left == right;
            } else if ("!=".equals(operator)) {
                return left != right;
            }
            return false;
        }

        // Equality operators
        if ("==".equals(operator)) {
            return left.toString().equals(right.toString());
        }
        if ("!=".equals(operator)) {
            return !left.toString().equals(right.toString());
        }

        // Numeric comparisons
        double leftNum = toDouble(left);
        double rightNum = toDouble(right);

        switch (operator) {
            case ">":
                return leftNum > rightNum;
            case "<":
                return leftNum < rightNum;
            case ">=":
                return leftNum >= rightNum;
            case "<=":
                return leftNum <= rightNum;
            default:
                return false;
        }
    }

    /**
     * Converts a value to double for numeric comparison.
     */
    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Clears all registered property accessors and MethodHandles.
     * Useful for testing.
     */
    public static void clearCache() {
        propertyAccessors.clear();
        methodHandleCache.clear();
    }
}
