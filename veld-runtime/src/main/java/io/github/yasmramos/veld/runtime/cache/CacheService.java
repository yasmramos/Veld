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
package io.github.yasmramos.veld.runtime.cache;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory cache service with TTL and size limits.
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
public class CacheService {

    private static final CacheService INSTANCE = new CacheService();

    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    private CacheService() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "veld-cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
    }

    public static CacheService getInstance() {
        return INSTANCE;
    }

    /**
     * Gets or creates a cache with the given name.
     */
    public Cache getCache(String name, long ttl, int maxSize) {
        return caches.computeIfAbsent(name, n -> new Cache(n, ttl, maxSize));
    }

    /**
     * Gets a value from cache.
     */
    public Object get(String cacheName, String methodName, Object[] args) {
        Cache cache = caches.get(cacheName);
        if (cache == null) {
            return null;
        }
        String key = generateKey(methodName, args);
        return cache.get(key);
    }

    /**
     * Puts a value in cache.
     */
    public void put(String cacheName, String methodName, Object[] args, Object value, long ttl, int maxSize) {
        Cache cache = getCache(cacheName, ttl, maxSize);
        String key = generateKey(methodName, args);
        cache.put(key, value);
    }

    /**
     * Evicts a value from cache.
     */
    public void evict(String cacheName, String methodName, Object[] args) {
        Cache cache = caches.get(cacheName);
        if (cache != null) {
            String key = generateKey(methodName, args);
            cache.evict(key);
        }
    }

    /**
     * Clears all entries from a cache.
     */
    public void clear(String cacheName) {
        Cache cache = caches.get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Generates a cache key from method name and arguments.
     */
    public String generateKey(String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            return methodName;
        }
        StringBuilder sb = new StringBuilder(methodName);
        sb.append(":");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(args[i] == null ? "null" : args[i].toString());
        }
        return sb.toString();
    }

    private void cleanupExpired() {
        for (Cache cache : caches.values()) {
            cache.cleanupExpired();
        }
    }

    /**
     * Shuts down the cache service.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        caches.clear();
    }

    /**
     * Individual cache instance.
     */
    public static class Cache {
        private final String name;
        private final long ttl;
        private final int maxSize;
        private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

        Cache(String name, long ttl, int maxSize) {
            this.name = name;
            this.ttl = ttl;
            this.maxSize = maxSize > 0 ? maxSize : Integer.MAX_VALUE;
        }

        public Object get(String key) {
            CacheEntry entry = entries.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                entries.remove(key);
                return null;
            }
            return entry.getValue();
        }

        public void put(String key, Object value) {
            // Evict if at capacity
            if (entries.size() >= maxSize) {
                evictOldest();
            }
            entries.put(key, new CacheEntry(value, ttl));
        }

        public void evict(String key) {
            entries.remove(key);
        }

        public void clear() {
            entries.clear();
        }

        void cleanupExpired() {
            entries.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        private void evictOldest() {
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<String, CacheEntry> e : entries.entrySet()) {
                if (e.getValue().getCreatedAt() < oldestTime) {
                    oldestTime = e.getValue().getCreatedAt();
                    oldestKey = e.getKey();
                }
            }
            if (oldestKey != null) {
                entries.remove(oldestKey);
            }
        }

        public String getName() {
            return name;
        }

        public int size() {
            return entries.size();
        }
    }

    /**
     * Cache entry with expiration.
     */
    static class CacheEntry {
        private final Object value;
        private final long createdAt;
        private final long expiresAt;

        CacheEntry(Object value, long ttl) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = ttl > 0 ? createdAt + ttl : Long.MAX_VALUE;
        }

        Object getValue() {
            return value;
        }

        long getCreatedAt() {
            return createdAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
