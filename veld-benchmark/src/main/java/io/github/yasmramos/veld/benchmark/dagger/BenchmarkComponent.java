/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.dagger;

import io.github.yasmramos.veld.benchmark.common.*;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Dagger component for benchmark.
 */
@Singleton
@Component(modules = DaggerModule.class)
public interface BenchmarkComponent {
    
    Logger logger();
    
    Repository repository();
    
    Validator validator();
    
    Service simpleService();
    
    ComplexService complexService();
    
    DeepService deepService();
}
