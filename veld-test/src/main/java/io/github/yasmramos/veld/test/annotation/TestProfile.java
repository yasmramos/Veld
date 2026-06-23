package io.github.yasmramos.veld.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to select the test configuration profile.
 * 
 * <p>Profiles allow defining different configurations for
 * different test types. Each profile can load different
 * beans, properties, and behaviors.</p>
 * 
 * <h2>Available Profiles</h2>
 * <ul>
 *   <li>{@code "test"} - Default profile, basic configuration</li>
 *   <li>{@code "mock"} - Replaces all infrastructure beans with mocks</li>
 *   <li>{@code "in-memory"} - Uses in-memory implementations (H2, etc.)</li>
 *   <li>{@code "integration"} - Full integration configuration</li>
 *   <li>{@code "fast"} - Optimized for speed, disables expensive features</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Unit test with mocks
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
 * // Integration test with in-memory database
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
 * <h2>Profile Configuration</h2>
 * <p>Profiles are configured through properties files or configuration classes:</p>
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
     * Name of the profile to activate.
     * 
     * @return test profile name
     */
    String value() default "test";
    
    /**
     * Indicator to activate all profiles with a prefix.
     * 
     * <p>If {@code true}, activates all profiles that
     * start with the specified value.</p>
     * 
     * @return true to activate prefix-matched profiles
     */
    boolean prefixMatch() default false;
}
