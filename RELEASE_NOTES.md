# Veld Framework v1.0.0 - Release Notes

## Overview
Veld v1.0.0 is the first stable release of the ultra-fast dependency injection framework for Java. Veld provides zero-reflection runtime performance through compile-time bytecode generation.

## Key Features

### 🚀 Ultra-Fast Performance
- **Thread-local cache**: ~2ns component lookup
- **Hash table lookup**: ~5ns component lookup  
- **Linear fallback**: ~15ns component lookup (rare)
- **Zero reflection overhead** at runtime
- **Direct bytecode generation** for maximum speed

### 🔧 Complete Automatic Integration
All features work automatically when you call `Veld.get()`:
- ✅ Lifecycle callbacks (@PostConstruct, @PreDestroy)
- ✅ EventBus integration (@Subscribe methods)
- ✅ Value resolution (@Value annotations)
- ✅ Conditional loading (@Profile, @ConditionalOnProperty)
- ✅ Named injection (get(Class, String))
- ✅ Provider<T> and Optional<T> support
- ✅ Dependencies management (@DependsOn)

### 📋 Standards Compliance
- **JSR-330** - Full support for `javax.inject.*` annotations
- **Jakarta Inject** - Full support for `jakarta.inject.*` annotations
- **Mixed Usage** - Use both standards in the same project

### 🏗️ Architecture
- **Compile-time processing** with annotation processors
- **ASM bytecode generation** for optimized injection code
- **Runtime registry** for fast component retrieval
- **Zero reflection** for maximum performance

## Modules

### Core Modules
- **veld-annotations** - Core annotations (@Singleton, @Inject, etc.)
- **veld-runtime** - Minimal runtime container
- **veld-processor** - Annotation processor for compile-time generation
- **veld-weaver** - Bytecode weaving for private field injection

### Integration Modules
- **veld-aop** - Aspect-oriented programming support
- **veld-spring-boot-starter** - Spring Boot integration
- **veld-maven-plugin** - Unified Maven build process

### Utility Modules
- **veld-benchmark** - Performance benchmarks
- **veld-example** - Comprehensive usage examples

## Performance Comparison

| Framework | Lookup Time | Reflection Overhead |
|-----------|-------------|-------------------|
| **Veld** | **~2-15ns** | **None** |
| Spring | ~500ns | High |
| Guice | ~300ns | Medium |
| CDI | ~800ns | High |

*Benchmark: 1M component lookups on Intel i7, Java 17*

## Usage Example

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
    
    @Subscribe
    public void onOrderEvent(OrderEvent event) {
        // Automatically registered in EventBus
    }
    
    public void createOrder(String userId, double amount) {
        User user = userService.getUser(userId);
        paymentService.processPayment(user, amount);
    }
}

// Everything works automatically:
// - Dependencies injected
// - @Value resolved from properties  
// - @PostConstruct executed
// - @Subscribe registered in EventBus
OrderService service = Veld.get(OrderService.class);
```

## Supported Annotations

### Component Annotations
- `@io.github.yasmramos.veld.annotation.Component` + `@Singleton`/`@Prototype`
- `@io.github.yasmramos.veld.annotation.Singleton` (Veld native)
- `@io.github.yasmramos.veld.annotation.Prototype` (Veld native)
- `@javax.inject.Singleton` (JSR-330)
- `@jakarta.inject.Singleton` (Jakarta EE)

### Injection Annotations
- `@io.github.yasmramos.veld.annotation.Inject` (Veld native)
- `@javax.inject.Inject` (JSR-330)
- `@jakarta.inject.Inject` (Jakarta EE)

### Configuration Annotations
- `@io.github.yasmramos.veld.annotation.Value`
- `@io.github.yasmramos.veld.annotation.Named`

### Lifecycle Annotations
- `@io.github.yasmramos.veld.annotation.PostConstruct`
- `@io.github.yasmramos.veld.annotation.PreDestroy`

### Event Annotations
- `@io.github.yasmramos.veld.annotation.Subscribe`

### Conditional Annotations
- `@io.github.yasmramos.veld.annotation.Profile`
- `@io.github.yasmramos.veld.annotation.ConditionalOnClass`
- `@io.github.yasmramos.veld.annotation.ConditionalOnProperty`
- `@io.github.yasmramos.veld.annotation.ConditionalOnMissingBean`

## Dependencies

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

## Java Compatibility
- **Java 11+** (tested with Java 11, 17, 21)
- **Maven 3.6+**
- **Gradle 7.0+**

## License
Apache License 2.0 - see [LICENSE](LICENSE) file for details.

## Support
- **GitHub Issues**: https://github.com/yasmramos/Veld/issues
- **Documentation**: https://github.com/yasmramos/Veld
- **Benchmarks**: https://github.com/yasmramos/Veld/tree/main/veld-benchmark

## Known Limitations
- Requires annotation processing at compile time
- Private field injection requires bytecode weaving
- Not compatible with Java 8 and below

---

**Release Date**: December 17, 2025  
**Version**: 1.0.0  
**Type**: Major Release (First Stable)