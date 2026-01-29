package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean should be considered as the primary candidate when
 * multiple beans of the same type are available for injection.
 *
 * <p>When the dependency resolver finds multiple matching beans and none
 * have been specified by a {@link Qualifier}, it will inject the bean
 * marked with {@code @Primary} if one exists.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code @Component}
 * public class DefaultService implements Service {
 *     // Default implementation
 * }
 *
 * {@code @Component}
 * {@code @Primary}
 * public class PrimaryService implements Service {
 *     // Primary implementation that will be injected by default
 * }
 * </pre>
 *
 * @see Qualifier
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Primary {
}
