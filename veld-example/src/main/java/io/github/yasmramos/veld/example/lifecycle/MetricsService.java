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

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.runtime.lifecycle.DisposableBean;
import io.github.yasmramos.veld.runtime.lifecycle.InitializingBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example of implementing InitializingBean and DisposableBean interfaces.
 */
@Singleton
public class MetricsService implements InitializingBean, DisposableBean {
    
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private boolean initialized = false;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("  [MetricsService] InitializingBean.afterPropertiesSet()");
        
        // Initialize default counters
        counters.put("requests.total", new AtomicLong(0));
        counters.put("requests.success", new AtomicLong(0));
        counters.put("requests.error", new AtomicLong(0));
        counters.put("cache.hits", new AtomicLong(0));
        counters.put("cache.misses", new AtomicLong(0));
        
        initialized = true;
        System.out.println("  [MetricsService] Initialized with " + counters.size() + " counters");
    }
    
    @Override
    public void destroy() throws Exception {
        System.out.println("  [MetricsService] DisposableBean.destroy()");
        System.out.println("  [MetricsService] Final metrics:");
        counters.forEach((name, value) -> 
            System.out.println("    - " + name + ": " + value.get())
        );
        counters.clear();
        initialized = false;
    }
    
    public void increment(String counter) {
        if (!initialized) {
            throw new IllegalStateException("MetricsService not initialized");
        }
        counters.computeIfAbsent(counter, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public long get(String counter) {
        AtomicLong value = counters.get(counter);
        return value != null ? value.get() : 0;
    }
    
    public void recordRequest(boolean success) {
        increment("requests.total");
        if (success) {
            increment("requests.success");
        } else {
            increment("requests.error");
        }
    }
    
    public void recordCacheAccess(boolean hit) {
        if (hit) {
            increment("cache.hits");
        } else {
            increment("cache.misses");
        }
    }
}
