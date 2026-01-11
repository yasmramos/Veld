package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Verbose logging service for debugging purposes.
 * Activated in both "dev" and "test" profiles but NOT in production.
 * 
 * <p>Example of using multiple profiles with OR logic.
 */
@Singleton
@Profile({"dev", "test"})
public class VerboseLoggingService {
    
    private boolean enabled = true;
    
    @PostConstruct
    public void init() {
        System.out.println("[VerboseLoggingService] Verbose logging ENABLED for dev/test environment");
    }
    
    /**
     * Logs a debug message (only in dev/test).
     * 
     * @param message the message to log
     */
    public void debug(String message) {
        if (enabled) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    /**
     * Logs a trace message with extra details.
     * 
     * @param message the message
     * @param details additional details
     */
    public void trace(String message, Object... details) {
        if (enabled) {
            StringBuilder sb = new StringBuilder("[TRACE] ");
            sb.append(message);
            for (Object detail : details) {
                sb.append(" | ").append(detail);
            }
            System.out.println(sb.toString());
        }
    }
    
    /**
     * Enables or disables verbose logging.
     * 
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Checks if verbose logging is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
