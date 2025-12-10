package io.github.yasmramos.processor;

import io.github.yasmramos.runtime.Scope;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class VeldSourceGeneratorTest {

    @Test
    void testGenerateEmptyComponents() {
        VeldSourceGenerator generator = new VeldSourceGenerator(Collections.emptyList());
        
        String source = generator.generate();
        
        assertNotNull(source);
        assertTrue(source.contains("package io.github.yasmramos.generated"));
        assertTrue(source.contains("public final class Veld"));
    }

    @Test
    void testGenerateWithSingletonComponent() {
        List<ComponentInfo> components = new ArrayList<>();
        ComponentInfo comp = createTestComponent("com.example.UserService", Scope.SINGLETON);
        components.add(comp);
        
        VeldSourceGenerator generator = new VeldSourceGenerator(components);
        String source = generator.generate();
        
        assertNotNull(source);
        assertTrue(source.contains("UserService"));
        assertTrue(source.contains("static"));
    }

    @Test
    void testGenerateWithPrototypeComponent() {
        List<ComponentInfo> components = new ArrayList<>();
        ComponentInfo comp = createTestComponent("com.example.RequestScope", Scope.PROTOTYPE);
        components.add(comp);
        
        VeldSourceGenerator generator = new VeldSourceGenerator(components);
        String source = generator.generate();
        
        assertNotNull(source);
        assertTrue(source.contains("RequestScope"));
    }

    @Test
    void testGenerateWithMultipleComponents() {
        List<ComponentInfo> components = new ArrayList<>();
        components.add(createTestComponent("com.example.ServiceA", Scope.SINGLETON));
        components.add(createTestComponent("com.example.ServiceB", Scope.SINGLETON));
        components.add(createTestComponent("com.example.ServiceC", Scope.PROTOTYPE));
        
        VeldSourceGenerator generator = new VeldSourceGenerator(components);
        String source = generator.generate();
        
        assertNotNull(source);
        assertTrue(source.contains("ServiceA"));
        assertTrue(source.contains("ServiceB"));
        assertTrue(source.contains("ServiceC"));
    }

    @Test
    void testGeneratedCodeHasPrivateConstructor() {
        VeldSourceGenerator generator = new VeldSourceGenerator(Collections.emptyList());
        String source = generator.generate();
        
        assertTrue(source.contains("private Veld()"));
    }

    @Test
    void testGeneratedCodeHasImports() {
        VeldSourceGenerator generator = new VeldSourceGenerator(Collections.emptyList());
        String source = generator.generate();
        
        assertTrue(source.contains("import java.util.ArrayList"));
        assertTrue(source.contains("import java.util.List"));
    }

    private ComponentInfo createTestComponent(String className, Scope scope) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return new ComponentInfo(className, simpleName, scope, false);
    }
}
