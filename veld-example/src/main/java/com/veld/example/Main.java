package com.veld.example;

import com.veld.runtime.VeldContainer;

/**
 * Main class demonstrating Veld DI framework capabilities.
 * 
 * This example shows:
 * 1. Constructor injection (UserRepositoryImpl, EmailNotification)
 * 2. Field injection (ConfigService, RequestContext)
 * 3. Method injection (UserService)
 * 4. @Singleton scope (LogService, ConfigService, UserRepositoryImpl, UserService)
 * 5. @Prototype scope (RequestContext, EmailNotification)
 * 6. @PostConstruct and @PreDestroy lifecycle callbacks
 * 7. Interface-based injection (IUserRepository -> UserRepositoryImpl)
 * 8. JSR-330 compatibility (javax.inject.*)
 * 9. Jakarta Inject compatibility (jakarta.inject.*)
 * 
 * Simple API: Just create a new VeldContainer() - that's it!
 * All bytecode generation happens at compile-time using ASM.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           Veld DI Framework - Example Application        ║");
        System.out.println("║        Pure ASM Bytecode Generation - Simple API         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Create the container - simple as that!
        // The registry is automatically discovered and loaded
        VeldContainer container = new VeldContainer();
        
        try {
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("1. SINGLETON DEMONSTRATION");
            System.out.println("══════════════════════════════════════════════════════════");
            demonstrateSingleton(container);
            
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("2. PROTOTYPE DEMONSTRATION");
            System.out.println("══════════════════════════════════════════════════════════");
            demonstratePrototype(container);
            
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("3. DEPENDENCY INJECTION CHAIN");
            System.out.println("══════════════════════════════════════════════════════════");
            demonstrateInjectionChain(container);
            
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("4. INTERFACE-BASED INJECTION");
            System.out.println("══════════════════════════════════════════════════════════");
            demonstrateInterfaceInjection(container);
            
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("5. JSR-330 & JAKARTA INJECT COMPATIBILITY");
            System.out.println("══════════════════════════════════════════════════════════");
            demonstrateJsr330AndJakarta(container);
            
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("6. SERVICE USAGE");
            System.out.println("══════════════════════════════════════════════════════════");
            demonstrateServiceUsage(container);
            
        } finally {
            System.out.println("\n══════════════════════════════════════════════════════════");
            System.out.println("CONTAINER SHUTDOWN - @PreDestroy callbacks");
            System.out.println("══════════════════════════════════════════════════════════");
            container.close();
        }
        
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              Example Completed Successfully!              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Demonstrates singleton behavior - same instance returned each time.
     */
    private static void demonstrateSingleton(VeldContainer container) {
        System.out.println("\n→ Getting LogService twice (should be same instance):");
        LogService log1 = container.get(LogService.class);
        LogService log2 = container.get(LogService.class);
        
        System.out.println("  log1 hashCode: " + System.identityHashCode(log1));
        System.out.println("  log2 hashCode: " + System.identityHashCode(log2));
        System.out.println("  Same instance? " + (log1 == log2 ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Getting ConfigService twice (should be same instance):");
        ConfigService config1 = container.get(ConfigService.class);
        ConfigService config2 = container.get(ConfigService.class);
        
        System.out.println("  config1 hashCode: " + System.identityHashCode(config1));
        System.out.println("  config2 hashCode: " + System.identityHashCode(config2));
        System.out.println("  Same instance? " + (config1 == config2 ? "YES ✓" : "NO ✗"));
    }
    
    /**
     * Demonstrates prototype behavior - new instance created each time.
     */
    private static void demonstratePrototype(VeldContainer container) {
        System.out.println("\n→ Getting RequestContext three times (should be different instances):");
        RequestContext req1 = container.get(RequestContext.class);
        RequestContext req2 = container.get(RequestContext.class);
        RequestContext req3 = container.get(RequestContext.class);
        
        System.out.println("  req1: Instance #" + req1.getInstanceNumber() + ", ID: " + req1.getRequestId());
        System.out.println("  req2: Instance #" + req2.getInstanceNumber() + ", ID: " + req2.getRequestId());
        System.out.println("  req3: Instance #" + req3.getInstanceNumber() + ", ID: " + req3.getRequestId());
        System.out.println("  All different instances? " + 
            (req1 != req2 && req2 != req3 && req1 != req3 ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Getting EmailNotification twice (should be different instances):");
        EmailNotification email1 = container.get(EmailNotification.class);
        EmailNotification email2 = container.get(EmailNotification.class);
        
        System.out.println("  email1: Notification #" + email1.getNotificationNumber());
        System.out.println("  email2: Notification #" + email2.getNotificationNumber());
        System.out.println("  Different instances? " + (email1 != email2 ? "YES ✓" : "NO ✗"));
    }
    
    /**
     * Demonstrates how dependencies are injected through the chain.
     */
    private static void demonstrateInjectionChain(VeldContainer container) {
        System.out.println("\n→ UserService receives dependencies via method injection:");
        UserService userService = container.get(UserService.class);
        
        System.out.println("\n→ ConfigService receives LogService via field injection:");
        ConfigService configService = container.get(ConfigService.class);
        LogService injectedLog = configService.getLogService();
        System.out.println("  ConfigService has LogService? " + (injectedLog != null ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Verifying singleton consistency in injection chain:");
        LogService directLog = container.get(LogService.class);
        System.out.println("  LogService from container == LogService in ConfigService? " + 
            (directLog == injectedLog ? "YES ✓" : "NO ✗"));
    }
    
    /**
     * Demonstrates interface-based injection.
     * IUserRepository is an interface, UserRepositoryImpl is the implementation.
     * Veld automatically resolves the interface to its implementation.
     */
    private static void demonstrateInterfaceInjection(VeldContainer container) {
        System.out.println("\n→ Injecting by INTERFACE (IUserRepository):");
        IUserRepository repoByInterface = container.get(IUserRepository.class);
        System.out.println("  Requested: IUserRepository.class");
        System.out.println("  Received:  " + repoByInterface.getClass().getSimpleName());
        System.out.println("  Is UserRepositoryImpl? " + 
            (repoByInterface instanceof UserRepositoryImpl ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Injecting by CONCRETE CLASS (UserRepositoryImpl):");
        UserRepositoryImpl repoByClass = container.get(UserRepositoryImpl.class);
        System.out.println("  Requested: UserRepositoryImpl.class");
        System.out.println("  Received:  " + repoByClass.getClass().getSimpleName());
        
        System.out.println("\n→ Verifying singleton consistency:");
        System.out.println("  Same instance? " + (repoByInterface == repoByClass ? "YES ✓" : "NO ✗"));
        System.out.println("  Both hashCodes: " + System.identityHashCode(repoByInterface) + 
            " == " + System.identityHashCode(repoByClass));
        
        System.out.println("\n→ UserService injects IUserRepository (interface):");
        System.out.println("  This demonstrates that services can depend on interfaces,");
        System.out.println("  and Veld resolves them to concrete implementations automatically.");
    }
    
    /**
     * Demonstrates JSR-330 (javax.inject) and Jakarta Inject (jakarta.inject) compatibility.
     */
    private static void demonstrateJsr330AndJakarta(VeldContainer container) {
        System.out.println("\n→ PaymentService uses javax.inject.* annotations:");
        System.out.println("  @javax.inject.Singleton for scope");
        System.out.println("  @javax.inject.Inject for constructor and method injection");
        PaymentService paymentService = container.get(PaymentService.class);
        System.out.println("  PaymentService obtained: " + (paymentService != null ? "YES" : "NO"));
        
        System.out.println("\n→ OrderService uses jakarta.inject.* annotations:");
        System.out.println("  @jakarta.inject.Singleton for scope");
        System.out.println("  @jakarta.inject.Inject for constructor and method injection");
        OrderService orderService = container.get(OrderService.class);
        System.out.println("  OrderService obtained: " + (orderService != null ? "YES" : "NO"));
        
        System.out.println("\n→ NotificationService uses MIXED annotations:");
        System.out.println("  @com.veld.annotation.Singleton for scope");
        System.out.println("  @javax.inject.Inject for constructor");
        System.out.println("  @jakarta.inject.Inject for method");
        System.out.println("  @com.veld.annotation.Inject for field");
        NotificationService notificationService = container.get(NotificationService.class);
        System.out.println("  NotificationService obtained: " + (notificationService != null ? "YES" : "NO"));
        
        System.out.println("\n→ Testing PaymentService functionality:");
        boolean valid = paymentService.validatePayment(500.0);
        System.out.println("  Payment of $500 valid? " + (valid ? "YES" : "NO"));
        paymentService.processPayment("ORD-001", 500.0);
        
        System.out.println("\n→ Testing OrderService with Jakarta annotations:");
        String orderId = orderService.createOrder(1L, "Veld Framework", 99.99);
        System.out.println("  Order created: " + orderId);
        
        System.out.println("\n→ Testing NotificationService with mixed annotations:");
        notificationService.sendWelcomeNotification("new-user@example.com");
        
        System.out.println("\n→ Verifying all services are singletons:");
        PaymentService payment2 = container.get(PaymentService.class);
        OrderService order2 = container.get(OrderService.class);
        NotificationService notif2 = container.get(NotificationService.class);
        
        System.out.println("  PaymentService singleton? " + (paymentService == payment2 ? "YES" : "NO"));
        System.out.println("  OrderService singleton? " + (orderService == order2 ? "YES" : "NO"));
        System.out.println("  NotificationService singleton? " + (notificationService == notif2 ? "YES" : "NO"));
    }
    
    /**
     * Demonstrates actual usage of the services.
     */
    private static void demonstrateServiceUsage(VeldContainer container) {
        // Get services
        UserService userService = container.get(UserService.class);
        
        System.out.println("\n→ Listing existing users:");
        userService.listAllUsers();
        
        System.out.println("\n→ Looking up user by ID:");
        String user = userService.getUserName(1L);
        System.out.println("  User with ID 1: " + user);
        
        System.out.println("\n→ Creating new user:");
        userService.createUser(4L, "Diana");
        
        System.out.println("\n→ Listing users after creation:");
        userService.listAllUsers();
        
        System.out.println("\n→ Sending email notification (prototype):");
        EmailNotification notification = container.get(EmailNotification.class);
        notification
            .to("user@example.com")
            .withSubject("Welcome to Veld!")
            .withBody("Thank you for trying Veld DI Framework.")
            .send();
        
        System.out.println("\n→ Processing requests (prototype instances):");
        RequestContext request1 = container.get(RequestContext.class);
        request1.process("GET /users");
        
        RequestContext request2 = container.get(RequestContext.class);
        request2.process("POST /users");
    }
}
