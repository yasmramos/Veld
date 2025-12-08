# Veld DI Framework

[![CI/CD](https://github.com/yasmramos/Veld/actions/workflows/ci.yml/badge.svg)](https://github.com/yasmramos/Veld/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-11--25-orange)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/yasmramos/Veld)](https://github.com/yasmramos/Veld/releases)

A lightweight, compile-time Dependency Injection framework for Java using pure ASM bytecode generation. **Zero reflection, zero runtime overhead.**

## Features

### Core DI
- **Pure Bytecode Generation**: Uses ASM to generate optimized factory classes at compile time
- **Zero Reflection**: All dependency resolution happens via generated code, not reflection
- **Private Field Injection**: Inject into `private`, `private static`, `private final`, and `private static final` fields without reflection
- **@Value Injection**: Inject configuration values from system properties, environment variables, and config files
- **JSR-330 Compatible**: Full support for `javax.inject.*` annotations
- **Jakarta Inject Compatible**: Full support for `jakarta.inject.*` annotations
- **Mixed Annotations**: Use Veld, JSR-330, and Jakarta annotations together seamlessly
- **Scope Management**: Built-in Singleton and Prototype scopes
- **Lazy Initialization**: `@Lazy` for deferred component creation
- **Provider Injection**: `Provider<T>` for on-demand instance creation
- **Optional Injection**: `@Optional` and `Optional<T>` for graceful handling of missing dependencies
- **Conditional Registration**: `@ConditionalOnProperty`, `@ConditionalOnClass`, `@ConditionalOnMissingBean`
- **Interface-Based Injection**: Inject by interface, resolved to concrete implementations
- **Lifecycle Callbacks**: `@PostConstruct` and `@PreDestroy` support
- **Circular Dependency Detection**: Compile-time detection with clear error messages
- **Lightweight**: Minimal runtime footprint

### Java Platform Module System (JPMS)
- **Full JPMS Support**: All modules include proper `module-info.java` descriptors
- **Strong Encapsulation**: Clean module boundaries with explicit exports
- **Java 11-25 Compatibility**: Tested and verified across all LTS versions

### EventBus
- **Publish/Subscribe**: Lightweight event-driven architecture
- **Synchronous & Asynchronous**: Choose between sync and async event delivery
- **Priority Ordering**: Control handler execution order
- **Type Hierarchy**: Handlers receive events of declared type and subtypes
- **Weak References**: Automatic cleanup of unsubscribed handlers

### AOP (Aspect-Oriented Programming)
- **ASM Proxy Generation**: Dynamic proxy creation using bytecode manipulation
- **AspectJ-like Syntax**: Familiar pointcut expression language
- **Multiple Advice Types**: `@Around`, `@Before`, `@After` (RETURNING, THROWING, FINALLY)
- **CDI Interceptors**: `@Interceptor`, `@AroundInvoke`, `@InterceptorBinding`
- **Built-in Interceptors**: Logging, Timing, Validation, Transactions

### Advanced Lifecycle Management
- **Lifecycle Interfaces**: `Lifecycle`, `SmartLifecycle` with phase ordering
- **Bean Callbacks**: `InitializingBean`, `DisposableBean`
- **Post-Processing**: `BeanPostProcessor` for custom bean modification
- **Lifecycle Annotations**: `@PostInitialize`, `@OnStart`, `@OnStop`, `@DependsOn`
- **Lifecycle Events**: `ContextRefreshedEvent`, `ContextStartedEvent`, `ContextStoppedEvent`, `ContextClosedEvent`

## Requirements

- **Java**: 11, 17, 21, or 25 (LTS versions recommended)
- **Maven**: 3.6+

## Quick Start

### 1. Add Dependencies

```xml
<dependency>
    <groupId>com.veld</groupId>
    <artifactId>veld-annotations</artifactId>
    <version>1.0.0-alpha.6</version>
</dependency>
<dependency>
    <groupId>com.veld</groupId>
    <artifactId>veld-runtime</artifactId>
    <version>1.0.0-alpha.6</version>
</dependency>
<dependency>
    <groupId>com.veld</groupId>
    <artifactId>veld-aop</artifactId>
    <version>1.0.0-alpha.6</version>
</dependency>
<dependency>
    <groupId>com.veld</groupId>
    <artifactId>veld-processor</artifactId>
    <version>1.0.0-alpha.6</version>
    <scope>provided</scope>
</dependency>
```

### 2. Configure Annotation Processor

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>11</source>
        <target>11</target>
        <annotationProcessorPaths>
            <path>
                <groupId>com.veld</groupId>
                <artifactId>veld-processor</artifactId>
                <version>1.0.0-alpha.6</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3. Define Components

```java
import com.veld.annotation.Inject;
import com.veld.annotation.Singleton;

@Singleton
public class UserService {
    @Inject
    UserRepository userRepository;

    public User findUser(Long id) {
        return userRepository.findById(id);
    }
}
```

### 4. Use the Container

```java
import com.veld.runtime.VeldContainer;

public class Main {
    public static void main(String[] args) {
        VeldContainer container = new VeldContainer();
        try {
            UserService userService = container.get(UserService.class);
            User user = userService.findUser(1L);
        } finally {
            container.close();
        }
    }
}
```

## JPMS Module Configuration

Veld is fully modularized with JPMS. Here are the module names:

| Artifact | Module Name |
|----------|-------------|
| veld-annotations | `com.veld.annotation` |
| veld-runtime | `com.veld.runtime` |
| veld-aop | `com.veld.aop` |
| veld-processor | `com.veld.processor` |
| veld-weaver | `com.veld.weaver` |
| veld-benchmark | `com.veld.benchmark` |

### Example module-info.java

```java
module com.myapp {
    requires com.veld.annotation;
    requires com.veld.runtime;
    requires com.veld.aop;
    
    // Open packages for dependency injection
    opens com.myapp.services to com.veld.runtime;
    opens com.myapp.repositories to com.veld.runtime;
}
```

## Annotation Support

Veld supports annotations from three sources, which can be mixed freely:

| Feature | Veld | JSR-330 (javax) | Jakarta |
|---------|------|-----------------|-----------|
| Injection | `@Inject` | `@Inject` | `@Inject` |
| Singleton | `@Singleton` | `@Singleton` | `@Singleton` |
| Prototype | `@Prototype` | - | - |
| Lazy | `@Lazy` | - | - |
| Optional | `@Optional` | - | - |
| Conditional | `@ConditionalOnProperty`, `@ConditionalOnClass`, `@ConditionalOnMissingBean` | - | - |
| Qualifier | `@Named` | `@Named` | `@Named` |
| Provider | `Provider<T>` | `Provider<T>` | `Provider<T>` |
| Post-construct | `@PostConstruct` | - | - |
| Pre-destroy | `@PreDestroy` | - | - |
| Value | `@Value` | - | - |

> **Note**: `@Singleton`, `@Prototype`, and `@Lazy` automatically imply `@Component`, so you don't need to add both.

### Example: Mixed Annotations

```java
@Singleton // Veld annotation (implies @Component)
public class PaymentService {
    @javax.inject.Inject
    private LogService logService;

    @jakarta.inject.Inject
    public void setConfig(ConfigService config) {
        this.config = config;
    }
}
```

## Injection Types

### Constructor Injection

```java
@Singleton
public class OrderService {
    private final PaymentService paymentService;

    @Inject
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

### Field Injection

```java
@Singleton
public class UserService {
    @Inject
    UserRepository userRepository;
}
```

### Private Field Injection

Veld supports injection into **private fields** without using reflection, thanks to bytecode weaving:

```java
@Singleton
public class SecureService {
    @Inject
    private AuthService authService;        // private field
    
    @Inject
    private static Logger logger;           // private static field
    
    @Inject
    private final ConfigService config;     // private final field
    
    @Inject
    private static final Validator validator; // private static final
}
```

The `veld-weaver` Maven plugin generates synthetic setter methods at compile time, enabling injection without reflection:

```xml
<plugin>
    <groupId>com.veld</groupId>
    <artifactId>veld-weaver</artifactId>
    <version>1.0.0-alpha.6</version>
    <executions>
        <execution>
            <goals>
                <goal>weave</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**How it works:**
- The weaver scans compiled classes for `@Inject` or `@Value` annotated private fields
- Generates synthetic setter methods: `__di_set_<fieldName>(FieldType value)`
- For `final` fields, the modifier is removed to allow injection
- The generated factory uses these setters instead of reflection

### Method Injection

```java
@Singleton
public class NotificationService {
    private EmailService emailService;

    @Inject
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}
```

## @Value Injection

Inject configuration values from system properties, environment variables, or configuration files:

### Basic Usage

```java
@Singleton
public class AppConfig {
    @Value("${app.name}")
    private String appName;
    
    @Value("${server.port}")
    private int port;
    
    @Value("${feature.enabled}")
    private boolean featureEnabled;
}
```

### Default Values

```java
@Singleton
public class ServerConfig {
    @Value("${app.name:MyApplication}")
    private String appName;  // Uses "MyApplication" if not configured
    
    @Value("${server.port:8080}")
    private int port;  // Uses 8080 if not configured
    
    @Value("${debug.enabled:false}")
    private boolean debug;  // Uses false if not configured
}
```

### Environment Variables

```java
@Singleton
public class DatabaseConfig {
    @Value("${DATABASE_URL}")
    private String dbUrl;
    
    @Value("${DB_PASSWORD:secret}")
    private String password;
}
```

### Constructor Injection with @Value

```java
@Singleton
public class DatabaseService {
    private final String url;
    private final int maxConnections;
    
    @Inject
    public DatabaseService(
            @Value("${db.url}") String url,
            @Value("${db.pool.size:10}") int maxConnections) {
        this.url = url;
        this.maxConnections = maxConnections;
    }
}
```

### Supported Types for @Value

| Type | Example |
|------|--------|
| String | `@Value("${app.name}")` |
| int / Integer | `@Value("${port:8080}")` |
| long / Long | `@Value("${timeout:5000}")` |
| double / Double | `@Value("${rate:0.5}")` |
| float / Float | `@Value("${factor:1.0}")` |
| boolean / Boolean | `@Value("${enabled:true}")` |
| byte / Byte | `@Value("${level:1}")` |
| short / Short | `@Value("${count:100}")` |
| char / Character | `@Value("${separator:,}")` |

### Value Resolution Order

Values are resolved in the following order (first match wins):

1. **System Properties**: `-Dproperty=value`
2. **Environment Variables**: `export PROPERTY=value`
3. **Configuration Files**: `application.properties`

## Interface-Based Injection

```java
public interface IUserRepository {
    User findById(Long id);
}

@Singleton
public class UserRepositoryImpl implements IUserRepository {
    @Override
    public User findById(Long id) { /* ... */ }
}

@Singleton
public class UserService {
    @Inject
    IUserRepository userRepository; // Injects UserRepositoryImpl
}
```

## Scopes

### Singleton (Default)

```java
@Singleton
public class ConfigService {
    // One instance shared across the container
}
```

### Prototype

```java
@Prototype
public class RequestContext {
    // New instance created for each injection
}
```

## Lazy Initialization

Components marked with `@Lazy` are not instantiated until first accessed:

```java
@Singleton
@Lazy
public class ExpensiveService {
    public ExpensiveService() {
        // Heavy initialization - only happens when first requested
        loadLargeDataset();
    }
}
```

## Provider Injection

Use `Provider<T>` for on-demand instance creation, especially useful with `@Prototype` components:

```java
@Singleton
public class ReportGenerator {
    @Inject
    Provider<RequestContext> contextProvider;

    public void generateReports() {
        // Each call creates a new RequestContext
        RequestContext ctx1 = contextProvider.get();
        RequestContext ctx2 = contextProvider.get();
        // ctx1 != ctx2
    }
}
```

Veld supports all three Provider types:
- `com.veld.runtime.Provider<T>`
- `javax.inject.Provider<T>`
- `jakarta.inject.Provider<T>`

## Optional Injection

Handle missing dependencies gracefully without throwing exceptions:

### Using @Optional Annotation

```java
@Singleton
public class MyService {
    @Inject
    @Optional
    CacheService cache; // Will be null if CacheService is not registered

    public void doWork() {
        if (cache != null) {
            cache.put("key", "value");
        }
    }
}
```

### Using Optional<T> Wrapper

```java
@Singleton
public class MyService {
    @Inject
    java.util.Optional<MetricsService> metrics; // Will be Optional.empty() if not found

    public void doWork() {
        metrics.ifPresent(m -> m.recordEvent("work.done"));
    }
}
```

### Container Methods

```java
VeldContainer container = new VeldContainer();

// Returns null if not found
CacheService cache = container.tryGet(CacheService.class);

// Returns Optional.empty() if not found
Optional<MetricsService> metrics = container.getOptional(MetricsService.class);
```

## Conditional Configuration

Veld supports conditional component registration, similar to Spring Boot's auto-configuration.

### @ConditionalOnProperty

Register a component only when a system property or environment variable matches:

```java
@Singleton
@ConditionalOnProperty(name = "app.debug", havingValue = "true")
public class DebugService {
    // Only registered when -Dapp.debug=true or APP_DEBUG=true
}

@Singleton
@ConditionalOnProperty(name = "feature.x.enabled", matchIfMissing = true)
public class FeatureXService {
    // Registered by default, unless feature.x.enabled=false
}
```

### @ConditionalOnClass

Register a component only when specific classes are available in the classpath:

```java
@Singleton
@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonJsonService {
    // Only registered if Jackson is on the classpath
}
```

### @ConditionalOnMissingBean

Register a component only when specific beans are NOT already registered:

```java
@Singleton
@ConditionalOnMissingBean(DatabaseService.class)
public class DefaultDatabaseService implements DatabaseService {
    // Only registered if no other DatabaseService exists
}
```

## Lifecycle Callbacks

```java
@Singleton
public class DatabaseService {
    @PostConstruct
    public void init() {
        // Called after dependency injection
        connection = createConnection();
    }

    @PreDestroy
    public void cleanup() {
        // Called when container closes
        connection.close();
    }
}
```

## EventBus

Veld includes a lightweight publish/subscribe event system:

### Publishing Events

```java
import com.veld.runtime.event.EventBus;

EventBus eventBus = new EventBus();

// Synchronous publishing
eventBus.post(new UserCreatedEvent(user));

// Asynchronous publishing
eventBus.postAsync(new EmailNotificationEvent(email));
```

### Subscribing to Events

```java
import com.veld.annotation.Subscribe;

public class UserEventHandler {
    @Subscribe
    public void onUserCreated(UserCreatedEvent event) {
        System.out.println("User created: " + event.getUser().getName());
    }

    @Subscribe(priority = 10) // Higher priority = earlier execution
    public void onHighPriorityEvent(OrderEvent event) {
        // Executed before lower priority handlers
    }
}

// Register handler
eventBus.register(new UserEventHandler());
```

### Event Hierarchy

Handlers receive events of the declared type AND all subtypes:

```java
public class BaseEvent { }
public class UserEvent extends BaseEvent { }
public class AdminEvent extends UserEvent { }

public class EventHandler {
    @Subscribe
    public void onBaseEvent(BaseEvent event) {
        // Receives BaseEvent, UserEvent, and AdminEvent
    }

    @Subscribe
    public void onUserEvent(UserEvent event) {
        // Receives UserEvent and AdminEvent only
    }
}
```

### Dead Events

Undelivered events are wrapped in `DeadEvent`:

```java
@Subscribe
public void onDeadEvent(DeadEvent event) {
    System.out.println("Unhandled event: " + event.getOriginalEvent());
}
```

## Advanced Lifecycle Management

Veld provides comprehensive lifecycle management for your beans:

### Lifecycle Interfaces

```java
import com.veld.runtime.lifecycle.*;

// SmartLifecycle with phase ordering
@Singleton
public class DatabaseConnection implements SmartLifecycle {
    private boolean running = false;

    @Override
    public void start() {
        // Connect to database
        running = true;
    }

    @Override
    public void stop() {
        // Close connections
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Lower values start first, stop last
        return -1000;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
```

### InitializingBean & DisposableBean

```java
@Singleton
public class MetricsService implements InitializingBean, DisposableBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        // Called after all dependencies are injected
        initializeMetrics();
    }

    @Override
    public void destroy() throws Exception {
        // Called when container is closing
        flushMetrics();
    }
}
```

### Lifecycle Annotations

```java
@Singleton
@DependsOn("databaseConnection")
public class CacheService {

    @PostInitialize
    public void warmCache() {
        // Called after ALL beans are initialized
    }

    @OnStart
    public void startCacheRefresh() {
        // Called when context starts
    }

    @OnStop
    public void stopCacheRefresh() {
        // Called when context stops
    }
}
```

### BeanPostProcessor

```java
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Before init: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("After init: " + beanName);
        // Can return a proxy wrapper here
        return bean;
    }
}
```

### Lifecycle Events

```java
@Singleton
public class LifecycleMonitor {
    @Subscribe
    public void onRefreshed(ContextRefreshedEvent event) {
        System.out.println("Container initialized with " + event.getBeanCount() + " beans");
    }

    @Subscribe
    public void onStarted(ContextStartedEvent event) {
        System.out.println("Application started");
    }

    @Subscribe
    public void onStopped(ContextStoppedEvent event) {
        System.out.println("Application stopped");
    }

    @Subscribe
    public void onClosed(ContextClosedEvent event) {
        System.out.println("Container closed after " + event.getUptime());
    }
}
```

### Lifecycle Execution Order

```
1. Bean Construction
2. Dependency Injection
3. BeanPostProcessor.postProcessBeforeInitialization()
4. InitializingBean.afterPropertiesSet()
5. @PostConstruct methods
6. BeanPostProcessor.postProcessAfterInitialization()
7. @PostInitialize methods (after ALL beans ready)
8. SmartLifecycle.start() (by phase order)
9. @OnStart methods
   ↓ [Application Running]
10. @OnStop methods
11. SmartLifecycle.stop() (reverse phase order)
12. @PreDestroy methods
13. DisposableBean.destroy()
```

## AOP (Aspect-Oriented Programming)

Veld provides complete AOP support with ASM-based proxy generation:

### Defining Aspects

```java
import com.veld.annotation.*;
import com.veld.aop.*;

@Aspect
public class LoggingAspect {

    @Around("execution(* com.example.service.*.*(..))")
    public Object logMethod(InvocationContext ctx) throws Exception {
        System.out.println("Entering: " + ctx.getMethod().getName());
        try {
            Object result = ctx.proceed();
            System.out.println("Exiting: " + ctx.getMethod().getName());
            return result;
        } catch (Exception e) {
            System.out.println("Exception in: " + ctx.getMethod().getName());
            throw e;
        }
    }

    @Before("execution(public * com.example..*.*(..))")
    public void beforeMethod(JoinPoint jp) {
        System.out.println("Before: " + jp.getSignature());
    }

    @After(value = "execution(* *.*(..))", type = AfterType.RETURNING)
    public void afterReturning(JoinPoint jp, Object result) {
        System.out.println("Returned: " + result);
    }

    @After(value = "execution(* *.*(..))", type = AfterType.THROWING)
    public void afterThrowing(JoinPoint jp, Throwable error) {
        System.out.println("Exception: " + error.getMessage());
    }
}
```

### Pointcut Expressions

```java
// Match all methods in a package
@Around("execution(* com.example.service.*.*(..))")

// Match methods by name pattern
@Before("execution(* *.save*(..))")

// Match by annotation
@Around("@annotation(Transactional)")

// Match by class type
@Before("within(com.example.repository..*)")

// Composite expressions
@Around("execution(* *.save*(..)) && @annotation(Validated)")
```

### Built-in Interceptors

```java
// Automatic logging
@Logged(level = "INFO")
public void processOrder(Order order) { }

// Performance measurement
@Timed(threshold = 100, unit = ChronoUnit.MILLIS)
public void slowOperation() { }

// Argument validation
@Validated
public void saveUser(User user) { } // Validates user != null

// Transaction management
@Transactional(propagation = Propagation.REQUIRED)
public void transferFunds(Account from, Account to, BigDecimal amount) { }
```

### Using AOP

```java
import com.veld.aop.*;

// Register aspects and interceptors
InterceptorRegistry registry = InterceptorRegistry.getInstance();
registry.registerAspect(new LoggingAspect());
registry.registerInterceptor(new TransactionInterceptor());

// Create proxied instance
ProxyFactory proxyFactory = new ProxyFactory();
UserService proxy = proxyFactory.createProxy(
    new UserService(),
    registry.getInterceptors(UserService.class.getMethod("save", User.class))
);

// Use the proxy - all interceptors are automatically applied
proxy.save(user);
```

## Project Structure

```
Veld/
├── pom.xml                     # Parent POM
├── CHANGELOG.md                # Version history
├── veld-annotations/           # Annotation definitions
│   ├── src/main/java/
│   │   ├── module-info.java    # JPMS module descriptor
│   │   └── com/veld/annotation/
│   │       ├── Component.java
│   │       ├── Inject.java
│   │       ├── Singleton.java
│   │       ├── Prototype.java
│   │       ├── Lazy.java
│   │       ├── Optional.java
│   │       ├── Named.java
│   │       ├── Subscribe.java  # EventBus
│   │       ├── Aspect.java     # AOP
│   │       ├── Around.java
│   │       ├── Before.java
│   │       ├── After.java
│   │       ├── Pointcut.java
│   │       ├── Interceptor.java
│   │       ├── AroundInvoke.java
│   │       ├── InterceptorBinding.java
│   │       └── ...
├── veld-runtime/               # Runtime container
│   ├── src/main/java/
│   │   ├── module-info.java    # JPMS module descriptor
│   │   └── com/veld/runtime/
│   │       ├── VeldContainer.java
│   │       ├── ComponentRegistry.java
│   │       ├── Provider.java
│   │       └── event/
│   │           ├── EventBus.java
│   │           └── DeadEvent.java
├── veld-aop/                   # AOP module
│   ├── src/main/java/
│   │   ├── module-info.java    # JPMS module descriptor
│   │   └── com/veld/aop/
│   │       ├── JoinPoint.java
│   │       ├── InvocationContext.java
│   │       ├── MethodInvocation.java
│   │       ├── MethodInterceptor.java
│   │       ├── Advice.java
│   │       ├── InterceptorRegistry.java
│   │       ├── PointcutExpression.java
│   │       ├── CompositePointcut.java
│   │       ├── ProxyFactory.java
│   │       ├── ProxyMethodHandler.java
│   │       └── interceptor/
│   │           ├── Logged.java
│   │           ├── LoggingInterceptor.java
│   │           ├── Timed.java
│   │           ├── TimingInterceptor.java
│   │           ├── Validated.java
│   │           ├── ValidationInterceptor.java
│   │           ├── Transactional.java
│   │           └── TransactionInterceptor.java
├── veld-processor/             # Compile-time annotation processor
│   ├── src/main/java/
│   │   ├── module-info.java    # JPMS module descriptor
│   │   └── com/veld/processor/
│   │       ├── VeldProcessor.java
│   │       ├── AnnotationHelper.java
│   │       └── ...
├── veld-weaver/                # Bytecode weaver Maven plugin
│   └── src/main/java/
│       └── com/veld/weaver/
│           ├── WeaverMojo.java         # Maven plugin goal
│           └── FieldInjectorWeaver.java # Private field injection support
├── veld-benchmark/             # Performance benchmarks
│   └── src/main/java/
│       └── com/veld/benchmark/
│           ├── StartupBenchmark.java   # Container startup time
│           ├── InjectionBenchmark.java # Dependency lookup time
│           ├── ThroughputBenchmark.java # Operations per second
│           ├── MemoryBenchmark.java    # Memory footprint
│           └── BenchmarkRunner.java    # Main runner
└── veld-example/               # Example application
    ├── src/main/java/
    │   ├── module-info.java    # JPMS module descriptor
    │   └── com/veld/example/
    │       ├── Main.java
    │       ├── aop/
    │       │   ├── LoggingAspect.java
    │       │   ├── PerformanceAspect.java
    │       │   ├── CalculatorService.java
    │       │   └── ProductService.java
    │       └── ...
```

## Build

```bash
# Build all modules
mvn clean install

# Run tests with coverage
mvn clean verify

# View coverage report
open veld-example/target/site/jacoco-aggregate/index.html
```

## Run Example

```bash
cd veld-example
mvn exec:java -Dexec.mainClass="com.veld.example.Main"
```

## How It Works

1. **Compile Time**: The annotation processor scans for `@Component` classes (or `@Singleton`, `@Prototype`, `@Lazy`)
2. **Analysis**: Builds a dependency graph and validates for cycles
3. **Generation**: Creates optimized factory classes using ASM bytecode
4. **Weaving**: `veld-weaver` adds synthetic setters for private field injection
5. **Runtime**: Container uses generated factories - no reflection needed
6. **AOP**: ProxyFactory generates bytecode proxies that intercept method calls

### Generated Code Example

For a component like:

```java
@Singleton
public class UserService {
    @Inject 
    private LogService logService;
    
    @Value("${app.name}")
    private String appName;
}
```

Veld generates an optimized factory and weaves the class:

**1. Weaved Class (private fields get synthetic setters):**
```java
public class UserService {
    private LogService logService;
    private String appName;
    
    // Generated by veld-weaver:
    public synthetic void __di_set_logService(LogService value) {
        this.logService = value;
    }
    
    public synthetic void __di_set_appName(String value) {
        this.appName = value;
    }
}
```

**2. Generated Factory:**
```java
public class UserService$$VeldFactory implements ComponentFactory<UserService> {
    public UserService create(VeldContainer container) {
        UserService instance = new UserService();
        // Uses synthetic setters for private fields:
        instance.__di_set_logService(container.get(LogService.class));
        instance.__di_set_appName(ValueResolver.getInstance().resolve("${app.name}", String.class));
        return instance;
    }
}
```

## Compatibility Matrix

| Java Version | Status | Notes |
|--------------|--------|-------|
| Java 11 | ✅ Supported | Minimum required version |
| Java 17 | ✅ Supported | LTS - Recommended |
| Java 21 | ✅ Supported | LTS - Recommended |
| Java 25 | ✅ Supported | LTS - Latest |

### Dependencies

- **ASM**: 9.8 (supports Java 25 bytecode)
- **Mockito**: 5.20.0 (for testing)
- **ByteBuddy**: 1.17.4 (for testing, Java 25 compatible)
- **JUnit**: 5.11.3

## Benchmarks

Veld includes a comprehensive benchmark suite comparing performance against Spring, Guice, and Dagger.

### Run Benchmarks

```bash
# Build benchmark module
mvn clean package -pl veld-benchmark -am -DskipTests

# Run all benchmarks
java -jar veld-benchmark/target/veld-benchmark.jar

# Run specific benchmark
java -jar veld-benchmark/target/veld-benchmark.jar Startup

# Quick mode (for development)
java -jar veld-benchmark/target/veld-benchmark.jar -f 1 -wi 1 -i 2
```

### Benchmark Results

All benchmarks run on JDK 17, JMH 1.37, with `-Xms512m -Xmx512m`.

#### Throughput (ops/ms - higher is better)

| Framework | Single Thread | Concurrent (4 threads) |
|-----------|---------------|------------------------|
| **Veld** | **3,114,562** | **6,072,014** |
| Dagger | 880,518 | 1,779,421 |
| Spring | 26,181 | 50,549 |
| Guice | 23,099 | 15,631 |

> **Veld is 3.5x faster than Dagger** in throughput operations.

#### Injection Latency (ns/op - lower is better)

| Framework | Simple | Complex | Logger |
|-----------|--------|---------|--------|
| **Veld** | **0.317** | **0.318** | **0.325** |
| Dagger | 1.107 | 1.101 | 1.095 |
| Spring | 38.60 | 51.22 | 51.27 |
| Guice | 45.56 | 42.94 | 45.77 |

> **Veld is 3.5x faster than Dagger** in dependency injection latency.

#### Static Access (ops/ms - higher is better)

| Framework | Score |
|-----------|-------|
| **Veld** | **3,149,165** |
| Dagger | 910,972 |

> **Veld is 3.46x faster than Dagger** in static access patterns.

#### Startup Time (us/op - lower is better)

| Framework | Time |
|-----------|------|
| **Veld** | **≈ 0.001** |
| Dagger | 0.108 |
| Guice | 76.78 |
| Spring | 859.06 |

> **Veld startup is ~100x faster than Dagger**, ~860,000x faster than Spring.

#### Prototype Creation (ns/op - lower is better)

| Framework | Simple |
|-----------|--------|
| **Veld** | **1.58** |
| Dagger | 8.61 |
| Guice | 67.51 |
| Spring | 2,892 |

> **Veld is 5.4x faster than Dagger** in prototype instance creation.

#### Memory (500 containers, ms - lower is better)

| Framework | Time |
|-----------|------|
| **Veld** | **0.010** |
| Dagger | 0.101 |
| Guice | 106.09 |
| Spring | 552.82 |

> **Veld uses 10x less memory than Dagger**, ~55,000x less than Spring.

### Why Veld is Fast

Veld achieves superior performance through:
- **Static Singleton Fields** - All singletons are `static final` fields initialized in static block
- **Zero Reflection** - Pure bytecode generation at compile time
- **Direct Method Calls** - No HashMap lookups, no dynamic dispatch
- **JIT Optimization** - Code pattern allows maximum JVM optimization
- **No Container Overhead** - Singletons live as static fields, not in container maps

See [veld-benchmark/README.md](veld-benchmark/README.md) for detailed benchmark documentation.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and release notes.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
