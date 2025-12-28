package io.github.yasmramos.veld.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar campos que deben ser reemplazados por mocks
 * durante la ejecución de pruebas.
 * 
 * <p>Los campos anotados con {@code @RegisterMock} son automáticamente
 * convertidos en mocks de Mockito y registrados en el contenedor Veld
 * antes de la resolución de dependencias. Esto permite reemplazar
 * implementaciones reales con comportamiento controlado para pruebas.</p>
 * 
 * <h2>Comportamiento</h2>
 * <ul>
 *   <li>El campo se convierte en un mock de Mockito del tipo declarado</li>
 *   <li>El mock se registra en el contenedor Veld con el nombre del campo</li>
 *   <li>Beans que dependan del tipo del mock recibirán el mock en lugar de la implementación real</li>
 *   <li>El campo se inyecta con el mock para configuración en la prueba</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso</h2>
 * <pre>{@code
 * @VeldTest
 * class OrderServiceTest {
 *     
 *     @RegisterMock
 *     private PaymentGateway paymentGateway;
 *     
 *     @Inject
 *     private OrderService orderService;
 *     
 *     @BeforeEach
 *     void setUp() {
 *         // Configuración común de mocks
 *         when(paymentGateway.process(any())).thenReturn(true);
 *     }
 *     
 *     @Test
 *     void testProcessOrder_Success() {
 *         Order order = new Order("TEST-001");
 *         
 *         // El servicio usa el mock automáticamente
 *         boolean result = orderService.processOrder(order);
 *         
 *         assertThat(result).isTrue();
 *         verify(paymentGateway).process(order.getId());
 *     }
 *     
 *     @Test
 *     void testProcessOrder_PaymentFailure() {
 *         // Sobrescribir comportamiento para esta prueba específica
 *         when(paymentGateway.process(any())).thenReturn(false);
 *         
 *         Order order = new Order("TEST-002");
 *         boolean result = orderService.processOrder(order);
 *         
 *         assertThat(result).isFalse();
 *     }
 * }
 * }</pre>
 * 
 * <h2>Casos de Uso Comunes</h2>
 * <ul>
 *   <li><b>Repositorios:</b> Reemplazar acceso a base de datos</li>
 *   <li><b>Servicios externos:</b> Simular APIs de terceros</li>
 *   <li><b>Componentes de infraestructura:</b> Mockear mailers, caches, etc.</li>
 *   <li><b>Clases con efectos secundarios:</b> Aislar lógica de negocio</li>
 * </ul>
 * 
 * <h2>Múltiples Mocks del Mismo Tipo</h2>
 * <pre>{@code
 * @VeldTest
 * class MultiRepositoryTest {
 *     
 *     @RegisterMock(name = "userRepo")
 *     private UserRepository userRepository;
 *     
 *     @RegisterMock(name = "adminRepo")
 *     private UserRepository adminUserRepository;
 *     
 *     @Inject
 *     private UserService userService;
 *     
 *     // El servicio puede recibir el mock apropiado según el calificador
 * }
 * }</pre>
 * 
 * @author Veld Framework
 * @since 1.0.0
 * @see VeldTest
 * @see io.github.yasmramos.veld.test.context.TestContext
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RegisterMock {
    
    /**
     * Nombre opcional para el mock en el contenedor.
     * 
     * <p>Si no se especifica, se utiliza el nombre del campo
     * como nombre del bean en el contenedor.</p>
     * 
     * @return nombre del bean mock en el contenedor
     */
    String name() default "";
    
    /**
     * Indica si el mock debe reemplazarse en cada método de prueba.
     * 
     * <p>Cuando está habilitado, el mock se resetea antes de cada
     * prueba, asegurando aislamiento. Deshabilitado por defecto
     * para mejor rendimiento.</p>
     * 
     * @return true si se debe resetear el mock entre pruebas
     */
    boolean resetBeforeEach() default false;
    
    /**
     * Respuesta personalizada para el mock.
     * 
     * <p>Permite especificar un Answer personalizado para definir
     * comportamiento por defecto del mock.</p>
     * 
     * @return Answer a utilizar
     */
    org.mockito.Answers answer() default org.mockito.Answers.RETURNS_DEFAULTS;
}
