package io.github.yasmramos.veld.annotation;

import java.lang.annotation.*;

/**
 * Indicates that an annotation attribute is an alias for another attribute.
 * This allows defining equivalent names for annotation parameters.
 * 
 * <p>Example usage:
 * <pre>
 * {@code @interface MyAnnotation {
 *     String value() default "";
 *     
 *     {@code @AliasFor(attribute = "value")}
 *     String name() default "";
 * }}
 * </pre>
 * 
 * <p>Both "value" and "name" would be treated as equivalent.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AliasFor {
    
    /**
     * The name of the attribute that this attribute is an alias for.
     * 
     * @return the aliased attribute name
     */
    String attribute() default "";
    
    /**
     * The annotation type that contains the aliased attribute.
     * Defaults to the enclosing annotation type.
     * 
     * @return the annotation class
     */
    Class<? extends Annotation> annotation() default Annotation.class;
}
