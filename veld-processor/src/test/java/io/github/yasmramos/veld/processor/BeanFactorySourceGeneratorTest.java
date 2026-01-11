package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.annotation.ScopeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BeanFactorySourceGenerator.
 * Tests code generation without requiring compilation.
 */
class BeanFactorySourceGeneratorTest {

    private FactoryInfo createTestFactory() {
        return new FactoryInfo("com.example.ServiceFactory", "serviceFactory");
    }

    private FactoryInfo.BeanMethod createTestBeanMethod(String returnType, String methodName) {
        return new FactoryInfo.BeanMethod(
            methodName,
            "()V",
            returnType,
            "Lcom/example/" + returnType + ";",
            methodName,
            false
        );
    }

    @Nested
    @DisplayName("BeanFactorySourceGenerator - Scope Generation")
    class ScopeGenerationTests {

        @Test
        @DisplayName("Should generate SINGLETON scope by default")
        void shouldGenerateSingletonScopeByDefault() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("return ScopeType.SINGLETON;"),
                "Generated code should return ScopeType.SINGLETON by default");
            assertFalse(code.contains("ScopeType.PROTOTYPE"),
                "Generated code should not contain PROTOTYPE scope");
        }

        @Test
        @DisplayName("Should generate PROTOTYPE scope when specified")
        void shouldGeneratePrototypeScopeWhenSpecified() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            beanMethod.setScope(ScopeType.PROTOTYPE);
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("return ScopeType.PROTOTYPE;"),
                "Generated code should return ScopeType.PROTOTYPE when specified");
        }
    }

    @Nested
    @DisplayName("BeanFactorySourceGenerator - Lifecycle Generation")
    class LifecycleGenerationTests {

        @Test
        @DisplayName("Should generate invokePostConstruct method")
        void shouldGenerateInvokePostConstructMethod() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            beanMethod.setPostConstruct("init", "()V");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("public void invokePostConstruct"),
                "Generated code should have invokePostConstruct method");
            assertTrue(code.contains("instance.init()"),
                "Generated code should call init() on the instance");
        }

        @Test
        @DisplayName("Should generate invokePreDestroy method")
        void shouldGenerateInvokePreDestroyMethod() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            beanMethod.setPreDestroy("cleanup", "()V");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("public void invokePreDestroy"),
                "Generated code should have invokePreDestroy method");
            assertTrue(code.contains("instance.cleanup()"),
                "Generated code should call cleanup() on the instance");
        }

        @Test
        @DisplayName("Should not call lifecycle methods when not present")
        void shouldNotCallLifecycleMethodsWhenAbsent() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            // Don't set any lifecycle methods
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            // Method should exist but be empty
            assertTrue(code.contains("public void invokePostConstruct"),
                "Generated code should have invokePostConstruct method");
            assertTrue(code.contains("public void invokePreDestroy"),
                "Generated code should have invokePreDestroy method");
        }
    }

    @Nested
    @DisplayName("BeanFactorySourceGenerator - Qualifier Generation")
    class QualifierGenerationTests {

        @Test
        @DisplayName("Should generate getQualifier method when qualifier is present")
        void shouldGenerateGetQualifierWhenPresent() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            beanMethod.setQualifier("premium");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("public String getQualifier()"),
                "Generated code should have getQualifier method");
            assertTrue(code.contains("return \"premium\";"),
                "Generated code should return the qualifier value");
        }

        @Test
        @DisplayName("Should not generate getQualifier when no qualifier is present")
        void shouldNotGenerateGetQualifierWhenAbsent() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            // Don't set any qualifier
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertFalse(code.contains("public String getQualifier()"),
                "Generated code should not have getQualifier method when no qualifier is set");
        }
    }

    @Nested
    @DisplayName("BeanFactorySourceGenerator - Factory Class Name")
    class FactoryClassNameTests {

        @Test
        @DisplayName("Should generate correct factory class name with index")
        void shouldGenerateCorrectFactoryClassName() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("com.example.MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 5);
            String className = generator.getFactoryClassName();

            assertEquals("com.example.MyService$$VeldBeanFactory$5", className,
                "Factory class name should include the return type and index");
        }
    }

    @Nested
    @DisplayName("BeanFactorySourceGenerator - Code Structure")
    class CodeStructureTests {

        @Test
        @DisplayName("Should generate valid Java source code structure")
        void shouldGenerateValidJavaSourceCode() {
            FactoryInfo factory = createTestFactory();
            // Use fully qualified name for return type to generate package declaration
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("com.example.MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            // Verify basic structure
            assertTrue(code.contains("package com.example;"),
                "Generated code should have package declaration");
            assertTrue(code.contains("public final class"),
                "Generated code should have public final class");
            assertTrue(code.contains("implements ComponentFactory"),
                "Generated code should implement ComponentFactory interface");
            assertTrue(code.endsWith("}\n"),
                "Generated code should end with closing brace");
        }

        @Test
        @DisplayName("Should include required imports")
        void shouldIncludeRequiredImports() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("import io.github.yasmramos.veld.Veld;"),
                "Should import Veld");
            assertTrue(code.contains("import io.github.yasmramos.veld.runtime.ComponentFactory;"),
                "Should import ComponentFactory");
            assertTrue(code.contains("import io.github.yasmramos.veld.annotation.ScopeType;"),
                "Should import Scope");
        }

        @Test
        @DisplayName("Should generate getComponentType method")
        void shouldGenerateGetComponentType() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("public Class<MyService> getComponentType()"),
                "Should generate getComponentType method");
            assertTrue(code.contains("return MyService.class;"),
                "Should return the component type class");
        }

        @Test
        @DisplayName("Should generate getComponentName method")
        void shouldGenerateGetComponentName() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("public String getComponentName()"),
                "Should generate getComponentName method");
            assertTrue(code.contains("return \"createService\";"),
                "Should return the bean name");
        }

        @Test
        @DisplayName("Should generate create method with no parameters")
        void shouldGenerateCreateMethodWithVeld() {
            FactoryInfo factory = createTestFactory();
            FactoryInfo.BeanMethod beanMethod = createTestBeanMethod("MyService", "createService");
            factory.addBeanMethod(beanMethod);

            BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(factory, beanMethod, 0);
            String code = generator.generate().toString();

            assertTrue(code.contains("public MyService create()"),
                "Should generate create method without parameters");
            assertTrue(code.contains("factoryInstance.createService()"),
                "Should invoke the bean method on the factory instance");
        }
    }
}
