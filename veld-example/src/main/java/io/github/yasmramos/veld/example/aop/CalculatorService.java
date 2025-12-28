/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.example.aop;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Timed;
import io.github.yasmramos.veld.annotation.Valid;

/**
 * Example service demonstrating AOP with calculator operations.
 *
 * <p>Methods in this service are intercepted by LoggingAspect
 * and PerformanceAspect.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Component
public class CalculatorService {

    /**
     * Adds two numbers.
     */
    @Timed
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * Subtracts b from a.
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Multiplies two numbers.
     */
    @Timed
    public int multiply(int a, int b) {
        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return a * b;
    }

    /**
     * Divides a by b.
     *
     * @throws ArithmeticException if b is zero
     */
    @Valid
    public double divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return (double) a / b;
    }

    /**
     * Calculates factorial.
     */
    public long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Negative number");
        }
        if (n <= 1) return 1;
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
