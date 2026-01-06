package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional annotation that activates a component only when a specified
 * system property or environment variable has a specific value.
 * 
 * <p>The condition checks the following sources in order:</p>
 * <ol>
 *   <li>System properties (via {@code System.getProperty()})</li>
 *   <li>Environment variables (via {@code System.getenv()})</li>
 * </ol>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Activates when "cache.enabled" property equals "true"
 * &#64;Singleton
 * &#64;ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
 * public class CacheService { }
 * 
 * // Activates when "feature.x" property exists (any value)
 * &#64;Singleton
 * &#64;ConditionalOnProperty(name = "feature.x")
 * public class FeatureXService { }
 * 
 * // Activates when property is missing (with matchIfMissing = true)
 * &#64;Singleton
 * &#64;ConditionalOnProperty(name = "legacy.mode", havingValue = "false", matchIfMissing = true)
 * public class ModernService { }
 * </pre>
 * 
 * @since 1.0.0
 * @see ConditionalOnClass
 * @see ConditionalOnMissingBean
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConditionalOnProperty {
    
    /**
     * The name of the property to check.
     * <p>This can be a system property or environment variable name.</p>
     * <p>For environment variables, dots (.) are automatically converted to underscores (_)
     * if the exact name is not found.</p>
     * 
     * @return the property name
     */
    String name();
    
    /**
     * The expected value of the property.
     * <p>If not specified, the condition matches if the property exists with any value.</p>
     * <p>If specified, the condition matches only if the property value equals this value.</p>
     * 
     * @return the expected value, or empty string to match any value
     */
    String havingValue() default "";
    
    /**
     * Whether the condition should match if the property is not set.
     * <p>Defaults to {@code false}, meaning the condition fails if the property is missing.</p>
     * 
     * @return true to match when the property is missing
     */
    boolean matchIfMissing() default false;
}
