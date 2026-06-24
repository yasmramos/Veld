# Veld SPI

The `veld-spi` module provides the Service Provider Interface (SPI) for the Veld Framework.

## Overview

The SPI allows third-party extensions and custom implementations to integrate with Veld. It defines contracts for:

- Custom scopes
- Dependency resolvers
- Bean factories
- Proxy providers
- Aspect handlers

## Available Interfaces

### ScopeProvider

Define custom scopes:

```java
public interface ScopeProvider {
    String getName();
    <T> T get(Context context, String name, Class<T> type, Provider<T> provider);
    void clear();
}
```

### DependencyResolver

Customize dependency resolution:

```java
public interface DependencyResolver {
    <T> T resolve(Class<T> type, Qualifier qualifier);
    <T> List<T> resolveAll(Class<T> type);
    boolean canResolve(Class<?> type);
}
```

### BeanFactory

Create custom bean factories:

```java
public interface BeanFactory {
    <T> T createBean(Class<T> type, Context context);
    void destroyBean(Object bean);
}
```

### ProxyProvider

Provide custom proxy implementations:

```java
public interface ProxyProvider {
    <T> T createProxy(Class<T> type, Object target, List<Interceptor> interceptors);
}
```

## Implementing a Custom Scope

Example: Request scope implementation

```java
@ScopeId("request")
public class RequestScope implements ScopeProvider {
    
    private final ThreadLocal<Map<String, Object>> storage = 
        ThreadLocal.withInitial(HashMap::new);
    
    @Override
    public String getName() {
        return "request";
    }
    
    @Override
    public <T> T get(Context context, String name, Class<T> type, Provider<T> provider) {
        Map<String, Object> map = storage.get();
        return (T) map.computeIfAbsent(name, k -> provider.get());
    }
    
    @Override
    public void clear() {
        storage.remove();
    }
}
```

## Registering Extensions

Create a service file in `META-INF/services`:

```
# META-INF/services/io.github.yasmramos.veld.spi.ScopeProvider
com.example.RequestScope
```

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-spi</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Best Practices

1. **Thread Safety**: Ensure your implementations are thread-safe if needed.
2. **Performance**: Minimize overhead in frequently called methods.
3. **Error Handling**: Provide clear error messages for failures.
4. **Documentation**: Document behavior and requirements clearly.

## Testing

Test your implementations with the Veld test utilities:

```java
@Test
public void testCustomScope() {
    ScopeProvider scope = new RequestScope();
    // Test scope behavior
}
```
