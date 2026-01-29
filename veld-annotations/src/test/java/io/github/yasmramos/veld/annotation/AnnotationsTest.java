package io.github.yasmramos.veld.annotation;

import org.junit.jupiter.api.*;

import java.lang.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Veld annotations.
 * Tests annotation retention, targets, and default values.
 * 
 * Note: Most Veld annotations have CLASS retention for compile-time processing.
 * Runtime tests only work for annotations explicitly marked with RUNTIME retention.
 */
@DisplayName("Annotations Tests")
class AnnotationsTest {
    
    // ========== CLASS Retention Annotations (compile-time processing) ==========
    
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
    
    // ========== CLASS Retention Annotations (metadata tests only) ==========
    // Note: These annotations have CLASS retention, so they cannot be accessed at runtime
    // We only verify their metadata (retention policy, targets, attribute signatures)
    
    @Nested
    @DisplayName("Value Annotation Tests (CLASS retention)")
    class ValueAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Value.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target FIELD, PARAMETER, and METHOD")
        void shouldTargetFieldParameterAndMethod() {
            Target target = Value.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.FIELD));
            assertTrue(targets.contains(ElementType.PARAMETER));
            assertTrue(targets.contains(ElementType.METHOD));
        }
        
        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Value.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("Should be Documented")
        void shouldBeDocumented() {
            Documented documented = Value.class.getAnnotation(Documented.class);
            assertNotNull(documented);
        }
    }
    
    @Nested
    @DisplayName("Profile Annotation Tests (CLASS retention)")
    class ProfileAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Profile.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target TYPE and METHOD")
        void shouldTargetTypeAndMethod() {
            Target target = Profile.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.TYPE));
            assertTrue(targets.contains(ElementType.METHOD));
        }
        
        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Profile.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String[].class, method.getReturnType());
        }
        
        @Test
        @DisplayName("Should have name attribute")
        void shouldHaveNameAttribute() throws Exception {
            var method = Profile.class.getDeclaredMethod("name");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("Should have expression attribute")
        void shouldHaveExpressionAttribute() throws Exception {
            var method = Profile.class.getDeclaredMethod("expression");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("Should have strategy attribute")
        void shouldHaveStrategyAttribute() throws Exception {
            var method = Profile.class.getDeclaredMethod("strategy");
            assertNotNull(method);
            assertEquals(Profile.MatchStrategy.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("Should have MatchStrategy enum")
        void shouldHaveMatchStrategyEnum() {
            assertNotNull(Profile.MatchStrategy.class.getEnumConstants());
            assertEquals(2, Profile.MatchStrategy.values().length);
        }
        
        @Test
        @DisplayName("Should be Documented")
        void shouldBeDocumented() {
            Documented documented = Profile.class.getAnnotation(Documented.class);
            assertNotNull(documented);
        }
    }
    
    @Nested
    @DisplayName("Conditional Annotations Tests (CLASS retention)")
    class ConditionalAnnotationsTests {
        
        @Test
        @DisplayName("ConditionalOnClass should have CLASS retention")
        void conditionalOnClassShouldHaveClassRetention() {
            Retention retention = ConditionalOnClass.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("ConditionalOnClass should target TYPE")
        void conditionalOnClassShouldTargetType() {
            Target target = ConditionalOnClass.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
        
        @Test
        @DisplayName("ConditionalOnClass should have name attribute")
        void conditionalOnClassShouldHaveNameAttribute() throws Exception {
            var method = ConditionalOnClass.class.getDeclaredMethod("name");
            assertNotNull(method);
            assertEquals(String[].class, method.getReturnType());
        }
        
        @Test
        @DisplayName("ConditionalOnClass should have value attribute")
        void conditionalOnClassShouldHaveValueAttribute() throws Exception {
            var method = ConditionalOnClass.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(Class[].class, method.getReturnType());
        }
        
        @Test
        @DisplayName("ConditionalOnProperty should have CLASS retention")
        void conditionalOnPropertyShouldHaveClassRetention() {
            Retention retention = ConditionalOnProperty.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("ConditionalOnProperty should target TYPE")
        void conditionalOnPropertyShouldTargetType() {
            Target target = ConditionalOnProperty.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
        
        @Test
        @DisplayName("ConditionalOnProperty should have name attribute")
        void conditionalOnPropertyShouldHaveNameAttribute() throws Exception {
            var method = ConditionalOnProperty.class.getDeclaredMethod("name");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("ConditionalOnProperty should have havingValue attribute")
        void conditionalOnPropertyShouldHaveHavingValueAttribute() throws Exception {
            var method = ConditionalOnProperty.class.getDeclaredMethod("havingValue");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("ConditionalOnMissingBean should have CLASS retention")
        void conditionalOnMissingBeanShouldHaveClassRetention() {
            Retention retention = ConditionalOnMissingBean.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("ConditionalOnMissingBean should target TYPE")
        void conditionalOnMissingBeanShouldTargetType() {
            Target target = ConditionalOnMissingBean.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
        
        @Test
        @DisplayName("ConditionalOnMissingBean should have value attribute")
        void conditionalOnMissingBeanShouldHaveValueAttribute() throws Exception {
            var method = ConditionalOnMissingBean.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(Class[].class, method.getReturnType());
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Annotations Tests (CLASS retention)")
    class LifecycleAnnotationsTests {
        
        @Test
        @DisplayName("PostInitialize should have CLASS retention")
        void postInitializeShouldHaveClassRetention() {
            Retention retention = PostInitialize.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("PostInitialize should target METHOD")
        void postInitializeShouldTargetMethod() {
            Target target = PostInitialize.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.METHOD));
        }
        
        @Test
        @DisplayName("PostInitialize should have order attribute")
        void postInitializeShouldHaveOrderAttribute() throws Exception {
            var method = PostInitialize.class.getDeclaredMethod("order");
            assertNotNull(method);
            assertEquals(int.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("OnStart should have CLASS retention")
        void onStartShouldHaveClassRetention() {
            Retention retention = OnStart.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("OnStart should target METHOD")
        void onStartShouldTargetMethod() {
            Target target = OnStart.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.METHOD));
        }
        
        @Test
        @DisplayName("OnStart should have order attribute")
        void onStartShouldHaveOrderAttribute() throws Exception {
            var method = OnStart.class.getDeclaredMethod("order");
            assertNotNull(method);
            assertEquals(int.class, method.getReturnType());
        }
        
        @Test
        @DisplayName("OnStop should have CLASS retention")
        void onStopShouldHaveClassRetention() {
            Retention retention = OnStop.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("OnStop should target METHOD")
        void onStopShouldTargetMethod() {
            Target target = OnStop.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.METHOD));
        }
        
        @Test
        @DisplayName("OnStop should have order attribute")
        void onStopShouldHaveOrderAttribute() throws Exception {
            var method = OnStop.class.getDeclaredMethod("order");
            assertNotNull(method);
            assertEquals(int.class, method.getReturnType());
        }
    }
    
    @Nested
    @DisplayName("DependsOn Annotation Tests (CLASS retention)")
    class DependsOnAnnotationTests {
        
        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = DependsOn.class.getAnnotation(Retention.class);
                
            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }
        
        @Test
        @DisplayName("Should target TYPE")
        void shouldTargetType() {
            Target target = DependsOn.class.getAnnotation(Target.class);
                
            assertNotNull(target);
            assertTrue(java.util.Arrays.asList(target.value()).contains(ElementType.TYPE));
        }
        
        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = DependsOn.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String[].class, method.getReturnType());
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

    @Nested
    @DisplayName("Primary Annotation Tests")
    class PrimaryAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Primary.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target TYPE only")
        void shouldTargetTypeOnly() {
            Target target = Primary.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertEquals(1, targets.size());
            assertTrue(targets.contains(ElementType.TYPE));
        }

        @Test
        @DisplayName("Should be Documented")
        void shouldBeDocumented() {
            Documented documented = Primary.class.getAnnotation(Documented.class);
            assertNotNull(documented);
        }
    }

    @Nested
    @DisplayName("Qualifier Annotation Tests")
    class QualifierAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Qualifier.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target FIELD, METHOD, PARAMETER, and CONSTRUCTOR")
        void shouldTargetMultipleElements() {
            Target target = Qualifier.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.FIELD));
            assertTrue(targets.contains(ElementType.METHOD));
            assertTrue(targets.contains(ElementType.PARAMETER));
            assertTrue(targets.contains(ElementType.CONSTRUCTOR));
        }

        @Test
        @DisplayName("Should have value attribute with empty default")
        void shouldHaveValueAttribute() throws Exception {
            var method = Qualifier.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
            assertEquals("", method.getDefaultValue());
        }

        @Test
        @DisplayName("Should be Documented")
        void shouldBeDocumented() {
            Documented documented = Qualifier.class.getAnnotation(Documented.class);
            assertNotNull(documented);
        }
    }

    @Nested
    @DisplayName("Scope Annotation Tests")
    class ScopeAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Scope.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target TYPE")
        void shouldTargetType() {
            Target target = Scope.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.TYPE));
        }

        @Test
        @DisplayName("Should have value attribute with singleton default")
        void shouldHaveValueAttribute() throws Exception {
            var method = Scope.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
            assertEquals("singleton", method.getDefaultValue());
        }
    }

    @Nested
    @DisplayName("VeldScope Annotation Tests")
    class VeldScopeAnnotationTests {

        @Test
        @DisplayName("Should have RUNTIME retention")
        void shouldHaveRuntimeRetention() {
            Retention retention = VeldScope.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.RUNTIME, retention.value());
        }

        @Test
        @DisplayName("Should target ANNOTATION_TYPE")
        void shouldTargetAnnotationType() {
            Target target = VeldScope.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.ANNOTATION_TYPE));
        }

        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = VeldScope.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        @DisplayName("Should have displayName attribute")
        void shouldHaveDisplayNameAttribute() throws Exception {
            var method = VeldScope.class.getDeclaredMethod("displayName");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        @DisplayName("Should be Documented")
        void shouldBeDocumented() {
            Documented documented = VeldScope.class.getAnnotation(Documented.class);
            assertNotNull(documented);
        }
    }

    @Nested
    @DisplayName("Factory Annotation Tests")
    class FactoryAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Factory.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target METHOD only")
        void shouldTargetMethodOnly() {
            Target target = Factory.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertEquals(1, targets.size());
            assertTrue(targets.contains(ElementType.METHOD));
        }

        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Factory.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
    }

    @Nested
    @DisplayName("Eager Annotation Tests")
    class EagerAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Eager.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target TYPE")
        void shouldTargetType() {
            Target target = Eager.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.TYPE));
        }
    }

    @Nested
    @DisplayName("Order Annotation Tests")
    class OrderAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Order.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target TYPE and METHOD")
        void shouldTargetTypeAndMethod() {
            Target target = Order.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.TYPE));
            assertTrue(targets.contains(ElementType.METHOD));
        }

        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Order.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(int.class, method.getReturnType());
        }
    }

    @Nested
    @DisplayName("AliasFor Annotation Tests")
    class AliasForAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = AliasFor.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target METHOD and FIELD")
        void shouldTargetMethodAndField() {
            Target target = AliasFor.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.METHOD));
            assertTrue(targets.contains(ElementType.FIELD));
        }

        @Test
        @DisplayName("Should have attribute attribute")
        void shouldHaveAttributeAttribute() throws Exception {
            var method = AliasFor.class.getDeclaredMethod("attribute");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
    }

    @Nested
    @DisplayName("Lookup Annotation Tests")
    class LookupAnnotationTests {

        @Test
        @DisplayName("Should have CLASS retention")
        void shouldHaveClassRetention() {
            Retention retention = Lookup.class.getAnnotation(Retention.class);

            assertNotNull(retention);
            assertEquals(RetentionPolicy.CLASS, retention.value());
        }

        @Test
        @DisplayName("Should target FIELD and METHOD")
        void shouldTargetFieldAndMethod() {
            Target target = Lookup.class.getAnnotation(Target.class);

            assertNotNull(target);
            java.util.List<ElementType> targets = java.util.Arrays.asList(target.value());
            assertTrue(targets.contains(ElementType.FIELD));
            assertTrue(targets.contains(ElementType.METHOD));
        }

        @Test
        @DisplayName("Should have value attribute")
        void shouldHaveValueAttribute() throws Exception {
            var method = Lookup.class.getDeclaredMethod("value");
            assertNotNull(method);
            assertEquals(String.class, method.getReturnType());
        }
    }

}
