# Veld Framework

**Ultra-fast Dependency Injection for Java - Zero Reflection, Pure Bytecode Generation**

[![Build Status](https://github.com/yasmramos/Veld/actions/workflows/maven.yml/badge.svg)](https://github.com/yasmramos/Veld/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)](https://maven.apache.org/)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/yasmramos/veld)

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

### Resilience & Fault Tolerance (`veld-resilience`)
- **Retry** - `@Retry` automatic retry with exponential backoff
- **Rate Limiting** - `@RateLimiter` to control method call frequency
- **Circuit Breaker** - `@CircuitBreaker` prevents cascading failures
- **Bulkhead** - `@Bulkhead` limits concurrent executions
- **Timeout** - `@Timeout` cancels long-running operations

### Caching (`veld-cache`)
- **Cacheable** - `@Cacheable` caches method results
- **Cache Eviction** - `@CacheEvict` removes cache entries
- **Cache Put** - `@CachePut` updates cache without checking

### Validation (`veld-validation`)
- **Bean Validation** - `@Valid`, `@NotNull`, `@NotEmpty`, `@Size`
- **Numeric Constraints** - `@Min`, `@Max`
- **Pattern Matching** - `@Email`, `@Pattern`

### Security (`veld-security`)
- **Role-Based Access** - `@Secured`, `@RolesAllowed`
- **Method Security** - `@PreAuthorize`, `@PermitAll`, `@DenyAll`

### Metrics (`veld-metrics`)
- **Timing** - `@Timed` records execution duration
- **Counting** - `@Counted` tracks invocations
- **Gauges** - `@Gauge` exposes values as metrics

### Transactions (`veld-tx`)
- **Declarative TX** - `@Transactional` with propagation control
- **Rollback Rules** - Configure rollback for specific exceptions

### Async & Scheduling
- **Async Execution** - `@Async` for background thread execution
- **Scheduled Tasks** - `@Scheduled` with cron expressions, fixed rate/delay
- **Managed Executors** - Named executor pools for resource control

## Quick Start

### 1. Add Dependencies

**Maven:**
```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-runtime</artifactId>
    <version>1.0.3</version>
</dependency>

<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-annotations</artifactId>
    <version>1.0.3</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.yasmramos:veld-runtime:1.0.3'
implementation 'io.github.yasmramos:veld-annotations:1.0.3'
```

**Note:** Veld uses a unified plugin approach that handles everything automatically.

### 2. Maven Plugin Configuration

**The `veld-maven-plugin` is required for Veld to work properly.** This plugin simplifies the build process and provides all necessary features:

**Minimal Configuration (Required):**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.3</version>
        </plugin>
    </plugins>
</build>
```

**The plugin handles everything automatically:**
- Compiles your code with the Veld annotation processor
- Weaves bytecode to add synthetic setters for private field injection
- Generates the optimized `Veld.class` registry

**Advanced Configuration (Optional):**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.3</version>
            <executions>
                <execution>
                    <id>veld-compile</id>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                    <phase>compile</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Benefits of using the unified plugin:**
- **Simplified Configuration** - One plugin replaces multiple Maven configurations
- **Automatic Processing** - Automatically runs annotation processing and bytecode weaving during compile phase
- **IDE Compatibility** - Better integration with modern IDEs
- **JPMS Support** - Enhanced support for Java Module System
- **Build Optimization** - Optimized compilation pipeline

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

### Resilience (NEW in 1.1.0)

| Annotation | Description |
|------------|-------------|
| `@Retry` | Automatic retry with configurable attempts, delay, and exponential backoff |
| `@RateLimiter` | Limit method call frequency with permits per period |

### Async & Scheduling (NEW in 1.1.0)

| Annotation | Description |
|------------|-------------|
| `@Async` | Execute method asynchronously in a background thread |
| `@Scheduled` | Schedule method execution with cron, fixedRate, or fixedDelay |

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

## Resilience Features

Veld 1.1.0 introduces powerful resilience patterns for fault-tolerant applications:

### Retry with Exponential Backoff

```java
@Component
public class ExternalApiClient {
    
    @Retry(maxAttempts = 3, delay = 1000, multiplier = 2.0)
    public Response callApi(Request request) {
        return httpClient.execute(request);
    }
    
    @Retry(maxAttempts = 5, delay = 500, include = {IOException.class, TimeoutException.class})
    public Data fetchData(String id) {
        return remoteService.get(id);
    }
}
```

### Rate Limiting

```java
@Component
public class ApiService {
    
    @RateLimiter(permits = 10, period = 1000)  // 10 calls per second
    public Response callExternalApi(Request request) {
        return httpClient.execute(request);
    }
    
    @RateLimiter(permits = 100, period = 60000, blocking = false)  // 100 calls per minute
    public Data getData(String id) {
        return repository.findById(id);
    }
}
```

### Circuit Breaker

```java
@Component
public class PaymentService {
    
    @CircuitBreaker(failureThreshold = 5, waitDuration = 30000, fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Order order) {
        return paymentGateway.charge(order);
    }
    
    public PaymentResult fallbackPayment(Order order) {
        return PaymentResult.pending("Service temporarily unavailable");
    }
}
```

### Bulkhead

```java
@Component
public class ResourceService {
    
    @Bulkhead(maxConcurrentCalls = 10, maxWaitDuration = 5000)
    public Resource allocateResource(String type) {
        return resourcePool.allocate(type);
    }
}
```

## Caching

```java
@Component
public class ProductService {
    
    @Cacheable(value = "products", ttl = 60000)
    public Product getProduct(Long id) {
        return productRepository.findById(id);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() {
        // Cache cleared after method execution
    }
    
    @CachePut(value = "products")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
}
```

## Validation

```java
public class UserDTO {
    @NotNull
    @Size(min = 2, max = 50)
    private String name;
    
    @Email
    private String email;
    
    @Min(18) @Max(120)
    private int age;
}

@Component
public class UserService {
    public void createUser(@Valid UserDTO user) {
        // Validation happens automatically
        userRepository.save(user);
    }
}
```

## Security

```java
@Component
@Secured
public class AdminService {
    
    @RolesAllowed({"ADMIN", "MANAGER"})
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    @PermitAll
    public List<User> listUsers() {
        return userRepository.findAll();
    }
    
    @DenyAll
    public void dangerousOperation() {
        // Never allowed
    }
}

// Set security context
SecurityContext.setPrincipal(new Principal("admin", Set.of("ADMIN")));
```

## Metrics

```java
@Component
public class OrderService {
    
    @Timed("orders.processing")
    public Order processOrder(Order order) {
        // Execution time recorded
        return orderProcessor.process(order);
    }
    
    @Counted("orders.created")
    public Order createOrder(OrderRequest request) {
        return orderFactory.create(request);
    }
}

// Access metrics
Map<String, Object> metrics = MetricsRegistry.getAllMetrics();
```

## Transactions

```java
@Component
public class TransferService {
    
    @Transactional
    public void transfer(Account from, Account to, BigDecimal amount) {
        from.debit(amount);
        to.credit(amount);
        // Commits on success, rolls back on exception
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   rollbackFor = {BusinessException.class})
    public void auditTransfer(Transfer transfer) {
        auditLog.record(transfer);
    }
}
```

## Async Execution

Execute methods asynchronously without blocking the caller:

```java
@Component
public class EmailService {
    
    @Async
    public void sendEmail(String to, String subject, String body) {
        // Runs in background thread
        emailClient.send(to, subject, body);
    }
    
    @Async
    public CompletableFuture<Boolean> sendEmailWithResult(String to, String subject) {
        boolean sent = emailClient.send(to, subject, "Hello");
        return CompletableFuture.completedFuture(sent);
    }
}
```

## Scheduled Tasks

Schedule methods to run periodically or at specific times:

```java
@Component
public class CleanupService {
    
    @Scheduled(fixedRate = 60000)  // Every minute
    public void cleanupTempFiles() {
        fileService.deleteOldTempFiles();
    }
    
    @Scheduled(cron = "0 0 2 * * ?")  // Daily at 2 AM
    public void dailyBackup() {
        backupService.performBackup();
    }
    
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void processQueue() {
        // First run after 10s, then 5s after each completion
        queueProcessor.processNext();
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
| `veld-resilience` | Circuit Breaker, Bulkhead, Timeout patterns |
| `veld-cache` | Caching with `@Cacheable`, `@CacheEvict` |
| `veld-validation` | Bean validation annotations |
| `veld-security` | Method-level security |
| `veld-metrics` | Timing, counting, and gauges |
| `veld-tx` | Declarative transaction management |
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

- **Java 17+** (tested up to Java 21)
- **Maven 3.6+**

## Performance Benchmarks

**JMH Benchmark Results** (OpenJDK 17, December 2025)

### Throughput (ops/ms - higher is better)

| Benchmark | Veld | Dagger | Guice | Spring |
|-----------|------|--------|-------|--------|
| Single thread | **3,118,161** | 879,584 | 18,479 | 26,080 |
| Concurrent (4 threads) | **5,915,896** | 1,749,664 | 24,191 | 50,438 |

**Veld is 3.5x faster than Dagger and 119x faster than Spring in throughput tests.**

### Injection Latency (ns/op - lower is better)

| Benchmark | Veld | Dagger | Guice | Spring |
|-----------|------|--------|-------|--------|
| Simple Injection | **0.320** | 1.128 | 52.53 | 38.10 |
| Complex Injection | **0.325** | 1.106 | 54.61 | 52.21 |
| Logger Lookup | **0.320** | 1.095 | 53.86 | 51.99 |

**Veld achieves sub-nanosecond latency - 3.5x faster than Dagger, 119x faster than Spring.**

### Prototype Creation (ns/op - lower is better)

| Benchmark | Veld | Dagger | Guice | Spring |
|-----------|------|--------|-------|--------|
| Simple | **2.15** | 8.84 | 76.84 | 3,206 |
| Complex | - | 2,954 | 3,120 | 9,797 |

### Startup Time (us/op - lower is better)

| Framework | Time |
|-----------|------|
| **Veld** | **≈0.001** |
| Dagger | 0.109 |
| Guice | 101.2 |
| Spring | 1,207 |

**Veld starts 109x faster than Dagger and 1,207,000x faster than Spring.**

### Memory Benchmark (ms to create N beans - lower is better)

| Beans | Veld | Dagger | Guice | Spring |
|-------|------|--------|-------|--------|
| 10 | **0.005** | 0.026 | 13.5 | 56.3 |
| 100 | **0.036** | 0.133 | 59.3 | 223.4 |
| 500 | **0.024** | 0.115 | 117.3 | 636.7 |

```
┌──────────────────────────────────────────────────────────────────┐
│                    PERFORMANCE SUMMARY                            │
├──────────────────────────────────────────────────────────────────┤
│  Veld vs Dagger (compile-time DI):                               │
│    • 3.5x faster throughput                                      │
│    • 3.5x lower latency                                          │
│    • 109x faster startup                                         │
│                                                                   │
│  Veld vs Spring (reflection-based):                              │
│    • 119x faster throughput                                      │
│    • 119x lower latency                                          │
│    • 1,207,000x faster startup                                   │
│                                                                   │
│  Key metrics:                                                     │
│    • 0.32 ns average injection latency                           │
│    • 5.9 billion ops/sec concurrent throughput                   │
│    • Lock-free singleton access (Holder idiom)                   │
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
