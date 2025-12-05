/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark.dagger;

import com.veld.benchmark.common.*;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for benchmark components.
 */
@Module
public class DaggerModule {
    
    @Provides
    @Singleton
    static Logger provideLogger() {
        return new SimpleLogger();
    }
    
    @Provides
    @Singleton
    static Repository provideRepository() {
        return new SimpleRepository();
    }
    
    @Provides
    @Singleton
    static Validator provideValidator() {
        return new SimpleValidator();
    }
    
    @Provides
    @Singleton
    static Service provideSimpleService(Logger logger) {
        return new SimpleService(logger);
    }
    
    @Provides
    @Singleton
    static ComplexService provideComplexService(Repository repository, Logger logger, Validator validator) {
        return new ComplexService(repository, logger, validator);
    }
    
    @Provides
    @Singleton
    static DeepService provideDeepService(ComplexService complexService, Logger logger) {
        SimpleService simpleService = new SimpleService(logger);
        return new DeepService(complexService, simpleService, logger);
    }
}
