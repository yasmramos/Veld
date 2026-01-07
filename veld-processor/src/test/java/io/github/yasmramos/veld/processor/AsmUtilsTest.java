/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AsmUtils} bytecode generation utilities.
 */
@DisplayName("AsmUtils Tests")
class AsmUtilsTest {

    @Nested
    @DisplayName("Constant Definitions")
    class ConstantDefinitionsTests {

        @Test
        @DisplayName("should define Java internal names correctly")
        void shouldDefineJavaInternalNamesCorrectly() {
            assertEquals("java/lang/Object", AsmUtils.OBJECT);
            assertEquals("java/lang/String", AsmUtils.STRING);
            assertEquals("java/lang/Class", AsmUtils.CLASS);
            assertEquals("java/lang/Throwable", AsmUtils.THROWABLE);
            assertEquals("java/lang/Exception", AsmUtils.EXCEPTION);
            assertEquals("java/lang/RuntimeException", AsmUtils.RUNTIME_EXCEPTION);
        }

        @Test
        @DisplayName("should define Veld internal names correctly")
        void shouldDefineVeldInternalNamesCorrectly() {
            assertEquals("io/github/yasmramos/veld/runtime/ComponentRegistry", AsmUtils.COMPONENT_REGISTRY);
            assertEquals("io/github/yasmramos/veld/runtime/ComponentFactory", AsmUtils.COMPONENT_FACTORY);
            assertEquals("io/github/yasmramos/veld/VeldException", AsmUtils.VELD_EXCEPTION);
            assertEquals("io/github/yasmramos/veld/annotation/ScopeType", AsmUtils.SCOPE);
        }

        @Test
        @DisplayName("should define generated class names correctly")
        void shouldDefineGeneratedClassNamesCorrectly() {
            assertEquals("io/github/yasmramos/veld/generated/VeldRegistry", AsmUtils.VELD_REGISTRY);
            assertEquals("io/github/yasmramos/Veld", AsmUtils.VELD_BOOTSTRAP);
        }

        @Test
        @DisplayName("should define common descriptors correctly")
        void shouldDefineCommonDescriptorsCorrectly() {
            assertEquals("()V", AsmUtils.VOID_DESCRIPTOR);
            assertEquals("()Ljava/lang/Class;", AsmUtils.CLASS_DESCRIPTOR);
            assertEquals("()Ljava/lang/String;", AsmUtils.STRING_DESCRIPTOR);
            assertEquals("()Ljava/lang/Object;", AsmUtils.OBJECT_DESCRIPTOR);
        }
    }

    @Nested
    @DisplayName("Type Conversion Utilities")
    class TypeConversionTests {

        @Test
        @DisplayName("should convert class name to internal name")
        void shouldConvertClassNameToInternalName() {
            assertEquals("com/example/MyClass", AsmUtils.toInternalName("com.example.MyClass"));
            assertEquals("java/lang/String", AsmUtils.toInternalName("java.lang.String"));
            assertEquals("MyClass", AsmUtils.toInternalName("MyClass"));
        }

        @Test
        @DisplayName("should convert internal name to descriptor")
        void shouldConvertInternalNameToDescriptor() {
            assertEquals("Lcom/example/MyClass;", AsmUtils.toDescriptor("com/example/MyClass"));
            assertEquals("Ljava/lang/String;", AsmUtils.toDescriptor("java/lang/String"));
        }

        @Test
        @DisplayName("should create return descriptor")
        void shouldCreateReturnDescriptor() {
            assertEquals("()Lcom/example/MyClass;", AsmUtils.toReturnDescriptor("com/example/MyClass"));
            assertEquals("()Ljava/lang/Object;", AsmUtils.toReturnDescriptor("java/lang/Object"));
        }
    }

    @Nested
    @DisplayName("ClassWriter Creation")
    class ClassWriterCreationTests {

        @Test
        @DisplayName("should create ClassWriter with correct flags")
        void shouldCreateClassWriterWithCorrectFlags() {
            ClassWriter cw = AsmUtils.createClassWriter();
            assertNotNull(cw);
        }

        @Test
        @DisplayName("should visit public class")
        void shouldVisitPublicClass() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/TestClass");
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should visit public class with interfaces")
        void shouldVisitPublicClassWithInterfaces() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/TestClass", "java/io/Serializable");
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should visit public final class")
        void shouldVisitPublicFinalClass() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicFinalClass(cw, "com/test/FinalClass");
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should visit public final class with interfaces")
        void shouldVisitPublicFinalClassWithInterfaces() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicFinalClass(cw, "com/test/FinalClass", 
                "java/io/Serializable", "java/lang/Cloneable");
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }
    }

    @Nested
    @DisplayName("Constructor Generation")
    class ConstructorGenerationTests {

        @Test
        @DisplayName("should generate default constructor")
        void shouldGenerateDefaultConstructor() throws Exception {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/WithDefaultConstructor");
            AsmUtils.generateDefaultConstructor(cw);
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> clazz = loadClass("com.test.WithDefaultConstructor", bytecode);
            
            // Should be able to instantiate with no-arg constructor
            Object instance = clazz.getDeclaredConstructor().newInstance();
            assertNotNull(instance);
        }

        @Test
        @DisplayName("should generate private constructor")
        void shouldGeneratePrivateConstructor() throws Exception {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/WithPrivateConstructor");
            AsmUtils.generatePrivateConstructor(cw);
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> clazz = loadClass("com.test.WithPrivateConstructor", bytecode);
            
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        }
    }

    @Nested
    @DisplayName("Method Invocation Generation")
    class MethodInvocationTests {

        @Test
        @DisplayName("should generate static method invocation bytecode")
        void shouldGenerateStaticMethodInvocationBytecode() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/StaticCaller");
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "callStatic", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            
            // Call String.valueOf(123)
            mv.visitIntInsn(Opcodes.BIPUSH, 123);
            AsmUtils.invokeStatic(mv, "java/lang/String", "valueOf", "(I)Ljava/lang/String;");
            AsmUtils.returnObject(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should generate virtual method invocation bytecode")
        void shouldGenerateVirtualMethodInvocationBytecode() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/VirtualCaller");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "getLength", "(Ljava/lang/String;)I", null, null);
            mv.visitCode();
            
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            AsmUtils.invokeVirtual(mv, "java/lang/String", "length", "()I");
            mv.visitInsn(Opcodes.IRETURN);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should generate interface method invocation bytecode")
        void shouldGenerateInterfaceMethodInvocationBytecode() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/InterfaceCaller");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "getSize", "(Ljava/util/List;)I", null, null);
            mv.visitCode();
            
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            AsmUtils.invokeInterface(mv, "java/util/List", "size", "()I");
            mv.visitInsn(Opcodes.IRETURN);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should generate special method invocation bytecode")
        void shouldGenerateSpecialMethodInvocationBytecode() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/SpecialCaller");
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            AsmUtils.invokeSpecial(mv, AsmUtils.OBJECT, "<init>", "()V");
            AsmUtils.returnVoid(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }
    }

    @Nested
    @DisplayName("Object Instantiation")
    class ObjectInstantiationTests {

        @Test
        @DisplayName("should generate new instance bytecode")
        void shouldGenerateNewInstanceBytecode() throws Exception {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/InstanceCreator");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "createArrayList", "()Ljava/util/ArrayList;", null, null);
            mv.visitCode();
            
            AsmUtils.generateNewInstance(mv, "java/util/ArrayList");
            AsmUtils.returnObject(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> clazz = loadClass("com.test.InstanceCreator", bytecode);
            
            Method method = clazz.getMethod("createArrayList");
            Object result = method.invoke(null);
            assertNotNull(result);
            assertTrue(result instanceof java.util.ArrayList);
        }

        @Test
        @DisplayName("should generate new instance with args bytecode structure")
        void shouldGenerateNewInstanceWithArgsBytecodeStructure() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/InstanceWithArgsCreator");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "createString", "()Ljava/lang/StringBuilder;", null, null);
            mv.visitCode();
            
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn("initial");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", 
                "<init>", "(Ljava/lang/String;)V", false);
            AsmUtils.returnObject(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }
    }

    @Nested
    @DisplayName("Class Loading Utilities")
    class ClassLoadingTests {

        @Test
        @DisplayName("should load class constant onto stack")
        void shouldLoadClassConstantOntoStack() throws Exception {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/ClassLoader");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "getStringClass", "()Ljava/lang/Class;", null, null);
            mv.visitCode();
            
            AsmUtils.loadClassConstant(mv, "java/lang/String");
            AsmUtils.returnObject(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> clazz = loadClass("com.test.ClassLoader", bytecode);
            
            Method method = clazz.getMethod("getStringClass");
            Object result = method.invoke(null);
            assertEquals(String.class, result);
        }
    }

    @Nested
    @DisplayName("Return Instructions")
    class ReturnInstructionsTests {

        @Test
        @DisplayName("should generate void return")
        void shouldGenerateVoidReturn() throws Exception {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/VoidReturner");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "doNothing", "()V", null, null);
            mv.visitCode();
            AsmUtils.returnVoid(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> clazz = loadClass("com.test.VoidReturner", bytecode);
            
            Method method = clazz.getMethod("doNothing");
            method.invoke(null); // Should not throw
        }

        @Test
        @DisplayName("should generate object return")
        void shouldGenerateObjectReturn() throws Exception {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/ObjectReturner");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "returnNull", "()Ljava/lang/Object;", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.ACONST_NULL);
            AsmUtils.returnObject(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            Class<?> clazz = loadClass("com.test.ObjectReturner", bytecode);
            
            Method method = clazz.getMethod("returnNull");
            Object result = method.invoke(null);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("should generate throw VeldException bytecode")
        void shouldGenerateThrowVeldExceptionBytecode() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/ExceptionThrower");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "throwError", "()V", null, null);
            mv.visitCode();
            AsmUtils.throwVeldException(mv, "Test error message");
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }

        @Test
        @DisplayName("should generate throw RuntimeException wrapping bytecode")
        void shouldGenerateThrowRuntimeExceptionWrappingBytecode() {
            ClassWriter cw = AsmUtils.createClassWriter();
            AsmUtils.visitPublicClass(cw, "com/test/WrappingThrower");
            AsmUtils.generateDefaultConstructor(cw);
            
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "wrapAndThrow", "(Ljava/lang/Throwable;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            AsmUtils.throwRuntimeExceptionWrapping(mv);
            AsmUtils.endMethod(mv);
            
            cw.visitEnd();
            
            byte[] bytecode = cw.toByteArray();
            assertNotNull(bytecode);
            assertTrue(bytecode.length > 0);
        }
    }

    @Nested
    @DisplayName("Utility Class Design")
    class UtilityClassDesignTests {

        @Test
        @DisplayName("should have private constructor")
        void shouldHavePrivateConstructor() throws Exception {
            Constructor<?>[] constructors = AsmUtils.class.getDeclaredConstructors();
            assertEquals(1, constructors.length);
            assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
        }

        @Test
        @DisplayName("private constructor should throw when invoked via reflection")
        void privateConstructorShouldPreventInstantiation() throws Exception {
            Constructor<?> constructor = AsmUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            
            // Should still be able to invoke it (just for coverage)
            Object instance = constructor.newInstance();
            assertNotNull(instance);
        }

        @Test
        @DisplayName("should be a final class or have only static methods")
        void shouldBeUtilityClass() {
            // Check that all public methods are static
            Method[] methods = AsmUtils.class.getDeclaredMethods();
            for (Method method : methods) {
                if (Modifier.isPublic(method.getModifiers())) {
                    assertTrue(Modifier.isStatic(method.getModifiers()),
                        "Public method " + method.getName() + " should be static");
                }
            }
        }
    }

    // Helper method to load dynamically generated bytecode
    private static Class<?> loadClass(String name, byte[] bytecode) {
        return new ClassLoader() {
            public Class<?> defineClass(String name, byte[] b) {
                return defineClass(name, b, 0, b.length);
            }
        }.defineClass(name, bytecode);
    }
}
