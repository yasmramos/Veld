package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Inject;

import java.util.Optional;

/**
 * Servicio de auditoría.
 */
@Singleton
@Component
public class AuditService {
    private final Optional<AuditLogger> auditLogger;
    
    @Inject
    public AuditService(Optional<AuditLogger> auditLogger) {
        this.auditLogger = auditLogger;
    }
    
    public void logAction(String action) {
        auditLogger.ifPresent(logger -> logger.log("ACTION: " + action));
    }
    
    public Optional<AuditLogger> getAuditLogger() {
        return auditLogger;
    }
}
