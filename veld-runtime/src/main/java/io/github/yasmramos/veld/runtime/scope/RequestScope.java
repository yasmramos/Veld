package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.runtime.ComponentFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scope implementation for request-scoped beans.
 * 
 * <p>A request scope maintains one instance per HTTP request.
 * This scope is typically used in web applications where beans
 * should be created once per request and shared within that request.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>One instance per HTTP request</li>
 *   <li>Instance is created on first access within the request</li>
 *   <li>Automatically cleared at the end of the request</li>
 *   <li>Thread-safe within the same request</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Define the scope annotation
 * @VeldScope(RequestScope.class)
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.TYPE)
 * public @interface RequestScoped {
 * }
 * 
 * // Use the custom scope
 * @RequestScoped
 * public class RequestContext {
 *     private final String requestId;
 *     private final User currentUser;
 *     
 *     public RequestContext() {
 *         this.requestId = UUID.randomUUID().toString();
 *         this.currentUser = SecurityContext.getCurrentUser();
 *     }
 * }
 * }</pre>
 * 
 * <h2>Integration with Web Frameworks:</h2>
 * <p>This scope requires integration with the web framework's request lifecycle.
 * Register a request context holder in your web framework integration:</p>
 * <pre>{@code
 * // Example: Servlet filter integration
 * public class RequestScopeFilter implements Filter {
 *     private static final ThreadLocal<Map<String, Object>> REQUEST_SCOPE = 
 *         ThreadLocal.withInitial(ConcurrentHashMap::new);
 *     
 *     @Override
 *     public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
 *         try {
 *             // Initialize request scope
 *             RequestScope.setRequestScope(REQUEST_SCOPE.get());
 *             chain.doFilter(request, response);
 *         } finally {
 *             // Clear request scope
 *             REQUEST_SCOPE.remove();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @see Scope
 * @see SingletonScope
 * @see PrototypeScope
 */
public class RequestScope implements Scope {
    
    private static final String SCOPE_ID = "request";
    
    // ThreadLocal storage for request-scoped beans
    // Each thread (request) gets its own map of beans
    private static final ThreadLocal<Map<String, Object>> requestBeans = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Map from scope class to scope instance (for singleton scopes that hold request state)
    private static final Map<Class<?>, Scope> scopeInstances = new ConcurrentHashMap<>();
    
    /**
     * Creates a new RequestScope instance.
     */
    public RequestScope() {
        // Default constructor
    }
    
    @Override
    public String getId() {
        return SCOPE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Request";
    }
    
    @Override
    public <T> T get(String name, ComponentFactory<T> factory) {
        Map<String, Object> beans = requestBeans.get();
        
        @SuppressWarnings("unchecked")
        T instance = (T) beans.get(name);
        
        if (instance == null) {
            instance = factory.create();
            beans.put(name, instance);
        }
        
        return instance;
    }
    
    @Override
    public Object remove(String name) {
        Map<String, Object> beans = requestBeans.get();
        return beans.remove(name);
    }
    
    @Override
    public void destroy() {
        // Clear all request-scoped beans
        Map<String, Object> beans = requestBeans.get();
        beans.clear();
        requestBeans.remove();
    }
    
    @Override
    public boolean isActive() {
        // Scope is active if we're within a request context
        return requestBeans.get() != null;
    }
    
    @Override
    public String describe() {
        int beanCount = requestBeans.get().size();
        return "RequestScope[beans=" + beanCount + ", active=" + isActive() + "]";
    }
    
    /**
     * Sets the request scope map for the current thread.
     * Used by web framework integration code.
     * 
     * @param scopeMap the scope map to use
     */
    public static void setRequestScope(Map<String, Object> scopeMap) {
        if (scopeMap != null) {
            requestBeans.set(scopeMap);
        }
    }
    
    /**
     * Clears the request scope for the current thread.
     * Called at the end of request processing.
     */
    public static void clearRequestScope() {
        requestBeans.remove();
    }
    
    /**
     * Returns the number of beans in the current request scope.
     * Useful for debugging.
     * 
     * @return the bean count, or 0 if not in a request context
     */
    public static int getRequestBeanCount() {
        Map<String, Object> beans = requestBeans.get();
        return beans != null ? beans.size() : 0;
    }
    
    /**
     * Checks if the current thread is within a request context.
     * 
     * @return true if within a request
     */
    public static boolean isInRequestContext() {
        return requestBeans.get() != null;
    }
}
