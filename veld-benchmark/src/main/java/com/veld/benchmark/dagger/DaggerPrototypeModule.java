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

/**
 * Dagger module for prototype scope (new instance each time).
 */
@Module
public class DaggerPrototypeModule {
    
    @Provides
    static Logger provideLogger() {
        return new SimpleLogger();
    }
    
    @Provides
    static Repository provideRepository() {
        return new SimpleRepository();
    }
    
    @Provides
    static Validator provideValidator() {
        return new SimpleValidator();
    }
    
    @Provides
    static Service provideSimpleService(Logger logger) {
        return new SimpleService(logger);
    }
    
    @Provides
    static ComplexService provideComplexService(Repository repository, Logger logger, Validator validator) {
        return new ComplexService(repository, logger, validator);
    }
}
