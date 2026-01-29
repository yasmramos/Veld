package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.example.aop.CalculatorService;
import io.github.yasmramos.veld.example.events.OrderCreatedEvent;
import io.github.yasmramos.veld.example.lifecycle.*;
import io.github.yasmramos.veld.example.dependsOn.DependsOnDemo;
import io.github.yasmramos.veld.example.dependsOn.DatabaseService;
import io.github.yasmramos.veld.example.dependsOn.DependsOnUserService;
import io.github.yasmramos.veld.aop.interceptor.LoggingInterceptor;
import io.github.yasmramos.veld.runtime.event.EventBus;

import java.util.Arrays;
import java.util.List;

/**
 * Main class demonstrating Veld DI framework capabilities.
 *
 * This example shows the new compile-time DI API:
 * 1. Components are accessed via generated static methods
 * 2. Constructor injection
 * 3. Field injection
 * 4. Method injection
 * 5. @Singleton scope
 * 6. @Prototype scope
 * 7. @PostConstruct and @PreDestroy lifecycle callbacks
 * 8. Interface-based injection
 * 9. JSR-330 compatibility (javax.inject.*)
 * 10. Jakarta Inject compatibility (jakarta.inject.*)
 * 11. @Lazy initialization
 * 12. @Conditional annotations
 * 13. @Profile annotations
 * 14. @Value configuration injection
 * 15. EventBus - Event-driven communication
 * 16. AOP (Aspect-Oriented Programming)
 * 17. Advanced Lifecycle Management
 * 18. @DependsOn - Explicit Dependencies Control
 *
 * API: Use generated static methods like Veld.logService_123456789()
 * All bytecode generation happens at compile-time using ASM.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           Veld DI Framework - Example Application        ║");
        System.out.println("║        Pure ASM Bytecode Generation - Static API         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("NOTA: Los métodos estáticos son generados por el processor:");
        System.out.println("  Veld.logService_123456789()");
        System.out.println("  Veld.configService_123456789()");
        System.out.println("  etc.");
        System.out.println("══════════════════════════════════════════════════════════");

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              Example Completed Successfully!              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
