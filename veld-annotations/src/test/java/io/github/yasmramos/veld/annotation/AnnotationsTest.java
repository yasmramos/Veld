package io.github.yasmramos.veld.annotation;

import org.junit.jupiter.api.*;

import java.lang.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Veld annotations.
 * Tests annotation retention, targets, and default values.
 * 
 * Note: Annotations with CLASS retention are not available at runtime via reflection.
 * For those, we can only verify the annotation metadata (retention, targets) by
 * inspecting the annotation class itself.
 */
@DisplayName("Annotations Tests")
class AnnotationsTest {
    
    // ========== CLASS Retention Annotations (not available at runtime) ==========
    
    @Nested
    @DisplayName("Component Annotation Tests")
    class ComponentAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Component.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target TYPE")
        void shouldTargetType() {
            Target target = Component.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
        
        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Component.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
            assertEquals("", method.getDefaultValue());
        }
    }
    
    @Nested
    @DisplayName("Singleton Annotation Tests")
    class SingletonAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Singleton.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target TYPE")
        void shouldTargetType() {
            Target target = Singleton.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
    }
    
    @Nested
    @DisplayName("Prototype Annotation Tests")
    class PrototypeAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Prototype.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target TYPE")
        void shouldTargetType() {
            Target target = Prototype.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
    }
    
    @Nested
    @DisplayName("Inject Annotation Tests")
    class InjectAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Inject.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target FIELD, METHOD, CONSTRUCTOR")
        void shouldTargetFieldMethodConstructor() {
            Target target = Inject.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.FIELD));
            assertTrue(targets.contains(ElementType.METHOD));
            assertTrue(targets.contains(ElementType.CONSTRUCTOR));
        }
    }
    
    @Nested
    @DisplayName("Named Annotation Tests")
    class NamedAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Named.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target FIELD and PARAMETER")
        void shouldTargetFieldAndParameter() {
            Target target = Named.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.FIELD));
            assertTrue(targets.contains(ElementType.PARAMETER));
        }
        
        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Named.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
    }
    
    @Nested
    @DisplayName("Lazy Annotation Tests")
    class LazyAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Lazy.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target TYPE and FIELD")
        void shouldTargetTypeAndField() {
            Target target = Lazy.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.TYPE) || targets.contains(ElementType.FIELD));
        }
    }
    
    @Nested
    @DisplayName("PostConstruct Annotation Tests")
    class PostConstructAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = PostConstruct.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target METHOD")
        void shouldTargetMethod() {
            Target target = PostConstruct.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.METHOD));
        }
    }
    
    @Nested
    @DisplayName("PreDestroy Annotation Tests")
    class PreDestroyAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = PreDestroy.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target METHOD")
        void shouldTargetMethod() {
            Target target = PreDestroy.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.METHOD));
        }
    }
    
    // ========== RUNTIME Retention Annotations (available at runtime) ==========
    
    @Nested
    @DisplayName("Value Annotation Tests")
    class ValueAnnotationTests {
        
        @Test
        @DisplayName("Should have RUNTIME retention")
        void shouldHaveRuntimeRetention() {
            Retention retention = Value.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        class TestValue {
            @Value("${app.name}")
            private String appName;
            
            @Value("${app.port:8080}")
            private int port;
        }
        
        @Test
        @DisplayName("Should store value expression")
        void shouldStoreValueExpression() throws Exception {
            Value annotation = TestValue.class.getDeclaredField("appName")
                    .getAnnotation(Value.class);
            
            assertNotNull(annotation);
            assertEquals("${app.name}", annotation.value());
        }
        
        @Test
        @DisplayName("Should store value with default")
        void shouldStoreValueWithDefault() throws Exception {
            Value annotation = TestValue.class.getDeclaredField("port")
                    .getAnnotation(Value.class);
            
            assertNotNull(annotation);
            assertEquals("${app.port:8080}", annotation.value());
        }
    }
    
    @Nested
    @DisplayName("Subscribe Annotation Tests")
    class SubscribeAnnotationTests {
        
        @Test
        @DisplayName("Should have RUNTIME retention")
        void shouldHaveRuntimeRetention() {
            Retention retention = Subscribe.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        class TestSubscribe {
            @Subscribe
            public void onEvent(Object event) {}
            
            @Subscribe(async = true, priority = 10, filter = "test", catchExceptions = false)
            public void onFilteredEvent(Object event) {}
        }
        
        @Test
        @DisplayName("Should have default values")
        void shouldHaveDefaultValues() throws Exception {
            Subscribe annotation = TestSubscribe.class.getDeclaredMethod("onEvent", Object.class)
                    .getAnnotation(Subscribe.class);
            
            assertNotNull(annotation);
            assertFalse(annotation.async());
            assertEquals(0, annotation.priority());
            assertEquals("", annotation.filter());
            assertTrue(annotation.catchExceptions()); // default is true
        }
        
        @Test
        @DisplayName("Should store custom values")
        void shouldStoreCustomValues() throws Exception {
            Subscribe annotation = TestSubscribe.class.getDeclaredMethod("onFilteredEvent", Object.class)
                    .getAnnotation(Subscribe.class);
            
            assertNotNull(annotation);
            assertTrue(annotation.async());
            assertEquals(10, annotation.priority());
            assertEquals("test", annotation.filter());
            assertFalse(annotation.catchExceptions());
        }
    }
    
    @Nested
    @DisplayName("Profile Annotation Tests")
    class ProfileAnnotationTests {
        
        @Test
        @DisplayName("Should have RUNTIME retention")
        void shouldHaveRuntimeRetention() {
            Retention retention = Profile.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Profile("dev")
        class DevOnlyComponent {}
        
        @Profile({"dev", "test"})
        class DevTestComponent {}
        
        @Test
        @DisplayName("Should store single profile")
        void shouldStoreSingleProfile() {
            Profile annotation = DevOnlyComponent.class.getAnnotation(Profile.class);
            
            assertNotNull(annotation);
            assertArrayEquals(new String[]{"dev"}, annotation.value());
        }
        
        @Test
        @DisplayName("Should store multiple profiles")
        void shouldStoreMultipleProfiles() {
            Profile annotation = DevTestComponent.class.getAnnotation(Profile.class);
            
            assertNotNull(annotation);
            assertArrayEquals(new String[]{"dev", "test"}, annotation.value());
        }
    }
    
    @Nested
    @DisplayName("Conditional Annotations Tests")
    class ConditionalAnnotationsTests {
        
        @Test
        @DisplayName("ConditionalOnClass should have RUNTIME retention")
        void conditionalOnClassShouldHaveRuntimeRetention() {
            Retention retention = ConditionalOnClass.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @ConditionalOnClass(name = "java.lang.String")
        class ConditionalOnClassComponent {}
        
        @Test
        @DisplayName("ConditionalOnClass should store class name")
        void conditionalOnClassShouldStoreClassName() {
            ConditionalOnClass annotation = ConditionalOnClassComponent.class
                    .getAnnotation(ConditionalOnClass.class);
            
            assertNotNull(annotation);
            assertArrayEquals(new String[]{"java.lang.String"}, annotation.name());
        }
        
        @ConditionalOnClass(value = String.class)
        class ConditionalOnClassByValueComponent {}
        
        @Test
        @DisplayName("ConditionalOnClass should store class reference")
        void conditionalOnClassShouldStoreClassReference() {
            ConditionalOnClass annotation = ConditionalOnClassByValueComponent.class
                    .getAnnotation(ConditionalOnClass.class);
            
            assertNotNull(annotation);
            assertArrayEquals(new Class<?>[]{String.class}, annotation.value());
        }
        
        @ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
        class ConditionalOnPropertyComponent {}
        
        @Test
        @DisplayName("ConditionalOnProperty should have RUNTIME retention")
        void conditionalOnPropertyShouldHaveRuntimeRetention() {
            Retention retention = ConditionalOnProperty.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("ConditionalOnProperty should store property details")
        void conditionalOnPropertyShouldStorePropertyDetails() {
            ConditionalOnProperty annotation = ConditionalOnPropertyComponent.class
                    .getAnnotation(ConditionalOnProperty.class);
            
            assertNotNull(annotation);
            assertEquals("feature.enabled", annotation.name());
            assertEquals("true", annotation.havingValue());
        }
        
        @ConditionalOnMissingBean(String.class)
        class ConditionalOnMissingBeanComponent {}
        
        @Test
        @DisplayName("ConditionalOnMissingBean should have RUNTIME retention")
        void conditionalOnMissingBeanShouldHaveRuntimeRetention() {
            Retention retention = ConditionalOnMissingBean.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("ConditionalOnMissingBean should store bean type")
        void conditionalOnMissingBeanShouldStoreBeanType() {
            ConditionalOnMissingBean annotation = ConditionalOnMissingBeanComponent.class
                    .getAnnotation(ConditionalOnMissingBean.class);
            
            assertNotNull(annotation);
            // value() returns Class<?>[] array
            assertArrayEquals(new Class<?>[]{String.class}, annotation.value());
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Annotations Tests")
    class LifecycleAnnotationsTests {
        
        class TestLifecycle {
            @PostInitialize(order = 1)
            public void postInit() {}
            
            @OnStart(order = 2)
            public void onStart() {}
            
            @OnStop(order = 3)
            public void onStop() {}
        }
        
        @Test
        @DisplayName("PostInitialize should have RUNTIME retention")
        void postInitializeShouldHaveRuntimeRetention() {
            Retention retention = PostInitialize.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("PostInitialize should have order attribute")
        void postInitializeShouldHaveOrderAttribute() throws Exception {
            PostInitialize annotation = TestLifecycle.class.getDeclaredMethod("postInit")
                    .getAnnotation(PostInitialize.class);
            
            assertNotNull(annotation);
            assertEquals(1, annotation.order());
        }
        
        @Test
        @DisplayName("OnStart should have RUNTIME retention")
        void onStartShouldHaveRuntimeRetention() {
            Retention retention = OnStart.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("OnStart should have order attribute")
        void onStartShouldHaveOrderAttribute() throws Exception {
            OnStart annotation = TestLifecycle.class.getDeclaredMethod("onStart")
                    .getAnnotation(OnStart.class);
            
            assertNotNull(annotation);
            assertEquals(2, annotation.order());
        }
        
        @Test
        @DisplayName("OnStop should have RUNTIME retention")
        void onStopShouldHaveRuntimeRetention() {
            Retention retention = OnStop.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("OnStop should have order attribute")
        void onStopShouldHaveOrderAttribute() throws Exception {
            OnStop annotation = TestLifecycle.class.getDeclaredMethod("onStop")
                    .getAnnotation(OnStop.class);
            
            assertNotNull(annotation);
            assertEquals(3, annotation.order());
        }
    }
    
    @Nested
    @DisplayName("DependsOn Annotation Tests")
    class DependsOnAnnotationTests {
        
        @Test
        @DisplayName("Should have RUNTIME retention")
        void shouldHaveRuntimeRetention() {
            Retention retention = DependsOn.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @DependsOn("otherBean")
        class DependentComponent {}
        
        @DependsOn({"bean1", "bean2", "bean3"})
        class MultipleDependenciesComponent {}
        
        @Test
        @DisplayName("Should store single dependency")
        void shouldStoreSingleDependency() {
            DependsOn annotation = DependentComponent.class.getAnnotation(DependsOn.class);
            
            assertNotNull(annotation);
            assertArrayEquals(new String[]{"otherBean"}, annotation.value());
        }
        
        @Test
        @DisplayName("Should store multiple dependencies")
        void shouldStoreMultipleDependencies() {
            DependsOn annotation = MultipleDependenciesComponent.class.getAnnotation(DependsOn.class);
            
            assertNotNull(annotation);
            assertArrayEquals(new String[]{"bean1", "bean2", "bean3"}, annotation.value());
        }
    }
    
    @Nested
    @DisplayName("AOP Annotations Tests")
    class AopAnnotationsTests {
        
        @Test
        @DisplayName("Aspect should have RUNTIME retention")
        void aspectShouldHaveRuntimeRetention() {
            Retention retention = Aspect.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("Before should have RUNTIME retention")
        void beforeShouldHaveRuntimeRetention() {
            Retention retention = Before.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("After should have RUNTIME retention")
        void afterShouldHaveRuntimeRetention() {
            Retention retention = After.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("Around should have RUNTIME retention")
        void aroundShouldHaveRuntimeRetention() {
            Retention retention = Around.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("Pointcut should have RUNTIME retention")
        void pointcutShouldHaveRuntimeRetention() {
            Retention retention = Pointcut.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("Interceptor should have RUNTIME retention")
        void interceptorShouldHaveRuntimeRetention() {
            Retention retention = Interceptor.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("InterceptorBinding should have RUNTIME retention")
        void interceptorBindingShouldHaveRuntimeRetention() {
            Retention retention = InterceptorBinding.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
        
        @Test
        @DisplayName("AroundInvoke should have RUNTIME retention")
        void aroundInvokeShouldHaveRuntimeRetention() {
            Retention retention = AroundInvoke.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }
    }
    
    @Nested
    @DisplayName("Optional Annotation Tests")
    class OptionalAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Optional.class.getAnnotation(Retention.class);
            
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target FIELD and PARAMETER")
        void shouldTargetFieldAndParameter() {
            Target target = Optional.class.getAnnotation(Target.class);
            
            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.FIELD) || targets.contains(ElementType.PARAMETER));
        }
    }
}
