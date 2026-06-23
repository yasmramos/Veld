package io.github.yasmramos.veld.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields that should be replaced with mocks
 * during test execution.
 * 
 * <p>Fields annotated with {@code @RegisterMock} are automatically
 * converted into Mockito mocks and registered in the Veld container
 * before dependency resolution. This allows replacing real
 * implementations with controlled behavior for testing.</p>
 * 
 * <h2>Behavior</h2>
 * <ul>
 *   <li>The field becomes a Mockito mock of the declared type</li>
 *   <li>The mock is registered in the Veld container with the field name</li>
 *   <li>Beans depending on the mock type will receive the mock instead of the real implementation</li>
 *   <li>The field is injected with the mock for configuration in the test</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
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
 *         // Common mock configuration
 *         when(paymentGateway.process(any())).thenReturn(true);
 *     }
 *     
 *     @Test
 *     void testProcessOrder_Success() {
 *         Order order = new Order("TEST-001");
 *         
 *         // The service uses the mock automatically
 *         boolean result = orderService.processOrder(order);
 *         
 *         assertThat(result).isTrue();
 *         verify(paymentGateway).process(order.getId());
 *     }
 *     
 *     @Test
 *     void testProcessOrder_PaymentFailure() {
 *         // Override behavior for this specific test
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
 * <h2>Common Use Cases</h2>
 * <ul>
 *   <li><b>Repositories:</b> Replace database access</li>
 *   <li><b>External services:</b> Simulate third-party APIs</li>
 *   <li><b>Infrastructure components:</b> Mock mailers, caches, etc.</li>
 *   <li><b>Classes with side effects:</b> Isolate business logic</li>
 * </ul>
 * 
 * <h2>Multiple Mocks of the Same Type</h2>
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
 *     // The service can receive the appropriate mock based on the qualifier
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
     * Optional name for the mock in the container.
     * 
     * <p>If not specified, the field name is used
     * as the bean name in the container.</p>
     * 
     * @return mock bean name in the container
     */
    String name() default "";
    
    /**
     * Indicates if the mock should be replaced in each test method.
     * 
     * <p>When enabled, the mock is reset before each
     * test, ensuring isolation. Disabled by default
     * for better performance.</p>
     * 
     * @return true if the mock should be reset between tests
     */
    boolean resetBeforeEach() default false;
    
    /**
     * Custom answer for the mock.
     * 
     * <p>Allows specifying a custom Answer to define
     * the default behavior of the mock.</p>
     * 
     * @return Answer to use
     */
    org.mockito.Answers answer() default org.mockito.Answers.RETURNS_DEFAULTS;
}
