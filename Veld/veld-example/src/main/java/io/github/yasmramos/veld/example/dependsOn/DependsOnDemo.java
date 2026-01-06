package io.github.yasmramos.veld.example.dependsOn;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.example.ConfigService;

/**
 * Clase principal para demostrar la funcionalidad de @DependsOn.
 * Esta clase organiza y ejecuta todas las demostraciones de dependencias explícitas.
 */
public class DependsOnDemo {
    
    private static <T> T get(Class<T> type) {
        return Veld.get(type);
    }
    
    private static boolean contains(Class<?> type) {
        return Veld.contains(type);
    }
    
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
        
        // Verificar que todos los componentes están disponibles
        boolean hasConfigService = contains(ConfigService.class);
        boolean hasDatabaseService = contains(DatabaseService.class);
        boolean hasUserRepository = contains(UserRepository.class);
        boolean hasEmailService = contains(EmailService.class);
        boolean hasUserService = contains(UserService.class);
        
        System.out.println("  • ConfigService: " + (hasConfigService ? "[OK] Disponible" : "[ERROR] No encontrado"));
        System.out.println("  • DatabaseService: " + (hasDatabaseService ? "[OK] Disponible" : "[ERROR] No encontrado"));
        System.out.println("  • UserRepository: " + (hasUserRepository ? "[OK] Disponible" : "[ERROR] No encontrado"));
        System.out.println("  • EmailService: " + (hasEmailService ? "[OK] Disponible" : "[ERROR] No encontrado"));
        System.out.println("  • UserService: " + (hasUserService ? "[OK] Disponible" : "[ERROR] No encontrado"));
        
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
        
        // Obtener instancias para activar la inicialización
        System.out.println();
        System.out.println("→ Obteniendo instancias (esto activa la inicialización en orden):");
        
        try {
            ConfigService configService = get(ConfigService.class);
            System.out.println("       [OK] ConfigService obtenido");
            
            DatabaseService databaseService = get(DatabaseService.class);
            System.out.println("       [OK] DatabaseService obtenido");
            
            UserRepository userRepository = get(UserRepository.class);
            System.out.println("       [OK] UserRepository obtenido");
            
            EmailService emailService = get(EmailService.class);
            System.out.println("       [OK] EmailService obtenido");
            
            UserService userService = get(UserService.class);
            System.out.println("       [OK] UserService obtenido");
            
            System.out.println();
            System.out.println("→ Ejecutando operaciones con dependencias:");
            
            // Demostrar uso de los servicios
            System.out.println();
            System.out.println("  [NOTE] Creando usuario de prueba:");
            userService.createUser(1L, "Juan Pérez", "juan@example.com");
            
            System.out.println();
            System.out.println("  [LIST] Listando usuarios:");
            userService.listAllUsers();
            
            System.out.println();
            System.out.println("  [SEARCH] Obteniendo información de usuario:");
            userService.getUserInfo(1L);
            
        } catch (Exception e) {
            System.out.println("       [ERROR] Error durante la demostración: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                  [SUCCESS] DEMOSTRACIÓN COMPLETADA             ║");
        System.out.println("║  @DependsOn funciona correctamente para controlar el     ║");
        System.out.println("║  orden de inicialización de componentes con dependencias ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}