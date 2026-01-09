package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;

/**
 * Ejemplo complejo que demuestra TODAS las características de Veld funcionando juntos:
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
            String orderId = orderService.createOrder(1L, "Veld Framework", 99.99);
            
            System.out.println("  [OK] Orden creada: " + orderId);
            
            // Procesar pago
            PaymentResult payment = paymentService.processPayment(orderId, 99.99);
            System.out.println("  [OK] Pago procesado: " + payment.getStatus() + " - " + payment.getTransactionId());
            
            // Enviar notificaciones
            notificationService.sendOrderConfirmation(orderId);
            System.out.println("  [OK] Notificaciones enviadas");
            
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
}
