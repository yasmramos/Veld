package io.github.yasmramos.veld.weaver;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.tree.*;

/**
 * Bytecode weaver that adds synthetic setter methods for private field injection.
 * 
 * <p>Supports all field modifiers:
 * <ul>
 *   <li>{@code private} - Instance setter with PUTFIELD</li>
 *   <li>{@code private static} - Static setter with PUTSTATIC</li>
 *   <li>{@code private final} - Removes final modifier + instance setter</li>
 *   <li>{@code private static final} - Removes final modifier + static setter</li>
 * </ul>
 * 
 * <p>For each field annotated with @Inject or @Value, this weaver generates a
 * synthetic method {@code __di_set_<fieldName>} that allows the generated factory
 * to inject values without using reflection.
 * 
 * <p>Example transformations:
 * <pre>
 * // Instance field
 * &#64;Inject private Repository repository;
 * → public synthetic void __di_set_repository(Repository value) {
 *       this.repository = value;
 *   }
 * 
 * // Static field
 * &#64;Inject private static Config config;
 * → public static synthetic void __di_set_config(Config value) {
 *       ClassName.config = value;
 *   }
 * 
 * // Final field (modifier removed)
 * &#64;Inject private final Service service;
 * → &#64;Inject private Service service; // final removed
 *   public synthetic void __di_set_service(Service value) {
 *       this.service = value;
 *   }
 * </pre>
 * 
 * <p>This approach maintains the "zero-reflection" principle while supporting
 * all field types, similar to Lombok, Micronaut, and Quarkus.
 */
public class FieldInjectorWeaver {
    
    /** Prefix for generated injection setter methods */
    public static final String SETTER_PREFIX = "__di_set_";
    
    /** Annotations that mark fields for injection */
    private static final Set<String> INJECT_ANNOTATIONS = Set.of(
        "Lio/github/yasmramos/veld/annotation/Inject;",
        "Ljavax/inject/Inject;",
        "Ljakarta/inject/Inject;",
        "Lio/github/yasmramos/veld/annotation/Value;"
    );
    
    private final List<WeavingResult> results = new ArrayList<>();
    
    /**
     * Weaves all class files in the specified directory.
     * Also generates Veld.class from component metadata.
     * 
     * @param classesDirectory the directory containing compiled .class files
     * @return list of weaving results
     * @throws IOException if an I/O error occurs
     */
    public List<WeavingResult> weaveDirectory(Path classesDirectory) throws IOException {
        results.clear();
        
        if (!Files.exists(classesDirectory)) {
            return results;
        }
        
        // First, weave all classes to add synthetic setters
        Files.walk(classesDirectory)
            .filter(path -> path.toString().endsWith(".class"))
            .forEach(this::weaveClassFile);
        
        // Note: Veld.class is now generated as source code by the processor and compiled by javac.
        // The weaver no longer regenerates it - this avoids signature conflicts between
        // the source-generated version (using Set<String>) and the bytecode-generated version.
        // generateVeldClass(classesDirectory);
        
        return new ArrayList<>(results);
    }
    
    /**
     * Weaves a single class file.
     * 
     * @param classFile path to the .class file
     */
    private void weaveClassFile(Path classFile) {
        try {
            byte[] originalBytes = Files.readAllBytes(classFile);
            WeavingResult result = weaveClass(originalBytes);
            
            if (result.modified()) {
                Files.write(classFile, result.bytecode());
                results.add(result);
            }
        } catch (IOException e) {
            results.add(WeavingResult.error(classFile.toString(), e.getMessage()));
        }
    }
    
    /**
     * Weaves a class, adding synthetic setter methods for injectable fields.
     * 
     * @param classBytes the original class bytecode
     * @return the weaving result containing modified bytecode if changes were made
     */
    public WeavingResult weaveClass(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        
        // Find fields that need injection setters
        List<FieldNode> injectableFields = findInjectableFields(classNode);
        
        if (injectableFields.isEmpty()) {
            return WeavingResult.unchanged(classNode.name);
        }
        
        // Check if setters already exist (avoid re-weaving)
        Set<String> existingMethods = new HashSet<>();
        for (MethodNode method : classNode.methods) {
            existingMethods.add(method.name);
        }
        
        List<String> addedSetters = new ArrayList<>();
        List<String> modifiedFields = new ArrayList<>();
        
        // Generate synthetic setters for each injectable field
        for (FieldNode field : injectableFields) {
            String setterName = SETTER_PREFIX + field.name;
            
            // Skip if setter already exists
            if (existingMethods.contains(setterName)) {
                continue;
            }
            
            // Remove final modifier if present (required for injection)
            if (isFinal(field)) {
                removeFinalModifier(field);
                modifiedFields.add(field.name + " (final removed)");
            }
            
            MethodNode setter = generateSetter(classNode.name, field);
            classNode.methods.add(setter);
            addedSetters.add(setterName + (isStatic(field) ? " (static)" : ""));
        }
        
        if (addedSetters.isEmpty()) {
            return WeavingResult.unchanged(classNode.name);
        }
        
        // Write modified class
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        
        return WeavingResult.modified(classNode.name, writer.toByteArray(), addedSetters);
    }
    
    /**
     * Finds all fields that have injection annotations.
     * Generates synthetic setters for non-public fields so they can be accessed
     * from Veld.java (which is in package: io.github.yasmramos.veld).
     */
    private List<FieldNode> findInjectableFields(ClassNode classNode) {
        List<FieldNode> injectableFields = new ArrayList<>();
        
        for (FieldNode field : classNode.fields) {
            if (hasInjectAnnotation(field)) {
                // Generate synthetic setters for all non-public fields
                // (private, package-private, protected) since Veld.java is in
                // io.github.yasmramos.veld and cannot access non-public members
                if ((field.access & ACC_PUBLIC) == 0) {
                    injectableFields.add(field);
                }
            }
        }
        
        return injectableFields;
    }
    
    /**
     * Removes the final modifier from a field to allow injection.
     * This is necessary for final fields since PUTFIELD/PUTSTATIC
     * cannot assign to final fields after initialization.
     * 
     * @param field the field to modify
     * @return true if the field was modified (was final)
     */
    private boolean removeFinalModifier(FieldNode field) {
        if ((field.access & ACC_FINAL) != 0) {
            field.access &= ~ACC_FINAL;
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a field is static.
     */
    private boolean isStatic(FieldNode field) {
        return (field.access & ACC_STATIC) != 0;
    }
    
    /**
     * Checks if a field is final.
     */
    private boolean isFinal(FieldNode field) {
        return (field.access & ACC_FINAL) != 0;
    }
    
    /**
     * Checks if a field has any injection annotation.
     */
    private boolean hasInjectAnnotation(FieldNode field) {
        if (field.visibleAnnotations != null) {
            for (AnnotationNode annotation : field.visibleAnnotations) {
                if (INJECT_ANNOTATIONS.contains(annotation.desc)) {
                    return true;
                }
            }
        }
        
        if (field.invisibleAnnotations != null) {
            for (AnnotationNode annotation : field.invisibleAnnotations) {
                if (INJECT_ANNOTATIONS.contains(annotation.desc)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Generates a synthetic setter method for a field.
     * Handles both instance and static fields.
     * 
     * <p>For instance fields:
     * <pre>
     * public synthetic void __di_set_fieldName(FieldType value) {
     *     this.fieldName = value;
     * }
     * </pre>
     * 
     * <p>For static fields:
     * <pre>
     * public static synthetic void __di_set_fieldName(FieldType value) {
     *     ClassName.fieldName = value;
     * }
     * </pre>
     */
    private MethodNode generateSetter(String classInternalName, FieldNode field) {
        String setterName = SETTER_PREFIX + field.name;
        String setterDesc = "(" + field.desc + ")V";
        boolean isStaticField = isStatic(field);
        
        // Method access: public synthetic, optionally static
        int methodAccess = ACC_PUBLIC | ACC_SYNTHETIC;
        if (isStaticField) {
            methodAccess |= ACC_STATIC;
        }
        
        MethodNode setter = new MethodNode(
            methodAccess,
            setterName,
            setterDesc,
            null,  // no generic signature
            null   // no exceptions
        );
        
        InsnList instructions = setter.instructions;
        
        if (isStaticField) {
            // Static field: ClassName.field = value;
            // Load the parameter (index 0 for static methods)
            int loadOpcode = getLoadOpcode(field.desc);
            instructions.add(new VarInsnNode(loadOpcode, 0));
            
            // PUTSTATIC ClassName.field = value
            instructions.add(new FieldInsnNode(
                PUTSTATIC,
                classInternalName,
                field.name,
                field.desc
            ));
        } else {
            // Instance field: this.field = value;
            // ALOAD 0 - load 'this'
            instructions.add(new VarInsnNode(ALOAD, 0));
            
            // Load the parameter based on its type (index 1 for instance methods)
            int loadOpcode = getLoadOpcode(field.desc);
            instructions.add(new VarInsnNode(loadOpcode, 1));
            
            // PUTFIELD this.field = value
            instructions.add(new FieldInsnNode(
                PUTFIELD,
                classInternalName,
                field.name,
                field.desc
            ));
        }
        
        // RETURN
        instructions.add(new InsnNode(RETURN));
        
        // Set max stack and locals (will be computed by ClassWriter)
        // For wide types (long, double), we need stack size 2
        int stackSize = isWideType(field.desc) ? 2 : 1;
        if (!isStaticField) {
            stackSize++; // +1 for 'this'
        }
        setter.maxStack = stackSize;
        setter.maxLocals = isStaticField ? (isWideType(field.desc) ? 2 : 1) : (isWideType(field.desc) ? 3 : 2);
        
        return setter;
    }
    
    /**
     * Checks if a type descriptor represents a wide type (long or double).
     */
    private boolean isWideType(String desc) {
        return desc.equals("J") || desc.equals("D");
    }
    
    /**
     * Gets the appropriate load opcode for a type descriptor.
     */
    private int getLoadOpcode(String desc) {
        if (desc.length() == 1) {
            // Primitive types
            return switch (desc.charAt(0)) {
                case 'Z', 'B', 'C', 'S', 'I' -> ILOAD;
                case 'J' -> LLOAD;
                case 'F' -> FLOAD;
                case 'D' -> DLOAD;
                default -> ALOAD;
            }; // boolean
            // byte
            // char
            // short
            // int
            // long
            // float
            // double
        }
        // Object or array type
        return ALOAD;
    }
    
    /**
     * Represents the result of weaving a class.
     */
    public record WeavingResult(String className, byte[] bytecode, List<String> addedSetters, 
                                boolean modified, String errorMessage) {
        
        public WeavingResult {
            addedSetters = addedSetters != null ? List.copyOf(addedSetters) : List.of();
        }
        
        public static WeavingResult unchanged(String className) {
            return new WeavingResult(className, null, null, false, null);
        }
        
        public static WeavingResult modified(String className, byte[] bytecode, List<String> addedSetters) {
            return new WeavingResult(className, bytecode, addedSetters, true, null);
        }
        
        public static WeavingResult error(String className, String errorMessage) {
            return new WeavingResult(className, null, null, false, errorMessage);
        }
        
        public boolean hasError() {
            return errorMessage != null;
        }
        
        @Override
        public String toString() {
            if (hasError()) {
                return "WeavingResult[" + className + ", ERROR: " + errorMessage + "]";
            }
            if (modified) {
                return "WeavingResult[" + className + ", setters=" + addedSetters + "]";
            }
            return "WeavingResult[" + className + ", unchanged]";
        }
    }
}
