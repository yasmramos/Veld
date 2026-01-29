package io.github.yasmramos.veld.annotation;

import java.lang.annotation.*;

/**
 * Indicates that a bean or method should be validated using Bean Validation.
 * Supports validation groups for different validation scenarios.
 * 
 * <p>When placed on a class, all @Valid annotated fields will be validated.
 * When placed on a method, the return value will be validated.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define validation groups
 * public interface ValidationGroups {
 *     interface Create {}
 *     interface Update {}
 *     interface Delete {}
 * }
 * 
 * // Use groups in validation
 * @Singleton
 * @Validated(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
 * public class UserService {
 *     private UserRepository userRepository;
 *     
 *     @Inject
 *     public void setUserRepository(@Valid UserRepository userRepository) {
 *         this.userRepository = userRepository;
 *     }
 * }
 * 
 * // Method parameter validation
 * public void createUser(@Valid @Validated(groups = ValidationGroups.Create.class) User user) {
 *     // user is validated before method execution
 * }
 * }</pre>
 * 
 * @see Valid
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {
    
    /**
     * Validation groups to use for validation.
     * If empty, default validation group is used.
     * 
     * @return array of validation group classes
     */
    Class<?>[] groups() default {};
    
    /**
     * Whether to fail fast (stop validating after first failure).
     * Default is false (validate all constraints).
     * 
     * @return true for fail-fast mode
     */
    boolean failFast() default false;
    
    /**
     * Whether to validate cascading nested objects.
     * When true, @Valid annotations are processed recursively.
     * Default is true.
     * 
     * @return true for cascading validation
     */
    boolean cascade() default true;
}
