# Examples

Veld includes several example projects demonstrating different features and use cases.

## veld-example

A comprehensive example demonstrating all core features:

```bash
cd veld-example
mvn clean compile exec:java -Dexec.mainClass="io.github.yasmramos.veld.example.Main"
```

### Features Demonstrated

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

### Project Structure

```
veld-example/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── io/
                └── github/
                    └── yasmramos/
                        └── veld/
                            └── example/
                                ├── Main.java
                                ├── annotation/
                                │   ├── ConstructorInjectionExample.java
                                │   ├── FieldInjectionExample.java
                                │   └── MethodInjectionExample.java
                                ├── component/
                                │   ├── SimpleComponent.java
                                │   └── DependentComponent.java
                                ├── lifecycle/
                                │   ├── LifecycleExample.java
                                │   └── PostConstructExample.java
                                ├── qualifier/
                                │   ├── NamedExample.java
                                │   └── PrimaryExample.java
                                ├── scope/
                                │   ├── SingletonExample.java
                                │   └── PrototypeExample.java
                                └── Main.java
```

## veld-spring-boot-example

Example showing Veld integration with Spring Boot:

```bash
cd veld-spring-boot-example
mvn clean spring-boot:run
```

### Features Demonstrated

- Spring Boot auto-configuration
- Veld alongside Spring components
- Hybrid dependency injection
- Spring Boot properties integration
- Actuator endpoints with Veld components

### Project Structure

```
veld-spring-boot-example/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── io/
        │       └── github/
        │           └── yasmramos/
        │               └── veld/
        │                   └── spring/
        │                       ├── VeldSpringApplication.java
        │                       ├── controller/
        │                       │   └── HelloController.java
        │                       ├── service/
        │                       │   ├── GreetingService.java
        │                       │   └── GreetingServiceImpl.java
        │                       └── config/
        │                           └── AppConfig.java
        └── resources/
            └── application.properties
```

## veld-benchmark

JMH benchmarks for performance testing:

```bash
cd veld-benchmark
mvn clean package -DskipTests
java -jar target/veld-benchmark.jar
```

### Available Benchmarks

- Injection throughput
- Injection latency
- Startup time
- Memory usage
- Prototype creation
- Concurrent access

### Running Specific Benchmarks

```bash
# Run only injection benchmarks
java -jar target/veld-benchmark.jar -f 1 -i 5 -t 1 "*Injection*"

# Run only startup benchmarks
java -jar target/veld-benchmark.jar -f 1 -i 5 -t 1 "*Startup*"
```

## Common Patterns

### Basic Dependency Injection

```java
// Define components
@Component
public class ServiceA { }

@Component
public class ServiceB {
    @Inject
    private ServiceA serviceA;
}

// Use Veld
ServiceB serviceB = Veld.serviceB();
```

### Constructor Injection

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

### Scope Configuration

```java
@Component
public class CacheService {
    // Singleton is default
}

@Component
@Prototype
public class RequestContext {
    // New instance every time
}

@Component
@Lazy
public class HeavyInitialization {
    // Created on first access
}
```

### Conditional Beans

```java
@Component
@ConditionalOnProperty("app.database.enabled")
public class DatabaseService { }

@Component
@ConditionalOnClass(PostgreSQLDriver.class)
public class PostgresService { }
```

### Event Publishing

```java
@Component
public class OrderService {
    @Inject
    private EventBus eventBus;
    
    public void createOrder(Order order) {
        // Process order
        eventBus.publish(new OrderCreatedEvent(order));
    }
}

@Component
public class OrderListener {
    @Subscribe
    public void onOrderCreated(OrderCreatedEvent event) {
        // Handle event
    }
}
```

### AOP Aspect

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("execution(* *..*Service.*(..))")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        System.out.println(pjp.getSignature() + " took " + duration + "ms");
        return result;
    }
}
```

## More Examples

Additional examples can be found in:

- [Core Features Documentation](core-features.md)
- [Annotations Reference](annotations.md)
- [API Reference](api.md)
