/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.example.lifecycle;

import io.github.yasmramos.veld.annotation.DependsOn;
import io.github.yasmramos.veld.annotation.PostInitialize;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Example of using @PostInitialize for cache warming.
 * 
 * <p>This component uses @DependsOn to ensure the database is ready
 * before attempting to warm the cache.
 */
@Singleton
@DependsOn("databaseConnection")
public class CacheWarmer {
    
    private boolean cacheWarmed = false;
    
    /**
     * Called after ALL beans are initialized.
     * This is the ideal place for cache warming since all dependencies
     * are guaranteed to be ready.
     */
    @PostInitialize(order = 10)
    public void warmCache() {
        System.out.println("  [CacheWarmer] @PostInitialize - Warming caches...");
        
        // Simulate loading data into cache
        System.out.println("  [CacheWarmer] Loading user cache...");
        System.out.println("  [CacheWarmer] Loading product cache...");
        System.out.println("  [CacheWarmer] Loading config cache...");
        
        cacheWarmed = true;
        System.out.println("  [CacheWarmer] Cache warming complete!");
    }
    
    public boolean isCacheWarmed() {
        return cacheWarmed;
    }
    
    public String getFromCache(String key) {
        if (!cacheWarmed) {
            return null;
        }
        return "cached_value_for_" + key;
    }
}
