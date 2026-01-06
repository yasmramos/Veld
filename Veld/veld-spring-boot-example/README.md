# Veld Spring Boot Example - Complete Demo

Este ejemplo demuestra la integración completa entre Veld DI Framework y Spring Boot, incluyendo migración gradual y casos de uso reales.

## 🎯 Características Demostradas

### ✅ Integración Bidireccional
- **Spring → Veld**: Servicios Spring inyectados en componentes Veld
- **Veld → Spring**: Componentes Veld registrados en contexto Spring
- **Bridging Automático**: Configuración automática de beans cruzados

### ✅ Casos de Uso Reales
- **Gestión de Usuarios**: CRUD completo con Spring Data JPA
- **Gestión de Tareas**: Todo application con análisis y estadísticas
- **Migración Gradual**: Patrón "Strangler Fig" para migración sin interrupciones

### ✅ Patrones Avanzados
- **Repository Pattern**: Spring Data JPA con repositories
- **Service Layer**: Separación de lógica de negocio
- **Controller Layer**: Tanto Spring MVC como Veld controllers
- **Transaction Management**: @Transactional en servicios Spring

## 🏗️ Arquitectura

```
src/main/java/io/github/yasmramos/veld/boot/example/
├── domain/                    # Entidades JPA
│   ├── User.java             # Entidad User
│   └── Todo.java             # Entidad Todo
├── repository/               # Spring Data Repositories
│   ├── UserRepository.java   # Repository para User
│   └── TodoRepository.java   # Repository para Todo
├── service/
│   ├── spring/               # Servicios Spring
│   │   └── TodoBusinessService.java
│   └── veld/                 # Componentes Veld
│       ├── TodoAnalysisService.java
│       └── UserManagementService.java
├── controller/
│   ├── spring/               # Controllers Spring MVC (Legacy)
│   │   └── LegacyTodoController.java
│   ├── veld/                 # Controllers Veld
│   │   └── TodoController.java
│   └── MigrationController.java  # Demo de migración
├── config/
│   └── JpaConfig.java        # Configuración JPA
└── VeldSpringBootExampleApplication.java
```

## 🚀 Ejecutar la Aplicación

### Prerrequisitos
- Java 11+
- Maven 3.6+

### Comandos
```bash
# Compilar y ejecutar
mvn spring-boot:run

# O compilar primero
mvn clean compile
mvn spring-boot:run
```

### URLs de Prueba
Una vez ejecutándose, prueba estos endpoints:

#### 🎯 Endpoints Veld (Nuevos)
```bash
# Gestión de usuarios
curl "http://localhost:8080/api/v2/users/create?username=test&email=test@example.com&firstName=Test&lastName=User"

# Gestión de tareas
curl "http://localhost:8080/api/v2/todos?userId=1"
curl "http://localhost:8080/api/v2/todos/create?userId=1&title=Learn Veld&description=Study Veld framework&priority=HIGH"

# Análisis y estadísticas
curl "http://localhost:8080/api/v2/todos/statistics/1"
curl "http://localhost:8080/api/v2/todos/productivity/1"
curl "http://localhost:8080/api/v2/todos/activity"

# Demo de integración
curl "http://localhost:8080/api/v2/todos/demo/spring-integration?userId=1"
```

#### 🏛️ Endpoints Spring (Legacy)
```bash
# Gestión legacy de tareas
curl "http://localhost:8080/api/v1/todos?userId=1"
curl "http://localhost:8080/api/v1/todos/create?userId=1&title=Legacy Task&description=Old approach"
```

#### 🔄 Endpoints de Migración
```bash
# Guía de migración
curl "http://localhost:8080/api/migration/overview"

# Comparación de enfoques
curl "http://localhost:8080/api/migration/comparison/todos?userId=1"
curl "http://localhost:8080/api/migration/comparison/performance"

# Pasos de migración
curl "http://localhost:8080/api/migration/steps"
```

#### 🏥 Health Checks
```bash
# Health check general
curl "http://localhost:8080/actuator/health"

# Health check específico de Veld
curl "http://localhost:8080/actuator/health/veld"

# H2 Console (desarrollo)
# Navegar a: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:veldexample
```

## 📊 Base de Datos

### Datos de Ejemplo
La aplicación se inicia con datos de ejemplo:

**Usuarios:**
- johndoe (john.doe@example.com)
- janedoe (jane.doe@example.com)
- bobsmith (bob.smith@example.com)
- alicejohnson (alice.johnson@example.com)

**Tareas:**
- 8 tareas distribuidas entre usuarios
- Algunas completadas, otras pendientes
- Diferentes prioridades (HIGH, MEDIUM, LOW)

### Configuración
- **Base de datos**: H2 en memoria
- **DDL**: `create-drop` (se recrea al iniciar)
- **Datos**: Auto-carga desde `data.sql`

## 🔧 Configuración

### application.properties
```properties
# Veld Configuration
veld.spring-integration.enabled=true
veld.spring-integration.bridge-beans=true

# Database
spring.datasource.url=jdbc:h2:mem:veldexample
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## 📚 Casos de Uso Demostrados

### 1. **Spring → Veld Bridging**
```java
// TodoAnalysisService (Veld) <- TodoBusinessService (Spring)
@Component("todoAnalysisService")
public class TodoAnalysisService {
    @Inject
    private TodoBusinessService springTodoService; // ← Spring service en Veld
}
```

### 2. **Veld → Spring Bridging**
```java
// UserManagementService (Veld) <- UserRepository (Spring)
@Component("userManagementService") 
public class UserManagementService {
    @Inject
    private UserRepository springUserRepository; // ← Spring repo en Veld
}
```

### 3. **Migración Gradual**
- **Legacy**: `/api/v1/todos/*` (Spring MVC)
- **Nuevo**: `/api/v2/todos/*` (Veld)
- **Comparación**: `/api/migration/*`

### 4. **Coexistencia**
Ambos enfoques funcionan simultáneamente sin interferencias.

## 🧪 Testing

### Pruebas Manuales
1. **Crear usuario**: Usa los endpoints de usuarios
2. **Crear tareas**: Prueba tanto endpoints legacy como nuevos
3. **Verificar estadísticas**: Compara resultados entre enfoques
4. **Health checks**: Verifica que ambos frameworks funcionen

### Validación de Bridging
```bash
# 1. Crear usuario via Veld service (usa Spring repository)
curl "http://localhost:8080/api/v2/users/create?username=newuser&email=new@example.com&firstName=New&lastName=User"

# 2. Crear tarea via Veld controller (usa Spring service)
curl "http://localhost:8080/api/v2/todos/create?userId=1&title=Test Task&priority=HIGH"

# 3. Ver análisis via Veld service (procesa datos de Spring service)
curl "http://localhost:8080/api/v2/todos/statistics/1"
```

## 🎓 Conceptos Clave Demostrados

### ✅ Dependency Injection Patterns
- **Constructor Injection**: Preferred pattern
- **Field Injection**: Private field injection via Veld
- **Service Injection**: Cross-framework injection

### ✅ Transaction Management
- **@Transactional**: En servicios Spring
- **Propagation**: Proper transaction boundaries
- **Rollback**: Automatic on exceptions

### ✅ Data Access
- **Spring Data JPA**: Repository pattern
- **Entity Mapping**: @Entity, @Table, @Column
- **Relationships**: @ManyToOne, @OneToMany

### ✅ Web Layer
- **Spring MVC**: Traditional controllers
- **Veld Controllers**: New approach
- **RESTful APIs**: Consistent API design

## 🚀 Siguientes Pasos

1. **Experimenta**: Prueba todos los endpoints
2. **Migra**: Usa el patrón Strangler Fig en proyectos reales
3. **Optimiza**: Mide performance gains
4. **Adopta**: Integra Veld en producción

## 📖 Documentación Relacionada

- [Spring Boot Integration Guide](../../docs/spring-boot.html)
- [Veld Core Documentation](../../docs/index.html)
- [Migration Guide](../../docs/migration.html)

---

**Nota**: Este ejemplo demuestra capacidades avanzadas de Veld. Para proyectos nuevos, considera usar solo Veld controllers desde el inicio para máximo rendimiento.
