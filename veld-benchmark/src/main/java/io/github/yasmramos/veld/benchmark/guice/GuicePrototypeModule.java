/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.yasmramos.veld.benchmark.common.*;

/**
 * Guice module with prototype scope (no scope = new instance each time).
 */
public class GuicePrototypeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // No scope = prototype (new instance each time)
        bind(Logger.class).to(SimpleLogger.class);
        bind(Repository.class).to(SimpleRepository.class);
        bind(Validator.class).to(SimpleValidator.class);
    }
    
    @Provides
    public Service provideSimpleService(Logger logger) {
        return new SimpleService(logger);
    }
    
    @Provides
    public ComplexService provideComplexService(Repository repository, Logger logger, Validator validator) {
        return new ComplexService(repository, logger, validator);
    }
}
