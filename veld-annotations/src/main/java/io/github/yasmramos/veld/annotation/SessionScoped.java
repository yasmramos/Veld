package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component as having session scope.
 * 
 * <p>A session-scoped bean is created once per HTTP session and is shared
 * across all requests within that session. The bean persists for the lifetime
 * of the user's session.</p>
 * 
 * <p>This scope is typically used for user-specific data such as shopping carts,
 * user preferences, authentication tokens, etc.</p>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * @SessionScoped
 * @Component
 * public class UserSession {
 *     private User user;
 *     private List<CartItem> cartItems;
 *     private String sessionId;
 *     
 *     public void login(User user) {
 *         this.user = user;
 *         this.sessionId = UUID.randomUUID().toString();
 *     }
 *     
 *     public boolean isLoggedIn() {
 *         return user != null;
 *     }
 * }
 * 
 * @Component
 * public class ShoppingCartService {
 *     private final UserSession userSession;
 *     
 *     public ShoppingCartService(UserSession userSession) {
 *         this.userSession = userSession;
 *     }
 *     
 *     public void addToCart(Product product) {
 *         if (!userSession.isLoggedIn()) {
 *             throw new IllegalStateException("User must be logged in");
 *         }
 *         userSession.getCartItems().add(product);
 *     }
 * }
 * }</pre>
 * 
 * <h2>Integration Requirements:</h2>
 * <p>For session scope to work properly, you must integrate Veld with your
 * web framework's session management. This is typically done with a
 * HttpSessionListener:</p>
 * <pre>{@code
 * public class VeldSessionScopeListener implements HttpSessionListener {
 *     @Override
 *     public void sessionCreated(HttpSessionEvent se) {
 *         // Session scope is initialized when session is created
 *     }
 *     
 *     @Override
 *     public void sessionDestroyed(HttpSessionEvent se) {
 *         // Session scope is destroyed when session ends
 *         SessionScope.clearSessionScope(se.getSession().getId());
 *     }
 * }
 * }</pre>
 * 
 * <h2>Important Considerations:</h2>
 * <ul>
 *   <li>Session-scoped beans should be Serializable if using distributed sessions</li>
 *   <li>Be careful with memory consumption - large objects in session scope persist for a long time</li>
 *   <li>Thread safety: session beans are accessed by multiple threads within a session</li>
 *   <li>Consider using RequestScoped for data that doesn't need to persist across requests</li>
 * </ul>
 * 
 * @see io.github.yasmramos.veld.runtime.scope.SessionScope
 * @see io.github.yasmramos.veld.annotation.VeldScope
 * @see RequestScoped
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@VeldScope(value = "io.github.yasmramos.veld.runtime.scope.SessionScope", displayName = "Session Scope")
public @interface SessionScoped {
    
    /**
     * Optional name for the bean.
     * If not specified, the bean name will be derived from the class name.
     * 
     * @return the bean name, or empty for auto-derivation
     */
    String value() default "";
}
