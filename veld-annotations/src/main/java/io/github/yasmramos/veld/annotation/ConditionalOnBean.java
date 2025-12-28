package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional annotation that activates a component only when ALL specified
 * beans are already registered in the container.
 * 
 * <p>This is the inverse of {@link ConditionalOnMissingBean} and is useful
 * when you want a bean to be registered only as an extension or override
 * of an existing bean.</p>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Only registered when DataSource bean already exists
 * &#64;Singleton
 * &#64;ConditionalOnBean(DataSource.class)
 * public class DataSourceWrapper implements DataSource { }
 * 
 * // Only registered when multiple beans exist
 * &#64;Singleton
 * &#64;ConditionalOnBean(value = {DataSource.class, EntityManager.class})
 * public class TransactionService { }
 * 
 * // Check by name
 * &#64;Singleton
 * &#64;ConditionalOnBean(name = "primaryDataSource")
 * public class PrimaryDataSourceMonitor { }
 * 
 * // Combined with other conditions
 * &#64;Singleton
 * &#64;ConditionalOnBean(DataSource.class)
 * &#64;ConditionalOnProperty(name = "app.datasource.enabled", havingValue = "true")
 * public class DataSourceHealthCheck { }
 * </pre>
 * 
 * <h3>Evaluation Order:</h3>
 * <p>The container evaluates conditions in the following order:</p>
 * <ol>
 *   <li>{@link ConditionalOnClass} - Classes must be in classpath</li>
 *   <li>{@link ConditionalOnBean} - Required beans must be registered</li>
 *   <li>{@link ConditionalOnMissingBean} - No conflicting beans must exist</li>
 *   <li>{@link ConditionalOnProperty} - Property conditions must match</li>
 * </ol>
 * 
 * <h3>Important Notes:</h3>
 * <ul>
 *   <li>Either {@code value} or {@code name} can be specified (not both required)</li>
 *   <li>When using {@code value}, the check is performed against the bean type</li>
 *   <li>When using {@code name}, the check is performed against the bean name</li>
 *   <li>All specified conditions must match for the bean to be registered (AND logic)</li>
 *   <li>This annotation can be combined with other conditional annotations</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see ConditionalOnMissingBean
 * @see ConditionalOnClass
 * @see ConditionalOnProperty
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConditionalOnBean {
    
    /**
     * The class types that must already be registered as beans in the container.
     * <p>The condition matches only if ALL of these beans are registered.</p>
     * <p>Interface types will match any implementation registered for that interface.</p>
     * 
     * @return the types that must be present
     */
    Class<?>[] value() default {};
    
    /**
     * The bean names that must already be registered in the container.
     * <p>The condition matches only if ALL of these named beans are registered.</p>
     * 
     * @return the names that must be present
     */
    String[] name() default {};
}
