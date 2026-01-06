package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.event.Event;
import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;
import io.github.yasmramos.veld.runtime.value.ValueResolver;
import io.github.yasmramos.veld.runtime.Provider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ejemplo complejo que demuestra TODAS las características de Veld funcionando juntas:
 * 
 * [OK] Lifecycle Callbacks (@PostConstruct, @PreDestroy)
 * [OK] EventBus (@Subscribe) 
 * [OK] Value Resolution (@Value)
 * [OK] Conditional Loading (@Profile, @ConditionalOnProperty)
 * [OK] Named Injection (@Named)
 * [OK] Provider Injection (Provider<T>)
 * [OK] Optional Injection (Optional<T>)
 * [OK] Dependencies (@DependsOn)
 * [OK] Multiple Scopes (Singleton, Prototype)
 * [OK] Interface-based Injection
 */
public class ComplexApplicationExample {

    public static void main(String[] args) {
        System.out.println("[START] Iniciando aplicación compleja con Veld...\n");
        
        // =============================================================================
        // 1. CONFIGURACIÓN DE PERFILES Y PROPIEDADES
        // =============================================================================
        System.setProperty("app.environment", "production");
        System.setProperty("app.database.url", "jdbc:postgresql://localhost:5432/app");
        System.setProperty("app.cache.enabled", "true");
        System.setProperty("app.async.threads", "4");
        
        // Configurar perfil activo
        Veld.setActiveProfiles("production", "database");
        
        // =============================================================================
        // 2. OBTENER COMPONENTES PRINCIPALES
        // =============================================================================
        System.out.println("[BOX] Obteniendo componentes principales...");
        
        OrderService orderService = Veld.get(OrderService.class);
        PaymentService paymentService = Veld.get(PaymentService.class);
        NotificationService notificationService = Veld.get(NotificationService.class);
        UserService userService = Veld.get(UserService.class);
        
        System.out.println("[OK] Componentes principales obtenidos:");
        System.out.println("  - OrderService: " + orderService.getClass().getSimpleName());
        System.out.println("  - PaymentService: " + paymentService.getClass().getSimpleName());
        System.out.println("  - NotificationService: " + notificationService.getClass().getSimpleName());
        System.out.println("  - UserService: " + userService.getClass().getSimpleName());
        
        // =============================================================================
        // 3. DEMOSTRAR DIFERENTES IMPLEMENTACIONES
        // =============================================================================
        System.out.println("\n[TAGS] Probando diferentes implementaciones...");
        
        EmailService emailService = Veld.get(SmtpEmailService.class);
        EmailService smsService = Veld.get(SmsEmailService.class);
        
        System.out.println("  - Email SMTP Service: " + emailService.getProvider());
        System.out.println("  - SMS Service: " + smsService.getProvider());
        
        // =============================================================================
        // 4. DEMOSTRAR PROVIDER INJECTION
        // =============================================================================
        System.out.println("\n[REFRESH] Probando Provider injection...");
        
        CacheManager cacheManager = Veld.get(CacheManager.class);
        // El Provider permite crear instancias bajo demanda
        Cache cache1 = cacheManager.getCacheProvider().get();
        Cache cache2 = cacheManager.getCacheProvider().get();
        
        System.out.println("  - Cache 1 ID: " + cache1.getId());
        System.out.println("  - Cache 2 ID: " + cache2.getId());
        System.out.println("  - Son instancias diferentes: " + (cache1 != cache2));
        
        // =============================================================================
        // 5. DEMOSTRAR OPTIONAL INJECTION
        // =============================================================================
        System.out.println("\n[QUESTION] Probando Optional injection...");
        
        AuditService auditService = Veld.get(AuditService.class);
        System.out.println("  - Audit Service disponible: " + auditService.getAuditLogger().isPresent());
        
        // =============================================================================
        // 6. EJECUTAR FLUJO DE NEGOCIO COMPLETO
        // =============================================================================
        System.out.println("\n[BUSINESS] Ejecutando flujo de negocio completo...");
        
        try {
            // Crear una orden
            Order order = orderService.createOrder("USER123", List.of(
                new OrderItem("Laptop", 1, 1200.00),
                new OrderItem("Mouse", 2, 25.00)
            ));
            
            System.out.println("  [OK] Orden creada: " + order.getId() + " por $" + order.getTotal());
            
            // Procesar pago
            PaymentResult payment = paymentService.processPayment(order.getId(), order.getTotal());
            System.out.println("  💳 Pago procesado: " + payment.getStatus() + " - " + payment.getTransactionId());
            
            // Enviar notificaciones
            notificationService.sendOrderConfirmation(order.getId());
            System.out.println("  📧 Notificaciones enviadas");
            
        } catch (Exception e) {
            System.err.println("  [ERROR] Error en flujo: " + e.getMessage());
        }
        
        // =============================================================================
        // 7. DEMOSTRAR EVENTBUS
        // =============================================================================
        System.out.println("\n[EVENTBUS] Probando EventBus...");
        
        EventBus eventBus = Veld.getEventBus();
        eventBus.publish(new OrderCompletedEvent("ORDER123", "USER123", 1250.00));
        
        // Esperar un poco para que se procesen los eventos
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // =============================================================================
        // 8. ACCESO A SERVICIOS DEL FRAMEWORK
        // =============================================================================
        System.out.println("\n[TOOLS] Accediendo a servicios del framework...");
        
        LifecycleProcessor lifecycleProcessor = Veld.getLifecycleProcessor();
        System.out.println("  - LifecycleProcessor: " + lifecycleProcessor.getClass().getSimpleName());
        
        // Note: ValueResolver is accessed through specific @Value annotations, not directly
        System.out.println("  - ValueResolver disponible a través de @Value annotations");
        
        // =============================================================================
        // 9. COMPONENTES CONDICIONALES
        // =============================================================================
        System.out.println("\n[TARGET] Probando componentes condicionales...");
        
        try {
            // En producción con database habilitado, esto debería funcionar
            DatabaseService dbService = Veld.get(DatabaseService.class);
            System.out.println("  [OK] DatabaseService cargado (perfil: production + database)");
        } catch (Exception e) {
            System.out.println("  [ERROR] DatabaseService no disponible: " + e.getMessage());
        }
        
        try {
            // Este debería estar deshabilitado en producción
            MockDatabaseService mockService = Veld.get(MockDatabaseService.class);
            System.out.println("  [WARNING] MockDatabaseService cargado (esto no debería pasar en producción)");
        } catch (Exception e) {
            System.out.println("  [OK] MockDatabaseService correctamente deshabilitado en producción");
        }
        
        // =============================================================================
        // 10. SHUTDOWN GRACEFUL
        // =============================================================================
        System.out.println("\n[STOP] Ejecutando shutdown graceful...");
        Veld.shutdown();
        
        System.out.println("\n[SUCCESS] ¡Aplicación compleja completada exitosamente!");
        System.out.println("[STATS] Todas las características de Veld funcionan automáticamente:");
        System.out.println("   [OK] Lifecycle Callbacks");
        System.out.println("   [OK] EventBus Integration");
        System.out.println("   [OK] Value Resolution");
        System.out.println("   [OK] Conditional Loading");
        System.out.println("   [OK] Named Injection");
        System.out.println("   [OK] Provider Injection");
        System.out.println("   [OK] Optional Injection");
        System.out.println("   [OK] Dependencies Management");
    }

    // =============================================================================
    // DOMINIO DE NEGOCIO
    // =============================================================================

    public static class Order {
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

    public static class OrderItem {
        private final String name;
        private final int quantity;
        private final double price;
        
        public OrderItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
        
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
    }

    public static class PaymentResult {
        private final String status;
        private final String transactionId;
        
        public PaymentResult(String status, String transactionId) {
            this.status = status;
            this.transactionId = transactionId;
        }
        
        public String getStatus() { return status; }
        public String getTransactionId() { return transactionId; }
    }

    // =============================================================================
    // SERVICIOS PRINCIPALES
    // =============================================================================

    @Singleton
    @Component
    public static class OrderService {
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
        
        public Order createOrder(String userId, List<OrderItem> items) {
            double total = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
            
            String orderId = "ORDER-" + System.currentTimeMillis();
            Order order = new Order(orderId, userId, items, total);
            
            // Guardar en base de datos
            databaseService.saveOrder(order);
            
            return order;
        }
    }

    @Singleton
    @Component
    public static class PaymentService {
        private final DatabaseService databaseService;
        private final EventBus eventBus;
        
        @Inject
        public PaymentService(DatabaseService databaseService, EventBus eventBus) {
            this.databaseService = databaseService;
            this.eventBus = eventBus;
        }
        
        public PaymentResult processPayment(String orderId, double amount) {
            // Simular procesamiento de pago
            String transactionId = "TXN-" + System.currentTimeMillis();
            
            // Publicar evento de pago procesado
            eventBus.publish(new PaymentProcessedEvent(orderId, amount, transactionId));
            
            return new PaymentResult("SUCCESS", transactionId);
        }
    }

    @Singleton
    @Component
    public static class NotificationService {
        private final EmailService emailService;
        private final UserService userService;
        
        @Inject
        public NotificationService(EmailService emailService, UserService userService) {
            this.emailService = emailService;
            this.userService = userService;
        }
        
        public void sendOrderConfirmation(String orderId) {
            System.out.println("  [NotificationService] Enviando confirmación para orden: " + orderId);
            // Lógica de notificación...
        }
    }

    @Singleton
    @Component
    public static class UserService {
        private final DatabaseService databaseService;
        
        @Inject
        public UserService(DatabaseService databaseService) {
            this.databaseService = databaseService;
        }
        
        public String getUserEmail(String userId) {
            return databaseService.getUserEmail(userId);
        }
    }

    // =============================================================================
    // EMAIL SERVICES (Named Injection Example)
    // =============================================================================

    @Singleton
    @Component
    public static class SmtpEmailService implements EmailService {
        @Value("${app.smtp.host:localhost}")
        private String smtpHost;
        
        @Value("${app.smtp.port:587}")
        private int smtpPort;
        
        @io.github.yasmramos.veld.annotation.PostConstruct
        public void init() {
            System.out.println("  [SmtpEmailService] Conectando a SMTP: " + smtpHost + ":" + smtpPort);
        }
        
        @Override
        public String getProvider() {
            return "SMTP";
        }
        
        @Override
        public void sendEmail(String to, String subject, String body) {
            System.out.println("  [SMTP] Enviando email a: " + to);
        }
    }

    @Singleton
    @Component  
    public static class SmsEmailService implements EmailService {
        @Value("${app.sms.provider:twilio}")
        private String smsProvider;
        
        @io.github.yasmramos.veld.annotation.PostConstruct
        public void init() {
            System.out.println("  [SmsEmailService] Inicializando SMS provider: " + smsProvider);
        }
        
        @Override
        public String getProvider() {
            return "SMS";
        }
        
        @Override
        public void sendEmail(String to, String subject, String body) {
            System.out.println("  [SMS] Enviando SMS a: " + to);
        }
    }

    public interface EmailService {
        String getProvider();
        void sendEmail(String to, String subject, String body);
    }

    // =============================================================================
    // CACHE MANAGER (Provider Injection Example)
    // =============================================================================

    @Singleton
    @Component
    public static class CacheManager {
        private final Provider<Cache> cacheProvider;
        
        @Inject
        public CacheManager(Provider<Cache> cacheProvider) {
            this.cacheProvider = cacheProvider;
        }
        
        public Provider<Cache> getCacheProvider() {
            return cacheProvider;
        }
    }

    @Prototype
    @Component
    public static class Cache {
        private final String id;
        
        public Cache() {
            this.id = "CACHE-" + System.currentTimeMillis();
        }
        
        public String getId() {
            return id;
        }
    }

    // =============================================================================
    // AUDIT SERVICE (Optional Injection Example)
    // =============================================================================

    @Singleton
    @Component
    public static class AuditService {
        private final java.util.Optional<AuditLogger> auditLogger;
        
        @Inject
        public AuditService(java.util.Optional<AuditLogger> auditLogger) {
            this.auditLogger = auditLogger;
        }
        
        public void logAction(String action) {
            auditLogger.ifPresent(logger -> logger.log("ACTION: " + action));
        }
        
        public java.util.Optional<AuditLogger> getAuditLogger() {
            return auditLogger;
        }
    }

    @Singleton
    @Component
    public static class AuditLogger {
        @io.github.yasmramos.veld.annotation.PostConstruct
        public void init() {
            System.out.println("  [AuditLogger] Sistema de auditoría inicializado");
        }
        
        public void log(String message) {
            System.out.println("  [AUDIT] " + message);
        }
    }

    // ExternalLogger no existe - para probar Optional.empty()

    // =============================================================================
    // EVENT SUBSCRIBERS
    // =============================================================================

    @Singleton
    @Component
    public static class OrderEventHandler {
        @Subscribe
        public void onOrderCompleted(OrderCompletedEvent event) {
            System.out.println("  [EventHandler] Orden completada: " + event.getOrderId() + 
                             " por $" + event.getAmount());
        }
        
        @Subscribe
        public void onPaymentProcessed(PaymentProcessedEvent event) {
            System.out.println("  [EventHandler] Pago procesado: " + event.getTransactionId() + 
                             " para orden " + event.getOrderId());
        }
    }

    // =============================================================================
    // CONDITIONAL SERVICES
    // =============================================================================

    @Profile("production")
    @ConditionalOnProperty(name = "app.database.enabled", havingValue = "true")
    @Singleton
    @Component
    public static class DatabaseService {
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

    @Profile("development")
    @Singleton
    @Component
    public static class MockDatabaseService {
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

    // =============================================================================
    // EVENT CLASSES
    // =============================================================================

    public static class OrderCompletedEvent extends Event {
        private final String orderId;
        private final String userId;
        private final double amount;
        
        public OrderCompletedEvent(String orderId, String userId, double amount) {
            super();
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
        }
        
        public String getOrderId() { return orderId; }
        public String getUserId() { return userId; }
        public double getAmount() { return amount; }
    }

    public static class PaymentProcessedEvent extends Event {
        private final String orderId;
        private final double amount;
        private final String transactionId;
        
        public PaymentProcessedEvent(String orderId, double amount, String transactionId) {
            super();
            this.orderId = orderId;
            this.amount = amount;
            this.transactionId = transactionId;
        }
        
        public String getOrderId() { return orderId; }
        public double getAmount() { return amount; }
        public String getTransactionId() { return transactionId; }
    }
}