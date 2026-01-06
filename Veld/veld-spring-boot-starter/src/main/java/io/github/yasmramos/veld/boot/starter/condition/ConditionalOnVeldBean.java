package io.github.yasmramos.veld.boot.starter.condition;

import io.github.yasmramos.veld.annotation.Component;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional that checks whether a Veld bean of the specified type exists.
 * 
 * Similar to Spring's {@link org.springframework.boot.autoconfigure.condition.ConditionalOnBean}
 * but for Veld-managed beans.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Register bean only if a specific Veld bean exists:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnVeldBean(VeldCacheService.class)
 * public class CacheConfiguration {
 *     // This configuration will only be loaded if VeldCacheService exists
 * }
 * }</pre>
 * 
 * Register bean based on bean name:
 * <pre>{@code
 * @Component
 * @ConditionalOnVeldBean(name = "redisCache")
 * public class RedisHealthIndicator { }
 * }</pre>
 * 
 * Register bean if any bean of a type exists:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnVeldBean(value = CacheService.class)
 * public class CacheMetricsConfiguration { }
 * }</pre>
 * 
 * @author Veld Team
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(VeldBeanCondition.class)
public @interface ConditionalOnVeldBean {

    /**
     * The class type of the Veld bean to check for.
     * If specified, the condition matches if a bean of the specified type exists.
     */
    Class<?>[] value() default {};

    /**
     * The bean name to check for.
     * If specified, the condition matches if a bean with the specified name exists.
     */
    String[] name() default {};

    /**
     * Strategy to determine if the search is for beans in the Veld container only
     * or in the combined Veld + Spring container.
     * Default is combined search (Veld first, then Spring).
     */
    SearchStrategy search() default SearchStrategy.ALL;

    /**
     * Search strategy for bean lookup
     */
    enum SearchStrategy {
        /**
         * Search only in Veld container
         */
        VELD_ONLY,
        /**
         * Search only in Spring context
         */
        SPRING_ONLY,
        /**
         * Search in both Veld and Spring containers (combined)
         */
        ALL
    }
}
