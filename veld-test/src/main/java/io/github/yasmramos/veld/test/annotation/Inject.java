package io.github.yasmramos.veld.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject beans from the Veld container into test fields.
 * 
 * <p>Fields annotated with {@code @Inject} are automatically populated
 * with beans from the test container after mocks have
 * been registered. This allows accessing real beans or beans that
 * depend on the registered mocks.</p>
 * 
 * <h2>Behavior</h2>
 * <ul>
 *   <li>The field is injected with the corresponding bean of the declared type</li>
 *   <li>Resolution follows the normal Veld container rules</li>
 *   <li>If mocks of the same type exist, beans using them will be injected</li>
 *   <li>Injection occurs after mock registration but before each test</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @VeldTest
 * class UserServiceIntegrationTest {
 *     
 *     // Injection of the real bean (which internally uses mocks)
 *     @Inject
 *     private UserService userService;
 *     
 *     // Direct injection of a mock
 *     @RegisterMock
 *     private EmailService emailService;
 *     
 *     @Test
 *     void testCreateUser_SendsWelcomeEmail() {
 *         User user = new User("test@example.com", "Test User");
 *         
 *         userService.createUser(user);
 *         
 *         // Verify that the real service used the mock
 *         verify(emailService).sendWelcomeEmail(user.getEmail());
 *     }
 * }
 * }</pre>
 * 
 * <h2>Supported Types</h2>
 * <ul>
 *   <li><b>Interfaces:</b> The bean implementing the interface is injected</li>
 *   <li><b>Concrete classes:</b> The bean of that class is injected</li>
 *   <li><b>Qualifiers:</b> Use with {@code @Named} or custom annotations</li>
 *   <li><b>Providers:</b> {@code Provider<T>} for lazy resolution</li>
 *   <li><b>Optionals:</b> {@code Optional<T>} for optional beans</li>
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
     * Name of the bean to inject.
     * 
     * <p>If specified, the bean with that exact name is searched.
     * If not specified, type-based resolution is used.</p>
     * 
     * @return bean name in the container
     */
    String name() default "";
    
    /**
     * Indicates if the injection is optional.
     * 
     * <p>When {@code true}, if the bean does not exist, {@code null}
     * is injected instead of throwing an exception.</p>
     * 
     * @return true if injection is optional
     */
    boolean optional() default false;
}
