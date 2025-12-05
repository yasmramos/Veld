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
 * Service with deep dependency chain for benchmark testing.
 * This represents a complex DI scenario with nested dependencies.
 * 
 * Dependency graph:
 * DeepService -> ComplexService -> Repository
 *                               -> Logger
 *                               -> Validator
 *             -> SimpleService  -> Logger
 *             -> Logger
 */
public class DeepService implements Service {
    
    private final ComplexService complexService;
    private final SimpleService simpleService;
    private final Logger logger;
    
    public DeepService(ComplexService complexService, SimpleService simpleService, Logger logger) {
        this.complexService = complexService;
        this.simpleService = simpleService;
        this.logger = logger;
    }
    
    @Override
    public String process(String input) {
        logger.log("DeepService processing: " + input);
        
        // Use both services
        String simple = simpleService.process(input);
        String complex = complexService.process(simple);
        
        return "deep:" + complex;
    }
    
    @Override
    public String getName() {
        return "DeepService";
    }
    
    public ComplexService getComplexService() {
        return complexService;
    }
    
    public SimpleService getSimpleService() {
        return simpleService;
    }
    
    public Logger getLogger() {
        return logger;
    }
}
