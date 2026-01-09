# Veld Guides

## Getting Started

### 1. Add Dependencies

```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-annotations</artifactId>
    <version>1.0.3</version>
</dependency>
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-runtime</artifactId>
    <version>1.0.3</version>
</dependency>
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-processor</artifactId>
    <version>1.0.3</version>
    <scope>provided</scope>
</dependency>
```

### 2. Create Components

```java
@Component
public class MyService {
    @Inject
    private OtherService otherService;

    @PostConstruct
    public void init() {
        // Initialization logic
    }

    @PreDestroy
    public void cleanup() {
        // Cleanup logic
    }
}
```

### 3. Start the Container

```java
Veld veld = Veld.start();
MyService service = veld.getBean(MyService.class);
```

## Available Guides

- [Dependency Injection](dependency-injection.md)
- [Scope Management](scopes.md)
- [Lifecycle Callbacks](lifecycle.md)
- [Events](events.md)
- [AOP](aop.md)
- [Spring Boot Integration](spring-boot.md)
