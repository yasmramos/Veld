package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Profile;

/**
 * Servicio de base de datos mock para desarrollo.
 */
@Profile("development")
@Singleton
@Component
public class MockDatabaseService {
    @io.github.yasmramos.veld.annotation.PostConstruct
    public void init() {
        System.out.println("  [MockDatabaseService] Usando base de datos mock");
    }
    
    public void saveOrder(Order order) {
        System.out.println("  [MockDatabaseService] Mock guardado de orden: " + order.getId());
    }
    
    public String getUserEmail(String userId) {
        return "mock-" + userId.toLowerCase() + "@example.com";
    }
}
