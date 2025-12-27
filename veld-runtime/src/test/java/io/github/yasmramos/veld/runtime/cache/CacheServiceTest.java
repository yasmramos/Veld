/*
 * Copyright 2025 Veld Framework
 */
package io.github.yasmramos.veld.runtime.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = CacheService.getInstance();
        cacheService.clear("test");
    }

    @Test
    void put_and_get_returnsValue() {
        cacheService.put("test", "findById", new Object[]{1L}, "User1", 0, 100);
        Object result = cacheService.get("test", "findById", new Object[]{1L});
        assertEquals("User1", result);
    }

    @Test
    void get_nonexistent_returnsNull() {
        Object result = cacheService.get("test", "findById", new Object[]{999L});
        assertNull(result);
    }

    @Test
    void evict_removesEntry() {
        cacheService.put("test", "findById", new Object[]{1L}, "User1", 0, 100);
        cacheService.evict("test", "findById", new Object[]{1L});
        assertNull(cacheService.get("test", "findById", new Object[]{1L}));
    }

    @Test
    void clear_removesAllEntries() {
        cacheService.put("test", "method1", new Object[]{}, "val1", 0, 100);
        cacheService.put("test", "method2", new Object[]{}, "val2", 0, 100);
        cacheService.clear("test");
        assertNull(cacheService.get("test", "method1", new Object[]{}));
        assertNull(cacheService.get("test", "method2", new Object[]{}));
    }

    @Test
    void generateKey_noArgs_returnsMethodName() {
        String key = cacheService.generateKey("myMethod", null);
        assertEquals("myMethod", key);
    }

    @Test
    void generateKey_withArgs_includesArgs() {
        String key = cacheService.generateKey("findById", new Object[]{1L, "test"});
        assertEquals("findById:1,test", key);
    }

    @Test
    void ttl_expiredEntry_returnsNull() throws InterruptedException {
        cacheService.put("ttl-test", "expiring", new Object[]{}, "value", 20, 100);
        Thread.sleep(150);
        assertNull(cacheService.get("ttl-test", "expiring", new Object[]{}));
    }

    @Test
    void maxSize_evictsOldest() {
        CacheService.Cache cache = cacheService.getCache("small", 0, 2);
        cache.put("key1", "val1");
        cache.put("key2", "val2");
        cache.put("key3", "val3");
        
        assertNull(cache.get("key1")); // Should be evicted
        assertEquals("val2", cache.get("key2"));
        assertEquals("val3", cache.get("key3"));
    }
}
