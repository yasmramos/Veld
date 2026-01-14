package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.VeldException;
import io.github.yasmramos.veld.runtime.ComponentFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Scope implementation for HTTP session-scoped beans.
 * 
 * <p>A session scope maintains one instance per HTTP session.
 * This scope is typically used in web applications where beans
 * should persist across multiple requests from the same user.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>One instance per HTTP session</li>
 *   <li>Instance is created on first access within the session</li>
 *   <li>Persists across multiple requests</li>
 *   <li>Automatically cleaned up when session expires</li>
 *   <li>Thread-safe across requests within the same session</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * @SessionScoped
 * @Component
 * public class UserSession {
 *     private User user;
 *     private String sessionId;
 *     
 *     public void login(User user) {
 *         this.user = user;
 *         this.sessionId = UUID.randomUUID().toString();
 *     }
 * }
 * }</pre>
 * 
 * <h2>Integration with Web Frameworks:</h2>
 * <p>This scope requires integration with the web framework's session lifecycle.
 * Register the session scope in your web framework integration:</p>
 * <pre>{@code
 * // Example: Servlet context listener integration
 * public class VeldSessionScopeInitializer implements ServletContextListener {
 *     @Override
 *     public void contextInitialized(ServletContextEvent sce) {
 *         // Session scope is automatically registered via ScopeRegistry
 *     }
 * }
 * 
 * // Example: Getting/setting session-scoped beans
 * public class MyServlet extends HttpServlet {
 *     protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
 *         String sessionId = req.getSession().getId();
 *         
 *         // Set the current session context
 *         SessionScope.setCurrentSession(sessionId);
 *         
 *         try {
 *             UserSession session = Veld.get(UserSession.class);
 *             // Use the session-scoped bean
 *         } finally {
 *             SessionScope.clearCurrentSession();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @see Scope
 * @see RequestScope
 * @see io.github.yasmramos.veld.annotation.SessionScoped
 */
public class SessionScope implements Scope {
    
    public static final String SCOPE_ID = "session";
    
    // Map from session ID to session beans
    // Each session gets its own map of beans
    private static final Map<String, Map<String, Object>> SESSION_BEANS = new ConcurrentHashMap<>();
    
    // Map from session ID to scope metadata
    private static final Map<String, SessionMetadata> SESSION_METADATA = new ConcurrentHashMap<>();
    
    // Current session ID holder (set by web framework integration)
    private static final ThreadLocal<String> CURRENT_SESSION_ID = new ThreadLocal<>();
    
    // Shared session context for testing and concurrent access scenarios
    // When set, this session ID is used instead of the ThreadLocal value
    private static volatile String sharedCurrentSessionId = null;
    
    /**
     * Creates a new SessionScope instance.
     */
    public SessionScope() {
        // Default constructor
    }
    
    @Override
    public String getId() {
        return SCOPE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Session";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String name, ComponentFactory<T> factory) {
        String sessionId = getCurrentSessionId();
        if (sessionId == null) {
            throw new NoSessionContextException(
                "No active session context. Session scope requires an active HTTP session. " +
                "Ensure you are within a valid session (e.g., after HttpServletRequest.getSession()). " +
                "Web framework integration is required to use @SessionScoped beans.");
        }
        
        Map<String, Object> beans = getOrCreateSessionBeans(sessionId);
        
        // Track session access
        trackSessionAccess(sessionId);
        
        return (T) beans.computeIfAbsent(name, k -> factory.create());
    }
    
    @Override
    public Object remove(String name) {
        String sessionId = getCurrentSessionId();
        if (sessionId != null) {
            Map<String, Object> beans = SESSION_BEANS.get(sessionId);
            if (beans != null) {
                return beans.remove(name);
            }
        }
        return null;
    }
    
    @Override
    public void destroy() {
        // Clear all session data
        SESSION_BEANS.clear();
        SESSION_METADATA.clear();
        CURRENT_SESSION_ID.remove();
        sharedCurrentSessionId = null;
    }
    
    @Override
    public boolean isActive() {
        return getCurrentSessionId() != null;
    }
    
    @Override
    public String describe() {
        String sessionId = getCurrentSessionId();
        if (sessionId == null) {
            return "SessionScope[inactive]";
        }
        int beanCount = getSessionBeanCount(sessionId);
        boolean active = isActive();
        // Safely truncate session ID to avoid issues with short IDs
        String sessionDisplay = sessionId.length() > 8 
            ? sessionId.substring(0, 8) + "..." 
            : sessionId;
        return "SessionScope[active=" + active + ", beans=" + beanCount + ", session=" + sessionDisplay + "]";
    }
    
    /**
     * Gets or creates the beans map for a session.
     * 
     * @param sessionId the session ID
     * @return the beans map for the session
     */
    private Map<String, Object> getOrCreateSessionBeans(String sessionId) {
        return SESSION_BEANS.computeIfAbsent(sessionId, 
            k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Gets the current session ID, either from shared context or ThreadLocal.
     * 
     * @return the current session ID, or null if not in a session context
     */
    public static String getCurrentSessionId() {
        if (sharedCurrentSessionId != null) {
            return sharedCurrentSessionId;
        }
        return CURRENT_SESSION_ID.get();
    }
    
    /**
     * Gets the number of beans in the current session scope.
     * Uses the current session context (shared or ThreadLocal).
     * 
     * @param sessionId the session ID to check
     * @return the bean count for that session
     */
    public static int getSessionBeanCount(String sessionId) {
        if (sessionId == null) {
            return 0;
        }
        Map<String, Object> beans = SESSION_BEANS.get(sessionId);
        return beans != null ? beans.size() : 0;
    }
    
    /**
     * Sets the current session ID for the calling thread.
     * Used by web framework integration code.
     * 
     * @param sessionId the session ID to set
     */
    public static void setCurrentSession(String sessionId) {
        // Clear shared context when setting a new session
        sharedCurrentSessionId = null;
        if (sessionId != null) {
            CURRENT_SESSION_ID.set(sessionId);
        }
    }
    
    /**
     * Clears the current session context for the calling thread.
     * Called at the end of request processing.
     */
    public static void clearCurrentSession() {
        CURRENT_SESSION_ID.remove();
    }
    
    /**
     * Clears all beans for a specific session.
     * Called when session expires or is invalidated.
     * 
     * @param sessionId the session ID to clear
     * @return the removed beans map, or null if session didn't exist
     */
    public static Map<String, Object> clearSession(String sessionId) {
        SESSION_METADATA.remove(sessionId);
        return SESSION_BEANS.remove(sessionId);
    }
    
    /**
     * Gets the number of active sessions.
     * Useful for monitoring and debugging.
     * 
     * @return the number of active sessions
     */
    public static int getActiveSessionCount() {
        return SESSION_BEANS.size();
    }
    
    /**
     * Gets the number of beans in the current session scope.
     * Useful for debugging.
     * 
     * @return the bean count, or 0 if not in a session context
     */
    public static int getSessionBeanCount() {
        String sessionId = getCurrentSessionId();
        return getSessionBeanCount(sessionId);
    }
    
    /**
     * Checks if the current thread is within a session context.
     * 
     * @return true if within a session
     */
    public static boolean isInSessionContext() {
        return getCurrentSessionId() != null;
    }
    
    /**
     * Sets a shared session ID that will be used by all threads.
     * Useful for testing concurrent access scenarios.
     * 
     * @param sessionId the session ID to use, or null to disable sharing
     */
    public static void setSharedCurrentSessionId(String sessionId) {
        sharedCurrentSessionId = sessionId;
    }
    
    /**
     * Clears the shared session context.
     */
    public static void clearSharedCurrentSessionId() {
        sharedCurrentSessionId = null;
    }
    
    /**
     * Tracks session access for metadata.
     * 
     * @param sessionId the session ID
     */
    private void trackSessionAccess(String sessionId) {
        SESSION_METADATA.computeIfAbsent(sessionId, 
            k -> new SessionMetadata(k, System.currentTimeMillis()));
        SESSION_METADATA.get(sessionId).updateLastAccess();
    }
    
    /**
     * Exception thrown when session context is not available.
     */
    public static class NoSessionContextException extends VeldException {
        private static final long serialVersionUID = 1L;

        NoSessionContextException(String message) {
            super(message);
        }
    }
    
    /**
     * Metadata about a session.
     */
    private static final class SessionMetadata {
        private final String sessionId;
        private final long createdAt;
        private volatile long lastAccess;
        private volatile int accessCount;
        
        SessionMetadata(String sessionId, long createdAt) {
            this.sessionId = sessionId;
            this.createdAt = createdAt;
            this.lastAccess = createdAt;
            this.accessCount = 0;
        }
        
        void updateLastAccess() {
            this.lastAccess = System.currentTimeMillis();
            this.accessCount++;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public long getLastAccess() {
            return lastAccess;
        }
        
        public int getAccessCount() {
            return accessCount;
        }
    }
}
