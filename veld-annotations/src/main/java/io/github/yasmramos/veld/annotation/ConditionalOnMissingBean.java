package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional annotation that activates a component only when a bean of the
 * specified type or name is NOT already registered in the container.
 * 
 * <p>This is particularly useful for providing default implementations that
 * can be overridden by user-defined beans.</p>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Default implementation - only registered if no other DataSource exists
 * &#64;Singleton
 * &#64;ConditionalOnMissingBean(DataSource.class)
 * public class DefaultDataSource implements DataSource { }
 * 
 * // Check by name
 * &#64;Singleton
 * &#64;ConditionalOnMissingBean(name = "customCache")
 * public class DefaultCacheService { }
 * 
 * // User override takes precedence
 * &#64;Singleton
 * public class MyCustomDataSource implements DataSource { }
 * </pre>
 * 
 * <h3>Registration Order:</h3>
 * <p>The order in which beans are evaluated matters. Beans are processed in
 * the order they are discovered by the annotation processor. To ensure
 * predictable behavior:</p>
 * <ul>
 *   <li>Place user-defined beans before default implementations in package structure</li>
 *   <li>Use explicit ordering via package naming conventions</li>
 *   <li>The annotation processor processes classes alphabetically within packages</li>
 * </ul>
 * 
 * <h3>Important Notes:</h3>
 * <ul>
 *   <li>Either {@code value} or {@code name} must be specified (not both)</li>
 *   <li>When using {@code value}, the check is performed against the bean type</li>
 *   <li>When using {@code name}, the check is performed against the bean name</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see ConditionalOnProperty
 * @see ConditionalOnClass
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ConditionalOnMissingBean {
    
    /**
     * The class types to check for absence in the container.
     * <p>The condition matches only if NO bean of any of these types is registered.</p>
     * <p>Interface types will match any implementation registered for that interface.</p>
     * 
     * @return the types that must NOT be present
     */
    Class<?>[] value() default {};
    
    /**
     * The bean names to check for absence in the container.
     * <p>The condition matches only if NO bean with any of these names is registered.</p>
     * 
     * @return the names that must NOT be present
     */
    String[] name() default {};
}
