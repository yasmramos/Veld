package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache service.
 * This service is intentionally NOT registered as a component in the first test
 * to demonstrate optional injection.
 * 
 * Uncomment @Singleton to register it as a component.
 */
// @Singleton  // <- Uncomment to make it available
public class CacheService {
    
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        System.out.println("  CacheService initialized");
    }
    
    public void put(String key, Object value) {
        cache.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }
    
    public boolean contains(String key) {
        return cache.containsKey(key);
    }
    
    public void clear() {
        cache.clear();
    }
}
