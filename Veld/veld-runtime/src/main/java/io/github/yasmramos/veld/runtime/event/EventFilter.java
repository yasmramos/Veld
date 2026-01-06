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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates filter expressions for event subscribers.
 *
 * <p>Supports a simple expression language for filtering events based
 * on their properties using zero-reflection API.
 *
 * <h2>Zero-Reflection Property Access</h2>
 * <p>For property access to work without reflection, events should implement
 * the {@link EventPropertyAccessor} interface or use the
 * {@link #registerPropertyAccessor} method to register accessors.</p>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class EventFilter {

    /**
     * Interface for zero-reflection property access.
     */
    public interface EventPropertyAccessor {
        Object getProperty(String propertyName);
        Class<?> getEventClass();
    }

    private static final Map<Class<?>, EventPropertyAccessor> propertyAccessors = new ConcurrentHashMap<>();

    /**
     * Registers a property accessor for a specific event class.
     *
     * @param accessor the property accessor
     */
    public static void registerPropertyAccessor(EventPropertyAccessor accessor) {
        propertyAccessors.put(accessor.getEventClass(), accessor);
    }

    /**
     * Gets a property value from an event using zero-reflection accessor.
     */
    private static Object getPropertyValue(Event event, String propertyName) {
        EventPropertyAccessor accessor = propertyAccessors.get(event.getClass());
        if (accessor != null) {
            return accessor.getProperty(propertyName);
        }

        // Fallback to reflection for backward compatibility (deprecated)
        return getPropertyValueReflection(event, propertyName);
    }

    /**
     * Gets a property value from an event using reflection.
     *
     * @deprecated Use {@link #registerPropertyAccessor} instead for zero-reflection mode
     */
    @Deprecated
    private static Object getPropertyValueReflection(Event event, String propertyName) {
        try {
            String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            String cacheKey = event.getClass().getName() + "." + getterName;

            Method getter = methodCache.computeIfAbsent(cacheKey, k -> {
                try {
                    Method m = event.getClass().getMethod(getterName);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException e) {
                    // Try isXxx for boolean
                    try {
                        String isName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                        Method m = event.getClass().getMethod(isName);
                        m.setAccessible(true);
                        return m;
                    } catch (NoSuchMethodException e2) {
                        return null;
                    }
                }
            });

            if (getter == null) {
                System.err.println("[EventFilter] Property not found: " + propertyName);
                return null;
            }

            return getter.invoke(event);

        } catch (Exception e) {
            System.err.println("[EventFilter] Error getting property '" + propertyName + "': " + e.getMessage());
            return null;
        }
    }

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "event\\.([a-zA-Z][a-zA-Z0-9]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+)"
    );

    private static final Map<String, Method> methodCache = new ConcurrentHashMap<>();

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
}
