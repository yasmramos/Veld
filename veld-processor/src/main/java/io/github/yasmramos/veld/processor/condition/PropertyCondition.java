package io.github.yasmramos.veld.processor.condition;

import java.util.Set;

/**
 * Represents a condition based on configuration properties.
 *
 * <p>Translates the @ConditionalOnProperty annotation into evaluable Java code at compile time:</p>
 * <ul>
 *   <li>If {@code havingValue} is empty: check only property existence</li>
 *   <li>If {@code havingValue} has a value: check that the property has that specific value</li>
 *   <li>If {@code matchIfMissing} is true: the condition is met if the property does not exist</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @ConditionalOnProperty(name = "feature.x.enabled", havingValue = "true")
 * }</pre>
 *
 * <p>Generates code like:</p>
 * <pre>{@code
 * properties.getProperty("feature.x.enabled", "true")
 * }</pre>
 */
public record PropertyCondition(
    String propertyName,
    String havingValue,
    boolean matchIfMissing
) implements ConditionExpression {

    private static final String PROP_PREFIX = "PROP_";

    public PropertyCondition {
        if (propertyName == null || propertyName.isBlank()) {
            throw new IllegalArgumentException("propertyName cannot be null or empty");
        }
    }

    @Override
    public String toJavaCode(GenerationContext context) {
        if (matchIfMissing) {
            // matchIfMissing=true means: the condition is met if the property does NOT exist
            if (havingValue.isEmpty()) {
                return "!properties.hasProperty(\"" + propertyName + "\")";
            } else {
                return "!properties.hasProperty(\"" + propertyName + "\") || !properties.getProperty(\""
                    + propertyName + "\", \"" + havingValue + "\").equals(\"" + havingValue + "\")";
            }
        }
        
        if (havingValue.isEmpty()) {
            // Check existence only
            return "properties.hasProperty(\"" + propertyName + "\")";
        }
        
        // Check specific value - convert to boolean
        return "Boolean.parseBoolean(properties.getProperty(\"" + propertyName + "\", \"" + havingValue + "\"))";
    }

    @Override
    public Set<String> getRequiredFlags() {
        return Set.of(PROP_PREFIX + sanitize(propertyName));
    }

    /**
     * Generates the initialization code for the flag for this property.
     *
     * @param context Generation context
     * @return Java code to initialize the flag
     */
    public String initializeFlagCode(GenerationContext context) {
        String flagName = PROP_PREFIX + sanitize(propertyName);
        
        if (matchIfMissing) {
            return flagName + " = !properties.hasProperty(\"" + propertyName + "\");";
        }
        
        if (havingValue.isEmpty()) {
            return flagName + " = properties.hasProperty(\"" + propertyName + "\");";
        }
        
        // Check specific value - convert to boolean
        return flagName + " = Boolean.parseBoolean(properties.getProperty(\"" + propertyName + "\", \"" + havingValue + "\"));";
    }

    /**
     * Sanitizes the property name to use it as a flag name.
     * Uses same logic as GenerationContext.sanitize() for consistency.
     */
    private String sanitize(String name) {
        return GenerationContext.sanitize(name);
    }

    /**
     * Creates a PropertyCondition from ConditionInfo data.
     */
    public static PropertyCondition fromInfo(
            String propertyName, 
            String havingValue, 
            boolean matchIfMissing) {
        return new PropertyCondition(propertyName, havingValue != null ? havingValue : "", matchIfMissing);
    }
}
