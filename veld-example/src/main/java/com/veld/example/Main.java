package com.veld.example;

import com.veld.example.aop.CalculatorService;
import com.veld.example.aop.LoggingAspect;
import com.veld.example.aop.PerformanceAspect;
import com.veld.example.aop.ProductService;
import com.veld.example.events.NotificationEvent;
import com.veld.example.events.OrderCreatedEvent;
import com.veld.example.lifecycle.*;
import com.veld.aop.InterceptorRegistry;
import com.veld.aop.interceptor.LoggingInterceptor;
import com.veld.aop.interceptor.TimingInterceptor;
import com.veld.aop.interceptor.TransactionInterceptor;
import com.veld.aop.interceptor.ValidationInterceptor;
import com.veld.aop.proxy.ProxyFactory;
import com.veld.runtime.event.EventBus;
import com.veld.runtime.lifecycle.LifecycleProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;

/**
 * Main class demonstrating Veld DI framework capabilities.
 * 
 * This example shows:
 * 1. Static access via generated Veld class (Veld.userService(), Veld.get(Class))
 * 2. Constructor injection
 * 3. Field injection
 * 4. Method injection
 * 5. @Singleton scope
 * 6. @Prototype scope
 * 7. @PostConstruct and @PreDestroy lifecycle callbacks
 * 8. Interface-based injection
 * 9. JSR-330 compatibility (javax.inject.*)
 * 10. Jakarta Inject compatibility (jakarta.inject.*)
 * 11. @Lazy initialization
 * 12. @Conditional annotations
 * 13. @Profile annotations
 * 14. @Value configuration injection
 * 15. EventBus - Event-driven communication
 * 16. AOP (Aspect-Oriented Programming)
 * 17. Advanced Lifecycle Management
 * 
 * Simple API: Just use Veld.get(Class) or Veld.serviceName()!
 * All bytecode generation happens at compile-time using ASM.
 */
public class Main {
    
    // Dynamic access to generated Veld class
    private static Class<?> veldClass;
    private static MethodHandle getByClassHandle;
    private static MethodHandle containsHandle;
    
    static {
        try {
            veldClass = Class.forName("com.veld.generated.Veld");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            getByClassHandle = lookup.findStatic(veldClass, "get", 
                MethodType.methodType(Object.class, Class.class));
            containsHandle = lookup.findStatic(veldClass, "contains",
                MethodType.methodType(boolean.class, Class.class));
        } catch (Exception e) {
            System.err.println("Warning: Veld class not found. Run annotation processor first.");
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T get(Class<T> type) {
        try {
            return (T) getByClassHandle.invoke(type);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get " + type.getName(), e);
        }
    }
    
    private static boolean contains(Class<?> type) {
        try {
            return (boolean) containsHandle.invoke(type);
        } catch (Throwable e) {
            return false;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           Veld DI Framework - Example Application        ║");
        System.out.println("║        Pure ASM Bytecode Generation - Simple API         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        if (veldClass == null) {
            System.err.println("ERROR: Veld generated class not found!");
            System.err.println("Make sure to run the annotation processor first.");
            return;
        }
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("1. SINGLETON DEMONSTRATION");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateSingleton();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("2. PROTOTYPE DEMONSTRATION");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstratePrototype();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("3. DEPENDENCY INJECTION CHAIN");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateInjectionChain();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("4. INTERFACE-BASED INJECTION");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateInterfaceInjection();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("5. JSR-330 & JAKARTA INJECT COMPATIBILITY");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateJsr330AndJakarta();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("6. @LAZY INITIALIZATION");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateLazy();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("7. @CONDITIONAL ANNOTATIONS");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateConditional();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("8. @VALUE CONFIGURATION INJECTION");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateValueInjection();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("9. EVENTBUS - EVENT-DRIVEN COMMUNICATION");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateEventBus();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("10. AOP (ASPECT-ORIENTED PROGRAMMING)");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateAop();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("11. ADVANCED LIFECYCLE MANAGEMENT");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateAdvancedLifecycle();
        
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("12. SERVICE USAGE");
        System.out.println("══════════════════════════════════════════════════════════");
        demonstrateServiceUsage();
        
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              Example Completed Successfully!              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
    
    private static void demonstrateSingleton() {
        System.out.println("\n→ Getting LogService twice (should be same instance):");
        LogService log1 = get(LogService.class);
        LogService log2 = get(LogService.class);
        
        System.out.println("  log1 hashCode: " + System.identityHashCode(log1));
        System.out.println("  log2 hashCode: " + System.identityHashCode(log2));
        System.out.println("  Same instance? " + (log1 == log2 ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Getting ConfigService twice (should be same instance):");
        ConfigService config1 = get(ConfigService.class);
        ConfigService config2 = get(ConfigService.class);
        
        System.out.println("  config1 hashCode: " + System.identityHashCode(config1));
        System.out.println("  config2 hashCode: " + System.identityHashCode(config2));
        System.out.println("  Same instance? " + (config1 == config2 ? "YES ✓" : "NO ✗"));
    }
    
    private static void demonstratePrototype() {
        System.out.println("\n→ Getting RequestContext three times (should be different instances):");
        RequestContext req1 = get(RequestContext.class);
        RequestContext req2 = get(RequestContext.class);
        RequestContext req3 = get(RequestContext.class);
        System.out.println("  req1: Instance #" + req1.getInstanceNumber() + ", ID: " + req1.getRequestId());
        System.out.println("  req2: Instance #" + req2.getInstanceNumber() + ", ID: " + req2.getRequestId());
        System.out.println("  req3: Instance #" + req3.getInstanceNumber() + ", ID: " + req3.getRequestId());
        System.out.println("  All different instances? " + 
            (req1 != req2 && req2 != req3 && req1 != req3 ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Getting EmailNotification twice (should be different instances):");
        EmailNotification email1 = get(EmailNotification.class);
        EmailNotification email2 = get(EmailNotification.class);
        
        System.out.println("  email1: Notification #" + email1.getNotificationNumber());
        System.out.println("  email2: Notification #" + email2.getNotificationNumber());
        System.out.println("  Different instances? " + (email1 != email2 ? "YES ✓" : "NO ✗"));
    }
    
    private static void demonstrateInjectionChain() {
        System.out.println("\n→ UserService receives dependencies via method injection:");
        UserService userService = get(UserService.class);
        
        System.out.println("\n→ ConfigService receives LogService via field injection:");
        ConfigService configService = get(ConfigService.class);
        LogService injectedLog = configService.getLogService();
        System.out.println("  ConfigService has LogService? " + (injectedLog != null ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Verifying singleton consistency in injection chain:");
        LogService directLog = get(LogService.class);
        System.out.println("  LogService from Veld == LogService in ConfigService? " + 
            (directLog == injectedLog ? "YES ✓" : "NO ✗"));
    }
    
    private static void demonstrateInterfaceInjection() {
        System.out.println("\n→ Injecting by INTERFACE (IUserRepository):");
        IUserRepository repoByInterface = get(IUserRepository.class);
        System.out.println("  Requested: IUserRepository.class");
        System.out.println("  Received:  " + repoByInterface.getClass().getSimpleName());
        System.out.println("  Is UserRepositoryImpl? " + 
            (repoByInterface instanceof UserRepositoryImpl ? "YES ✓" : "NO ✗"));
        
        System.out.println("\n→ Injecting by CONCRETE CLASS (UserRepositoryImpl):");
        UserRepositoryImpl repoByClass = get(UserRepositoryImpl.class);
        System.out.println("  Requested: UserRepositoryImpl.class");
        System.out.println("  Received:  " + repoByClass.getClass().getSimpleName());
        
        System.out.println("\n→ Verifying singleton consistency:");
        System.out.println("  Same instance? " + (repoByInterface == repoByClass ? "YES ✓" : "NO ✗"));
    }
    
    private static void demonstrateJsr330AndJakarta() {
        System.out.println("\n→ PaymentService uses javax.inject.* annotations:");
        PaymentService paymentService = get(PaymentService.class);
        System.out.println("  PaymentService obtained: " + (paymentService != null ? "YES" : "NO"));
        
        System.out.println("\n→ OrderService uses jakarta.inject.* annotations:");
        OrderService orderService = get(OrderService.class);
        System.out.println("  OrderService obtained: " + (orderService != null ? "YES" : "NO"));
        
        System.out.println("\n→ NotificationService uses MIXED annotations:");
        NotificationService notificationService = get(NotificationService.class);
        System.out.println("  NotificationService obtained: " + (notificationService != null ? "YES" : "NO"));
        
        System.out.println("\n→ Testing PaymentService functionality:");
        boolean valid = paymentService.validatePayment(500.0);
        System.out.println("  Payment of $500 valid? " + (valid ? "YES" : "NO"));
        paymentService.processPayment("ORD-001", 500.0);
        
        System.out.println("\n→ Testing OrderService with Jakarta annotations:");
        String orderId = orderService.createOrder(1L, "Veld Framework", 99.99);
        System.out.println("  Order created: " + orderId);
    }
    
    private static void demonstrateLazy() {
        ExpensiveService.resetInstanceCount();
        
        System.out.println("\n→ ExpensiveService is marked with @Lazy");
        System.out.println("  Current instance count: " + ExpensiveService.getInstanceCount());
        
        System.out.println("\n→ Now requesting ExpensiveService for the first time...");
        ExpensiveService expensive1 = get(ExpensiveService.class);
        System.out.println("  Instance count after first request: " + ExpensiveService.getInstanceCount());
        
        System.out.println("\n→ Requesting ExpensiveService again (should be same singleton)...");
        ExpensiveService expensive2 = get(ExpensiveService.class);
        System.out.println("  Instance count after second request: " + ExpensiveService.getInstanceCount());
        System.out.println("  Same instance? " + (expensive1 == expensive2 ? "YES ✓" : "NO ✗"));
    }
    
    private static void demonstrateConditional() {
        System.out.println("\n→ Conditional Registration Demo:");
        
        System.out.println("\n→ @ConditionalOnMissingBean Demo:");
        boolean hasDbService = contains(DatabaseService.class);
        System.out.println("  DatabaseService available? " + (hasDbService ? "YES" : "NO"));
        if (hasDbService) {
            DatabaseService db = get(DatabaseService.class);
            System.out.println("  Using: " + db.getClass().getSimpleName());
            System.out.println("  Connection info: " + db.getConnectionInfo());
        }
        
        System.out.println("\n→ @ConditionalOnProperty Demo:");
        String debugProp = System.getProperty("app.debug", System.getenv("APP_DEBUG"));
        System.out.println("  Current app.debug value: " + (debugProp != null ? debugProp : "<not set>"));
        boolean hasDebug = contains(DebugService.class);
        System.out.println("  DebugService available? " + (hasDebug ? "YES" : "NO"));
        
        System.out.println("\n→ ConditionalDemoService integrates with conditional beans:");
        ConditionalDemoService conditionalDemo = get(ConditionalDemoService.class);
        conditionalDemo.runDemo();
    }
    
    private static void demonstrateValueInjection() {
        System.out.println("\n→ @Value Configuration Injection Demo:");
        
        System.out.println("\n→ AppConfigService uses @Value for all configuration:");
        AppConfigService appConfig = get(AppConfigService.class);
        
        System.out.println("\n→ Configuration Values Retrieved:");
        System.out.println("  App Name: " + appConfig.getAppName());
        System.out.println("  Version: " + appConfig.getAppVersion());
        System.out.println("  Environment: " + appConfig.getEnvironment());
        System.out.println("  Server Port: " + appConfig.getServerPort());
        System.out.println("  Debug Mode: " + appConfig.isDebugMode());
    }
    
    private static void demonstrateEventBus() {
        System.out.println("\n→ EventBus - Decoupled component communication\n");
        
        EventBus eventBus = EventBus.getInstance();
        
        System.out.println("→ Registering event handlers:");
        OrderEventHandler orderHandler = get(OrderEventHandler.class);
        NotificationHandler notificationHandler = get(NotificationHandler.class);
        
        eventBus.register(orderHandler);
        eventBus.register(notificationHandler);
        
        EventDemoService eventService = get(EventDemoService.class);
        
        System.out.println("\n→ Publishing OrderCreatedEvent:");
        eventService.createOrder(
            299.99,
            "customer@example.com",
            Arrays.asList("Veld Framework License", "Premium Support"),
            "123 Main St, Tech City"
        );
        
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        
        System.out.println("\n→ EventBus Statistics:");
        System.out.println(eventBus.getStatistics());
    }
    
    private static void demonstrateAop() {
        System.out.println("\n→ AOP - Aspect-Oriented Programming Demo");
        
        InterceptorRegistry registry = InterceptorRegistry.getInstance();
        
        LoggingAspect loggingAspect = new LoggingAspect();
        PerformanceAspect performanceAspect = new PerformanceAspect();
        registry.registerAspect(loggingAspect);
        registry.registerAspect(performanceAspect);
        
        registry.registerInterceptor(new LoggingInterceptor());
        registry.registerInterceptor(new TimingInterceptor());
        registry.registerInterceptor(new ValidationInterceptor());
        registry.registerInterceptor(new TransactionInterceptor());
        
        ProxyFactory proxyFactory = ProxyFactory.getInstance();
        
        CalculatorService calculator = proxyFactory.createProxy(CalculatorService.class);
        
        System.out.println("\n→ Testing CalculatorService:");
        System.out.println("  add(5, 3) = " + calculator.add(5, 3));
        System.out.println("  multiply(7, 6) = " + calculator.multiply(7, 6));
        
        PerformanceAspect.clearStatistics();
        TimingInterceptor.clearStatistics();
        registry.clear();
    }
    
    private static void demonstrateAdvancedLifecycle() {
        System.out.println("\n→ Advanced Lifecycle Management Demo");
        
        LifecycleProcessor lifecycleProcessor = new LifecycleProcessor();
        EventBus eventBus = EventBus.getInstance();
        lifecycleProcessor.setEventBus(eventBus);
        
        LoggingBeanPostProcessor postProcessor = new LoggingBeanPostProcessor();
        lifecycleProcessor.addBeanPostProcessor(postProcessor);
        
        DatabaseConnection dbConnection = new DatabaseConnection();
        lifecycleProcessor.registerBean("databaseConnection", dbConnection);
        
        MetricsService metricsService = new MetricsService();
        lifecycleProcessor.registerBean("metricsService", metricsService);
        
        lifecycleProcessor.onRefresh(150);
        lifecycleProcessor.start();
        
        System.out.println("  LifecycleProcessor running: " + lifecycleProcessor.isRunning());
        
        lifecycleProcessor.stop();
        lifecycleProcessor.destroy();
    }
    
    private static void demonstrateServiceUsage() {
        UserService userService = get(UserService.class);
        
        System.out.println("\n→ Listing existing users:");
        userService.listAllUsers();
        
        System.out.println("\n→ Looking up user by ID:");
        String user = userService.getUserName(1L);
        System.out.println("  User with ID 1: " + user);
        
        System.out.println("\n→ Creating new user:");
        userService.createUser(4L, "Diana");
        
        System.out.println("\n→ Sending email notification:");
        EmailNotification notification = get(EmailNotification.class);
        notification
            .to("user@example.com")
            .withSubject("Welcome to Veld!")
            .withBody("Thank you for trying Veld DI Framework.")
            .send();
    }
}
