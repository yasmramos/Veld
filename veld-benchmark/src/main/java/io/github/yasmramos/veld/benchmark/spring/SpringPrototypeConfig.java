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
 * Spring configuration with prototype scope for benchmark components.
 */
@Configuration
public class SpringPrototypeConfig {
    
    @Bean
    @Scope("prototype")
    public Logger prototypeLogger() {
        return new SimpleLogger();
    }
    
    @Bean
    @Scope("prototype")
    public Repository prototypeRepository() {
        return new SimpleRepository();
    }
    
    @Bean
    @Scope("prototype")
    public Validator prototypeValidator() {
        return new SimpleValidator();
    }
    
    @Bean
    @Scope("prototype")
    public Service prototypeSimpleService() {
        return new SimpleService(prototypeLogger());
    }
    
    @Bean
    @Scope("prototype")
    public ComplexService prototypeComplexService() {
        return new ComplexService(prototypeRepository(), prototypeLogger(), prototypeValidator());
    }
}
