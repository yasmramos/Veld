package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para métodos factory que generan instancias.
 * 
 * <p>En Veld, los métodos @Factory siguen el modelo compile-time statico.
 * El procesador genera código en Veld.java que:</p>
 * <ol>
 *   <li>Resuelve las dependencias (parámetros del método)</li>
 *   <li>Llama al método factory</li>
 *   <li>Invoca @PostConstruct si existe en el bean retornado</li>
 *   <li>Retorna la nueva instancia</li>
 * </ol>
 * 
 * <p>El scope (singleton vs prototype) se determina automáticamente:</p>
 * <ul>
 *   <li>Si otro componente depende de este factory y se injecta una vez → singleton</li>
 *   <li>Si se llama directamente desde Veld.methodName() → prototype</li>
 * </ul>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * @Component
 * public class ConfigFactory {
 * 
 *     @Factory
 *     public DatabaseConfig createDbConfig(AppProperties props) {
 *         return new DatabaseConfig(props.getUrl(), props.getUser());
 *     }
 * 
 *     @Factory
 *     public UserSession createSession(DatabaseConfig db, SessionConfig cfg) {
 *         return new UserSession(db, cfg);
 *     }
 * }
 * }</pre>
 * 
 * <p>El procesador genera en Veld.java:</p>
 * <pre>{@code
 * // Singleton (usado por otros componentes)
 * private static final DatabaseConfig databaseConfig;
 * static {
 *     databaseConfig = ConfigFactory.createDbConfig(appProperties);
 *     if (databaseConfig instanceof HasPostConstruct) {
 *         ((HasPostConstruct) databaseConfig).init();
 *     }
 * }
 * 
 * // Prototype (llamado directamente)
 * public static UserSession createSession() {
 *     UserSession instance = ConfigFactory.createSession(databaseConfig, sessionConfig);
 *     if (instance instanceof HasPostConstruct) {
 *         ((HasPostConstruct) instance).init();
 *     }
 *     return instance;
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Factory {

    /**
     * Nombre del bean producido.
     * Si no se especifica, se usa el nombre del método.
     */
    String value() default "";

    /**
     * Si es true, el factory se inicializa inmediatamente (eager).
     * Útil para factories cuyos productos son críticos al startup.
     */
    boolean eager() default false;
}
