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

import io.github.yasmramos.veld.annotation.OnStart;
import io.github.yasmramos.veld.annotation.OnStop;
import io.github.yasmramos.veld.annotation.Singleton;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Example of using @OnStart and @OnStop for managing scheduled tasks.
 */
@Singleton
public class ScheduledTaskRunner {
    
    private ScheduledExecutorService executor;
    private final AtomicInteger taskCount = new AtomicInteger(0);
    
    @OnStart(order = 100)
    public void startScheduler() {
        System.out.println("  [ScheduledTaskRunner] @OnStart - Starting scheduler");
        executor = Executors.newScheduledThreadPool(2);
        
        // Schedule a simple task
        executor.scheduleAtFixedRate(() -> {
            int count = taskCount.incrementAndGet();
            System.out.println("  [ScheduledTaskRunner] Task executed #" + count);
        }, 100, 500, TimeUnit.MILLISECONDS);
        
        System.out.println("  [ScheduledTaskRunner] Scheduler started with 2 threads");
    }
    
    @OnStop(order = 100)
    public void stopScheduler() {
        System.out.println("  [ScheduledTaskRunner] @OnStop - Stopping scheduler");
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("  [ScheduledTaskRunner] Scheduler stopped. Total tasks: " + taskCount.get());
    }
    
    public int getTaskCount() {
        return taskCount.get();
    }
}
