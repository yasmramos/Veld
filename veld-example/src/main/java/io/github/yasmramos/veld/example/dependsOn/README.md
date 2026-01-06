# @DependsOn - Dependencias Explícitas

Este módulo demuestra la funcionalidad de `@DependsOn` en el framework Veld, que permite especificar dependencias explícitas entre componentes y controlar el orden de inicialización.

## 📋 Componentes de Ejemplo

### Componentes Base (Sin Dependencias)
- **ConfigService**: Proporciona configuración de aplicación
- **DatabaseService**: Maneja conectividad y operaciones de base de datos

### Componentes con Dependencias Explícitas

#### UserRepository
```java
@Component("userRepository")
@DependsOn("databaseService")
public class UserRepository {
    // Se inicializa DESPUÉS de DatabaseService
}
```

#### EmailService
```java
@Component("emailService")
@DependsOn("configService")
public class EmailService {
    // Se inicializa DESPUÉS de ConfigService
}
```

#### UserService
```java
@Component("userService")
@DependsOn({"databaseService", "configService", "emailService"})
public class UserService {
    // Se inicializa DESPUÉS de todos los servicios especificados
}
```

## ⚡ Orden de Inicialización

El orden de inicialización es determinado automáticamente por Veld:

1. **ConfigService** (sin dependencias)
2. **DatabaseService** (sin dependencias)
3. **UserRepository** (espera DatabaseService)
4. **EmailService** (espera ConfigService)
5. **UserService** (espera DatabaseService, ConfigService, EmailService)

## 🚀 Ejecutar Demostración

```bash
# Compilar el proyecto
mvn clean compile -pl veld-example -am

# Ejecutar la demostración
mvn exec:java -pl veld-example -Dexec.mainClass="io.github.yasmramos.veld.example.Main"
```

O directamente desde la sección 13 del Main:

```java
// La demostración de @DependsOn se ejecuta automáticamente
// cuando se ejecuta Main.main()
```

## 🔍 Características Demostradas

### ✅ Dependencias Simples
```java
@DependsOn("singleService")
```

### ✅ Dependencias Múltiples
```java
@DependsOn "service2", "service3"})
```

### ✅ Validación de Depend({"service1", componente verifica que susencias
Cada### ✅ Ordenamiento dependencias estén disponibles durante la inicialización.

 Topológico
Veld usa algoritmo de ordenamiento topológico para determinar el orden óptimo de inicialización.

### ✅ Detección de Ciclos
El sistema detecta y previene ciclos de dependencia automáticamente.

## 📝 Uso en Código Real

```java
@Component("myService")
@DependsOn({"configService", "databaseService"})
public class MyService {
    
    private ConfigService config;
    private DatabaseService database;
    
    @PostConstruct
    public void init() {
        // En este punto, config y database ya están inicializados
        System.out.println("MyService initialized with dependencies");
    }
    
    public void doWork() {
        // Usar las dependencias de forma segura
        String appName = config.getAppName();
        database.executeQuery("SELECT * FROM data");
    }
}
```

## 🎯 Beneficios de @DependsOn

1. **Control Explícito**: Define claramente las dependencias entre componentes
2. **Inicialización Ordenada**: Garantiza que los componentes se inicialicen en el orden correcto
3. **Validación Automática**: Verifica que todas las dependencias estén disponibles
4. **Código Más Limpio**: Elimina la necesidad de verificaciones manuales de dependencias
5. **Prevención de Errores**: Detecta problemas de dependencias en tiempo de compilación

## ⚠️ Notas Importantes

- Las dependencias deben ser nombres de beans válidos
- Se pueden especificar múltiples dependencias en un array
- El orden en el array no afecta el orden de inicialización
- Veld maneja automáticamente la resolución de dependencias
- No se pueden crear ciclos de dependencia

## 🔧 Integración con Veld

Esta funcionalidad está integrada automáticamente en el proceso de generación de bytecode de Veld. No requiere configuración adicional más allá de agregar la anotación `@DependsOn` a los componentes.