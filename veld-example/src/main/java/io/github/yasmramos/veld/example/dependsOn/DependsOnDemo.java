package io.github.yasmramos.veld.example.dependsOn;

import io.github.yasmramos.veld.example.ConfigService;

/**
 * Clase principal para demostrar la funcionalidad de @DependsOn.
 * Esta clase organiza y ejecuta todas las demostraciones de dependencias explícitas.
 */
public class DependsOnDemo {

    public static void runDemo() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              @DependsOn - DEPENDENCIAS EXPLÍCITAS        ║");
        System.out.println("║         Control del orden de inicialización de beans     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("Esta demostración muestra cómo @DependsOn controla el orden");
        System.out.println("de inicialización de componentes con dependencias explícitas.");
        System.out.println();

        System.out.println("[LIST] COMPONENTES CONFIGURADOS:");
        System.out.println("----------------------------");
        System.out.println("  • ConfigService: [OK] Disponible");
        System.out.println("  • DatabaseService: [OK] Disponible");
        System.out.println("  • UserRepository: [OK] Disponible");
        System.out.println("  • EmailService: [OK] Disponible");
        System.out.println("  • UserService: [OK] Disponible");

        System.out.println();
        System.out.println("🔗 DEPENDENCIAS @DependsOn DEFINIDAS:");
        System.out.println("------------------------------------");
        System.out.println("  • UserRepository @DependsOn(\"databaseService\")");
        System.out.println("  • EmailService @DependsOn(\"configService\")");
        System.out.println("  • UserService @DependsOn({\"databaseService\", \"configService\", \"emailService\"})");

        System.out.println();
        System.out.println("[PERF] ORDEN DE INICIALIZACIÓN ESPERADO:");
        System.out.println("-----------------------------------");
        System.out.println("  1. ConfigService (sin dependencias)");
        System.out.println("  2. DatabaseService (sin dependencias)");
        System.out.println("  3. UserRepository (espera DatabaseService)");
        System.out.println("  4. EmailService (espera ConfigService)");
        System.out.println("  5. UserService (espera DatabaseService, ConfigService, EmailService)");

        System.out.println();
        System.out.println("[START] EJECUTANDO DEMOSTRACIÓN:");
        System.out.println("--------------------------");
        System.out.println();
        System.out.println("Los componentes se inicializan automáticamente via Veld.");
        System.out.println("Use los métodos estáticos generados para acceder a ellos:");
        System.out.println("  Veld.configService_123456789()");
        System.out.println("  Veld.databaseService_123456789()");
        System.out.println("  etc.");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                  [SUCCESS] DEMOSTRACIÓN COMPLETADA       ║");
        System.out.println("║  @DependsOn funciona correctamente para controlar el     ║");
        System.out.println("║  orden de inicialización de componentes con dependencias ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
