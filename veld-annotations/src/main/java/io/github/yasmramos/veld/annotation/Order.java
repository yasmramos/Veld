package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the order of a bean relative to other beans of the same type.
 * 
 * <p>Lower values have higher priority and will be registered/injected first.
 * The default order is {@link #LOWEST_PRECEDENCE} ( Integer.MAX_VALUE ).</p>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // High priority - will be injected first
 * &#64;Singleton
 * &#64;Order(10)
 * public class PrimaryService implements Service { }
 * 
 * // Low priority - will be injected last
 * &#64;Singleton
 * &#64;Order(100)
 * public class FallbackService implements Service { }
 * 
 * // Default priority
 * &#64;Singleton
 * public class DefaultService implements Service { }
 * 
 * // Multiple implementations - ordered injection
 * &#64;Singleton
 * public class ServiceConsumer {
 *     // List will contain services ordered by @Order
 *     &#64;Inject
 *     List&lt;Service&gt; services;
 * }
 * </pre>
 * 
 * <h3>Order of Operations:</h3>
 * <ol>
 *   <li>Lower {@code value} = Higher Priority = Registered First</li>
 *   <li>When values are equal, alphabetical class name is used as tiebreaker</li>
 *   <li>{@link Primary} beans take precedence over non-primary beans regardless of order</li>
 *   <li>Beans without {@code @Order} use default priority ({@code LOWEST_PRECEDENCE})</li>
 * </ol>
 * 
 * <h3>Integration with Other Annotations:</h3>
 * <ul>
 *   <li>{@link Primary} - Primary beans are selected even if not highest ordered</li>
 *   <li>{@link DependsOn} - Explicit dependencies take precedence over ordering</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see Primary
 * @see DependsOn
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Order {
    
    /**
     * The order value.
     * Lower values have higher priority and are registered first.
     */
    int value() default LOWEST_PRECEDENCE;
    
    /**
     * Constant for lowest precedence.
     * Use this as the default value.
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;
    
    /**
     * Constant for highest precedence.
     * Use this for beans that must be registered first.
     */
    int HIGHEST_PRECEDENCE = 0;
}
