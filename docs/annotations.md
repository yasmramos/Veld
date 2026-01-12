# Annotations Reference

Veld provides a comprehensive set of annotations covering all aspects of dependency injection, aspect-oriented programming, resilience patterns, and more.

## Component Registration

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Component` | Marks a class as a managed component | `@Component public class MyService {}` |
| `@Bean` | Declares a factory bean method | `@Bean public DataSource dataSource() {}` |
| `@Factory` | Marks a factory class for bean creation | `@Factory public class ServiceFactory {}` |
| `@Configuration` | Marks a configuration class | `@Configuration public class AppConfig {}` |

## Scopes & Instance Management

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Singleton` | Single instance per application (default) | `@Singleton @Component public class Cache {}` |
| `@Prototype` | New instance on every request | `@Prototype @Component public class Request {}` |
| `@VeldScope` | Custom scope annotation | `@VeldScope("session") public @interface SessionScope {}` |
| `@Lazy` | Deferred initialization on first access | `@Lazy @Component public class HeavyService {}` |

## Qualifiers & Disambiguation

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Named` | Qualify bean by name | `@Named("primary") @Component public class PrimaryDB {}` |
| `@Qualifier` | Custom qualifier annotation | `@Qualifier public @interface Primary {}` |
| `@Primary` | Mark as primary implementation | `@Primary @Component public class PrimaryService {}` |
| `@AliasFor` | Attribute aliasing for meta-annotations | `@AliasFor(annotation = Named.class) String value() default "";` |

## Dependency Injection

| Annotation | Target | Description |
|------------|--------|-------------|
| `@Inject` | Constructor | Constructor injection (recommended) |
| `@Inject` | Field | Field injection (any visibility, including private) |
| `@Inject` | Method | Method/setter injection |
| `@Value` | Field/Parameter | Configuration value injection from properties |
| `@Named` | Parameter/Field | Qualify dependency by name |
| `@Optional` | Field/Parameter | Mark dependency as optional (null if not present) |
| `@Lookup` | Method | Lookup dependency dynamically |

## Lifecycle Callbacks

| Annotation | Description | Example |
|------------|-------------|---------|
| `@PostConstruct` | Called after dependency injection completes | `@PostConstruct public void init() {}` |
| `@PreDestroy` | Called before bean destruction | `@PreDestroy public void cleanup() {}` |
| `@PostInitialize` | Called after all beans are initialized | `@PostInitialize public void postInit() {}` |
| `@OnStart` | Called when application starts | `@OnStart public void onStart() {}` |
| `@OnStop` | Called when application stops | `@OnStop public void onStop() {}` |
| `@DependsOn` | Specify initialization order | `@DependsOn({"database", "cache"}) public class Service {}` |
| `@Order` | Control initialization order | `@Order(1) public class FirstService {}` |

## Conditional Registration

| Annotation | Description | Example |
|------------|-------------|---------|
| `@ConditionalOnProperty` | Register if property matches | `@ConditionalOnProperty("app.cache.enabled")` |
| `@ConditionalOnMissingBean` | Register if no other bean exists | `@ConditionalOnMissingBean(Cache.class)` |
| `@ConditionalOnClass` | Register if class is present on classpath | `@ConditionalOnClass(Database.class)` |
| `@ConditionalOnBean` | Register if specific bean exists | `@ConditionalOnBean(DataSource.class)` |

## Aspect-Oriented Programming (AOP)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Aspect` | Marks a class as an aspect | `@Aspect @Component public class LoggingAspect {}` |
| `@Before` | Execute before method | `@Before("execution(* Service.*(..))")` |
| `@After` | Execute after method (finally) | `@After("execution(* *..*Service.*(..))")` |
| `@Around` | Wrap method execution | `@Around("execution(* *..*Service.*(..))")` |
| `@AroundInvoke` | CDI-style interceptor method | `@AroundInvoke public Object intercept(InvocationContext) {}` |
| `@Pointcut` | Define reusable pointcut | `@Pointcut("execution(* *..*Service.*(..))")` |
| `@Interceptor` | Mark as interceptor class | `@Interceptor @Priority(100) public class AuditInterceptor {}` |
| `@InterceptorBinding` | Custom interceptor binding | `@InterceptorBinding public @interface Audited {}` |

## Resilience & Fault Tolerance (`veld-resilience`)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Retry` | Automatic retry with exponential backoff | `@Retry(maxAttempts = 3, delay = 1000)` |
| `@RateLimiter` | Limit calls per time period | `@RateLimiter(permits = 10, period = 1000)` |
| `@CircuitBreaker` | Prevent cascading failures | `@CircuitBreaker(failureThreshold = 5, waitDuration = 30000)` |
| `@Bulkhead` | Limit concurrent executions | `@Bulkhead(maxConcurrentCalls = 10)` |
| `@Timeout` | Cancel long-running operations | `@Timeout(value = 5000, unit = MILLISECONDS)` |

## Caching (`veld-cache`)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Cacheable` | Cache method result | `@Cacheable(value = "users", key = "#id")` |
| `@CacheEvict` | Remove cache entries | `@CacheEvict(value = "users", allEntries = true)` |
| `@CachePut` | Update cache without checking | `@CachePut(value = "users", key = "#user.id")` |

## Validation (`veld-validation`)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Valid` | Trigger validation on parameter | `public void create(@Valid User user) {}` |
| `@NotNull` | Value must not be null | `@NotNull String name;` |
| `@NotEmpty` | String/Collection must not be empty | `@NotEmpty String username;` |
| `@Size` | Size constraints | `@Size(min = 2, max = 50) String name;` |
| `@Min` | Minimum numeric value | `@Min(18) int age;` |
| `@Max` | Maximum numeric value | `@Max(120) int age;` |
| `@Email` | Valid email format | `@Email String email;` |
| `@Pattern` | Regex pattern match | `@Pattern(regexp = "[A-Za-z0-9_]+") String code;` |
| `@Validated` | Method-level validation | `@Validated public void process(User user) {}` |

## Security (`veld-security`)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Secured` | Enable method security | `@Secured public class AdminService {}` |
| `@RolesAllowed` | Allowed roles | `@RolesAllowed({"ADMIN", "MANAGER"})` |
| `@PermitAll` | Allow all users | `@PermitAll public User getPublicUser() {}` |
| `@DenyAll` | Deny all access | `@DenyAll public void sensitiveOperation() {}` |
| `@PreAuthorize` | Expression-based authorization | `@PreAuthorize("hasRole('ADMIN')")` |

## Metrics (`veld-metrics`)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Timed` | Record execution time | `@Timed("orders.processing")` |
| `@Counted` | Count invocations | `@Counted("emails.sent")` |
| `@Gauge` | Expose value as metric | `@Gauge("queue.size")` |

## Transactions (`veld-tx`)

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Transactional` | Declarative transaction | `@Transactional public void transfer() {}` |

## Async & Scheduling

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Async` | Execute in background thread | `@Async public void sendEmail() {}` |
| `@Scheduled` | Schedule method execution | `@Scheduled(fixedRate = 60000)` |

## Events

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Subscribe` | Subscribe to events | `@Subscribe public void onEvent(Object event) {}` |

## Additional Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Lookup` | Dynamic bean lookup | `@Lookup public UserService userService;` |
| `@AliasFor` | Meta-annotation attribute mapping | `@AliasFor(annotation = Named.class) String value();` |
| `@Order` | Bean ordering | `@Order(1) public class PriorityBean {}` |
| `@Priority` | Interceptor priority | `@Priority(100) public class Interceptor {}` |
