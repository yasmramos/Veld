package io.github.yasmramos.boot.starter.test;

import io.github.yasmramos.annotation.Component;

/**
 * Test component for Veld annotation processor.
 * This will trigger generation of io.github.yasmramos.generated.Veld class.
 */
@Component
public class TestVeldComponent {
    
    public String getMessage() {
        return "Hello from Veld!";
    }
}
