package io.github.yasmramos.veld.test;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Simple greeting service for integration testing.
 */
@Singleton
@Component
public class GreetingService {

    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
