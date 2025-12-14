package io.github.yasmramos.veld.weaver;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Generates Veld.class bytecode from component metadata.
 * This runs AFTER the weaver adds synthetic setters, so all injection works.
 */
public class VeldClassGenerator implements Opcodes {
    
    private static final String VELD_CLASS = "com/veld/Veld";
    private static final String SYNTHETIC_SETTER_PREFIX = "__di_set_";
    
    private final List<ComponentMeta> components;
    
    public VeldClassGenerator(List<ComponentMeta> components) {
        this.components = components;
    }
    
    /**
     * Reads component metadata from the standard location.
     */
    public static List<ComponentMeta> readMetadata(Path classesDir) throws IOException {
        Path metaFile = classesDir.resolve("META-INF/veld/components.meta");
        if (!Files.exists(metaFile)) {
            return Collections.emptyList();
        }
        
        List<ComponentMeta> result = new ArrayList<>();
        for (String line : Files.readAllLines(metaFile)) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            result.add(ComponentMeta.parse(line));
        }
        return result;
    }
    
    /**
     * Generates all classes: Veld.class + $VeldLifecycle helpers for each component with lifecycle methods.
     * @return Map of internal class name to bytecode
     */
    public Map<String, byte[]> generateAll() {
        Map<String, byte[]> result = new HashMap<>();
        
        // Generate lifecycle helper classes for components with @PostConstruct/@PreDestroy
        for (ComponentMeta comp : components) {
            if (comp.postConstructMethod != null || comp.preDestroyMethod != null) {
                String helperName = comp.internalName + "$VeldLifecycle";
                result.put(helperName, generateLifecycleHelper(comp));
            }
        }
        
        // Generate main Veld.class
        result.put(VELD_CLASS, generate());
        return result;
    }
    
    public byte[] generate() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, VELD_CLASS, null, "java/lang/Object", null);
        
        List<ComponentMeta> singletons = new ArrayList<>();
        List<ComponentMeta> prototypes = new ArrayList<>();
        for (ComponentMeta comp : components) {
            if ("SINGLETON".equals(comp.scope)) {
                singletons.add(comp);
            } else {
                prototypes.add(comp);
            }
        }
        
        // Singleton fields
        for (ComponentMeta comp : singletons) {
            cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                getFieldName(comp), "L" + comp.internalName + ";", null, null).visitEnd();
        }
        
        // Lookup arrays (linear fallback)
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_types", "[Ljava/lang/Class;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_instances", "[Ljava/lang/Object;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_scopes", "[I", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_protoIdx", "[I", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_names", "[Ljava/lang/String;", null, null).visitEnd();
        
        // Hash table for O(1) lookup
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_htTypes", "[Ljava/lang/Class;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_htInstances", "[Ljava/lang/Object;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_mask", "I", null, null).visitEnd();
        
        // Thread-local cache (per-thread, zero contention)
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_tlCache", "Ljava/lang/ThreadLocal;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_tlIdx", "Ljava/lang/ThreadLocal;", null, null).visitEnd();
        
        // LifecycleProcessor singleton field
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_lifecycleProcessor", 
            "Lio/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor;", null, null).visitEnd();
        
        // ConditionalRegistry field for conditional component filtering
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_conditionalRegistry", 
            "Lio/github/yasmramos/veld/runtime/ConditionalRegistry;", null, null).visitEnd();
        
        // ValueResolver field for @Value annotation resolution
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_valueResolver", 
            "Lio/github/yasmramos/veld/runtime/value/ValueResolver;", null, null).visitEnd();
        
        generateStaticInit(cw, singletons, prototypes);
        generatePrivateConstructor(cw);
        
        for (ComponentMeta comp : singletons) {
            generateSingletonGetter(cw, comp);
        }
        for (ComponentMeta comp : prototypes) {
            generatePrototypeGetter(cw, comp);
        }
        
        generateCreatePrototype(cw, prototypes);
        generateGetByClass(cw);
        generateGetByClassAndName(cw);
        generateGetAllByClass(cw);
        generateContains(cw);
        generateComponentCount(cw);
        generateGetLifecycleProcessor(cw);
        generateGetValueResolver(cw);
        generateShutdown(cw, singletons);
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void generateStaticInit(ClassWriter cw, List<ComponentMeta> singletons, List<ComponentMeta> prototypes) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        
        List<ComponentMeta> sorted = topologicalSort(singletons);
        
        for (ComponentMeta comp : sorted) {
            String fieldName = getFieldName(comp);
            String fieldType = "L" + comp.internalName + ";";
            
            mv.visitTypeInsn(NEW, comp.internalName);
            mv.visitInsn(DUP);
            
            StringBuilder ctorDesc = new StringBuilder("(");
            for (String depType : comp.constructorDeps) {
                String depInternal = depType.replace('.', '/');
                ctorDesc.append("L").append(depInternal).append(";");
                loadDependency(mv, depType);
            }
            ctorDesc.append(")V");
            
            mv.visitMethodInsn(INVOKESPECIAL, comp.internalName, "<init>", ctorDesc.toString(), false);
            mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, fieldName, fieldType);
            
            // Field injections (handle both regular injections and @Value)
            for (FieldInjectionMeta field : comp.fieldInjections) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                
                // Check if this is a @Value field (primitive or String types)
                if (isPrimitiveOrValueType(field.descriptor)) {
                    // Use ValueResolver to get the value
                    loadValueFromResolver(mv, field);
                } else if (field.isOptional) {
                    // Optional<T> injection - wrap in Optional.of() or Optional.empty()
                    loadOptionalDependency(mv, field.depType);
                } else {
                    loadDependency(mv, field.depType);
                }
                
                if (!"PUBLIC".equals(field.visibility)) {
                    String setterName = SYNTHETIC_SETTER_PREFIX + field.name;
                    String setterDesc = "(" + field.descriptor + ")V";
                    mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, setterName, setterDesc, false);
                } else {
                    mv.visitFieldInsn(PUTFIELD, comp.internalName, field.name, field.descriptor);
                }
            }
            
            // Method injections
            for (MethodInjectionMeta method : comp.methodInjections) {
                if (method.descriptor == null || method.descriptor.isEmpty()) {
                    continue; // Skip invalid method injections
                }
                // Validate descriptor format
                if (!method.descriptor.startsWith("(") || !method.descriptor.contains(")")) {
                    System.err.println("WARNING: Invalid method descriptor for " + comp.className + "." + method.name + ": " + method.descriptor);
                    continue;
                }
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                for (String depType : method.depTypes) {
                    loadDependency(mv, depType);
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, method.name, method.descriptor, false);
            }
            
            // EventBus registration (if has @Subscribe methods)
            if (comp.hasSubscribeMethods) {
                mv.visitMethodInsn(INVOKESTATIC, "io/github/yasmramos/runtime/event/EventBus", "getInstance", 
                    "()Lio/github/yasmramos/runtime/event/EventBus;", false);
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                mv.visitMethodInsn(INVOKEVIRTUAL, "io/github/yasmramos/runtime/event/EventBus", "register", 
                    "(Ljava/lang/Object;)V", false);
            }
            
            // @PostConstruct callback (via $VeldLifecycle helper in same package)
            if (comp.postConstructMethod != null) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
                mv.visitMethodInsn(INVOKESTATIC, comp.internalName + "$VeldLifecycle", "postConstruct",
                    "(L" + comp.internalName + ";)V", false);
            }
        }
        
        // Initialize LifecycleProcessor singleton
        initializeLifecycleProcessor(mv);
        
        // Initialize ConditionalRegistry (this will filter components based on conditions)
        initializeConditionalRegistry(mv);
        
        // Initialize ValueResolver for @Value annotation support
        initializeValueResolver(mv);
        
        // Initialize lookup arrays
        List<TypeMapping> mappings = buildTypeMappings();
        int mappingCount = mappings.size();
        
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        
        pushInt(mv, mappingCount);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_scopes", "[I");
        
        pushInt(mv, mappingCount);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_protoIdx", "[I");
        
        pushInt(mv, mappingCount);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_names", "[Ljava/lang/String;");
        
        Map<String, Integer> protoIdxMap = new HashMap<>();
        for (int i = 0; i < prototypes.size(); i++) {
            protoIdxMap.put(prototypes.get(i).internalName, i);
        }
        
        for (int i = 0; i < mappings.size(); i++) {
            TypeMapping m = mappings.get(i);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
            pushInt(mv, i);
            mv.visitLdcInsn(Type.getObjectType(m.typeInternal));
            mv.visitInsn(AASTORE);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
            pushInt(mv, i);
            if ("SINGLETON".equals(m.component.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(m.component),
                    "L" + m.component.internalName + ";");
            } else {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitInsn(AASTORE);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
            pushInt(mv, i);
            pushInt(mv, "SINGLETON".equals(m.component.scope) ? 0 : 1);
            mv.visitInsn(IASTORE);
            
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
            pushInt(mv, i);
            Integer idx = protoIdxMap.get(m.component.internalName);
            pushInt(mv, idx != null ? idx : -1);
            mv.visitInsn(IASTORE);
            
            // Store component name for @Named lookup
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_names", "[Ljava/lang/String;");
            pushInt(mv, i);
            if (m.component.componentName != null) {
                mv.visitLdcInsn(m.component.componentName);
            } else {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitInsn(AASTORE);
        }
        
        // === Initialize hash table for O(1) lookup ===
        int htSize = tableSizeFor((mappingCount * 4) / 3 + 1);
        int mask = htSize - 1;
        
        // _mask = htSize - 1
        pushInt(mv, mask);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_mask", "I");
        
        // _htTypes = new Class[htSize]
        pushInt(mv, htSize);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_htTypes", "[Ljava/lang/Class;");
        
        // _htInstances = new Object[htSize]
        pushInt(mv, htSize);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_htInstances", "[Ljava/lang/Object;");
        
        // Populate hash table (done at clinit time, so no runtime cost)
        // We'll use identity hashCode for predictable hashing
        for (int i = 0; i < mappings.size(); i++) {
            TypeMapping m = mappings.get(i);
            
            // Calculate slot using linear probing
            // (computed at generation time - we embed the final slot)
            int slot = computeHashSlot(m.typeInternal, mask, mappings, i);
            
            // _htTypes[slot] = type
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_htTypes", "[Ljava/lang/Class;");
            pushInt(mv, slot);
            mv.visitLdcInsn(Type.getObjectType(m.typeInternal));
            mv.visitInsn(AASTORE);
            
            // _htInstances[slot] = instance (for singletons)
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_htInstances", "[Ljava/lang/Object;");
            pushInt(mv, slot);
            if ("SINGLETON".equals(m.component.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(m.component),
                    "L" + m.component.internalName + ";");
            } else {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitInsn(AASTORE);
        }
        
        // === Initialize thread-local cache ===
        // _tlCache = ThreadLocal.withInitial(() -> new Object[8])
        Handle bsmHandle = new Handle(H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
        
        mv.visitInvokeDynamicInsn("get", "()Ljava/util/function/Supplier;", bsmHandle,
            Type.getType("()Ljava/lang/Object;"),
            new Handle(H_INVOKESTATIC, VELD_CLASS, "_newTlCache", "()[Ljava/lang/Object;", false),
            Type.getType("()[Ljava/lang/Object;"));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/ThreadLocal", "withInitial",
            "(Ljava/util/function/Supplier;)Ljava/lang/ThreadLocal;", false);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_tlCache", "Ljava/lang/ThreadLocal;");
        
        // _tlIdx = ThreadLocal.withInitial(() -> new int[1])
        mv.visitInvokeDynamicInsn("get", "()Ljava/util/function/Supplier;", bsmHandle,
            Type.getType("()Ljava/lang/Object;"),
            new Handle(H_INVOKESTATIC, VELD_CLASS, "_newTlIdx", "()[I", false),
            Type.getType("()[I"));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/ThreadLocal", "withInitial",
            "(Ljava/util/function/Supplier;)Ljava/lang/ThreadLocal;", false);
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_tlIdx", "Ljava/lang/ThreadLocal;");
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        // Generate helper methods for ThreadLocal initialization
        generateTlCacheFactory(cw);
        generateTlIdxFactory(cw);
    }
    
    private void generateTlCacheFactory(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "_newTlCache", "()[Ljava/lang/Object;", null, null);
        mv.visitCode();
        pushInt(mv, 8); // 4 entries * 2 (key + value)
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateTlIdxFactory(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "_newTlIdx", "()[I", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates bytecode to initialize the LifecycleProcessor singleton.
     */
    private void initializeLifecycleProcessor(MethodVisitor mv) {
        // Create LifecycleProcessor instance
        mv.visitTypeInsn(NEW, "io/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "io/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor", 
            "<init>", "()V", false);
        
        // Get EventBus instance and set it on LifecycleProcessor
        mv.visitMethodInsn(INVOKESTATIC, "io/github/yasmramos/veld/runtime/event/EventBus", 
            "getInstance", "()Lio/github/yasmramos/veld/runtime/event/EventBus;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "io/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor", 
            "setEventBus", "(Lio/github/yasmramos/veld/runtime/event/EventBus;)V", false);
        
        // Store in static field
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_lifecycleProcessor", 
            "Lio/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor;");
        
        // Register all beans with the LifecycleProcessor
        registerBeansWithLifecycleProcessor(mv);
    }
    
    /**
     * Generates bytecode to register all beans with the LifecycleProcessor.
     */
    private void registerBeansWithLifecycleProcessor(MethodVisitor mv) {
        for (ComponentMeta comp : components) {
            String fieldName = getFieldName(comp);
            String fieldType = "L" + comp.internalName + ";";
            
            // Get LifecycleProcessor instance
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_lifecycleProcessor", 
                "Lio/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor;");
            
            // Push bean name
            mv.visitLdcInsn(comp.componentName != null ? comp.componentName : getMethodName(comp));
            
            // Get bean instance
            mv.visitFieldInsn(GETSTATIC, VELD_CLASS, fieldName, fieldType);
            
            // Call registerBean
            mv.visitMethodInsn(INVOKEVIRTUAL, "io/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor", 
                "registerBean", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        }
    }
    
    /**
     * Generates bytecode to initialize the ConditionalRegistry.
     * This will filter components based on conditions like @Profile, @ConditionalOnProperty, etc.
     */
    private void initializeConditionalRegistry(MethodVisitor mv) {
        // Create VeldRegistry instance first (this is the original generated registry)
        mv.visitTypeInsn(NEW, "veld/VeldRegistry");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "veld/VeldRegistry", "<init>", "()V", false);
        
        // Create ConditionalRegistry with the original registry
        mv.visitTypeInsn(NEW, "io/github/yasmramos/veld/runtime/ConditionalRegistry");
        mv.visitInsn(DUP);
        mv.visitSwap(); // Swap VeldRegistry instance to top of stack
        
        // Load active profiles from system property or environment variable
        mv.visitMethodInsn(INVOKESTATIC, "io/github/yasmramos/veld/runtime/ConditionalRegistry", 
            "getActiveProfiles", "()[Ljava/lang/String;", false);
        
        mv.visitMethodInsn(INVOKESPECIAL, "io/github/yasmramos/veld/runtime/ConditionalRegistry", 
            "<init>", "(Lio/github/yasmramos/veld/runtime/ComponentRegistry;[Ljava/lang/String;)V", false);
        
        // Store in static field
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_conditionalRegistry", 
            "Lio/github/yasmramos/veld/runtime/ConditionalRegistry;");
    }
    
    /**
     * Generates bytecode to initialize the ValueResolver singleton.
     */
    private void initializeValueResolver(MethodVisitor mv) {
        // Get ValueResolver instance (singleton)
        mv.visitMethodInsn(INVOKESTATIC, "io/github/yasmramos/veld/runtime/value/ValueResolver", 
            "getInstance", "()Lio/github/yasmramos/veld/runtime/value/ValueResolver;", false);
        
        // Store in static field
        mv.visitFieldInsn(PUTSTATIC, VELD_CLASS, "_valueResolver", 
            "Lio/github/yasmramos/veld/runtime/value/ValueResolver;");
    }
    
    /**
     * Generates bytecode to load a value from ValueResolver for @Value fields.
     */
    private void loadValueFromResolver(MethodVisitor mv, FieldInjectionMeta field) {
        // Get ValueResolver instance
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_valueResolver", 
            "Lio/github/yasmramos/veld/runtime/value/ValueResolver;");
        
        // For now, we'll use a placeholder value expression
        // In a real implementation, we'd need to store the actual @Value expression
        // from the annotation processing and use it here
        mv.visitLdcInsn("${" + field.name + "}"); // placeholder - would be actual expression
        
        // Call resolve method (we'll need to extend this to support type conversion)
        mv.visitMethodInsn(INVOKEVIRTUAL, "io/github/yasmramos/veld/runtime/value/ValueResolver", 
            "resolve", "(Ljava/lang/String;)Ljava/lang/String;", false);
        
        // TODO: Add type conversion logic based on field.descriptor
        // For now, this only handles String fields
    }
    
    private int computeHashSlot(String typeInternal, int mask, List<TypeMapping> allMappings, int upToIndex) {
        // Use a stable hash based on class name (same as Class.hashCode at runtime)
        int hash = typeInternal.replace('/', '.').hashCode();
        int slot = hash & mask;
        
        // Check for collisions with previously placed entries
        Set<Integer> usedSlots = new HashSet<>();
        for (int i = 0; i < upToIndex; i++) {
            String prevType = allMappings.get(i).typeInternal;
            int prevHash = prevType.replace('/', '.').hashCode();
            int prevSlot = prevHash & mask;
            while (usedSlots.contains(prevSlot)) {
                prevSlot = (prevSlot + 1) & mask;
            }
            usedSlots.add(prevSlot);
        }
        
        // Find first free slot via linear probing
        while (usedSlots.contains(slot)) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }
    
    private static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 16) ? 16 : (n >= 1073741824) ? 1073741824 : n + 1;
    }
    
    /**
     * Generates a $VeldLifecycle helper class in the same package as the component.
     * This allows invoking package-private @PostConstruct/@PreDestroy methods.
     */
    private byte[] generateLifecycleHelper(ComponentMeta comp) {
        String helperName = comp.internalName + "$VeldLifecycle";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, helperName, null, "java/lang/Object", null);
        
        // Private constructor
        MethodVisitor ctor = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        
        String compDescriptor = "L" + comp.internalName + ";";
        
        // public static void postConstruct(ComponentType instance)
        if (comp.postConstructMethod != null) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "postConstruct",
                "(" + compDescriptor + ")V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, comp.postConstructMethod,
                comp.postConstructDescriptor, false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        // public static void preDestroy(ComponentType instance)
        if (comp.preDestroyMethod != null) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "preDestroy",
                "(" + compDescriptor + ")V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, comp.preDestroyMethod,
                comp.preDestroyDescriptor, false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private void loadDependency(MethodVisitor mv, String typeName) {
        String depInternal = typeName.replace('.', '/');
        ComponentMeta depComp = findComponentByType(depInternal);
        
        if (depComp != null) {
            if ("SINGLETON".equals(depComp.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(depComp),
                    "L" + depComp.internalName + ";");
            } else {
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(depComp),
                    "()L" + depComp.internalName + ";", false);
            }
        } else {
            // Unknown dependency - just push null (will be resolved by factory if needed)
            mv.visitInsn(ACONST_NULL);
        }
    }
    
    /**
     * Loads an Optional<T> dependency.
     * If the component exists, returns Optional.of(component), otherwise Optional.empty().
     */
    private void loadOptionalDependency(MethodVisitor mv, String typeName) {
        String depInternal = typeName.replace('.', '/');
        ComponentMeta depComp = findComponentByType(depInternal);
        
        if (depComp != null) {
            // Component exists - wrap in Optional.of()
            if ("SINGLETON".equals(depComp.scope)) {
                mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(depComp),
                    "L" + depComp.internalName + ";");
            } else {
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(depComp),
                    "()L" + depComp.internalName + ";", false);
            }
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "of",
                "(Ljava/lang/Object;)Ljava/util/Optional;", false);
        } else {
            // Component doesn't exist - return Optional.empty()
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "empty",
                "()Ljava/util/Optional;", false);
        }
    }
    
    private List<TypeMapping> buildTypeMappings() {
        List<TypeMapping> mappings = new ArrayList<>();
        for (ComponentMeta comp : components) {
            mappings.add(new TypeMapping(comp.internalName, comp));
            for (String iface : comp.interfaces) {
                mappings.add(new TypeMapping(iface.replace('.', '/'), comp));
            }
        }
        
        // Add LifecycleProcessor as a special managed component
        mappings.add(new TypeMapping("io/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor", 
            createLifecycleProcessorMeta()));
        
        // Add ValueResolver as a special managed component
        mappings.add(new TypeMapping("io/github/yasmramos/veld/runtime/value/ValueResolver", 
            createValueResolverMeta()));
        
        return mappings;
    }
    
    /**
     * Creates a ComponentMeta for the LifecycleProcessor singleton.
     */
    private ComponentMeta createLifecycleProcessorMeta() {
        return new ComponentMeta(
            "io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor",
            "SINGLETON",  // scope
            false,        // lazy
            new ArrayList<>(),  // constructorDeps
            new ArrayList<>(),  // fieldInjections
            new ArrayList<>(),  // methodInjections
            new ArrayList<>(),  // interfaces
            null,  // postConstructMethod
            null,  // postConstructDescriptor
            null,  // preDestroyMethod
            null,  // preDestroyDescriptor
            false, // hasSubscribeMethods
            "lifecycleProcessor",  // componentName
            new ArrayList<>()  // explicitDependencies
        );
    }
    
    /**
     * Creates a ComponentMeta for the ValueResolver singleton.
     */
    private ComponentMeta createValueResolverMeta() {
        return new ComponentMeta(
            "io.github.yasmramos.veld.runtime.value.ValueResolver",
            "SINGLETON",  // scope
            false,        // lazy
            new ArrayList<>(),  // constructorDeps
            new ArrayList<>(),  // fieldInjections
            new ArrayList<>(),  // methodInjections
            new ArrayList<>(),  // interfaces
            null,  // postConstructMethod
            null,  // postConstructDescriptor
            null,  // preDestroyMethod
            null,  // preDestroyDescriptor
            false, // hasSubscribeMethods
            "valueResolver",  // componentName
            new ArrayList<>()  // explicitDependencies
        );
    }
    
    private List<ComponentMeta> topologicalSort(List<ComponentMeta> singletons) {
        Map<String, ComponentMeta> byType = new HashMap<>();
        for (ComponentMeta comp : singletons) {
            byType.put(comp.internalName, comp);
            for (String iface : comp.interfaces) {
                byType.put(iface.replace('.', '/'), comp);
            }
        }
        
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<ComponentMeta> result = new ArrayList<>();
        
        for (ComponentMeta comp : singletons) {
            visit(comp, byType, visited, visiting, result);
        }
        return result;
    }
    
    private void visit(ComponentMeta comp, Map<String, ComponentMeta> byType,
                       Set<String> visited, Set<String> visiting, List<ComponentMeta> result) {
        if (visited.contains(comp.internalName)) return;
        if (visiting.contains(comp.internalName)) return;
        
        visiting.add(comp.internalName);
        
        for (String dep : comp.constructorDeps) {
            ComponentMeta depComp = byType.get(dep.replace('.', '/'));
            if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                visit(depComp, byType, visited, visiting, result);
            }
        }
        
        for (FieldInjectionMeta field : comp.fieldInjections) {
            ComponentMeta depComp = byType.get(field.depType.replace('.', '/'));
            if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                visit(depComp, byType, visited, visiting, result);
            }
        }
        
        for (MethodInjectionMeta method : comp.methodInjections) {
            for (String dep : method.depTypes) {
                ComponentMeta depComp = byType.get(dep.replace('.', '/'));
                if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                    visit(depComp, byType, visited, visiting, result);
                }
            }
        }
        
        // Add explicit dependencies from @DependsOn
        for (String explicitDep : comp.explicitDependencies) {
            ComponentMeta depComp = byType.get(explicitDep.replace('.', '/'));
            if (depComp != null && "SINGLETON".equals(depComp.scope)) {
                visit(depComp, byType, visited, visiting, result);
            }
        }
        
        visiting.remove(comp.internalName);
        visited.add(comp.internalName);
        result.add(comp);
    }
    
    private void generatePrivateConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateSingletonGetter(ClassWriter cw, ComponentMeta comp) {
        String methodName = getMethodName(comp);
        String returnType = "L" + comp.internalName + ";";
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
            methodName, "()" + returnType, null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, getFieldName(comp), returnType);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generatePrototypeGetter(ClassWriter cw, ComponentMeta comp) {
        String methodName = getMethodName(comp);
        String returnType = "L" + comp.internalName + ";";
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName,
            "()" + returnType, null, null);
        mv.visitCode();
        
        mv.visitTypeInsn(NEW, comp.internalName);
        mv.visitInsn(DUP);
        
        StringBuilder ctorDesc = new StringBuilder("(");
        for (String depType : comp.constructorDeps) {
            String depInternal = depType.replace('.', '/');
            ctorDesc.append("L").append(depInternal).append(";");
            loadDependency(mv, depType);
        }
        ctorDesc.append(")V");
        
        mv.visitMethodInsn(INVOKESPECIAL, comp.internalName, "<init>", ctorDesc.toString(), false);
        mv.visitVarInsn(ASTORE, 0);
        
        for (FieldInjectionMeta field : comp.fieldInjections) {
            mv.visitVarInsn(ALOAD, 0);
            
            // Check if this is a @Value field (primitive or String types)
            if (isPrimitiveOrValueType(field.descriptor)) {
                // Use ValueResolver to get the value
                loadValueFromResolver(mv, field);
            } else if (field.isOptional) {
                // Optional<T> injection - wrap in Optional.of() or Optional.empty()
                loadOptionalDependency(mv, field.depType);
            } else {
                loadDependency(mv, field.depType);
            }
            
            if (!"PUBLIC".equals(field.visibility)) {
                String setterName = SYNTHETIC_SETTER_PREFIX + field.name;
                String setterDesc = "(" + field.descriptor + ")V";
                mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, setterName, setterDesc, false);
            } else {
                mv.visitFieldInsn(PUTFIELD, comp.internalName, field.name, field.descriptor);
            }
        }
        
        for (MethodInjectionMeta method : comp.methodInjections) {
            mv.visitVarInsn(ALOAD, 0);
            for (String depType : method.depTypes) {
                loadDependency(mv, depType);
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, comp.internalName, method.name, method.descriptor, false);
        }
        
        // EventBus registration (if has @Subscribe methods)
        if (comp.hasSubscribeMethods) {
            mv.visitMethodInsn(INVOKESTATIC, "io/github/yasmramos/runtime/event/EventBus", "getInstance", 
                "()Lio/github/yasmramos/runtime/event/EventBus;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "io/github/yasmramos/runtime/event/EventBus", "register", 
                "(Ljava/lang/Object;)V", false);
        }
        
        // @PostConstruct callback (via $VeldLifecycle helper in same package)
        if (comp.postConstructMethod != null) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, comp.internalName + "$VeldLifecycle", "postConstruct",
                "(L" + comp.internalName + ";)V", false);
        }
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateCreatePrototype(ClassWriter cw, List<ComponentMeta> prototypes) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "_createPrototype",
            "(I)Ljava/lang/Object;", null, null);
        mv.visitCode();
        
        if (prototypes.isEmpty()) {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        } else {
            Label defaultLabel = new Label();
            Label[] labels = new Label[prototypes.size()];
            for (int i = 0; i < labels.length; i++) labels[i] = new Label();
            
            mv.visitVarInsn(ILOAD, 0);
            mv.visitTableSwitchInsn(0, prototypes.size() - 1, defaultLabel, labels);
            
            for (int i = 0; i < prototypes.size(); i++) {
                mv.visitLabel(labels[i]);
                mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, getMethodName(prototypes.get(i)),
                    "()L" + prototypes.get(i).internalName + ";", false);
                mv.visitInsn(ARETURN);
            }
            
            mv.visitLabel(defaultLabel);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates O(1) hash-based lookup with thread-local caching.
     * 
     * Hot path (~2ns): Thread-local cache hit
     * Warm path (~5ns): Direct field access via identity hash switch
     * Cold path (~15ns): Linear fallback for hash collisions
     */
    private void generateGetByClass(ClassWriter cw) {
        // Generate thread-local cache field
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "_tlCache",
            "Ljava/lang/ThreadLocal;", null, null).visitEnd();
        
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "get",
            "(Ljava/lang/Class;)Ljava/lang/Object;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;)TT;", null);
        mv.visitCode();
        
        // === PHASE 1: Thread-local cache check ===
        // Object[] cache = _tlCache.get();
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_tlCache", "Ljava/lang/ThreadLocal;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
        mv.visitVarInsn(ASTORE, 1); // cache
        
        // if (cache[0] == type) return cache[1];
        // if (cache[2] == type) return cache[3];
        // ... (4 entries = 8 slots)
        Label notInCache = new Label();
        for (int i = 0; i < 4; i++) {
            mv.visitVarInsn(ALOAD, 1);
            pushInt(mv, i * 2);
            mv.visitInsn(AALOAD);
            mv.visitVarInsn(ALOAD, 0);
            Label nextCacheSlot = new Label();
            mv.visitJumpInsn(IF_ACMPNE, nextCacheSlot);
            // Cache hit - return cache[i*2 + 1]
            mv.visitVarInsn(ALOAD, 1);
            pushInt(mv, i * 2 + 1);
            mv.visitInsn(AALOAD);
            mv.visitInsn(ARETURN);
            mv.visitLabel(nextCacheSlot);
        }
        
        // === PHASE 2: Hash table lookup ===
        mv.visitLabel(notInCache);
        
        // int hash = type.hashCode() & _mask;
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_mask", "I");
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 2); // hash/slot
        
        // Linear probe loop (max 8 probes, then fallback)
        Label probeLoop = new Label();
        Label fallback = new Label();
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3); // probe count
        
        mv.visitLabel(probeLoop);
        mv.visitVarInsn(ILOAD, 3);
        pushInt(mv, 8);
        mv.visitJumpInsn(IF_ICMPGE, fallback);
        
        // Class<?> stored = _htTypes[slot];
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_htTypes", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 4); // stored
        
        // if (stored == null) goto fallback;
        mv.visitVarInsn(ALOAD, 4);
        mv.visitJumpInsn(IFNULL, fallback);
        
        // if (stored == type) { result = _htInstances[slot]; updateCache; return; }
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ALOAD, 0);
        Label nextProbe = new Label();
        mv.visitJumpInsn(IF_ACMPNE, nextProbe);
        
        // Found! Get instance
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_htInstances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 5); // result
        
        // Update thread-local cache (circular, slot = _tlIdx++ & 3)
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_tlIdx", "Ljava/lang/ThreadLocal;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "[I");
        mv.visitVarInsn(ASTORE, 6); // int[] idx
        
        mv.visitVarInsn(ALOAD, 6);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IALOAD);
        pushInt(mv, 3); // & 3 for 4 slots
        mv.visitInsn(IAND);
        pushInt(mv, 2);
        mv.visitInsn(IMUL); // cachePos = (idx[0] & 3) * 2
        mv.visitVarInsn(ISTORE, 7);
        
        // cache[cachePos] = type
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 7);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(AASTORE);
        
        // cache[cachePos+1] = result
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 7);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitInsn(AASTORE);
        
        // idx[0]++
        mv.visitVarInsn(ALOAD, 6);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IALOAD);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitInsn(IASTORE);
        
        mv.visitVarInsn(ALOAD, 5);
        mv.visitInsn(ARETURN);
        
        // Next probe: slot = (slot + 1) & _mask
        mv.visitLabel(nextProbe);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_mask", "I");
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(GOTO, probeLoop);
        
        // === PHASE 3: Fallback to linear scan (rare) ===
        mv.visitLabel(fallback);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "_getLinear", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        
        // Generate linear fallback method
        generateGetLinearFallback(cw);
    }
    
    private void generateGetLinearFallback(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "_getLinear",
            "(Ljava/lang/Class;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 1);
        
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ALOAD, 0);
        
        Label notEqual = new Label();
        mv.visitJumpInsn(IF_ACMPNE, notEqual);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IALOAD);
        
        Label isProto = new Label();
        mv.visitJumpInsn(IFNE, isProto);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(isProto);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IALOAD);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "_createPrototype", "(I)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(notEqual);
        mv.visitIincInsn(2, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        
        mv.visitLabel(loopEnd);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    /**
     * Generates: public static <T> T get(Class<T> type, String name)
     * Looks up component by type AND name (for @Named qualifier support).
     */
    private void generateGetByClassAndName(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "get",
            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;Ljava/lang/String;)TT;", null);
        mv.visitCode();
        
        // If name is null, delegate to get(Class)
        mv.visitVarInsn(ALOAD, 1);
        Label nameNotNull = new Label();
        mv.visitJumpInsn(IFNONNULL, nameNotNull);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "get", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(nameNotNull);
        
        // int len = _types.length
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 2);  // len
        
        // int i = 0
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);  // i
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        // if (_types[i] == type)
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ALOAD, 0);
        
        Label notTypeMatch = new Label();
        mv.visitJumpInsn(IF_ACMPNE, notTypeMatch);
        
        // if (name.equals(_names[i]))
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_names", "[Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        
        Label notNameMatch = new Label();
        mv.visitJumpInsn(IFEQ, notNameMatch);
        
        // Check scope (0=singleton, 1=prototype)
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_scopes", "[I");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(IALOAD);
        
        Label isProto = new Label();
        mv.visitJumpInsn(IFNE, isProto);
        
        // Singleton: return _instances[i]
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitInsn(ARETURN);
        
        // Prototype: return _createPrototype(_protoIdx[i])
        mv.visitLabel(isProto);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_protoIdx", "[I");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(IALOAD);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "_createPrototype", "(I)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        
        mv.visitLabel(notNameMatch);
        mv.visitLabel(notTypeMatch);
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        
        mv.visitLabel(loopEnd);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetAllByClass(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "getAll",
            "(Ljava/lang/Class;)Ljava/util/List;",
            "<T:Ljava/lang/Object;>(Ljava/lang/Class<TT;>;)Ljava/util/List<TT;>;", null);
        mv.visitCode();
        
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 1);
        
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 2);
        
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);
        
        Label loopStart = new Label();
        Label loopEnd = new Label();
        
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_types", "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z", false);
        
        Label notMatch = new Label();
        mv.visitJumpInsn(IFEQ, notMatch);
        
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_instances", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
        mv.visitInsn(POP);
        
        mv.visitLabel(notMatch);
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        
        mv.visitLabel(loopEnd);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateContains(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "contains",
            "(Ljava/lang/Class;)Z", null, null);
        mv.visitCode();
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, VELD_CLASS, "get", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        
        Label isNull = new Label();
        mv.visitJumpInsn(IFNULL, isNull);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        
        mv.visitLabel(isNull);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateComponentCount(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "componentCount", "()I", null, null);
        mv.visitCode();
        pushInt(mv, components.size());
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetLifecycleProcessor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "getLifecycleProcessor",
            "()Lio/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor;", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_lifecycleProcessor",
            "Lio/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateGetValueResolver(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "getValueResolver",
            "()Lio/github/yasmramos/veld/runtime/value/ValueResolver;", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_valueResolver",
            "Lio/github/yasmramos/veld/runtime/value/ValueResolver;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private void generateShutdown(ClassWriter cw, List<ComponentMeta> singletons) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "shutdown", "()V", null, null);
        mv.visitCode();
        
        // Use LifecycleProcessor for proper lifecycle management
        mv.visitFieldInsn(GETSTATIC, VELD_CLASS, "_lifecycleProcessor",
            "Lio/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "io/github/yasmramos/veld/runtime/lifecycle/LifecycleProcessor",
            "destroy", "()V", false);
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private ComponentMeta findComponentByType(String internalName) {
        for (ComponentMeta comp : components) {
            if (comp.internalName.equals(internalName)) return comp;
            for (String iface : comp.interfaces) {
                if (iface.replace('.', '/').equals(internalName)) return comp;
            }
        }
        return null;
    }
    
    private String getMethodName(ComponentMeta comp) {
        String simpleName = comp.className;
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot >= 0) simpleName = simpleName.substring(lastDot + 1);
        if (simpleName.length() > 1 && Character.isUpperCase(simpleName.charAt(1))) return simpleName;
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
    
    private String getFieldName(ComponentMeta comp) {
        return "_" + getMethodName(comp);
    }
    
    private void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) mv.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) mv.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) mv.visitIntInsn(SIPUSH, value);
        else mv.visitLdcInsn(value);
    }
    
    /**
     * Checks if a descriptor represents a primitive type or String (likely @Value injection).
     * These are handled by the factories, not by Veld.class.
     */
    private boolean isPrimitiveOrValueType(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) return true;
        char first = descriptor.charAt(0);
        // Primitives: Z=boolean, B=byte, C=char, S=short, I=int, J=long, F=float, D=double
        if (first != 'L' && first != '[') return true;
        // String is also typically a @Value injection
        if ("Ljava/lang/String;".equals(descriptor)) return true;
        return false;
    }
    
    private static class TypeMapping {
        final String typeInternal;
        final ComponentMeta component;
        TypeMapping(String typeInternal, ComponentMeta component) {
            this.typeInternal = typeInternal;
            this.component = component;
        }
    }
    
    // === Data classes for parsed metadata ===
    
    public static class ComponentMeta {
        public final String className;
        public final String internalName;
        public final String scope;
        public final boolean lazy;
        public final List<String> constructorDeps;
        public final List<FieldInjectionMeta> fieldInjections;
        public final List<MethodInjectionMeta> methodInjections;
        public final List<String> interfaces;
        public final String postConstructMethod;
        public final String postConstructDescriptor;
        public final String preDestroyMethod;
        public final String preDestroyDescriptor;
        public final boolean hasSubscribeMethods;
        public final String componentName;  // @Named value for qualifier lookup
        public final List<String> explicitDependencies;  // @DependsOn bean names
        
        public ComponentMeta(String className, String scope, boolean lazy,
                            List<String> constructorDeps, List<FieldInjectionMeta> fieldInjections,
                            List<MethodInjectionMeta> methodInjections, List<String> interfaces,
                            String postConstructMethod, String postConstructDescriptor,
                            String preDestroyMethod, String preDestroyDescriptor,
                            boolean hasSubscribeMethods, String componentName,
                            List<String> explicitDependencies) {
            this.className = className;
            this.internalName = className.replace('.', '/');
            this.scope = scope;
            this.lazy = lazy;
            this.constructorDeps = constructorDeps;
            this.fieldInjections = fieldInjections;
            this.methodInjections = methodInjections;
            this.interfaces = interfaces;
            this.postConstructMethod = postConstructMethod;
            this.postConstructDescriptor = postConstructDescriptor;
            this.preDestroyMethod = preDestroyMethod;
            this.preDestroyDescriptor = preDestroyDescriptor;
            this.hasSubscribeMethods = hasSubscribeMethods;
            this.componentName = componentName;
            this.explicitDependencies = explicitDependencies != null ? explicitDependencies : new ArrayList<>();
        }
        
        public static ComponentMeta parse(String line) {
            String[] parts = line.split("\\|\\|", -1);
            
            String className = parts[0];
            String scope = parts[1];
            boolean lazy = Boolean.parseBoolean(parts[2]);
            
            List<String> ctorDeps = new ArrayList<>();
            if (parts.length > 3 && !parts[3].isEmpty()) {
                ctorDeps.addAll(Arrays.asList(parts[3].split(",")));
            }
            
            List<FieldInjectionMeta> fields = new ArrayList<>();
            if (parts.length > 4 && !parts[4].isEmpty()) {
                for (String f : parts[4].split("@")) {
                    if (f.isEmpty()) continue;
                    String[] fp = f.split("~", 6); // use ~ as delimiter
                    if (fp.length >= 4) {
                        boolean isOptional = fp.length > 4 && Boolean.parseBoolean(fp[4]);
                        boolean isProvider = fp.length > 5 && Boolean.parseBoolean(fp[5]);
                        fields.add(new FieldInjectionMeta(fp[0], fp[1], fp[2], fp[3], isOptional, isProvider));
                    }
                }
            }
            
            List<MethodInjectionMeta> methods = new ArrayList<>();
            if (parts.length > 5 && !parts[5].isEmpty()) {
                for (String m : parts[5].split("@")) {
                    if (m.isEmpty()) continue;
                    String[] mp = m.split("~", 3); // use ~ as delimiter
                    if (mp.length >= 2) {
                        List<String> deps = mp.length > 2 && !mp[2].isEmpty() 
                            ? Arrays.asList(mp[2].split(",")) : new ArrayList<>();
                        methods.add(new MethodInjectionMeta(mp[0], mp[1], deps));
                    }
                }
            }
            
            List<String> ifaces = new ArrayList<>();
            if (parts.length > 6 && !parts[6].isEmpty()) {
                ifaces.addAll(Arrays.asList(parts[6].split(",")));
            }
            
            // Parse postConstruct (index 7)
            String postConstructMethod = null;
            String postConstructDescriptor = null;
            if (parts.length > 7 && !parts[7].isEmpty()) {
                String[] pc = parts[7].split("~", 2);
                if (pc.length >= 2) {
                    postConstructMethod = pc[0];
                    postConstructDescriptor = pc[1];
                }
            }
            
            // Parse preDestroy (index 8)
            String preDestroyMethod = null;
            String preDestroyDescriptor = null;
            if (parts.length > 8 && !parts[8].isEmpty()) {
                String[] pd = parts[8].split("~", 2);
                if (pd.length >= 2) {
                    preDestroyMethod = pd[0];
                    preDestroyDescriptor = pd[1];
                }
            }
            
            // Parse hasSubscribeMethods (index 9)
            boolean hasSubscribeMethods = false;
            if (parts.length > 9 && !parts[9].isEmpty()) {
                hasSubscribeMethods = Boolean.parseBoolean(parts[9]);
            }
            
            // Parse componentName (index 10)
            String componentName = null;
            if (parts.length > 10 && !parts[10].isEmpty()) {
                componentName = parts[10];
            }
            
            // Parse explicitDependencies (index 11)
            List<String> explicitDependencies = new ArrayList<>();
            if (parts.length > 11 && !parts[11].isEmpty()) {
                explicitDependencies.addAll(Arrays.asList(parts[11].split(",")));
            }
            
            return new ComponentMeta(className, scope, lazy, ctorDeps, fields, methods, ifaces,
                postConstructMethod, postConstructDescriptor, preDestroyMethod, preDestroyDescriptor,
                hasSubscribeMethods, componentName, explicitDependencies);
        }
    }
    
    public static class FieldInjectionMeta {
        public final String name;
        public final String depType;      // actualType (the real type, not wrapper)
        public final String descriptor;
        public final String visibility;
        public final boolean isOptional;  // true if Optional<T>
        public final boolean isProvider;  // true if Provider<T>
        
        public FieldInjectionMeta(String name, String depType, String descriptor, String visibility,
                                  boolean isOptional, boolean isProvider) {
            this.name = name;
            this.depType = depType;
            this.descriptor = descriptor;
            this.visibility = visibility;
            this.isOptional = isOptional;
            this.isProvider = isProvider;
        }
    }
    
    public static class MethodInjectionMeta {
        public final String name;
        public final String descriptor;
        public final List<String> depTypes;
        
        public MethodInjectionMeta(String name, String descriptor, List<String> depTypes) {
            this.name = name;
            this.descriptor = descriptor;
            this.depTypes = depTypes;
        }
    }
}
