# 🌿 Convenciones de Nombres de Ramas - Veld Framework

## 📋 Resumen

Este documento define las convenciones de nombres para las ramas del proyecto Veld Framework. Estas convenciones aseguran consistencia, facilitan la identificación de tipos de cambios y mejoran la organización del flujo de trabajo.

## 🎯 Objetivos

- ✅ Consistencia en nombres de ramas
- ✅ Identificación rápida del tipo de cambio
- ✅ Facilitar la automatización con Git hooks
- ✅ Mejorar la trazabilidad de cambios
- ✅ Simplificar la generación automática de PRs

## 🌿 Estructura de Nombres

### Formato General
```
<tipo>/<descripcion-corta>
```

### Tipos de Ramas

#### 🆕 **Feature Branches** (`feature/`)
Para nuevas funcionalidades y características.

**Patrón:** `feature/nombre-funcionalidad`
**Ejemplos:**
```
feature/dependency-injection-container
feature/metrics-monitoring-api
feature/spring-boot-integration
feature/custom-annotation-processor
```

**Reglas:**
- Usar sustantivos descriptivos
- Separar palabras con guiones (`-`)
- Máximo 50 caracteres
- Describir la funcionalidad, no el trabajo técnico

#### 🐛 **Bug Fix Branches** (`bugfix/`)
Para corrección de bugs no críticos.

**Patrón:** `bugfix/descripcion-del-bug`
**Ejemplos:**
```
bugfix/npe-in-container-initialization
bugfix/memory-leak-in-dependency-resolution
bugfix/wrong-scope-resolution-for-prototype
bugfix/incorrect-annotation-processing-order
```

**Reglas:**
- Describir el problema, no la solución
- Incluir el tipo de error (NPE, memory leak, etc.)
- Mencionar el componente afectado

#### 🔥 **Hotfix Branches** (`hotfix/`)
Para correcciones urgentes en producción.

**Patrón:** `hotfix/descripcion-critica`
**Ejemplos:**
```
hotfix/critical-security-vulnerability
hotfix/production-outage-fix
hotfix/data-corruption-prevention
hotfix/performance-degradation-critical
```

**Reglas:**
- Usar solo para situaciones críticas
- Prefijos que indiquen urgencia
- Máximo 40 caracteres
- Describir el impacto crítico

#### ♻️ **Refactor Branches** (`refactor/`)
Para refactorización y mejoras de código.

**Patrón:** `refactor/area-mejorada`
**Ejemplos:**
```
refactor/container-initialization-logic
refactor/dependency-resolution-algorithm
refactor/annotation-processor-architecture
refactor/memory-management-optimization
```

**Reglas:**
- Especificar qué se está refactorizando
- Enfocarse en el área, no en la técnica
- Indicar el beneficio esperado

#### 📚 **Documentation Branches** (`docs/`)
Para actualizaciones de documentación.

**Patrón:** `docs/tema-documentacion`
**Ejemplos:**
```
docs/api-reference-update
docs/getting-started-guide
docs/performance-tuning-tips
docs/contribution-guidelines-update
```

#### ✅ **Test Branches** (`test/`)
Para mejoras en testing y cobertura.

**Patrón:** `test/area-de-testing`
**Ejemplos:**
```
test/integration-tests-for-container
test/performance-benchmarks-expansion
test/mock-object-optimization
test/test-coverage-improvement
```

#### 🔧 **Chore Branches** (`chore/`)
Para tareas de mantenimiento y configuración.

**Patrón:** `chore/tipo-tarea`
**Ejemplos:**
```
chore/dependency-updates
chore/build-configuration-optimization
chore/code-coverage-reports
chore/release-process-automation
```

#### 🎨 **Style Branches** (`style/`)
Para cambios de formato y estilo de código.

**Patrón:** `style/tipo-estilo`
**Ejemplos:**
```
style/code-formatting-standards
style/naming-convention-updates
style/documentation-style-guide
```

#### ⚡ **Performance Branches** (`perf/`)
Para optimizaciones de rendimiento.

**Patrón:** `perf/area-optimizada`
**Ejemplos:**
```
perf/container-startup-time
perf/memory-allocation-optimization
perf/dependency-resolution-speed
perf/annotation-processing-efficiency`
```

#### 🚀 **Release Branches** (`release/`)
Para preparación de releases.

**Patrón:** `release/version-numero`
**Ejemplos:**
```
release/v1.2.0
release/v1.1.5
release/v2.0.0-rc1
```

## 🚫 Restricciones y Reglas

### ❌ No Permitido
```
- Nombres con espacios
- Caracteres especiales (!@#$%^&*)
- Mayúsculas
- Nombres de más de 60 caracteres
- Nombres sin prefijo de tipo
- Nombres genéricos (feature/test, chore/update)
```

### ✅ Ejemplos Incorrectos
```
❌ NewFeature (sin prefijo, mayúsculas)
❌ bug fix for memory (espacios)
❌ hotfix! (carácter especial)
❌ feature_very_long_name_that_exceeds_the_character_limit_and_should_not_be_used
❌ update (genérico, sin prefijo)
```

### ✅ Ejemplos Correctos
```
✅ feature/dependency-injection-container
✅ bugfix/memory-leak-resolution
✅ hotfix/security-patch-critical
✅ docs/api-documentation-update
```

## 🛠️ Herramientas y Automatización

### Git Hooks
- **Pre-commit**: Valida nombres de ramas antes de commit
- **Pre-push**: Verifica convenciones antes de push

### Scripts de Ayuda
- **create-branch.sh**: Crea ramas con el formato correcto
- **validate-branch-name.sh**: Valida nombres de ramas existentes

### CI/CD Integration
- Los workflows validan automáticamente los nombres de ramas
- PRs con nombres incorrectos reciben comentarios automáticos

## 📊 Ejemplos por Contexto

### Nuevas Funcionalidades
```
feature/rest-api-endpoints
feature/configuration-management-system
feature/multi-module-support
feature/custom-scope-definitions
```

### Corrección de Bugs
```
bugfix/null-pointer-in-scanner
bugfix/incorrect-binding-resolution
bugfix/circular-dependency-detection
bugfix/wrong-qualifier-matching
```

### Mejoras de Performance
```
perf/container-initialization-speed
perf/memory-usage-optimization
perf/annotation-processing-efficiency
perf/dependency-graph-traversal
```

### Documentación
```
docs/api-reference-completeness
docs/getting-started-tutorial
docs/architecture-diagrams-update
docs/migration-guide-v2
```

## 🔄 Flujo de Trabajo Recomendado

### 1. Crear Rama
```bash
git checkout -b feature/nueva-funcionalidad
```

### 2. Desarrollo
- Implementar cambios siguiendo las convenciones
- Hacer commits con mensajes convencionales

### 3. Crear Pull Request
- El nombre de la rama se usa como título base
- Se agrega contexto adicional en la descripción

### 4. Review y Merge
- Después del review, hacer squash merge
- La rama se elimina automáticamente

## 🧪 Validación Automática

### Scripts de Validación
```bash
# Validar nombre de rama actual
./scripts/validate-branch-name.sh

# Crear nueva rama con formato correcto
./scripts/create-branch.sh feature mi-nueva-funcionalidad
```

### Git Hook Setup
```bash
# Instalar hooks automáticamente
./scripts/setup-git-hooks.sh
```

## 📈 Beneficios de las Convenciones

### Para Desarrolladores
- ✅ **Claridad**: Inmediatamente se sabe el tipo de cambio
- ✅ **Consistencia**: Todos siguen el mismo patrón
- ✅ **Automatización**: Scripts y hooks facilitan el trabajo
- ✅ **Búsqueda**: Fácil encontrar ramas relacionadas

### Para Reviewers
- ✅ **Contexto**: El nombre de rama proporciona contexto inicial
- ✅ **Priorización**: Hotfixes se identifican rápidamente
- ✅ **Organización**: Ramas agrupadas por tipo

### Para DevOps
- ✅ **Automatización**: Workflows pueden actuar basado en tipos
- ✅ **Monitoreo**: Métricas por tipo de rama
- ✅ **Release Management**: Ramas de release fáciles de identificar

## 📝 Checklist para Ramas

### Antes de Crear
- [ ] ¿El nombre sigue el patrón `<tipo>/<descripcion>`?
- [ ] ¿La descripción es clara y concisa?
- [ ] ¿No excede 60 caracteres?
- [ ] ¿Usa solo minúsculas y guiones?

### Antes del Push
- [ ] ¿La rama tiene al menos un commit?
- [ ] ¿Los commits siguen convenciones?
- [ ] ¿El código pasa todos los tests?
- [ ] ¿El nombre sigue las convenciones?

## 🚨 Excepciones

### Ramas Temporales
```
temp/experiment-feature-x
temp/debug-performance-issue
temp/test-new-configuration
```

### Ramas de Equipo Específico
```
team/devops/ci-cd-improvements
team/docs/api-reference-overhaul
team/perf/optimization-phase-2
```

## 📞 Soporte

### Preguntas Frecuentes

**¿Qué pasa si mi rama no encaja en ninguna categoría?**
- Usa `chore/` para tareas de mantenimiento
- Usa `refactor/` para mejoras de código

**¿Puedo usar guiones bajos en lugar de guiones?**
- No, usa solo guiones (`-`) para consistencia

**¿Qué pasa con ramas muy largas?**
- Usa abreviaciones conocidas (API, DI, IoC, etc.)
- Enfócate en la esencia del cambio

**¿Cómo manejo múltiples issues en una rama?**
- Agrupa por funcionalidad principal
- Usa nombres que reflejen el objetivo común

---

## 🔗 Referencias

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
- [GitHub Flow](https://docs.github.com/en/get-started/using-github/github-flow)

---

*Última actualización: 2025-12-21*
*Versión del documento: 1.0*