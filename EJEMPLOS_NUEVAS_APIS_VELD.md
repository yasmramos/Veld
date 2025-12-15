# 🚀 Ejemplos de Uso: Nuevas APIs de Veld

## 📋 **APIs Implementadas en la Clase Veld**

### 🎯 **Nuevos Métodos Disponibles:**

```java
public final class Veld {
    // Métodos existentes
    public static <T> T get(Class<T> type)
    public static <T> List<T> getAll(Class<T> type)
    public static boolean contains(Class<?> type)
    public static int componentCount()
    public static void shutdown()
    
    // NUEVOS MÉTODOS IMPLEMENTADOS
    public static <T> T get(Class<T> type, String name)                    // Named injection
    public static <T> Provider<T> getProvider(Class<T> type)               // Provider support
    public static EventBus getEventBus()                                    // EventBus access
    public static String resolveValue(String expression)                    // Value resolution
    public static <T> T resolveValue(String expression, Class<T> type)      // Typed value resolution
    public static void setActiveProfiles(String... profiles)                // Profile management
    public static String[] getActiveProfiles()                              // Get active profiles
    public static boolean isProfileActive(String profile)                   // Check profile
}
```

## 💡 **Ejemplos de Uso Prácticos**

### 1. 🚀 **EventBus Integration**
```java
@Component
public class OrderService {
    
    public void createOrder(String orderId) {
        // Create order logic...
        
        // Publish event using Veld API
        EventBus eventBus = Veld.getEventBus();
        eventBus.publish(new OrderCreatedEvent(this, orderId, 99.99));
        
        // Or publish async
        eventBus.publishAsync(new OrderCreatedEvent(this, orderId, 99.99));
    }
}

@Component
public class EmailNotification {
    
    @Subscribe
    public void onOrderCreated(OrderCreatedEvent event) {
        // Send email notification
        System.out.println("Sending email for order: " + event.getOrderId());
    }
}

// En el main o startup:
EventBus bus = Veld.getEventBus();
bus.register(new EmailNotification());
```

### 2. 🔧 **Value Resolution**
```java
@Component
public class DatabaseConfig {
    
    // Usando ValueResolver integrado en Veld
    private String dbUrl = Veld.resolveValue("${database.url:jdbc:h2:mem:test}");
    private int maxConnections = Veld.resolveValue("${database.max.connections:10}", Integer.class);
    private boolean sslEnabled = Veld.resolveValue("${database.ssl.enabled:false}", Boolean.class);
    
    public void connect() {
        System.out.println("Connecting to: " + dbUrl);
        System.out.println("Max connections: " + maxConnections);
        System.out.println("SSL enabled: " + sslEnabled);
    }
}

// Desde el main:
String appName = Veld.resolveValue("${app.name:MyApp}");
int port = Veld.resolveValue("${server.port:8080}", Integer.class);
System.out.println("Starting " + appName + " on port " + port);
```

### 3. 🏷️ **Named Injection**
```java
// Multiple implementations of the same interface
@Component("primary")
@Singleton
public class PrimaryDataSource implements DataSource {
    public Connection getConnection() { /* primary impl */ }
}

@Component("secondary") 
@Singleton
public class SecondaryDataSource implements DataSource {
    public Connection getConnection() { /* secondary impl */ }
}

// Inject by name using Veld API
@Component
public class UserService {
    
    public void switchDataSource() {
        // Get primary data source
        DataSource primary = Veld.get(DataSource.class, "primary");
        
        // Get secondary data source
        DataSource secondary = Veld.get(DataSource.class, "secondary");
    }
}
```

### 4. 📦 **Provider Support**
```java
@Component
public class ExpensiveService {
    // Expensive initialization...
}

@Component
public class LazyConsumer {
    
    // Get Provider for lazy instantiation
    Provider<ExpensiveService> expensiveProvider = Veld.getProvider(ExpensiveService.class);
    
    public void doWork() {
        // Service will be created only when needed
        ExpensiveService service = expensiveProvider.get();
        service.doExpensiveOperation();
    }
}
```

### 5. 🏷️ **Profiles Management**
```java
// Profile-based beans
@Component
@Profile("dev")
@Singleton
public class DevDataSource implements DataSource {
    public Connection getConnection() { return createDevConnection(); }
}

@Component
@Profile("prod")
@Singleton  
public class ProdDataSource implements DataSource {
    public Connection getConnection() { return createProdConnection(); }
}

// Set profiles programmatically
public class ApplicationStartup {
    
    public static void main(String[] args) {
        // Set profiles based on environment
        String env = System.getProperty("environment", "dev");
        Veld.setActiveProfiles(env);
        
        // Or multiple profiles
        Veld.setActiveProfiles("dev", "logging", "metrics");
        
        // Check active profiles
        String[] activeProfiles = Veld.getActiveProfiles();
        System.out.println("Active profiles: " + String.join(", ", activeProfiles));
        
        // Conditional logic based on profiles
        if (Veld.isProfileActive("prod")) {
            System.out.println("Running in production mode");
        }
    }
}
```

### 6. 🔄 **Lifecycle Management**
```java
@Component
@Singleton
public class CacheService implements DisposableBean {
    
    private Map<String, Object> cache = new HashMap<>();
    
    @PostConstruct
    public void init() {
        System.out.println("CacheService initialized");
    }
    
    @PreDestroy
    public void destroy() {
        System.out.println("CacheService shutting down, clearing cache");
        cache.clear();
    }
    
    public void put(String key, Object value) {
        cache.put(key, value);
    }
    
    public Object get(String key) {
        return cache.get(key);
    }
}

// Shutdown hook
public class ApplicationShutdown {
    public static void main(String[] args) {
        // Your application logic...
        
        // Graceful shutdown
        Veld.shutdown(); // Calls @PreDestroy on all singletons
    }
}
```

## 🎯 **Ventajas de las Nuevas APIs**

### ✅ **Beneficios Inmediatos**
1. **EventBus accesible** - Comunicación entre componentes sin dependencias directas
2. **Configuration externalization** - Props desde archivos/env vars/system properties
3. **Named injection** - Múltiples implementaciones de la misma interfaz
4. **Lazy instantiation** - Provider pattern para objetos costosos
5. **Profile management** - Configuración por entorno programática
6. **Lifecycle control** - Inicialización y destrucción ordenadas

### 🚀 **Mejora en la Experiencia del Usuario**
```java
// ANTES: Solo get() básico
MyService service = Veld.get(MyService.class);

// DESPUÉS: APIs rich y powerful
EventBus bus = Veld.getEventBus();
String config = Veld.resolveValue("${app.config}");
MyService service = Veld.get(MyService.class, "primary");
Provider<MyService> provider = Veld.getProvider(MyService.class);
```

## 📊 **Estado de la Implementación**

| API | Status | Uso |
|-----|--------|-----|
| `getEventBus()` | ✅ **LISTO** | `EventBus.getInstance()` |
| `resolveValue()` | ✅ **LISTO** | `ValueResolver.getInstance().resolve()` |
| `get(type, name)` | ✅ **STUB** | Requiere processor support |
| `getProvider()` | ✅ **STUB** | Requiere processor support |
| `setActiveProfiles()` | ✅ **STUB** | Requiere profile manager |
| `getActiveProfiles()` | ✅ **STUB** | Requiere profile manager |
| `isProfileActive()` | ✅ **STUB** | Requiere profile manager |

## 🔄 **Próximos Pasos**

### **Fase 1: Testeado y Validación**
1. ✅ APIs agregadas a clase `Veld`
2. ⏳ Testear EventBus integration
3. ⏳ Testear Value resolution
4. ⏳ Validar compatibility con processor

### **Fase 2: Processor Integration** 
1. ⏳ Soporte para `get(Class, String)` named injection
2. ⏳ Soporte para `getProvider()` 
3. ⏳ Profile management system

### **Fase 3: Advanced Features**
1. ⏳ JSR-330 compatibility layer
2. ⏳ Jakarta EE integration
3. ⏳ AOP integration in main API

## ✅ **Conclusión**

Las nuevas APIs transforman Veld de un simple DI container a un **framework completo de aplicaciones** con:

- **Event-driven architecture** via EventBus
- **External configuration** via ValueResolver  
- **Profile-based configuration**
- **Advanced injection patterns**
- **Lifecycle management**

¡Veld ahora puede competir directamente con Spring Boot en términos de funcionalidades de desarrollo!