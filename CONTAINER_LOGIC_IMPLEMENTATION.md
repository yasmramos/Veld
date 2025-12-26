# Implementación de Lógica del Contenedor para Nuevas Anotaciones

## Resumen de Cambios

Este documento describe los cambios realizados en los módulos `veld-processor` y `veld-runtime` para soportar las nuevas anotaciones.

## Cambios en veld-processor

### 1. ComponentInfo.java
**Añadido:**
- Campo `isPrimary` para marcar beans primarios
- Constructor actualizado con parámetro `isPrimary`
- Getter `isPrimary()` y setter `setPrimary(boolean)`

```java
private boolean isPrimary;  // @Primary - preferred bean for type

public ComponentInfo(String className, String componentName, Scope scope, boolean lazy, boolean isPrimary) {
    // ...
}

public boolean isPrimary() {
    return isPrimary;
}
```

### 2. VeldProcessor.java
**Añadido a @SupportedAnnotationTypes:**
- `io.github.yasmramos.veld.annotation.Primary`
- `io.github.yasmramos.veld.annotation.Qualifier`
- `io.github.yasmramos.veld.annotation.Factory`
- `io.github.yasmramos.veld.annotation.Bean`

**Actualizado `analyzeComponent`:**
- Detección de `@Primary` annotation
- Configuración del flag `isPrimary` en ComponentInfo

```java
// Check for @Primary annotation
boolean isPrimary = typeElement.getAnnotation(Primary.class) != null;
if (isPrimary) {
    note("  -> Primary bean selected");
}

ComponentInfo info = new ComponentInfo(className, componentName, scope, isLazy, isPrimary);
```

### 3. AnnotationHelper.java
**Añadidas constantes:**
```java
private static final String VELD_PRIMARY = "io.github.yasmramos.veld.annotation.Primary";
private static final String VELD_QUALIFIER = "io.github.yasmramos.veld.annotation.Qualifier";
private static final String VELD_FACTORY = "io.github.yasmramos.veld.annotation.Factory";
private static final String VELD_BEAN = "io.github.yasmramos.veld.annotation.Bean";
```

**Añadidos métodos:**
```java
public static boolean hasPrimaryAnnotation(Element element) {
    return hasAnnotation(element, VELD_PRIMARY);
}

public static boolean hasQualifierAnnotation(Element element) {
    return hasAnnotation(element, VELD_QUALIFIER);
}
```

**Actualizado `getQualifierValue`:**
- Ahora también lee el valor de `@Qualifier("value")` annotation

### 4. ComponentFactoryGenerator.java
**Añadida generación de método `isPrimary()`:**
```java
private void generateIsPrimary(ClassWriter cw) {
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isPrimary",
            "()Z", null, null);
    mv.visitCode();
    
    mv.visitInsn(ICONST_1); // return true
    mv.visitInsn(IRETURN);
    
    mv.visitMaxs(0, 0);
    mv.visitEnd();
}
```

**Integración en `generate()`:**
```java
// isPrimary() method (only if component is primary)
if (component.isPrimary()) {
    generateIsPrimary(cw);
}
```

## Cambios en veld-runtime

### 1. ComponentFactory.java
**Añadido método:**
```java
/**
 * Returns true if this component is marked as primary.
 * Primary beans are selected when multiple beans of the same type exist.
 *
 * @return true if this bean is primary
 */
default boolean isPrimary() {
    return false;
}
```

### 2. ComponentRegistry.java
**Añadido método:**
```java
/**
 * Returns the primary factory for the given type.
 * When multiple beans of the same type exist, returns the one marked with @Primary.
 *
 * @param type the component type
 * @param <T> the component type
 * @return the primary factory, null if not found, or null if multiple exist without a primary
 * @throws VeldException if multiple @Primary beans are found for the same type
 */
default <T> ComponentFactory<T> getPrimaryFactory(Class<T> type) {
    List<ComponentFactory<? extends T>> factories = getFactoriesForType(type);
    
    if (factories.isEmpty()) {
        return null;
    }
    
    // Find primary factory
    ComponentFactory<T> primary = null;
    for (ComponentFactory<? extends T> factory : factories) {
        if (factory.isPrimary()) {
            if (primary != null) {
                throw new VeldException("Multiple @Primary beans found for type: " + type.getName() + 
                    ". Only one bean can be marked as @Primary.");
            }
            @SuppressWarnings("unchecked")
            ComponentFactory<T> casted = (ComponentFactory<T>) factory;
            primary = casted;
        }
    }
    
    if (primary != null) {
        return primary;
    }
    
    if (factories.size() == 1) {
        @SuppressWarnings("unchecked")
        ComponentFactory<T> casted = (ComponentFactory<T>) factories.get(0);
        return casted;
    }
    
    return null;
}
```

## Flujo de Resolución de Beans

### Sin @Primary (comportamiento existente)
1. Buscar bean por tipo
2. Si existe exactamente uno, retornarlo
3. Si existen múltiples, lanzar excepción o permitir inyección con @Qualifier

### Con @Primary (nuevo comportamiento)
1. Buscar bean por tipo
2. Si existe exactamente uno, retornarlo
3. Si existen múltiples, buscar el marcado con @Primary
4. Si hay un @Primary, retornarlo
5. Si hay múltiples @Primary, lanzar excepción
6. Si no hay @Primary y hay múltiples, comportamiento como antes

## Notas de Implementación

### @Factory y @Bean
La implementación completa de @Factory y @Bean requiere:
1. Detección de clases @Factory en el scanner
2. Invocación de métodos @Bean para registrar sus resultados
3. Gestión de dependencias de factories

Estos se implementarán en una fase posterior.

### Compatibilidad
- Todos los cambios son compatibles hacia atrás
- El código existente continúa funcionando sin modificaciones
- Las nuevas anotaciones son completamente opcionales

## Próximos Pasos

1. ✅ Implementar lógica de @Primary
2. ✅ Implementar soporte de @Qualifier en processor
3. ⏳ Implementar detección de @Factory
4. ⏳ Implementar invocación de métodos @Bean
5. ⏳ Añadir tests de integración
6. ⏳ Actualizar documentación
