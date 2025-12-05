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
 * Simple repository interface for benchmark testing.
 */
public interface Repository {
    
    String findById(String id);
    
    void save(String id, String data);
    
    int count();
}
