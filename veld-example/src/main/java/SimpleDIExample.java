package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.runtime.Veld;

/**
 * Ejemplo simple de inyección de dependencias con Veld.
 * 
 * Este ejemplo demuestra las características básicas:
 * - @Singleton y @Prototype
 * - @Inject constructor
 * - @Value injection
 * - @PostConstruct y @PreDestroy lifecycle
 */
public class SimpleDIExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== Ejemplo Simple de DI con Veld ===");
            
            // Obtener servicios (Veld se inicializa automáticamente)
            MessageService messageService = Veld.get(MessageService.class);
            ConsolePrinter printer = Veld.get(ConsolePrinter.class);
            
            // Usar los servicios
            messageService.sendMessage("¡Hola desde Veld DI!");
            printer.print("Demo completado exitosamente");
            
        } finally {
            // Limpiar recursos
            Veld.shutdown();
        }
    }
}

// =============================================================================
// SERVICIOS SIMPLES
// =============================================================================

@Singleton
@Component
class MessageService {
    private final ConfigService config;
    
    @Inject
    public MessageService(ConfigService config) {
        this.config = config;
    }
    
    public void sendMessage(String message) {
        String prefix = config.getMessagePrefix();
        System.out.println(prefix + ": " + message);
    }
}

@Prototype
@Component
class ConsolePrinter {
    
    public void print(String text) {
        System.out.println("[PRINT]: " + text);
    }
}

@Singleton
@Component
class ConfigService {
    
    @Value("${app.name:Veld App}")
    private String appName;
    
    public String getMessagePrefix() {
        return "[" + appName + "]";
    }
    
    public String getAppName() {
        return appName;
    }
}