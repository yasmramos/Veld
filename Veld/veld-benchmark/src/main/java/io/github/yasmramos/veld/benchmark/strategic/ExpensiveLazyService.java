/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.strategic;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Lazy;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.benchmark.veld.VeldLogger;

/**
 * Expensive lazy service for contention testing.
 * Simulates a service that should be lazily initialized and causes high contention
 * when accessed concurrently from multiple threads.
 */
@Lazy
@Singleton
public class ExpensiveLazyService {
    
    private final VeldLogger logger;
    private final String initializationData;
    
    // Simulate expensive initialization
    @Inject
    public ExpensiveLazyService(VeldLogger logger) {
        this.logger = logger;
        
        // Expensive initialization to simulate real-world lazy service
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("expensive_data_").append(i).append("_");
        }
        this.initializationData = sb.toString();
        
        logger.log("ExpensiveLazyService initialized with " + initializationData.length() + " characters");
    }
    
    public String getInitializationData() {
        return initializationData;
    }
    
    public VeldLogger getLogger() {
        return logger;
    }
    
    // Add some processing methods to make it realistic
    public String processData(String input) {
        return "Processed: " + input + " with " + initializationData.substring(0, Math.min(50, initializationData.length()));
    }
    
    public boolean isInitialized() {
        return initializationData != null && !initializationData.isEmpty();
    }
}