# 🚀 Veld - Ultra-Fast Dependency Injection Framework

**Veld** is an ultra-fast dependency injection framework that generates optimized bytecode at compile-time. **NO reflection at runtime** for maximum performance.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.yasmramos/veld-parent.svg)](https://mvnrepository.com/artifact/io.github.yasmramos/veld-parent)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-11%2B-green.svg)](https://www.oracle.com/java/)

## ✨ Key Features

### ⚡ **Ultra-Fast Performance**
- **Thread-local cache**: ~2ns lookup time
- **Hash table lookup**: ~5ns lookup time  
- **Linear fallback**: ~15ns lookup time (rare)
- **Zero runtime reflection overhead**
- **Direct bytecode generation for maximum speed**

### 🔧 **Automatic Complete Integration**
All these features work **automatically** when you do `Veld.get()`:

| Feature | Description | Status |
|---|---|---|
| **Lifecycle Callbacks** | `@PostConstruct`, `@PreDestroy` execute automatically | ✅ |
| **EventBus Integration** | `@Subscribe` methods register automatically | ✅ |
| **Value Resolution** | `@Value` annotations resolve automatically | ✅ |
| **Conditional Loading** | `@Profile`, `@ConditionalOnProperty` filter automatically | ✅ |
| **Named Injection** | Name-based injection using `get(Class, String)` | ✅ |
| **Provider Injection** | Automatic `Provider<T>` support | ✅ |
| **Optional Injection** | Automatic `Optional<T>` support | ✅ |
| **Dependencies Management** | `@DependsOn` and circular dependency detection | ✅ |
| **Multiple Scopes** | Singleton and Prototype with optimal performance | ✅ |
| **Interface-based Injection** | Interface-based injection | ✅ |

## 🎯 Usage Example

```java
@Singleton
@Component
public class OrderService {
    
    @Inject
    private UserService userService;
    
    @Inject
    private PaymentService paymentService;
    
    @Value("${app.database.url}")
    private String databaseUrl;
    
    @PostConstruct
    public void init() {
        System.out.println("OrderService initialized with DB: " + databaseUrl);
    }
    
    public void createOrder(String userId, double amount) {
        User user = userService.getUser(userId);
        paymentService.processPayment(user, amount);
        System.out.println("Order created for user: " + user.getName());
    }
}

// Usage
OrderService orderService = Veld.get(OrderService.class);
orderService.createOrder("user123", 99.99);
```

## 🚀 Quick Start

### Add to your project

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
```

**Gradle:**
```gradle
implementation 'io.github.yasmramos:veld-runtime:1.0.0'
annotationProcessor 'io.github.yasmramos:veld-processor:1.0.0'
```

### Basic Configuration

**Veld automatically detects and configures:**

1. **Components** - Classes with `@Singleton`, `@Prototype`, or `@Component`
2. **Dependencies** - Fields with `@Inject` 
3. **Configuration** - `@Value` from properties/environment
4. **Lifecycle** - `@PostConstruct` and `@PreDestroy` methods
5. **Events** - Methods with `@Subscribe` for EventBus
6. **Conditions** - `@Profile`, `@ConditionalOnProperty`, etc.

### Advanced Configuration

```java
// Profile-based activation
Veld.setActiveProfiles("production", "mysql");

// Get components by interface
Service service = Veld.get(Service.class);

// Get named components
UserService userService = Veld.get(UserService.class, "mainUserService");

// Optional injection
Optional<EmailService> emailService = Veld.getOptional(EmailService.class);
if (emailService.isPresent()) {
    emailService.get().sendEmail("test@example.com");
}

// Provider injection (for circular dependencies)
@Component
public class CircularA {
    @Inject
    private Provider<CircularB> circularBProvider;
    
    public void doSomething() {
        CircularB b = circularBProvider.get();
        b.doSomethingElse();
    }
}
```

## 📋 Supported Annotations

### Component Annotations (choose ONE - mutually exclusive):
- `@io.github.yasmramos.veld.annotation.Component` (requires scope annotation)
- `@io.github.yasmramos.veld.annotation.Singleton` (Veld native - singleton scope)
- `@io.github.yasmramos.veld.annotation.Prototype` (Veld native - prototype scope)
- `@io.github.yasmramos.veld.annotation.Lazy` (Veld native - lazy singleton)
- `@javax.inject.Singleton` (JSR-330 - singleton scope)
- `@jakarta.inject.Singleton` (Jakarta EE - singleton scope)

### Injection Annotations:
- `@io.github.yasmramos.veld.annotation.Inject` (Veld native)
- `@javax.inject.Inject` (JSR-330)
- `@jakarta.inject.Inject` (Jakarta EE)

### Qualifier Annotations:
- `@io.github.yasmramos.veld.annotation.Named` (Veld native)
- `@javax.inject.Named` (JSR-330)
- `@jakarta.inject.Named` (Jakarta EE)

### Lifecycle Annotations:
- `@io.github.yasmramos.veld.annotation.PostConstruct`
- `@io.github.yasmramos.veld.annotation.PreDestroy`

### Configuration Annotations:
- `@io.github.yasmramos.veld.annotation.Value`

### Event Annotations:
- `@io.github.yasmramos.veld.annotation.Subscribe`

### Conditional Annotations:
- `@io.github.yasmramos.veld.annotation.Profile`
- `@io.github.yasmramos.veld.annotation.ConditionalOnClass`
- `@io.github.yasmramos.veld.annotation.ConditionalOnProperty`
- `@io.github.yasmramos.veld.annotation.ConditionalOnMissingBean`

### Dependency Annotations:
- `@io.github.yasmramos.veld.annotation.DependsOn`

### Provider Support:
- `javax.inject.Provider<T>` (JSR-330)
- `jakarta.inject.Provider<T>` (Jakarta EE)

## 🏗️ Architecture

### Compile-Time Processing
1. **Annotation Processor** scans for components
2. **ASM Bytecode Generation** creates optimized lookup code
3. **Runtime Registry** provides fast component retrieval
4. **Zero Reflection** for maximum performance

### Performance Optimizations
- **Thread-local caching** for single-threaded workloads
- **Hash table lookups** for multi-threaded scenarios
- **Linear search fallback** for rare edge cases
- **Direct bytecode** eliminating reflection overhead

## 📊 Benchmarks

Compared to other DI frameworks:

| Framework | Lookup Time | Reflection Overhead |
|-----------|-------------|-------------------|
| **Veld** | **~2-15ns** | **None** |
| Spring | ~500ns | High |
| Guice | ~300ns | Medium |
| CDI | ~800ns | High |

*Benchmark: 1M component lookups on Intel i7, Java 17*

## 🔧 Spring Boot Integration

Use the Spring Boot starter for seamless integration:

```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or with Gradle:
```gradle
implementation 'io.github.yasmramos:veld-spring-boot-starter:1.0.0'
```

## 🧪 Testing

```java
// Clean slate for each test
Veld.clear();

// Register test components
Veld.register(TestComponent.class);

// Get components for testing
TestComponent component = Veld.get(TestComponent.class);
```

## 📚 Documentation

- [API Documentation](https://javadoc.io/doc/io.github.yasmramos/veld-runtime)
- [Examples](https://github.com/yasmramos/Veld/tree/main/veld-example)
- [Benchmark Results](https://github.com/yasmramos/Veld/tree/main/veld-benchmark)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for your changes
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚡ Performance Summary

- **2-15 nanoseconds** component lookup time
- **Zero reflection** at runtime
- **Compile-time optimization** with bytecode generation
- **Thread-safe** with optimal performance
- **Memory efficient** with minimal overhead

**Veld** brings you the performance of manual dependency injection with the convenience of a modern DI framework!