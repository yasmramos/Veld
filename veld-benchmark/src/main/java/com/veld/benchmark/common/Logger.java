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
 * Simple logger interface for benchmark testing.
 */
public interface Logger {
    
    void log(String message);
    
    void debug(String message);
    
    void error(String message, Throwable throwable);
}
