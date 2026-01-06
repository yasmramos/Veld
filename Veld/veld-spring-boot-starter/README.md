# Veld Spring Boot Starter

## 🚀 Integración Automática Veld + Spring Boot

El **Veld Spring Boot Starter** proporciona integración automática entre el framework Veld DI y Spring Boot, permitiendo usar las ventajas de **cero reflexión** de Veld en aplicaciones Spring Boot existentes.

### ✨ Características

- **🔄 Auto-configuración**: Inicialización automática del contenedor Veld
- **⚙️ Configuración flexible**: Personalización completa vía `application.properties`
- **📊 Health Checks**: Integración con Spring Boot Actuator
- **🔌 Compatibilidad**: Funciona junto a Spring DI sin conflictos
- **🎯 Zero Reflection**: Rendimiento superior sin overhead de reflexión
- **📝 Logging Integrado**: Registro detallado del ciclo de vida

### 📦 Dependencia Maven

```xml
<dependency>
    <groupId>io.github.yasmramos.veld</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
    <version>1.0.0-alpha.6</version>
</dependency>
```

### ⚙️ Configuración

#### Configuración Básica (`application.properties`)

```properties
# Veld Framework Configuration
veld.profiles=dev,default
veld.logging.enabled=true
veld.logging.level=INFO

# Spring Integration Features
veld.spring-integration.enabled=true
veld.spring-integration.health-indicator=true
```

#### Configuración Avanzada

```properties
# Container lifecycle
veld.container.auto-start=true
veld.container.auto-close=true

# Profiles configuration
veld.profiles=dev,test,production

# Logging
veld.logging.enabled=true
veld.logging.level=DEBUG

# Spring integration
veld.spring-integration.enabled=true
veld.spring-integration.bridge-beans=true
veld.spring-integration.health-indicator=true
```

### 🏗️ Uso

#### 1. Bean de Veld

```java
import io.github.yasmramos.veld.annotations.Component;
import io.github.yasmramos.veld.annotations.Inject;
import io.github.yasmramos.veld.annotations.PostConstruct;

@Component("userService")
public class UserService {

    private final DatabaseService databaseService;

    @Inject
    public UserService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PostConstruct
    public void init() {
        // Lifecycle callback - zero reflection overhead!
    }

    public User findUser(Long id) {
        return databaseService.findById(id);
    }
}
```

#### 2. Integración con Spring Controllers

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    // Spring bean accessing Veld component
    @Autowired
    private UserService veldUserService;

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return veldUserService.findUser(id);
    }
}
```

### 🏥 Health Checks

Accede a información de salud del contenedor Veld:

```
GET /actuator/health/veld
```

**Respuesta ejemplo:**

```json
{
  "status": "UP",
  "details": {
    "container": "running",
    "initialized": true,
    "version": "1.0.0-alpha.6",
    "framework": "Veld DI - Zero Reflection"
  }
}
```

### 🔧 Configuración Avanzada

#### Deshabilitar Features

```properties
# Deshabilitar completamente Veld
veld.spring-integration.enabled=false

# Solo health checks
veld.spring-integration.enabled=true
veld.spring-integration.bridge-beans=false
```

#### Profiles por Entorno

**Desarrollo (`application-dev.properties`):**
```properties
veld.logging.level=DEBUG
veld.profiles=dev,local
```

**Producción (`application-prod.properties`):**
```properties
veld.logging.level=WARN
veld.profiles=prod
```

### 📊 Ventajas vs Spring DI

| Característica | Veld | Spring DI |
|---|---|---|
| **Arranque** | <1ms | 50-200ms |
| **Reflexión** | ❌ Cero | ⚡ Intensive |
| **Overhead Memoria** | Mínimo | Alto |
| **Performance** | O(1) | O(log n) |
| **Bytecode** | Generado | No |

### 🧪 Ejemplo Completo

Consulta el proyecto `veld-spring-boot-example` para un ejemplo completo:

```bash
cd veld-spring-boot-example
mvn spring-boot:run
```

Endpoints disponibles:
- `GET /api/veld/message` - Mensaje del servicio Veld
- `GET /api/veld/welcome?name=John` - Mensaje personalizado
- `GET /api/veld/status` - Estado de la integración
- `GET /actuator/health/veld` - Health check detallado

### 🔍 Monitoreo

#### Métricas Disponibles

```properties
# Habilitar todas las métricas
management.endpoints.web.exposure.include=health,info,metrics

# Ver métricas Veld
GET /actuator/metrics/veld.container.initialization
GET /actuator/metrics/veld.components.count
```

### 🚨 Solución de Problemas

#### Veld Container no inicia

```properties
# Habilitar logging detallado
logging.level.io.github.yasmramos.veld=DEBUG
```

#### Conflicto con Spring DI

```properties
# Usar naming diferente
@Component("veldUserService")
```

#### Health Check falla

```bash
# Verificar logs
curl /actuator/loggers/io.github.yasmramos.veld

# Health check directo
curl /actuator/health/veld
```

### 📚 Migración desde Spring DI

1. **Agregar dependencia**:
```xml
<dependency>
    <groupId>io.github.yasmramos.veld</groupId>
    <artifactId>veld-spring-boot-starter</artifactId>
</dependency>
```

2. **Anotar beans existentes**:
```java
@Component // en lugar de @Service
@Inject    // en lugar de @Autowired
```

3. **Configurar profiles**:
```properties
veld.profiles=migration,default
```

4. **Verificar health check**:
```bash
curl /actuator/health/veld
```

### 🎯 Próximos Pasos

- [ ] **Bean Bridging**: Integración completa de beans Veld ↔ Spring
- [ ] **Performance Metrics**: Métricas detalladas de rendimiento
- [ ] **Starter for Spring Security**: Integración con Spring Security
- [ ] **GraalVM Native**: Soporte nativo para GraalVM
- [ ] **Spring Cloud**: Integración con Spring Cloud

---

**¿Problemas?** Crea un issue en [GitHub](https://github.com/yasmramos/Veld/issues)

**¿Contribuciones?** Lee nuestra [Guía de Contribución](CONTRIBUTING.md)