/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.example.nativeimage;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.Veld;

/**
 * Ejemplo simplificado de Native Image para el framework Veld.
 * Esta clase demuestra el uso basico de Veld DI compilado como ejecutable nativo.
 *
 * Para compilar como imagen nativa:
 *   mvn clean package -Pnative -pl veld-example -am
 *
 * Para ejecutar:
 *   ./veld-example/target/veld-native-demo
 */
public class NativeImageDemo {

    /**
     * Componente de servicio simple para demostracion de inyeccion de dependencias.
     */
    @Singleton
    @Component
    public static class GreetingService {
        private final MessageRepository repository;

        public GreetingService(MessageRepository repository) {
            this.repository = repository;
        }

        public String greet(String name) {
            return repository.getMessage() + ", " + name + "!";
        }

        public String getAuthor() {
            return repository.getAuthor();
        }
    }

    /**
     * Repositorio simple para demostracion de inyeccion de dependencia.
     */
    @Singleton
    @Component
    public static class MessageRepository {
        public String getMessage() {
            return "Hola";
        }

        public String getAuthor() {
            return "Veld Framework";
        }
    }

    public static void main(String[] args) {
        long startTime = System.nanoTime();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║        Veld Framework - Native Image Demo                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        // Verificar que Veld esta inicializado
        System.out.println("Inicializando Veld Framework...");
        System.out.println("Veld disponible: " + (Veld.get(GreetingService.class) != null ? "SI" : "NO"));

        // Obtener servicios via Veld
        GreetingService greetingService = Veld.get(GreetingService.class);
        MessageRepository messageRepository = Veld.get(MessageRepository.class);

        // Verificar inyeccion de dependencias
        System.out.println();
        System.out.println("Verificando inyeccion de dependencias:");
        System.out.println("  GreetingService obtuvo MessageRepository: " + (greetingService != null ? "SI" : "NO"));

        // Ejecutar operaciones
        System.out.println();
        System.out.println("Ejecutando operaciones:");
        String greeting = greetingService.greet("Mundo");
        System.out.println("  Saludo: " + greeting);
        System.out.println("  Autor: " + greetingService.getAuthor());

        // Verificar singletons
        System.out.println();
        System.out.println("Verificando comportamiento singleton:");
        GreetingService greetingService2 = Veld.get(GreetingService.class);
        System.out.println("  Misma instancia de GreetingService: " + (greetingService == greetingService2 ? "SI" : "NO"));

        // Calcular tiempo de inicio
        long endTime = System.nanoTime();
        double startupTimeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              Demo completada exitosamente!               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Tiempo de inicio: " + String.format("%.3f", startupTimeMs) + " ms");
        System.out.println("Este ejecutable es una imagen nativa de GraalVM.");
        System.out.println("No se requiere JVM para ejecutar esta aplicacion.");
    }
}
