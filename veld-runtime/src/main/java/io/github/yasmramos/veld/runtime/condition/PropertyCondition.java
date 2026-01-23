package io.github.yasmramos.veld.runtime.condition;

/**
 * Condition that checks for the presence and/or value of a system property
 * or environment variable.
 * 
 * @since 1.0.0
 */
public final class PropertyCondition implements Condition {
    
    private final String propertyName;
    private final String expectedValue;
    private final boolean matchIfMissing;
    
    /**
     * Creates a new property condition.
     * 
     * @param propertyName the name of the property to check
     * @param expectedValue the expected value (empty string means any value)
     * @param matchIfMissing whether to match when property is missing
     */
    public PropertyCondition(String propertyName, String expectedValue, boolean matchIfMissing) {
        this.propertyName = propertyName;
        this.expectedValue = expectedValue;
        this.matchIfMissing = matchIfMissing;
    }
    
    @Override
    public boolean matches(ConditionContext context) {
        String value = context.getProperty(propertyName);
        
        // Property is missing
        if (value == null) {
            return matchIfMissing;
        }
        
        // Property exists - check value if specified
        if (expectedValue == null || expectedValue.isEmpty()) {
            return true; // Any value matches
        }
        
        return expectedValue.equals(value);
    }
    
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("@ConditionalOnProperty(name=\"");
        sb.append(propertyName).append("\"");
        
        if (expectedValue != null && !expectedValue.isEmpty()) {
            sb.append(", havingValue=\"").append(expectedValue).append("\"");
        }
        
        if (matchIfMissing) {
            sb.append(", matchIfMissing=true");
        }
        
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getFailureReason(ConditionContext context) {
        String actualValue = context.getProperty(propertyName);

        // Property is missing
        if (actualValue == null) {
            if (matchIfMissing) {
                return ""; // Should have matched, not a failure
            }
            return String.format(
                "Property '%s' is not set (matchIfMissing=false)",
                propertyName
            );
        }

        // Property exists but value doesn't match
        if (expectedValue != null && !expectedValue.isEmpty()) {
            if (!expectedValue.equals(actualValue)) {
                return String.format(
                    "Property '%s' has value '%s' but expected '%s'",
                    propertyName,
                    actualValue,
                    expectedValue
                );
            }
        }

        return ""; // Condition passed
    }

    public String getPropertyName() {
        return propertyName;
    }
    
    public String getExpectedValue() {
        return expectedValue;
    }
    
    public boolean isMatchIfMissing() {
        return matchIfMissing;
    }
}
