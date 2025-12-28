package io.github.yasmramos.veld.test.annotation;

import io.github.yasmramos.veld.test.extension.VeldJupiterExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación principal para marcar clases de prueba que requieren
 * integración con el contenedor Veld.
 * 
 * <p>Esta anotación configura automáticamente un contexto de pruebas
 * ligero que proporciona:</p>
 * <ul>
 *   <li>Inyección automática de beans reales y mocks en campos de prueba</li>
 *   <li>Gestión del ciclo de vida del contenedor entre pruebas</li>
 *   <li>Soporte para perfiles de prueba configurables</li>
 *   <li>Integración transparente con Mockito para mocks</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso</h2>
 * <pre>{@code
 * @VeldTest
 * class UserServiceTest {
 *     
 *     @RegisterMock
 *     private UserRepository userRepository;
 *     
 *     @Inject
 *     private UserService userService;
 *     
 *     @Test
 *     void testFindUserById() {
 *         // Configurar comportamiento del mock
 *         when(userRepository.findById(1L))
 *             .thenReturn(new User(1, "Test User"));
 *         
 *         // El servicio usa el mock automáticamente
 *         User result = userService.findUserById(1L);
 *         
 *         assertThat(result.getName()).isEqualTo("Test User");
 *         verify(userRepository).findById(1L);
 *     }
 * }
 * }</pre>
 * 
 * <h2>Configuración de Perfiles</h2>
 * <pre>{@code
 * @VeldTest(profile = "integration")
 * class DatabaseIntegrationTest {
 *     // El perfil "integration" carga configuración específica
 *     // para pruebas de integración con base de datos real
 * }
 * 
 * @VeldTest(profile = "mock")
 * class MockedServiceTest {
 *     // El perfil "mock" reemplaza automáticamente todos los
 *     // beans de infraestructura con mocks
 * }
 * }</pre>
 * 
 * <h2>Propiedades Personalizadas</h2>
 * <pre>{@code
 * @VeldTest(properties = {
 *     "database.url=jdbc:h2:mem:testdb",
 *     "server.port=0"
 * })
 * class CustomPropertiesTest {
 *     // Las propiedades personalizadas están disponibles
 *     // en el contexto de pruebas
 * }
 * }</pre>
 * 
 * @author Veld Framework
 * @since 1.0.0
 * @see RegisterMock
 * @see io.github.yasmramos.veld.test.context.TestContext
 * @see VeldJupiterExtension
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(VeldJupiterExtension.class)
@Tag("veld-integration-test")
public @interface VeldTest {
    
    /**
     * Perfil de configuración de pruebas a activar.
     * 
     * <p>Los perfiles permiten cargar diferentes configuraciones
     * según el tipo de prueba:</p>
     * <ul>
     *   <li>{@code "test"} - Perfil por defecto, configuración básica</li>
     *   <li>{@code "mock"} - Reemplaza beans de infraestructura con mocks</li>
     *   <li>{@code "in-memory"} - Usa implementaciones en memoria</li>
     *   <li>{@code "integration"} - Configuración completa de integración</li>
     *   <li>{@code "fast"} - Configuración optimizada para velocidad</li>
     * </ul>
     * 
     * @return el nombre del perfil a activar
     */
    String profile() default "test";
    
    /**
     * Clases de configuración adicionales a cargar.
     * 
     * <p>Estas clases se cargan además del escaneo de componentes
     * y permiten definir configuración explícita para las pruebas.</p>
     * 
     * @return clases de configuración a cargar
     */
    Class<?>[] classes() default {};
    
    /**
     * Propiedades del sistema a sobrescribir para las pruebas.
     * 
     * <p>Formato: {@code "key=value"}. Estas propiedades están
     * disponibles para los beans mediante {@code @Value}.</p>
     * 
     * @return propiedades en formato "key=value"
     */
    String[] properties() default {};
    
    /**
     * Indica si se debe limpiar el contexto entre cada método de prueba.
     * 
     * <p>Cuando está habilitado, el contexto se reinicia completamente
     * antes de cada prueba, garantizando completo aislamiento.
     * Deshabilitado por defecto para mejorar rendimiento.</p>
     * 
     * @return true si se requiere aislamiento completo entre pruebas
     */
    boolean isolateBetweenTests() default false;
}
