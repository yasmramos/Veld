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

import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.runtime.lifecycle.SmartLifecycle;

/**
 * Example of a SmartLifecycle component that manages database connections.
 * 
 * <p>Uses a low phase number (-1000) to ensure it starts early and stops late,
 * since other components may depend on the database connection.
 */
@Singleton
public class DatabaseConnection implements SmartLifecycle {
    
    private volatile boolean running = false;
    private String connectionUrl = "jdbc:example://localhost:5432/mydb";
    
    @PostConstruct
    public void init() {
        System.out.println("  [DatabaseConnection] @PostConstruct - Preparing connection pool");
    }
    
    @Override
    public void start() {
        System.out.println("  [DatabaseConnection] Starting - Connecting to: " + connectionUrl);
        // Simulate connection
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running = true;
        System.out.println("  [DatabaseConnection] Started - Connection established");
    }
    
    @Override
    public void stop() {
        System.out.println("  [DatabaseConnection] Stopping - Closing connections");
        running = false;
        System.out.println("  [DatabaseConnection] Stopped - Connections closed");
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getPhase() {
        // Start early, stop late
        return -1000;
    }
    
    @Override
    public boolean isAutoStartup() {
        return true;
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("  [DatabaseConnection] @PreDestroy - Releasing resources");
    }
    
    public void executeQuery(String sql) {
        if (!running) {
            throw new IllegalStateException("Database connection not running");
        }
        System.out.println("  [DatabaseConnection] Executing: " + sql);
    }
}
