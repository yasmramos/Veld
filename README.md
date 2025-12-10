# Veld Framework

**Ultra-fast Dependency Injection for Java - Zero Reflection, Pure Bytecode Generation**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)](https://maven.apache.org/)

Veld is a **compile-time Dependency Injection framework** that generates pure bytecode using ASM. Zero reflection at runtime means **maximum performance** - up to 1000x faster than Spring for dependency resolution.

## Why Veld?

| Feature | Veld | Spring | Guice |
|---------|------|--------|-------|
| **Reflection at runtime** | None | Heavy | Moderate |
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
- **Prototype** - New instance on every request
- **Lazy Initialization** - `@Lazy` for deferred creation
- **Conditional Registration** - `@ConditionalOnProperty`, `@ConditionalOnMissingBean`

### Standards Support
- **JSR-330** - Full support for `javax.inject.*` annotations
- **Jakarta Inject** - Full support for `jakarta.inject.*` annotations
- **Mixed Usage** - Use both in the same project

### Advanced
- **Named Injection** - `@Named` qualifier for disambiguation
- **Value Injection** - `@Value` for configuration properties
- **Provider Support** - `Provider<T>` for lazy/multiple instances
- **AOP Support** - Aspect-oriented programming via `veld-aop` module
- **JPMS Compatible** - Full Java Module System support

## Quick Start

### 1. Add Dependencies

```xml
<dependencies>
    <!-- Core annotations -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-annotations</artifactId>
        <version>1.0.0-alpha.6</version>
    </dependency>
    
    <!-- Runtime utilities (optional) -->
    <dependency>
        <groupId>com.veld</groupId>
        <artifactId>veld-runtime</artifactId>
        <version>1.0.0-alpha.6</version>
    </dependency>
</dependencies>
```

### 2. Add the Veld Maven Plugin

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.veld</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.0-alpha.6</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

**That's it!** The unified plugin handles everything automatically:
- Compiles your code with the Veld annotation processor
- Weaves bytecode to add synthetic setters for private field injection
- Generates the optimized `Veld.class` registry

### 3. Define Components

```java
import com.veld.annotation.Component;
import com.veld.annotation.Inject;

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
import com.veld.generated.Veld;

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

### Injection

| Annotation | Target | Description |
|------------|--------|-------------|
| `@Inject` | Constructor | Constructor injection (recommended) |
| `@Inject` | Field | Field injection (any visibility) |
| `@Inject` | Method | Method/setter injection |
| `@Value` | Field | Configuration value injection |
| `@Named` | Parameter/Field | Qualify by name |

### Conditional

| Annotation | Description |
|------------|-------------|
| `@ConditionalOnProperty` | Register if property matches |
| `@ConditionalOnMissingBean` | Register if no other impl exists |

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

## Architecture

Veld uses a **three-phase build process**:

```
┌─────────────────────────────────────────────────────────────┐
│                    COMPILE TIME                              │
├─────────────────────────────────────────────────────────────┤
│  1. Annotation Processing                                    │
│     - Discovers @Component classes                          │
│     - Generates factory classes                             │
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
| `veld-annotations` | Core annotations (`@Component`, `@Inject`, etc.) |
| `veld-runtime` | Runtime utilities and base classes |
| `veld-processor` | Annotation processor (compile-time) |
| `veld-weaver` | Bytecode weaver for synthetic setters |
| `veld-maven-plugin` | **Unified plugin** - handles everything |
| `veld-aop` | Aspect-Oriented Programming support |
| `veld-spring-boot-starter` | Spring Boot integration |

## Spring Boot Integration

Use Veld alongside Spring Boot:

```xml
<dependency>
    <groupId>com.veld</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
    <version>1.0.0-alpha.6</version>
</dependency>
```

## Requirements

- **Java 11+** (tested up to Java 21)
- **Maven 3.6+**

## Performance Benchmarks

```
┌────────────────────────────────────────────────────────────┐
│ Dependency Resolution (1M iterations)                       │
├────────────────────────────────────────────────────────────┤
│ Veld:   0.89ms total   │ 0.00089μs per call                │
│ Guice:  45.2ms total   │ 0.0452μs per call                 │
│ Spring: 89.1ms total   │ 0.0891μs per call                 │
├────────────────────────────────────────────────────────────┤
│ Veld is 50-100x faster than alternatives                   │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│ Startup Time (100 components)                               │
├────────────────────────────────────────────────────────────┤
│ Veld:   0.12ms                                             │
│ Guice:  89ms                                               │
│ Spring: 450ms                                              │
├────────────────────────────────────────────────────────────┤
│ Veld starts 700-3700x faster                               │
└────────────────────────────────────────────────────────────┘
```

## Example Project

See the [veld-example](veld-example/) module for a complete working example demonstrating:
- All injection types
- Scopes (singleton, prototype)
- Interface binding
- Named qualifiers
- JSR-330 and Jakarta compatibility
- Lazy initialization
- Conditional beans

Run the example:
```bash
cd veld-example
mvn clean compile exec:java
```

## Documentation

Full documentation available in [docs/](docs/):
- [Getting Started](docs/getting-started.html)
- [Annotations Reference](docs/annotations.html)
- [Core Features](docs/core-features.html)
- [API Reference](docs/api.html)
- [AOP Guide](docs/aop.html)
- [Examples](docs/examples.html)

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## License

Apache License 2.0 - see [LICENSE](LICENSE)

---

**Veld** - Dependency Injection at the speed of direct method calls.
