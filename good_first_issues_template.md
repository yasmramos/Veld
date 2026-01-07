# Good First Issues para Veld Framework

Este archivo contiene los 13 issues listos para crear en GitHub.

---

## Issue 1: Documentación de APIs del Módulo Runtime

**Título:** `[Docs] Documentar APIs del módulo veld-runtime`

**Descripción:**

El módulo `veld-runtime` contiene múltiples clases de infraestructura que carecen de documentación Javadoc adecuada. Las clases como `ValueResolver`, `EventBus`, `ScopeRegistry`, `ConditionEvaluator` y los exportadores de grafo (`DependencyGraph`, `DotExporter`, `JsonExporter`) tienen funcionalidad importante pero documentación incompleta.

**Criterios de aceptación:**

- [ ] Todas las clases públicas tienen documentación Javadoc con descripción de propósito
- [ ] Cada método público está documentado con parámetros, retorno y excepciones
- [ ] Los ejemplos de uso están incluidos donde es apropiado
- [ ] El comando `./mvnw javadoc:javadoc` genera documentación sin errores

**Archivos relacionados:**
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/value/ValueResolver.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/event/EventBus.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/scope/ScopeRegistry.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/condition/ConditionEvaluator.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/DependencyGraph.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/DotExporter.java`
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/JsonExporter.java`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 3-5 horas  
**Etiquetas:** `documentation`, `good first issue`, `help wanted`

---

## Issue 2: Tests Unitarios para ValueResolver

**Título:** `[Test] Agregar tests unitarios para ValueResolver`

**Descripción:**

El `ValueResolver` es responsable de resolver expresiones de propiedades pero tiene cobertura de pruebas limitada.

**Criterios de aceptación:**

- [ ] Tests para resolución de propiedades existentes
- [ ] Tests para propiedades con valores por defecto
- [ ] Tests para propiedades sin valor por defecto
- [ ] Tests para expresiones con tipos primitivos
- [ ] Tests para expresiones mal formateadas

**Archivo a modificar:**
- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/value/ValueResolver.java`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `testing`, `good first issue`, `help wanted`

---

## Issue 3: Guía de Inicio Rápido Completa

**Título:** `[Docs] Crear guía de inicio rápido con ejemplos completos`

**Descripción:**

El README.md contiene una sección básica de "Quick Start". Se necesita una guía completa que demuestre todas las características importantes.

**Criterios de aceptación:**

- [ ] Guía con al menos 8 secciones cubriendo diferentes características
- [ ] Cada sección incluye código funcional
- [ ] Todos los ejemplos han sido verificados funcionando

**Secciones sugeridas:**
1. Inyección constructor básica
2. Field injection (campos privados)
3. Method injection
4. Scopes: Singleton y Prototype
5. Lifecycle callbacks
6. Uso básico del EventBus
7. Anotaciones cualificadas (@Named)
8. Configuración de propiedades (@Value)

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 3-5 horas  
**Etiquetas:** `documentation`, `good first issue`, `help wanted`

---

## Issue 4: Tests para DependencyNode

**Título:** `[Test] Agregar tests para DependencyNode`

**Descripción:**

La clase `DependencyNode` carece de tests unitarios dedicados para la funcionalidad de visualización del grafo de dependencias.

**Criterios de aceptación:**

- [ ] Tests para creación de DependencyNode con diferentes scopes
- [ ] Tests para agregar y consultar dependencias de un nodo
- [ ] Tests para la serialización correcta a formatos exportables

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `testing`, `good first issue`, `help wanted`

---

## Issue 5: Documentación de Anotaciones de Resiliencia

**Título:** `[Docs] Documentar anotaciones del módulo veld-resilience`

**Descripción:**

Las anotaciones de resiliencia (@Retry, @CircuitBreaker, @RateLimiter, @Bulkhead, @Timeout) carecen de documentación Javadoc detallada.

**Criterios de aceptación:**

- [ ] Cada anotación tiene documentación Javadoc completa
- [ ] La documentación incluye descripción de cada parámetro con valores por defecto
- [ ] La documentación incluye ejemplos de uso

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `documentation`, `good first issue`, `help wanted`

---

## Issue 6: Tests para ScopeRegistry

**Título:** `[Test] Agregar tests para ScopeRegistry`

**Descripción:**

El `ScopeRegistry` carece de tests unitarios dedicados para el registro y resolución de scopes personalizados.

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `testing`, `good first issue`, `help wanted`

---

## Issue 7: Actualización de Dependencias

**Título:** `[Chore] Actualizar dependencias del proyecto`

**Descripción:**

El archivo `pom.xml` tiene dependencias que podrían actualizarse a versiones más recientes manteniendo compatibilidad.

**Criterios de aceptación:**

- [ ] JaCoCo actualizado a la última versión estable
- [ ] Todas las pruebas pasan después de la actualización
- [ ] Los cambios documentados en CHANGELOG.md

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `chore`, `dependencies`, `good first issue`, `help wanted`

---

## Issue 8: Mejora del README de Spring Boot Starter

**Título:** `[Docs] Mejorar documentación de veld-spring-boot-starter`

**Descripción:**

El README no demuestra todos los escenarios de integración posibles entre Veld y Spring Boot.

**Criterios de aceptación:**

- [ ] Ejemplos de beans Veld en aplicación Spring Boot
- [ ] Documentación de configuración mixta
- [ ] Sección de troubleshooting

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 2-4 horas  
**Etiquetas:** `documentation`, `integration`, `good first issue`, `help wanted`

---

## Issue 9: Normalización de Plantillas de Pull Request

**Título:** `[Maintenance] Normalizar plantillas de Pull Request`

**Descripción:**

Las plantillas de Pull Request tienen diferentes formatos. Normalizar mejorará la consistencia.

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `maintenance`, `documentation`, `good first issue`, `help wanted`

---

## Issue 10: Ejemplo del Módulo de Resiliencia

**Título:** `[Example] Crear ejemplo funcional de anotaciones de resiliencia`

**Descripción:**

El módulo `veld-resilience` no tiene un ejemplo funcional completo que demuestre @Retry, @CircuitBreaker, @RateLimiter, etc.

**Criterios de aceptación:**

- [ ] Ejemplo funcional con simulación de servicio externo
- [ ] Demostración de @Retry con diferentes configuraciones
- [ ] Demostración de @CircuitBreaker con transición de estados

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 4-6 horas  
**Etiquetas:** `example`, `documentation`, `good first issue`, `help wanted`

---

## Issue 11: Ejemplo de Conditional Registration

**Título:** `[Example] Crear ejemplos de registro condicional`

**Descripción:**

El sistema de registro condicional (@ConditionalOnProperty, @ConditionalOnMissingBean, etc.) carece de ejemplos dedicados.

**Criterios de aceptación:**

- [ ] Ejemplo funcional para @ConditionalOnProperty
- [ ] Ejemplo funcional para @ConditionalOnMissingBean
- [ ] Ejemplo funcional para @ConditionalOnClass
- [ ] Ejemplo funcional para @ConditionalOnBean

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 3-4 horas  
**Etiquetas:** `example`, `documentation`, `good first issue`, `help wanted`

---

## Issue 12: Mejora del Manejo de Errores en EventBus

**Título:** `[Enhancement] Mejorar manejo de errores en EventBus`

**Descripción:**

El `EventBus` podría mejorar su manejo de errores con mensajes más descriptivos.

**Criterios de aceptación:**

- [ ] El EventBus loguea advertencias cuando un suscriptor tiene firma inválida
- [ ] Los mensajes de error incluyen información suficiente para debugging

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `enhancement`, `error-handling`, `good first issue`, `help wanted`

---

## Issue 13: Mejora de Mensajes de Error en ConditionEvaluator

**Título:** `[Enhancement] Mejorar mensajes de error en ConditionEvaluator`

**Descripción:**

El `ConditionEvaluator` podría proporcionar mensajes de error más descriptivos cuando las condiciones fallan.

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `enhancement`, `error-handling`, `good first issue`, `help wanted`

---

## Resumen

| # | Issue | Dificultad | Tiempo |
|---|-------|------------|--------|
| 1 | Documentación de APIs del Módulo Runtime | 🟢 | 3-5h |
| 2 | Tests Unitarios para ValueResolver | 🟢 | 2-3h |
| 3 | Guía de Inicio Rápido Completa | 🟢 | 3-5h |
| 4 | Tests para DependencyNode | 🟢 | 1-2h |
| 5 | Documentación de Anotaciones de Resiliencia | 🟢 | 2-3h |
| 6 | Tests para ScopeRegistry | 🟢 | 1-2h |
| 7 | Actualización de Dependencias | 🟢 | 1-2h |
| 8 | Mejora del README de Spring Boot Starter | 🟢 | 2-4h |
| 9 | Normalización de Plantillas de Pull Request | 🟢 | 1-2h |
| 10 | Ejemplo del Módulo de Resiliencia | 🟡 | 4-6h |
| 11 | Ejemplo de Conditional Registration | 🟡 | 3-4h |
| 12 | Mejora del Manejo de Errores en EventBus | 🟡 | 2-3h |
| 13 | Mejora de Mensajes de Error en ConditionEvaluator | 🟡 | 2-3h |

**Total:** 13 issues (9 principiante, 4 intermedio)
