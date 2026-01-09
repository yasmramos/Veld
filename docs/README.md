# Veld Framework Documentation

## Overview

Veld is an ultra-fast Dependency Injection (DI) framework for Java that uses pure ASM bytecode generation with zero reflection at runtime.

## Documentation Sections

- [Architecture](architecture/) - Core concepts and design
- [Guides](guides/) - How-to guides and tutorials
- [API](api/) - API reference documentation

## Quick Links

- [Getting Started](../README.md)
- [GitHub Repository](https://github.com/yasmramos/Veld)
- [Examples](../veld-example/)

## Modules

| Module | Description |
|--------|-------------|
| `veld-annotations` | Core annotations (@Component, @Singleton, @Inject, etc.) |
| `veld-runtime` | DI container, lifecycle management, events |
| `veld-processor` | Annotation processor for compile-time factory generation |
| `veld-weaver` | Bytecode weaving for synthetic setters |
| `veld-aop` | Aspect-Oriented Programming support |
| `veld-resilience` | Circuit breaker, retry, rate limiting |
| `veld-cache` | Caching with @Cacheable |
| `veld-validation` | Bean validation integration |
| `veld-security` | Role-based access control |
| `veld-metrics` | Metrics and instrumentation |
| `veld-tx` | Declarative transactions |
| `veld-spring-boot-starter` | Spring Boot integration |

## License

Apache License 2.0
