package io.github.yasmramos.veld.annotation;

import java.lang.annotation.*;

/**
 * Indicates a method that should return a dynamically looked-up bean.
 * Allows bean lookup by name or type at runtime.
 * 
 * <p>Supports the following lookup types:
 * <ul>
 *   <li>BY_TYPE - Look up by return type (default)</li>
 *   <li>BY_NAME - Look up by bean name</li>
 *   <li>BY_QUALIFIED_NAME - Look up by fully qualified bean name</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * {@code @Singleton}
 * public class MyService {
 *     {@code @Lookup("myOtherBean")}
 *     private OtherService otherService;
 *     
 *     {@code @Lookup(byType = true)}
 *     private Provider<OtherService> otherServiceProvider;
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {
    
    /**
     * The name of the bean to look up.
     * Used when byName or byQualifiedName is true.
     * 
     * @return the bean name
     */
    String value() default "";
    
    /**
     * Whether to look up by type (return type of field/method).
     * 
     * @return true if lookup by type
     */
    boolean byType() default false;
    
    /**
     * Whether to look up by bean name.
     * 
     * @return true if lookup by name
     */
    boolean byName() default false;
    
    /**
     * Whether to look up by fully qualified bean name (class name).
     * 
     * @return true if lookup by qualified name
     */
    boolean byQualifiedName() default false;
    
    /**
     * Whether the lookup is optional (returns null if not found).
     * 
     * @return true if optional
     */
    boolean optional() default false;
    
    /**
     * Lookup type enumeration for programmatic lookup.
     */
    enum LookupType {
        BY_TYPE,
        BY_NAME,
        BY_QUALIFIED_NAME
    }
}
