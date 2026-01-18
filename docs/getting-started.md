# Getting Started

This guide will help you get started with Veld in your project. Follow these steps to set up Veld and create your first dependency injection configuration.

## Requirements

- **Java 17+** (tested up to Java 21)
- **Maven 3.6+** or Gradle 7+

## Installation

### Maven

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Core Veld dependencies -->
    <dependency>
        <groupId>io.github.yasmramos</groupId>
        <artifactId>veld-runtime</artifactId>
        <version>1.0.3</version>
    </dependency>
    
    <dependency>
        <groupId>io.github.yasmramos</groupId>
        <artifactId>veld-annotations</artifactId>
        <version>1.0.3</version>
    </dependency>
</dependencies>
```

Add the Maven plugin for compile-time processing:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.yasmramos</groupId>
            <artifactId>veld-maven-plugin</artifactId>
            <version>1.0.3</version>
        </plugin>
    </plugins>
</build>
```

### Gradle

Add dependencies to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.github.yasmramos:veld-runtime:1.0.3'
    implementation 'io.github.yasmramos:veld-annotations:1.0.3'
}
```

Apply the Veld plugin:

```groovy
plugins {
    id 'io.github.yasmramos.veld' version '1.0.3'
}
```

## Your First Application

### 1. Create a Simple Component

```java
package com.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

@Component
public class GreeterService {
    
    @Inject
    private MessageProvider messageProvider;
    
    public String greet(String name) {
        return messageProvider.getMessage() + ", " + name + "!";
    }
}
```

### 2. Create a Dependency

```java
package com.example;

import io.github.yasmramos.veld.annotation.Component;

@Component
public class MessageProvider {
    
    public String getMessage() {
        return "Hello";
    }
}
```

### 3. Use Veld to Get Components

```java
package com.example;

import io.github.yasmramos.veld.Veld;

public class Application {
    public static void main(String[] args) {
        // Get a component
        GreeterService greeter = Veld.greeterService();
        
        // Use the component
        String greeting = greeter.greet("World");
        System.out.println(greeting);  // Output: Hello, World!
    }
}
```

## Dependency Injection Patterns

### Constructor Injection (Recommended)

Constructor injection is the recommended pattern as it makes dependencies explicit and facilitates testing.

```java
@Component
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    @Inject
    public OrderService(PaymentService paymentService, 
                        InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```

### Field Injection

Veld supports field injection for private fields across packages through bytecode weaving.

```java
@Component
public class UserService {
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private EmailService emailService;
}
```

### Method Injection

Setter-based injection for optional or late-binding dependencies.

```java
@Component
public class ConfigurationService {
    private ConfigLoader configLoader;
    
    @Inject
    public void setConfigLoader(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }
}
```

## Scopes

### Singleton (Default)

All components are singleton by default - one instance per application.

```java
@Component
public class CacheService {
    // Singleton by default
}
```

### Prototype

Create a new instance every time the component is requested.

```java
@Component
@Prototype
public class RequestContext {
    // New instance each time
}
```

### Lazy Initialization

Defer creation until first access.

```java
@Component
@Lazy
public class HeavyService {
    // Created on first use
}
```

## Qualifiers

### Named Qualifier

When multiple implementations exist, use `@Named` to disambiguate.

```java
@Component
@Named("mysql")
public class MySQLDataSource implements DataSource { }

@Component
@Named("postgres")
public class PostgresDataSource implements DataSource { }

@Component
public class DatabaseService {
    @Inject
    @Named("mysql")
    private DataSource dataSource;  // Gets MySQLDataSource
}
```

### Primary Bean

Mark one implementation as primary when multiple exist.

```java
@Component
@Primary
public class PrimaryService implements ServiceInterface { }

@Component
public class ClientService {
    @Inject
    private ServiceInterface service;  // Gets PrimaryService
}
```

## Lifecycle Callbacks

### PostConstruct and PreDestroy

Execute code after injection completes and before destruction.

```java
@Component
public class DatabaseService {
    
    @PostConstruct
    public void init() {
        // Called after dependency injection
        connect();
    }
    
    @PreDestroy
    public void cleanup() {
        // Called before bean destruction
        disconnect();
    }
}
}

## Building Your Project

Compile your project normally. Veld's annotation processor and Maven plugin will automatically:

1. Discover all annotated components
2. Generate the Veld registry
3. Weave bytecode for private field injection

```bash
mvn clean compile
```

## Running Tests

```bash
mvn clean test
```

All tests will run with Veld's dependency injection configured.

## Next Steps

- [Core Features](core-features.md) - Learn about all Veld features
- [Annotations Reference](annotations.md) - Complete annotations documentation
- [Architecture](architecture.md) - Understanding how Veld works
- [Examples](examples.md) - Example projects

## Common Issues

### Component Not Found

Ensure your component classes are in a scanned package. Veld scans from the root package by default.

### Private Field Injection Fails

Make sure the `veld-maven-plugin` is configured and the `veld-weaver` dependency is available.

### Multiple Implementations

Use `@Named` or custom qualifiers to disambiguate when multiple implementations exist.

## Getting Help

- [GitHub Issues](https://github.com/yasmramos/Veld/issues)
- [Contributing Guide](CONTRIBUTING.md)
- [License](LICENSE)
