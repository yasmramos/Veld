/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.veld.benchmark.veld;

import com.veld.annotation.Singleton;
import com.veld.benchmark.common.Validator;

/**
 * Veld implementation of Validator.
 */
@Singleton
public class VeldValidator implements Validator {
    
    @Override
    public boolean validate(String data) {
        return data != null && !data.isEmpty() && data.length() < 1000;
    }
    
    @Override
    public String sanitize(String data) {
        if (data == null) {
            return "";
        }
        return data.trim().replaceAll("[<>\"']", "");
    }
}
