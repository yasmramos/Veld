package io.github.yasmramos.veld.security;

import java.util.Collections;
import java.util.Set;

/**
 * Security context holder for the current thread.
 */
public class SecurityContext {
    
    private static final ThreadLocal<Principal> currentPrincipal = new ThreadLocal<>();
    
    public static void setPrincipal(Principal principal) {
        currentPrincipal.set(principal);
    }
    
    public static Principal getPrincipal() {
        return currentPrincipal.get();
    }
    
    public static void clear() {
        currentPrincipal.remove();
    }
    
    public static boolean isAuthenticated() {
        return currentPrincipal.get() != null;
    }
    
    public static boolean hasRole(String role) {
        Principal p = currentPrincipal.get();
        return p != null && p.getRoles().contains(role);
    }
    
    public static boolean hasAnyRole(String... roles) {
        Principal p = currentPrincipal.get();
        if (p == null) return false;
        for (String role : roles) {
            if (p.getRoles().contains(role)) return true;
        }
        return false;
    }
    
    public record Principal(String username, Set<String> roles, Set<String> permissions) {
        public Principal(String username, Set<String> roles) {
            this(username, roles, Collections.emptySet());
        }
        
        public Set<String> getRoles() { return roles; }
        public Set<String> getPermissions() { return permissions; }
    }
}
