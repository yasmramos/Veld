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
package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.annotation.AroundInvoke;
import io.github.yasmramos.veld.annotation.Interceptor;
import io.github.yasmramos.veld.aop.InvocationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Interceptor that measures method execution time.
 *
 * <p>Records execution time and provides statistics.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Interceptor(priority = 50)
@Timed
public class TimingInterceptor {

    private static final Map<String, MethodStats> statistics = new ConcurrentHashMap<>();

    /**
     * Statistics for a method.
     */
    public static class MethodStats {
        private final String methodName;
        private final LongAdder invocationCount = new LongAdder();
        private final LongAdder totalTimeNanos = new LongAdder();
        private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTimeNanos = new AtomicLong(0);

        MethodStats(String methodName) {
            this.methodName = methodName;
        }

        void record(long nanos) {
            invocationCount.increment();
            totalTimeNanos.add(nanos);
            
            // Update min
            long currentMin;
            do {
                currentMin = minTimeNanos.get();
            } while (nanos < currentMin && !minTimeNanos.compareAndSet(currentMin, nanos));
            
            // Update max
            long currentMax;
            do {
                currentMax = maxTimeNanos.get();
            } while (nanos > currentMax && !maxTimeNanos.compareAndSet(currentMax, nanos));
        }

        public String getMethodName() {
            return methodName;
        }

        public long getInvocationCount() {
            return invocationCount.sum();
        }

        public long getTotalTimeNanos() {
            return totalTimeNanos.sum();
        }

        public long getMinTimeNanos() {
            long min = minTimeNanos.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getMaxTimeNanos() {
            return maxTimeNanos.get();
        }

        public double getAverageTimeNanos() {
            long count = invocationCount.sum();
            return count == 0 ? 0 : (double) totalTimeNanos.sum() / count;
        }

        @Override
        public String toString() {
            return String.format("%s: count=%d, avg=%.3fms, min=%.3fms, max=%.3fms",
                    methodName,
                    getInvocationCount(),
                    getAverageTimeNanos() / 1_000_000.0,
                    getMinTimeNanos() / 1_000_000.0,
                    getMaxTimeNanos() / 1_000_000.0);
        }
    }

    @AroundInvoke
    public Object measureTime(InvocationContext ctx) throws Throwable {
        // Use zero-reflection API for method identification
        String methodName = ctx.getDeclaringClass().getSimpleName() + "." + ctx.getMethodName();
        
        // Get annotation configuration via declaring class
        Class<?> declaringClass = ctx.getDeclaringClass();
        Method method = ctx.getMethod(); // May be null in zero-reflection mode
        
        Timed config = null;
        if (method != null) {
            config = method.getAnnotation(Timed.class);
        }
        if (config == null && declaringClass != null) {
            config = declaringClass.getAnnotation(Timed.class);
        }

        Timed.Unit unit = config != null ? config.unit() : Timed.Unit.MILLISECONDS;
        long warnThreshold = config != null ? config.warnThreshold() : 0;

        long startTime = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            long elapsed = System.nanoTime() - startTime;
            
            // Record statistics
            statistics.computeIfAbsent(methodName, MethodStats::new).record(elapsed);

            // Convert to requested unit
            double elapsedInUnit = convertNanos(elapsed, unit);
            String unitStr = getUnitString(unit);

            System.out.printf("[TIMING] %s executed in %.3f %s%n", methodName, elapsedInUnit, unitStr);

            // Check threshold
            if (warnThreshold > 0) {
                long thresholdNanos = convertToNanos(warnThreshold, unit);
                if (elapsed > thresholdNanos) {
                    System.out.printf("[TIMING] WARNING: %s exceeded threshold (%.3f > %d %s)%n",
                            methodName, elapsedInUnit, warnThreshold, unitStr);
                }
            }
        }
    }

    private double convertNanos(long nanos, Timed.Unit unit) {
        switch (unit) {
            case NANOSECONDS: return nanos;
            case MICROSECONDS: return nanos / 1_000.0;
            case MILLISECONDS: return nanos / 1_000_000.0;
            case SECONDS: return nanos / 1_000_000_000.0;
            default: return nanos / 1_000_000.0;
        }
    }

    private long convertToNanos(long value, Timed.Unit unit) {
        switch (unit) {
            case NANOSECONDS: return value;
            case MICROSECONDS: return value * 1_000;
            case MILLISECONDS: return value * 1_000_000;
            case SECONDS: return value * 1_000_000_000;
            default: return value * 1_000_000;
        }
    }

    private String getUnitString(Timed.Unit unit) {
        switch (unit) {
            case NANOSECONDS: return "ns";
            case MICROSECONDS: return "µs";
            case MILLISECONDS: return "ms";
            case SECONDS: return "s";
            default: return "ms";
        }
    }

    /**
     * Gets statistics for all timed methods.
     *
     * @return map of method name to statistics
     */
    public static Map<String, MethodStats> getStatistics() {
        return statistics;
    }

    /**
     * Clears all recorded statistics.
     */
    public static void clearStatistics() {
        statistics.clear();
    }

    /**
     * Prints all statistics.
     */
    public static void printStatistics() {
        System.out.println("\n=== Method Timing Statistics ===");
        statistics.values().forEach(System.out::println);
        System.out.println("================================\n");
    }
}
