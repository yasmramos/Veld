# Veld Architecture

## Core Concepts

### Zero Reflection at Runtime

Veld generates factory classes at compile time using an annotation processor.
This means:

- No reflection overhead during bean creation
- Faster startup time
- Better GraalVM native image support
- Compile-time safety for dependency configuration

### Component Lifecycle

1. **Discovery** - Scan for @Component annotations
2. **Registration** - Register components with their dependencies
3. **Factory Generation** - Generate factory classes via annotation processor
4. **Instantiation** - Create instances via generated factories
5. **Injection** - Inject dependencies
6. **Initialization** - Call @PostConstruct methods
7. **Usage** - Application runs
8. **Destruction** - Call @PreDestroy methods, release resources

### Scopes

- **Singleton** - Single instance per container (default)
- **Prototype** - New instance per injection point
- Custom scopes via `Scope` interface

## Directory Structure

```
veld/
├── veld-annotations/          # Core annotations
├── veld-runtime/              # DI container core
├── veld-processor/            # Annotation processor
├── veld-weaver/               # Bytecode weaving
├── veld-aop/                  # AOP support
├── veld-spring-boot-starter/  # Spring Boot integration
└── veld-example/              # Examples
```
