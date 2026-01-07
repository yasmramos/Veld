# Good First Issues para Veld Framework

Este archivo contiene una lista de issues preparados para crear en GitHub. Copia el contenido de cada sección en un issue separado.

---

## Issue 1: Documentación de APIs del Módulo Runtime

**Título:** `[Docs] Documentar APIs del módulo veld-runtime`

**Descripción:**

El módulo `veld-runtime` contiene múltiples clases de infraestructura que carecen de documentación Javadoc adecuada. Las clases como `ValueResolver`, `EventBus`, `ScopeRegistry`, `ConditionEvaluator` y los exportadores de grafo (`DependencyGraph`, `DotExporter`, `JsonExporter`) tienen funcionalidad importante pero documentación incompleta o inexistente.

**Tareas:**

- Documentar Javadoc para `ValueResolver.java`
- Documentar Javadoc para `EventBus.java`
- Documentar Javadoc para `ScopeRegistry.java`
- Documentar Javadoc para `ConditionEvaluator.java`
- Documentar Javadoc para `DependencyGraph.java`
- Documentar Javadoc para `DotExporter.java`
- Documentar Javadoc para `JsonExporter.java`

**Criterios de aceptación:**

- [ ] Todas las clases públicas tienen documentación Javadoc con descripción de propósito
- [ ] Cada método público está documentado con parámetros, retorno y excepciones
- [ ] Los ejemplos de uso están incluidos donde es apropiado
- [ ] Las clases relacionadas tienen enlaces entre sí mediante @see
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

El `ValueResolver` es responsable de resolver expresiones de propiedades como `${property.name:defaultValue}` pero actualmente tiene cobertura de pruebas limitada. Agregar tests exhaustivos mejorará la confianza en esta funcionalidad y documentará el comportamiento esperado del sistema de resolución de valores.

**Criterios de aceptación:**

- [ ] Tests para resolución de propiedades existentes
- [ ] Tests para propiedades con valores por defecto
- [ ] Tests para propiedades sin valor por defecto
- [ ] Tests para expresiones con tipos primitivos (int, boolean, etc.)
- [ ] Tests para expresiones mal formateadas
- [ ] Tests para variables de entorno del sistema

**Pasos:**

1. Crear directorio `veld-runtime/src/test/java/io/github/yasmramos/veld/runtime/value/`
2. Crear clase `ValueResolverTest.java`
3. Agregar tests para cada escenario identificado
4. Ejecutar `./mvnw test` para verificar

**Archivo a modificar:**

- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/value/ValueResolver.java`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `testing`, `good first issue`, `help wanted`

---

## Issue 3: Tests para DependencyNode

**Título:** `[Test] Agregar tests para DependencyNode`

**Descripción:**

La clase `DependencyNode` que representa un nodo en el grafo de dependencias carece de tests unitarios dedicados. Esta clase es fundamental para la funcionalidad de visualización del grafo de dependencias.

**Criterios de aceptación:**

- [ ] Tests para creación de DependencyNode con diferentes scopes
- [ ] Tests para agregar y consultar dependencias de un nodo
- [ ] Tests para verificar la serialización correcta a formatos exportables
- [ ] Tests para edge cases como nodos con nombres de clase complejos

**Archivo a modificar:**

- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/graph/DependencyNode.java`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `testing`, `good first issue`, `help wanted`

---

## Issue 4: Guía de Inicio Rápido Completa

**Título:** `[Docs] Crear guía de inicio rápido con ejemplos completos`

**Descripción:**

El README.md contiene una sección de "Quick Start" básica, pero no demuestra todas las características importantes del framework. Se necesita una guía completa que muestre ejemplos funcionales para diferentes características.

**Criterios de aceptación:**

- [ ] Guía con al menos 8 secciones cubriendo diferentes características
- [ ] Cada sección incluye código funcional y listo para copiar
- [ ] La guía incluye explicación de cada ejemplo
- [ ] Todos los ejemplos han sido verificados funcionando
- [ ] La tabla de contenidos permite navegación fácil

**Secciones sugeridas:**

1. Inyección constructor básica
2. Field injection (campos privados)
3. Method injection
4. Scopes: Singleton y Prototype
5. Lifecycle callbacks (@PostConstruct, @PreDestroy)
6. Uso básico del EventBus
7. Anotaciones cualificadas (@Named)
8. Configuración de propiedades (@Value)

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 3-5 horas  
**Etiquetas:** `documentation`, `good first issue`, `help wanted`

---

## Issue 5: Mejora del Manejo de Errores en EventBus

**Título:** `[Enhancement] Mejorar manejo de errores en EventBus`

**Descripción:**

El `EventBus` actual podría mejorar su manejo de errores cuando se registran suscriptores con firmas de método inválidas o cuando se publican eventos a suscriptores que no pueden procesarlos.

**Criterios de aceptación:**

- [ ] El EventBus loguea advertencias cuando un suscriptor tiene firma inválida
- [ ] Los mensajes de error incluyen información suficiente para debugging
- [ ] El EventBus no lanza excepciones no controladas durante publicación de eventos
- [ ] Los tests verifican el comportamiento del manejo de errores

**Archivo a modificar:**

- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/event/EventBus.java`

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `enhancement`, `error-handling`, `good first issue`, `help wanted`

---

## Issue 6: Documentación de Anotaciones de Resiliencia

**Título:** `[Docs] Documentar anotaciones del módulo veld-resilience`

**Descripción:**

Las anotaciones de resiliencia en el módulo `veld-resilience` (@Retry, @CircuitBreaker, @RateLimiter, @Bulkhead, @Timeout) carecen de documentación Javadoc detallada con ejemplos de uso.

**Criterios de aceptación:**

- [ ] Cada anotación de resiliencia tiene documentación Javadoc completa
- [ ] La documentación incluye descripción de cada parámetro con valores por defecto
- [ ] La documentación incluye ejemplos de uso
- [ ] La documentación diferencia entre comportamiento síncrono y asíncrono

**Archivos a modificar (identificar en `veld-resilience/src/main/java/`):**

- Anotaciones @Retry, @CircuitBreaker, @RateLimiter, @Bulkhead, @Timeout

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `documentation`, `good first issue`, `help wanted`

---

## Issue 7: Ejemplo del Módulo de Resiliencia

**Título:** `[Example] Crear ejemplo funcional de anotaciones de resiliencia`

**Descripción:**

El módulo `veld-resilience` no tiene un ejemplo funcional completo que demuestre cómo usar las anotaciones de resiliencia en un escenario realista.

**Criterios de aceptación:**

- [ ] Ejemplo funcional con simulación de servicio externo
- [ ] Demostración de @Retry con diferentes configuraciones
- [ ] Demostración de @CircuitBreaker con transición de estados
- [ ] Documentación en código de cada configuración
- [ ] El ejemplo compila y ejecuta sin errores

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 4-6 horas  
**Etiquetas:** `example`, `documentation`, `good first issue`, `help wanted`

---

## Issue 8: Actualización de Dependencias

**Título:** `[Chore] Actualizar dependencias del proyecto`

**Descripción:**

El archivo `pom.xml` principal tiene varias dependencias que podrían actualizarse a versiones más recientes mientras mantienen compatibilidad.

**Criterios de aceptación:**

- [ ] JaCoCo actualizado a la última versión estable verificada
- [ ] Verificar compatibilidad de todas las dependencias actualizadas
- [ ] Todas las pruebas pasan después de la actualización
- [ ] El reporte de JaCoCo se genera correctamente
- [ ] Los cambios documentados en CHANGELOG.md

**Archivo a modificar:**

- `pom.xml` raíz

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `chore`, `dependencies`, `good first issue`, `help wanted`

---

## Issue 9: Normalización de Plantillas de Pull Request

**Título:** `[Maintenance] Normalizar plantillas de Pull Request`

**Descripción:**

Las plantillas de Pull Request tienen diferentes formatos y campos. Normalizar estas plantillas mejorará la consistencia y facilitará el proceso de revisión de código.

**Criterios de aceptación:**

- [ ] Todas las plantillas tienen estructura consistente
- [ ] Campos comunes (descripción, tipo, testing) están presentes en todas
- [ ] Cada plantilla tiene campos específicos relevantes a su tipo
- [ ] Las plantillas se renderizan correctamente en GitHub

**Archivos a modificar:**

- `.github/PULL_REQUEST_TEMPLATE/*.md`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `maintenance`, `documentation`, `good first issue`, `help wanted`

---

## Issue 10: Mejora del README de Spring Boot Starter

**Título:** `[Docs] Mejorar documentación de veld-spring-boot-starter`

**Descripción:**

El README del módulo `veld-spring-boot-starter` no demuestra todos los escenarios de integración posibles entre Veld y Spring Boot.

**Criterios de aceptación:**

- [ ] README incluye ejemplos de beans Veld en aplicación Spring Boot
- [ ] Documentación de configuración mixta (Veld + Spring beans)
- [ ] Ejemplos de inyección de beans Spring en componentes Veld y viceversa
- [ ] Sección de troubleshooting con problemas comunes
- [ ] La aplicación de ejemplo compila y ejecuta correctamente

**Archivo a modificar:**

- `veld-spring-boot-starter/README.md`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 2-4 horas  
**Etiquetas:** `documentation`, `integration`, `good first issue`, `help wanted`

---

## Issue 11: Tests para ScopeRegistry

**Título:** `[Test] Agregar tests para ScopeRegistry`

**Descripción:**

El `ScopeRegistry` que maneja el registro y resolución de scopes personalizados carece de tests unitarios dedicados.

**Criterios de aceptación:**

- [ ] Tests para registro de scopes personalizados
- [ ] Tests para resolución de scopes registrados
- [ ] Tests para scopes predefinidos (singleton, prototype)
- [ ] Tests para edge cases como registro de scopes con nombres duplicados

**Archivo a modificar:**

- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/scope/ScopeRegistry.java`

**Dificultad:** 🟢 Principiante  
**Tiempo estimado:** 1-2 horas  
**Etiquetas:** `testing`, `good first issue`, `help wanted`

---

## Issue 12: Ejemplo de Conditional Registration

**Título:** `[Example] Crear ejemplos de registro condicional`

**Descripción:**

El sistema de registro condicional de Veld (@ConditionalOnProperty, @ConditionalOnMissingBean, @ConditionalOnClass, @ConditionalOnBean) carece de ejemplos dedicados.

**Criterios de aceptación:**

- [ ] Ejemplo funcional para @ConditionalOnProperty
- [ ] Ejemplo funcional para @ConditionalOnMissingBean
- [ ] Ejemplo funcional para @ConditionalOnClass
- [ ] Ejemplo funcional para @ConditionalOnBean
- [ ] Documentación de cada tipo de condición

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 3-4 horas  
**Etiquetas:** `example`, `documentation`, `good first issue`, `help wanted`

---

## Issue 13: Mejora de Mensajes de Error en ConditionEvaluator

**Título:** `[Enhancement] Mejorar mensajes de error en ConditionEvaluator`

**Descripción:**

El `ConditionEvaluator` podría proporcionar mensajes de error más descriptivos cuando las condiciones fallan.

**Criterios de aceptación:**

- [ ] Los mensajes de error incluyen información de contexto suficiente
- [ ] Se diferencia entre diferentes tipos de fallos de condición
- [ ] Las pruebas unitarias verifican los mensajes de error

**Archivo a modificar:**

- `veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/condition/ConditionEvaluator.java`

**Dificultad:** 🟡 Intermedio  
**Tiempo estimado:** 2-3 horas  
**Etiquetas:** `enhancement`, `error-handling`, `good first issue`, `help wanted`

---

## Resumen de Issues

| # | Issue | Dificultad | Tiempo |
|---|-------|------------|--------|
| 1 | Documentación de APIs del Módulo Runtime | 🟢 Principiante | 3-5h |
| 2 | Tests Unitarios para ValueResolver | 🟢 Principiante | 2-3h |
| 3 | Tests para DependencyNode | 🟢 Principiante | 1-2h |
| 4 | Guía de Inicio Rápido Completa | 🟢 Principiante | 3-5h |
| 5 | Mejora del Manejo de Errores en EventBus | 🟡 Intermedio | 2-3h |
| 6 | Documentación de Anotaciones de Resiliencia | 🟢 Principiante | 2-3h |
| 7 | Ejemplo del Módulo de Resiliencia | 🟡 Intermedio | 4-6h |
| 8 | Actualización de Dependencias | 🟢 Principiante | 1-2h |
| 9 | Normalización de Plantillas de Pull Request | 🟢 Principiante | 1-2h |
| 10 | Mejora del README de Spring Boot Starter | 🟢 Principiante | 2-4h |
| 11 | Tests para ScopeRegistry | 🟢 Principiante | 1-2h |
| 12 | Ejemplo de Conditional Registration | 🟡 Intermedio | 3-4h |
| 13 | Mejora de Mensajes de Error en ConditionEvaluator | 🟡 Intermedio | 2-3h |

**Total de issues:** 13  
**Tiempo total estimado:** 30-43 horas
