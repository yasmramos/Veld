# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Placeholder for future features

### Changed
- Bumped Byte Buddy to 1.18.3 in dependency management
- Updated Spring Framework versions for parent (7.0.2) and benchmarks (5.3.39)

## [1.0.3] - 2025-12-29

### Added
- Complete test coverage with 543 passing tests across all modules
- New dependency graph visualization capabilities
- `DependencyGraph` class for building and analyzing component graphs
- `DependencyNode` class with scope, profiles, and dependency tracking
- `DotExporter` for Graphviz-compatible DOT format export
- `JsonExporter` for JSON format export with full metadata
- Root and leaf node detection algorithms
- Circular dependency detection at runtime
- `ScopeType` enum for type-safe scope definitions
- Comprehensive tests for graph operations and exporters
- New test files: DependencyGraphTest, DotExporterTest, DependencyNodeTest, ScopeTest
- EventBusTest for event bus functionality verification
- AsmUtilsTest for ASM utilities testing

### Fixed
- Compilation error in DotExporterTest where assertEquals was used on void method
- NullPointerException in ComponentFactoryTest by removing null-scope test
- Incorrect root/leaf node expectations in DependencyGraphTest
- AsmUtilsTest to expect ScopeType instead of Scope enum
- Source generators updated to use ScopeType for type-safe scope handling
- Test fixture constructors updated to use new ComponentInfo signature

### Changed
- Enhanced Veld API with dependency graph export methods (exportDependencyGraphDot, exportDependencyGraphJson, getDependencyGraph)
- Updated README with complete annotations reference (64 annotations documented)
- Updated README with complete Veld API documentation (component retrieval, inspection, value resolution, EventBus, profiles, graph export, shutdown)
- Added real DOT and JSON output examples to documentation with actual format samples
- Added missing modules to documentation (veld-benchmark, veld-example, veld-spring-boot-example, veld-test)
- Added Testing Infrastructure section highlighting 543 passing tests
- Added Dependency Graph Visualization section with complete usage examples
- Updated veld-runtime description to include dependency graph visualization

## [1.0.2] - 2025-12-22

### Fixed
- Updated CodeQL action from v2 to v3 to resolve deprecation warning
- Added security-events: write permission to fix 'Resource not accessible by integration' error
- Ensured proper GitHub Security tab integration for vulnerability scan results

### Changed
- Updated all POM files to version 1.0.2 for version consistency

## [1.0.1] - 2025-12-22

### Changed
- Refactored VeldException to independent class for better API design
- Unified Veld.java files into single comprehensive public API
- Improved Spring Boot starter compatibility

### Fixed
- Corrected VeldException import in VeldSpringBootService
- Fixed compilation errors in Spring Boot integration

### Removed
- Removed emojis from Java documentation and code for professional appearance
- Cleaned up temporary release documentation files

## [1.0.0] - 2025-12-17

### Added
- Complete automatic integration of all lifecycle callbacks (@PostConstruct, @PreDestroy)
- Automatic EventBus registration for @Subscribe methods
- Automatic value resolution for @Value annotations
- Automatic conditional loading with @Profile and @ConditionalOnProperty
- Named injection support via get(Class, String)
- Provider<T> injection support
- Optional<T> injection support
- Comprehensive integration tests
- Complex application example demonstrating all features
- Ultra-fast dependency injection framework
- Zero reflection runtime performance
- Thread-local cache for ~2ns lookup times
- Hash table lookup for ~5ns lookup times
- Compile-time bytecode generation
- Support for constructor, field, and method injection
- Singleton and Prototype scopes
- Interface-based injection
- Named qualifiers (@Named)
- JSR-330 and Jakarta Inject compatibility
- Lifecycle callbacks (@PostConstruct, @PreDestroy)
- Value injection (@Value)
- EventBus integration
- Profile-based conditional loading
- AOP support via veld-aop module
- Spring Boot integration
- Comprehensive benchmarks showing 80x faster than Spring
- Maven plugin for unified build process

### Changed
- Enhanced Veld.class API with complete feature access
- Updated documentation with all integrated capabilities
- Streamlined project structure

### Fixed
- All compilation and dependency issues resolved
- Maven plugin integration perfected

## [1.0.0] - 2025-12-17

### Added
- Ultra-fast dependency injection framework
- Zero reflection runtime performance
- Thread-local cache for ~2ns lookup times
- Hash table lookup for ~5ns lookup times
- Compile-time bytecode generation
- Support for constructor, field, and method injection
- Singleton and Prototype scopes
- Interface-based injection
- Named qualifiers (@Named)
- JSR-330 and Jakarta Inject compatibility
- Lifecycle callbacks (@PostConstruct, @PreDestroy)
- Value injection (@Value)
- EventBus integration
- Profile-based conditional loading
- AOP support via veld-aop module
- Spring Boot integration
- Comprehensive benchmarks showing 80x faster than Spring
- Maven plugin for unified build process

### Technical Features
- ASM bytecode generation for maximum performance
- Synthetic setters for private field injection across packages
- Topological sorting for dependency initialization
- Circular dependency detection at compile-time
- Graceful shutdown with @PreDestroy callbacks
- Thread-safe singleton initialization
- Memory-efficient with zero allocations per injection

### Performance Metrics
- Startup time: 0.12ms (vs 458ms Spring)
- Lookup latency: 2.09ns average (vs 161.7ns Spring)
- Throughput: 479M ops/sec (vs 6M ops/sec Spring)
- Memory footprint: 2.1MB (vs 48.7MB Spring)

### Modules
- veld-annotations: Core annotations
- veld-runtime: Runtime utilities and EventBus
- veld-processor: Compile-time annotation processing
- veld-weaver: Bytecode weaving for synthetic setters
- veld-maven-plugin: Unified build plugin
- veld-aop: Aspect-Oriented Programming
- veld-spring-boot-starter: Spring Boot integration
- veld-benchmark: Performance benchmarking
- veld-example: Complete usage examples

---

## Roadmap Estratégico

El roadmap estratégico completo está disponible en: **[ROADMAP.md](ROADMAP.md)**

El roadmap incluye:
- **Fase 1:** Consolidación del Core (DX, Testing, Observabilidad) - Q1-Q2 2026
- **Fase 2:** Expansión del Ecosistema (JPA, Kafka, Web, Messaging) - Q3-Q4 2026
- **Fase 3:** Diferenciación y Liderazgo (Native Cloud, AI/ML, Portal) - 2027
- **Fase 4:** Comunidad y Ecosistema - Paralelo
- **Visión 2028:** Veld 3.0 native binary
- **KPIs de éxito** con métricas trimestrales
- **Recomendaciones** de inversión de esfuerzo

---

[Unreleased]: https://github.com/yasmramos/Veld/compare/v1.0.0...HEAD
[1.0.3]: https://github.com/yasmramos/Veld/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/yasmramos/Veld/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/yasmramos/Veld/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/yasmramos/Veld/releases/tag/v1.0.0
