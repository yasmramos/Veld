package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.yasmramos.veld.annotation.ScopeType;
import io.github.yasmramos.veld.runtime.ComponentFactory;
import io.github.yasmramos.veld.runtime.ComponentRegistry;

import java.lang.SuppressWarnings;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates VeldRegistry.java source code for component registration.
 * 
 * <p>Supports two modes:</p>
 * 
 * <h2>Pure Mode</h2>
 * <ul>
 *   <li>No ComponentFactory interface - factories are plain classes</li>
 *   <li>No ComponentRegistry interface - registry is a static holder</li>
 *   <li>All methods use Object type internally</li>
 *   <li>Type-safe access via generated static methods on Veld class</li>
 * </ul>
 * 
 * <h2>Advanced Mode</h2>
 * <ul>
 *   <li>Factories implement ComponentFactory interface from veld-runtime</li>
 *   <li>Registry delegates to veld-runtime ComponentRegistry</li>
 *   <li>Full lifecycle management through LifecycleProcessor</li>
 *   <li>Event bus integration for @Subscribe methods</li>
 * </ul>
 */
public final class RegistrySourceGenerator {

    /**
     * Mode of generation - determines what features are included.
     */
    public enum RegistryMode {
        PURE,    // No runtime dependencies, pure compile-time DI
        ADVANCED // Full features, delegates to runtime container
    }

    private final List<ComponentInfo> components;
    private final Map<String, List<Integer>> supertypeIndices = new HashMap<>();
    private final RegistryMode mode;

    public RegistrySourceGenerator(List<ComponentInfo> components) {
        this(components, RegistryMode.PURE);
    }

    public RegistrySourceGenerator(List<ComponentInfo> components, RegistryMode mode) {
        this.components = components;
        this.mode = mode;
        buildSupertypeIndices();
    }

    private void buildSupertypeIndices() {
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            addSupertypeIndex(comp.getClassName(), i);
            for (String iface : comp.getImplementedInterfaces()) {
                addSupertypeIndex(iface, i);
            }
        }
    }

    private void addSupertypeIndex(String type, int index) {
        supertypeIndices.computeIfAbsent(type, k -> new ArrayList<>()).add(index);
    }

    public String getClassName() {
        return "io.github.yasmramos.veld.VeldRegistry";
    }

    public JavaFile generate() {
        // Build the class using JavaPoet
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("VeldRegistry")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(
                        "Generated component registry for Veld DI container.\n" +
                        "This class is auto-generated - do not modify.\n" +
                        "Mode: " + mode + "\n")
                .addAnnotation(createSuppressWarningsAnnotation());

        // In ADVANCED mode, implement ComponentRegistry interface
        if (mode == RegistryMode.ADVANCED) {
            classBuilder.addSuperinterface(ClassName.get(ComponentRegistry.class));
        }

        // Static fields
        addStaticFields(classBuilder);

        // Instance fields (for singleton cache only)
        addInstanceFields(classBuilder);

        // Static initializer
        addStaticInitializer(classBuilder);

        // Constructor
        addConstructor(classBuilder);

        // Methods - add @Override annotation in ADVANCED mode
        boolean isAdvanced = (mode == RegistryMode.ADVANCED);
        addGetComponentCount(classBuilder, isAdvanced);
        addGetSingleton(classBuilder, isAdvanced);
        addGetFactory(classBuilder, isAdvanced);
        addGetFactoryByName(classBuilder, isAdvanced);
        addGetAllFactories(classBuilder, isAdvanced);
        addInvokeCreateMethod(classBuilder);
        addCreateFromFactoryClassMethod(classBuilder);

        // Build JavaFile
        JavaFile.Builder javaFileBuilder = JavaFile.builder("io.github.yasmramos.veld", classBuilder.build());

        return javaFileBuilder.build();
    }

    private void addStaticFields(TypeSpec.Builder classBuilder) {
        // Public singleton instance for factory access
        FieldSpec instanceField = FieldSpec.builder(
                ClassName.bestGuess("io.github.yasmramos.veld.VeldRegistry"), "INSTANCE")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T()", ClassName.bestGuess("io.github.yasmramos.veld.VeldRegistry"))
                .build();
        classBuilder.addField(instanceField);

        // TYPE_INDICES - maps Class to component index (uses Object internally)
        FieldSpec typeIndicesField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(IdentityHashMap.class),
                ClassName.get(Class.class),
                ClassName.get(Integer.class)), "TYPE_INDICES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", IdentityHashMap.class)
                .build();
        classBuilder.addField(typeIndicesField);

        // NAME_INDICES - maps bean name to component index
        FieldSpec nameIndicesField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(HashMap.class),
                ClassName.get(String.class),
                ClassName.get(Integer.class)), "NAME_INDICES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();
        classBuilder.addField(nameIndicesField);

        // SCOPES - stores scope for each component
        FieldSpec scopesField = FieldSpec.builder(String[].class, "SCOPES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
        classBuilder.addField(scopesField);

        // LAZY_FLAGS - stores lazy flag for each component
        FieldSpec lazyFlagsField = FieldSpec.builder(boolean[].class, "LAZY_FLAGS")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
        classBuilder.addField(lazyFlagsField);

        // FACTORY_CLASSES - stores the actual factory class for each component
        // Create Class<?>[] using a parameterized type and getting its array representation
        java.lang.reflect.Type factoryArrayType = new java.lang.reflect.ParameterizedType() {
            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return new java.lang.reflect.Type[] { new java.lang.reflect.WildcardType() {
                    @Override
                    public java.lang.reflect.Type[] getUpperBounds() { return new java.lang.reflect.Type[] { Object.class }; }
                    @Override
                    public java.lang.reflect.Type[] getLowerBounds() { return new java.lang.reflect.Type[0]; }
                } };
            }
            @Override
            public java.lang.reflect.Type getRawType() {
                return Class.class;
            }
            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }
        };
        java.lang.reflect.Type wildcardClassArray = new java.lang.reflect.GenericArrayType() {
            @Override
            public java.lang.reflect.Type getGenericComponentType() {
                return factoryArrayType;
            }
        };
        FieldSpec factoryClassesField = FieldSpec.builder(
                ArrayTypeName.get(wildcardClassArray), "FACTORY_CLASSES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .build();
        classBuilder.addField(factoryClassesField);
    }

    private void addInstanceFields(TypeSpec.Builder classBuilder) {
        // Factory instances array - stores factory instances (Object type, no interface)
        FieldSpec factoriesField = FieldSpec.builder(ArrayTypeName.of(ClassName.get(Object.class)), "factories")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T[$L]", Object.class, components.size())
                .build();
        classBuilder.addField(factoriesField);

        // Singleton cache - stores instantiated singletons (Object type)
        FieldSpec singletonCacheField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(IdentityHashMap.class),
                ClassName.get(Class.class),
                ClassName.get(Object.class)), "singletonCache")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", IdentityHashMap.class)
                .build();
        classBuilder.addField(singletonCacheField);
    }

    private void addStaticInitializer(TypeSpec.Builder classBuilder) {
        StringBuilder typeIndicesInit = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            typeIndicesInit.append("        TYPE_INDICES.put(").append(toSourceName(comp.getClassName())).append(".class, ").append(i).append(");\n");
            for (String iface : comp.getImplementedInterfaces()) {
                typeIndicesInit.append("        TYPE_INDICES.put(").append(toSourceName(iface)).append(".class, ").append(i).append(");\n");
            }
        }

        StringBuilder nameIndicesInit = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            nameIndicesInit.append("        NAME_INDICES.put(\"").append(comp.getComponentName()).append("\", ").append(i).append(");\n");
        }

        StringBuilder scopesInit = new StringBuilder();
        scopesInit.append("        SCOPES = new String[] {\n");
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            String scopeId = comp.getScopeId();
            scopesInit.append("            \"").append(scopeId).append("\"");
            if (i < components.size() - 1) scopesInit.append(",");
            scopesInit.append("\n");
        }
        scopesInit.append("        };\n");

        StringBuilder lazyFlagsInit = new StringBuilder();
        lazyFlagsInit.append("        LAZY_FLAGS = new boolean[] {");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) lazyFlagsInit.append(", ");
            lazyFlagsInit.append(components.get(i).isLazy());
        }
        lazyFlagsInit.append("};\n");

        StringBuilder factoryClassesInit = new StringBuilder();
        factoryClassesInit.append("        FACTORY_CLASSES = new Class<?>[] {\n");
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            String factoryClassName = comp.getFactoryClassName();
            factoryClassesInit.append("            ").append(escapeForCodeBlock(toSourceName(factoryClassName))).append(".class");
            if (i < components.size() - 1) factoryClassesInit.append(",");
            factoryClassesInit.append("\n");
        }
        factoryClassesInit.append("        };\n");

        // Build the static initializer
        CodeBlock staticInitCode = CodeBlock.builder()
                .add(typeIndicesInit.toString())
                .add("\n")
                .add(nameIndicesInit.toString())
                .add("\n")
                .add(scopesInit.toString())
                .add("\n")
                .add(lazyFlagsInit.toString())
                .add("\n")
                .add(factoryClassesInit.toString())
                .build();

        classBuilder.addStaticBlock(staticInitCode);
    }

    private void addConstructor(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addCode("try {\n");
        
        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);
            // Instantiate the factory using its class directly (no ComponentFactory interface)
            constructorBuilder.addStatement("factories[$L] = FACTORY_CLASSES[$L].getDeclaredConstructor().newInstance()", i, i);
        }
        
        constructorBuilder.addCode("} catch (Exception e) {\n");
        constructorBuilder.addStatement("throw new RuntimeException($S, e)", "Failed to instantiate factories");
        constructorBuilder.addCode("}\n");
        
        classBuilder.addMethod(constructorBuilder.build());
    }

    private void addGetComponentCount(TypeSpec.Builder classBuilder, boolean isAdvanced) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getComponentCount")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return factories.length");
        if (isAdvanced) {
            methodBuilder.addAnnotation(AnnotationSpec.builder(Override.class).build());
        }
        classBuilder.addMethod(methodBuilder.build());
    }

    private void addGetSingleton(TypeSpec.Builder classBuilder, boolean isAdvanced) {
        MethodSpec.Builder getSingletonBuilder = MethodSpec.methodBuilder("getSingleton")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("T"))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type");
        if (isAdvanced) {
            getSingletonBuilder.addAnnotation(AnnotationSpec.builder(Override.class).build());
        }

        // Get factory index
        getSingletonBuilder.addStatement("$T indexObj = TYPE_INDICES.get(type)", Object.class)
                .beginControlFlow("if (indexObj == null)")
                .addStatement("return null")
                .endControlFlow();

        getSingletonBuilder.addStatement("int index = (Integer) indexObj");

        // Check scope and create singleton or prototype
        getSingletonBuilder.beginControlFlow("if (\"singleton\".equals(SCOPES[index]))")
                .beginControlFlow("synchronized (singletonCache)")
                .addStatement("$T cached = singletonCache.get(type)", Object.class)
                .beginControlFlow("if (cached != null)")
                .addStatement("return ($T) cached", TypeVariableName.get("T"))
                .endControlFlow()
                .addStatement("$T factory = factories[index]", Object.class)
                .addStatement("$T newInstance = invokeCreateMethod(factory)", Object.class)
                .addStatement("singletonCache.put(type, newInstance)")
                .addStatement("return ($T) newInstance", TypeVariableName.get("T"))
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("$T factory = factories[index]", Object.class)
                .addStatement("return ($T) invokeCreateMethod(factory)", TypeVariableName.get("T"))
                .endControlFlow();

        classBuilder.addMethod(getSingletonBuilder.build());
    }

    private void addGetFactory(TypeSpec.Builder classBuilder, boolean isAdvanced) {
        // In ADVANCED mode, return ComponentFactory<T>; in PURE mode, return Class<?>
        TypeName returnType = isAdvanced 
            ? ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("T"))
            : ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("?"));
            
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getFactory")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type");

        if (isAdvanced) {
            // ADVANCED mode: instantiate factory and cast to ComponentFactory
            methodBuilder.addAnnotation(AnnotationSpec.builder(Override.class).build())
                    .addStatement("$T indexObj = TYPE_INDICES.get(type)", Object.class)
                    .beginControlFlow("if (indexObj == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("int index = (Integer) indexObj")
                    .addStatement("return ($T) factories[index]", ComponentFactory.class);
        } else {
            // PURE mode: return the factory class
            methodBuilder.addStatement("$T indexObj = TYPE_INDICES.get(type)", Object.class)
                    .beginControlFlow("if (indexObj == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("int index = (Integer) indexObj")
                    .addStatement("return FACTORY_CLASSES[index]");
        }
        
        classBuilder.addMethod(methodBuilder.build());
    }

    private void addGetFactoryByName(TypeSpec.Builder classBuilder, boolean isAdvanced) {
        // In ADVANCED mode, return ComponentFactory<?>; in PURE mode, return Class<?>
        TypeName returnType = isAdvanced 
            ? ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("?"))
            : ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("?"));
            
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getFactory")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(ClassName.get(String.class), "name");

        if (isAdvanced) {
            // ADVANCED mode: instantiate factory and cast to ComponentFactory
            methodBuilder.addAnnotation(AnnotationSpec.builder(Override.class).build())
                    .addStatement("$T indexObj = NAME_INDICES.get(name)", Object.class)
                    .beginControlFlow("if (indexObj == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("int index = (Integer) indexObj")
                    .addStatement("return ($T<?>) factories[index]", ComponentFactory.class);
        } else {
            // PURE mode: return the factory class
            methodBuilder.addStatement("$T indexObj = NAME_INDICES.get(name)", Object.class)
                    .beginControlFlow("if (indexObj == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("int index = (Integer) indexObj")
                    .addStatement("return FACTORY_CLASSES[index]");
        }
        
        classBuilder.addMethod(methodBuilder.build());
    }

    private void addGetAllFactories(TypeSpec.Builder classBuilder, boolean isAdvanced) {
        // In ADVANCED mode, return List<ComponentFactory<?>>; in PURE mode, return List<Class<?>>
        TypeName factoryType = isAdvanced 
            ? ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("?"))
            : ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("?"));
            
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(List.class), factoryType);
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getAllFactories")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);

        if (isAdvanced) {
            methodBuilder.addAnnotation(AnnotationSpec.builder(Override.class).build());
            // ADVANCED mode: collect ComponentFactory instances
            methodBuilder.addStatement("$T<$T<?>> result = new $T<>(FACTORY_CLASSES.length)", List.class, ComponentFactory.class, ArrayList.class)
                    .addStatement("for (Object factory : factories) { result.add(($T<?>) factory); }", ComponentFactory.class)
                    .addStatement("return result");
        } else {
            // PURE mode: return factory classes
            methodBuilder.addStatement("$T<Class<?>> result = new $T<>(FACTORY_CLASSES.length)", List.class, ArrayList.class)
                    .addStatement("for ($T factoryClass : FACTORY_CLASSES) { result.add(factoryClass); }", Class.class)
                    .addStatement("return result");
        }
        
        classBuilder.addMethod(methodBuilder.build());
        
        // Add getFactoriesForType method in ADVANCED mode (required by ComponentRegistry)
        if (isAdvanced) {
            addGetFactoriesForType(classBuilder);
        }
    }

    /**
     * Adds the getFactoriesForType method required by ComponentRegistry interface.
     * Returns all factories that can produce the given type or its subtypes.
     */
    private void addGetFactoriesForType(TypeSpec.Builder classBuilder) {
        MethodSpec getFactoriesForType = MethodSpec.methodBuilder("getFactoriesForType")
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class),
                        ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("? extends T"))))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type")
                .addStatement("$T<$T<? extends T>> result = new $T<>()", List.class, ComponentFactory.class, ArrayList.class)
                .addStatement("for (Object factory : factories) {")
                .beginControlFlow("if (factory != null)")
                .addStatement("$T factoryType = factory.getClass()", Object.class)
                .addStatement("result.add(($T) factory)", ComponentFactory.class)
                .endControlFlow()
                .addStatement("}")
                .addStatement("return result")
                .build();
        classBuilder.addMethod(getFactoriesForType);
    }

    /**
     * Invokes the create() method on a factory instance using reflection.
     * This is the only reflection in Pure Mode, used internally by the registry.
     * Made public so generated factories can use it for Optional/Provider dependencies.
     */
    private void addInvokeCreateMethod(TypeSpec.Builder classBuilder) {
        MethodSpec invokeCreate = MethodSpec.methodBuilder("invokeCreateMethod")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Object.class)
                .addParameter(ClassName.get(Object.class), "factory")
                .addCode("try {\n")
                .addStatement("return factory.getClass().getMethod($S).invoke(factory)", "create")
                .addCode("} catch (Exception e) {\n")
                .addStatement("throw new RuntimeException($S, e)", "Failed to create component")
                .addCode("}\n")
                .build();
        classBuilder.addMethod(invokeCreate);
    }
    
    /**
     * Creates a component from its factory class, handling all reflection exceptions.
     * This is a convenience method for generated factories that need to create
     * Optional or Provider dependencies at runtime.
     */
    private void addCreateFromFactoryClassMethod(TypeSpec.Builder classBuilder) {
        MethodSpec createFromFactoryClass = MethodSpec.methodBuilder("createFromFactoryClass")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeVariableName.get("T"))
                .addParameter(ClassName.get(Class.class), "factoryClass")
                .addCode("try {\n")
                .addStatement("Object factory = factoryClass.getDeclaredConstructor().newInstance()")
                .addStatement("return ($T) factory.getClass().getMethod($S).invoke(factory)", TypeVariableName.get("T"), "create")
                .addCode("} catch (Exception e) {\n")
                .addStatement("throw new RuntimeException($S, e)", "Failed to create component from factory")
                .addCode("}\n")
                .build();
        classBuilder.addMethod(createFromFactoryClass);
    }

    private AnnotationSpec createSuppressWarningsAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .addMember("value", "$S", "rawtypes")
                .build();
    }

    /**
     * Escapes special JavaPoet characters in source code.
     * In JavaPoet format strings, $ is a special character for code templates.
     */
    private String escapeForCodeBlock(String code) {
        return code.replace("$", "$$");
    }

    /**
     * Converts a binary class name to a source code compatible name.
     * Inner classes should use '$' not '.' to separate from outer class.
     */
    private String toSourceName(String binaryName) {
        // Don't replace '$' with '.' - inner classes like UserService$VeldFactory
        // should remain as UserService$VeldFactory in generated code
        return binaryName;
    }
}
