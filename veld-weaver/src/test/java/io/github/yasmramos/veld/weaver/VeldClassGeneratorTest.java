package io.github.yasmramos.veld.weaver;

import io.github.yasmramos.veld.weaver.VeldClassGenerator.ComponentMeta;
import io.github.yasmramos.veld.weaver.VeldClassGenerator.FieldInjectionMeta;
import io.github.yasmramos.veld.weaver.VeldClassGenerator.MethodInjectionMeta;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class VeldClassGeneratorTest {

    // Format: className||scope||lazy||ctorDeps||fields||methods||ifaces||postConstruct||preDestroy||subscribe||componentName
    // Indices:    0        1      2       3        4       5        6          7             8          9           10
    // Total: 11 campos (0-10), 10 separadores ||

    @Test
    void testParseBasicMeta() {
        // Empty fields from 3 to 10 (8 empty fields = 8 separadores after lazy)
        String line = "com.example.UserService||SINGLETON||false||||||||||||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals("com.example.UserService", meta.className);
        assertEquals("com/example/UserService", meta.internalName);
        assertEquals("SINGLETON", meta.scope);
        assertFalse(meta.lazy);
    }

    @Test
    void testParseLazyComponent() {
        String line = "com.example.LazyService||PROTOTYPE||true||||||||||||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals("PROTOTYPE", meta.scope);
        assertTrue(meta.lazy);
    }

    @Test
    void testParseWithConstructorDeps() {
        // ctorDeps in parts[3], rest empty
        String line = "com.example.Svc||SINGLETON||false||com.Dep1,com.Dep2||||||||||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals(2, meta.constructorDeps.size());
        assertEquals("com.Dep1", meta.constructorDeps.get(0));
        assertEquals("com.Dep2", meta.constructorDeps.get(1));
    }

    @Test
    void testParseWithFieldInjections() {
        // fields in parts[4]
        String line = "com.example.Svc||SINGLETON||false||||@field1~com.Dep1~Lcom/Dep1;~private~false~false@field2~com.Dep2~Lcom/Dep2;~private~true~false||||||||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals(2, meta.fieldInjections.size());
        assertEquals("field1", meta.fieldInjections.get(0).name);
        assertEquals("com.Dep1", meta.fieldInjections.get(0).depType);
        assertFalse(meta.fieldInjections.get(0).isOptional);
        
        assertEquals("field2", meta.fieldInjections.get(1).name);
        assertTrue(meta.fieldInjections.get(1).isOptional);
    }

    @Test
    void testParseWithMethodInjections() {
        // methods in parts[5]
        String line = "com.example.Svc||SINGLETON||false||||||@setDep~(Lcom/Dep;)V~com.Dep||||||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals(1, meta.methodInjections.size());
        assertEquals("setDep", meta.methodInjections.get(0).name);
        assertEquals("(Lcom/Dep;)V", meta.methodInjections.get(0).descriptor);
    }

    @Test
    void testParseWithInterfaces() {
        // interfaces in parts[6]
        String line = "com.example.Svc||SINGLETON||false||||||||com.IService,com.IRepository||||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals(2, meta.interfaces.size());
        assertTrue(meta.interfaces.contains("com.IService"));
        assertTrue(meta.interfaces.contains("com.IRepository"));
    }

    @Test
    void testParseWithPostConstruct() {
        // postConstruct in parts[7]
        String line = "com.example.Svc||SINGLETON||false||||||||||init~()V||||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals("init", meta.postConstructMethod);
        assertEquals("()V", meta.postConstructDescriptor);
    }

    @Test
    void testParseWithPreDestroy() {
        // preDestroy in parts[8]
        String line = "com.example.Svc||SINGLETON||false||||||||||||cleanup~()V||||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals("cleanup", meta.preDestroyMethod);
        assertEquals("()V", meta.preDestroyDescriptor);
    }

    @Test
    void testParseWithSubscribeMethods() {
        // subscribe in parts[9]
        String line = "com.example.Svc||SINGLETON||false||||||||||||||true||";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertTrue(meta.hasSubscribeMethods);
    }

    @Test
    void testParseWithComponentName() {
        // componentName in parts[10]
        String line = "com.example.Svc||SINGLETON||false||||||||||||||||myService";
        ComponentMeta meta = ComponentMeta.parse(line);
        
        assertEquals("myService", meta.componentName);
    }

    @Test
    void testFieldInjectionMeta() {
        FieldInjectionMeta field = new FieldInjectionMeta("myField", "com.example.Type", "Lcom/example/Type;", "private", true, false);
        
        assertEquals("myField", field.name);
        assertEquals("com.example.Type", field.depType);
        assertEquals("Lcom/example/Type;", field.descriptor);
        assertEquals("private", field.visibility);
        assertTrue(field.isOptional);
        assertFalse(field.isProvider);
    }

    @Test
    void testFieldInjectionMetaWithProvider() {
        FieldInjectionMeta field = new FieldInjectionMeta("providerField", "com.example.Type", "Lcom/example/Provider;", "private", false, true);
        
        assertFalse(field.isOptional);
        assertTrue(field.isProvider);
    }

    @Test
    void testMethodInjectionMeta() {
        List<String> deps = Arrays.asList("com.Dep1", "com.Dep2");
        MethodInjectionMeta method = new MethodInjectionMeta("setServices", "(Lcom/Dep1;Lcom/Dep2;)V", deps);
        
        assertEquals("setServices", method.name);
        assertEquals("(Lcom/Dep1;Lcom/Dep2;)V", method.descriptor);
        assertEquals(2, method.depTypes.size());
    }

    @Test
    void testComponentMetaConstructor() {
        List<FieldInjectionMeta> fields = Arrays.asList(
            new FieldInjectionMeta("f1", "Type1", "LType1;", "private", false, false)
        );
        List<MethodInjectionMeta> methods = Arrays.asList(
            new MethodInjectionMeta("m1", "(LType2;)V", Arrays.asList("Type2"))
        );
        
        ComponentMeta meta = new ComponentMeta(
            "com.example.Test",
            "SINGLETON",
            false,
            Collections.emptyList(),
            fields,
            methods,
            Arrays.asList("com.ITest"),
            "init",
            "()V",
            "destroy",
            "()V",
            true,
            "testComponent",
            Collections.emptyList()
        );
        
        assertEquals("com.example.Test", meta.className);
        assertEquals("com/example/Test", meta.internalName);
        assertEquals("SINGLETON", meta.scope);
        assertFalse(meta.lazy);
        assertEquals(1, meta.fieldInjections.size());
        assertEquals(1, meta.methodInjections.size());
        assertEquals(1, meta.interfaces.size());
        assertEquals("init", meta.postConstructMethod);
        assertEquals("destroy", meta.preDestroyMethod);
        assertTrue(meta.hasSubscribeMethods);
        assertEquals("testComponent", meta.componentName);
    }

    @Test
    void testVeldClassGeneratorConstructor() {
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.Test", "SINGLETON", false, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                null, null, null, null, false, null, Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        assertNotNull(generator);
    }

    @Test
    void testGenerateReturnsNonEmptyBytecode() {
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.example.SimpleService", "SINGLETON", false, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, false, "simpleService", 
                Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        byte[] bytecode = generator.generate();
        
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

    @Test
    void testGenerateAllReturnsMultipleClasses() {
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.example.ServiceA", "SINGLETON", false, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, false, "serviceA", 
                Collections.emptyList()),
            new ComponentMeta("com.example.ServiceB", "PROTOTYPE", true, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, false, "serviceB", 
                Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        java.util.Map<String, byte[]> result = generator.generateAll();
        
        assertNotNull(result);
        // Should contain at least 1 class (registry)
        assertFalse(result.isEmpty());
    }

    @Test
    void testGenerateWithConstructorDeps() {
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.example.WithDeps", "SINGLETON", false, 
                Arrays.asList("com.example.Dependency"), 
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, false, "withDeps", 
                Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        byte[] bytecode = generator.generate();
        
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

    @Test
    void testGenerateWithFieldInjections() {
        List<FieldInjectionMeta> fields = Arrays.asList(
            new FieldInjectionMeta("dep", "com.example.Dep", "Lcom/example/Dep;", "private", false, false)
        );
        
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.example.WithFields", "SINGLETON", false, 
                Collections.emptyList(), fields, Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, false, "withFields", 
                Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        byte[] bytecode = generator.generate();
        
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

    @Test
    void testGenerateWithLifecycleMethods() {
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.example.WithLifecycle", "SINGLETON", false, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), "init", "()V", "destroy", "()V", false, 
                "withLifecycle", Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        byte[] bytecode = generator.generate();
        
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

    @Test
    void testGenerateWithInterfaces() {
        List<ComponentMeta> components = Arrays.asList(
            new ComponentMeta("com.example.WithInterface", "SINGLETON", false, 
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Arrays.asList("com.example.IService"), null, null, null, null, false, 
                "withInterface", Collections.emptyList())
        );
        
        VeldClassGenerator generator = new VeldClassGenerator(components);
        byte[] bytecode = generator.generate();
        
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
    }

}
