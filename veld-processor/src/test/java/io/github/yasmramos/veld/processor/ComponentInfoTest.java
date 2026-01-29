package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComponentInfo class.
 */
@DisplayName("ComponentInfo Tests")
class ComponentInfoTest {
    
    private ComponentInfo componentInfo;
    
    @BeforeEach
    void setUp() {
        componentInfo = new ComponentInfo(
            "com.example.MyService",
            "myService",
            "singleton"
        );
    }
    
    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {
        
        @Test
        @DisplayName("Should return correct class name")
        void shouldReturnCorrectClassName() {
            assertEquals("com.example.MyService", componentInfo.getClassName());
        }
        
        @Test
        @DisplayName("Should return correct internal name")
        void shouldReturnCorrectInternalName() {
            assertEquals("com/example/MyService", componentInfo.getInternalName());
        }
        
        @Test
        @DisplayName("Should return correct component name")
        void shouldReturnCorrectComponentName() {
            assertEquals("myService", componentInfo.getComponentName());
        }
        
        @Test
        @DisplayName("Should return correct scope")
        void shouldReturnCorrectScope() {
            assertEquals("singleton", componentInfo.getScope());
        }
    }

    @Nested
    @DisplayName("Inner Class Tests")
    class InnerClassTests {

        @Test
        @DisplayName("Should return correct package name for inner class")
        void shouldReturnCorrectPackageNameForInnerClass() {
            ComponentInfo innerClassInfo = new ComponentInfo(
                "io.github.yasmramos.veld.example.ComplexApplicationExample$OrderService",
                "orderService",
                "singleton"
            );

            // Package should be the outer class's package
            assertEquals("io.github.yasmramos.veld.example", innerClassInfo.getPackageName());
        }

        @Test
        @DisplayName("Should return correct simple name for inner class")
        void shouldReturnCorrectSimpleNameForInnerClass() {
            ComponentInfo innerClassInfo = new ComponentInfo(
                "com.example.OuterClass$InnerClass",
                "innerClass",
                "singleton"
            );

            // Simple name should be just "InnerClass", not "OuterClass$InnerClass"
            assertEquals("InnerClass", innerClassInfo.getSimpleName());
        }

        @Test
        @DisplayName("Should handle deeply nested inner class")
        void shouldHandleDeeplyNestedInnerClass() {
            ComponentInfo deeplyNestedInfo = new ComponentInfo(
                "com.example.OuterClass$MiddleClass$InnerClass",
                "innerClass",
                "singleton"
            );

            // Package should still be the outermost class's package
            assertEquals("com.example", deeplyNestedInfo.getPackageName());
            assertEquals("InnerClass", deeplyNestedInfo.getSimpleName());
        }
    }
    
    @Nested
    @DisplayName("Injection Points")
    class InjectionPoints {
        
        @Test
        @DisplayName("Should add and retrieve field injections")
        void shouldAddAndRetrieveFieldInjections() {
            InjectionPoint field = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "logService",
                "Lcom/example/LogService;",
                List.of(new InjectionPoint.Dependency("com.example.LogService", "Lcom/example/LogService;", null))
            );
            
            componentInfo.addFieldInjection(field);
            
            assertTrue(componentInfo.hasFieldInjections());
            assertEquals(1, componentInfo.getFieldInjections().size());
            assertEquals("logService", componentInfo.getFieldInjections().get(0).getName());
        }
        
        @Test
        @DisplayName("Should add and retrieve method injections")
        void shouldAddAndRetrieveMethodInjections() {
            InjectionPoint method = new InjectionPoint(
                InjectionPoint.Type.METHOD,
                "setLogService",
                "(Lcom/example/LogService;)V",
                List.of(new InjectionPoint.Dependency("com.example.LogService", "Lcom/example/LogService;", null))
            );
            
            componentInfo.addMethodInjection(method);
            
            assertTrue(componentInfo.hasMethodInjections());
            assertEquals(1, componentInfo.getMethodInjections().size());
            assertEquals("setLogService", componentInfo.getMethodInjections().get(0).getName());
        }
        
        @Test
        @DisplayName("Should set and retrieve constructor injection")
        void shouldSetAndRetrieveConstructorInjection() {
            InjectionPoint constructor = new InjectionPoint(
                InjectionPoint.Type.CONSTRUCTOR,
                "<init>",
                "(Lcom/example/LogService;)V",
                List.of(new InjectionPoint.Dependency("com.example.LogService", "Lcom/example/LogService;", null))
            );
            
            componentInfo.setConstructorInjection(constructor);
            
            assertNotNull(componentInfo.getConstructorInjection());
            assertEquals("<init>", componentInfo.getConstructorInjection().getName());
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Methods")
    class LifecycleMethods {
        
        @Test
        @DisplayName("Should set and retrieve @PostConstruct method")
        void shouldSetAndRetrievePostConstruct() {
            componentInfo.setPostConstruct("init", "()V");
            
            assertTrue(componentInfo.hasPostConstruct());
            assertEquals("init", componentInfo.getPostConstructMethod());
            assertEquals("()V", componentInfo.getPostConstructDescriptor());
        }
        
        @Test
        @DisplayName("Should set and retrieve @PreDestroy method")
        void shouldSetAndRetrievePreDestroy() {
            componentInfo.setPreDestroy("cleanup", "()V");
            
            assertTrue(componentInfo.hasPreDestroy());
            assertEquals("cleanup", componentInfo.getPreDestroyMethod());
            assertEquals("()V", componentInfo.getPreDestroyDescriptor());
        }
        
        @Test
        @DisplayName("hasPostConstruct should return false when not set")
        void hasPostConstructShouldReturnFalseWhenNotSet() {
            assertFalse(componentInfo.hasPostConstruct());
        }
        
        @Test
        @DisplayName("hasPreDestroy should return false when not set")
        void hasPreDestroyShouldReturnFalseWhenNotSet() {
            assertFalse(componentInfo.hasPreDestroy());
        }
    }
    
    @Nested
    @DisplayName("Interface Support")
    class InterfaceSupport {
        
        @Test
        @DisplayName("Should add and retrieve implemented interfaces")
        void shouldAddAndRetrieveInterfaces() {
            componentInfo.addImplementedInterface("com.example.IService");
            componentInfo.addImplementedInterface("com.example.IRepository");
            
            assertTrue(componentInfo.hasImplementedInterfaces());
            assertEquals(2, componentInfo.getImplementedInterfaces().size());
            assertTrue(componentInfo.getImplementedInterfaces().contains("com.example.IService"));
            assertTrue(componentInfo.getImplementedInterfaces().contains("com.example.IRepository"));
        }
        
        @Test
        @DisplayName("Should return interfaces in internal name format")
        void shouldReturnInterfacesInInternalFormat() {
            componentInfo.addImplementedInterface("com.example.IService");
            
            List<String> internalNames = componentInfo.getImplementedInterfacesInternal();
            
            assertEquals(1, internalNames.size());
            assertEquals("com/example/IService", internalNames.get(0));
        }
        
        @Test
        @DisplayName("hasImplementedInterfaces should return false when empty")
        void hasImplementedInterfacesShouldReturnFalseWhenEmpty() {
            assertFalse(componentInfo.hasImplementedInterfaces());
        }
    }
    
    @Nested
    @DisplayName("Prototype Scope")
    class PrototypeScope {
        
        @Test
        @DisplayName("Should handle prototype scope")
        void shouldHandlePrototypeScope() {
            ComponentInfo prototypeInfo = new ComponentInfo(
                "com.example.Request",
                "request",
                "prototype"
            );
            
            assertEquals("prototype", prototypeInfo.getScope());
        }
    }
    
    @Nested
    @DisplayName("Lazy Initialization")
    class LazyInitialization {
        
        @Test
        @DisplayName("Should return false for lazy by default")
        void shouldReturnFalseForLazyByDefault() {
            assertFalse(componentInfo.isLazy());
        }
        
        @Test
        @DisplayName("Should return true when created with lazy flag")
        void shouldReturnTrueWhenCreatedWithLazyFlag() {
            ComponentInfo lazyInfo = new ComponentInfo(
                "com.example.LazyService",
                "lazyService",
                "singleton",
                null,
                true
            );
            assertTrue(lazyInfo.isLazy());
        }
        
        @Test
        @DisplayName("Should return false when created with lazy=false")
        void shouldReturnFalseWhenCreatedWithLazyFalse() {
            ComponentInfo nonLazyInfo = new ComponentInfo(
                "com.example.EagerService",
                "eagerService",
                "singleton",
                null,
                false
            );
            assertFalse(nonLazyInfo.isLazy());
        }
    }
    
    @Nested
    @DisplayName("Subscribe Methods")
    class SubscribeMethods {
        
        @Test
        @DisplayName("Should return false for hasSubscribeMethods by default")
        void shouldReturnFalseByDefault() {
            assertFalse(componentInfo.hasSubscribeMethods());
        }
        
        @Test
        @DisplayName("Should set and get hasSubscribeMethods")
        void shouldSetAndGetHasSubscribeMethods() {
            componentInfo.setHasSubscribeMethods(true);
            assertTrue(componentInfo.hasSubscribeMethods());
            
            componentInfo.setHasSubscribeMethods(false);
            assertFalse(componentInfo.hasSubscribeMethods());
        }
    }
    
    @Nested
    @DisplayName("Condition Info")
    class ConditionInfoTests {
        
        @Test
        @DisplayName("Should return null for conditionInfo by default")
        void shouldReturnNullByDefault() {
            assertNull(componentInfo.getConditionInfo());
        }
        
        @Test
        @DisplayName("Should set and get conditionInfo")
        void shouldSetAndGetConditionInfo() {
            ConditionInfo condition = new ConditionInfo();
            componentInfo.setConditionInfo(condition);
            assertNotNull(componentInfo.getConditionInfo());
            assertEquals(condition, componentInfo.getConditionInfo());
        }
    }
    
    @Nested
    @DisplayName("Explicit Dependencies")
    class ExplicitDependenciesTests {
        
        @Test
        @DisplayName("Should return empty list by default")
        void shouldReturnEmptyListByDefault() {
            assertTrue(componentInfo.getExplicitDependencies().isEmpty());
        }
        
        @Test
        @DisplayName("Should add explicit dependencies")
        void shouldAddExplicitDependencies() {
            componentInfo.addExplicitDependency("otherBean");
            componentInfo.addExplicitDependency("anotherBean");
            
            assertEquals(2, componentInfo.getExplicitDependencies().size());
            assertTrue(componentInfo.getExplicitDependencies().contains("otherBean"));
            assertTrue(componentInfo.getExplicitDependencies().contains("anotherBean"));
        }
    }
    
    @Nested
    @DisplayName("Order")
    class OrderTests {
        
        @Test
        @DisplayName("Should return 0 for order by default")
        void shouldReturnZeroByDefault() {
            assertEquals(0, componentInfo.getOrder());
        }
        
        @Test
        @DisplayName("Should set and get order value")
        void shouldSetAndGetOrderValue() {
            componentInfo.setOrder(100);
            assertEquals(100, componentInfo.getOrder());
        }
        
        @Test
        @DisplayName("Should handle negative order values")
        void shouldHandleNegativeOrderValues() {
            componentInfo.setOrder(-1);
            assertEquals(-1, componentInfo.getOrder());
        }
        
        @Test
        @DisplayName("Should handle zero order value")
        void shouldHandleZeroOrderValue() {
            componentInfo.setOrder(0);
            assertEquals(0, componentInfo.getOrder());
        }
        
        @Test
        @DisplayName("Should handle high positive order values")
        void shouldHandleHighPositiveOrderValues() {
            componentInfo.setOrder(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, componentInfo.getOrder());
        }
        
        @Test
        @DisplayName("Should handle high negative order values")
        void shouldHandleHighNegativeOrderValues() {
            componentInfo.setOrder(Integer.MIN_VALUE);
            assertEquals(Integer.MIN_VALUE, componentInfo.getOrder());
        }
        
        @Test
        @DisplayName("Should update order value correctly")
        void shouldUpdateOrderValueCorrectly() {
            componentInfo.setOrder(10);
            assertEquals(10, componentInfo.getOrder());

            componentInfo.setOrder(20);
            assertEquals(20, componentInfo.getOrder());

            componentInfo.setOrder(5);
            assertEquals(5, componentInfo.getOrder());
        }
    }

    @Nested
    @DisplayName("Holder Pattern Tests")
    class HolderPatternTests {

        @Test
        @DisplayName("Simple singleton should use holder pattern")
        void simpleSingletonShouldUseHolderPattern() {
            ComponentInfo simpleComponent = new ComponentInfo(
                "com.example.SimpleService",
                "simpleService",
                "singleton"
            );

            assertTrue(simpleComponent.canUseHolderPattern());
            assertNull(simpleComponent.getHolderPatternRestriction());
        }

        @Test
        @DisplayName("Prototype scope should not use holder pattern")
        void prototypeShouldNotUseHolderPattern() {
            ComponentInfo prototypeComponent = new ComponentInfo(
                "com.example.PrototypeService",
                "prototypeService",
                "prototype"
            );

            assertFalse(prototypeComponent.canUseHolderPattern());
            assertNotNull(prototypeComponent.getHolderPatternRestriction());
            assertTrue(prototypeComponent.getHolderPatternRestriction().contains("prototype"));
        }

        @Test
        @DisplayName("Lazy singleton should not use holder pattern")
        void lazySingletonShouldNotUseHolderPattern() {
            ComponentInfo lazyComponent = new ComponentInfo(
                "com.example.LazyService",
                "lazyService",
                "singleton",
                null,
                true
            );

            assertFalse(lazyComponent.canUseHolderPattern());
            assertNotNull(lazyComponent.getHolderPatternRestriction());
            assertTrue(lazyComponent.getHolderPatternRestriction().contains("lazy"));
        }

        @Test
        @DisplayName("Singleton with constructor injection should not use holder pattern")
        void singletonWithInjectionShouldNotUseHolderPattern() {
            ComponentInfo injectedComponent = new ComponentInfo(
                "com.example.InjectedService",
                "injectedService",
                "singleton"
            );
            injectedComponent.setConstructorInjection(
                new InjectionPoint(
                    InjectionPoint.Type.CONSTRUCTOR,
                    "<init>",
                    "(Lcom/example/OtherService;)V",
                    List.of(new InjectionPoint.Dependency("com.example.OtherService", "Lcom/example/OtherService;", null))
                )
            );

            assertFalse(injectedComponent.canUseHolderPattern());
            assertNotNull(injectedComponent.getHolderPatternRestriction());
            assertTrue(injectedComponent.getHolderPatternRestriction().contains("injection"));
        }

        @Test
        @DisplayName("Singleton with field injection should not use holder pattern")
        void singletonWithFieldInjectionShouldNotUseHolderPattern() {
            ComponentInfo injectedComponent = new ComponentInfo(
                "com.example.FieldInjectedService",
                "fieldInjectedService",
                "singleton"
            );
            InjectionPoint fieldInjection = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "dependency",
                "Lcom/example/DependencyService;",
                List.of(new InjectionPoint.Dependency("com.example.DependencyService", "Lcom/example/DependencyService;", null))
            );
            injectedComponent.addFieldInjection(fieldInjection);

            assertFalse(injectedComponent.canUseHolderPattern());
            assertNotNull(injectedComponent.getHolderPatternRestriction());
        }

        @Test
        @DisplayName("Singleton with method injection should not use holder pattern")
        void singletonWithMethodInjectionShouldNotUseHolderPattern() {
            ComponentInfo injectedComponent = new ComponentInfo(
                "com.example.MethodInjectedService",
                "methodInjectedService",
                "singleton"
            );
            InjectionPoint methodInjection = new InjectionPoint(
                InjectionPoint.Type.METHOD,
                "setDependency",
                "(Lcom/example/DependencyService;)V",
                List.of(new InjectionPoint.Dependency("com.example.DependencyService", "Lcom/example/DependencyService;", null))
            );
            injectedComponent.addMethodInjection(methodInjection);

            assertFalse(injectedComponent.canUseHolderPattern());
            assertNotNull(injectedComponent.getHolderPatternRestriction());
        }

        @Test
        @DisplayName("Singleton with @PostConstruct should not use holder pattern")
        void singletonWithPostConstructShouldNotUseHolderPattern() {
            ComponentInfo lifecycleComponent = new ComponentInfo(
                "com.example.LifecycleService",
                "lifecycleService",
                "singleton"
            );
            lifecycleComponent.setPostConstruct("init", "()V");

            assertFalse(lifecycleComponent.canUseHolderPattern());
            assertNotNull(lifecycleComponent.getHolderPatternRestriction());
            assertTrue(lifecycleComponent.getHolderPatternRestriction().contains("PostConstruct"));
        }

        @Test
        @DisplayName("Singleton with @PreDestroy should not use holder pattern")
        void singletonWithPreDestroyShouldNotUseHolderPattern() {
            ComponentInfo lifecycleComponent = new ComponentInfo(
                "com.example.LifecycleService",
                "lifecycleService",
                "singleton"
            );
            lifecycleComponent.setPreDestroy("cleanup", "()V");

            assertFalse(lifecycleComponent.canUseHolderPattern());
            assertNotNull(lifecycleComponent.getHolderPatternRestriction());
            assertTrue(lifecycleComponent.getHolderPatternRestriction().contains("PreDestroy"));
        }

        @Test
        @DisplayName("Singleton with @Conditional should not use holder pattern")
        void singletonWithConditionalShouldNotUseHolderPattern() {
            ComponentInfo conditionalComponent = new ComponentInfo(
                "com.example.ConditionalService",
                "conditionalService",
                "singleton"
            );
            ConditionInfo conditionInfo = new ConditionInfo();
            conditionInfo.addPropertyCondition("feature.enabled", null, false);
            conditionalComponent.setConditionInfo(conditionInfo);

            assertFalse(conditionalComponent.canUseHolderPattern());
            assertNotNull(conditionalComponent.getHolderPatternRestriction());
            assertTrue(conditionalComponent.getHolderPatternRestriction().contains("Conditional"));
        }

        @Test
        @DisplayName("Singleton with AOP interceptors should not use holder pattern")
        void singletonWithAopShouldNotUseHolderPattern() {
            ComponentInfo aopComponent = new ComponentInfo(
                "com.example.AopService",
                "aopService",
                "singleton"
            );
            aopComponent.addAopInterceptor("com.example.LoggingInterceptor");

            assertFalse(aopComponent.canUseHolderPattern());
            assertNotNull(aopComponent.getHolderPatternRestriction());
            assertTrue(aopComponent.getHolderPatternRestriction().contains("AOP"));
        }

        @Test
        @DisplayName("Singleton with @Subscribe methods should not use holder pattern")
        void singletonWithSubscribeShouldNotUseHolderPattern() {
            ComponentInfo eventComponent = new ComponentInfo(
                "com.example.EventService",
                "eventService",
                "singleton"
            );
            eventComponent.setHasSubscribeMethods(true);

            assertFalse(eventComponent.canUseHolderPattern());
            assertNotNull(eventComponent.getHolderPatternRestriction());
            assertTrue(eventComponent.getHolderPatternRestriction().contains("Subscribe"));
        }

        @Test
        @DisplayName("Complex component with multiple restrictions should return first restriction")
        void complexComponentShouldReturnFirstRestriction() {
            ComponentInfo complexComponent = new ComponentInfo(
                "com.example.ComplexService",
                "complexService",
                "prototype"
            );
            complexComponent.setConstructorInjection(
                new InjectionPoint(
                    InjectionPoint.Type.CONSTRUCTOR,
                    "<init>",
                    "(Lcom/example/OtherService;)V",
                    List.of(new InjectionPoint.Dependency("com.example.OtherService", "Lcom/example/OtherService;", null))
                )
            );
            complexComponent.setPostConstruct("init", "()V");

            // Prototype is the first check, so that's what should be reported
            assertFalse(complexComponent.canUseHolderPattern());
            assertNotNull(complexComponent.getHolderPatternRestriction());
            assertTrue(complexComponent.getHolderPatternRestriction().contains("prototype"));
        }

        @Test
        @DisplayName("Eager singleton without dependencies should use holder pattern")
        void eagerSingletonWithoutDependenciesShouldUseHolderPattern() {
            // This is the ideal case for holder pattern
            ComponentInfo idealComponent = new ComponentInfo(
                "com.example.IdealService",
                "idealService",
                "singleton",
                null,
                false  // Not lazy
            );
            // No constructor injection, no field injection, no method injection
            // No lifecycle callbacks, no conditions, no AOP, no event subscriptions

            assertTrue(idealComponent.canUseHolderPattern());
            assertNull(idealComponent.getHolderPatternRestriction());
        }
    }
}
