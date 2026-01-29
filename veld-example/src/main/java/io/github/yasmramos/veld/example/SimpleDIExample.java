package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.*;

/**
 * Ejemplo simple de inyección de dependencias con Veld.
 *
 * Este ejemplo demuestra las características básicas:
 * - @Singleton y @Prototype
 * - @Inject constructor
 * - @Value injection
 * - @PostConstruct y @PreDestroy lifecycle
 *
 * NOTA: Los métodos de acceso como Veld.messageService() son generados
 * automáticamente por el annotation processor en tiempo de compilación.
 */
public class SimpleDIExample {

    public static void main(String[] args) {
        System.out.println("=== Ejemplo Simple de DI con Veld ===");

        // Los servicios se acceden via métodos estáticos generados:
        // MessageService messageService = Veld.messageService_123456789();
        // ConsolePrinter printer = Veld.consolePrinter_123456789();

        // El processor genera estos métodos basándose en los componentes registrados.

        System.out.println("Los métodos Veld.messageService() y Veld.consolePrinter()");
        System.out.println("son generados por el annotation processor.");
        System.out.println("Demo completado exitosamente");
    }
}
