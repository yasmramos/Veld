![Veld](.github/assets/logo.png)

[![Build Status](https://github.com/yasmramos/Veld/actions/workflows/maven.yml/badge.svg)](https://github.com/yasmramos/Veld/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)](https://maven.apache.org/)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/yasmramos/veld)
[![Tests](https://img.shields.io/badge/Tests-0%20passed-green)](https://github.com/yasmramos/Veld/actions)
[![Coverage](https://img.shields.io/badge/Coverage-%25-green)](https://codecov.io/gh/yasmramos/Veld)
[![Last Release](https://img.shields.io/github/v/release/yasmramos/Veld?display_name=tag)](https://github.com/yasmramos/Veld/releases)
[![Last Commit](https://img.shields.io/github/last-commit/yasmramos/Veld/develop)](https://github.com/yasmramos/Veld/commits/develop)
[![Issues](https://img.shields.io/github/issues/yasmramos/Veld)](https://github.com/yasmramos/Veld/issues)
[![Forks](https://img.shields.io/github/forks/yasmramos/Veld/network/members)](https://github.com/yasmramos/Veld/network/members)
[![Stars](https://img.shields.io/github/stars/yasmramos/Veld)](https://github.com/yasmramos/Veld/stargazers)
[![Contributors](https://img.shields.io/github/contributors/yasmramos/Veld)](https://github.com/yasmramos/Veld/graphs/contributors)

# Veld Framework

**Ultra-fast Dependency Injection for Java - Zero Reflection, Pure Code Generation**

Veld is a compile-time Dependency Injection framework that generates pure Java code. Zero reflection at runtime means maximum performance - up to 100x faster than Spring in dependency resolution benchmarks.

Designed for developers who want maximum performance, full control, and zero runtime magic.

## Quick Start

### 1. Add Dependencies

**Maven:**
```xml
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
```

**Gradle:**
```gradle
implementation 'io.github.yasmramos:veld-runtime:1.0.3'
implementation 'io.github.yasmramos:veld-annotations:1.0.3'
```

### 2. Configure Maven Plugin

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

### 3. Create Components

```java
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

@Component
public class UserService {
    private final UserRepository repository;
    
    @Inject
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
    
    public User getUser(Long id) {
        return repository.findById(id);
    }
}
```

### 4. Use Veld

```java
import io.github.yasmramos.veld.Veld;

public class Main {
    public static void main(String[] args) {
        UserService userService = Veld.userService();
        User user = userService.getUser(1L);
    }
}
```

## Why Veld?

| Feature | Veld | Spring | Guice |
|---------|------|--------|-------|
| **Reflection at runtime** | None | Heavy | Moderate |
| **Startup time** | ~0.1ms | ~500ms+ | ~100ms |
| **Injection speed** | ~0.001ms | ~0.01ms | ~0.005ms |

## Performance Highlights

- **Up to 100x faster** than Spring in dependency resolution benchmarks
- **3ns** average injection latency
- **0.003ms** startup time
- Zero runtime reflection overhead

## Documentation

| Topic | Location |
|-------|----------|
| Getting Started | [docs/getting-started.md](docs/getting-started.md) |
| Annotations Reference | [docs/annotations.md](docs/annotations.md) |
| Core Features | [docs/core-features.md](docs/core-features.md) |
| API Reference | [docs/api.md](docs/api.md) |
| AOP Guide | [docs/aop.md](docs/aop.md) |
| EventBus | [docs/eventbus.md](docs/eventbus.md) |
| Performance Benchmarks | [docs/benchmarks.md](docs/benchmarks.md) |
| Architecture | [docs/architecture.md](docs/architecture.md) |
| Examples | [docs/examples.md](docs/examples.md) |

## Modules

| Module | Description |
|--------|-------------|
| `veld-annotations` | Core annotations |
| `veld-runtime` | Runtime utilities |
| `veld-processor` | Annotation processor |
| `veld-weaver` | Bytecode weaving |
| `veld-maven-plugin` | Unified build plugin |
| `veld-aop` | Aspect-Oriented Programming |
| `veld-resilience` | Circuit Breaker, Retry, Rate Limiter |
| `veld-cache` | Caching support |
| `veld-validation` | Bean validation |
| `veld-security` | Method-level security |
| `veld-metrics` | Metrics collection |
| `veld-tx` | Transaction management |
| `veld-spring-boot-starter` | Spring Boot integration |




## Spring Boot Starter

To integrate Veld with Spring Boot applications, use the Spring Boot starter.

### Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```
**Gradle:**
```gradle
implementation 'io.github.yasmramos:veld-spring-boot-starter:1.0.3'
```


## Building from Source

```bash
git clone https://github.com/yasmramos/Veld.git
cd Veld
mvn clean install
```

## Links

- [GitHub](https://github.com/yasmramos/Veld)
- [Issues](https://github.com/yasmramos/Veld/issues)
- [Contributing](CONTRIBUTING.md)
- [License](LICENSE)

---

**Veld** - Dependency Injection at the speed of direct method calls.
