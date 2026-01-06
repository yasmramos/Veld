package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier annotation to distinguish between multiple components of the same type.
 * Can be used on injection points (constructor parameters, fields, method parameters)
 * to specify which named component should be injected.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Named {

    /**
     * The name of the component to inject.
     */
    String value();
}
