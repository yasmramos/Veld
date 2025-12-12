package io.github.yasmramos.veld.weaver;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for @Named/@Qualifier support in VeldClassGenerator.
 */
@DisplayName("VeldClassGenerator Named/Qualifier Tests")
class VeldClassGeneratorNamedTest {
    
    @Nested
    @DisplayName("ComponentMeta Parsing Tests")
    class ComponentMetaParsingTests {
        
        @Test
        @DisplayName("Should parse componentName from metadata line")
        void shouldParseComponentNameFromMetadata() {
            // Format: className||scope||lazy||ctorDeps||fields||methods||interfaces||lifecycle||preDestroy||hasSubscribe||componentName
            String line = "com.example.MyService||SINGLETON||false||||||||com.example.Service||||||false||myService";
            
            VeldClassGenerator.ComponentMeta meta = VeldClassGenerator.ComponentMeta.parse(line);
            
            assertEquals("com.example.MyService", meta.className);
            assertEquals("SINGLETON", meta.scope);
            assertEquals("myService", meta.componentName);
        }
        
        @Test
        @DisplayName("Should handle null componentName when not provided")
        void shouldHandleNullComponentName() {
            String line = "com.example.MyService||SINGLETON||false||||||||com.example.Service||||||false||";
            
            VeldClassGenerator.ComponentMeta meta = VeldClassGenerator.ComponentMeta.parse(line);
            
            assertEquals("com.example.MyService", meta.className);
            assertNull(meta.componentName);
        }
        
        @Test
        @DisplayName("Should handle empty componentName")
        void shouldHandleEmptyComponentName() {
            String line = "com.example.MyService||SINGLETON||false||||||||com.example.Service||||||false||";
            
            VeldClassGenerator.ComponentMeta meta = VeldClassGenerator.ComponentMeta.parse(line);
            
            assertNull(meta.componentName);
        }
    }
    
    @Nested
    @DisplayName("VeldClassGenerator with Named Components")
    class GeneratorWithNamedComponentsTests {
        
        @Test
        @DisplayName("Should generate Veld class with named components")
        void shouldGenerateVeldClassWithNamedComponents() {
            // Create components with names
            VeldClassGenerator.ComponentMeta comp1 = new VeldClassGenerator.ComponentMeta(
                "com.example.ServiceA", "SINGLETON", false,
                List.of(), List.of(), List.of(), List.of("com.example.Service"),
                null, null, null, null, false, "serviceA"
            );
            
            VeldClassGenerator.ComponentMeta comp2 = new VeldClassGenerator.ComponentMeta(
                "com.example.ServiceB", "SINGLETON", false,
                List.of(), List.of(), List.of(), List.of("com.example.Service"),
                null, null, null, null, false, "serviceB"
            );
            
            VeldClassGenerator generator = new VeldClassGenerator(List.of(comp1, comp2));
            Map<String, byte[]> result = generator.generateAll();
            
            // Should generate Veld.class
            assertTrue(result.containsKey("com/veld/Veld"));
            byte[] veldBytes = result.get("com/veld/Veld");
            assertNotNull(veldBytes);
            assertTrue(veldBytes.length > 0);
        }
        
        @Test
        @DisplayName("Should generate Veld class with mixed named and unnamed components")
        void shouldGenerateVeldClassWithMixedComponents() {
            VeldClassGenerator.ComponentMeta namedComp = new VeldClassGenerator.ComponentMeta(
                "com.example.NamedService", "SINGLETON", false,
                List.of(), List.of(), List.of(), List.of(),
                null, null, null, null, false, "myNamedService"
            );
            
            VeldClassGenerator.ComponentMeta unnamedComp = new VeldClassGenerator.ComponentMeta(
                "com.example.UnnamedService", "SINGLETON", false,
                List.of(), List.of(), List.of(), List.of(),
                null, null, null, null, false, null
            );
            
            VeldClassGenerator generator = new VeldClassGenerator(List.of(namedComp, unnamedComp));
            Map<String, byte[]> result = generator.generateAll();
            
            assertTrue(result.containsKey("com/veld/Veld"));
        }
        
        @Test
        @DisplayName("Should generate Veld class with prototype named components")
        void shouldGenerateVeldClassWithPrototypeNamedComponents() {
            VeldClassGenerator.ComponentMeta prototypeNamed = new VeldClassGenerator.ComponentMeta(
                "com.example.PrototypeService", "PROTOTYPE", false,
                List.of(), List.of(), List.of(), List.of(),
                null, null, null, null, false, "protoService"
            );
            
            VeldClassGenerator generator = new VeldClassGenerator(List.of(prototypeNamed));
            Map<String, byte[]> result = generator.generateAll();
            
            assertTrue(result.containsKey("com/veld/Veld"));
        }
    }
    
    @Nested
    @DisplayName("ComponentMeta Constructor Tests")
    class ComponentMetaConstructorTests {
        
        @Test
        @DisplayName("Should create ComponentMeta with all fields")
        void shouldCreateComponentMetaWithAllFields() {
            VeldClassGenerator.FieldInjectionMeta field1 = new VeldClassGenerator.FieldInjectionMeta(
                "field1", "com.example.Field", "Lcom/example/Field;", "private", false, false);
            VeldClassGenerator.MethodInjectionMeta method1 = new VeldClassGenerator.MethodInjectionMeta(
                "setMethod", "(Lcom/example/Arg;)V", List.of("com.example.Arg"));
            
            VeldClassGenerator.ComponentMeta meta = new VeldClassGenerator.ComponentMeta(
                "com.example.TestService",
                "SINGLETON",
                true,
                List.of("com.example.Dep1", "com.example.Dep2"),
                List.of(field1),
                List.of(method1),
                List.of("com.example.Interface1"),
                "init", "()V",
                "destroy", "()V",
                true,
                "testService"
            );
            
            assertEquals("com.example.TestService", meta.className);
            assertEquals("com/example/TestService", meta.internalName);
            assertEquals("SINGLETON", meta.scope);
            assertTrue(meta.lazy);
            assertEquals(2, meta.constructorDeps.size());
            assertEquals(1, meta.fieldInjections.size());
            assertEquals(1, meta.methodInjections.size());
            assertEquals(1, meta.interfaces.size());
            assertEquals("init", meta.postConstructMethod);
            assertEquals("()V", meta.postConstructDescriptor);
            assertEquals("destroy", meta.preDestroyMethod);
            assertEquals("()V", meta.preDestroyDescriptor);
            assertTrue(meta.hasSubscribeMethods);
            assertEquals("testService", meta.componentName);
        }
        
        @Test
        @DisplayName("Should handle null componentName in constructor")
        void shouldHandleNullComponentNameInConstructor() {
            VeldClassGenerator.ComponentMeta meta = new VeldClassGenerator.ComponentMeta(
                "com.example.TestService",
                "PROTOTYPE",
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null, null, null, null,
                false,
                null
            );
            
            assertNull(meta.componentName);
            assertEquals("PROTOTYPE", meta.scope);
        }
    }
}
