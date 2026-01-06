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

import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Baseline bean with no dependencies for overhead measurement.
 */
@Singleton
public class NoInjectionTestBean {
    private final String value = "test";

    public String getValue() {
        return value;
    }
}
