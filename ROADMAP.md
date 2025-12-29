# Veld Roadmap Estratégico 2025-2027

**Visión: "Veld como el Rust de los Microservicios Java"**

Objetivo: Convertirse en el framework estándar para microservicios ultrarrápidos (<10ms startup) sin sacrificar la experiencia del desarrollador.

> **Última actualización:** 2025-12-29
> **Estado actual:** v1.0.3 - 543 tests pasando, dependency graph visualization completado

---

## Fase 1: Consolidación del Core (Q1-Q2 2026)

### 1.1 Developer Experience (DX) - Prioridad CRÍTICA

| Feature | Descripción | Estado | Progreso |
|---------|-------------|--------|----------|
| IntelliJ IDEA Plugin | Autocomplete para @Value, navegación a beans, visualización de grafo | 🔲 Planificado | 0% |
| Gradle Plugin Estable | veld-gradle-plugin con feature-parity al Maven plugin | 🔲 Planificado | 0% |
| Error Messages Humanizados | Mensajes como "Circular dependency detected: A → B → A. Use @Lazy to break it." | 🔲 Planificado | 0% |
| Live Reload | Hot reload de beans en desarrollo (similar a Spring DevTools) | 🔲 Planificado | 0% |
| CLI Tool | veld init my-service para generar proyecto boilerplate | 🔲 Planificado | 0% |

### 1.2 Testing & Quality

| Feature | Descripción | Estado | Progreso |
|---------|-------------|--------|----------|
| @VeldTest Annotation | Runner de tests similar a @SpringBootTest con VeldTestContext | 🔲 Planificado | 0% |
| Mock Injection | @MockBean para reemplazar componentes en tests | 🔲 Planificado | 0% |
| Performance Regression Tests | Pipeline CI que falla si latency sube >5% | 🔲 Planificado | 0% |

### 1.3 Observabilidad Enterprise

| Feature | Descripción | Estado | Progreso |
|---------|-------------|--------|----------|
| Micrometer Integration | veld-metrics-micrometer para Prometheus, Datadog, New Relic | 🔲 Planificado | 0% |
| Distributed Tracing | @Trace para OpenTelemetry + mostrar grafo en Jaeger | 🔲 Planificado | 0% |
| Health Checks | /veld/health endpoint + readiness/liveness indicators | 🔲 Planificado | 0% |

---

## Fase 2: Expansión del Ecosistema (Q3-Q4 2026)

### 2.1 Data & Persistence

| Feature | Prioridad | Descripción | Estado | Progreso |
|---------|-----------|-------------|--------|----------|
| JPA/Hibernate Starter | 🔴 Alta | veld-jpa-starter con @Transactional integrado | 🔲 Planificado | 0% |
| R2DBC Reactive | 🟡 Media | veld-r2dbc para apps reactivas | 🔲 Planificado | 0% |
| Redis Starter | 🟢 Baja | veld-redis-starter con @Cacheable Redis | 🔲 Planificado | 0% |
| Flyway/Liquibase | 🟡 Media | Auto-migración en startup | 🔲 Planificado | 0% |
| MongoDB Starter | 🟡 Media | Integración con MongoDB reactive | 🔲 Planificado | 0% |

### 2.2 Web & APIs

| Feature | Prioridad | Descripción | Estado | Progreso |
|---------|-----------|-------------|--------|----------|
| HTTP Server (Undertow) | 🔴 Alta | veld-web-undertow con @Get, @Post | 🔲 Planificado | 0% |
| GraphQL Starter | 🟡 Media | @GraphQLQuery con codegen compile-time | 🔲 Planificado | 0% |
| gRPC Integration | 🟡 Media | veld-grpc con stubs generados en compile-time | 🔲 Planificado | 0% |
| WebSocket Support | 🟢 Baja | @WebSocketEndpoint | 🔲 Planificado | 0% |

### 2.3 Messaging & Streaming

| Feature | Prioridad | Descripción | Estado | Progreso |
|---------|-----------|-------------|--------|----------|
| Kafka Starter | 🔴 Alta | veld-kafka con @KafkaListener compile-time | 🔲 Planificado | 0% |
| RabbitMQ Starter | 🟡 Media | @RabbitListener sin reflection | 🔲 Planificado | 0% |
| AWS SQS/SNS | 🟡 Media | Integración nativa cloud | 🔲 Planificado | 0% |
| NATs/JetStream | 🟢 Baja | Niche, pero para performance-critical | 🔲 Planificado | 0% |

---

## Fase 3: Diferenciación y Liderazgo (2027)

### 3.1 Native Cloud-Native

| Feature | Descripción | Por qué es único | Estado | Progreso |
|---------|-------------|------------------|--------|----------|
| Cost Optimizer | Analiza grafo en compile-time y sugiere optimizaciones de costos cloud | Solo posible con metadata completa del grafo | 🔲 Planificado | 0% |
| Polyglot Integration | Genera stubs para Go/Rust/Python basado en grafo de Veld | Bytecode analysis permite codegen cross-language | 🔲 Planificado | 0% |
| Dead Code Elimination | Si un bean no es referenciado, Veld elimina su bytecode (tree-shaking) | Spring no puede (usa reflection) | 🔲 Planificado | 0% |
| Startup Predictor | En compile-time, calcula startup time exacto | Medición real, no estimación | 🔲 Planificado | 0% |
| Security Audit | En compile-time, detecta vulnerabilidades de seguridad | Análisis estático completo | 🔲 Planificado | 0% |

### 3.2 AI/ML Integration

| Feature | Descripción | Estado | Progreso |
|---------|-------------|--------|----------|
| Model Serving | veld-ml con @Model para servir modelos ONNX/TensorFlow sin overhead | 🔲 Planificado | 0% |
| Feature Flags ML | @MLFeatureFlag que usa modelo para decidir feature activation | 🔲 Planificado | 0% |
| Auto-Scaling Hints | Basado en grafo, sugiere escalado bajo carga | 🔲 Planificado | 0% |

### 3.3 Developer Portal (Veld Cloud)

| Feature | Descripción | Estado | Progreso |
|---------|-------------|--------|----------|
| Veld Studio | Web app donde subes tu Veld.class y visualizas grafo, performance, security issues | 🔲 Planificado | 0% |
| Marketplace | Comunidad contribuye veld-starters auditados | 🔲 Planificado | 0% |
| Performance Simulator | Simula carga en tu grafo antes de deploy | 🔲 Planificado | 0% |

---

## Fase 4: Comunidad y Ecosistema (Paralelo a todas las fases)

### 4.1 Comunidad & Adopción

| Métrica | Objetivo | Timeline | Actual | Estado |
|---------|----------|----------|--------|--------|
| Contribuidores | 100+ | 12 meses | TBD | 🔲 En progreso |
| Case Studies Enterprise | 3+ | 9 meses | TBD | 🔲 En progreso |
| Descargas/mes | 50k+ | 18 meses | TBD | 🔲 En progreso |
| Discord miembros | 5k | 6 meses | TBD | 🔲 En progreso |
| Conference Talks | 10+ | 12 meses | TBD | 🔲 En progreso |
| Proyectos Gradle | 100+ | 12 meses | TBD | 🔲 En progreso |

### 4.2 Governance & Soporte

| Feature | Timeline | Descripción | Estado |
|---------|----------|-------------|--------|
| Open Governance | 12 meses | Crear Veld Foundation | 🔲 Planificado |
| Commercial Support | 18 meses | Veld Enterprise con SLA | 🔲 Planificado |
| Training & Certificación | 24 meses | "Veld Certified Developer" | 🔲 Planificado |

---

## 12 Meses a 24 Meses: Visión de Largo Plazo

### Veld 3.0 (2028): El Runtime de Microservicios Nativo

En lugar de generar solo `Veld.class`, genera un ejecutable nativo completo:

```bash
# En 2028:
mvn veld:build-native
# Genera: order-service.veld (binario de 5MB, startup 0.05ms)
./order-service.veld --port=8080
```

Inspirado en GraalVM Native Image, pero optimizado específicamente para el grafo Veld (no genérico como Graal).

---

## KPIs de Éxito del Roadmap

### Corte de 12 Meses (Diciembre 2026)

| Métrica | Objetivo | Actual | Estado |
|---------|----------|--------|--------|
| Descargas/mes | 10,000+ | TBD | 🔲 En progreso |
| Contribuidores activos | 50+ | TBD | 🔲 En progreso |
| Adopciones en producción | 3+ | TBD | 🔲 En progreso |
| Proyectos Gradle | 100+ | TBD | 🔲 En progreso |
| Plugin IntelliJ ratings | 500+ | TBD | 🔲 En progreso |
| Bugs críticos post-release | 0 | TBD | 🔲 En progreso |
| JaCoCo coverage | >95% | TBD | 🔲 En progreso |

### Corte de 24 Meses (Diciembre 2027)

| Métrica | Objetivo | Actual | Estado |
|---------|----------|--------|--------|
| Descargas/mes | 100,000+ | TBD | 🔲 En progreso |
| Contribuidores | 200+ | TBD | 🔲 En progreso |
| Empresas en case studies | 10+ | TBD | 🔲 En progreso |
| Feature parity Spring Boot Web | Completo | TBD | 🔲 En progreso |
| Veld 3.0 native binary | Alpha | TBD | 🔲 En progreso |

---

## Recomendación de Inversión de Esfuerzo

Distribución de tiempo del maintainer principal `@yasmramos`:

| Área | % Esfuerzo | Razón |
|------|------------|-------|
| Core DX (IntelliJ, Gradle, errores) | 40% | Bloquea adopción masiva |
| Testing & QA | 20% | Garantiza producción-ready |
| Comunidad (reviews, Discord, blog) | 15% | Escalabilidad del proyecto |
| Integraciones (JPA, Kafka, Undertow) | 15% | Paridad funcional |
| Innovación (graph viz, polyglot) | 10% | Diferenciación |

**Recomendación:** No trabajar solo. Nombrar 2-3 maintainers clave en Fase 1:
- DX (IntelliJ, Gradle, CLI)
- Testing (@VeldTest, Mock Injection)
- Integraciones (JPA, Kafka)

---

## Estado General del Proyecto

### v1.0.3 (Actual) - ✅ Completado

- **543 tests pasando** en todos los módulos
- **Dependency Graph Visualization** completamente funcional
  - DependencyGraph class
  - DependencyNode class
  - DotExporter (Graphviz)
  - JsonExporter (con metadata)
  - Root/Leaf detection
  - Cycle detection
- **Documentación completa** con 64 anotaciones
- **Veld API documentada** con todos los métodos

### Progreso General

```
Fase 1: [██████░░░░░░░░░░░░░░░░░░░░░░░] 5% - Solo foundation
Fase 2: [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0% - No iniciado
Fase 3: [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0% - No iniciado
Fase 4: [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0% - No iniciado
```

---

## Changelog de Roadmap

| Fecha | Versión | Cambios |
|-------|---------|---------|
| 2025-12-29 | 1.0.3 | Versión inicial del roadmap |
| | | |
| | | |

---

## Cómo Contribuir al Roadmap

1. **Revisa los issues** etiquetados como "roadmap" en GitHub
2. **Propón nuevas features** mediante GitHub Discussions
3. **Implementa features** de las fases planificadas
4. **Ayuda con documentación** y ejemplos
5. **Reporta bugs** y sugiere mejoras de DX

---

## Referencias

- **Repositorio:** https://github.com/yasmramos/Veld
- **Documentación:** https://github.com/yasmramos/Veld/tree/develop/docs
- **CHANGELOG:** https://github.com/yasmramos/Veld/blob/develop/CHANGELOG.md
- **Issues:** https://github.com/yasmramos/Veld/issues

---

**Veld** - Dependency Injection at the speed of direct method calls.
