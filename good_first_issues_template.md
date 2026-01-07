# Good First Issues for Veld Framework

This file contains the 13 issues ready to be created on GitHub.

---

## Issue 1: Document Runtime Module APIs

**Title:** `[Docs] Document veld-runtime module APIs`

**Description:**

The `veld-runtime` module contains multiple infrastructure classes that lack adequate Javadoc documentation. Classes like `ValueResolver`, `EventBus`, `ScopeRegistry`, `ConditionEvaluator`, and graph exporters (`DependencyGraph`, `DotExporter`, `JsonExporter`) have important functionality but incomplete documentation.

**Acceptance Criteria:**

- [ ] All public classes have Javadoc documentation with purpose description
- [ ] Each public method is documented with parameters, return, and exceptions
- [ ] Usage examples are included where appropriate
- [ ] The command `./mvnw javadoc:javadoc` generates documentation without errors

**Related Files:**
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/value/ValueResolver.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/event/EventBus.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/scope/ScopeRegistry.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/condition/ConditionEvaluator.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/DependencyGraph.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/DotExporter.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/JsonExporter.java`

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 3-5 hours  
**Labels:** `documentation`, `good first issue`, `help wanted`

---

## Issue 2: Add Unit Tests for ValueResolver

**Title:** `[Test] Add unit tests for ValueResolver`

**Description:**

The `ValueResolver` is responsible for resolving property expressions but has limited test coverage.

**Acceptance Criteria:**

- [ ] Tests for existing property resolution
- [ ] Tests for properties with default values
- [ ] Tests for properties without default value
- [ ] Tests for expressions with primitive types
- [ ] Tests for malformed expressions

**File to Modify:**
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/value/ValueResolver.java`

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 2-3 hours  
**Labels:** `testing`, `good first issue`, `help wanted`

---

## Issue 3: Create Complete Quick Start Guide

**Title:** `[Docs] Create quick start guide with complete examples`

**Description:**

The README.md contains a basic "Quick Start" section. A complete guide demonstrating all important features is needed.

**Acceptance Criteria:**

- [ ] Guide with at least 8 sections covering different features
- [ ] Each section includes functional code
- [ ] All examples have been verified working

**Suggested Sections:**
1. Basic constructor injection
2. Field injection (private fields)
3. Method injection
4. Scopes: Singleton and Prototype
5. Lifecycle callbacks
6. Basic EventBus usage
7. Qualified annotations (@Named)
8. Property configuration (@Value)

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 3-5 hours  
**Labels:** `documentation`, `good first issue`, `help wanted`

---

## Issue 4: Add Tests for DependencyNode

**Title:** `[Test] Add tests for DependencyNode`

**Description:**

The `DependencyNode` class lacks dedicated unit tests for the dependency graph visualization functionality.

**Acceptance Criteria:**

- [ ] Tests for DependencyNode creation with different scopes
- [ ] Tests for adding and querying node dependencies
- [ ] Tests for correct serialization to exportable formats

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 1-2 hours  
**Labels:** `testing`, `good first issue`, `help wanted`

---

## Issue 5: Document Resilience Annotations

**Title:** `[Docs] Document veld-resilience module annotations`

**Description:**

Resilience annotations (@Retry, @CircuitBreaker, @RateLimiter, @Bulkhead, @Timeout) lack detailed Javadoc documentation.

**Acceptance Criteria:**

- [ ] Each annotation has complete Javadoc documentation
- [ ] Documentation includes description of each parameter with default values
- [ ] Documentation includes usage examples

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 2-3 hours  
**Labels:** `documentation`, `good first issue`, `help wanted`

---

## Issue 6: Add Tests for ScopeRegistry

**Title:** `[Test] Add tests for ScopeRegistry`

**Description:**

The `ScopeRegistry` lacks dedicated unit tests for custom scope registration and resolution.

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 1-2 hours  
**Labels:** `testing`, `good first issue`, `help wanted`

---

## Issue 7: Update Dependencies

**Title:** `[Chore] Update project dependencies`

**Description:**

The `pom.xml` file has dependencies that could be updated to more recent versions while maintaining compatibility.

**Acceptance Criteria:**

- [ ] JaCoCo updated to latest stable version
- [ ] All tests pass after the update
- [ ] Changes documented in CHANGELOG.md

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 1-2 hours  
**Labels:** `chore`, `dependencies`, `good first issue`, `help wanted`

---

## Issue 8: Improve Spring Boot Starter README

**Title:** `[Docs] Improve veld-spring-boot-starter documentation`

**Description:**

The README does not demonstrate all possible integration scenarios between Veld and Spring Boot.

**Acceptance Criteria:**

- [ ] Examples of Veld beans in Spring Boot application
- [ ] Mixed configuration documentation
- [ ] Troubleshooting section

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 2-4 hours  
**Labels:** `documentation`, `integration`, `good first issue`, `help wanted`

---

## Issue 9: Normalize Pull Request Templates

**Title:** `[Maintenance] Normalize Pull Request templates`

**Description:**

Pull Request templates have different formats. Normalizing will improve consistency.

**Difficulty:** 🟢 Beginner  
**Estimated Time:** 1-2 hours  
**Labels:** `maintenance`, `documentation`, `good first issue`, `help wanted`

---

## Issue 10: Create Resilience Module Example

**Title:** `[Example] Create functional resilience annotations example`

**Description:**

The `veld-resilience` module lacks a complete functional example demonstrating @Retry, @CircuitBreaker, @RateLimiter, etc.

**Acceptance Criteria:**

- [ ] Functional example with external service simulation
- [ ] Demonstration of @Retry with different configurations
- [ ] Demonstration of @CircuitBreaker with state transitions

**Difficulty:** 🟡 Intermediate  
**Estimated Time:** 4-6 hours  
**Labels:** `example`, `documentation`, `good first issue`, `help wanted`

---

## Issue 11: Create Conditional Registration Examples

**Title:** `[Example] Create conditional registration examples`

**Description:**

The conditional registration system (@ConditionalOnProperty, @ConditionalOnMissingBean, etc.) lacks dedicated examples.

**Acceptance Criteria:**

- [ ] Functional example for @ConditionalOnProperty
- [ ] Functional example for @ConditionalOnMissingBean
- [ ] Functional example for @ConditionalOnClass
- [ ] Functional example for @ConditionalOnBean

**Difficulty:** 🟡 Intermediate  
**Estimated Time:** 3-4 hours  
**Labels:** `example`, `documentation`, `good first issue`, `help wanted`

---

## Issue 12: Improve EventBus Error Handling

**Title:** `[Enhancement] Improve EventBus error handling`

**Description:**

The `EventBus` could improve error handling with more descriptive messages.

**Acceptance Criteria:**

- [ ] EventBus logs warnings when subscriber has invalid signature
- [ ] Error messages include sufficient information for debugging

**Difficulty:** 🟡 Intermediate  
**Estimated Time:** 2-3 hours  
**Labels:** `enhancement`, `error-handling`, `good first issue`, `help wanted`

---

## Issue 13: Improve ConditionEvaluator Error Messages

**Title:** `[Enhancement] Improve ConditionEvaluator error messages`

**Description:**

The `ConditionEvaluator` could provide more descriptive error messages when conditions fail.

**Difficulty:** 🟡 Intermediate  
**Estimated Time:** 2-3 hours  
**Labels:** `enhancement`, `error-handling`, `good first issue`, `help wanted`

---

## Summary

| # | Issue | Difficulty | Time |
|---|-------|------------|------|
| 1 | Document Runtime Module APIs | 🟢 | 3-5h |
| 2 | Add Unit Tests for ValueResolver | 🟢 | 2-3h |
| 3 | Create Complete Quick Start Guide | 🟢 | 3-5h |
| 4 | Add Tests for DependencyNode | 🟢 | 1-2h |
| 5 | Document Resilience Annotations | 🟢 | 2-3h |
| 6 | Add Tests for ScopeRegistry | 🟢 | 1-2h |
| 7 | Update Dependencies | 🟢 | 1-2h |
| 8 | Improve Spring Boot Starter README | 🟢 | 2-4h |
| 9 | Normalize Pull Request Templates | 🟢 | 1-2h |
| 10 | Create Resilience Module Example | 🟡 | 4-6h |
| 11 | Create Conditional Registration Examples | 🟡 | 3-4h |
| 12 | Improve EventBus Error Handling | 🟡 | 2-3h |
| 13 | Improve ConditionEvaluator Error Messages | 🟡 | 2-3h |

**Total:** 13 issues (9 beginner, 4 intermediate)
