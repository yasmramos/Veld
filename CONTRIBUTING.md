# Contributing to Veld Framework

Thank you for your interest in contributing to Veld Framework! This document provides guidelines and instructions for contributing to our high-performance Java dependency injection framework.

## Table of Contents

- [Welcome](#welcome)
- [Philosophy](#philosophy)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Bytecode Guidelines](#bytecode-guidelines)
- [Pull Request Process](#pull-request-process)
- [Code Review](#code-review)
- [Community](#community)

## Welcome

Veld Framework is an ultra-fast, compile-time dependency injection framework that generates pure bytecode using ASM. We welcome contributions from developers who share our passion for performance engineering and elegant solutions to complex problems.

Before contributing, please familiarize yourself with our project goals:
- **Zero Reflection at Runtime**: All injection code is generated at compile-time
- **Maximum Performance**: Sub-nanosecond injection latency
- **Clean API**: Intuitive annotations that feel familiar to Java developers
- **Bytecode Purity**: No proxies, no reflection, no runtime overhead

## Philosophy

### Performance First

Every change to Veld must consider its performance impact. We measure success in:
- Injection latency (nanoseconds)
- Startup time (microseconds)
- Memory footprint (minimal allocations)
- Throughput (operations per second)

### Zero Reflection Rule

Contributions that introduce reflection at runtime will be rejected. All functionality must be achieved through:
- Compile-time annotation processing
- Bytecode generation with ASM
- Static method calls and field access

### Backward Compatibility

We take backward compatibility seriously. Changes to the public API must follow semantic versioning and include deprecation warnings when applicable.

## Getting Started

### Prerequisites

- **Java Development Kit**: JDK 17 or JDK 21 (we test against both)
- **Maven**: Version 3.6 or higher
- **Git**: For version control
- **ASM Knowledge**: Understanding of bytecode manipulation basics

### Setting Up Your Environment

1. **Fork the Repository**

   Visit [Veld Framework on GitHub](https://github.com/yasmramos/Veld) and click the "Fork" button.

2. **Clone Your Fork**

   ```bash
   git clone https://github.com/YOUR-USERNAME/Veld.git
   cd Veld
   ```

3. **Add Upstream Remote**

   ```bash
   git remote add upstream https://github.com/yasmramos/Veld.git
   ```

4. **Verify Setup**

   ```bash
   # Sync with upstream
   git fetch upstream
   
   # Run tests to verify everything works
   ./mvnw clean test
   ```

### IDE Configuration

We recommend IntelliJ IDEA for development due to its excellent Maven and bytecode analysis support.

**Recommended Plugins:**
- ASM Bytecode Viewer
- Maven Helper
- CheckStyle-IDEA

## Development Workflow

### Branch Naming Convention

We follow a structured branch naming convention:

| Branch Type | Pattern | Example |
|-------------|---------|---------|
| Features | `feature/[short-description]` | `feature/add-circuit-breaker` |
| Bug Fixes | `fix/[issue-number]-[short-desc]` | `fix/123-fix-null-injection` |
| Improvements | `improvement/[area]-[desc]` | `improvement/weaver-performance` |
| Documentation | `docs/[area]` | `docs/update-api-docs` |
| Experiments | `experiment/[description]` | `experiment/new-scope-model` |

### Creating a Feature Branch

```bash
# Ensure you're on develop
git checkout develop
git pull upstream develop

# Create your feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: Add descriptive commit message"

# Push to your fork
git push origin feature/your-feature-name
```

### Commit Message Format

We follow the Conventional Commits specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `perf`: Performance improvement
- `refactor`: Code refactoring (no behavior change)
- `test`: Adding or modifying tests
- `docs`: Documentation changes
- `chore`: Maintenance tasks
- `asm`: Bytecode manipulation changes

**Examples:**
```
feat(veld-runtime): Add DependencyGraph visualization

Implement DOT and JSON export for dependency graph analysis

fix(veld-processor): Resolve null pointer in annotation processing

The processor now correctly handles missing @Component annotations
on inner classes

perf(veld-weaver): Reduce bytecode size for synthetic setters

Generated setters are now 30% smaller through optimization
```

## Project Structure

Veld uses a multi-module Maven structure:

```
Veld/
├── veld-annotations/          # All annotation definitions
├── veld-runtime/              # Runtime utilities, EventBus, lifecycle
├── veld-processor/            # Compile-time annotation processing
├── veld-weaver/               # Bytecode weaving for synthetic setters
├── veld-maven-plugin/         # Unified Maven plugin
├── veld-aop/                  # Aspect-Oriented Programming support
├── veld-resilience/           # Circuit Breaker, Bulkhead, Retry
├── veld-cache/                # Caching annotations
├── veld-validation/           # Bean validation
├── veld-security/             # Method-level security
├── veld-metrics/              # Metrics and instrumentation
├── veld-tx/                   # Transaction management
├── veld-spring-boot-starter/  # Spring Boot integration
├── veld-benchmark/            # JMH performance benchmarks
├── veld-example/              # Complete working examples
├── veld-spring-boot-example/  # Spring Boot examples
└── veld-test/                 # Testing utilities
```

### Understanding Module Boundaries

- **Compile-time vs Runtime**: Never introduce runtime dependencies in compile-time modules
- **API Stability**: Annotations (veld-annotations) have the strictest stability requirements
- **Bytecode**: Changes to veld-weaver and veld-processor require extra validation

## Coding Standards

### Java Style Guide

We follow standard Java conventions with these additions:

```java
// 1. Four spaces for indentation
public class Example {
    public void example() {
        if (condition) {
            doSomething();
        }
    }
}

// 2. Descriptive variable names
private DependencyGraph dependencyGraph;  // ✅ Good
private DG dg;                            // ❌ Avoid

// 3. Javadoc for public API
/**
 * Retrieves the singleton instance of the specified component.
 * 
 * @param <T> the type of component to retrieve
 * @param componentClass the class of the component
 * @return the component instance
 * @throws VeldException if the component cannot be resolved
 */
public static <T> T get(Class<T> componentClass) { ... }

// 4. Annotations on separate lines
@Component
@Singleton
public class MyService { }

// 5. Braces for all blocks (K&R style)
if (condition) {
    doSomething();
} else {
    doSomethingElse();
}
```

### XML/POM Conventions

```xml
<!-- Two spaces for indentation in XML files -->
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.github.yasmramos</groupId>
  <artifactId>veld-runtime</artifactId>
</project>
```

### Markdown Formatting

```markdown
# H1 for top-level headings
## H2 for section headings
### H3 for subsections

- Use bullet points
- For lists

1. Numbered lists
2. When sequence matters

```java
// Code blocks with language
System.out.println("Hello");
```
```

## Testing Requirements

### Test Coverage Mandate

All contributions must maintain or improve our test coverage:

- ✅ All 543+ existing tests must pass
- ✅ New features require corresponding unit tests
- ✅ Bug fixes require regression tests
- ✅ Performance changes require benchmark validation

### Running Tests

```bash
# Run all tests
./mvnw clean test

# Run specific module tests
./mvnw test -pl veld-runtime

# Run with coverage report
./mvnw clean test jacoco:report

# Run benchmarks
cd veld-benchmark
./mvnw clean package
java -jar target/veld-benchmark.jar
```

### Writing Tests

```java
@Singleton
@Component
public class TestService {
    @Inject
    private DependencyService dependency;
    
    @PostConstruct
    public void init() {
        // Test setup
    }
}

class TestServiceTest {
    @Test
    void shouldInjectDependencies() {
        TestService service = Veld.get(TestService.class);
        assertNotNull(service.getDependency());
    }
}
```

## Bytecode Guidelines

### When Modifying Bytecode Generation

Changes to veld-weaver and veld-processor require extra care:

1. **Verify Generated Bytecode**
   ```bash
   # Use ASM Bytecode Viewer plugin in IntelliJ
   # Or use javap to inspect class files
   javap -c -p target/classes/io/github/veld/MyClass.class
   ```

2. **Validate Synthetic Methods**
   - Synthetic setters follow pattern: `__di_setFieldName`
   - Bytecode must be valid JVM bytecode
   - No stack overflows or malformed instructions

3. **Performance Impact**
   - Changes to generated bytecode affect runtime performance
   - Run benchmarks before and after changes
   - Regression beyond 5% requires justification

4. **ASM API Usage**
   ```java
   // Use ASM tree API for complex transformations
   ClassNode classNode = new ClassNode();
   ClassReader reader = new ClassReader bytecode);
   reader.accept(classNode, 0);
   
   // Make modifications to classNode
   classNode.methods.add(generateSyntheticSetter());
   
   // Write back to bytecode
   ClassWriter writer = new ClassWriter(0);
   classNode.accept(writer);
   byte[] newBytecode = writer.toByteArray();
   ```

### Prohibited Patterns

The following are NOT allowed in bytecode generation:

- ❌ Reflection calls in generated code
- ❌ Dynamic class loading
- ❌ Exception throwing in synthetic methods (unless critical)
- ❌ Blocking operations or synchronization
- ❌ Object allocation beyond constructor parameters

## Pull Request Process

### Before Creating a PR

1. **Update Your Branch**
   ```bash
   git fetch upstream
   git rebase upstream/develop
   ```

2. **Verify All Tests Pass**
   ```bash
   ./mvnw clean test
   ```

3. **Check Code Quality**
   ```bash
   # Run checkstyle
   ./mvnw checkstyle:check
   
   # Run spotbugs
   ./mvnw spotbugs:check
   ```

4. **Update Documentation**
   - Update README if adding features
   - Add JavaDoc for new public APIs
   - Update CHANGELOG.md

### Creating the PR

1. Fill out the PR template completely
2. Link related issues
3. Provide clear description of changes
4. Include benchmark results if performance-related
5. Mark as draft if still in progress

### PR Template

```markdown
## Description
[Describe your changes clearly and concisely]

## Type of Change
- [ ] Bug fix (non-breaking change)
- [ ] New feature (non-breaking change)
- [ ] Performance improvement
- [ ] Refactoring (no behavior change)
- [ ] Documentation update

## Testing
- [ ] All existing tests pass
- [ ] New tests added for changes
- [ ] Manual testing performed

## Performance Impact
[Describe any performance impact]
[Run benchmarks if applicable]

## Breaking Changes
[Does this PR introduce breaking changes?]

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review performed
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] Benchmarks run (if applicable)
```

## Code Review

### Review Process

1. **Automated Checks**: CI runs tests, checkstyle, and spotbugs
2. **Maintainer Review**: A project maintainer reviews the PR
3. **Feedback Loop**: Address any requested changes
4. **Approval**: Maintainer approves the PR
5. **Merge**: PR is merged to develop branch

### What Reviewers Look For

- ✅ Correctness and completeness
- ✅ Performance impact analysis
- ✅ Test coverage
- ✅ Code style compliance
- ✅ Documentation accuracy
- ✅ Security implications
- ✅ Backward compatibility

### Addressing Feedback

```bash
# Make changes based on review
git add .
git commit -m "chore: Address PR review feedback"

# Push updates
git push origin feature/your-feature-name
```

## Community

### Communication Channels

- **GitHub Discussions**: General questions and ideas
- **GitHub Issues**: Bug reports and feature requests
- **Pull Requests**: Code contributions

### Getting Help

1. Check existing documentation (README.md, docs/)
2. Search existing issues and discussions
3. Ask questions in GitHub Discussions
4. Review similar pull requests for patterns

### Recognition

Contributors are recognized in:
- **CONTRIBUTORS.md** (when created)
- Release notes for significant contributions
- GitHub contributor statistics

---

## Thank You!

Your contributions make Veld Framework better for everyone. We appreciate your time and effort in helping us build the fastest Java dependency injection framework.

**Happy Coding!** 🚀

---

**Last Updated**: 2025-12-30
**Version**: 1.0.3
