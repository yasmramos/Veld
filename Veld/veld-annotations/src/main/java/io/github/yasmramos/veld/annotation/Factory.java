package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a factory that produces beans for the Veld container.
 *
 * <p>A factory class contains one or more methods annotated with {@link Bean}
 * that return instances of objects to be managed by the container. Unlike
 * regular {@link Component} classes where the class itself is instantiated,
 * factory classes are instantiated once and their methods are called to
 * create bean instances.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code @Factory}
 * public class ConnectionFactory {
 *
 *     {@code @Bean}
 *     public Connection createDatabaseConnection() {
 *         return new DatabaseConnection(config.getUrl());
 *     }
 *
 *     {@code @Bean(name = "pooledConnection")}
 *     public Connection createPooledConnection() {
 *         ConnectionPool pool = new ConnectionPool();
 *         return pool.getConnection();
 *     }
 * }
 * </pre>
 *
 * <p>Factory classes can also inject dependencies to help create their beans:</p>
 * <pre>
 * {@code @Factory}
 * public class ServiceFactory {
 *
 *     {@code @Inject}
 *     private Configuration config;
 *
 *     {@code @Bean}
 *     public UserService createUserService() {
 *         return new UserService(config.getUserRepository());
 *     }
 * }
 * </pre>
 *
 * @see Bean
 * @see Component
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Factory {

    /**
     * Optional name for the factory. If not specified, the simple class name
     * will be used as the factory identifier.
     *
     * @return the factory name
     */
    String name() default "";
}
