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
package io.github.yasmramos.veld.example.aop;

import io.github.yasmramos.veld.annotation.Around;
import io.github.yasmramos.veld.annotation.Aspect;
import io.github.yasmramos.veld.aop.InvocationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Example aspect for performance monitoring.
 *
 * <p>Tracks execution time and invocation counts for methods.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
@Aspect(order = 2)
public class PerformanceAspect {

    private static final Map<String, LongAdder> invocationCounts = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> totalTimes = new ConcurrentHashMap<>();

    /**
     * Around advice for all service methods (using wildcard).
     *
     * <p>Tracks execution time and invocation count.
     */
    @Around("execution(* io.github.yasmramos.veld.example.aop.*Service.*(..))")
    public Object measurePerformance(InvocationContext ctx) throws Throwable {
        String methodKey = ctx.getDeclaringClass().getSimpleName() + "." + ctx.getMethodName();
        
        invocationCounts.computeIfAbsent(methodKey, k -> new LongAdder()).increment();
        
        long start = System.nanoTime();
        try {
            return ctx.proceed();
        } finally {
            long elapsed = System.nanoTime() - start;
            totalTimes.computeIfAbsent(methodKey, k -> new LongAdder()).add(elapsed);
        }
    }

    /**
     * Prints performance statistics.
     */
    public static void printStatistics() {
        System.out.println("\n========== Performance Statistics ==========");
        invocationCounts.forEach((method, count) -> {
            long total = totalTimes.getOrDefault(method, new LongAdder()).sum();
            double avg = count.sum() > 0 ? (total / (double) count.sum()) / 1_000_000.0 : 0;
            System.out.printf("  %s: %d calls, avg %.3f ms%n", method, count.sum(), avg);
        });
        System.out.println("=============================================\n");
    }

    /**
     * Clears all statistics.
     */
    public static void clearStatistics() {
        invocationCounts.clear();
        totalTimes.clear();
    }
}
