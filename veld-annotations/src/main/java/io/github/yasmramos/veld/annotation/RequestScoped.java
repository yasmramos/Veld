package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component as having request scope.
 * 
 * <p>A request-scoped bean is created once per HTTP request and is shared
 * across all injections within that request. The bean is automatically
 * cleared at the end of the request.</p>
 * 
 * <p>This scope is typically used in web applications where beans should
 * be isolated to a single request thread.</p>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * @RequestScoped
 * @Component
 * public class RequestContext {
 *     private final String requestId;
 *     private final User currentUser;
 *     
 *     public RequestContext(HttpServletRequest request) {
 *         this.requestId = UUID.randomUUID().toString();
 *         this.currentUser = extractUser(request);
 *     }
 * }
 * 
 * @Component
 * public class MyService {
 *     private final RequestContext requestContext;
 *     
 *     public MyService(RequestContext requestContext) {
 *         this.requestContext = requestContext;
 *     }
 *     
 *     public void process() {
 *         // Same RequestContext instance as above within the same request
 *     }
 * }
 * }</pre>
 * 
 * <h2>Integration Requirements:</h2>
 * <p>For request scope to work properly, you must integrate Veld with your
 * web framework. This is typically done with a servlet filter or interceptor:</p>
 * <pre>{@code
 * public class VeldRequestScopeFilter implements Filter {
 *     @Override
 *     public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
 *         try {
 *             // Request scope is automatically initialized via RequestScope
 *             chain.doFilter(request, response);
 *         } finally {
 *             // Request scope is automatically cleared
 *             io.github.yasmramos.veld.runtime.scope.RequestScope.clearRequestScope();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h2>Thread Safety:</h2>
 * <p>Request-scoped beans are thread-safe within a single request but must
 * not be shared across different requests or threads.</p>
 * 
 * @see io.github.yasmramos.veld.runtime.scope.RequestScope
 * @see io.github.yasmramos.veld.annotation.VeldScope
 * @see SessionScoped
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@VeldScope(value = "io.github.yasmramos.veld.runtime.scope.RequestScope", displayName = "Request Scope")
public @interface RequestScoped {
    
    /**
     * Optional name for the bean.
     * If not specified, the bean name will be derived from the class name.
     * 
     * @return the bean name, or empty for auto-derivation
     */
    String value() default "";
}
