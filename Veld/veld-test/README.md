# Veld Test Framework

Integración nativa con pruebas unitarias para el framework Veld.

## Descripción

El módulo `veld-test` proporciona integración transparente con JUnit 5 para facilitar la escritura de pruebas unitarias y de integración con el contenedor Veld. Características principales:

- **Configuración mínima**: Anotación única `@VeldTest` activa toda la infraestructura
- **Mocks automáticos**: Campos anotados con `@RegisterMock` se convierten en mocks de Mockito
- **Inyección en pruebas**: Beans del contenedor se injectan automáticamente en campos de prueba
- **Perfiles de prueba**: Configuraciones predefinidas para diferentes tipos de pruebas
- **Aislamiento entre pruebas**: Mocks se resetean automáticamente entre tests

## Uso Básico

### Dependencia Maven

```xml
<dependency>
    <groupId>io.github.yasmramos.veld</groupId>
    <artifactId>veld-test</artifactId>
    <version>1.0.3</version>
    <scope>test</scope>
</dependency>
```

### Prueba Unitaria Simple

```java
@VeldTest
class UserServiceTest {
    
    @Inject
    private UserService userService;
    
    @Test
    void shouldReturnUserById() {
        User user = userService.findById(1L);
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("Test User");
    }
}
```

### Uso de Mocks

```java
@VeldTest
class OrderServiceTest {
    
    @RegisterMock
    private PaymentGateway paymentGateway;
    
    @Inject
    private OrderService orderService;
    
    @BeforeEach
    void setUp() {
        when(paymentGateway.process(any())).thenReturn(true);
    }
    
    @Test
    void shouldProcessOrderSuccessfully() {
        Order order = new Order("TEST-001");
        
        boolean result = orderService.processOrder(order);
        
        assertThat(result).isTrue();
        verify(paymentGateway).process(order.getId());
    }
}
```

### Perfiles de Prueba

```java
// Prueba con mocks automáticos
@VeldTest(profile = "mock")
class MockedServiceTest { }

// Prueba con base de datos en memoria
@VeldTest(profile = "in-memory")
class IntegrationTest { }

// Prueba con configuración personalizada
@VeldTest(
    profile = "test",
    properties = {
        "database.url=jdbc:h2:mem:testdb",
        "server.port=0"
    }
)
class CustomConfigTest { }
```

### Uso Directo de TestContext

```java
@Test
void manualContextTest() {
    try (TestContext context = TestContext.Builder.create()
            .withProfile("test")
            .withProperty("debug", "true")
            .build()) {
        
        MyService service = context.getBean(MyService.class);
        
        // Ejecutar pruebas...
    }
}
```

## Anotaciones

### @VeldTest

Anotación principal para clases de prueba.

| Atributo | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `profile` | Perfil de configuración | `"test"` |
| `classes` | Clases de configuración adicionales | `{}` |
| `properties` | Propiedades del sistema | `{}` |
| `isolateBetweenTests` | Reiniciar contexto entre tests | `false` |

### @RegisterMock

Marca un campo para ser reemplazado por un mock.

```java
@RegisterMock
private UserRepository userRepository;

@RegisterMock(name = "customRepo")
private UserRepository customRepository;
```

### @Inject

Inyecta un bean del contenedor en un campo de prueba.

```java
@Inject
private UserService userService;

@Inject(name = "specificBean")
private MyService myService;
```

### @TestProfile

Selecciona un perfil de configuración.

```java
@VeldTest
@TestProfile("mock")
class MockedTest { }
```

## Perfiles Disponibles

| Perfil | Descripción |
|--------|-------------|
| `test` | Configuración básica para pruebas |
| `mock` | Todos los beans de infraestructura son mocks |
| `in-memory` | Usa implementaciones en memoria (H2, etc.) |
| `integration` | Configuración completa de integración |
| `fast` | Optimizado para velocidad |

## Ejemplo Completo

```java
@VeldTest(profile = "mock")
class UserServiceIntegrationTest {
    
    @RegisterMock
    private UserRepository userRepository;
    
    @RegisterMock
    private EmailService emailService;
    
    @Inject
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        when(userRepository.findById(1L))
            .thenReturn(new User(1, "Test User"));
    }
    
    @Test
    void shouldCreateUserAndSendEmail() {
        User newUser = new User("new@example.com");
        
        userService.createUser(newUser);
        
        verify(userRepository).save(newUser);
        verify(emailService).sendWelcomeEmail(newUser.getEmail());
    }
    
    @Test
    void shouldThrowExceptionForInvalidUser() {
        User invalidUser = new User("invalid");
        
        assertThrows(ValidationException.class, 
            () -> userService.createUser(invalidUser));
        
        verify(userRepository, never()).save(any());
    }
}
```

## Integración con Mockito

El módulo utiliza Mockito internamente. Puedes usar todas las funcionalidades de Mockito:

```java
@VeldTest
class AdvancedMockingTest {
    
    @RegisterMock
    private CalculatorService calculator;
    
    @Test
    void shouldUseAdvancedMocking() {
        // Stubbing
        when(calculator.add(1, 2)).thenReturn(3);
        when(calculator.divide(10, 2)).thenReturn(5);
        
        // Argument matchers
        when(calculator.multiply(
            argThat(x -> x > 0), 
            anyInt()
        )).thenReturn(100);
        
        // Verification
        verify(calculator, times(2)).add(anyInt(), anyInt());
        verifyNoMoreInteractions(calculator);
    }
}
```

## Configuración Avanzada

### Clases de Configuración

```java
@VeldTest(classes = {TestConfig.class})
class CustomConfigTest { }

@Configuration
class TestConfig {
    @Bean
    public MyService myService() {
        return new MockMyService();
    }
}
```

### Propiedades Personalizadas

```java
@VeldTest(properties = {
    "database.url=jdbc:h2:mem:testdb",
    "app.feature.enabled=true"
})
class PropertiesTest {
    @Inject
    @Value("${app.feature.enabled}")
    private boolean featureEnabled;
    
    @Test
    void shouldInjectProperty() {
        assertThat(featureEnabled).isTrue();
    }
}
```

## Requisitos

- Java 11 o superior
- JUnit 5.10 o superior
- Mockito 5.x

## Licencia

Veld Framework es software libre bajo licencia Apache 2.0.
