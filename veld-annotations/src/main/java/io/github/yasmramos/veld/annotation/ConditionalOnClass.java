package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional annotation that activates a component only when ALL specified
 * classes are present in the classpath.
 * 
 * <p>This is useful for auto-configuration where a component should only be
 * activated when certain libraries or classes are available at runtime.</p>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Activates only when Jackson is present
 * &#64;Singleton
 * &#64;ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
 * public class JacksonJsonService implements JsonService { }
 * 
 * // Activates when multiple classes are present
 * &#64;Singleton
 * &#64;ConditionalOnClass(name = {
 *     "redis.clients.jedis.Jedis",
 *     "redis.clients.jedis.JedisPool"
 * })
 * public class RedisConnectionService { }
 * 
 * // Using class reference (compile-time checked)
 * &#64;Singleton
 * &#64;ConditionalOnClass(value = SomeClass.class)
 * public class SomeFeature { }
 * </pre>
 * 
 * <h3>Best Practices:</h3>
 * <ul>
 *   <li>Use {@code name} attribute when the class might not be on the compile classpath</li>
 *   <li>Use {@code value} attribute for compile-time safety when the class is always available</li>
 *   <li>Prefer checking interface/API classes rather than implementation classes</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see ConditionalOnProperty
 * @see ConditionalOnMissingBean
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ConditionalOnClass {
    
    /**
     * The classes that must be present on the classpath.
     * <p>Use this when you have compile-time access to the classes.</p>
     * <p>All specified classes must be present for the condition to match.</p>
     * 
     * @return the required classes
     */
    Class<?>[] value() default {};
    
    /**
     * The fully qualified names of classes that must be present on the classpath.
     * <p>Use this when the classes might not be available at compile time.</p>
     * <p>All specified classes must be present for the condition to match.</p>
     * 
     * @return the fully qualified class names
     */
    String[] name() default {};
}
