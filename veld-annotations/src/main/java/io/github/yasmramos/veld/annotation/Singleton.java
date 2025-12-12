package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a singleton-scoped component.
 * Only one instance will be created and shared across all injection points.
 * 
 * <p>This annotation implies {@link Component}, so you don't need to add both:
 * <pre>
 * // Just use @Singleton - no need for @Component
 * &#64;Singleton
 * public class MyService {
 * }
 * </pre>
 * 
 * <p>Optionally, you can specify a component name:
 * <pre>
 * &#64;Singleton("myCustomName")
 * public class MyService {
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Component  // Meta-annotation: @Singleton implies @Component
public @interface Singleton {
    
    /**
     * Optional name for the component.
     * If not specified, the class name with lowercase first letter will be used.
     */
    String value() default "";
}
