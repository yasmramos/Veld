package com.veld.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a component should be lazily initialized.
 * 
 * When applied to a @Component class, the component will not be instantiated
 * until it is first requested from the container.
 * 
 * When applied to an injection point (field, constructor parameter, or method parameter),
 * the dependency will be lazily resolved.
 * 
 * <pre>
 * // Lazy component - not created until first use
 * {@literal @}Component
 * {@literal @}Lazy
 * public class ExpensiveService {
 *     // ...
 * }
 * 
 * // Lazy injection point
 * {@literal @}Component
 * public class MyService {
 *     {@literal @}Inject
 *     {@literal @}Lazy
 *     private ExpensiveService expensive;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface Lazy {
}
