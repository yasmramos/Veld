package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.runtime.LegacyScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for private field injection support in ComponentFactoryGenerator.
 */
@DisplayName("ComponentFactoryGenerator Private Field Tests")
class ComponentFactoryGeneratorPrivateFieldTest {
    
    private static final String SYNTHETIC_SETTER_PREFIX = "__di_set_";
    
    @Nested
    @DisplayName("Private Field Injection")
    class PrivateFieldInjection {
        
        @Test
        @DisplayName("Should generate INVOKEVIRTUAL for private field injection")
        void shouldGenerateInvokeVirtualForPrivateField() {
            // Create component with private field
            ComponentInfo component = new ComponentInfo(
                "com.example.MyService",
                "myService",
                LegacyScope.SINGLETON
            );
            
            // Add private field injection point
            InjectionPoint privateField = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "repository",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency(
                    "com.example.Repository",
                    "Lcom/example/Repository;",
                    null
                )),
                InjectionPoint.Visibility.PRIVATE
            );
            component.addFieldInjection(privateField);
            
            // Generate factory
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(component);
            byte[] bytecode = generator.generate();
            
            // Parse and verify bytecode
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            // Find create method
            MethodNode createMethod = findMethod(classNode, "create");
            assertNotNull(createMethod, "create method should exist");
            
            // Verify INVOKEVIRTUAL is used instead of PUTFIELD
            boolean foundSyntheticSetterCall = false;
            for (AbstractInsnNode insn : createMethod.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.name.equals(SYNTHETIC_SETTER_PREFIX + "repository")) {
                        assertEquals(INVOKEVIRTUAL, methodInsn.getOpcode());
                        assertEquals("com/example/MyService", methodInsn.owner);
                        assertEquals("(Lcom/example/Repository;)V", methodInsn.desc);
                        foundSyntheticSetterCall = true;
                    }
                }
            }
            
            assertTrue(foundSyntheticSetterCall, 
                "Should call synthetic setter __di_set_repository for private field");
        }
        
        @Test
        @DisplayName("Should generate PUTFIELD for package-private field injection")
        void shouldGeneratePutFieldForPackagePrivateField() {
            // Create component with package-private field
            ComponentInfo component = new ComponentInfo(
                "com.example.MyService",
                "myService",
                LegacyScope.SINGLETON
            );
            
            // Add package-private field injection point (default visibility)
            InjectionPoint field = new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "repository",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency(
                    "com.example.Repository",
                    "Lcom/example/Repository;",
                    null
                ))
            );
            component.addFieldInjection(field);
            
            // Generate factory
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(component);
            byte[] bytecode = generator.generate();
            
            // Parse and verify bytecode
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            // Find create method
            MethodNode createMethod = findMethod(classNode, "create");
            assertNotNull(createMethod);
            
            // Verify PUTFIELD is used
            boolean foundPutField = false;
            for (AbstractInsnNode insn : createMethod.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (fieldInsn.getOpcode() == PUTFIELD && 
                        fieldInsn.name.equals("repository")) {
                        assertEquals("com/example/MyService", fieldInsn.owner);
                        assertEquals("Lcom/example/Repository;", fieldInsn.desc);
                        foundPutField = true;
                    }
                }
            }
            
            assertTrue(foundPutField, 
                "Should use PUTFIELD for package-private field");
        }
    }
    
    @Nested
    @DisplayName("Mixed Visibility Fields")
    class MixedVisibilityFields {
        
        @Test
        @DisplayName("Should handle mix of private and non-private fields")
        void shouldHandleMixOfPrivateAndNonPrivateFields() {
            ComponentInfo component = new ComponentInfo(
                "com.example.MyService",
                "myService",
                LegacyScope.SINGLETON
            );
            
            // Add private field
            component.addFieldInjection(new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "privateRepo",
                "Lcom/example/Repository;",
                List.of(new InjectionPoint.Dependency(
                    "com.example.Repository", "Lcom/example/Repository;", null)),
                InjectionPoint.Visibility.PRIVATE
            ));
            
            // Add public field
            component.addFieldInjection(new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "publicConfig",
                "Lcom/example/Config;",
                List.of(new InjectionPoint.Dependency(
                    "com.example.Config", "Lcom/example/Config;", null)),
                InjectionPoint.Visibility.PUBLIC
            ));
            
            // Generate factory
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(component);
            byte[] bytecode = generator.generate();
            
            // Parse bytecode
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            MethodNode createMethod = findMethod(classNode, "create");
            assertNotNull(createMethod);
            
            // Count method calls vs field accesses
            int syntheticSetterCalls = 0;
            int putFieldCalls = 0;
            
            for (AbstractInsnNode insn : createMethod.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.name.startsWith(SYNTHETIC_SETTER_PREFIX)) {
                        syntheticSetterCalls++;
                    }
                }
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if (fieldInsn.getOpcode() == PUTFIELD) {
                        putFieldCalls++;
                    }
                }
            }
            
            assertEquals(1, syntheticSetterCalls, 
                "Should have 1 synthetic setter call for private field");
            assertEquals(1, putFieldCalls, 
                "Should have 1 PUTFIELD for public field");
        }
    }
    
    @Nested
    @DisplayName("Primitive Type Private Fields")
    class PrimitiveTypePrivateFields {
        
        @Test
        @DisplayName("Should generate correct setter call for primitive int field")
        void shouldGenerateCorrectSetterForIntField() {
            ComponentInfo component = new ComponentInfo(
                "com.example.MyService", "myService", LegacyScope.SINGLETON);
            
            component.addFieldInjection(new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "timeout",
                "I",
                List.of(InjectionPoint.Dependency.forValue("int", "I", "${timeout:30}")),
                InjectionPoint.Visibility.PRIVATE
            ));
            
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(component);
            byte[] bytecode = generator.generate();
            
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            MethodNode createMethod = findMethod(classNode, "create");
            
            boolean foundSetterCall = false;
            for (AbstractInsnNode insn : createMethod.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.name.equals("__di_set_timeout")) {
                        assertEquals("(I)V", methodInsn.desc, 
                            "Setter should take int parameter");
                        foundSetterCall = true;
                    }
                }
            }
            
            assertTrue(foundSetterCall);
        }
        
        @Test
        @DisplayName("Should generate correct setter call for primitive boolean field")
        void shouldGenerateCorrectSetterForBooleanField() {
            ComponentInfo component = new ComponentInfo(
                "com.example.MyService", "myService", LegacyScope.SINGLETON);
            
            component.addFieldInjection(new InjectionPoint(
                InjectionPoint.Type.FIELD,
                "enabled",
                "Z",
                List.of(InjectionPoint.Dependency.forValue("boolean", "Z", "${enabled:true}")),
                InjectionPoint.Visibility.PRIVATE
            ));
            
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(component);
            byte[] bytecode = generator.generate();
            
            ClassReader reader = new ClassReader(bytecode);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            MethodNode createMethod = findMethod(classNode, "create");
            
            boolean foundSetterCall = false;
            for (AbstractInsnNode insn : createMethod.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.name.equals("__di_set_enabled")) {
                        assertEquals("(Z)V", methodInsn.desc, 
                            "Setter should take boolean parameter");
                        foundSetterCall = true;
                    }
                }
            }
            
            assertTrue(foundSetterCall);
        }
    }
    
    // Helper method
    private MethodNode findMethod(ClassNode classNode, String name) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name)) {
                return method;
            }
        }
        return null;
    }
}
