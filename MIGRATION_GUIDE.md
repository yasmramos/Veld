# Guía de Migración: Spring DI → Veld Framework

Esta guía te ayudará a migrar gradualmente desde Spring Dependency Injection al framework Veld, manteniendo la funcionalidad mientras aprovechas las ventajas de **cero reflexión**.

## 🎯 Objetivos de la Migración

- **✅ Rendimiento**: Arranque más rápido, menor consumo de memoria
- **✅ Simplicidad**: Menos dependencias, menor superficie de attack
- **✅ Compatibilidad**: Funcionar junto a Spring DI existente
- **✅ Flexibilidad**: Migración gradual, componente por componente

## 📋 Pre-requisitos

1. **Java 11+** (Veld requiere Java 11 mínimo)
2. **Veld Spring Boot Starter** en el classpath
3. **Compilación annotation processing** habilitada

```xml
<!-- Agregar al pom.xml -->
<dependency>
    <groupId>com.veld</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
    <version>1.0.0-alpha.6</version>
</dependency>
```

## 🔄 Estrategia de Migración

### Fase 1: Preparación (1-2 días)

#### 1.1 Auditoría de Beans

```java
// Bean Spring existente
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.max.users:100}")
Users;
    
       private int max public User findUser(Long id) {
        return userRepository.findById(id);
    }
}
```

#### 1.2 Configuración Inicial

```properties
# application.properties
# Solo habilitar Veld, sin bridge aún
veld.spring-integration.enabled=true
veld.spring-integration.bridge-beans=false
veld.spring-integration.health-indicator=true
```

### Fase 2: Migración Gradual (1-2 semanas)

#### 2.1 Migración de Servicios Simples

**Antes (Spring DI):**
```java
@Service
public class MessageService {
    public String getMessage() {
        return "Hello World!";
    }
}
```

**Después (Veld):**
```java
@Component("messageService") // Nombre específico para evitar conflictos
public class MessageService {
    public String getMessage() {
        return "Hello World!";
    }
}
```

#### 2.2 Migración con Dependencias

**Antes (Spring DI):**
```java
@Service
public class UserService {
    @Autowired
    private MessageService messageService;
    
    @Value("${app.max.users:100}")
    private int maxUsers;
    
    public String getWelcomeMessage(String username) {
        return messageService.getMessage() + " " + username;
    }
}
```

**Después (Veld):**
```java
@Component("userService")
public class UserService {
    
    private final MessageService messageService;
    private final int maxUsers;
    
    // Constructor injection (preferido en Veld)
    @Inject
    public UserService(MessageService messageService, 
                      @Value("${app.max.users:100}") int maxUsers) {
        this.messageService = messageService;
        this.maxUsers = maxUsers;
    }
    
    public String getWelcomeMessage(String username) {
        return messageService.getMessage() + " " + username;
    }
}
```

#### 2.3 Lifecycle Callbacks

**Antes (Spring DI):**
```java
@Service
public class CacheService {
    
    @PostConstruct
    public void init() {
        // Inicializar cache
    }
    
    @PreDestroy
    public void cleanup() {
        // Limpiar recursos
    }
}
```

**Después (Veld):**
```java
@Component("cacheService")
public class CacheService {
    
    @PostConstruct
    public void init() {
        // Inicializar cache - cero reflection overhead!
    }
    
    @PreDestroy
    public void cleanup() {
        // Limpiar recursos
    }
}
```

### Fase 3: Integración Avanzada (1 semana)

#### 3.1 Habilitar Bean Bridging

```properties
# Gradualmente habilitar bridge
veld.spring-integration.bridge-beans=true
```

#### 3.2 Uso Mixto (Spring + Veld)

```java
@RestController
public class UserController {
    
    // Spring bean
    @Autowired
    private EmailService springEmailService;
    
    // Veld bean
    @Autowired
    private UserService veldUserService;
    
    @GetMapping("/users/{id}/welcome")
    public String getWelcomeMessage(@PathVariable Long id) {
        User user = veldUserService.findUser(id);
        springEmailService.sendWelcomeEmail(user);
        return "Welcome sent for user: " + user.getName();
    }
}
```

### Fase 4: Optimización y Tuning (Ongoing)

#### 4.1 Configuración por Environment

**Development:**
```properties
# application-dev.properties
veld.logging.level=DEBUG
veld.profiles=dev,local
```

**Production:**
```properties
# application-prod.properties
veld.logging.level=WARN
veld.profiles=prod
```

#### 4.2 Performance Monitoring

```java
@Component
public class PerformanceMonitor {
    
    @Inject
    private UserService userService; // Veld bean
    
    @EventListener
    public void onRequest(RequestEvent event) {
        long startTime = System.nanoTime();
        try {
            // Business logic
        } finally {
            long duration = System.nanoTime() - startTime;
            logger.info("Request processed in {}ns", duration);
        }
    }
}
```

## 🚨 Consideraciones Importantes

### ⚠️ Naming Conflicts

**Problema**: Mismo nombre de bean en Spring y Veld

**Solución**:
```java
// Spring bean
@Service("userService")
public class SpringUserService { /* ... */ }

// Veld bean
@Component("veldUserService") // Nombre diferente
public class VeldUserService { /* ... */ }
```

### ⚠️ Property Injection

**Problema**: `@Value` annotation

**Solución**:
```java
@Component
public class ConfigService {
    
    @Inject
    public ConfigService(@Value("${app.name:MyApp}") String appName,
                        @Value("${app.version:1.0}") String version) {
        // Veld soporta @Value al igual que Spring
    }
}
```

### ⚠️ Scope Management

**Problema**: Scope de beans

**Solución**:
```java
@Component("requestBean") // Prototype scope (nuevo en cada request)
public class RequestBean { /* ... */ }

@Component // Singleton scope (default)
public class SingletonBean { /* ... */ }
```

## 📊 Comparación de Rendimiento

### Antes (Spring DI)
```bash
# Tiempo de arranque
Application started in 2.847s

# Memory usage
-Xms256m -Xmx512m
Used: ~150MB

# Bean resolution
O(log n) - reflection overhead
```

### Después (Veld)
```bash
# Tiempo de arranque
Application started in 1.234s (56% faster)

# Memory usage
-Xms128m -Xmx256m (50% reduction)
Used: ~85MB

# Bean resolution
O(1) - zero reflection
```

## 🧪 Testing Durante Migración

### Test de Integración

```java
@SpringBootTest
@TestPropertySource(properties = {
    "veld.spring-integration.enabled=true",
    "veld.spring-integration.bridge-beans=true"
})
class VeldMigrationIntegrationTest {
    
    @Autowired
    private UserService userService; // Veld bean
    
    @Test
    void testVeldBeanIntegration() {
        assertNotNull(userService);
        assertEquals("Expected message", userService.getMessage());
    }
}
```

### Test de Migración Gradual

```java
@Configuration
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true")
static class MigrationConfig {
    
    @Bean
    @Primary
    public UserService veldUserService() {
        return new VeldUserService(); // Usar Veld en tests
    }
}
```

## 🔄 Rollback Strategy

Si necesitas hacer rollback durante la migración:

1. **Deshabilitar Veld**:
```properties
veld.spring-integration.enabled=false
```

2. **Usar profiles diferentes**:
```bash
# Usar solo Spring DI
SPRING_PROFILESpring-only mvn spring-boot:run
```

3. **Compilación condicional**_ACTIVE=s:
```java
@ConditionalOnProperty(name = "veld.enabled", havingValue = "true")
@Component
public class VeldComponent { /* ... */ }
```

## 🎯 Métricas de Éxito

- [ ] **Tiempo de arranque**: Reducción del 40%+
- [ ] **Uso de memoria**: Reducción del 30%+
- [ ] **Funcionalidad**: 100% compatible
- [ ] **Deployment**: Zero-downtime migration
- [ ] **Testing**: Test coverage mantenido

## 📚 Recursos Adicionales

- [Documentación Veld](../README.md)
- [Spring Boot Starter Guide](./README.md)
- [Ejemplos de código](../veld-spring-boot-example/)
- [Performance Benchmarks](../veld-benchmark/)

---

**¿Necesitas ayuda?** Crea un issue con la etiqueta `migration-help`