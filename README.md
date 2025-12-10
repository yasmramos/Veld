# Veld Framework

**Ultra-fast Dependency Injection for Java - Zero Reflection, Pure Bytecode**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://openjdk.java.net/)

Veld is a compile-time Dependency Injection framework that generates pure bytecode using ASM. No reflection at runtime means **maximum performance**.

## Features

- **Zero Reflection** - All injection code generated at compile-time
- **Ultra-Fast** - Direct field access and method calls
- **Simple API** - Just `Veld.get(MyService.class)`
- **JSR-330 Compatible** - Supports `javax.inject` and `jakarta.inject`
- **Full DI Support** - Constructor, field, and method injection
- **Scopes** - Singleton and Prototype out of the box
- **Lazy Loading** - `@Lazy` for deferred initialization
- **Named Injection** - `@Named` for disambiguation

## Quick Start

### 1. Add Dependencies

```xml
<dependencies>
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

That's it! The plugin handles everything:
- Annotation processing
- Bytecode weaving
- Registry generation

### 3. Define Components

```java
@Component
public class LogService {
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
}

@Component
public class UserService {
    @Inject
    private LogService logService;
    
    public void createUser(String name) {
        logService.log("Creating user: " + name);
    }
}
```

### 4. Use Your Components

```java
public class Main {
    public static void main(String[] args) {
        UserService userService = Veld.get(UserService.class);
        userService.createUser("John");
    }
}
```

## Annotations

| Annotation | Description |
|------------|-------------|
| `@Component` | Marks a class as a managed component |
| `@Inject` | Marks a field, constructor, or method for injection |
| `@Named` | Qualifies injection by name |
| `@Singleton` | Single instance (default) |
| `@Prototype` | New instance per request |
| `@Lazy` | Deferred initialization |
| `@Value` | Inject configuration values |

## Injection Types

### Constructor Injection
```java
@Component
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
@Component
public class NotificationService {
    @Inject
    private EmailService emailService;
}
```

### Method Injection
```java
@Component
public class ReportService {
    private DataSource dataSource;
    
    @Inject
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
```

## Performance

Veld outperforms reflection-based frameworks:

| Framework | Startup | Injection |
|-----------|---------|-----------|
| Veld | ~0.1ms | ~0.001ms |
| Spring | ~500ms | ~0.01ms |
| Guice | ~100ms | ~0.005ms |

## Modules

| Module | Description |
|--------|-------------|
| `veld-annotations` | Core annotations |
| `veld-runtime` | Runtime utilities |
| `veld-processor` | Annotation processor |
| `veld-weaver` | Bytecode weaver |
| `veld-maven-plugin` | Unified Maven plugin |
| `veld-aop` | AOP support |
| `veld-spring-boot-starter` | Spring Boot integration |

## Documentation

See the [docs](docs/) folder for detailed documentation.

## License

Apache License 2.0 - see [LICENSE](LICENSE)
