/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.veld;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.benchmark.common.*;

/**
 * Veld implementation of ComplexService.
 */
@Singleton
public class VeldComplexService implements Service {
    
    private final Repository repository;
    private final Logger logger;
    private final Validator validator;
    
    @Inject
    public VeldComplexService(Repository repository, Logger logger, Validator validator) {
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
        return "VeldComplexService";
    }
}
