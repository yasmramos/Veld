# 💡 Veld DI Framework - Practical Examples

This document provides practical, real-world examples of using Veld DI Framework in various scenarios.

---

## 🏗️ Basic Examples

### 1. Simple Dependency Injection

```java
// Repository layer
@Component
@Singleton
public class UserRepository {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }
    
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}

// Service layer
@Component
public class UserService {
    
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    @Inject
    public UserService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }
    
    public User createUser(String name, String email) {
        User user = new User(name, email);
        User saved = userRepository.save(user);
        
        // Notify about new user
        notificationService.sendWelcomeEmail(saved);
        
        return saved;
    }
    
    public Optional<User> getUser(String id) {
        return userRepository.findById(id);
    }
}

// Notification service
@Component
@Singleton
public class NotificationService {
    
    private final EmailService emailService;
    
    @Inject
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to our platform!";
        String body = "Hello " + user.getName() + ", welcome!";
        emailService.sendEmail(user.getEmail(), subject, body);
    }
}

// Application
@Component
public class Application {
    
    @Inject
    private UserService userService;
    
    public void run() {
        User user = userService.createUser("John Doe", "john@example.com");
        System.out.println("Created user: " + user);
        
        Optional<User> found = userService.getUser(user.getId());
        System.out.println("Found user: " + found.orElse(null));
    }
    
    public static void main(String[] args) {
        Veld.start();
        
        Application app = Veld.getBean(Application.class);
        app.run();
        
        Veld.shutdown();
    }
}
```

---

## ⚙️ Configuration Examples

### 2. External Configuration with @Value

```java
// Configuration class
@Component
public class DatabaseConfig {
    
    @Value("${database.url}")
    private String url;
    
    @Value("${database.username}")
    private String username;
    
    @Value("${database.password}")
    private String password;
    
    @Value("${database.pool.size:10}")
    private int poolSize;
    
    @Value("${database.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${database.timeout:30000}")
    private Duration timeout;
    
    @PostConstruct
    public void validate() {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL is required");
        }
        if (poolSize <= 0 || poolSize > 50) {
            throw new IllegalArgumentException("Pool size must be between 1 and 50");
        }
    }
    
    public HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setConnectionTimeout(timeout.toMillis());
        config.setIdleTimeout(Duration.ofMinutes(10).toMillis());
        config.setLeakDetectionThreshold(Duration.ofSeconds(60).toMillis());
        
        Properties props = new Properties();
        props.setProperty("ssl", sslEnabled ? "require" : "disable");
        config.setDataSourceProperties(props);
        
        return config;
    }
}

// Service using configuration
@Component
@Singleton
public class DatabaseService {
    
    private final DataSource dataSource;
    
    @Inject
    public DatabaseService(DatabaseConfig config) {
        HikariConfig hikariConfig = config.createHikariConfig();
        this.dataSource = new HikariDataSource(hikariConfig);
    }
    
    public void executeQuery(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            // Process results
            while (rs.next()) {
                // Handle result set
            }
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }
}
```

**application.properties:**
```properties
# Database Configuration
database.url=jdbc:postgresql://localhost:5432/myapp
database.username=myuser
database.password=mypassword
database.pool.size=20
database.ssl.enabled=true
database.timeout=45000
```

---

## 🔄 Lifecycle Management Examples

### 3. Lifecycle Callbacks

```java
@Component
@Singleton
public class CacheManager {
    
    private final Cache<String, Object> cache;
    private final ScheduledExecutorService scheduler;
    
    @Inject
    public CacheManager(@Value("${cache.max.size:1000}") int maxSize,
                       @Value("${cache.expire.minutes:30}") int expireMinutes) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(Duration.ofMinutes(expireMinutes))
            .recordStats()
            .build();
            
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    @PostConstruct
    public void initialize() {
        System.out.println("Cache Manager initialized");
        
        // Start background tasks
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 
            5, 5, TimeUnit.MINUTES);
            
        scheduler.scheduleAtFixedRate(this::logStats, 
            1, 1, TimeUnit.MINUTES);
    }
    
    @PreDestroy
    public void shutdown() {
        System.out.println("Cache Manager shutting down");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Cleanup cache
        cache.cleanUp();
    }
    
    private void cleanupExpiredEntries() {
        cache.cleanUp();
    }
    
    private void logStats() {
        CacheStats stats = cache.stats();
        System.out.println("Cache stats: hitRate=" + stats.hitRate() + 
                         ", size=" + cache.estimatedSize());
    }
    
    public <T> T get(String key, Class<T> type) {
        return type.cast(cache.getIfPresent(key));
    }
    
    public void put(String key, Object value) {
        cache.put(key, value);
    }
}
```

---

## 🧵 Thread Safety Examples

### 4. Concurrent Access Patterns

```java
@Component
@ThreadSafe
public class CounterService {
    
    private final AtomicLong counter = new AtomicLong(0);
    private final AtomicReference<Map<String, Long>> metrics = 
        new AtomicReference<>(new ConcurrentHashMap<>());
    
    @Inject
    private CacheService cacheService;
    
    public long incrementAndGet() {
        return counter.incrementAndGet();
    }
    
    public long decrementAndGet() {
        return counter.decrementAndGet();
    }
    
    public long get() {
        return counter.get();
    }
    
    public void recordMetric(String name, long value) {
        metrics.updateAndGet(map -> {
            map.put(name, map.getOrDefault(name, 0L) + value);
            return map;
        });
    }
    
    public Map<String, Long> getMetrics() {
        return new HashMap<>(metrics.get());
    }
}

// Usage in concurrent environment
@Component
public class ConcurrentProcessor {
    
    @Inject
    private CounterService counterService;
    
    public void processBatch(List<String> items) {
        items.parallelStream().forEach(item -> {
            // Increment counter for each item
            long current = counterService.incrementAndGet();
            
            // Process item
            processItem(item);
            
            // Record metric
            counterService.recordMetric("processed_items", 1);
            
            if (current % 1000 == 0) {
                System.out.println("Processed " + current + " items");
            }
        });
    }
    
    private void processItem(String item) {
        // Business logic here
    }
}
```

---

## 🎯 Factory Pattern Examples

### 5. Factory Pattern Implementation

```java
// Product interface
public interface Message {
    void send();
}

// Concrete products
@Component
@Qualifier("email")
public class EmailMessage implements Message {
    
    private final String to;
    private final String subject;
    private final String body;
    
    public EmailMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
    
    @Override
    public void send() {
        System.out.println("Sending email to " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
    }
}

@Component
@Qualifier("sms")
public class SMSMessage implements Message {
    
    private final String to;
    private final String message;
    
    public SMSMessage(String to, String message) {
        this.to = to;
        this.message = message;
    }
    
    @Override
    public void send() {
        System.out.println("Sending SMS to " + to);
        System.out.println("Message: " + message);
    }
}

// Factory
@Component
public class MessageFactory {
    
    @Inject
    private EmailService emailService;
    
    @Inject
    private SMSService smsService;
    
    public Message createEmail(String to, String subject, String body) {
        return new EmailMessage(to, subject, body);
    }
    
    public Message createSMS(String to, String message) {
        return new SMSMessage(to, message);
    }
    
    public Message createMessage(MessageType type, String... params) {
        return switch (type) {
            case EMAIL -> createEmail(params[0], params[1], params[2]);
            case SMS -> createSMS(params[0], params[1]);
            default -> throw new IllegalArgumentException("Unknown message type: " + type);
        };
    }
}

// Usage
@Component
public class NotificationService {
    
    @Inject
    private MessageFactory messageFactory;
    
    public void sendWelcomeMessage(String userEmail, String userPhone) {
        // Create email
        Message welcomeEmail = messageFactory.createMessage(
            MessageType.EMAIL, 
            userEmail, 
            "Welcome!", 
            "Welcome to our platform!"
        );
        
        // Create SMS
        Message welcomeSMS = messageFactory.createMessage(
            MessageType.SMS,
            userPhone,
            "Welcome to our platform!"
        );
        
        // Send both
        welcomeEmail.send();
        welcomeSMS.send();
    }
}

enum MessageType {
    EMAIL, SMS
}
```

---

## 🔔 Event System Examples

### 6. Event-Driven Architecture

```java
// Event classes
public abstract class DomainEvent {
    private final String eventId;
    private final Instant timestamp;
    
    public DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }
    
    public String getEventId() { return eventId; }
    public Instant getTimestamp() { return timestamp; }
}

public class UserCreatedEvent extends DomainEvent {
    private final User user;
    
    public UserCreatedEvent(User user) {
        this.user = user;
    }
    
    public User getUser() { return user; }
}

public class OrderPlacedEvent extends DomainEvent {
    private final Order order;
    
    public OrderPlacedEvent(Order order) {
        this.order = order;
    }
    
    public Order getOrder() { return order; }
}

// Event publisher
@Component
public class EventPublisher {
    
    @Inject
    private EventBus eventBus;
    
    public void publish(DomainEvent event) {
        eventBus.publish(event);
    }
    
    public void publishAsync(DomainEvent event) {
        eventBus.publishAsync(event);
    }
}

// Event handlers
@Component
public class EmailNotificationHandler {
    
    @Inject
    private EmailService emailService;
    
    @EventHandler
    public void onUserCreated(UserCreatedEvent event) {
        User user = event.getUser();
        String subject = "Welcome " + user.getName();
        String body = "Thank you for joining our platform!";
        emailService.sendEmail(user.getEmail(), subject, body);
    }
    
    @EventHandler
    public void onOrderPlaced(OrderPlacedEvent event) {
        Order order = event.getOrder();
        String subject = "Order Confirmation #" + order.getId();
        String body = "Your order has been placed successfully.";
        emailService.sendEmail(order.getCustomerEmail(), subject, body);
    }
}

@Component
public class AnalyticsHandler {
    
    @Inject
    private AnalyticsService analyticsService;
    
    @EventHandler
    public void onUserCreated(UserCreatedEvent event) {
        analyticsService.trackUserRegistration(event.getUser());
    }
    
    @EventHandler
    public void onOrderPlaced(OrderPlacedEvent event) {
        analyticsService.trackOrderPlacement(event.getOrder());
    }
}

@Component
public class InventoryHandler {
    
    @Inject
    private InventoryService inventoryService;
    
    @EventHandler
    public void onOrderPlaced(OrderPlacedEvent event) {
        Order order = event.getOrder();
        inventoryService.reserveItems(order.getItems());
    }
}

// Using the event system
@Component
public class UserService {
    
    @Inject
    private UserRepository userRepository;
    @Inject
    private EventPublisher eventPublisher;
    
    public User createUser(String name, String email) {
        User user = new User(name, email);
        User saved = userRepository.save(user);
        
        // Publish event
        eventPublisher.publish(new UserCreatedEvent(saved));
        
        return saved;
    }
}

@Component
public class OrderService {
    
    @Inject
    private OrderRepository orderRepository;
    @Inject
    private EventPublisher eventPublisher;
    
    public Order placeOrder(List<OrderItem> items, String customerEmail) {
        Order order = new Order(items, customerEmail);
        Order saved = orderRepository.save(order);
        
        // Publish event
        eventPublisher.publishAsync(new OrderPlacedEvent(saved));
        
        return saved;
    }
}
```

---

## 🧪 Testing Examples

### 7. Unit Testing with Veld

```java
// Test configuration
@Configuration
public class TestConfig {
    
    @Bean
    @Primary
    public UserRepository testUserRepository() {
        return new InMemoryUserRepository();
    }
    
    @Bean
    @Primary
    public EmailService testEmailService() {
        return new MockEmailService();
    }
}

// Test service
@Component
public class UserServiceTest {
    
    @Inject
    private UserService userService;
    @Inject
    private MockEmailService emailService;
    
    @Test
    public void testCreateUser() {
        // Given
        String name = "Test User";
        String email = "test@example.com";
        
        // When
        User user = userService.createUser(name, email);
        
        // Then
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(emailService.getSentEmails()).hasSize(1);
        assertThat(emailService.getSentEmails().get(0).getTo()).isEqualTo(email);
    }
}

// Mock implementations
@Component
@Primary
public class MockEmailService implements EmailService {
    
    private final List<EmailMessage> sentEmails = new ArrayList<>();
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        sentEmails.add(new EmailMessage(to, subject, body));
    }
    
    public List<EmailMessage> getSentEmails() {
        return new ArrayList<>(sentEmails);
    }
}

@Component
@Primary
public class InMemoryUserRepository implements UserRepository {
    
    private final Map<String, User> users = new HashMap<>();
    
    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }
    
    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }
}
```

---

## 🌟 Integration Examples

### 8. Spring Boot Integration

```java
@SpringBootApplication
public class VeldSpringBootApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VeldSpringBootApplication.class, args);
    }
}

// Hybrid service using both Veld and Spring
@Component
public class HybridService {
    
    @Inject
    private VeldComponent veldComponent; // From Veld
    
    @Autowired
    private SpringComponent springComponent; // From Spring
    
    public void doComplexWork() {
        // Use Veld component for ultra-fast operations
        Object veldResult = veldComponent.processFast();
        
        // Use Spring component for enterprise features
        Object springResult = springComponent.processWithTransaction();
        
        // Combine results
        return combineResults(veldResult, springResult);
    }
}

// Veld component
@Component
public class VeldComponent {
    
    @Inject
    private FastCache cache;
    
    public Object processFast() {
        // Ultra-fast processing
        return cache.get("fast-data");
    }
}

// Spring component
@Component
public class SpringComponent {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Transactional
    public Object processWithTransaction() {
        // Enterprise-grade processing with transactions
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }
}
```

---

## 🚀 Performance Examples

### 9. High-Performance Caching

```java
@Component
@Singleton
public class PerformanceService {
    
    @Inject
    private Cache<String, ExpensiveResult> cache;
    
    public ExpensiveResult computeExpensiveOperation(String key, Supplier<ExpensiveResult> computation) {
        // Check cache first
        ExpensiveResult cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        
        // Compute if not cached
        ExpensiveResult result = computation.get();
        cache.put(key, result);
        
        return result;
    }
    
    public List<ExpensiveResult> processBatch(List<String> keys) {
        return keys.parallelStream()
            .map(key -> computeExpensiveOperation(key, () -> expensiveComputation(key)))
            .collect(Collectors.toList());
    }
    
    private ExpensiveResult expensiveComputation(String key) {
        // Simulate expensive computation
        try {
            Thread.sleep(100); // Simulate I/O or heavy computation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Computation interrupted", e);
        }
        return new ExpensiveResult(key, System.currentTimeMillis());
    }
}

// Usage example
@Component
public class BatchProcessor {
    
    @Inject
    private PerformanceService performanceService;
    
    public void processLargeDataset() {
        List<String> dataKeys = generateLargeDataset();
        
        // Process with caching - should be much faster after first run
        List<ExpensiveResult> results = performanceService.processBatch(dataKeys);
        
        System.out.println("Processed " + results.size() + " items");
    }
}
```

---

## 💡 Best Practices

### 10. Design Patterns and Best Practices

```java
// ✅ Good: Constructor injection with final fields
@Component
public class GoodService {
    
    private final UserRepository userRepository;
    private final NotificationService notification;
    
    @Inject
    public GoodService(UserRepository userRepository, NotificationService notification) {
        this.userRepository = userRepository;
        this.notification = notification;
    }
    
    // Business methods...
}

// ❌ Avoid: Field injection (harder to test)
@Component
public class BadService {
    
    @Inject
    private UserRepository userRepository; // Circular dependency risk
}

// ✅ Good: Immutable configuration
@Component
@Immutable
public class AppConfig {
    
    private final String apiKey;
    private final int timeout;
    private final boolean debugMode;
    
    @Inject
    public AppConfig(@Value("${api.key}") String apiKey,
                    @Value("${api.timeout:5000}") int timeout,
                    @Value("${api.debug:false}") boolean debugMode) {
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.debugMode = debugMode;
    }
}

// ✅ Good: Proper lifecycle management
@Component
public class ResourceManager {
    
    private ExpensiveResource resource;
    
    @PostConstruct
    public void initialize() {
        this.resource = ExpensiveResource.create();
    }
    
    @PreDestroy
    public void cleanup() {
        if (resource != null) {
            resource.close();
        }
    }
}

// ✅ Good: Use profiles for environment-specific beans
@Component
@Profile("development")
public class DevDataSource {
    // Development database configuration
}

@Component
@Profile("production")
public class ProdDataSource {
    // Production database configuration
}
```

---

**These examples demonstrate Veld's flexibility, performance, and ease of use in real-world scenarios. Each example showcases different aspects of the framework while maintaining the ultra-fast performance characteristics.**