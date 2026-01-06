package io.github.yasmramos.veld.runtime.condition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates SpEL-style expressions for condition matching.
 * 
 * <p>Supports:
 * <ul>
 *   <li>Logical operators: {@code &&}, {@code ||}, {@code !}</li>
 *   <li>Comparison: {@code ==}, {@code !=}</li>
 *   <li>Property-based: {@code property.name} evaluates to true if property exists</li>
 *   <li>Boolean literals: {@code true}, {@code false}</li>
 *   <li>Parentheses for grouping</li>
 * </ul>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public final class ExpressionEvaluator {
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"]*)\"");
    
    private ExpressionEvaluator() {
        // Utility class
    }
    
    /**
     * Evaluates an expression against the given condition context.
     * 
     * @param expression the SpEL-style expression to evaluate
     * @param context the condition context providing access to properties and environment
     * @return true if the expression evaluates to true, false otherwise
     */
    public static boolean evaluate(String expression, ConditionContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }
        
        String normalized = expression.trim();
        
        // Handle logical operators
        if (normalized.contains("&&") || normalized.contains(" and ")) {
            return evaluateAndExpression(normalized, context);
        }
        
        if (normalized.contains("||") || normalized.contains(" or ")) {
            return evaluateOrExpression(normalized, context);
        }
        
        // Handle negation
        if (normalized.startsWith("!") || normalized.startsWith("not ")) {
            String inner = normalized.startsWith("!") 
                ? normalized.substring(1).trim() 
                : normalized.substring(4).trim();
            return !evaluate(inner, context);
        }
        
        // Handle comparison with ==
        if (normalized.contains(" == ")) {
            String[] parts = normalized.split("==");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                return compareValues(left, right, context);
            }
        }
        
        // Handle comparison with !=
        if (normalized.contains(" != ")) {
            String[] parts = normalized.split("!=");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                return !compareValues(left, right, context);
            }
        }
        
        // Handle parentheses
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            String inner = normalized.substring(1, normalized.length() - 1).trim();
            return evaluate(inner, context);
        }
        
        // Handle property reference - evaluates to true if property exists and is truthy
        if (PROPERTY_PATTERN.matcher(normalized).matches()) {
            return evaluateProperty(normalized, context);
        }
        
        // Handle boolean literal
        if (BOOLEAN_PATTERN.matcher(normalized).matches()) {
            return Boolean.parseBoolean(normalized.toLowerCase());
        }
        
        // Handle string comparison
        Matcher stringMatcher = STRING_PATTERN.matcher(normalized);
        if (stringMatcher.matches()) {
            String value = stringMatcher.group(1);
            return evaluateProperty(value, context);
        }
        
        // Default: treat as property name
        return evaluateProperty(normalized, context);
    }
    
    /**
     * Evaluates an AND expression.
     */
    private static boolean evaluateAndExpression(String expression, ConditionContext context) {
        // Split by && (keeping track of " and " as well)
        String[] parts = expression.split("&&| and ");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !evaluate(trimmed, context)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Evaluates an OR expression.
     */
    private static boolean evaluateOrExpression(String expression, ConditionContext context) {
        // Split by || (keeping track of " or " as well)
        String[] parts = expression.split("\\|\\|");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            // Handle " or " split
            if (trimmed.endsWith(" or")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
            if (!trimmed.isEmpty() && evaluate(trimmed, context)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Compares two values.
     */
    private static boolean compareValues(String left, String right, ConditionContext context) {
        Object leftValue = resolveValue(left, context);
        Object rightValue = resolveValue(right, context);
        
        if (leftValue == null && rightValue == null) {
            return true;
        }
        
        if (leftValue == null || rightValue == null) {
            return false;
        }
        
        // Handle string comparison
        if (leftValue instanceof String && rightValue instanceof String) {
            return leftValue.equals(rightValue);
        }
        
        // Handle numeric comparison
        if (leftValue instanceof Number && rightValue instanceof Number) {
            double leftDouble = ((Number) leftValue).doubleValue();
            double rightDouble = ((Number) rightValue).doubleValue();
            return Double.compare(leftDouble, rightDouble) == 0;
        }
        
        return leftValue.equals(rightValue);
    }
    
    /**
     * Resolves a value string to its actual value.
     */
    private static Object resolveValue(String value, ConditionContext context) {
        String trimmed = value.trim();
        
        // Remove quotes if string
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        
        // Boolean
        if (trimmed.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        
        // Number
        try {
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            }
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            // Not a number, fall through to property resolution
        }
        
        // Property reference
        if (PROPERTY_PATTERN.matcher(trimmed).matches()) {
            return context.getProperty(trimmed);
        }
        
        // Default: return as-is
        return trimmed;
    }
    
    /**
     * Evaluates a property reference.
     * Returns true if the property exists and has a truthy value.
     */
    private static boolean evaluateProperty(String propertyName, ConditionContext context) {
        Object value = context.getProperty(propertyName);
        
        if (value == null) {
            return false;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        if (value instanceof String) {
            String str = (String) value;
            return !str.isEmpty() && !str.equalsIgnoreCase("false") && !str.equalsIgnoreCase("null");
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        
        return true;
    }
}
