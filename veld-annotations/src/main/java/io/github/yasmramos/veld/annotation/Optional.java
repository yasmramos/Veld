package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a dependency injection point as optional.
 * 
 * When a dependency is marked as optional:
 * - If the dependency exists, it will be injected normally
 * - If the dependency does NOT exist, null will be injected instead of throwing an exception
 * 
 * This annotation can be applied to:
 * - Constructor parameters
 * - Field injections
 * - Method injection parameters
 * 
 * Example usage:
 * <pre>
 * &#64;Singleton
 * public class MyService {
 *     
 *     // Optional field injection - will be null if not found
 *     &#64;Inject &#64;Optional
 *     LogService logger;
 *     
 *     // Optional constructor parameter
 *     &#64;Inject
 *     public MyService(@Optional CacheService cache) {
 *         // cache may be null
 *     }
 * }
 * </pre>
 * 
 * Alternatively, you can use {@code java.util.Optional<T>} as the type:
 * <pre>
 * &#64;Inject
 * Optional&lt;CacheService&gt; cacheService;  // Will be Optional.empty() if not found
 * </pre>
 * 
 * @see Inject
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Optional {
}
