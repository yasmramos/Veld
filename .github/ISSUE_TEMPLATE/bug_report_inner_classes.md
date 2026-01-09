# Bug: Annotation Processor fails with inner classes

## Description

The Veld annotation processor fails when processing classes that contain static inner classes annotated with `@Component`. This causes a compilation error where the processor tries to generate factory classes in a directory named after the outer class, which conflicts when the outer class is not a package.

## Steps to Reproduce

1. Create a class with static inner classes annotated with `@Component`:

```java
package io.github.yasmramos.veld.example;

public class ComplexApplicationExample {
    
    @Singleton
    @Component
    public static class OrderService {
        // ...
    }
    
    @Singleton
    @Component
    public static class PaymentService {
        // ...
    }
    
    // More inner component classes...
}
```

2. Compile the project with the annotation processor enabled

3. Observe the compilation error:

```
[ERROR] /path/to/ComplexApplicationExample.java:[30,8] class io.github.yasmramos.veld.example.ComplexApplicationExample clashes with package of same name

[ERROR] /path/to/target/generated-sources/annotations/io/github/yasmramos/veld/example/ComplexApplicationExample/veld/OrderService$$VeldFactory.java:[1,1] package io.github.yasmramos.veld.example.ComplexApplicationExample clashes with class of same name
```

## Expected Behavior

The annotation processor should correctly generate factory classes for inner classes without causing package/class name collisions.

## Actual Behavior

The processor attempts to create files in a directory structure like:
```
io/github/yasmramos/veld/example/ComplexApplicationExample/veld/OrderService$$VeldFactory.java
```

This fails because `ComplexApplicationExample` is a class, not a package, causing a name collision.

## Workaround

The current workaround is to extract all inner `@Component` classes to separate files, as done in commit `d71d050`.

## Environment

- Veld Framework version: Latest (develop branch)
- Java version: [Specify version]
- Build tool: Maven

## Priority

High - This breaks compilation for valid use cases involving inner classes.

## Labels

- bug
- high-priority
- annotation-processor

## Related

- Workaround commit: `d71d050`
