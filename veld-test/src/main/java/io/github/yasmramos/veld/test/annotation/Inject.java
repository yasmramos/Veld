package io.github.yasmramos.veld.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para inyectar beans del contenedor Veld en campos de prueba.
 * 
 * <p>Los campos anotados con {@code @Inject} se populan automáticamente
 * con beans del contenedor de pruebas después de que los mocks hayan
 * sido registrados. Esto permite acceder a beans reales o beans que
 * dependan de los mocks registrados.</p>
 * 
 * <h2>Comportamiento</h2>
 * <ul>
 *   <li>El campo se inyecta con el bean correspondiente del tipo declarado</li>
 *   <li>La resolución sigue las reglas normales del contenedor Veld</li>
 *   <li>Si existen mocks del mismo tipo, los beans que los utilizan serán inyectados</li>
 *   <li>La inyección ocurre después del registro de mocks pero antes de cada prueba</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso</h2>
 * <pre>{@code
 * @VeldTest
 * class UserServiceIntegrationTest {
 *     
 *     // Inyección del bean real (que internamente usa mocks)
 *     @Inject
 *     private UserService userService;
 *     
 *     // Inyección directa de un mock
 *     @RegisterMock
 *     private EmailService emailService;
 *     
 *     @Test
 *     void testCreateUser_SendsWelcomeEmail() {
 *         User user = new User("test@example.com", "Test User");
 *         
 *         userService.createUser(user);
 *         
 *         // Verificar que el servicio real utilizó el mock
 *         verify(emailService).sendWelcomeEmail(user.getEmail());
 *     }
 * }
 * }</pre>
 * 
 * <h2>Tipos Soportados</h2>
 * <ul>
 *   <li><b>Interfaces:</b> Se injecta el bean que implementa la interfaz</li>
 *   <li><b>Clases concretas:</b> Se injecta el bean de esa clase</li>
 *   <li><b>Qualifiers:</b> Usar con {@code @Named} o anotaciones personalizadas</li>
 *   <li><b>Proveedores:</b> {@code Provider<T>} para resolución lazy</li>
 *   <li><b>Opcionales:</b> {@code Optional<T>} para beans opcionales</li>
 * </ul>
 * 
 * @author Veld Framework
 * @since 1.0.0
 * @see RegisterMock
 * @see VeldTest
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {
    
    /**
     * Nombre del bean a inyectar.
     * 
     * <p>Si se especifica, se busca el bean con ese nombre exacto.
     * Si no se especifica, se utiliza la resolución por tipo.</p>
     * 
     * @return nombre del bean en el contenedor
     */
    String name() default "";
    
    /**
     * Indica si la inyección es opcional.
     * 
     * <p>Cuando es {@code true}, si el bean no existe se injecta {@code null}
     * en lugar de lanzar una excepción.</p>
     * 
     * @return true si la inyección es opcional
     */
    boolean optional() default false;
}
