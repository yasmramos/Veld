# Veld AOP

The `veld-aop` module provides Aspect-Oriented Programming (AOP) support for the Veld Framework.

## Features

- Compile-time weaving
- Support for method interception
- Async execution support
- Custom aspect creation

## Available Annotations

### @Aspect
Marks a class as an aspect that can intercept method calls.

```java
@Aspect
public class LoggingAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore() {
        System.out.println("Method called");
    }
}
```

### @Before
Executes code before a matched method.

### @After
Executes code after a matched method.

### @Around
Wraps around a matched method, allowing control over its execution.

```java
@Around("execution(* com.example.service.*.*(..))")
public Object around(ProceedingJoinPoint pjp) throws Throwable {
    System.out.println("Before");
    Object result = pjp.proceed();
    System.out.println("After");
    return result;
}
```

### @Async
Marks a method to be executed asynchronously.

```java
@Async
public CompletableFuture<String> asyncMethod() {
    return CompletableFuture.completedFuture("result");
}
```

## Usage

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-aop</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Configuration

Configure aspects in your Veld configuration:

```java
VeldConfig config = VeldConfig.builder()
    .addAspect(new LoggingAspect())
    .build();
```
