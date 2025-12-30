# Veld Framework - GitHub Project Template

## Project Overview

**Project Name:** Veld Framework Development
**Goal:** Convert Veld into the standard framework for ultra-fast microservices (<10ms startup)
**Timeline:** 2025-2027
**Link:** https://github.com/users/yasmramos/projects/1

---

## Column Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│  TO DO          │  IN PROGRESS    │  FASE 1  │  FASE 2  │   DONE   │
├─────────────────┼─────────────────┼──────────┼──────────┼──────────┤
│ Backlog items   │ Active sprint   │ Phase 1  │ Phase 2  │ Completed│
│ Future features │ items           │ items    │ items    │ items    │
└─────────────────┴─────────────────┴──────────┴──────────┴──────────┘
```

---

## Pre-populated Issues for Project

### FASE 1: Consolidación del Core (Q1-Q2 2026)

#### 1.1 Developer Experience (DX)

- [ ] Create IntelliJ IDEA Plugin #101
  - Status: To Do
  - Labels: dx, plugin, priority-critica
  - Assignee: @yasmramos
  - Milestone: Fase 1
  
- [ ] Implement Gradle Plugin #102
  - Status: To Do
  - Labels: dx, gradle, priority-alta
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Humanize Error Messages #103
  - Status: To Do
  - Labels: dx, errors, priority-media
  - Description: Replace ASMException with helpful messages like "Circular dependency detected: A → B → A. Use @Lazy to break it."
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Implement Live Reload #104
  - Status: To Do
  - Labels: dx, hot-reload, priority-media
  - Description: Hot reload de beans en desarrollo (similar a Spring DevTools)
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Create CLI Tool #105
  - Status: To Do
  - Labels: dx, cli, priority-baja
  - Description: `veld init my-service` para generar proyecto boilerplate
  - Assignee: 
  - Milestone: Fase 1

#### 1.2 Testing & Quality

- [ ] Implement @VeldTest Annotation #111
  - Status: To Do
  - Labels: testing, annotation, priority-alta
  - Description: Runner de tests similar a @SpringBootTest con VeldTestContext
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Implement Mock Injection #112
  - Status: To Do
  - Labels: testing, mock, priority-media
  - Description: @MockBean para reemplazar componentes en tests
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Setup Performance Regression Tests #113
  - Status: To Do
  - Labels: testing, performance, priority-media
  - Description: Pipeline CI que falla si latency sube >5%
  - Assignee: 
  - Milestone: Fase 1

#### 1.3 Observabilidad Enterprise

- [ ] Integrate Micrometer #121
  - Status: To Do
  - Labels: observability, metrics, priority-media
  - Description: veld-metrics-micrometer para Prometheus, Datadog, New Relic
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Implement Distributed Tracing #122
  - Status: To Do
  - Labels: observability, tracing, priority-baja
  - Description: @Trace para OpenTelemetry + mostrar grafo en Jaeger
  - Assignee: 
  - Milestone: Fase 1
  
- [ ] Add Health Checks Endpoint #123
  - Status: To Do
  - Labels: observability, health, priority-baja
  - Description: /veld/health endpoint + readiness/liveness indicators
  - Assignee: 
  - Milestone: Fase 1

---

### FASE 2: Expansión del Ecosistema (Q3-Q4 2026)

#### 2.1 Data & Persistence

- [ ] Create JPA/Hibernate Starter #201
  - Status: To Do
  - Labels: data, jpa, priority-alta
  - Description: veld-jpa-starter con @Transactional integrado
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Implement R2DBC Reactive #202
  - Status: To Do
  - Labels: data, reactive, priority-media
  - Description: veld-r2dbc para apps reactivas
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Create Redis Starter #203
  - Status: To Do
  - Labels: data, redis, priority-baja
  - Description: veld-redis-starter con @Cacheable Redis
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Add Flyway/Liquibase Integration #204
  - Status: To Do
  - Labels: data, migration, priority-baja
  - Description: Auto-migración en startup
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Create MongoDB Starter #205
  - Status: To Do
  - Labels: data, mongodb, priority-media
  - Description: Integración con MongoDB reactive
  - Assignee: 
  - Milestone: Fase 2

#### 2.2 Web & APIs

- [ ] Implement HTTP Server (Undertow) #211
  - Status: To Do
  - Labels: web, http, priority-alta
  - Description: veld-web-undertow con @Get, @Post
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Create GraphQL Starter #212
  - Status: To Do
  - Labels: web, graphql, priority-media
  - Description: @GraphQLQuery con codegen compile-time
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Implement gRPC Integration #213
  - Status: To Do
  - Labels: web, grpc, priority-media
  - Description: veld-grpc con stubs generados en compile-time
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Add WebSocket Support #214
  - Status: To Do
  - Labels: web, websocket, priority-baja
  - Description: @WebSocketEndpoint
  - Assignee: 
  - Milestone: Fase 2

#### 2.3 Messaging & Streaming

- [ ] Create Kafka Starter #221
  - Status: To Do
  - Labels: messaging, kafka, priority-alta
  - Description: veld-kafka con @KafkaListener compile-time
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Implement RabbitMQ Starter #222
  - Status: To Do
  - Labels: messaging, rabbitmq, priority-media
  - Description: @RabbitListener sin reflection
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Add AWS SQS/SNS Integration #223
  - Status: To Do
  - Labels: messaging, aws, priority-baja
  - Description: Integración nativa cloud
  - Assignee: 
  - Milestone: Fase 2
  
- [ ] Implement NATs/JetStream Support #224
  - Status: To Do
  - Labels: messaging, nats, priority-baja
  - Description: Niche, pero para performance-critical
  - Assignee: 
  - Milestone: Fase 2

---

### FASE 3: Diferenciación y Liderazgo (2027)

#### 3.1 Native Cloud-Native

- [ ] Implement Cost Optimizer #301
  - Status: To Do
  - Labels: native, cloud, priority-alta
  - Description: Analiza grafo en compile-time y sugiere optimizaciones de costos cloud
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Create Polyglot Integration #302
  - Status: To Do
  - Labels: native, polyglot, priority-media
  - Description: Genera stubs para Go/Rust/Python basado en grafo de Veld
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Implement Dead Code Elimination #303
  - Status: To Do
  - Labels: native, optimization, priority-media
  - Description: Tree-shaking de beans no referenciados
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Add Startup Predictor #304
  - Status: To Do
  - Labels: native, performance, priority-baja
  - Description: Calcula startup time exacto en compile-time
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Implement Security Audit #305
  - Status: To Do
  - Labels: native, security, priority-baja
  - Description: Análisis estático completo de vulnerabilidades
  - Assignee: 
  - Milestone: Fase 3

#### 3.2 AI/ML Integration

- [ ] Create Model Serving Module #311
  - Status: To Do
  - Labels: ai, ml, priority-media
  - Description: veld-ml con @Model para servir modelos ONNX/TensorFlow
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Implement ML Feature Flags #312
  - Status: To Do
  - Labels: ai, feature-flags, priority-baja
  - Description: @MLFeatureFlag que usa modelo para feature activation
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Add Auto-Scaling Hints #313
  - Status: To Do
  - Labels: ai, scaling, priority-baja
  - Description: Sugiere escalado basado en grafo
  - Assignee: 
  - Milestone: Fase 3

#### 3.3 Developer Portal (Veld Cloud)

- [ ] Create Veld Studio #321
  - Status: To Do
  - Labels: portal, web-ui, priority-alta
  - Description: Web app para visualizar grafo, performance, security issues
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Build Marketplace #322
  - Status: To Do
  - Labels: portal, community, priority-media
  - Description: Comunidad contribuye veld-starters auditados
  - Assignee: 
  - Milestone: Fase 3
  
- [ ] Implement Performance Simulator #323
  - Status: To Do
  - Labels: portal, performance, priority-baja
  - Description: Simula carga en grafo antes de deploy
  - Assignee: 
  - Milestone: Fase 3

---

### Completed Items (v1.0.3)

#### Infrastructure & Core

- [x] DependencyGraph class #001
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX
  
- [x] DependencyNode class #002
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX
  
- [x] DotExporter (Graphviz) #003
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX
  
- [x] JsonExporter (con metadata) #004
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX
  
- [x] Root/Leaf detection #005
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX
  
- [x] Cycle detection #006
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX
  
- [x] 543 tests passing #007
  - Status: Done
  - Completed: 2025-12-29
  - PR: #XXX

#### Documentation

- [x] Complete annotations reference #008
  - Status: Done
  - Completed: 2025-12-29
  - Description: 64 anotaciones documentadas
  
- [x] Complete Veld API documentation #009
  - Status: Done
  - Completed: 2025-12-29
  - Description: Todos los métodos documentados
  
- [x] Real DOT/JSON examples #010
  - Status: Done
  - Completed: 2025-12-29
  - Description: Ejemplos reales de output
  
- [x] Missing modules documentation #011
  - Status: Done
  - Completed: 2025-12-29
  - Description: veld-benchmark, veld-example, veld-spring-boot-example, veld-test
  
- [x] Create ROADMAP.md #012
  - Status: Done
  - Completed: 2025-12-29
  - Description: Roadmap estratégico independiente

---

## Labels to Create

```
priority-alta      color: #d73a4a  (Rojo)
priority-media     color: #fbca04  (Amarillo)
priority-baja      color: #0e8a16  (Verde)
dx                 color: #1d76db  (Azul)
testing            color: #7057ff  (Morado)
data               color: #ff7b72  (Rojo claro)
web                color: #a2eeef  (Cyan)
messaging          color: #f9c806  (Naranja)
native             color: #6e5494  (Púrpura)
ai                 color: #00c7b7  (Teal)
portal             color: #5319e7  (Violeta)
observability      color: #b4f072  (Lima)
fase-1             color: #fef2c0  (Amarillo claro)
fase-2             color: #bfdadc  (Cyan claro)
fase-3             color: #e4e669  (Amarillo oscuro)
bug                color: #d73a4a  (Rojo)
enhancement        color: #a2eeef  (Cyan)
documentation      color: #0075ca  (Azul oscuro)
```

---

## Milestones

### Fase 1: Consolidación del Core
- Due: 2026-06-30
- Budget: 40% del esfuerzo

### Fase 2: Expansión del Ecosistema
- Due: 2026-12-31
- Budget: 25% del esfuerzo

### Fase 3: Diferenciación
- Due: 2027-12-31
- Budget: 20% del esfuerzo

### Fase 4: Comunidad
- Due: 2027-12-31
- Budget: 15% del esfuerzo

---

## Commands to Create GitHub Project

### Option 1: Manual Creation

1. Go to https://github.com/users/yasmramos/projects
2. Click "New project"
3. Select "Board" template
4. Name: "Veld Framework Development"
5. Add columns: "To Do", "In Progress", "Fase 1", "Fase 2", "Done"
6. Add issues from this template

### Option 2: GitHub CLI (Local)

```bash
# Install GitHub CLI if not installed
# https://cli.github.com/

# Authenticate
gh auth login

# Create project
gh project create "Veld Framework Development" --template board

# Add columns manually via web interface
```

### Option 3: GitHub API (curl)

```bash
# Set your GitHub token
export GITHUB_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxx"

# Create project
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/yasmramos/Veld/projects \
  -d '{
    "name": "Veld Framework Development",
    "body": "Roadmap para convertir Veld en el framework estándar para microservicios ultrarrápidos (<10ms startup)"
  }'

# Note: You'll need to add columns manually via the web interface
```

---

## Project Stats

### Current Status (v1.0.3)

```
Total Issues: 25
Completed: 12 (48%)
To Do: 10 (40%)
In Progress: 0 (0%)
Fase 1: 8 (32%)
Fase 2: 5 (20%)
Fase 3: 0 (0%)
```

### Progress by Phase

```
Fase 1: [██████████░░░░░░░░░░░░░░] 48% - Lead con v1.0.3
Fase 2: [░░░░░░░░░░░░░░░░░░░░░░░░] 0%  - No iniciado
Fase 3: [░░░░░░░░░░░░░░░░░░░░░░░░] 0%  - No iniciado
```

---

## Review Cadence

- **Weekly:** Review "In Progress" items
- **Monthly:** Move items between columns
- **Quarterly:** Review roadmap and adjust priorities
- **Annual:** Major roadmap review and planning

---

**Last Updated:** 2025-12-29
**Maintainer:** @yasmramos
**Repository:** https://github.com/yasmramos/Veld
**Roadmap:** https://github.com/yasmramos/Veld/blob/develop/ROADMAP.md
**Changelog:** https://github.com/yasmramos/Veld/blob/develop/CHANGELOG.md
