package com.veld.boot.starter.test;

import com.veld.annotation.Component;

/**
 * Test component for Veld annotation processor.
 * This will trigger generation of com.veld.generated.Veld class.
 */
@Component
public class TestVeldComponent {
    
    public String getMessage() {
        return "Hello from Veld!";
    }
}
