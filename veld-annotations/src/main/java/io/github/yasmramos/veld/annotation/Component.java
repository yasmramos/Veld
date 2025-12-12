package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a component to be managed by the Veld container.
 * The processor will generate a factory class for this component.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Component {

    /**
     * Optional name for the component.
     * If not specified, the class name with lowercase first letter will be used.
     */
    String value() default "";
}
