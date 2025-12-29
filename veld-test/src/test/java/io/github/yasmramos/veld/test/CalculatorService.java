package io.github.yasmramos.veld.test;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Simple calculator service for integration testing.
 */
@Singleton
@Component
public class CalculatorService {

    public int add(int a, int b) {
        return a + b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }
}
