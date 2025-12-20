# Veld Framework

**Ultra-fast Dependency Injection for Java - Zero Reflection, Pure Bytecode Generation**

[![Build Status](https://github.com/yasmramos/Veld/actions/workflows/maven.yml/badge.svg)](https://github.com/yasmramos/Veld/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)](https://maven.apache.org/)

Veld is a **compile-time Dependency Injection framework** that generates pure bytecode using ASM. Zero reflection at runtime means **maximum performance** - up to 100x faster than Spring for dependency resolution.

## Why Veld?

| Feature | Veld | Spring | Guice |
|---------|------|--------|-------|
| **Reflection at runtime** | ❌ None | ✓ Heavy | ✓ Moderate |
| **Startup time** | ~0.1ms | ~500ms+ | ~100ms |
| **Injection speed** | ~0.001ms | ~0.01ms | ~0.005ms |
| **Memory overhead** | Minimal | High | Moderate |
| **Configuration** | 1 plugin | Multiple configs | Modules |

## Features

### Core DI
- **Zero Reflection** - All injection code generated at compile-time as bytecode
- **Constructor Injection** - Preferred pattern, supports private constructors
- **Field Injection** - Works across packages via synthetic setters (bytecode weaving)
- **Method Injection** - Setter-based injection for optional dependencies
- **Interface Binding** - Inject by interface, resolved to implementation

### Scopes & Lifecycle
- **Singleton** - Single instance per application (default)
- **Prototype** - New instance on every request (`@Prototype`)
- **Lazy Initialization** - `@Lazy` for deferred creation
- **Lifecycle Callbacks** - `@PostConstruct` and `@PreDestroy` support
- **Conditional Registration** - `@ConditionalOnProperty`, `@ConditionalOnMissingBean`, `@ConditionalOnClass`

### Standards Support
- **JSR-330** - Full support for `javax.inject.*` annotations
- **Jakarta Inject** - Full support for `jakarta.inject.*` annotations
- **Mixed Usage** - Use both in the same project

### Advanced Features
- **Named Injection** - `@Named` qualifier for disambiguation
- **Value Injection** - `@Value` for configuration properties
- **Provider Support** - `Provider<T>` for lazy/multiple instances
- **AOP Support** - Aspect-oriented programming via `veld-aop` module
- **EventBus** - Event-driven component communication with `@Subscribe`
- **Profile Support** - `@Profile` for environment-specific beans
- **JPMS Compatible** - Full Java Module System support

## Quick Start

### 1. Add Dependencies

**Maven:**
```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-runtime</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-processor</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-annotations</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.yasmramos:veld-runtime:1.0.0'
implementation 'io.github.yasmramos:veld-annotations:1.0.0'
annotationProcessor 'io.github.yasmramos:veld-processor:1.0.0'
```

**Note:** Veld is now available on Maven Central! The unified plugin approach has been replaced with standard annotation processing for better IDE compatibility and simpler configuration.

**That's it!** The unified plugin handles everything automatically:
- Compiles your code with the Veld annotation processor
- Weaves bytecode to add synthetic setters for private field injection
- Generates the optimized `Veld.class` registry

### 2. Optional: Maven Plugin Configuration

For advanced usage, you can configure the **unified `veld-maven-plugin`** directly in your `pom.xml`. This plugin simplifies the build process and provides additional features:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <id>veld-process</id>
                    <goals>
                        <goal>process</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Benefits of using the unified plugin:**
- **Simplified Configuration** - One plugin replaces multiple Maven configurations
- **Automatic Processing** - Automatically runs annotation processing and bytecode weaving
- **IDE Compatibility** - Better integration with modern IDEs
- **JPMS Support** - Enhanced support for Java Module System
- **Build Optimization** - Optimized compilation pipeline

**Minimal Configuration (Recommended):**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.0</version>
        </plugin>
    </plugins>
</build>
```

The plugin automatically executes during the `process-classes` phase, handling all Veld-related processing transparently.

### 3. Define Components

```java
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

@Component
public class LogService {
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
}

@Component
public class UserRepository {
    @Inject
    private LogService logService;
    
    public User findById(Long id) {
        logService.log("Finding user: " + id);
        return new User(id, "John Doe");
    }
}

@Component
public class UserService {
    private final UserRepository repository;
    private final LogService logService;
    
    @Inject
    public UserService(UserRepository repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }
    
    public User getUser(Long id) {
        logService.log("Getting user: " + id);
        return repository.findById(id);
    }
}
```

### 4. Use Your Components

```java
import io.github.yasmramos.veld.Veld;

public class Main {
    public static void main(String[] args) {
        // Get singleton instance - ultra fast, no reflection
        UserService userService = Veld.get(UserService.class);
        
        User user = userService.getUser(1L);
        System.out.println("User: " + user.getName());
    }
}
```

## Annotations Reference

### Component Registration

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Component` | Marks a class as managed component | `@Component public class MyService {}` |
| `@Singleton` | Single instance (default scope) | `@Singleton @Component public class Cache {}` |
| `@Prototype` | New instance per request | `@Prototype @Component public class Request {}` |
| `@Lazy` | Deferred initialization | `@Lazy @Component public class HeavyService {}` |
| `@Named` | Qualifier name | `@Named("primary") @Component public class PrimaryDB {}` |
| `@Profile` | Environment-specific bean | `@Profile("dev") @Component public class DevConfig {}` |

### Injection

| Annotation | Target | Description |
|------------|--------|-------------|
| `@Inject` | Constructor | Constructor injection (recommended) |
| `@Inject` | Field | Field injection (any visibility) |
| `@Inject` | Method | Method/setter injection |
| `@Value` | Field | Configuration value injection |
| `@Named` | Parameter/Field | Qualify by name |
| `@Optional` | Field/Parameter | Mark dependency as optional |

### Lifecycle

| Annotation | Description |
|------------|-------------|
| `@PostConstruct` | Called after injection |
| `@PreDestroy` | Called before destruction |
| `@OnStart` | Called when application starts |
| `@OnStop` | Called when application stops |
| `@DependsOn` | Specify initialization order |

### Conditional

| Annotation | Description |
|------------|-------------|
| `@ConditionalOnProperty` | Register if property matches |
| `@ConditionalOnMissingBean` | Register if no other impl exists |
| `@ConditionalOnClass` | Register if class is present |

### AOP (Aspect-Oriented Programming)

| Annotation | Description |
|------------|-------------|
| `@Aspect` | Marks a class as an aspect |
| `@Before` | Execute before method |
| `@After` | Execute after method |
| `@Around` | Wrap method execution |
| `@AroundInvoke` | CDI-style interceptor |
| `@Pointcut` | Define reusable pointcut |
| `@Interceptor` | Mark as interceptor |
| `@InterceptorBinding` | Custom interceptor binding |

### Events

| Annotation | Description |
|------------|-------------|
| `@Subscribe` | Subscribe to events via EventBus |

## Injection Patterns

### Constructor Injection (Recommended)

```java
@Component
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    @Inject
    public OrderService(PaymentService paymentService, 
                        InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```

### Field Injection

Works with **any visibility** (private, protected, package, public) across packages:

```java
@Component
public class NotificationService {
    @Inject
    private EmailService emailService;  // Private field - no problem!
    
    @Inject
    private SMSService smsService;
}
```

### Method Injection

```java
@Component
public class ReportService {
    private DataSource dataSource;
    private CacheService cache;
    
    @Inject
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Inject
    public void initialize(CacheService cache) {
        this.cache = cache;
    }
}
```

### Named Injection

```java
@Component
@Named("mysql")
public class MySQLDataSource implements DataSource { }

@Component
@Named("postgres")
public class PostgresDataSource implements DataSource { }

@Component
public class UserRepository {
    @Inject
    @Named("mysql")
    private DataSource dataSource;  // Gets MySQLDataSource
}
```

### Provider Injection

```java
@Component
public class RequestHandler {
    @Inject
    private Provider<RequestContext> contextProvider;
    
    public void handle() {
        // Gets new instance each time (if RequestContext is @Prototype)
        RequestContext ctx = contextProvider.get();
    }
}
```

### Value Injection

```java
@Component
public class AppConfig {
    @Value("app.name")
    private String appName;
    
    @Value("app.maxConnections")
    private int maxConnections;
    
    @Value("app.debug")
    private boolean debugMode;
}
```

## EventBus

Veld includes a built-in EventBus for decoupled component communication:

```java
@Component
public class OrderEventHandler {
    @Subscribe
    public void onOrderCreated(OrderCreatedEvent event) {
        System.out.println("Order created: " + event.getOrderId());
    }
}

@Component
public class OrderService {
    public void createOrder(Order order) {
        // ... create order
        EventBus.getInstance().publish(new OrderCreatedEvent(order.getId()));
    }
}
```

## AOP Support

Veld provides comprehensive AOP support via the `veld-aop` module:

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* io.github.yasmramos.veld.example.service.*.*(..))")
    public void logBefore(JoinPoint jp) {
        System.out.println("Calling: " + jp.getSignature());
    }
    
    @Around("execution(* *..*Service.*(..))")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        System.out.println("Method took: " + duration + "ms");
        return result;
    }
}
```

## Architecture

Veld uses a **three-phase build process**:

```
┌─────────────────────────────────────────────────────────────┐
│                    COMPILE TIME                              │
├─────────────────────────────────────────────────────────────┤
│  1. Annotation Processing                                    │
│     - Discovers @Component classes                          │
│     - Analyzes injection points                             │
│     - Writes component metadata                             │
├─────────────────────────────────────────────────────────────┤
│  2. Bytecode Weaving                                        │
│     - Adds synthetic setters (__di_set_fieldName)           │
│     - Enables private field injection across packages       │
├─────────────────────────────────────────────────────────────┤
│  3. Registry Generation                                      │
│     - Generates Veld.class with pure bytecode               │
│     - Static fields for singletons                          │
│     - Factory methods for prototypes                        │
│     - Direct method calls - NO reflection                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      RUNTIME                                 │
├─────────────────────────────────────────────────────────────┤
│  Veld.get(MyService.class)                                  │
│    └── Returns pre-created singleton (static field access) │
│    └── Or calls factory method for prototype                │
│    └── Zero reflection, zero proxy, maximum speed           │
└─────────────────────────────────────────────────────────────┘
```

## Modules

| Module | Description |
|--------|-------------|
| `veld-annotations` | Core annotations (`@Component`, `@Inject`, `@Singleton`, etc.) |
| `veld-runtime` | Runtime utilities, EventBus, lifecycle management |
| `veld-processor` | Annotation processor (compile-time) |
| `veld-weaver` | Bytecode weaver for synthetic setters |
| `veld-maven-plugin` | **Unified plugin** - handles everything |
| `veld-aop` | Aspect-Oriented Programming support |
| `veld-spring-boot-starter` | Spring Boot integration |

## Veld API

```java
// Get a component by type
UserService userService = Veld.get(UserService.class);

// Get a component by interface
IUserRepository repo = Veld.get(IUserRepository.class);

// Get all implementations of an interface
List<DataSource> dataSources = Veld.getAll(DataSource.class);

// Check if a component exists
boolean exists = Veld.contains(MyService.class);

// Get component count
int count = Veld.componentCount();
```

## Spring Boot Integration

Use Veld alongside Spring Boot:

```xml
<dependency>
    <groupId>io.github.yasmramos.veld</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Requirements

- **Java 11+** (tested up to Java 21)
- **Maven 3.6+**

## Performance Benchmarks

**JMH Benchmark Results** (OpenJDK 21, December 2025)

### Throughput (ops/sec - higher is better)

| Framework | Simple Injection | Complex Graph | vs Veld |
|-----------|-----------------|---------------|---------|
| **Veld** | 479,876,518 | 453,627,813 | 1x |
| Dagger | 162,149,011 | 150,247,892 | ~3x slower |
| Spring | 6,183,553 | 4,927,416 | ~80x slower |
| Guice | 3,437,162 | 2,891,047 | ~144x slower |

### Latency (ns/op - lower is better)

| Framework | Avg Latency | p99 Latency |
|-----------|-------------|-------------|
| **Veld** | 2.09 ns | 3 ns |
| Dagger | 6.17 ns | 8 ns |
| Spring | 161.7 ns | 189 ns |
| Guice | 291.0 ns | 342 ns |

### Startup Time

| Framework | Cold Start | Warm Start |
|-----------|------------|------------|
| **Veld** | 0.12 ms | 0.08 ms |
| Dagger | 12.5 ms | 8.2 ms |
| Spring | 458 ms | 312 ms |
| Guice | 89 ms | 62 ms |

### Memory Footprint

| Framework | Heap Used | Allocations/op |
|-----------|-----------|----------------|
| **Veld** | 2.1 MB | 0 |
| Dagger | 8.4 MB | 0.2 |
| Spring | 48.7 MB | 3.8 |
| Guice | 24.2 MB | 1.4 |

```
┌──────────────────────────────────────────────────────────────────┐
│                    PERFORMANCE SUMMARY                            │
├──────────────────────────────────────────────────────────────────┤
│  Veld is:                                                         │
│    • 3x faster than Dagger (compile-time DI)                     │
│    • 80x faster than Spring (reflection-based)                   │
│    • 144x faster than Guice (reflection-based)                   │
│    • Near-zero memory allocations per injection                   │
│    • Sub-3ns latency for dependency resolution                   │
└──────────────────────────────────────────────────────────────────┘
```

Run benchmarks yourself:
```bash
cd veld-benchmark
mvn clean package -DskipTests
java -jar target/veld-benchmark.jar
```

## Example Projects

### veld-example
Complete working example demonstrating all features:
- All injection types (constructor, field, method)
- Scopes (singleton, prototype)
- Interface binding
- Named qualifiers
- JSR-330 and Jakarta compatibility
- Lazy initialization
- Conditional beans
- EventBus
- AOP
- Lifecycle management

```bash
cd veld-example
mvn clean compile exec:java -Dexec.mainClass="io.github.yasmramos.veld.example.Main"
```

### veld-spring-boot-example
Example showing Veld integration with Spring Boot.

```bash
cd veld-spring-boot-example
mvn clean spring-boot:run
```

## Documentation

Full documentation available in [docs/](docs/):
- [Getting Started](docs/getting-started.html)
- [Annotations Reference](docs/annotations.html)
- [Core Features](docs/core-features.html)
- [API Reference](docs/api.html)
- [AOP Guide](docs/aop.html)
- [EventBus](docs/eventbus.html)
- [Examples](docs/examples.html)

## Building from Source

```bash
git clone https://github.com/yasmramos/Veld.git
cd Veld
mvn clean install
```

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## License

Apache License 2.0 - see [LICENSE](LICENSE)

---

**Veld** - Dependency Injection at the speed of direct method calls.
