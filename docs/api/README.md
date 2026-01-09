# Veld API Reference

## Core Classes

### Veld

The main entry point for the DI container.

```java
Veld veld = Veld.start();
T bean = veld.getBean(Class<T>);
void veld.stop();
```

### ComponentInfo

Metadata about a registered component.

### ComponentFactory

Factory interface for creating component instances.

## Annotations

| Annotation | Purpose |
|------------|---------|
| `@Component` | Mark a class as a bean |
| `@Singleton` | Singleton scope (default) |
| `@Prototype` | Prototype scope |
| `@Inject` | Field/constructor injection |
| `@PostConstruct` | Initialization callback |
| `@PreDestroy` | Destruction callback |
| `@Qualifier` | Custom qualifier |
| `@Named` | Named bean injection |

## Scopes

| Scope ID | Description |
|----------|-------------|
| `singleton` | Single instance (default) |
| `prototype` | New instance each time |
| Custom | Via `ScopeRegistry.register()` |
