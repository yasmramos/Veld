package io.github.yasmramos.veld.test;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.test.context.TestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para el módulo veld-test.
 * 
 * <p>Estas pruebas verifican el funcionamiento correcto de las
 * anotaciones y la extensión JUnit 5.</p>
 */
class VeldTestIntegrationTest {
    
    /**
     * Prueba básica de inyección de beans reales.
     */
    @Nested
    @DisplayName("Pruebas de Inyección Básica")
    class BasicInjectionTests {
        
        @Test
        @DisplayName("Debe inyectar beans reales correctamente")
        void shouldInjectRealBeans() {
            try (TestContext context = TestContext.Builder.create()
                    .withProfile("test")
                    .build()) {
                GreetingService service = context.getBean(GreetingService.class);
                assertThat(service).isNotNull();
                assertThat(service.greet("World")).isEqualTo("Hello, World!");
            }
        }
        
        @Test
        @DisplayName("Debe permitir acceso a múltiples beans")
        void shouldAllowMultipleBeans() {
            try (TestContext context = TestContext.Builder.create()
                    .withProfile("test")
                    .build()) {
                GreetingService service = context.getBean(GreetingService.class);
                CalculatorService calculator = context.getBean(CalculatorService.class);
                assertThat(service).isNotNull();
                assertThat(calculator).isNotNull();
                    
                assertThat(service.greet("Test")).isEqualTo("Hello, Test!");
                assertThat(calculator.add(2, 3)).isEqualTo(5);
            }
        }
    }
    
    /**
     * Prueba de uso de TestContext directamente.
     */
    @Nested
    @DisplayName("Pruebas de TestContext")
    class TestContextTests {
        
        @Test
        @DisplayName("Debe permitir uso directo de TestContext")
        void shouldAllowDirectTestContextUsage() {
            // Crear contexto directamente
            try (TestContext context = TestContext.Builder.create()
                    .withProfile("test")
                    .build()) {
                
                // Obtener bean
                GreetingService service = context.getBean(GreetingService.class);
                assertThat(service).isNotNull();
                assertThat(service.greet("Direct")).isEqualTo("Hello, Direct!");
            }
        }
        
        @Test
        @DisplayName("Debe permitir registro de mocks manuales")
        void shouldAllowManualMockRegistration() {
            MessageRepository mockRepo = org.mockito.Mockito.mock(MessageRepository.class);
            
            try (TestContext context = TestContext.Builder.create()
                    .withMock("mockRepository", mockRepo)
                    .build()) {
                
                MessageService service = context.getBean(MessageService.class);
                org.mockito.Mockito.when(mockRepo.getMessage("manual")).thenReturn("Manually Mocked");
                
                assertThat(service.getMessage("manual")).isEqualTo("Manually Mocked");
            }
        }
    }
    
    // === Beans de prueba ===
    
    @Singleton
    @Component
    static class GreetingService {
        public String greet(String name) {
            return "Hello, " + name + "!";
        }
    }
    
    @Singleton
    @Component
    static class CalculatorService {
        public int add(int a, int b) {
            return a + b;
        }
        
        public int multiply(int a, int b) {
            return a * b;
        }
    }
    
    interface MessageRepository {
        String getMessage(String key);
        void saveMessage(String key, String value);
    }
    
    @Singleton
    @Component
    static class MessageService {
        private final MessageRepository repository;
        
        public MessageService(MessageRepository repository) {
            this.repository = repository;
        }
        
        public String getMessage(String key) {
            return repository.getMessage(key);
        }
        
        public void saveMessage(String key, String value) {
            repository.saveMessage(key, value);
        }
    }
}
