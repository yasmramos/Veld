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
 * Simple service with single dependency for benchmark testing.
 * This represents the simplest DI scenario.
 */
public class SimpleService implements Service {
    
    private final Logger logger;
    
    public SimpleService(Logger logger) {
        this.logger = logger;
    }
    
    @Override
    public String process(String input) {
        logger.log("Processing: " + input);
        return "processed:" + input;
    }
    
    @Override
    public String getName() {
        return "SimpleService";
    }
    
    public Logger getLogger() {
        return logger;
    }
}
