# 📋 Veld DI Framework - Annotations Reference

## 🎯 Overview

Veld provides a comprehensive set of annotations for dependency injection, configuration, and lifecycle management. All annotations are processed at compile time for maximum performance.

## 🏗️ Core Annotations

### @Component
Marks a class as a managed component that can be injected into other components.

```java
import io.github.yasmramos.annotations.Component;

@Component
public class UserService {
    
    @Inject
    private UserRepository userRepository;
    
    public User createUser(String name, String email) {
        User user = new User(name, email);
        return userRepository.save(user);
    }
}
```

**Parameters:**
- `value` (optional): Component name for custom identification

```java
@Component("customUserService")
public class CustomUserService {
    // Component with custom name
}
```

### @Inject
Injects dependencies into fields or constructors.

#### Constructor Injection (Recommended)
```java
@Component
public class OrderService {
    
    private final UserRepository userRepo;
    private final PaymentGateway payment;
    
    @Inject
    public OrderService(UserRepository userRepo, PaymentGateway payment) {
        this.userRepo = userRepo;
        this.payment = payment;
    }
}
```

#### Field Injection
```java
@Component
public class EmailService {
    
    @Inject
    private TemplateEngine templateEngine;
    
    @Inject
    private EmailSender emailSender;
}
```

#### Method Injection
```java
@Component
public class ConfigurableService {
    
    private String apiKey;
    private int timeout;
    
    @Inject
    public void configure(@Value("${api.key}") String apiKey,
                         @Value("${api.timeout:5000}") int timeout) {
        this.apiKey = apiKey;
        this.timeout = timeout;
    }
}
```

### @Singleton
Defines component scope as singleton (only one instance per container).

```java
@Component
@Singleton
public class DatabaseService {
    
    private final ConnectionPool pool;
    
    @Inject
    public DatabaseService(ConnectionPool pool) {
        this.pool = pool;
    }
}
```

**Default behavior:** All components are singleton by default if no scope is specified.

### @Prototype
Defines component scope as prototype (new instance each time).

```java
@Component
@Prototype
public class RequestProcessor {
    
    private final String requestId = UUID.randomUUID().toString();
    
    public String getRequestId() {
        return requestId;
    }
}
```

### @Value
Injects configuration values from properties files or environment variables.

#### Basic Usage
```java
@Component
public class AppConfig {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String version;
    
    @Value("${app.debug:false}") // Default value
    private boolean debug;
}
```

#### Complex Types
```java
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
}
```

#### Array and List Injection
```java
@Component
public class FeatureFlags {
    
    @Value("${features.enabled}")
    private String[] enabledFeatures;
    
    @Value("${logging.levels}")
    private List<String> logLevels;
}
```

#### Map Injection
```java
@Component
public class RegionConfig {
    
    @Value("#{systemProperties}")
    private Map<String, String> systemProps;
    
    @Value("#{environment}")
    private Map<String, String> envVars;
}
```

## 🔄 Lifecycle Annotations

### @PostConstruct
Executes method after component initialization and dependency injection.

```java
@Component
public class InitializableService {
    
    private Cache cache;
    
    @Inject
    public InitializableService(Cache cache) {
        this.cache = cache;
    }
    
    @PostConstruct
    public void initialize() {
        cache.warmup();
        System.out.println("Service initialized successfully");
    }
}
```

### @PreDestroy
Executes method before component destruction during container shutdown.

```java
@Component
public class ResourceManager {
    
    private Resource resource;
    
    @Inject
    public ResourceManager(Resource resource) {
        this.resource = resource;
    }
    
    @PreDestroy
    public void cleanup() {
        resource.close();
        System.out.println("Resources cleaned up");
    }
}
```

## 🏷️ Advanced Annotations

### @Profile
Registers component only for specific profiles (development, test, production).

```java
@Component
@Profile("development")
public class DevDatabaseService {
    // Only available in development profile
}

@Component
@Profile("production")
public class ProdDatabaseService {
    // Only available in production profile
}

@Component
@Profile({"test", "development"})
public class TestAndDevService {
    // Available in both test and development
}
```

### @Conditional
Registers component based on custom conditions.

```java
@Component
@Conditional(OnPropertyTrue.class)
public class PropertyBasedService {
    // Registered only when specific property is true
}

@Conditional(OnClassPresent.class)
@Component
public class OptionalDependencyService {
    // Registered only when specific class is present on classpath
}
```

### @Lazy
Delays component initialization until first access.

```java
@Component
@Lazy
public class HeavyInitializationService {
    
    public HeavyInitializationService() {
        // Expensive initialization happens here
        initializeExpensiveResources();
    }
}
```

### @Qualifier
Distinguishes between multiple beans of the same type.

```java
@Component
@Qualifier("primary")
public class PrimaryRepository {
    // Primary implementation
}

@Component
@Qualifier("secondary")
public class SecondaryRepository {
    // Secondary implementation
}

@Component
public class Service {
    
    @Inject
    @Qualifier("primary")
    private Repository primaryRepo;
    
    @Inject
    @Qualifier("secondary")
    private Repository secondaryRepo;
}
```

### @Primary
Marks a bean as primary when multiple beans of same type exist.

```java
@Component
@Primary
public class PrimaryService {
    // This will be injected by default
}

@Component
public class SecondaryService {
    // Alternative implementation
}

@Component
public class Consumer {
    
    @Inject
    private Service service; // Gets PrimaryService
}
```

## 🎯 Custom Annotations

### Creating Custom Annotations

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Inject
public @interface InjectLogger {
    // Custom injection annotation
}
```

### Using Custom Annotations

```java
@Component
public class LoggingService {
    
    @InjectLogger
    private Logger logger; // Injected using custom annotation
    
    public void process(String data) {
        logger.info("Processing data: {}", data);
        // Business logic
    }
}
```

## 📊 Configuration Annotations

### @Configuration
Groups related configuration beans.

```java
@Configuration
public class DatabaseConfiguration {
    
    @Bean
    public DataSource dataSource(@Value("${database.url}") String url) {
        return new HikariDataSource(url);
    }
    
    @Bean
    @Singleton
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### @Bean
Defines a method as a bean producer.

```java
@Configuration
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();
    }
    
    @Bean
    @Singleton
    public Cache<String, Object> objectCache(CacheManager cacheManager) {
        return cacheManager.getCache("objects");
    }
}
```

## 🎨 Aspect-Oriented Programming Annotations

### @Aspect
Marks a class as an aspect.

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        Object[] args = pjp.getArgs();
        
        System.out.println("Executing: " + methodName);
        Object result = pjp.proceed();
        System.out.println("Completed: " + methodName);
        
        return result;
    }
}
```

### @Around
Defines advice that surrounds method execution.

```java
@Aspect
@Component
public class TimingAspect {
    
    @Around("execution(* com.example.repository.*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            System.out.println(pjp.getSignature() + " took " + duration + "ms");
        }
    }
}
```

### @Before
Defines advice that executes before method execution.

```java
@Aspect
@Component
public class ValidationAspect {
    
    @Before("execution(* com.example.service.*.*(..)) && args(..,request)")
    public void validateRequest(JoinPoint jp, Object request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
    }
}
```

### @After
Defines advice that executes after method execution (regardless of outcome).

```java
@Aspect
@Component
public class CleanupAspect {
    
    @After("execution(* com.example.service.*.*(..))")
    public void cleanupAfterExecution(JoinPoint jp) {
        // Cleanup resources
        System.out.println("Cleaned up after: " + jp.getSignature().getName());
    }
}
```

### @AfterReturning
Defines advice that executes after successful method execution.

```java
@Aspect
@Component
public class SuccessLoggingAspect {
    
    @AfterReturning(
        pointcut = "execution(* com.example.repository.*.*(..))",
        returning = "result"
    )
    public void logSuccessfulQuery(JoinPoint jp, Object result) {
        System.out.println("Query succeeded: " + jp.getSignature().getName());
    }
}
```

### @AfterThrowing
Defines advice that executes when method throws an exception.

```java
@Aspect
@Component
public class ErrorHandlingAspect {
    
    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "ex"
    )
    public void handleExceptions(JoinPoint jp, Throwable ex) {
        System.err.println("Exception in " + jp.getSignature().getName() + ": " + ex.getMessage());
        // Send to monitoring system
        monitoringService.reportError(jp.getSignature().getName(), ex);
    }
}
```

## 📝 Annotation Combinations

### Common Patterns

#### Repository Pattern
```java
@Repository
public class UserRepository {
    
    private final Map<String, User> users = new HashMap<>();
    
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }
    
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }
}
```

#### Service Layer Pattern
```java
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final NotificationService notification;
    
    @Inject
    public UserService(UserRepository userRepository, NotificationService notification) {
        this.userRepository = userRepository;
        this.notification = notification;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("UserService initialized");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("UserService shutting down");
    }
}
```

#### Configuration Pattern
```java
@Configuration
public class AppConfiguration {
    
    @Value("${app.features.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Bean
    @Singleton
    @ConditionalOnProperty(name = "app.features.cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return Caffeine.newBuilder().build();
    }
}
```

## 🎯 Best Practices

### 1. Prefer Constructor Injection
```java
// ✅ Good - Testable, clear dependencies
@Component
public class GoodService {
    private final Repository repo;
    
    @Inject
    public GoodService(Repository repo) {
        this.repo = repo;
    }
}

// ❌ Avoid - Harder to test
@Component
public class BadService {
    @Inject
    private Repository repo; // Circular dependency issues
}
```

### 2. Use Immutable Components
```java
@Component
@Immutable
public class Configuration {
    
    private final String apiKey;
    private final int timeout;
    
    @Inject
    public Configuration(@Value("${api.key}") String apiKey,
                        @Value("${api.timeout:5000}") int timeout) {
        this.apiKey = apiKey;
        this.timeout = timeout;
    }
}
```

### 3. Implement Proper Lifecycle
```java
@Component
public class ResourceIntensiveService {
    
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
```

### 4. Use Profiles for Environment-Specific Beans
```java
@Component
@Profile("development")
public class DevDataSource {
    // Development database
}

@Component
@Profile("production")
public class ProdDataSource {
    // Production database
}
```

---

**Veld's annotation system provides a complete, type-safe, and high-performance dependency injection framework for modern Java applications.**