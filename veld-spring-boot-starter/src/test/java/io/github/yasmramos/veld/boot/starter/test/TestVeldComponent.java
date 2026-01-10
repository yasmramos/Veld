package io.github.yasmramos.veld.boot.starter.test;

import io.github.yasmramos.veld.annotation.Component;

/**
 * Test component for Veld annotation processor.
 * This will trigger generation of io.github.yasmramos.veld.Veld class.
 */
@Component
public class TestVeldComponent {
    
    public String getMessage() {
        return "Hello from Veld!";
    }
}
