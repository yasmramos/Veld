package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a component is eligible for registration when one or more
 * specified profiles are active.
 * 
 * <p>A profile is a named logical grouping that may be activated programmatically
 * via {@code Veld.setActiveProfiles()} or through the system property
 * {@code veld.profiles.active} or environment variable {@code VELD_PROFILES_ACTIVE}.
 * 
 * <p>If a component is not annotated with {@code @Profile}, it belongs to the
 * "default" profile and is always registered unless a specific profile is active.
 * 
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Only registered when "dev" profile is active
 * @Singleton
 * @Profile("dev")
 * public class DevDataSource implements DataSource { }
 * 
 * // Only registered when "prod" profile is active
 * @Singleton
 * @Profile("prod")
 * public class ProdDataSource implements DataSource { }
 * 
 * // Registered when either "dev" OR "test" profile is active
 * @Singleton
 * @Profile({"dev", "test"})
 * public class MockEmailService implements EmailService { }
 * 
 * // Negation - registered when "prod" is NOT active
 * @Singleton
 * @Profile("!prod")
 * public class DebugInterceptor { }
 * 
 * // Expression - registered when "dev" AND "local" are both active
 * @Singleton
 * @Profile(expression = "dev && local")
 * public class LocalDevConfig { }
 * 
 * // Combined value and expression
 * @Singleton
 * @Profile(value = "dev", expression = "database.exists")
 * public class DevWithDbConfig { }
 * }</pre>
 * 
 * <h2>Expression Syntax</h2>
 * <p>The expression language supports:
 * <ul>
 *   <li>AND: {@code &&} or {@code and}</li>
 *   <li>OR: {@code ||} or {@code or}</li>
 *   <li>NOT: {@code !} or {@code not}</li>
 *   <li>Parentheses for grouping: {@code (expr)}</li>
 *   <li>Property-based conditions: {@code property.name}</li>
 * </ul>
 * 
 * <h2>Activating Profiles</h2>
 * 
 * <p>Profiles can be activated in several ways:
 * <ul>
 *   <li>System property: {@code -Dveld.profiles.active=dev,test}</li>
 *   <li>Environment variable: {@code VELD_PROFILES_ACTIVE=dev,test}</li>
 *   <li>Programmatically: {@code Veld.setActiveProfiles("dev", "test")}</li>
 * </ul>
 * 
 * @author Veld Framework
 * @since 1.0.0
 * @see Component
 * @see Singleton
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Profile {
    
    /**
     * The set of profiles for which the annotated component should be registered.
     * 
     * <p>Multiple profiles can be specified, and the component will be registered
     * if ANY of the profiles is active (OR logic).
     * 
     * <p>Profile names can be negated with "!" prefix to indicate that the
     * component should be registered when that profile is NOT active.
     * 
     * @return the profiles that must be active for the component to be registered
     */
    String[] value() default {};

    /**
     * Alias for {@link #value()} for single-profile use cases.
     * 
     * @return the profile name
     */
    String name() default "";
    
    /**
     * SpEL-style expression for complex profile conditions.
     * 
     * <p>Supports:
     * <ul>
     *   <li>Logical operators: {@code &&}, {@code ||}, {@code !}</li>
     *   <li>Comparison: {@code ==}, {@code !=}, {@code >}, {@code <}, {@code >=}, {@code <=}</li>
     *   <li>Property-based: {@code property.name} evaluates to true if property exists</li>
     *   <li>Regex matching: {@code value matches "pattern"}</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * @Profile(expression = "dev && database.type == 'h2'")
     * @Profile(expression = "!prod && (debug.enabled || local)")
     * }</pre>
     * 
     * @return the expression string
     */
    String expression() default "";
    
    /**
     * Strategy for evaluating multiple conditions.
     * ALL: All conditions (value + expression) must match
     * ANY: At least one condition must match
     * 
     * @return the match strategy
     */
    MatchStrategy strategy() default MatchStrategy.ALL;
    
    /**
     * Strategy for matching profile conditions.
     */
    enum MatchStrategy {
        /**
         * All conditions must be satisfied (AND logic)
         */
        ALL,
        
        /**
         * At least one condition must be satisfied (OR logic)
         */
        ANY
    }
}
