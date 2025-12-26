package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InjectionPoint and Dependency classes.
 */
@DisplayName("InjectionPoint Tests")
class InjectionPointTest {
    
    @Nested
    @DisplayName("InjectionPoint Tests")
    class InjectionPointBasics {
        
        @Test
        @DisplayName("Should create constructor injection point")
        void shouldCreateConstructorInjectionPoint() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.CONSTRUCTOR,
                "<init>",
                "(Lcom/example/Service;)V",
                List.of(new InjectionPoint.Dependency("com.example.Service", "Lcom/example/Service;", null))
            );
            
            assertEquals(InjectionPoint.Type.CONSTRUCTOR, ip.getType());
            assertEquals("<init>", ip.getName());
            assertEquals("(Lcom/example/Service;)V", ip.getDescriptor());
            assertEquals(1, ip.getDependencies().size());
        }
        
        @Test
        @DisplayName("Should create field injection point")
        void shouldCreateFieldInjectionPoint() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "service",
                "Lcom/example/Service;",
                List.of(new InjectionPoint.Dependency("com.example.Service", "Lcom/example/Service;", null))
            );
            
            assertEquals(InjectionPoint.Type.FIELD, ip.getType());
            assertEquals("service", ip.getName());
        }
        
        @Test
        @DisplayName("Should create method injection point")
        void shouldCreateMethodInjectionPoint() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.METHOD,
                "setService",
                "(Lcom/example/Service;)V",
                List.of(new InjectionPoint.Dependency("com.example.Service", "Lcom/example/Service;", null))
            );
            
            assertEquals(InjectionPoint.Type.METHOD, ip.getType());
            assertEquals("setService", ip.getName());
        }
        
        @Test
        @DisplayName("Should handle multiple dependencies")
        void shouldHandleMultipleDependencies() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.CONSTRUCTOR,
                "<init>",
                "(Lcom/example/ServiceA;Lcom/example/ServiceB;)V",
                List.of(
                    new InjectionPoint.Dependency("com.example.ServiceA", "Lcom/example/ServiceA;", null),
                    new InjectionPoint.Dependency("com.example.ServiceB", "Lcom/example/ServiceB;", null)
                )
            );
            
            assertEquals(2, ip.getDependencies().size());
        }
        
        @Test
        @DisplayName("Dependencies list should be immutable")
        void dependenciesListShouldBeImmutable() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.CONSTRUCTOR,
                "<init>",
                "()V",
                List.of()
            );
            
            assertThrows(UnsupportedOperationException.class, () -> {
                ip.getDependencies().add(new InjectionPoint.Dependency("test", "Ltest;", null));
            });
        }
    }
    
    @Nested
    @DisplayName("Dependency Tests")
    class DependencyTests {
        
        @Test
        @DisplayName("Should create dependency without qualifier")
        void shouldCreateDependencyWithoutQualifier() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service",
                "Lcom/example/Service;",
                null
            );
            
            assertEquals("com.example.Service", dep.getTypeName());
            assertEquals("Lcom/example/Service;", dep.getTypeDescriptor());
            assertNull(dep.getQualifierName());
            assertFalse(dep.hasQualifier());
        }
        
        @Test
        @DisplayName("Should create dependency with qualifier")
        void shouldCreateDependencyWithQualifier() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service",
                "Lcom/example/Service;",
                "primary"
            );
            
            assertEquals("primary", dep.getQualifierName());
            assertTrue(dep.hasQualifier());
        }
        
        @Test
        @DisplayName("Empty string qualifier should not be considered as having qualifier")
        void emptyQualifierShouldNotBeConsideredAsHavingQualifier() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service",
                "Lcom/example/Service;",
                ""
            );
            
            assertFalse(dep.hasQualifier());
        }

        @Test
        @DisplayName("Should create Provider dependency")
        void shouldCreateProviderDependency() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "javax.inject.Provider", "Ljavax/inject/Provider;", null,
                true, false, "com.example.Service", "Lcom/example/Service;"
            );

            assertTrue(dep.isProvider());
            assertFalse(dep.isLazy());
            assertEquals("com.example.Service", dep.getActualTypeName());
            assertEquals("Lcom/example/Service;", dep.getActualTypeDescriptor());
            assertTrue(dep.needsSpecialHandling());
        }

        @Test
        @DisplayName("Should create Lazy dependency")
        void shouldCreateLazyDependency() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service", "Lcom/example/Service;", null,
                false, true, "com.example.Service", "Lcom/example/Service;"
            );

            assertFalse(dep.isProvider());
            assertTrue(dep.isLazy());
            assertTrue(dep.needsSpecialHandling());
        }

        @Test
        @DisplayName("Should create Optional dependency")
        void shouldCreateOptionalDependency() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service", "Lcom/example/Service;", null,
                false, false, true, false,
                "com.example.Service", "Lcom/example/Service;"
            );

            assertTrue(dep.isOptional());
            assertFalse(dep.isOptionalWrapper());
            assertTrue(dep.allowsMissing());
            assertTrue(dep.needsSpecialHandling());
        }

        @Test
        @DisplayName("Should create Optional wrapper dependency")
        void shouldCreateOptionalWrapperDependency() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "java.util.Optional", "Ljava/util/Optional;", null,
                false, false, false, true,
                "com.example.Service", "Lcom/example/Service;"
            );

            assertFalse(dep.isOptional());
            assertTrue(dep.isOptionalWrapper());
            assertTrue(dep.allowsMissing());
            assertTrue(dep.needsSpecialHandling());
        }

        @Test
        @DisplayName("Should create Value dependency")
        void shouldCreateValueDependency() {
            InjectionPoint.Dependency dep = InjectionPoint.Dependency.forValue(
                "java.lang.String", "Ljava/lang/String;", "${app.name}"
            );

            assertTrue(dep.isValueInjection());
            assertEquals("${app.name}", dep.getValueExpression());
            assertFalse(dep.isProvider());
            assertFalse(dep.isLazy());
        }

        @Test
        @DisplayName("Non-value dependency should not be value injection")
        void nonValueDependencyShouldNotBeValueInjection() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service", "Lcom/example/Service;", null
            );

            assertFalse(dep.isValueInjection());
            assertNull(dep.getValueExpression());
        }

        @Test
        @DisplayName("Simple dependency should not need special handling")
        void simpleDependencyShouldNotNeedSpecialHandling() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "com.example.Service", "Lcom/example/Service;", null
            );

            assertFalse(dep.needsSpecialHandling());
            assertFalse(dep.allowsMissing());
        }

        @Test
        @DisplayName("Full constructor should set all properties")
        void fullConstructorShouldSetAllProperties() {
            InjectionPoint.Dependency dep = new InjectionPoint.Dependency(
                "javax.inject.Provider", "Ljavax/inject/Provider;", "named",
                true, true, true, true,
                "com.example.Service", "Lcom/example/Service;", "${value}"
            );

            assertEquals("javax.inject.Provider", dep.getTypeName());
            assertEquals("named", dep.getQualifierName());
            assertTrue(dep.hasQualifier());
            assertTrue(dep.isProvider());
            assertTrue(dep.isLazy());
            assertTrue(dep.isOptional());
            assertTrue(dep.isOptionalWrapper());
            assertTrue(dep.allowsMissing());
            assertTrue(dep.needsSpecialHandling());
            assertEquals("com.example.Service", dep.getActualTypeName());
            assertEquals("${value}", dep.getValueExpression());
            assertTrue(dep.isValueInjection());
        }
    }
    
    @Nested
    @DisplayName("Type Enum Tests")
    class TypeEnumTests {
        
        @Test
        @DisplayName("Should have all three types")
        void shouldHaveAllThreeTypes() {
            assertEquals(3, InjectionPoint.Type.values().length);
            assertNotNull(InjectionPoint.Type.valueOf("CONSTRUCTOR"));
            assertNotNull(InjectionPoint.Type.valueOf("FIELD"));
            assertNotNull(InjectionPoint.Type.valueOf("METHOD"));
        }
    }
    
    @Nested
    @DisplayName("Visibility Tests")
    class VisibilityTests {
        
        @Test
        @DisplayName("Should have all four visibility levels")
        void shouldHaveAllFourVisibilityLevels() {
            assertEquals(4, InjectionPoint.Visibility.values().length);
            assertNotNull(InjectionPoint.Visibility.valueOf("PRIVATE"));
            assertNotNull(InjectionPoint.Visibility.valueOf("PACKAGE_PRIVATE"));
            assertNotNull(InjectionPoint.Visibility.valueOf("PROTECTED"));
            assertNotNull(InjectionPoint.Visibility.valueOf("PUBLIC"));
        }
        
        @Test
        @DisplayName("Should default to PACKAGE_PRIVATE visibility")
        void shouldDefaultToPackagePrivateVisibility() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "service",
                "Lcom/example/Service;",
                List.of(new InjectionPoint.Dependency("com.example.Service", "Lcom/example/Service;", null))
            );
            
            assertEquals(InjectionPoint.Visibility.PACKAGE_PRIVATE, ip.getVisibility());
        }
        
        @Test
        @DisplayName("Should accept explicit visibility")
        void shouldAcceptExplicitVisibility() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "service",
                "Lcom/example/Service;",
                List.of(new InjectionPoint.Dependency("com.example.Service", "Lcom/example/Service;", null)),
                InjectionPoint.Visibility.PRIVATE
            );
            
            assertEquals(InjectionPoint.Visibility.PRIVATE, ip.getVisibility());
        }
    }
    
    @Nested
    @DisplayName("Synthetic Setter Tests")
    class SyntheticSetterTests {
        
        @Test
        @DisplayName("Private field should require synthetic setter")
        void privateFieldShouldRequireSyntheticSetter() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "repository",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency("com.example.Repository", "Lcom/example/Repository;", null)),
                InjectionPoint.Visibility.PRIVATE
            );
            
            assertTrue(ip.requiresSyntheticSetter());
        }
        
        @Test
        @DisplayName("Package-private field should NOT require synthetic setter")
        void packagePrivateFieldShouldNotRequireSyntheticSetter() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "repository",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency("com.example.Repository", "Lcom/example/Repository;", null)),
                InjectionPoint.Visibility.PACKAGE_PRIVATE
            );
            
            assertFalse(ip.requiresSyntheticSetter());
        }
        
        @Test
        @DisplayName("Protected field should NOT require synthetic setter")
        void protectedFieldShouldNotRequireSyntheticSetter() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "repository",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency("com.example.Repository", "Lcom/example/Repository;", null)),
                InjectionPoint.Visibility.PROTECTED
            );
            
            assertFalse(ip.requiresSyntheticSetter());
        }
        
        @Test
        @DisplayName("Public field should NOT require synthetic setter")
        void publicFieldShouldNotRequireSyntheticSetter() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "repository",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency("com.example.Repository", "Lcom/example/Repository;", null)),
                InjectionPoint.Visibility.PUBLIC
            );
            
            assertFalse(ip.requiresSyntheticSetter());
        }
        
        @Test
        @DisplayName("Constructor should NOT require synthetic setter even if private")
        void constructorShouldNotRequireSyntheticSetter() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.CONSTRUCTOR,
                "<init>",
                "()V",
                List.of(),
                InjectionPoint.Visibility.PRIVATE
            );
            
            assertFalse(ip.requiresSyntheticSetter());
        }
        
        @Test
        @DisplayName("Method should NOT require synthetic setter even if private")
        void methodShouldNotRequireSyntheticSetter() {
            InjectionPoint ip = new InjectionPoint(
                InjectionPoint.Type.METHOD,
                "setService",
                "(Lcom/example/Service;)V",
                List.of(new InjectionPoint.Dependency("com.example.Service", "Lcom/example/Service;", null)),
                InjectionPoint.Visibility.PRIVATE
            );
            
            assertFalse(ip.requiresSyntheticSetter());
        }
    }
}
