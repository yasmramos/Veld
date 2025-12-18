package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;
import io.github.yasmramos.veld.runtime.value.ValueResolver;
import io.github.yasmramos.veld.runtime.Veld;
import io.github.yasmramos.veld.annotation.Named;
import io.github.yasmramos.veld.runtime.event.Event;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.yasmramos.veld.runtime.Veld.*;

/**
 * Tests comprehensivos que verifican que TODAS las características
 * de Veld funcionan automáticamente cuando se hace Veld.get().
 */
@ExtendWith(MockitoExtension.class)
public class IntegrationTests {

    private static final AtomicBoolean postConstructCalled = new AtomicBoolean(false);
    private static final AtomicBoolean preDestroyCalled = new AtomicBoolean(false);
    private static final AtomicBoolean eventReceived = new AtomicBoolean(false);

    @BeforeEach
    void resetFlags() {
        postConstructCalled.set(false);
        preDestroyCalled.set(false);
        eventReceived.set(false);
        
        // Reset system properties for conditional tests
        System.clearProperty("test.property");
        System.clearProperty("veld.profiles.active");
    }

    @AfterEach
    void cleanup() {
        // Cleanup after each test
        try {
            shutdown();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        postConstructCalled.set(false);
        preDestroyCalled.set(false);
        eventReceived.set(false);
    }

    // =============================================================================
    // 1. LIFECYCLE CALLBACKS AUTOMÁTICOS (@PostConstruct, @PreDestroy)
    // =============================================================================

    @Test
    @DisplayName("Debería ejecutar @PostConstruct automáticamente en singleton")
    void shouldExecutePostConstructAutomatically() {
        TestService service = get(TestService.class);
        
        assertNotNull(service);
        assertTrue(postConstructCalled.get(), "@PostConstruct debería haberse ejecutado automáticamente");
    }

    @Test
    @DisplayName("Debería ejecutar @PreDestroy automáticamente en shutdown")
    void shouldExecutePreDestroyAutomatically() {
        TestService service = get(TestService.class);
        assertNotNull(service);
        
        // Reset flag para verificar que se ejecuta en shutdown
        postConstructCalled.set(false);
        preDestroyCalled.set(false);
        
        shutdown();
        
        assertTrue(preDestroyCalled.get(), "@PreDestroy debería haberse ejecutado automáticamente en shutdown");
    }

    @Test
    @DisplayName("Debería ejecutar @PostConstruct automáticamente en prototype")
    void shouldExecutePostConstructAutomaticallyInPrototype() {
        TestPrototypeService service1 = get(TestPrototypeService.class);
        TestPrototypeService service2 = get(TestPrototypeService.class);
        
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2, "Deberían ser instancias diferentes");
        assertTrue(service1.isInitialized(), "Primera instancia debería estar inicializada");
        assertTrue(service2.isInitialized(), "Segunda instancia debería estar inicializada");
    }

    // =============================================================================
    // 2. EVENTBUS REGISTRATION AUTOMÁTICA (@Subscribe)
    // =============================================================================

    @Test
    @DisplayName("Debería registrar automáticamente en EventBus con @Subscribe")
    void shouldRegisterEventBusAutomatically() {
        TestEventSubscriber subscriber = get(TestEventSubscriber.class);
        EventBus eventBus = getEventBus();
        
        assertNotNull(subscriber);
        assertNotNull(eventBus);
        
        // Publish un evento y verificar que se recibe
        eventBus.publish(new TestEvent("test"));
        
        // Esperar un poco para que el evento se procese
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(eventReceived.get(), "El evento debería haberse recibido automáticamente");
    }

    @Test
    @DisplayName("Debería permitir múltiples suscriptores del mismo evento")
    void shouldAllowMultipleEventSubscribers() {
        TestEventSubscriber subscriber1 = get(TestEventSubscriber.class);
        TestEventSubscriber subscriber2 = get(TestEventSubscriber.class);
        EventBus eventBus = getEventBus();
        
        assertNotNull(subscriber1);
        assertNotNull(subscriber2);
        assertNotSame(subscriber1, subscriber2);
        
        // Reset flags
        eventReceived.set(false);
        
        eventBus.publish(new TestEvent("multiple"));
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(eventReceived.get(), "Al menos un suscriptor debería haber recibido el evento");
    }

    // =============================================================================
    // 3. VALUE RESOLUTION AUTOMÁTICA (@Value)
    // =============================================================================

    @Test
    @DisplayName("Debería resolver @Value automáticamente desde propiedades del sistema")
    void shouldResolveValueAutomatically() {
        System.setProperty("test.property", "resolved_value");
        
        TestValueInjection service = get(TestValueInjection.class);
        
        assertNotNull(service);
        assertEquals("resolved_value", service.getPropertyValue());
    }

    @Test
    @DisplayName("Debería resolver @Value con valores por defecto")
    void shouldResolveValueWithDefault() {
        System.clearProperty("test.property");
        
        TestValueInjection service = get(TestValueInjection.class);
        
        assertNotNull(service);
        assertEquals("default_value", service.getPropertyValue());
    }

    @Test
    @DisplayName("Debería resolver @Value numéricas automáticamente")
    void shouldResolveNumericValueAutomatically() {
        System.setProperty("test.number", "42");
        
        TestValueInjection service = get(TestValueInjection.class);
        
        assertNotNull(service);
        assertEquals(42, service.getNumberValue());
    }

    // =============================================================================
    // 4. CONDITIONAL LOADING (@Profile, @ConditionalOnProperty)
    // =============================================================================

    @Test
    @DisplayName("Debería filtrar componentes por @Profile automáticamente")
    void shouldFilterComponentsByProfile() {
        // Configurar perfil activo
        setActiveProfiles("test");
        
        TestProfileService testService = get(TestProfileService.class);
        
        assertNotNull(testService);
        assertEquals("test_profile_active", testService.getProfile());
        
        // El servicio de prod no debería estar disponible en perfil test
        assertThrows(Exception.class, () -> {
            get(TestProductionService.class);
        });
    }

    @Test
    @DisplayName("Debería cargar componentes por @ConditionalOnProperty")
    void shouldLoadComponentsByConditionalProperty() {
        System.setProperty("feature.enabled", "true");
        
        TestConditionalService service = get(TestConditionalService.class);
        
        assertNotNull(service);
        assertTrue(service.isFeatureEnabled());
    }

    @Test
    @DisplayName("Debería no cargar componentes cuando la condición no se cumple")
    void shouldNotLoadComponentsWhenConditionFails() {
        System.setProperty("feature.enabled", "false");
        
        assertThrows(Exception.class, () -> {
            get(TestConditionalService.class);
        });
    }

    // =============================================================================
    // 5. NAMED INJECTION
    // =============================================================================

    @Test
    @DisplayName("Debería inyectar por nombre usando get(Class, String)")
    void shouldInjectByNamedQualifier() {
        TestNamedInjection service = get(TestNamedInjection.class);
        
        assertNotNull(service);
        assertNotNull(service.getPrimaryRepository());
        assertNotNull(service.getSecondaryRepository());
        assertNotSame(service.getPrimaryRepository(), service.getSecondaryRepository());
    }

    // =============================================================================
    // 6. PROVIDER INJECTION
    // =============================================================================

    @Test
    @DisplayName("Debería inyectar Provider<T> automáticamente")
    void shouldInjectProviderAutomatically() {
        TestProviderInjection service = get(TestProviderInjection.class);
        
        assertNotNull(service);
        assertNotNull(service.getRepositoryProvider());
        
        Repository repo1 = service.getRepositoryProvider().get();
        Repository repo2 = service.getRepositoryProvider().get();
        
        assertNotNull(repo1);
        assertNotNull(repo2);
        assertNotSame(repo1, repo2, "Provider debería crear instancias nuevas");
    }

    // =============================================================================
    // 7. OPTIONAL INJECTION
    // =============================================================================

    @Test
    @DisplayName("Debería inyectar Optional<T> para dependencias opcionales")
    void shouldInjectOptionalDependencies() {
        TestOptionalInjection service = get(TestOptionalInjection.class);
        
        assertNotNull(service);
        assertNotNull(service.getOptionalRepository());
        assertTrue(service.getOptionalRepository().isPresent());
        
        // Dependency inexistente debería retornar Optional.empty()
        assertNotNull(service.getOptionalNonExistent());
        assertFalse(service.getOptionalNonExistent().isPresent());
    }

    // =============================================================================
    // 8. LIFECYCLE PROCESSOR INTEGRATION
    // =============================================================================

    @Test
    @DisplayName("Debería proporcionar LifecycleProcessor automáticamente")
    void shouldProvideLifecycleProcessorAutomatically() {
        LifecycleProcessor processor = getLifecycleProcessor();
        
        assertNotNull(processor);
    }

    @Test
    @DisplayName("Debería permitir acceso a LifecycleProcessor desde Veld")
    void shouldAllowLifecycleProcessorAccess() {
        LifecycleProcessor processor = getLifecycleProcessor();
        assertNotNull(processor);
        
        // Verificar que está registrado el servicio
        TestService service = get(TestService.class);
        assertNotNull(service);
    }

    // =============================================================================
    // 9. VALUE RESOLVER INTEGRATION
    // =============================================================================

    @Test
    @DisplayName("Debería proporcionar ValueResolver automáticamente")
    void shouldProvideValueResolverAutomatically() {
        ValueResolver resolver = getValueResolver();
        
        assertNotNull(resolver);
    }

    @Test
    @DisplayName("Debería permitir resolver valores manualmente")
    void shouldAllowManualValueResolution() {
        System.setProperty("manual.property", "manual_value");
        
        ValueResolver resolver = getValueResolver();
        String resolved = resolver.resolve("manual.property");
        
        assertEquals("manual_value", resolved);
    }

    // =============================================================================
    // 10. DEPENDENCIES (@DependsOn)
    // =============================================================================

    @Test
    @DisplayName("Debería respetar dependencias explícitas @DependsOn")
    void shouldRespectExplicitDependencies() {
        // Esto debería funcionar sin errores circulares
        TestDependsOnService service = get(TestDependsOnService.class);
        
        assertNotNull(service);
        assertNotNull(service.getDependency());
    }

    // =============================================================================
    // CLASES DE TEST (COMPONENTES)
    // =============================================================================

    // Lifecycle test components
    @Singleton
    @Component
    public static class TestService {
        @PostConstruct
        public void init() {
            postConstructCalled.set(true);
        }
        
        @PreDestroy
        public void destroy() {
            preDestroyCalled.set(true);
        }
    }

    @Prototype
    @Component
    public static class TestPrototypeService {
        private boolean initialized = false;
        
        @PostConstruct
        public void init() {
            this.initialized = true;
        }
        
        public boolean isInitialized() {
            return initialized;
        }
    }

    // EventBus test components
    @Singleton
    @Component
    public static class TestEventSubscriber {
        @Subscribe
        public void onEvent(TestEvent event) {
            eventReceived.set(true);
        }
    }

    public static class TestEvent extends Event {
        private final String message;
        
        public TestEvent(String message) {
            super();
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }

    // Value injection test components
    @Singleton
    @Component
    public static class TestValueInjection {
        @Value("${test.property:default_value}")
        private String propertyValue;
        
        @Value("${test.number:0}")
        private int numberValue;
        
        public String getPropertyValue() {
            return propertyValue;
        }
        
        public int getNumberValue() {
            return numberValue;
        }
    }

    // Profile test components
    @Profile("test")
    @Singleton
    @Component
    public static class TestProfileService {
        private final String profile;
        
        public TestProfileService() {
            this.profile = "test_profile_active";
        }
        
        public String getProfile() {
            return profile;
        }
    }

    @Profile("prod")
    @Singleton
    @Component
    public static class TestProductionService {
        public TestProductionService() {
            // This should not be available when profile is "test"
        }
    }

    // Conditional test components
    @ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
    @Singleton
    @Component
    public static class TestConditionalService {
        private final boolean featureEnabled;
        
        public TestConditionalService() {
            this.featureEnabled = true;
        }
        
        public boolean isFeatureEnabled() {
            return featureEnabled;
        }
    }

    // Named injection test components
    @Singleton
    @Component
    public static class PrimaryRepository {
        private final String name = "primary";
    }

    @Singleton
    @Component
    public static class SecondaryRepository {
        private final String name = "secondary";
    }

    @Singleton
    @Component
    public static class TestNamedInjection {
        private final PrimaryRepository primaryRepository;
        private final SecondaryRepository secondaryRepository;
        
        @Inject
        public TestNamedInjection(
            @Named("primary") PrimaryRepository primaryRepository,
            @Named("secondary") SecondaryRepository secondaryRepository
        ) {
            this.primaryRepository = primaryRepository;
            this.secondaryRepository = secondaryRepository;
        }
        
        public PrimaryRepository getPrimaryRepository() {
            return primaryRepository;
        }
        
        public SecondaryRepository getSecondaryRepository() {
            return secondaryRepository;
        }
    }

    // Provider injection test components
    @Singleton
    @Component
    public static class Repository {
        private final String id = java.util.UUID.randomUUID().toString();
    }

    @Singleton
    @Component
    public static class TestProviderInjection {
        private final Provider<Repository> repositoryProvider;
        
        @Inject
        public TestProviderInjection(Provider<Repository> repositoryProvider) {
            this.repositoryProvider = repositoryProvider;
        }
        
        public Provider<Repository> getRepositoryProvider() {
            return repositoryProvider;
        }
    }

    // Optional injection test components
    @Singleton
    @Component
    public static class TestOptionalInjection {
        private final java.util.Optional<Repository> optionalRepository;
        private final java.util.Optional<NonExistentComponent> optionalNonExistent;
        
        @Inject
        public TestOptionalInjection(
            java.util.Optional<Repository> optionalRepository,
            java.util.Optional<NonExistentComponent> optionalNonExistent
        ) {
            this.optionalRepository = optionalRepository;
            this.optionalNonExistent = optionalNonExistent;
        }
        
        public java.util.Optional<Repository> getOptionalRepository() {
            return optionalRepository;
        }
        
        public java.util.Optional<NonExistentComponent> getOptionalNonExistent() {
            return optionalNonExistent;
        }
    }

    public static class NonExistentComponent {
        // This component doesn't exist - for optional injection testing
    }

    // DependsOn test components
    @Singleton
    @Component
    public static class DependencyComponent {
        public DependencyComponent() {
            // Simple dependency
        }
    }

    @DependsOn("dependencyComponent")
    @Singleton
    @Component
    public static class TestDependsOnService {
        private final DependencyComponent dependency;
        
        @Inject
        public TestDependsOnService(DependencyComponent dependency) {
            this.dependency = dependency;
        }
        
        public DependencyComponent getDependency() {
            return dependency;
        }
    }
}