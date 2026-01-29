/*
 * Copyright 2024 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.yasmramos.veld.benchmark.veld;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Prototype;
import io.github.yasmramos.veld.benchmark.common.Service;

/**
 * Veld prototype implementation of SimpleService.
 * Creates a new instance on each request.
 */
@Prototype
public class VeldPrototypeService implements Service {

    private final VeldLogger logger;

    @Inject
    public VeldPrototypeService(VeldLogger logger) {
        this.logger = logger;
    }

    @Override
    public String process(String input) {
        logger.log("Processing: " + input);
        return "processed:" + input;
    }

    @Override
    public String getName() {
        return "VeldPrototypeService";
    }
}
