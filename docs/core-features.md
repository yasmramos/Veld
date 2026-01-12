# Core Features

## Injection Patterns

### Constructor Injection (Recommended)

Constructor injection is the recommended pattern for most use cases. It ensures dependencies are clearly defined and makes testing easier.

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

Works with any visibility (private, protected, package, public) across packages. Veld uses bytecode weaving to inject into private fields.

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

Setter-based injection for optional or late-binding dependencies.

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

When multiple implementations exist, use `@Named` to disambiguate.

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

Use `Provider<T>` for lazy instance creation or when you need multiple instances.

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

Inject configuration properties from your application configuration.

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

Veld includes a built-in EventBus for decoupled component communication. Components can publish events without knowing about their subscribers.

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

Veld provides powerful resilience patterns for fault-tolerant applications through the `veld-resilience` module.

### Retry with Exponential Backoff

Automatically retry failed operations with configurable backoff.

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

Control the rate at which methods can be called.

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

Prevent cascading failures by stopping requests to failing services.

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

Limit the number of concurrent executions to isolate failures.

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

Cache method results to improve performance.

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

Validate input parameters using Bean Validation annotations.

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

Method-level security with role-based access control.

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

Record execution metrics for monitoring.

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

Declarative transaction management.

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

Execute methods asynchronously without blocking the caller.

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

Schedule methods to run periodically or at specific times.

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

## Lifecycle Callbacks

Execute code at specific points in the component lifecycle.

```java
@Component
public class DataService {
    private DatabaseConnection connection;
    
    @PostConstruct
    public void init() {
        connection = DatabaseConnection.connect();
        connection.start();
    }
    
    @PreDestroy
    public void cleanup() {
        connection.close();
    }
    
    @OnStart
    public void onApplicationStart() {
        // Called when application starts
    }
    
    @OnStop
    public void onApplicationStop() {
        // Called when application stops
    }
}
```
