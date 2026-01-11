/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.spring;

import io.github.yasmramos.veld.benchmark.common.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Spring configuration for benchmark components.
 */
@Configuration
public class SpringConfig {
    
    @Bean
    @Scope("singleton")
    public Logger logger() {
        return new SimpleLogger();
    }
    
    @Bean
    @Scope("singleton")
    public Repository repository() {
        return new SimpleRepository();
    }
    
    @Bean
    @Scope("singleton")
    public Validator validator() {
        return new SimpleValidator();
    }
    
    @Bean
    @Scope("singleton")
    public Service simpleService(Logger logger) {
        return new SimpleService(logger);
    }
    
    @Bean
    @Scope("singleton")
    public ComplexService complexService(Repository repository, Logger logger, Validator validator) {
        return new ComplexService(repository, logger, validator);
    }
    
    @Bean
    @Scope("singleton")
    public DeepService deepService(ComplexService complexService, Logger logger) {
        SimpleService simpleService = new SimpleService(logger);
        return new DeepService(complexService, simpleService, logger);
    }
}
