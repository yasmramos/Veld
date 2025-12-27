package io.github.yasmramos.veld.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Supplier;

/**
 * Simple metrics registry for counters, gauges, and timers.
 */
public class MetricsRegistry {
    
    private static final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<Double>> gauges = new ConcurrentHashMap<>();
    private static final Map<String, Timer> timers = new ConcurrentHashMap<>();
    
    public static void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
    }
    
    public static void incrementCounter(String name, long delta) {
        counters.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(delta);
    }
    
    public static long getCounter(String name) {
        AtomicLong counter = counters.get(name);
        return counter != null ? counter.get() : 0;
    }
    
    public static void registerGauge(String name, Supplier<Double> supplier) {
        gauges.put(name, supplier);
    }
    
    public static Double getGauge(String name) {
        Supplier<Double> supplier = gauges.get(name);
        return supplier != null ? supplier.get() : null;
    }
    
    public static Timer getTimer(String name) {
        return timers.computeIfAbsent(name, k -> new Timer());
    }
    
    public static void recordTime(String name, long durationMs) {
        getTimer(name).record(durationMs);
    }
    
    public static Map<String, Object> getAllMetrics() {
        Map<String, Object> all = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> all.put("counter." + k, v.get()));
        gauges.forEach((k, v) -> all.put("gauge." + k, v.get()));
        timers.forEach((k, v) -> all.put("timer." + k, v.getStats()));
        return all;
    }
    
    public static class Timer {
        private final AtomicLong count = new AtomicLong();
        private final DoubleAdder totalTime = new DoubleAdder();
        private volatile double min = Double.MAX_VALUE;
        private volatile double max = Double.MIN_VALUE;
        
        public void record(long durationMs) {
            count.incrementAndGet();
            totalTime.add(durationMs);
            min = Math.min(min, durationMs);
            max = Math.max(max, durationMs);
        }
        
        public Map<String, Double> getStats() {
            long c = count.get();
            return Map.of(
                "count", (double) c,
                "total", totalTime.sum(),
                "mean", c > 0 ? totalTime.sum() / c : 0,
                "min", min == Double.MAX_VALUE ? 0 : min,
                "max", max == Double.MIN_VALUE ? 0 : max
            );
        }
    }
}
