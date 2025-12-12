package io.github.yasmramos.example.simple;

import io.github.yasmramos.annotation.Component;
import io.github.yasmramos.annotation.Inject;
import io.github.yasmramos.annotation.Singleton;
import io.github.yasmramos.Veld;

/**
 * Ejemplo simple de Veld DI Framework
 * Demuestra inyección de dependencias sin Spring Boot
 */
public class SimpleVeldExample {
    
    @Singleton
    @Component
    public static class MessageService {
        public String getMessage() {
            return "Hello from Veld DI Framework!";
        }
        
        public String getWelcomeMessage(String name) {
            return "Welcome, " + name + "! Using Veld DI.";
        }
    }
    
    @Singleton
    @Component  
    public static class UserService {
        private final MessageService messageService;
        
        @Inject
        public UserService(MessageService messageService) {
            this.messageService = messageService;
        }
        
        public String getUserInfo(String username) {
            return "User: " + username + " - " + messageService.getMessage();
        }
    }
    
    @Component
    public static class SimpleController {
        private final UserService userService;
        
        @Inject
        public SimpleController(UserService userService) {
            this.userService = userService;
        }
        
        public void run() {
            System.out.println("=== Veld DI Framework Simple Example ===");
            System.out.println();
            
            // Demostrar inyección de dependencias
            System.out.println("1. Getting message directly:");
            MessageService messageService = Veld.get(MessageService.class);
            System.out.println("   " + messageService.getMessage());
            System.out.println();
            
            System.out.println("2. Getting user service (with dependency injection):");
            UserService userService = Veld.get(UserService.class);
            System.out.println("   " + userService.getUserInfo("Developer"));
            System.out.println();
            
            System.out.println("3. Multiple calls (singleton behavior):");
            UserService userService2 = Veld.get(UserService.class);
            System.out.println("   Same instance? " + (userService == userService2));
            System.out.println("   " + userService2.getUserInfo("Alice"));
            System.out.println();
            
            System.out.println("✅ Veld DI Framework working perfectly!");
            System.out.println("🚀 Ultra-fast dependency injection without reflection!");
        }
    }
    
    public static void main(String[] args) {
        try {
            // Mostrar información del container
            System.out.println("Veld components available: " + Veld.componentCount());
            
            // Ejecutar ejemplo
            SimpleController controller = Veld.get(SimpleController.class);
            controller.run();
            
            // Cerrar container
            Veld.shutdown();
            
        } catch (Exception e) {
            System.err.println("❌ Error running example: " + e.getMessage());
            e.printStackTrace();
        }
    }
}