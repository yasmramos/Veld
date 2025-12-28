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
 * Main annotation to mark test classes that require
 * integration with the Veld container.
 * 
 * <p>This annotation automatically configures a lightweight test
 * context that provides:</p>
 * <ul>
 *   <li>Automatic injection of real beans and mocks into test fields</li>
 *   <li>Container lifecycle management between tests</li>
 *   <li>Support for configurable test profiles</li>
 *   <li>Transparent integration with Mockito for mocks</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
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
 *         // Configure mock behavior
 *         when(userRepository.findById(1L))
 *             .thenReturn(new User(1, "Test User"));
 *         
 *         // The service uses the mock automatically
 *         User result = userService.findUserById(1L);
 *         
 *         assertThat(result.getName()).isEqualTo("Test User");
 *         verify(userRepository).findById(1L);
 *     }
 * }
 * }</pre>
 * 
 * <h2>Profile Configuration</h2>
 * <pre>{@code
 * @VeldTest(profile = "integration")
 * class DatabaseIntegrationTest {
 *     // The "integration" profile loads specific configuration
 *     // for integration tests with real database
 * }
 * 
 * @VeldTest(profile = "mock")
 * class MockedServiceTest {
 *     // The "mock" profile automatically replaces all
 *     // infrastructure beans with mocks
 * }
 * }</pre>
 * 
 * <h2>Custom Properties</h2>
 * <pre>{@code
 * @VeldTest(properties = {
 *     "database.url=jdbc:h2:mem:testdb",
 *     "server.port=0"
 * })
 * class CustomPropertiesTest {
 *     // Custom properties are available
 *     // in the test context
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
     * Test configuration profile to activate.
     * 
     * <p>Profiles allow loading different configurations
     * based on the test type:</p>
     * <ul>
 *   <li>{@code "test"} - Default profile, basic configuration</li>
 *   <li>{@code "mock"} - Replaces infrastructure beans with mocks</li>
 *   <li>{@code "in-memory"} - Uses in-memory implementations</li>
 *   <li>{@code "integration"} - Full integration configuration</li>
 *   <li>{@code "fast"} - Configuration optimized for speed</li>
     * </ul>
     * 
     * @return the profile name to activate
     */
    String profile() default "test";
    
    /**
     * Additional configuration classes to load.
     * 
     * <p>These classes are loaded in addition to component scanning
     * and allow defining explicit configuration for tests.</p>
     * 
     * @return configuration classes to load
     */
    Class<?>[] classes() default {};
    
    /**
     * System properties to override for tests.
     * 
     * <p>Format: {@code "key=value"}. These properties are
     * available for beans through {@code @Value}.</p>
     * 
     * @return properties in "key=value" format
     */
    String[] properties() default {};
    
    /**
     * Indicates if the context should be cleaned up between each test method.
     * 
     * <p>When enabled, the context is completely restarted
     * before each test, ensuring full isolation.
     * Disabled by default for better performance.</p>
     * 
     * @return true if full isolation is required between tests
     */
    boolean isolateBetweenTests() default false;
}
