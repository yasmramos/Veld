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

    // Default session timeout in milliseconds (30 minutes)
    private static final long DEFAULT_SESSION_TIMEOUT_MS = 30 * 60 * 1000L;

    // Maximum number of beans per session to prevent memory leaks
    private static final int MAX_BEANS_PER_SESSION = 1000;

    // Maximum number of active sessions to prevent memory leaks
    private static final int MAX_ACTIVE_SESSIONS = 10000;

    // Map from session ID to session beans
    // Each session gets its own map of beans
    private static final Map<String, Map<String, Object>> SESSION_BEANS = new ConcurrentHashMap<>();

    // Map from session ID to scope metadata
    private static final Map<String, SessionMetadata> SESSION_METADATA = new ConcurrentHashMap<>();

    // Shared context holder for current session (allows cross-thread access)
    private static final ContextHolder<String> CURRENT_SESSION_ID = new ContextHolder<>();

    // Session timeout in milliseconds
    private static volatile long sessionTimeoutMs = DEFAULT_SESSION_TIMEOUT_MS;
    
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
    public <T> T get(String name, ComponentFactory<T> factory) {
        String sessionId = getCurrentSessionId();
        if (sessionId == null) {
            throw new NoSessionContextException(
                "No active session context. Session scope requires an active HTTP session. " +
                "Ensure you are within a valid session (e.g., after HttpServletRequest.getSession()). " +
                "Web framework integration is required to use @SessionScoped beans.");
        }

        // Check if session has expired
        if (isSessionExpired(sessionId)) {
            clearSession(sessionId);
            throw new NoSessionContextException(
                "Session has expired. Session ID: " + sessionId + ". " +
                "Please restart your session.");
        }

        // Check bean limit
        Map<String, Object> beans = getOrCreateSessionBeans(sessionId);
        if (beans.size() >= MAX_BEANS_PER_SESSION) {
            throw new IllegalStateException(
                "Session bean limit exceeded. Maximum " + MAX_BEANS_PER_SESSION +
                " beans per session. Current count: " + beans.size());
        }

        // Use computeIfAbsent for thread-safe bean creation
        @SuppressWarnings("unchecked")
        T instance = (T) beans.computeIfAbsent(name, k -> {
            // Track session access when creating new bean
            trackSessionAccess(sessionId);
            return factory.create();
        });

        return instance;
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
        CURRENT_SESSION_ID.clear();
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
        Map<String, Object> beans = SESSION_BEANS.get(sessionId);
        int beanCount = beans != null ? beans.size() : 0;
        // Truncate long session IDs for readability
        String displayId = sessionId.length() > 13 ? sessionId.substring(0, 13) + "..." : sessionId;
        return "SessionScope[session=" + displayId + ", beans=" + beanCount + "]";
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
     * Gets the current session ID from thread-local context.
     *
     * @return the current session ID, or null if not in a session context
     */
    public static String getCurrentSessionId() {
        return CURRENT_SESSION_ID.get();
    }

    /**
     * Sets the current session ID for the calling thread.
     * Used by web framework integration code.
     *
     * @param sessionId the session ID to set
     */
    public static void setCurrentSession(String sessionId) {
        if (sessionId != null) {
            CURRENT_SESSION_ID.set(sessionId);
        }
    }

    /**
     * Clears the current session context for the calling thread.
     * Called at the end of request processing.
     */
    public static void clearCurrentSession() {
        CURRENT_SESSION_ID.clear();
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
     * Resets all session scope state.
     * Used for testing purposes.
     */
    static void reset() {
        SESSION_BEANS.clear();
        SESSION_METADATA.clear();
        CURRENT_SESSION_ID.clear();
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
        if (sessionId == null) {
            return 0;
        }
        Map<String, Object> beans = SESSION_BEANS.get(sessionId);
        return beans != null ? beans.size() : 0;
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
     * Checks if a session has expired based on last access time.
     *
     * @param sessionId the session ID to check
     * @return true if the session has expired
     */
    private boolean isSessionExpired(String sessionId) {
        SessionMetadata metadata = SESSION_METADATA.get(sessionId);
        if (metadata == null) {
            return false; // Session might be newly created
        }
        long now = System.currentTimeMillis();
        return (now - metadata.getLastAccess()) > sessionTimeoutMs;
    }

    /**
     * Sets the session timeout.
     *
     * @param timeoutMs timeout in milliseconds
     * @throws IllegalArgumentException if timeout is negative or zero
     */
    public static void setSessionTimeout(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Session timeout must be positive");
        }
        sessionTimeoutMs = timeoutMs;
    }

    /**
     * Gets the current session timeout.
     *
     * @return timeout in milliseconds
     */
    public static long getSessionTimeout() {
        return sessionTimeoutMs;
    }

    /**
     * Gets the maximum number of beans allowed per session.
     *
     * @return maximum bean count
     */
    public static int getMaxBeansPerSession() {
        return MAX_BEANS_PER_SESSION;
    }

    /**
     * Gets the maximum number of active sessions allowed.
     *
     * @return maximum session count
     */
    public static int getMaxActiveSessions() {
        return MAX_ACTIVE_SESSIONS;
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
