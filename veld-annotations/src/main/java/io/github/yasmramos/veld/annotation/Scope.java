package io.github.yasmramos.veld.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Scope {
    /**
     * Scope identifier (e.g., "singleton", "prototype").
     */
    String value() default "singleton";
}
