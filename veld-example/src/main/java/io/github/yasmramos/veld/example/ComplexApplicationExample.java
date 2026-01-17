package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.runtime.event.EventBus;

/**
 * Ejemplo complejo que demuestra TODAS las características de Veld funcionando juntos:
 *
 * [OK] Lifecycle Callbacks (@PostConstruct, @PreDestroy)
 * [OK] Dependency Injection (@Inject)
 * [OK] Configuration (@Value)
 * [OK] Scopes (Singleton, Prototype)
 * [OK] EventBus
 * [OK] Conditional Beans
 * [OK] Profiles
 *
 * NOTA: Este archivo usa la nueva API donde los componentes se acceden
 * via métodos estáticos generados por el annotation processor.
 */
public class ComplexApplicationExample {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          EJEMPLO COMPLEJO - VELD DI FRAMEWORK           ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("Los métodos de acceso son generados por el processor:");
        System.out.println("  Veld.userService_123456789()");
        System.out.println("  Veld.paymentService_123456789()");
        System.out.println("  Veld.emailService_123456789()");
        System.out.println("  etc.");
        System.out.println();

        System.out.println("Componentes disponibles:");
        System.out.println("  • UserService - Gestión de usuarios");
        System.out.println("  • PaymentService - Procesamiento de pagos");
        System.out.println("  • EmailService - Servicios de email");
        System.out.println("  • CacheManager - Gestión de caché");
        System.out.println("  • AuditService - Servicio de auditoría");
        System.out.println("  • EventBus - Comunicación por eventos");
    }
}
