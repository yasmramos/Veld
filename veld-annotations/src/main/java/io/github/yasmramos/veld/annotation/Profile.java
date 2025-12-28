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
 * }</pre>
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
    String[] value();

    /**
     * Alias for {@link #value()} for single-profile use cases.
     * 
     * @return the profile name
     */
    String name() default "";
}
