package com.veld.example;

import com.veld.runtime.VeldContainer;

/**
 * Main class demonstrating Veld DI framework capabilities.
 * 
 * This example shows:
 * 1. Constructor injection (UserRepository, EmailNotification)
 * 2. Field injection (ConfigService, RequestContext)
 * 3. Method injection (UserService)
 * 4. @Singleton scope (LogService, ConfigService, UserRepository, UserService)
 * 5. @Prototype scope (RequestContext, EmailNotification)
 * 6. @PostConstruct and @PreDestroy lifecycle callbacks
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
            System.out.println("4. SERVICE USAGE");
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
