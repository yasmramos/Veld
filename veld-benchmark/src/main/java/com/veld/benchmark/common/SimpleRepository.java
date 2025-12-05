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

import java.util.HashMap;
import java.util.Map;

/**
 * Simple repository implementation for benchmark testing.
 */
public class SimpleRepository implements Repository {
    
    private final Map<String, String> storage = new HashMap<>();
    
    public SimpleRepository() {
        // Initialize with some data
        for (int i = 0; i < 100; i++) {
            storage.put("key" + i, "value" + i);
        }
    }
    
    @Override
    public String findById(String id) {
        return storage.get(id);
    }
    
    @Override
    public void save(String id, String data) {
        storage.put(id, data);
    }
    
    @Override
    public int count() {
        return storage.size();
    }
}
