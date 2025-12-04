# Veld DI Framework

[![CI/CD](https://github.com/yasmramos/Veld/actions/workflows/ci.yml/badge.svg)](https://github.com/yasmramos/Veld/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-11%2B-orange)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/yasmramos/Veld)](https://github.com/yasmramos/Veld/releases)

A lightweight, compile-time Dependency Injection framework for Java using pure ASM bytecode generation. **Zero reflection, zero runtime overhead.**

## Features

### Core DI
- **Pure Bytecode Generation**: Uses ASM to generate optimized factory classes at compile time
- **Zero Reflection**: All dependency resolution happens via generated code, not reflection
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

## Quick Start

### 1. Add Dependencies

```xml
<dependencies>
    <!-- Veld Annotations -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-annotations</artifactId>
        <version>1.0.0-alpha.5</version>
    </dependency>
    
    <!-- Veld Runtime -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-runtime</artifactId>
        <version>1.0.0-alpha.5</version>
    </dependency>
    
    <!-- Veld AOP (optional) -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-aop</artifactId>
        <version>1.0.0-alpha.5</version>
    </dependency>
    
    <!-- Veld Processor (compile-time only) -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-processor</artifactId>
        <version>1.0.0-alpha.5</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 2. Configure Annotation Processor

```xml
<build>
    <plugins>
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
                        <version>1.0.0-alpha.5</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
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

## Annotation Support

Veld supports annotations from three sources, which can be mixed freely:

| Feature | Veld | JSR-330 (javax) | Jakarta |
|---------|------|-----------------|---------|
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

> **Note**: `@Singleton`, `@Prototype`, and `@Lazy` automatically imply `@Component`, so you don't need to add both.

### Example: Mixed Annotations

```java
@Singleton  // Veld annotation (implies @Component)
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
    UserRepository userRepository;  // Must be non-private (no reflection)
}
```

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
    IUserRepository userRepository;  // Injects UserRepositoryImpl
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
    CacheService cache;  // Will be null if CacheService is not registered
    
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
    java.util.Optional<MetricsService> metrics;  // Will be Optional.empty() if not found
    
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
    
    @Subscribe(priority = 10)  // Higher priority = earlier execution
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
public void saveUser(User user) { }  // Validates user != null

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
│   └── src/main/java/
│       └── com/veld/annotation/
│           ├── Component.java
│           ├── Inject.java
│           ├── Singleton.java
│           ├── Prototype.java
│           ├── Lazy.java
│           ├── Optional.java
│           ├── Named.java
│           ├── Subscribe.java          # EventBus
│           ├── Aspect.java             # AOP
│           ├── Around.java
│           ├── Before.java
│           ├── After.java
│           ├── Pointcut.java
│           ├── Interceptor.java
│           ├── AroundInvoke.java
│           ├── InterceptorBinding.java
│           └── ...
├── veld-runtime/               # Runtime container
│   └── src/main/java/
│       └── com/veld/runtime/
│           ├── VeldContainer.java
│           ├── ComponentRegistry.java
│           ├── Provider.java
│           └── event/
│               ├── EventBus.java
│               └── DeadEvent.java
├── veld-aop/                   # AOP module
│   └── src/main/java/
│       └── com/veld/aop/
│           ├── JoinPoint.java
│           ├── InvocationContext.java
│           ├── MethodInvocation.java
│           ├── MethodInterceptor.java
│           ├── Advice.java
│           ├── InterceptorRegistry.java
│           ├── PointcutExpression.java
│           ├── CompositePointcut.java
│           ├── ProxyFactory.java
│           ├── ProxyMethodHandler.java
│           └── interceptor/
│               ├── Logged.java
│               ├── LoggingInterceptor.java
│               ├── Timed.java
│               ├── TimingInterceptor.java
│               ├── Validated.java
│               ├── ValidationInterceptor.java
│               ├── Transactional.java
│               └── TransactionInterceptor.java
├── veld-processor/             # Compile-time annotation processor
│   └── src/main/java/
│       └── com/veld/processor/
│           ├── VeldProcessor.java
│           ├── AnnotationHelper.java
│           └── ...
└── veld-example/               # Example application
    └── src/main/java/
        └── com/veld/example/
            ├── Main.java
            ├── aop/
            │   ├── LoggingAspect.java
            │   ├── PerformanceAspect.java
            │   ├── CalculatorService.java
            │   └── ProductService.java
            └── ...
```

## Requirements

- **Java**: 11 or higher
- **Maven**: 3.6+

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
4. **Runtime**: Container uses generated factories - no reflection needed
5. **AOP**: ProxyFactory generates bytecode proxies that intercept method calls

### Generated Code Example

For a component like:

```java
@Singleton
public class UserService {
    @Inject LogService logService;
}
```

Veld generates an optimized factory:

```java
public class UserService$$VeldFactory implements ComponentFactory<UserService> {
    public UserService create(VeldContainer container) {
        UserService instance = new UserService();
        instance.logService = container.get(LogService.class);
        return instance;
    }
}
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and release notes.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
