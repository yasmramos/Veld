package io.github.yasmramos.veld.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para seleccionar el perfil de configuración de pruebas.
 * 
 * <p>Los perfiles permiten definir diferentes configuraciones para
 * distintos tipos de pruebas. Cada perfil puede cargar diferentes
 * beans, propiedades y comportamientos.</p>
 * 
 * <h2>Perfiles Disponibles</h2>
 * <ul>
 *   <li>{@code "test"} - Perfil por defecto, configuración básica</li>
 *   <li>{@code "mock"} - Reemplaza todos los beans de infraestructura con mocks</li>
 *   <li>{@code "in-memory"} - Usa implementaciones en memoria (H2, etc.)</li>
 *   <li>{@code "integration"} - Configuración completa de integración</li>
 *   <li>{@code "fast"} - Optimizado para velocidad, deshabilita características costosas</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso</h2>
 * <pre>{@code
 * // Prueba unitaria con mocks
 * @VeldTest
 * @TestProfile("mock")
 * class MockedUserServiceTest {
 *     @RegisterMock
 *     private UserRepository userRepository;
 *     
 *     @Inject
 *     private UserService userService;
 *     
 *     @Test
 *     void testFindById() {
 *         when(userRepository.findById(1L))
 *             .thenReturn(new User(1, "Test"));
 *         
 *         User result = userService.findById(1L);
 *         assertThat(result.getName()).isEqualTo("Test");
 *     }
 * }
 * 
 * // Prueba de integración con base de datos en memoria
 * @VeldTest
 * @TestProfile("in-memory")
 * class InMemoryUserRepositoryTest {
 *     @Inject
 *     private UserRepository userRepository;
 *     
 *     @Test
 *     void testSaveAndFind() {
 *         User user = new User("test@example.com");
 *         userRepository.save(user);
 *         
 *         User found = userRepository.findById(user.getId());
 *         assertThat(found.getEmail()).isEqualTo("test@example.com");
 *     }
 * }
 * }</pre>
 * 
 * <h2>Configuración de Perfiles</h2>
 * <p>Los perfiles se configuran mediante archivos de propiedades o clases de configuración:</p>
 * <pre>{@code
 * // application-mock.properties
 * database.enabled=false
 * external.api.mock=true
 * 
 * // application-in-memory.properties
 * database.url=jdbc:h2:mem:testdb
 * database.driver=org.h2.Driver
 * }</pre>
 * 
 * @author Veld Framework
 * @since 1.0.0
 * @see VeldTest
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestProfile {
    
    /**
     * Nombre del perfil a activar.
     * 
     * @return nombre del perfil de prueba
     */
    String value() default "test";
    
    /**
     * Indicador para activar todos los perfiles con prefijo.
     * 
     * <p>Si es {@code true}, activa todos los perfiles que
     * empiecen con el valor especificado.</p>
     * 
     * @return true para activar perfiles con prefijo
     */
    boolean prefixMatch() default false;
}
