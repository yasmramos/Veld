package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a prototype-scoped component.
 * A new instance will be created for each injection point or request.
 * 
 * <p>This annotation implies {@link Component}, so you don't need to add both:
 * <pre>
 * // Just use @Prototype - no need for @Component
 * &#64;Prototype
 * public class RequestContext {
 * }
 * </pre>
 * 
 * <p>Optionally, you can specify a component name:
 * <pre>
 * &#64;Prototype("requestScope")
 * public class RequestContext {
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Component  // Meta-annotation: @Prototype implies @Component
public @interface Prototype {
    
    /**
     * Optional name for the component.
     * If not specified, the class name with lowercase first letter will be used.
     */
    String value() default "";
}
