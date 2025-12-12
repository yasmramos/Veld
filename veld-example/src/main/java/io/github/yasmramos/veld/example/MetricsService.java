package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple metrics service.
 * This service is intentionally NOT registered as a component to demonstrate
 * Optional<T> injection.
 * 
 * Uncomment @Singleton to register it as a component.
 */
// @Singleton  // <- Uncomment to make it available
public class MetricsService {
    
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        System.out.println("  MetricsService initialized");
    }
    
    public void recordEvent(String eventName) {
        counters.computeIfAbsent(eventName, k -> new AtomicLong()).incrementAndGet();
    }
    
    public long getCount(String eventName) {
        AtomicLong counter = counters.get(eventName);
        return counter != null ? counter.get() : 0;
    }
    
    public void printMetrics() {
        System.out.println("Metrics:");
        counters.forEach((name, count) -> 
            System.out.println("  " + name + ": " + count.get()));
    }
    
    /**
     * Records a request with success/failure status.
     * @param success true if the request was successful, false otherwise
     */
    public void recordRequest(boolean success) {
        if (success) {
            recordEvent("requests.success");
        } else {
            recordEvent("requests.error");
        }
        recordEvent("requests.total");
    }
    
    /**
     * Records a cache access with hit/miss status.
     * @param hit true if the cache was hit, false if it was a miss
     */
    public void recordCacheAccess(boolean hit) {
        if (hit) {
            recordEvent("cache.hits");
        } else {
            recordEvent("cache.misses");
        }
        recordEvent("cache.total");
    }
    
    /**
     * Gets the total number of requests.
     * @return total request count
     */
    public long getTotalRequests() {
        return getCount("requests.total");
    }
    
    /**
     * Gets the number of successful requests.
     * @return successful request count
     */
    public long getSuccessfulRequests() {
        return getCount("requests.success");
    }
    
    /**
     * Gets the number of failed requests.
     * @return failed request count
     */
    public long getFailedRequests() {
        return getCount("requests.error");
    }
    
    /**
     * Gets the cache hit rate.
     * @return cache hit rate as a percentage (0-100)
     */
    public double getCacheHitRate() {
        long total = getCount("cache.total");
        if (total == 0) return 0.0;
        return (getCount("cache.hits") * 100.0) / total;
    }
}
