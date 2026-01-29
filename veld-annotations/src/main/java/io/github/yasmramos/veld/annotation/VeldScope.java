package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that defines a custom scope for Veld components.
 *
 * <p>This annotation is used to mark other annotations as scope definitions.
 * When applied to an annotation (like {@code @RequestScoped} or {@code @SessionScoped}),
 * it specifies the runtime scope implementation class and a display name for visualization
 * purposes (such as in dependency graph exports).</p>
 *
 * <p>The {@code value} attribute should contain the fully qualified class name of the
 * scope implementation class that will manage the lifecycle of beans annotated with
 * the custom scope annotation.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * @Documented
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.TYPE)
 * @VeldScope(
 *     value = "io.github.yasmramos.veld.runtime.scope.RequestScope",
 *     displayName = "Request Scope"
 * )
 * public @interface RequestScoped {
 *     String value() default "";
 * }
 * }</pre>
 *
 * <h2>Built-in Scopes:</h2>
 * <ul>
 *   <li>{@code @Singleton} - Single instance shared across the entire container</li>
 *   <li>{@code @Prototype} - New instance created for each injection point</li>
 *   <li>{@code @RequestScoped} - Instance per HTTP request (requires web integration)</li>
 *   <li>{@code @SessionScoped} - Instance per HTTP session (requires web integration)</li>
 * </ul>
 *
 * @see io.github.yasmramos.veld.runtime.scope.Scope
 * @see io.github.yasmramos.veld.runtime.scope.RequestScope
 * @see io.github.yasmramos.veld.runtime.scope.SessionScope
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface VeldScope {

    /**
     * The fully qualified class name of the scope implementation.
     *
     * <p>This class must implement {@link io.github.yasmramos.veld.runtime.scope.Scope}
     * and will be responsible for managing the lifecycle of beans with this scope.</p>
     *
     * @return the fully qualified class name of the scope implementation
     */
    String value();

    /**
     * A human-readable display name for this scope.
     *
     * <p>This name is used for visualization purposes, such as displaying
     * scope information in dependency graph exports or debugging tools.</p>
     *
     * @return the display name for this scope
     */
    String displayName();
}
