# Veld Processor

The `veld-processor` module provides the annotation processor for compile-time code generation in the Veld Framework.

## Features

- Compile-time dependency injection
- Code generation for components and aspects
- Fast build times
- No runtime reflection overhead

## How It Works

The Veld processor runs during Java compilation and generates necessary classes for dependency injection and AOP. It processes annotations like `@Component`, `@Inject`, `@Aspect`, etc., and creates the required boilerplate code.

## Configuration

### Maven

Add the processor as an annotation processor in your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.github.yasmramos</groupId>
                        <artifactId>veld-processor</artifactId>
                        <version>1.0.4</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Gradle

```groovy
dependencies {
    annotationProcessor 'io.github.yasmramos:veld-processor:1.0.4'
}
```

## Generated Code

The processor generates:
- Component factories
- Dependency injection bridges
- Aspect interceptors
- Proxy classes for AOP

## Debugging

To enable debug output from the processor, add the following option to your compiler:

```
-Aveld.debug=true
```

## Common Issues

### Circular Dependencies

Veld detects circular dependencies at compile time and will fail with a clear error message.

### Missing Inject Annotation

Ensure all injectable fields and constructors are properly annotated with `@Inject`.
