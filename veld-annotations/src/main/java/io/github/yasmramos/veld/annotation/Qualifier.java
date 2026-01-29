package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies a dependency injection point, allowing for disambiguation
 * when multiple beans of the same type are available in the container.
 *
 * <p>When used on an injection point (field, method parameter, or constructor
 * parameter), the container will look for a bean with a matching name that
 * was registered either:</p>
 *
 * <ul>
 *     <li>Explicitly with a name via {@link Named} annotation</li>
 *     <li>Implicitly using the class name (simple name with first letter lowercase)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Bean registration with explicit name
 * {@code @Component("fast")}
 * public class FastRepository implements Repository {
 * }
 *
 * {@code @Component("slow")}
 * public class SlowRepository implements Repository {
 * }
 *
 * // Injection with qualifier
 * {@code @Component}
 * public class Service {
 *     {@code @Inject}
 *     {@code @Qualifier("fast")}
 *     private Repository repository;  // Injects FastRepository
 * }
 *
 * // Method parameter injection
 * {@code @Component}
 * public class Service {
 *     public void process({@code @Qualifier("slow")} Repository repo) {
 *         // Uses SlowRepository
 *     }
 * }
 * </pre>
 *
 * @see Primary
 * @see Named
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
public @interface Qualifier {

    /**
     * The bean name qualifier value.
     *
     * @return the name to match against registered bean names
     */
    String value() default "";
}
