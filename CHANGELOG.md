# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-alpha.5] - 2025-12-04

### Added

#### EventBus Module (veld-runtime)
- **EventBus**: Lightweight publish/subscribe event system
  - Synchronous and asynchronous event publishing
  - `@Subscribe` annotation for event handlers
  - Priority-based handler ordering
  - Event type hierarchy support (handlers receive subtype events)
  - Weak reference subscribers to prevent memory leaks
  - Dead event handling for undelivered events
  - Thread-safe implementation with `ExecutorService` support

#### AOP Module (veld-aop) - NEW MODULE
- **Complete AOP Implementation** with ASM bytecode proxy generation
- **Aspect Annotations**:
  - `@Aspect` - Marks classes as aspects with optional priority
  - `@Around` - Around advice wrapping method execution
  - `@Before` - Advice executed before method
  - `@After` - Advice executed after method (RETURNING, THROWING, FINALLY)
  - `@Pointcut` - Reusable named pointcut definitions
- **CDI-Style Interceptors**:
  - `@Interceptor` - Marks classes as interceptors
  - `@AroundInvoke` - Marks interceptor invocation methods
  - `@InterceptorBinding` - Meta-annotation for custom bindings
- **Pointcut Expression Language**:
  - `execution(* package.Class.method(..))` - Method execution matching
  - `within(package..*)` - Package/class matching
  - `@annotation(AnnotationName)` - Annotation-based matching
  - `bean(beanName)` - Bean name matching
  - Wildcard support: `*` (single), `..` (any number)
  - Composite pointcuts with `&&`, `||`, `!` operators
- **Built-in Interceptors**:
  - `@Logged` + `LoggingInterceptor` - Automatic method logging
  - `@Timed` + `TimingInterceptor` - Performance measurement with statistics
  - `@Validated` + `ValidationInterceptor` - Argument validation
  - `@Transactional` + `TransactionInterceptor` - Transaction management simulation
- **Core AOP Classes**:
  - `JoinPoint` - Represents execution point
  - `InvocationContext` - CDI-compliant invocation context
  - `MethodInvocation` - Interceptor chain execution
  - `MethodInterceptor` - Functional interceptor interface
  - `Advice` - Encapsulates advice with pointcut
  - `InterceptorRegistry` - Central aspect/interceptor registry
  - `PointcutExpression` - Expression parsing and evaluation
  - `CompositePointcut` - Boolean pointcut combinations
  - `ProxyFactory` - ASM-based dynamic proxy generation
  - `ProxyMethodHandler` - Runtime method delegation

### Changed
- Updated parent POM to include `veld-aop` module
- Updated `veld-example` to demonstrate EventBus and AOP features

---

## [1.0.0-alpha.4] - 2025-12-03

### Added

#### Conditional Configuration
- `@ConditionalOnProperty` - Register components based on system properties/environment variables
  - `name` - Property name to check
  - `havingValue` - Expected value (default: "true")
  - `matchIfMissing` - Register if property not set (default: false)
- `@ConditionalOnClass` - Register components based on classpath availability
  - `value` - Array of Class objects to check
  - `name` - Array of fully-qualified class names
- `@ConditionalOnMissingBean` - Register fallback components
  - `value` - Bean types to check
  - `name` - Bean names to check
- `ConditionContext` - Runtime context for condition evaluation
- `ConditionEvaluator` - Evaluates conditions at container initialization
- `ConditionalRegistry` - Filters components based on conditions

#### Optional Dependencies
- `@Optional` annotation for optional dependency injection
- `Optional<T>` wrapper support for optional dependencies
- `container.tryGet()` - Returns null if component not found
- `container.getOptional()` - Returns Optional.empty() if not found
- Optional dependencies excluded from circular dependency detection

---

## [1.0.0-alpha.3] - 2025-12-02

### Added

#### Named/Qualified Injection
- `@Named` annotation support (Veld, JSR-330, Jakarta)
- Qualifier-based dependency resolution
- Named component registration and lookup

---

## [1.0.0-alpha.2] - 2025-12-02

### Added

#### Lazy Initialization
- `@Lazy` annotation for deferred component instantiation
- `LazyHolder<T>` wrapper for lazy loading
- Components only instantiated on first access

#### Provider Injection
- `Provider<T>` support for on-demand instance creation
- Compatible with:
  - `com.veld.runtime.Provider<T>`
  - `javax.inject.Provider<T>`
  - `jakarta.inject.Provider<T>`

### Changed
- Simplified annotations: `@Singleton`, `@Prototype`, `@Lazy` now imply `@Component`
- No longer need to add both `@Component` and scope annotations

---

## [1.0.0-alpha.1] - 2025-12-01

### Added

#### Core DI Framework
- **Compile-time Dependency Injection** using ASM bytecode generation
- **Zero Reflection** - All resolution via generated factories
- **Lightweight Runtime** - Minimal footprint

#### Injection Types
- Constructor injection
- Field injection (non-private fields)
- Method injection (setter methods)

#### Scopes
- `@Singleton` - Single shared instance
- `@Prototype` - New instance per injection

#### Annotation Support
- **Veld annotations**: `@Component`, `@Inject`, `@Singleton`, `@Prototype`
- **JSR-330 (javax.inject)**: `@Inject`, `@Singleton`, `@Named`
- **Jakarta Inject**: `@Inject`, `@Singleton`, `@Named`
- Full interoperability between all annotation sources

#### Interface-Based Injection
- Inject by interface type
- Automatic resolution to concrete implementations

#### Lifecycle Callbacks
- `@PostConstruct` - Called after dependency injection
- `@PreDestroy` - Called on container close

#### Safety Features
- Compile-time circular dependency detection
- Clear error messages for dependency issues
- Type-safe component resolution

---

## Version History Summary

| Version | Date | Highlights |
|---------|------|------------|
| 1.0.0-alpha.5 | 2025-12-04 | EventBus, Complete AOP with ASM proxies |
| 1.0.0-alpha.4 | 2025-12-03 | Conditional configuration, Optional injection |
| 1.0.0-alpha.3 | 2025-12-02 | Named/Qualified injection |
| 1.0.0-alpha.2 | 2025-12-02 | Lazy initialization, Provider injection |
| 1.0.0-alpha.1 | 2025-12-01 | Initial release, Core DI framework |

---

[1.0.0-alpha.5]: https://github.com/yasmramos/Veld/compare/v1.0.0-alpha.4...v1.0.0-alpha.5
[1.0.0-alpha.4]: https://github.com/yasmramos/Veld/compare/v1.0.0-alpha.3...v1.0.0-alpha.4
[1.0.0-alpha.3]: https://github.com/yasmramos/Veld/compare/v1.0.0-alpha.2...v1.0.0-alpha.3
[1.0.0-alpha.2]: https://github.com/yasmramos/Veld/compare/v1.0.0-alpha.1...v1.0.0-alpha.2
[1.0.0-alpha.1]: https://github.com/yasmramos/Veld/releases/tag/v1.0.0-alpha.1
