package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method inside a {@link Factory} class as a bean producer.
 *
 * <p>Methods annotated with {@code @Bean} are invoked by the Veld container
 * to create bean instances. The return type of the method determines the
 * bean type, and the method may optionally be given a name for explicit
 * identification.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code @Factory}
 * public class ServiceFactory {
 *
 *     // Simple bean - type inferred from return type
 *     {@code @Bean}
 *     public UserService createUserService() {
 *         return new UserService();
 *     }
 *
 *     // Named bean
 *     {@code @Bean(name = "premiumService")}
 *     public UserService createPremiumService() {
 *         return new UserService(PREMIUM_CONFIG);
 *     }
 *
 * // Factory with dependencies
 * {@code @Factory}
 * public class RepositoryFactory {
 *
 *     {@code @Inject}
 *     private DatabaseConfig config;
 *
 *     {@code @Bean}
 *     public UserRepository createUserRepository() {
 *         return new JdbcUserRepository(config.getJdbcUrl());
 *     }
 * }
 * </pre>
 *
 * <p>Factory methods can have parameters, which will be resolved from the container:</p>
 * <pre>
 * {@code @Factory}
 * public class ServiceFactory {
 *
 *     {@code @Bean}
 *     public OrderService createOrderService(UserRepository userRepo, PaymentGateway payment) {
 *         return new OrderService(userRepo, payment);
 *     }
 * }
 * </pre>
 *
 * @see Factory
 * @see Component
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Bean {

    /**
     * Optional name for the produced bean. If not specified, the method name
     * will be used as the default bean name.
     *
     * @return the bean name
     */
    String name() default "";

    /**
     * Whether this bean should be primary among beans of the same type.
     *
     * @return true if this bean should be treated as primary
     */
    boolean primary() default false;
}
