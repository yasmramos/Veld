package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.runtime.event.Event;

import java.util.List;

/**
 * Servicio para gestionar órdenes de compra.
 * Only available in production profile since it depends on production services.
 */
@Profile("production")
@Singleton
@Component
public class OrderService {
    private final UserService userService;
    private final DatabaseService databaseService;
    
    @Inject
    public OrderService(UserService userService, DatabaseService databaseService) {
        this.userService = userService;
        this.databaseService = databaseService;
    }
    
    @io.github.yasmramos.veld.annotation.PostConstruct
    public void init() {
        System.out.println("  [OrderService] Inicializando servicio de órdenes...");
    }
    
    /**
     * Crea una nueva orden de compra.
     * @param userId ID del usuario
     * @param productName Nombre del producto
     * @param amount Monto de la orden
     * @return ID de la orden creada
     */
    public String createOrder(Long userId, String productName, double amount) {
        String orderId = "ORDER-" + userId + "-" + System.currentTimeMillis();
        System.out.println("  [OrderService] Creating order: " + orderId + " for user: " + userId);
        System.out.println("  [OrderService] Product: " + productName + ", Amount: $" + amount);
        return orderId;
    }
}
