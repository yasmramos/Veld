package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Prototype;

/**
 * Caché de uso general.
 */
@Prototype
@Component
public class Cache {
    private final String id;
    
    public Cache() {
        this.id = "CACHE-" + System.currentTimeMillis();
    }
    
    public String getId() {
        return id;
    }
}
