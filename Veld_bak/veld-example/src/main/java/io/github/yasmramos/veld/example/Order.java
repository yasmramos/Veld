package io.github.yasmramos.veld.example;

import java.util.List;

/**
 * Clase de dominio para órdenes de compra.
 */
public class Order {
    private final String id;
    private final String userId;
    private final List<OrderItem> items;
    private final double total;
    
    public Order(String id, String userId, List<OrderItem> items, double total) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.total = total;
    }
    
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return items; }
    public double getTotal() { return total; }
}
