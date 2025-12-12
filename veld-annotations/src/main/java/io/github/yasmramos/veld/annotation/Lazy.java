package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component for lazy initialization.
 * The component will not be created until it is first requested.
 * 
 * <p>Can be combined with {@link Singleton} for lazy singletons:
 * <pre>
 * &#64;Lazy
 * &#64;Singleton
 * public class ExpensiveService {
 *     // Created only when first accessed
 * }
 * </pre>
 * 
 * <p>Can also be used alone (implies @Component with singleton scope):
 * <pre>
 * &#64;Lazy
 * public class ExpensiveService {
 *     // Equivalent to @Lazy @Singleton
 * }
 * </pre>
 * 
 * <p>Can also be applied to injection points:
 * <pre>
 * &#64;Inject
 * &#64;Lazy
 * private ExpensiveService service;  // Wrapped in LazyHolder
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Component  // Meta-annotation: @Lazy implies @Component (singleton by default)
public @interface Lazy {
    
    /**
     * Optional name for the component (when used on types).
     * If not specified, the class name with lowercase first letter will be used.
     */
    String value() default "";
}
