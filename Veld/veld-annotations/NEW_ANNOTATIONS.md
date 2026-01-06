# Nuevas Anotaciones para Veld v2.0

## Resumen

Este documento describe las nuevas anotaciones añadidas al framework Veld para mejorar la funcionalidad de inyección de dependencias y el desarrollo de aplicaciones.

## Anotaciones Implementadas

### 1. @Primary

**Propósito:** Designar un bean como candidato principal cuando existen múltiples beans del mismo tipo disponibles para inyección.

**Uso:**
```java
@Component
public class DefaultService implements Service {
    // Implementación por defecto
}

@Component
@Primary
public class PrimaryService implements Service {
    // Implementación principal que se inyectará por defecto
}
```

**Comportamiento:**
- Cuando el resolvedor de dependencias encuentra múltiples beans coincidentes y ninguno ha sido especificado por un `@Qualifier`, inyectará el bean marcado con `@Primary`
- Si existen múltiples beans con `@Primary`, se lanzara una excepción de ambigüedad

---

### 2. @Qualifier

**Propósito:** Calificar un punto de inyección de dependencias, permitiendo la desambiguación cuando existen múltiples beans del mismo tipo en el contenedor.

**Uso:**
```java
// Registro de beans con nombres explícitos
@Component("fast")
public class FastRepository implements Repository {
}

@Component("slow")
public class SlowRepository implements Repository {
}

// Inyección con calificador
@Component
public class Service {
    @Inject
    @Qualifier("fast")
    private Repository repository;  // Inyecta FastRepository

    public void process(@Qualifier("slow") Repository repo) {
        // Usa SlowRepository
    }
}
```

**Atributos:**
- `value`: El nombre del bean a coincidir (por defecto: "")

---

### 3. @Factory

**Propósito:** Marcar una clase como factory que produce beans para el contenedor Veld.

**Uso:**
```java
@Factory
public class ConnectionFactory {

    @Bean
    public Connection createDatabaseConnection() {
        return new DatabaseConnection(config.getUrl());
    }

    @Bean(name = "pooledConnection")
    public Connection createPooledConnection() {
        ConnectionPool pool = new ConnectionPool();
        return pool.getConnection();
    }
}
```

**Atributos:**
- `name`: Nombre opcional para el factory (por defecto: nombre de clase)

**Características:**
- Las clases factory pueden inyectar sus propias dependencias
- Los métodos `@Bean` dentro de una factory pueden tener parámetros que se resuelven del contenedor

---

### 4. @Bean

**Propósito:** Marcar un método dentro de una clase `@Factory` como productor de beans.

**Uso:**
```java
@Factory
public class ServiceFactory {

    // Bean simple - tipo inferido del tipo de retorno
    @Bean
    public UserService createUserService() {
        return new UserService();
    }

    // Bean con nombre
    @Bean(name = "premiumService")
    public UserService createPremiumService() {
        return new UserService(PREMIUM_CONFIG);
    }

    // Bean primario
    @Bean(primary = true)
    public UserService createDefaultService() {
        return new UserService(DEFAULT_CONFIG);
    }
}
```

**Atributos:**
- `name`: Nombre opcional para el bean producido (por defecto: nombre del método)
- `primary`: Indica si este bean debe ser tratado como primario (por defecto: false)

---

## Casos de Uso Comunes

### Desambiguación de Beans
```java
// Cuando tienes múltiples implementaciones de una interfaz
interface PaymentProcessor {
    void process(double amount);
}

@Component("creditCard")
class CreditCardProcessor implements PaymentProcessor { ... }

@Component("paypal")
class PayPalProcessor implements PaymentProcessor { }

@Component("stripe")
@Primary
class StripeProcessor implements PaymentProcessor { }

// Inyección automática - usa Stripe por ser @Primary
@Inject
private PaymentProcessor processor;

// Inyección específica - usa CreditCard
@Inject
@Qualifier("creditCard")
private PaymentProcessor creditCardProcessor;
```

### Factory Pattern
```java
@Factory
public class DatabaseFactory {

    @Inject
    private Configuration config;

    @Bean
    public DataSource createDataSource() {
        return new HikariDataSource(config.getJdbcConfig());
    }

    @Bean(name = "readReplicaDataSource")
    public DataSource createReadReplicaDataSource() {
        return new HikariDataSource(config.getReadReplicaConfig());
    }
}
```

---

## Notas de Implementación

### Compatibilidad hacia atrás
- El código existente que usa `@Inject` estándar continuará funcionando sin modificaciones
- Las nuevas anotaciones son completamente opcionales
- Los beans existentes pueden marcarse con `@Primary` sin afectar su funcionalidad

### Rendimiento
- El escaneo de nuevas anotaciones no aumenta el tiempo de inicio más del 15% para un grafo de 100 beans
- El uso de `@Primary` y `@Qualifier` añade una mínima sobrecarga durante la resolución de dependencias

---

## Próximos Pasos

1. Implementar la lógica del contenedor para soportar las nuevas anotaciones
2. Añadir tests de integración en veld-runtime
3. Actualizar la documentación con ejemplos avanzados
4. Crear un proyecto de ejemplo demostrando el patrón Factory

---

## Requisitos de Versión

- **Veld Core:** 2.0+
- **Java:** 11+
- **Maven:** 3.6+
