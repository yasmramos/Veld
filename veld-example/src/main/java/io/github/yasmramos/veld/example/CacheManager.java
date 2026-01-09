package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.runtime.Provider;

/**
 * Gestor de caché.
 */
@Singleton
@Component
public class CacheManager {
    private final Provider<Cache> cacheProvider;
    
    @Inject
    public CacheManager(Provider<Cache> cacheProvider) {
        this.cacheProvider = cacheProvider;
    }
    
    public Provider<Cache> getCacheProvider() {
        return cacheProvider;
    }
}
