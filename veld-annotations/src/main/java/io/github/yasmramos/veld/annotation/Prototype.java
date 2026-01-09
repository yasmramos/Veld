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
 * 
 * <h2>Lifecycle:</h2>
 * <ul>
 *   <li>New instance created for each request</li>
 *   <li>Container does not track prototype instances</li>
 *   <li>{@link io.github.yasmramos.veld.annotation.PostConstruct} is called after creation</li>
 *   <li>{@link io.github.yasmramos.veld.annotation.PreDestroy} is NOT called (container doesn't track instances)</li>
 * </ul>
 * 
 * <h2>When to Use Prototype:</h2>
 * <ul>
 *   <li>Stateful beans where each client needs its own instance</li>
 *   <li>Beans with mutable state that should not be shared</li>
 *   <li>Integration with external systems requiring isolation</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Component  // Meta-annotation: @Prototype implies @Component
@VeldScope(value = "io.github.yasmramos.veld.runtime.scope.PrototypeScope", id = "prototype", displayName = "Prototype")
public @interface Prototype {
    
    /**
     * Optional name for the component.
     * If not specified, the class name with lowercase first letter will be used.
     */
    String value() default "";
}
