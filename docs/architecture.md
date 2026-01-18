# Architecture

Veld uses a three-phase build process that transforms your annotated classes into highly optimized dependency injection code at compile-time.

## Build-Time Process

```
┌─────────────────────────────────────────────────────────────┐
│                    COMPILE TIME                              │
├─────────────────────────────────────────────────────────────┤
│  1. Annotation Processing                                    │
│     - Discovers @Component classes                          │
│     - Analyzes injection points                             │
│     - Writes component metadata                             │
├─────────────────────────────────────────────────────────────┤
│  2. Bytecode Weaving                                        │
│     - Adds synthetic setters (__di_set_fieldName)           │
│     - Enables private field injection across packages       │
├─────────────────────────────────────────────────────────────┤
│  3. Registry Generation                                      │
│     - Generates Veld.class with pure bytecode               │
│     - Static fields for singletons                          │
│     - Factory methods for prototypes                        │
│     - Direct method calls - NO reflection                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      RUNTIME                                 │
├─────────────────────────────────────────────────────────────┤
│  Veld.myService()                                             │
│    └── Returns pre-created singleton (static field access) │
│    └── Or calls factory method for prototype                │
│    └── Zero reflection, zero proxy, maximum speed           │
└─────────────────────────────────────────────────────────────┘
```

## Phase 1: Annotation Processing

The `veld-processor` module runs during compilation and:

1. **Discovers Components**: Scans classpath for `@Component`, `@Bean`, `@Configuration`, and `@Factory` annotations
2. **Analyzes Injection Points**: Identifies all `@Inject` fields, constructors, and methods
3. **Extracts Metadata**: Gathers scope information, qualifiers, dependencies, and lifecycle callbacks
4. **Generates Metadata File**: Writes component information to `META-INF/veld/components.bin`

## Phase 2: Bytecode Weaving

The `veld-weaver` module uses ASM to modify class bytecode:

1. **Synthetic Setters**: Adds methods like `__di_set_fieldName(FieldType value)` for private field injection
2. **Bridge Methods**: Creates necessary bridge methods for generic types
3. **Class Metadata**: Adds Veld-specific metadata for runtime discovery

## Phase 3: Registry Generation

The `veld-maven-plugin` generates the `Veld.class` registry:

1. **Singleton Storage**: Creates static fields for singleton instances
2. **Factory Methods**: Generates factory methods for prototype-scoped beans
3. **Direct Calls**: Replaces reflection with direct method invocations
4. **Static Initialization**: Initializes all singletons in dependency order

## Runtime Performance

At runtime, `Veld.myService()` performs:

1. **Direct Field Access**: Returns singleton from static field (3-5 CPU cycles)
2. **Factory Invocation**: Calls factory method for prototype (10-20 CPU cycles)
3. **No Reflection**: Zero reflection overhead
4. **No Proxies**: No dynamic proxy generation or interception

## Module Architecture

### veld-annotations

Contains all public annotations used by Veld:

- `@Component`, `@Bean`, `@Factory`, `@Configuration`
- `@Inject`, `@Value`, `@Named`, `@Qualifier`
- `@Singleton`, `@Prototype`, `@Lazy`, `@VeldScope`
- `@PostConstruct`, `@PreDestroy`, `@Order`
- Conditional annotations (`@ConditionalOnProperty`, etc.)

### veld-runtime

Core runtime implementation:

- `Veld` class - Main API entry point
- `EventBus` - Event publication and subscription
- `LifecycleProcessor` - Manages component lifecycle
- `ValueResolver` - Configuration property resolution
- `DependencyGraph` - Dependency visualization and analysis
- `GraphExporter` - DOT and JSON export

### veld-processor

Annotation processor that runs during compilation:

- Discovers annotated components
- Analyzes dependency relationships
- Generates component metadata file

### veld-weaver

Bytecode manipulation library:

- Adds synthetic setters for private field injection
- Enables injection across package boundaries
- Works with ASM for efficient bytecode modification

### veld-maven-plugin

Unified Maven plugin that orchestrates:

- Annotation processing
- Bytecode weaving
- Registry generation
- Integration with Maven build lifecycle

### veld-aop

Aspect-Oriented Programming support:

- `@Aspect` annotation for aspect classes
- `@Before`, `@After`, `@Around` advice
- Pointcut expressions for method matching
- Integration with dependency injection

### veld-resilience

Fault tolerance patterns:

- `@Retry` - Automatic retry with exponential backoff
- `@RateLimiter` - Request rate limiting
- `@CircuitBreaker` - Circuit breaker pattern
- `@Bulkhead` - Resource isolation
- `@Timeout` - Request timeout handling

### veld-cache

Caching support:

- `@Cacheable` - Cache method results
- `@CacheEvict` - Remove cache entries
- `@CachePut` - Update cache
- Configurable TTL and cache eviction policies

### veld-validation

Bean validation integration:

- `@Valid` - Trigger validation
- Standard constraint annotations (`@NotNull`, `@Size`, etc.)
- Method-level validation with `@Validated`

### veld-security

Method-level security:

- `@Secured` - Enable security
- `@RolesAllowed` - Role-based access
- `@PermitAll`, `@DenyAll` - Access control
- `@PreAuthorize` - Expression-based authorization

### veld-metrics

Runtime metrics collection:

- `@Timed` - Record execution time
- `@Counted` - Count invocations
- `@Gauge` - Expose values as metrics
- Metrics registry for collection

### veld-tx

Transaction management:

- `@Transactional` - Declarative transactions
- Propagation control
- Rollback rules

### veld-spring-boot-starter

Spring Boot integration:

- Auto-configuration
- `VeldApplication` class
- Spring Boot properties support
- Health indicators

## Design Principles

1. **Compile-Time First**: Do as much work as possible at compile-time
2. **Zero Runtime Reflection**: No reflection means predictable performance
3. **Minimal Overhead**: Generated code is hand-optimized for speed
4. **Standards Compliant**: Support JSR-330 and Jakarta Inject standards
5. **JPMS Compatible**: Full support for Java Module System
6. **IDE Friendly**: Works with all major Java IDEs
