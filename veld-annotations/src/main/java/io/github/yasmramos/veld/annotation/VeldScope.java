package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that defines a Veld scope.
 * 
 * <p>Use this annotation to create custom scope annotations that can be
 * recognized by the Veld processor and applied to components.</p>
 * 
 * <h2>Creating a Custom Scope:</h2>
 * <pre>{@code
 * import io.github.yasmramos.veld.annotation.VeldScope;
 * import io.github.yasmramos.veld.runtime.scope.ThreadScope;
 * 
 * // Define the scope annotation with fully-qualified class name
 * @VeldScope("io.github.yasmramos.veld.runtime.scope.ThreadScope")
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.TYPE)
 * public @interface ThreadScoped {
 * }
 * 
 * // Use the custom scope
 * @ThreadScoped
 * public class RequestContext {
 *     private final String requestId;
 *     
 *     public RequestContext() {
 *         this.requestId = UUID.randomUUID().toString();
 *     }
 * }
 * }</pre>
 * 
 * <h2>Creating a Custom Scope with Configuration:</h2>
 * <pre>{@code
 * import io.github.yasmramos.veld.annotation.VeldScope;
 * 
 * // Define the scope annotation with custom attributes
 * @VeldScope(value = "io.github.yasmramos.veld.runtime.scope.CustomScope", displayName = "Custom Scope")
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.TYPE)
 * public @interface CustomScoped {
 *     String name() default "";
 * }
 * }</pre>
 * 
 * <h2>Scope with Parameter:</h2>
 * <p>For scopes that require configuration, create a factory annotation:</p>
 * <pre>{@code
 * // Define a scope configuration annotation
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.TYPE)
 * public @interface SessionScoped {
 *     String value() default "";
 * }
 * 
 * // Register the scope implementation via ScopeProvider
 * public class SessionScopeProvider implements ScopeProvider {
 *     public Scope getScope() {
 *         return new SessionScope();
 *     }
 *     // ... other methods
 * }
 * }</pre>
 * 
 * @see io.github.yasmramos.veld.annotation.Singleton
 * @see io.github.yasmramos.veld.annotation.Prototype
 * @see io.github.yasmramos.veld.runtime.scope.Scope
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface VeldScope {
    
    /**
     * The fully-qualified name of the Scope implementation class.
     * 
     * <p>Must be the binary name (e.g., "com.example.MyScope") of a class that
     * implements {@link io.github.yasmramos.veld.runtime.scope.Scope} and has
     * a public no-arg constructor.</p>
     * 
     * @return the fully-qualified class name of the Scope implementation
     */
    String value() default "";
    
    /**
     * Optional scope identifier.
     * 
     * <p>If not specified, the simple name of the annotation being annotated
     * will be used (e.g., @ThreadScoped becomes "thread").</p>
     * 
     * @return the scope identifier, or empty for auto-detection
     */
    String id() default "";
    
    /**
     * Optional display name for documentation and debugging.
     * 
     * @return the display name, or empty for default
     */
    String displayName() default "";
    
    /**
     * Optional description of the scope's behavior.
     * 
     * @return the description, or empty for default
     */
    String description() default "";
}
