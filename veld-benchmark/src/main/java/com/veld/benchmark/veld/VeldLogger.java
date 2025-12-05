/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark.veld;

import com.veld.annotation.Singleton;
import com.veld.benchmark.common.Logger;

/**
 * Veld implementation of Logger.
 */
@Singleton
public class VeldLogger implements Logger {
    
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
