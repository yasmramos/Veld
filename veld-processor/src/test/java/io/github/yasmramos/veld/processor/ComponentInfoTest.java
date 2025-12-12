package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.Scope;
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
            Scope.SINGLETON
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
            assertEquals(Scope.SINGLETON, componentInfo.getScope());
        }
        
        @Test
        @DisplayName("Should generate correct factory class name")
        void shouldGenerateCorrectFactoryClassName() {
            assertEquals("com.example.MyService$$VeldFactory", componentInfo.getFactoryClassName());
        }
        
        @Test
        @DisplayName("Should generate correct factory internal name")
        void shouldGenerateCorrectFactoryInternalName() {
            assertEquals("com/example/MyService$$VeldFactory", componentInfo.getFactoryInternalName());
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
                Scope.PROTOTYPE
            );
            
            assertEquals(Scope.PROTOTYPE, prototypeInfo.getScope());
        }
    }
}
