/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.veld.benchmark.common.*;

/**
 * Guice module for benchmark components.
 */
public class GuiceModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Logger.class).to(SimpleLogger.class).in(Singleton.class);
        bind(Repository.class).to(SimpleRepository.class).in(Singleton.class);
        bind(Validator.class).to(SimpleValidator.class).in(Singleton.class);
    }
    
    @Provides
    @Singleton
    public Service provideSimpleService(Logger logger) {
        return new SimpleService(logger);
    }
    
    @Provides
    @Singleton
    public ComplexService provideComplexService(Repository repository, Logger logger, Validator validator) {
        return new ComplexService(repository, logger, validator);
    }
    
    @Provides
    @Singleton
    public DeepService provideDeepService(ComplexService complexService, Logger logger) {
        SimpleService simpleService = new SimpleService(logger);
        return new DeepService(complexService, simpleService, logger);
    }
}
