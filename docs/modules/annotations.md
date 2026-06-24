# Veld Annotations

The `veld-annotations` module provides core annotations for the Veld Framework.

## Available Annotations

### @Inject
Injects a dependency into a field, constructor, or method.

```java
@Inject
private MyService myService;
```

### @Singleton
Marks a class as a singleton scope.

```java
@Singleton
public class MyService {
    // ...
}
```

### @Component
Marks a class as a component that can be injected.

```java
@Component
public class MyComponent {
    // ...
}
```

### @Scope
Defines a custom scope for a component.

```java
@Scope(CustomScope.class)
public class MyScopedService {
    // ...
}
```

### @Qualifier
Used to distinguish between multiple implementations of the same type.

```java
@Qualifier("primary")
@Inject
private MyService myService;
```

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-annotations</artifactId>
    <version>1.0.4</version>
</dependency>
```
