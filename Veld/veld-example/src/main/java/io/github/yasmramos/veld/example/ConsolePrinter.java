package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Prototype;

/**
 * Simple console printer - Prototype scope.
 * Demonstrates prototype scope with Veld.
 */
@Prototype
@Component
public class ConsolePrinter {
    
    public void print(String text) {
        System.out.println("[PRINT]: " + text);
    }
}
