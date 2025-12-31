/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.features.injection;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Bean with method injection for performance measurement.
 */
@Singleton
public class MethodInjectionTestBean {
    InjectionDependencyBean dependency;

    @Inject
    public void setDependency(InjectionDependencyBean dependency) {
        this.dependency = dependency;
    }

    public InjectionDependencyBean getDependency() {
        return dependency;
    }
}
