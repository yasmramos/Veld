# Quick Start Guide

Get started with the Veld Framework in minutes.

## Prerequisites

- Java 17 or higher
- Maven 3.8+ or Gradle 7.0+

## Step 1: Add Dependencies

### Maven

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Core annotations -->
    <dependency>
        <groupId>io.github.yasmramos</groupId>
        <artifactId>veld-annotations</artifactId>
        <version>1.0.4</version>
    </dependency>
    
    <!-- AOP support (optional) -->
    <dependency>
        <groupId>io.github.yasmramos</groupId>
        <artifactId>veld-aop</artifactId>
        <version>1.0.4</version>
    </dependency>
</dependencies>

<!-- Build configuration -->
<build>
    <plugins>
        <!-- Veld Maven Plugin -->
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.4</version>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        
        <!-- Compiler plugin with annotation processor -->
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
    implementation 'io.github.yasmramos:veld-annotations:1.0.4'
    implementation 'io.github.yasmramos:veld-aop:1.0.4'
    annotationProcessor 'io.github.yasmramos:veld-processor:1.0.4'
}
```

## Step 2: Create Your First Component

```java
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

@Component
public class GreetingService {
    
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
```

## Step 3: Inject Dependencies

```java
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

@Component
public class Application {
    
    private final GreetingService greetingService;
    
    @Inject
    public Application(GreetingService greetingService) {
        this.greetingService = greetingService;
    }
    
    public void run() {
        System.out.println(greetingService.greet("World"));
    }
}
```

## Step 4: Add AOP (Optional)

Create an aspect:

```java
import io.github.yasmramos.veld.aop.annotation.Aspect;
import io.github.yasmramos.veld.aop.annotation.Before;

@Aspect
public class LoggingAspect {
    
    @Before("execution(* com.example..*.*(..))")
    public void logMethodCall() {
        System.out.println("Method called");
    }
}
```

## Step 5: Build and Run

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.Application"
```

## Next Steps

- Learn about [scopes](./modules/annotations.md#scope)
- Explore [AOP features](./modules/aop.md)
- Configure [security](./modules/security.md)
- Create [custom extensions](./modules/spi.md)

## Troubleshooting

### Compilation Errors

Ensure the Veld Maven Plugin is configured correctly and runs during compilation.

### Injection Failures

Check that all components are properly annotated and there are no circular dependencies.

### AOP Not Working

Verify that aspects are in the classpath and pointcut expressions are correct.
