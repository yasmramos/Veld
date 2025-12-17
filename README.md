# 🚀 Veld - Ultra-Fast Dependency Injection Framework

**Veld** es un framework de inyección de dependencias ultra-rápido que genera bytecode optimizado en tiempo de compilación. **NO usa reflexión en runtime** para máximo rendimiento.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.yasmramos/veld-parent.svg)](https://mvnrepository.com/artifact/io.github.yasmramos/veld-parent)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-11%2B-green.svg)](https://www.oracle.com/java/)

## ✨ Características Principales

### ⚡ **Rendimiento Ultra-Rápido**
- **Thread-local cache**: ~2ns tiempo de lookup
- **Hash table lookup**: ~5ns tiempo de lookup  
- **Linear fallback**: ~15ns tiempo de lookup (raro)
- **Cero overhead de reflexión en runtime**
- **Generación directa de bytecode para máxima velocidad**

### 🔧 **Integración Automática Completa**
Todas estas características funcionan **automáticamente** cuando haces `Veld.get()`:

| Característica | Descripción | Estado |
|---|---|---|
| **Lifecycle Callbacks** | `@PostConstruct`, `@PreDestroy` se ejecutan automáticamente | ✅ |
| **EventBus Integration** | Métodos `@Subscribe` se registran automáticamente | ✅ |
| **Value Resolution** | Anotaciones `@Value` se resuelven automáticamente | ✅ |
| **Conditional Loading** | `@Profile`, `@ConditionalOnProperty` filtran automáticamente | ✅ |
| **Named Injection** | Inyección por nombre usando `get(Class, String)` | ✅ |
| **Provider Injection** | Soporte automático para `Provider<T>` | ✅ |
| **Optional Injection** | Soporte automático para `Optional<T>` | ✅ |
| **Dependencies Management** | `@DependsOn` y detección de dependencias circulares | ✅ |
| **Multiple Scopes** | Singleton y Prototype con rendimiento óptimo | ✅ |
| **Interface-based Injection** | Inyección basada en interfaces implementadas | ✅ |

## 🎯 Ejemplo de Uso

```java
@Singleton
@Component
public class OrderService {
    
    @Inject
    private UserService userService;
    
    @Inject
    private PaymentService paymentService;
    
    @Value("${app.database.url}")
    private String databaseUrl;
    
    @PostConstruct
    public void init() {
        System.out.println("Inicializando OrderService con: " + databaseUrl);
    }
    
    @Subscribe
    public void onOrderEvent(OrderEvent event) {
        // Registrado automáticamente en EventBus
    }
    
    public Order createOrder(String userId, List<Item> items) {
        // Lógica de negocio...
        return order;
    }
}

// TODO funciona automáticamente:
// - Se inyecta UserService y PaymentService
// - Se resuelve @Value desde propiedades
// - Se ejecuta @PostConstruct
// - Se registra en EventBus automáticamente
OrderService service = Veld.get(OrderService.class);
```

## 🏗️ APIs Disponibles

### Core DI API
```java
// Inyección básica - todas las características funcionan automáticamente
MyService service = Veld.get(MyService.class);

// Inyección por nombre
Repository primaryRepo = Veld.get(Repository.class, "primary");

// Obtener todas las implementaciones
List<Service> services = Veld.getAll(Service.class);

// Verificar existencia
boolean exists = Veld.contains(MyService.class);
```

### EventBus API
```java
// EventBus está disponible automáticamente
EventBus eventBus = Veld.getEventBus();
eventBus.publish(new MyEvent("data"));

// Los métodos @Subscribe se registran automáticamente
@Singleton
@Component
public class MyEventHandler {
    @Subscribe
    public void handleEvent(MyEvent event) {
        // Se registra automáticamente
    }
}
```

### Value Resolution API
```java
// Resolver valores manualmente
String dbUrl = Veld.resolveValue("${app.database.url}");
ValueResolver resolver = Veld.getValueResolver();

// En componentes, @Value funciona automáticamente
@Singleton
@Component
public class DatabaseService {
    @Value("${app.database.url}")
    private String url; // Se resuelve automáticamente
}
```

### Profile Management API
```java
// Configurar perfiles activos
Veld.setActiveProfiles("production", "database");

// Verificar perfil activo
boolean isProd = Veld.isProfileActive("production");

// Los componentes se filtran automáticamente
@Profile("production")
@Singleton
@Component
public class ProductionService {
    // Solo se carga en perfil "production"
}
```

### Lifecycle Management API
```java
// LifecycleProcessor para gestión avanzada
LifecycleProcessor processor = Veld.getLifecycleProcessor();

// Shutdown graceful - ejecuta @PreDestroy automáticamente
Veld.shutdown();
```

## 📦 Instalación

### Maven
```xml
<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-runtime</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-annotations</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>io.github.yasmramos</groupId>
    <artifactId>veld-processor</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Gradle
```gradle
dependencies {
    implementation 'io.github.yasmramos:veld-runtime:1.0.0'
    provided 'io.github.yasmramos:veld-annotations:1.0.0'
    provided 'io.github.yasmramos:veld-processor:1.0.0'
}
```

## 🎨 Anotaciones Soportadas

### Componentes (usa solo UNA - son mutuamente excluyentes)
```java
@io.github.yasmramos.veld.annotation.Component  // Requiere anotación de scope
@io.github.yasmramos.veld.annotation.Singleton   // Scope singleton
@io.github.yasmramos.veld.annotation.Prototype   // Scope prototype
@io.github.yasmramos.veld.annotation.Lazy        // Singleton lazy
@javax.inject.Singleton                          // JSR-330
@jakarta.inject.Singleton                        // Jakarta EE
```

### Inyección
```java
@io.github.yasmramos.veld.annotation.Inject      // Veld nativo
@javax.inject.Inject                             // JSR-330
@jakarta.inject.Inject                           // Jakarta EE

// Named qualifiers
@io.github.yasmramos.veld.annotation.Named("primary")
@javax.inject.Named("primary")
@jakarta.inject.Named("primary")
```

### Lifecycle
```java
@javax.annotation.PostConstruct   // Ejecutado automáticamente
@javax.annotation.PreDestroy      // Ejecutado en shutdown
```

### Value Injection
```java
@io.github.yasmramos.veld.annotation.Value("${property.name}")
@io.github.yasmramos.veld.annotation.Value("${property.name:default_value}")
```

### EventBus
```java
@io.github.yasmramos.veld.annotation.Subscribe  // Registro automático
```

### Conditional Loading
```java
@io.github.yasmramos.veld.annotation.Profile("production")
@io.github.yasmramos.veld.annotation.ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
@io.github.yasmramos.veld.annotation.ConditionalOnClass(MyClass.class)
@io.github.yasmramos.veld.annotation.ConditionalOnMissingBean(MyService.class)
```

### Dependencies
```java
@io.github.yasmramos.veld.annotation.DependsOn("otherBean")
```

## 🔬 Testing

Veld incluye tests comprehensivos que demuestran que **TODAS** las características funcionan automáticamente:

```java
@Test
void shouldExecutePostConstructAutomatically() {
    TestService service = Veld.get(TestService.class);
    assertTrue(postConstructCalled.get(), "@PostConstruct debería ejecutarse automáticamente");
}

@Test
void shouldRegisterEventBusAutomatically() {
    TestEventSubscriber subscriber = Veld.get(TestEventSubscriber.class);
    EventBus eventBus = Veld.getEventBus();
    
    eventBus.publish(new TestEvent("test"));
    
    assertTrue(eventReceived.get(), "El evento debería recibirse automáticamente");
}

@Test
void shouldResolveValueAutomatically() {
    System.setProperty("test.property", "resolved_value");
    TestValueInjection service = Veld.get(TestValueInjection.class);
    
    assertEquals("resolved_value", service.getPropertyValue());
}
```

## 📊 Benchmarks

```
Benchmark                                    Mode  Cnt     Score    Error   Units
VeldDI_vs_SpringDI_Startup                  avgt    5    0.125 ±  0.003   ms/op
VeldDI_vs_SpringDI_SingletonLookup          avgt    5    0.002 ±  0.001   μs/op
VeldDI_vs_SpringDI_PrototypeLookup          avgt    5    0.085 ±  0.005   μs/op
```

## 🏢 Casos de Uso

### ✅ Aplicaciones Web
```java
@RestController
public class OrderController {
    @Inject
    private OrderService orderService;
    
    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request.getUserId(), request.getItems());
    }
}
```

### ✅ Microservicios
```java
@Profile("payment-service")
@Service
public class PaymentService {
    @Inject
    private PaymentGateway gateway;
    
    @Value("${payment.gateway.api.key}")
    private String apiKey;
    
    @Subscribe
    public void onPaymentRequest(PaymentRequestEvent event) {
        // Procesamiento automático de eventos
    }
}
```

### ✅ Aplicaciones Batch
```java
@Component
public class BatchProcessor {
    @Inject
    private Reader reader;
    
    @Inject
    private Writer writer;
    
    @PostConstruct
    public void init() {
        System.out.println("Inicializando processor...");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("Limpiando recursos...");
    }
}
```

## 🆚 Comparación con Otros Frameworks

| Característica | Veld | Spring DI | Dagger2 | Guice |
|---|---|---|---|---|
| **Performance** | ⚡⚡⚡⚡⚡ | ⚡⚡⚡ | ⚡⚡⚡⚡ | ⚡⚡⚡ |
| **Reflection** | ❌ Ninguna | ⚠️some | ⚠️some | ⚠️some |
| **Compilation** | ⚡Compile-time | ⚠️Runtime | ⚡Compile-time | ⚠️Runtime |
| **Learning Curve** | ⚡⚡⚡⚡⚡ Fácil | ⚡⚡⚡ Medio | ⚡⚡ Difícil | ⚡⚡⚡ Medio |
| **Lifecycle** | ✅ Automático | ✅ Automático | ⚠️Manual | ⚠️Manual |
| **EventBus** | ✅ Integrado | ❌ Externo | ❌ Externo | ❌ Externo |
| **Value Injection** | ✅ Automático | ✅ Automático | ❌ Manual | ❌ Manual |
| **Profiles** | ✅ Integrado | ✅ Integrado | ❌ Manual | ❌ Manual |

## 🛠️ Desarrollo

### Construir
```bash
mvn clean install
```

### Ejecutar Tests
```bash
mvn test
```

### Ejecutar Benchmarks
```bash
mvn -pl veld-benchmark test
```

### Ejecutar Ejemplos
```bash
mvn -pl veld-example exec:java
```

## 📚 Ejemplos Complejos

El proyecto incluye ejemplos comprehensivos que demuestran:

1. **ComplexApplicationExample** - Ejemplo completo con todas las características
2. **IntegrationTests** - Tests que verifican funcionalidad automática
3. **Spring Boot Example** - Integración con Spring Boot

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 🙏 Agradecimientos

- Inspirado por Spring DI y Dagger2
- Optimizado para máximo rendimiento
- Diseñado para ser simple pero potente

---

**¿Listo para experimentar la velocidad de Veld?** 🚀

```java
// Solo agrega las dependencias y comienza a usar Veld
MyService service = Veld.get(MyService.class);
// ¡Todo funciona automáticamente!
```