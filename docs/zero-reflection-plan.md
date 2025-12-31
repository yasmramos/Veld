# Eliminación de Reflexión del Framework Veld

## Resumen Ejecutivo

Este documento describe la estrategia para eliminar completamente el uso de reflexión en tiempo de ejecución del framework Veld, con el objetivo de lograr imágenes nativas más pequeñas y cumplir con el principio de "zero reflection" del framework.

## Estado Actual del Uso de Reflexión

### 1. EventBus.java ( líneas 591-620 )

**Problema:**
```java
for (Method method : clazz.getDeclaredMethods()) {
    Subscribe annotation = method.getAnnotation(Subscribe.class);
    // ...
}
```

**Solución:**
- El `VeldProcessor` ya analiza los métodos `@Subscribe` en `analyzeEventSubscribers()`
- Generar código de registro de eventos en el registry generado
- Eliminar la búsqueda de métodos en tiempo de ejecución

### 2. EventSubscriber.java

**Problema:**
```java
private final Method method;
private final MethodHandle methodHandle;

public EventSubscriber(Object target, Method method, Class<?> eventType, ...) {
    this.method = Objects.requireNonNull(method, "method cannot be null");
    this.methodHandle = precomputeMethodHandle(target, method);
}
```

**Solución:**
- Almacenar solo el `MethodHandle` (sin el objeto `Method`)
- Generar los `MethodHandle` en tiempo de compilación si es posible
- Usar tipos funcionales en lugar de reflexión

### 3. ComponentRegistry.java ( líneas 170-180 )

**Problema:**
```java
try {
    java.lang.reflect.Method getScopeIdMethod = factory.getClass().getMethod("getScopeId");
    Object scopeId = getScopeIdMethod.invoke(factory);
} catch (Exception e) {
    // Fallback
}
```

**Solución:**
- Agregar método `getScopeId()` a la interfaz `ComponentFactory`
- Eliminar la necesidad de reflexión

### 4. LoggingInterceptor.java ( líneas 52-55 )

**Problema:**
```java
Method method = ctx.getMethod();
if (method != null) {
    config = method.getAnnotation(Logged.class);
    isVoidMethod = method.getReturnType() == void.class;
}
```

**Solución:**
- Agregar métodos a `InvocationContext` para obtener la información sin reflexión
- Pasar la configuración de `@Logged` durante la generación de código AOP

## Plan de Implementación

### Fase 1: Refactorización de ComponentFactory

**Objetivo:** Eliminar la reflexión en ComponentRegistry

**Cambios:**
1. Agregar método `String getScopeId()` a `ComponentFactory`
2. Actualizar `ComponentFactoryGenerator` para implementar el método
3. Eliminar el código de reflexión en `ComponentRegistry.getScopeId()`

### Fase 2: Refactorización del Sistema de Eventos

**Objetivo:** Eliminar la reflexión en EventBus

**Cambios:**
1. Expandir `VeldProcessor` para generar `EventRegistry` con métodos de suscripción registrados
2. Modificar `EventSubscriber` para usar tipos funcionales en lugar de `Method`
3. Eliminar `clazz.getDeclaredMethods()` de `EventBus.register()`

### Fase 3: Refactorización de AOP

**Objetivo:** Eliminar la reflexión en interceptores

**Cambios:**
1. Expandir `InvocationContext` con métodos de información sin reflexión
2. Modificar `LoggingInterceptor` para usar la información del contexto
3. Generar configuración de anotaciones en tiempo de compilación

### Fase 4: Actualización de Configuración GraalVM

**Objetivo:** Eliminar los archivos de configuración de reflexión

**Cambios:**
1. Eliminar `jni-config.json` (ya vacío)
2. Eliminar `proxy-config.json` cuando no haya más proxies dinámicos
3. Verificar que no se necesiten más configuraciones de reflexión

## Archivos a Modificar

### veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/ComponentRegistry.java
- Eliminar líneas 170-180 (código de reflexión)

### veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/event/EventBus.java
- Modificar método `register()` para usar registro pre-generado
- Eliminar uso de `java.lang.reflect.Method`

### veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/event/EventSubscriber.java
- Cambiar de `Method` a tipo funcional
- Eliminar `java.lang.reflect.Method` del campo

### veld-aop/src/main/java/io/github/yasmramos/veld/aop/interceptor/LoggingInterceptor.java
- Usar `ctx.getMethodName()` y otros métodos del contexto
- Eliminar `method.getAnnotation()` y `method.getReturnType()`

### veld-processor/src/main/java/io/github/yasmramos/veld/processor/VeldProcessor.java
- Expandir generación de código de registro de eventos
- Generar `EventRegistry` con suscripciones pre-descubiertas

### veld-runtime/src/main/java/io/github/yasmramos/veld/runtime/ComponentFactory.java
- Agregar método `String getScopeId()`

## Beneficios Esperados

1. **Imagen nativa más pequeña:** Sin metadatos de reflexión necesarios
2. **Mejor rendimiento:** Sin sobrecarga de reflexión en tiempo de ejecución
3. **Inicio más rápido:** GraalVM puede optimizar mejor sin reflexión
4. **Cumplimiento del principio zero reflection:** El framework no usa reflexión

## Métricas de Éxito

- Eliminar 100% del uso de reflexión en tiempo de ejecución
- Reducir tamaño de imagen nativa en ~10-20%
- Mantener funcionalidad existente
- No romper la API pública
