/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark.common;

/**
 * Simple logger implementation for benchmark testing.
 * Uses a no-op implementation to avoid I/O overhead in benchmarks.
 */
public class SimpleLogger implements Logger {
    
    private volatile int logCount = 0;
    
    @Override
    public void log(String message) {
        logCount++;
    }
    
    @Override
    public void debug(String message) {
        logCount++;
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        logCount++;
    }
    
    public int getLogCount() {
        return logCount;
    }
}
