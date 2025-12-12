package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject configuration values into fields, constructor parameters,
 * or method parameters.
 * 
 * <p>Values are resolved from multiple sources in order:
 * <ol>
 *   <li>System properties (-Dproperty=value)</li>
 *   <li>Environment variables</li>
 *   <li>Configuration files (application.properties)</li>
 * </ol>
 * 
 * <h3>Basic Usage:</h3>
 * <pre>{@code
 * @Singleton
 * public class MyService {
 *     @Value("${app.name}")
 *     private String appName;
 *     
 *     @Value("${server.port}")
 *     private int port;
 * }
 * }</pre>
 * 
 * <h3>Default Values:</h3>
 * <pre>{@code
 * @Value("${app.name:MyApp}")
 * private String appName;  // Uses "MyApp" if not configured
 * 
 * @Value("${server.port:8080}")
 * private int port;  // Uses 8080 if not configured
 * 
 * @Value("${feature.enabled:false}")
 * private boolean enabled;  // Uses false if not configured
 * }</pre>
 * 
 * <h3>Literal Values:</h3>
 * <pre>{@code
 * @Value("Hello World")
 * private String greeting;  // Literal string value
 * 
 * @Value("42")
 * private int answer;  // Literal numeric value
 * }</pre>
 * 
 * <h3>Environment Variables:</h3>
 * <pre>{@code
 * // For environment variable DATABASE_URL
 * @Value("${DATABASE_URL}")
 * private String dbUrl;
 * 
 * // Environment variables can also have defaults
 * @Value("${DATABASE_URL:jdbc:h2:mem:test}")
 * private String dbUrl;
 * }</pre>
 * 
 * <h3>Supported Types:</h3>
 * <ul>
 *   <li>String</li>
 *   <li>int / Integer</li>
 *   <li>long / Long</li>
 *   <li>double / Double</li>
 *   <li>float / Float</li>
 *   <li>boolean / Boolean</li>
 *   <li>byte / Byte</li>
 *   <li>short / Short</li>
 *   <li>char / Character</li>
 * </ul>
 * 
 * <h3>Constructor Injection:</h3>
 * <pre>{@code
 * @Singleton
 * public class DatabaseService {
 *     private final String url;
 *     private final int maxConnections;
 *     
 *     @Inject
 *     public DatabaseService(
 *             @Value("${db.url}") String url,
 *             @Value("${db.pool.size:10}") int maxConnections) {
 *         this.url = url;
 *         this.maxConnections = maxConnections;
 *     }
 * }
 * }</pre>
 * 
 * @author Veld Framework
 * @since 1.0.0-alpha.5
 * @see io.github.yasmramos.veld.annotation.Inject
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    
    /**
     * The value expression to resolve.
     * 
     * <p>Can be:
     * <ul>
     *   <li>A placeholder: {@code ${property.name}}</li>
     *   <li>A placeholder with default: {@code ${property.name:defaultValue}}</li>
     *   <li>A literal value: {@code "some text"}</li>
     * </ul>
     * 
     * @return the value expression
     */
    String value();
}
