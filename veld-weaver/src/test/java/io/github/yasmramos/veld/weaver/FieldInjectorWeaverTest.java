package io.github.yasmramos.veld.weaver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for FieldInjectorWeaver.
 */
@DisplayName("FieldInjectorWeaver Tests")
class FieldInjectorWeaverTest {
    
    private final FieldInjectorWeaver weaver = new FieldInjectorWeaver();
    
    @Nested
    @DisplayName("Single Field Injection")
    class SingleFieldInjection {
        
        @Test
        @DisplayName("Should generate setter for private field with @Inject")
        void shouldGenerateSetterForPrivateFieldWithInject() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService",
                "repository",
                "Lcom/example/Repository;",
                "Lcom/veld/annotation/Inject;"
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified(), "Class should be modified");
            assertFalse(result.hasError(), "Should not have errors");
            assertEquals(1, result.getAddedSetters().size());
            assertEquals("__di_set_repository", result.getAddedSetters().get(0));
            
            // Verify the generated setter exists in bytecode
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_repository");
            
            assertNotNull(setter, "Setter method should exist");
            assertEquals("(Lcom/example/Repository;)V", setter.desc);
            assertTrue((setter.access & ACC_PUBLIC) != 0, "Setter should be public");
            assertTrue((setter.access & ACC_SYNTHETIC) != 0, "Setter should be synthetic");
        }
        
        @Test
        @DisplayName("Should generate setter for private field with javax.inject.Inject")
        void shouldGenerateSetterForJavaxInject() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService",
                "service",
                "Lcom/example/OtherService;",
                "Ljavax/inject/Inject;"
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            assertEquals(List.of("__di_set_service"), result.getAddedSetters());
        }
        
        @Test
        @DisplayName("Should generate setter for private field with jakarta.inject.Inject")
        void shouldGenerateSetterForJakartaInject() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService",
                "config",
                "Lcom/example/Config;",
                "Ljakarta/inject/Inject;"
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            assertEquals(List.of("__di_set_config"), result.getAddedSetters());
        }
        
        @Test
        @DisplayName("Should generate setter for private field with @Value")
        void shouldGenerateSetterForValueAnnotation() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService",
                "timeout",
                "I",
                "Lcom/veld/annotation/Value;"
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            assertEquals(List.of("__di_set_timeout"), result.getAddedSetters());
            
            // Verify setter takes int parameter
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_timeout");
            assertEquals("(I)V", setter.desc, "Setter should take int parameter");
        }
    }
    
    @Nested
    @DisplayName("Field Visibility")
    class FieldVisibility {
        
        @Test
        @DisplayName("Should NOT generate setter for public field")
        void shouldNotGenerateSetterForPublicField() {
            byte[] originalClass = generateClassWithField(
                "com/example/TestService",
                "repository",
                "Lcom/example/Repository;",
                "Lcom/veld/annotation/Inject;",
                ACC_PUBLIC
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertFalse(result.wasModified(), "Public field should not need weaving");
        }
        
        @Test
        @DisplayName("Should generate setter for protected field (needed for cross-package access)")
        void shouldGenerateSetterForProtectedField() {
            byte[] originalClass = generateClassWithField(
                "com/example/TestService",
                "repository",
                "Lcom/example/Repository;",
                "Lcom/veld/annotation/Inject;",
                ACC_PROTECTED
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified(), "Protected field needs weaving for cross-package access");
        }
        
        @Test
        @DisplayName("Should generate setter for package-private field (needed for cross-package access)")
        void shouldGenerateSetterForPackagePrivateField() {
            byte[] originalClass = generateClassWithField(
                "com/example/TestService",
                "repository",
                "Lcom/example/Repository;",
                "Lcom/veld/annotation/Inject;",
                0  // package-private
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified(), "Package-private field needs weaving for cross-package access");
        }
    }
    
    @Nested
    @DisplayName("No Injection Annotation")
    class NoInjectionAnnotation {
        
        @Test
        @DisplayName("Should NOT generate setter for field without @Inject")
        void shouldNotGenerateSetterForNonInjectField() {
            byte[] originalClass = generateClassWithoutAnnotation(
                "com/example/TestService",
                "repository",
                "Lcom/example/Repository;"
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertFalse(result.wasModified(), "Non-inject field should not need weaving");
        }
    }
    
    @Nested
    @DisplayName("Multiple Fields")
    class MultipleFields {
        
        @Test
        @DisplayName("Should generate setters for multiple private @Inject fields")
        void shouldGenerateSettersForMultipleFields() {
            byte[] originalClass = generateClassWithMultiplePrivateInjectFields(
                "com/example/TestService",
                new String[] { "repoA", "repoB", "config" },
                new String[] { "Lcom/example/RepoA;", "Lcom/example/RepoB;", "Lcom/example/Config;" }
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            assertEquals(3, result.getAddedSetters().size());
            assertTrue(result.getAddedSetters().contains("__di_set_repoA"));
            assertTrue(result.getAddedSetters().contains("__di_set_repoB"));
            assertTrue(result.getAddedSetters().contains("__di_set_config"));
        }
        
        @Test
        @DisplayName("Should only generate setters for private fields in mixed visibility class")
        void shouldOnlyGenerateSettersForPrivateFieldsInMixedClass() {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(V11, ACC_PUBLIC, "com/example/MixedService", null, "java/lang/Object", null);
            
            // Private field with @Inject - should get setter
            FieldVisitor fv1 = cw.visitField(ACC_PRIVATE, "privateField", "Lcom/example/Dep;", null, null);
            fv1.visitAnnotation("Lcom/veld/annotation/Inject;", false).visitEnd();
            fv1.visitEnd();
            
            // Public field with @Inject - should NOT get setter
            FieldVisitor fv2 = cw.visitField(ACC_PUBLIC, "publicField", "Lcom/example/Dep;", null, null);
            fv2.visitAnnotation("Lcom/veld/annotation/Inject;", false).visitEnd();
            fv2.visitEnd();
            
            // Private field WITHOUT @Inject - should NOT get setter
            cw.visitField(ACC_PRIVATE, "normalField", "Lcom/example/Dep;", null, null).visitEnd();
            
            // Add default constructor
            addDefaultConstructor(cw, "java/lang/Object");
            
            cw.visitEnd();
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(cw.toByteArray());
            
            assertTrue(result.wasModified());
            assertEquals(1, result.getAddedSetters().size());
            assertEquals("__di_set_privateField", result.getAddedSetters().get(0));
        }
    }
    
    @Nested
    @DisplayName("Primitive Types")
    class PrimitiveTypes {
        
        @Test
        @DisplayName("Should generate setter for int field")
        void shouldGenerateSetterForIntField() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService", "count", "I", "Lcom/veld/annotation/Value;");
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_count");
            assertEquals("(I)V", setter.desc);
        }
        
        @Test
        @DisplayName("Should generate setter for long field")
        void shouldGenerateSetterForLongField() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService", "timestamp", "J", "Lcom/veld/annotation/Value;");
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_timestamp");
            assertEquals("(J)V", setter.desc);
        }
        
        @Test
        @DisplayName("Should generate setter for double field")
        void shouldGenerateSetterForDoubleField() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService", "rate", "D", "Lcom/veld/annotation/Value;");
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_rate");
            assertEquals("(D)V", setter.desc);
        }
        
        @Test
        @DisplayName("Should generate setter for boolean field")
        void shouldGenerateSetterForBooleanField() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService", "enabled", "Z", "Lcom/veld/annotation/Value;");
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified());
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_enabled");
            assertEquals("(Z)V", setter.desc);
        }
    }
    
    @Nested
    @DisplayName("Re-weaving Prevention")
    class ReWeavingPrevention {
        
        @Test
        @DisplayName("Should not add setter if it already exists")
        void shouldNotAddSetterIfAlreadyExists() {
            // Create a class that already has the synthetic setter
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(V11, ACC_PUBLIC, "com/example/AlreadyWoven", null, "java/lang/Object", null);
            
            // Private field with @Inject
            FieldVisitor fv = cw.visitField(ACC_PRIVATE, "repo", "Lcom/example/Repo;", null, null);
            fv.visitAnnotation("Lcom/veld/annotation/Inject;", false).visitEnd();
            fv.visitEnd();
            
            // Existing setter (as if already woven)
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, 
                "__di_set_repo", "(Lcom/example/Repo;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, "com/example/AlreadyWoven", "repo", "Lcom/example/Repo;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            
            addDefaultConstructor(cw, "java/lang/Object");
            cw.visitEnd();
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(cw.toByteArray());
            
            assertFalse(result.wasModified(), "Should not modify already-woven class");
        }
    }
    
    @Nested
    @DisplayName("Directory Weaving")
    class DirectoryWeaving {
        
        @Test
        @DisplayName("Should weave all class files in directory")
        void shouldWeaveAllClassFilesInDirectory(@TempDir Path tempDir) throws IOException {
            // Create class file 1
            byte[] class1 = generateClassWithPrivateInjectField(
                "com/example/Service1", "dep", "Lcom/example/Dep;", "Lcom/veld/annotation/Inject;");
            Path classFile1 = tempDir.resolve("com/example/Service1.class");
            Files.createDirectories(classFile1.getParent());
            Files.write(classFile1, class1);
            
            // Create class file 2
            byte[] class2 = generateClassWithPrivateInjectField(
                "com/example/Service2", "config", "Lcom/example/Config;", "Lcom/veld/annotation/Inject;");
            Path classFile2 = tempDir.resolve("com/example/Service2.class");
            Files.write(classFile2, class2);
            
            // Create class file 3 (no injection)
            byte[] class3 = generateClassWithoutAnnotation(
                "com/example/NoInject", "field", "Lcom/example/Dep;");
            Path classFile3 = tempDir.resolve("com/example/NoInject.class");
            Files.write(classFile3, class3);
            
            List<FieldInjectorWeaver.WeavingResult> results = weaver.weaveDirectory(tempDir);
            
            assertEquals(2, results.size(), "Should have 2 modified classes");
            assertTrue(results.stream().allMatch(r -> r.wasModified()));
            
            // Verify files were modified
            byte[] woven1 = Files.readAllBytes(classFile1);
            byte[] woven2 = Files.readAllBytes(classFile2);
            
            ClassNode wovenClass1 = parseClass(woven1);
            ClassNode wovenClass2 = parseClass(woven2);
            
            assertNotNull(findMethod(wovenClass1, "__di_set_dep"));
            assertNotNull(findMethod(wovenClass2, "__di_set_config"));
        }
        
        @Test
        @DisplayName("Should handle non-existent directory gracefully")
        void shouldHandleNonExistentDirectory(@TempDir Path tempDir) throws IOException {
            Path nonExistent = tempDir.resolve("does/not/exist");
            
            List<FieldInjectorWeaver.WeavingResult> results = weaver.weaveDirectory(nonExistent);
            
            assertTrue(results.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Static Fields")
    class StaticFields {
        
        @Test
        @DisplayName("Should generate static setter for private static field")
        void shouldGenerateStaticSetterForPrivateStaticField() {
            byte[] originalClass = generateClassWithField(
                "com/example/StaticService",
                "instance",
                "Lcom/example/StaticService;",
                "Lcom/veld/annotation/Inject;",
                ACC_PRIVATE | ACC_STATIC
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified(), "Class should be modified");
            assertTrue(result.getAddedSetters().get(0).contains("static"), "Setter should be marked as static");
            
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_instance");
            
            assertNotNull(setter, "Static setter should exist");
            assertTrue((setter.access & ACC_STATIC) != 0, "Setter should be static");
            assertTrue((setter.access & ACC_PUBLIC) != 0, "Setter should be public");
            assertTrue((setter.access & ACC_SYNTHETIC) != 0, "Setter should be synthetic");
        }
        
        @Test
        @DisplayName("Static setter should use PUTSTATIC opcode")
        void staticSetterShouldUsePutstatic() {
            byte[] originalClass = generateClassWithField(
                "com/example/StaticService",
                "config",
                "Lcom/example/Config;",
                "Lcom/veld/annotation/Inject;",
                ACC_PRIVATE | ACC_STATIC
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_config");
            
            // Find PUTSTATIC instruction
            boolean hasPutstatic = false;
            for (AbstractInsnNode insn : setter.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (fieldInsn.getOpcode() == PUTSTATIC) {
                        hasPutstatic = true;
                        assertEquals("config", fieldInsn.name);
                        break;
                    }
                }
            }
            assertTrue(hasPutstatic, "Static setter should use PUTSTATIC");
        }
        
        @Test
        @DisplayName("Should generate static setter for primitive static field")
        void shouldGenerateStaticSetterForPrimitiveStaticField() {
            byte[] originalClass = generateClassWithField(
                "com/example/Counter",
                "count",
                "I",
                "Lcom/veld/annotation/Value;",
                ACC_PRIVATE | ACC_STATIC
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_count");
            
            assertNotNull(setter);
            assertEquals("(I)V", setter.desc, "Setter should take int parameter");
            assertTrue((setter.access & ACC_STATIC) != 0, "Setter should be static");
        }
    }
    
    @Nested
    @DisplayName("Final Fields")
    class FinalFields {
        
        @Test
        @DisplayName("Should remove final modifier and generate setter")
        void shouldRemoveFinalModifierAndGenerateSetter() {
            byte[] originalClass = generateClassWithField(
                "com/example/ImmutableService",
                "dependency",
                "Lcom/example/Dep;",
                "Lcom/veld/annotation/Inject;",
                ACC_PRIVATE | ACC_FINAL
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified(), "Class should be modified");
            
            ClassNode weavedClass = parseClass(result.getBytecode());
            
            // Verify final modifier was removed from field
            FieldNode field = findField(weavedClass, "dependency");
            assertNotNull(field);
            assertFalse((field.access & ACC_FINAL) != 0, "Final modifier should be removed");
            
            // Verify setter was generated
            MethodNode setter = findMethod(weavedClass, "__di_set_dependency");
            assertNotNull(setter, "Setter should exist");
        }
        
        @Test
        @DisplayName("Should handle private final primitive field")
        void shouldHandlePrivateFinalPrimitiveField() {
            byte[] originalClass = generateClassWithField(
                "com/example/Constants",
                "maxSize",
                "I",
                "Lcom/veld/annotation/Value;",
                ACC_PRIVATE | ACC_FINAL
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            ClassNode weavedClass = parseClass(result.getBytecode());
            
            FieldNode field = findField(weavedClass, "maxSize");
            assertFalse((field.access & ACC_FINAL) != 0, "Final should be removed");
            
            MethodNode setter = findMethod(weavedClass, "__di_set_maxSize");
            assertNotNull(setter);
            assertEquals("(I)V", setter.desc);
        }
    }
    
    @Nested
    @DisplayName("Static Final Fields")
    class StaticFinalFields {
        
        @Test
        @DisplayName("Should handle private static final field")
        void shouldHandlePrivateStaticFinalField() {
            byte[] originalClass = generateClassWithField(
                "com/example/Singleton",
                "INSTANCE",
                "Lcom/example/Singleton;",
                "Lcom/veld/annotation/Inject;",
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            
            assertTrue(result.wasModified(), "Class should be modified");
            
            ClassNode weavedClass = parseClass(result.getBytecode());
            
            // Verify final modifier was removed
            FieldNode field = findField(weavedClass, "INSTANCE");
            assertFalse((field.access & ACC_FINAL) != 0, "Final should be removed");
            assertTrue((field.access & ACC_STATIC) != 0, "Static should remain");
            
            // Verify static setter was generated
            MethodNode setter = findMethod(weavedClass, "__di_set_INSTANCE");
            assertNotNull(setter);
            assertTrue((setter.access & ACC_STATIC) != 0, "Setter should be static");
        }
        
        @Test
        @DisplayName("Static final setter should use PUTSTATIC")
        void staticFinalSetterShouldUsePutstatic() {
            byte[] originalClass = generateClassWithField(
                "com/example/Config",
                "DEFAULT",
                "Ljava/lang/String;",
                "Lcom/veld/annotation/Value;",
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_DEFAULT");
            
            boolean hasPutstatic = false;
            for (AbstractInsnNode insn : setter.instructions) {
                if (insn instanceof FieldInsnNode && insn.getOpcode() == PUTSTATIC) {
                    hasPutstatic = true;
                    break;
                }
            }
            assertTrue(hasPutstatic, "Static final setter should use PUTSTATIC");
        }
        
        @Test
        @DisplayName("Should handle static final long field")
        void shouldHandleStaticFinalLongField() {
            byte[] originalClass = generateClassWithField(
                "com/example/Timing",
                "TIMEOUT",
                "J",
                "Lcom/veld/annotation/Value;",
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            ClassNode weavedClass = parseClass(result.getBytecode());
            
            MethodNode setter = findMethod(weavedClass, "__di_set_TIMEOUT");
            assertNotNull(setter);
            assertEquals("(J)V", setter.desc, "Setter should take long parameter");
            assertTrue((setter.access & ACC_STATIC) != 0, "Setter should be static");
        }
    }
    
    @Nested
    @DisplayName("Bytecode Verification")
    class BytecodeVerification {
        
        @Test
        @DisplayName("Generated setter should have correct bytecode structure")
        void generatedSetterShouldHaveCorrectBytecodeStructure() {
            byte[] originalClass = generateClassWithPrivateInjectField(
                "com/example/TestService",
                "repository",
                "Lcom/example/Repository;",
                "Lcom/veld/annotation/Inject;"
            );
            
            FieldInjectorWeaver.WeavingResult result = weaver.weaveClass(originalClass);
            ClassNode weavedClass = parseClass(result.getBytecode());
            MethodNode setter = findMethod(weavedClass, "__di_set_repository");
            
            // Verify instruction sequence
            InsnList instructions = setter.instructions;
            
            int i = 0;
            // Skip any labels/line numbers
            while (i < instructions.size() && !(instructions.get(i) instanceof VarInsnNode)) {
                i++;
            }
            
            // ALOAD 0 (this)
            assertTrue(instructions.get(i) instanceof VarInsnNode);
            VarInsnNode aload0 = (VarInsnNode) instructions.get(i);
            assertEquals(ALOAD, aload0.getOpcode());
            assertEquals(0, aload0.var);
            i++;
            
            // ALOAD 1 (value)
            assertTrue(instructions.get(i) instanceof VarInsnNode);
            VarInsnNode aload1 = (VarInsnNode) instructions.get(i);
            assertEquals(ALOAD, aload1.getOpcode());
            assertEquals(1, aload1.var);
            i++;
            
            // PUTFIELD
            assertTrue(instructions.get(i) instanceof FieldInsnNode);
            FieldInsnNode putfield = (FieldInsnNode) instructions.get(i);
            assertEquals(PUTFIELD, putfield.getOpcode());
            assertEquals("com/example/TestService", putfield.owner);
            assertEquals("repository", putfield.name);
            assertEquals("Lcom/example/Repository;", putfield.desc);
            i++;
            
            // RETURN
            assertTrue(instructions.get(i) instanceof InsnNode);
            assertEquals(RETURN, instructions.get(i).getOpcode());
        }
    }
    
    @Nested
    @DisplayName("WeavingResult Tests")
    class WeavingResultTests {
        
        @Test
        @DisplayName("unchanged should create unmodified result")
        void unchangedShouldCreateUnmodifiedResult() {
            FieldInjectorWeaver.WeavingResult result = FieldInjectorWeaver.WeavingResult.unchanged("com.example.Test");
            
            assertEquals("com.example.Test", result.getClassName());
            assertFalse(result.wasModified());
            assertFalse(result.hasError());
            assertNull(result.getBytecode());
            assertTrue(result.getAddedSetters().isEmpty());
        }
        
        @Test
        @DisplayName("modified should create modified result with bytecode")
        void modifiedShouldCreateModifiedResult() {
            byte[] bytecode = new byte[]{1, 2, 3};
            List<String> setters = List.of("__di_set_field1", "__di_set_field2");
            
            FieldInjectorWeaver.WeavingResult result = FieldInjectorWeaver.WeavingResult.modified(
                "com.example.Service", bytecode, setters);
            
            assertEquals("com.example.Service", result.getClassName());
            assertTrue(result.wasModified());
            assertFalse(result.hasError());
            assertArrayEquals(bytecode, result.getBytecode());
            assertEquals(2, result.getAddedSetters().size());
        }
        
        @Test
        @DisplayName("error should create error result")
        void errorShouldCreateErrorResult() {
            FieldInjectorWeaver.WeavingResult result = FieldInjectorWeaver.WeavingResult.error(
                "com.example.Broken", "Failed to weave");
            
            assertEquals("com.example.Broken", result.getClassName());
            assertFalse(result.wasModified());
            assertTrue(result.hasError());
            assertEquals("Failed to weave", result.getErrorMessage());
        }
        
        @Test
        @DisplayName("toString should show error message for error result")
        void toStringShouldShowErrorMessage() {
            FieldInjectorWeaver.WeavingResult result = FieldInjectorWeaver.WeavingResult.error(
                "com.example.Test", "Parse error");
            
            String str = result.toString();
            assertTrue(str.contains("ERROR"));
            assertTrue(str.contains("Parse error"));
        }
        
        @Test
        @DisplayName("toString should show setters for modified result")
        void toStringShouldShowSettersForModified() {
            FieldInjectorWeaver.WeavingResult result = FieldInjectorWeaver.WeavingResult.modified(
                "com.example.Test", new byte[0], List.of("__di_set_x"));
            
            String str = result.toString();
            assertTrue(str.contains("setters"));
            assertTrue(str.contains("__di_set_x"));
        }
        
        @Test
        @DisplayName("toString should show unchanged for unmodified result")
        void toStringShouldShowUnchanged() {
            FieldInjectorWeaver.WeavingResult result = FieldInjectorWeaver.WeavingResult.unchanged("com.example.Test");
            
            String str = result.toString();
            assertTrue(str.contains("unchanged"));
        }
    }
    
    // ==================== Helper Methods ====================
    
    private byte[] generateClassWithPrivateInjectField(String className, String fieldName, 
                                                        String fieldDesc, String annotationDesc) {
        return generateClassWithField(className, fieldName, fieldDesc, annotationDesc, ACC_PRIVATE);
    }
    
    private byte[] generateClassWithField(String className, String fieldName, String fieldDesc,
                                          String annotationDesc, int fieldAccess) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V11, ACC_PUBLIC, className, null, "java/lang/Object", null);
        
        FieldVisitor fv = cw.visitField(fieldAccess, fieldName, fieldDesc, null, null);
        AnnotationVisitor av = fv.visitAnnotation(annotationDesc, false);
        av.visitEnd();
        fv.visitEnd();
        
        addDefaultConstructor(cw, "java/lang/Object");
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private byte[] generateClassWithoutAnnotation(String className, String fieldName, String fieldDesc) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V11, ACC_PUBLIC, className, null, "java/lang/Object", null);
        
        cw.visitField(ACC_PRIVATE, fieldName, fieldDesc, null, null).visitEnd();
        
        addDefaultConstructor(cw, "java/lang/Object");
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private byte[] generateClassWithMultiplePrivateInjectFields(String className, 
                                                                 String[] fieldNames, 
                                                                 String[] fieldDescs) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V11, ACC_PUBLIC, className, null, "java/lang/Object", null);
        
        for (int i = 0; i < fieldNames.length; i++) {
            FieldVisitor fv = cw.visitField(ACC_PRIVATE, fieldNames[i], fieldDescs[i], null, null);
            fv.visitAnnotation("Lcom/veld/annotation/Inject;", false).visitEnd();
            fv.visitEnd();
        }
        
        addDefaultConstructor(cw, "java/lang/Object");
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void addDefaultConstructor(ClassWriter cw, String superClass) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    private ClassNode parseClass(byte[] bytecode) {
        ClassReader reader = new ClassReader(bytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        return classNode;
    }
    
    private MethodNode findMethod(ClassNode classNode, String methodName) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodName)) {
                return method;
            }
        }
        return null;
    }
    
    private FieldNode findField(ClassNode classNode, String fieldName) {
        for (FieldNode field : classNode.fields) {
            if (field.name.equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
}
