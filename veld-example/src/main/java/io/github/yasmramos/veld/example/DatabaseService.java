package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.ConditionalOnProperty;
import io.github.yasmramos.veld.annotation.Value;

/**
 * Servicio de base de datos para producción.
 */
@Profile("production")
@ConditionalOnProperty(name = "app.database.enabled", havingValue = "true")
@Singleton
@Component
public class DatabaseService {
    @Value("${app.database.url}")
    private String databaseUrl;
    
    @io.github.yasmramos.veld.annotation.PostConstruct
    public void init() {
        System.out.println("  [DatabaseService] Conectando a: " + databaseUrl);
    }
    
    public void saveOrder(Order order) {
        System.out.println("  [DatabaseService] Guardando orden: " + order.getId());
    }
    
    public String getUserEmail(String userId) {
        return "user@" + userId.toLowerCase() + ".com";
    }
}
