/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.common;

/**
 * Complex service with multiple dependencies for benchmark testing.
 * This represents a realistic DI scenario with multiple injected components.
 */
public class ComplexService implements Service {
    
    private final Repository repository;
    private final Logger logger;
    private final Validator validator;
    
    public ComplexService(Repository repository, Logger logger, Validator validator) {
        this.repository = repository;
        this.logger = logger;
        this.validator = validator;
    }
    
    @Override
    public String process(String input) {
        logger.log("Starting process for: " + input);
        
        if (!validator.validate(input)) {
            logger.error("Validation failed for: " + input, null);
            return null;
        }
        
        String sanitized = validator.sanitize(input);
        String existing = repository.findById(sanitized);
        
        if (existing == null) {
            repository.save(sanitized, "processed:" + sanitized);
            logger.log("Saved new entry: " + sanitized);
        }
        
        return repository.findById(sanitized);
    }
    
    @Override
    public String getName() {
        return "ComplexService";
    }
    
    public Repository getRepository() {
        return repository;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Validator getValidator() {
        return validator;
    }
}
