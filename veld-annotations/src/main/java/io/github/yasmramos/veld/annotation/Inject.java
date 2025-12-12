package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor, field, or method for dependency injection.
 * 
 * <ul>
 *   <li>Constructor: The constructor will be used to instantiate the component</li>
 *   <li>Field: The field will be set after construction</li>
 *   <li>Method: The method will be called after construction with injected parameters</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
public @interface Inject {
}
