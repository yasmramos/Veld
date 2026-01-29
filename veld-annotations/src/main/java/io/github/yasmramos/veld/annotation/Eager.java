package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para inicialización inmediata de un bean.
 * 
 * <p>Los beans marcados con @Eager se inicializan inmediatamente cuando se carga
 * la clase Veld, en lugar de esperar a que sean solicitados explícitamente.
 * Esto es útil para:</p>
 * <ul>
 *   <li>Preload de servicios críticos que deben estar disponibles al inicio</li>
 *   <li>Validación temprana de configuración</li>
 *   <li>Carga de cachés de datos de startup</li>
 * </ul>
 * 
 * <p>Esta anotación es compatible con @Singleton (comportamiento por defecto).
 * Para beans prototype, @Eager no tiene efecto ya que las instancias se crean
 * bajo demanda.</p>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * @Eager
 * @Singleton
 * public class CacheWarmupService {
 *     @PostConstruct
 *     public void preload() {
 *         // Se ejecuta inmediatamente al cargar Veld
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Eager {

    /**
     * Prioridad de inicialización.
     * Beans con menor valor se inicializan primero.
     * Valor por defecto: 0 (misma prioridad que otros singletons).
     */
    int value() default 0;
}
