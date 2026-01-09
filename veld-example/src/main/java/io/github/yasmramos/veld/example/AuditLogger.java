package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Logger de auditoría.
 */
@Singleton
@Component
public class AuditLogger {
    @io.github.yasmramos.veld.annotation.PostConstruct
    public void init() {
        System.out.println("  [AuditLogger] Sistema de auditoría inicializado");
    }
    
    public void log(String message) {
        System.out.println("  [AUDIT] " + message);
    }
}
