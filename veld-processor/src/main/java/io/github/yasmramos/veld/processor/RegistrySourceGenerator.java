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
 * Generates VeldRegistry.java source code instead of bytecode.
 */
public final class RegistrySourceGenerator {

    private final List<ComponentInfo> components;
    private final List<FactoryInfo> factories;
    private final Map<String, List<Integer>> supertypeIndices = new HashMap<>();

    public RegistrySourceGenerator(List<ComponentInfo> components) {
        this(components, new ArrayList<>());
    }

    public RegistrySourceGenerator(List<ComponentInfo> components, List<FactoryInfo> factories) {
        this.components = components;
        this.factories = factories;
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
                .addSuperinterface(ClassName.get(ComponentRegistry.class))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(
                        "Generated component registry for Veld DI container.\n" +
                        "This class is auto-generated - do not modify.\n")
                .addAnnotation(createSuppressWarningsAnnotation());

        // Static fields
        addStaticFields(classBuilder);

        // Instance fields
        addInstanceFields(classBuilder);

        // Static initializer
        addStaticInitializer(classBuilder);

        // Constructor
        addConstructor(classBuilder);

        // Methods that need explicit implementation (abstract methods from interface)
        addGetAllFactories(classBuilder);
        addGetFactoryByType(classBuilder);
        addGetFactoryByName(classBuilder);
        addGetFactoriesForType(classBuilder);
        addGetSingleton(classBuilder);

        // Build JavaFile
        JavaFile.Builder javaFileBuilder = JavaFile.builder("io.github.yasmramos.veld", classBuilder.build());

        return javaFileBuilder.build();
    }

    private void addStaticFields(TypeSpec.Builder classBuilder) {
        // TYPE_INDICES
        FieldSpec typeIndicesField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(IdentityHashMap.class),
                ClassName.get(Class.class),
                ClassName.get(Integer.class)), "TYPE_INDICES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", IdentityHashMap.class)
                .build();
        classBuilder.addField(typeIndicesField);

        // NAME_INDICES
        FieldSpec nameIndicesField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(HashMap.class),
                ClassName.get(String.class),
                ClassName.get(Integer.class)), "NAME_INDICES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();
        classBuilder.addField(nameIndicesField);

        // SCOPES
        FieldSpec scopesField = FieldSpec.builder(String[].class, "SCOPES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
        classBuilder.addField(scopesField);

        // LAZY_FLAGS
        FieldSpec lazyFlagsField = FieldSpec.builder(boolean[].class, "LAZY_FLAGS")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
        classBuilder.addField(lazyFlagsField);

        // SUPERTYPE_INDICES
        FieldSpec supertypeIndicesField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(HashMap.class), ClassName.get(Class.class), TypeName.get(int[].class)), "SUPERTYPE_INDICES")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();
        classBuilder.addField(supertypeIndicesField);
    }

    private void addInstanceFields(TypeSpec.Builder classBuilder) {
        // factories array
        // factories array
        FieldSpec factoriesField = FieldSpec.builder(ArrayTypeName.of(
                ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("?"))), "factories")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T[$L]", ComponentFactory.class, components.size())
                .build();
        classBuilder.addField(factoriesField);

        // factoriesByType map
        FieldSpec factoriesByTypeField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(HashMap.class), ClassName.get(Class.class), ParameterizedTypeName.get(
                        ClassName.get(ComponentFactory.class), TypeVariableName.get("?"))), "factoriesByType")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();
        classBuilder.addField(factoriesByTypeField);

        // factoriesByName map
        FieldSpec factoriesByNameField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(HashMap.class), ClassName.get(String.class), ParameterizedTypeName.get(
                        ClassName.get(ComponentFactory.class), TypeVariableName.get("?"))), "factoriesByName")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();
        classBuilder.addField(factoriesByNameField);

        // factoriesBySupertype map
        FieldSpec factoriesBySupertypeField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(HashMap.class), ClassName.get(Class.class), ParameterizedTypeName.get(
                        ClassName.get(List.class), ParameterizedTypeName.get(
                                ClassName.get(ComponentFactory.class), TypeVariableName.get("?")))), "factoriesBySupertype")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build();
        classBuilder.addField(factoriesBySupertypeField);

        // Singleton cache - stores instantiated singletons
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

        StringBuilder supertypeIndicesInit = new StringBuilder();
        for (Map.Entry<String, List<Integer>> entry : supertypeIndices.entrySet()) {
            String type = entry.getKey();
            List<Integer> indices = entry.getValue();
            supertypeIndicesInit.append("        SUPERTYPE_INDICES.put(").append(toSourceName(type)).append(".class, new int[] {");
            for (int i = 0; i < indices.size(); i++) {
                if (i > 0) supertypeIndicesInit.append(", ");
                supertypeIndicesInit.append(indices.get(i));
            }
            supertypeIndicesInit.append("});\n");
        }

        // Build the static initializer with all statements using CodeBlock
        CodeBlock staticInitCode = CodeBlock.builder()
                .add(typeIndicesInit.toString())
                .add("\n")
                .add(nameIndicesInit.toString())
                .add("\n")
                .add(scopesInit.toString())
                .add("\n")
                .add(lazyFlagsInit.toString())
                .add("\n")
                .add(supertypeIndicesInit.toString())
                .build();

        classBuilder.addStaticBlock(staticInitCode);
    }

    private void addConstructor(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();

        for (int i = 0; i < components.size(); i++) {
            ComponentInfo comp = components.get(i);

            // Instantiate the generated factory class for this component
            // Factory is in the same package as the original component
            String factoryClassName = comp.getFactoryClassName();
            constructorBuilder.addStatement("factories[$L] = new $T()", i, ClassName.bestGuess(factoryClassName));

            constructorBuilder.addStatement("factoriesByType.put($T.class, factories[$L])",
                    ClassName.bestGuess(comp.getClassName()), i);
            constructorBuilder.addStatement("factoriesByName.put($S, factories[$L])", comp.getComponentName(), i);

            // Register interfaces
            for (String iface : comp.getImplementedInterfaces()) {
                constructorBuilder.addStatement("factoriesByType.put($T.class, factories[$L])",
                        ClassName.bestGuess(iface), i);
            }

            // Register in supertype map
            constructorBuilder.addStatement("factoriesBySupertype.computeIfAbsent($T.class, k -> new $T<>()).add(factories[$L])",
                    ClassName.bestGuess(comp.getClassName()), ArrayList.class, i);
            for (String iface : comp.getImplementedInterfaces()) {
                constructorBuilder.addStatement("factoriesBySupertype.computeIfAbsent($T.class, k -> new $T<>()).add(factories[$L])",
                        ClassName.bestGuess(iface), ArrayList.class, i);
            }
        }

        classBuilder.addMethod(constructorBuilder.build());
    }

    private void addGetAllFactories(TypeSpec.Builder classBuilder) {
        MethodSpec getAllFactories = MethodSpec.methodBuilder("getAllFactories")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class),
                        ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("?"))))
                .addStatement("return new $T<>($T.asList(factories))", ArrayList.class, Arrays.class)
                .build();
        classBuilder.addMethod(getAllFactories);
    }

    private void addGetFactoryByType(TypeSpec.Builder classBuilder) {
        MethodSpec getFactoryByType = MethodSpec.methodBuilder("getFactory")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("T")))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type")
                .addStatement("return ($T) factoriesByType.get(type)", ParameterizedTypeName.get(
                        ClassName.get(ComponentFactory.class), TypeVariableName.get("T")))
                .build();
        classBuilder.addMethod(getFactoryByType);
    }

    private void addGetFactoryByName(TypeSpec.Builder classBuilder) {
        MethodSpec getFactoryByName = MethodSpec.methodBuilder("getFactory")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ComponentFactory.class), TypeVariableName.get("?")))
                .addParameter(ClassName.get(String.class), "name")
                .addStatement("return factoriesByName.get(name)")
                .build();
        classBuilder.addMethod(getFactoryByName);
    }

    private void addGetFactoriesForType(TypeSpec.Builder classBuilder) {
        // Create wildcard type "? extends T" for the return type
        TypeName factoryWildcard = ParameterizedTypeName.get(
                ClassName.get(ComponentFactory.class), 
                TypeVariableName.get("? extends T"));
        
        MethodSpec getFactoriesForType = MethodSpec.methodBuilder("getFactoriesForType")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), factoryWildcard))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type")
                .addStatement("$T<?> list = factoriesBySupertype.get(type)", List.class)
                .beginControlFlow("if (list == null)")
                .addStatement("return $T.emptyList()", java.util.Collections.class)
                .endControlFlow()
                .addStatement("return ($T<ComponentFactory<? extends T>>) new $T<>(list)", List.class, ArrayList.class)
                .build();
        classBuilder.addMethod(getFactoriesForType);
    }

    private void addGetSingleton(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder getSingletonBuilder = MethodSpec.methodBuilder("getSingleton")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("T"))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type");

        // Get factory first
        getSingletonBuilder.addStatement("$T<?> factory = ($T<?>) factoriesByType.get(type)", ComponentFactory.class, ComponentFactory.class)
                .beginControlFlow("if (factory == null)")
                .addStatement("return null")
                .endControlFlow();

        // Check if singleton or prototype
        getSingletonBuilder.beginControlFlow("if (factory.getScope() == $T.SINGLETON)", ScopeType.class)
                .beginControlFlow("synchronized (singletonCache)")
                .addStatement("$T cached = ($T) singletonCache.get(type)", Object.class, Object.class)
                .beginControlFlow("if (cached != null)")
                .addStatement("return ($T) cached", TypeVariableName.get("T"))
                .endControlFlow()
                .addStatement("$T newInstance = ($T) factory.create()", Object.class, Object.class)
                .addStatement("singletonCache.put(type, newInstance)")
                .addStatement("return ($T) newInstance", TypeVariableName.get("T"))
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("return ($T) factory.create()", TypeVariableName.get("T"))
                .endControlFlow();

        classBuilder.addMethod(getSingletonBuilder.build());
    }

    private AnnotationSpec createSuppressWarningsAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .addMember("value", "$S", "rawtypes")
                .build();
    }

    /**
     * Converts a binary class name to a source code compatible name.
     * Binary names use '$' for inner classes (e.g., "Outer$Inner"),
     * but source code uses '.' (e.g., "Outer.Inner").
     *
     * @param binaryName the binary class name
     * @return the source-compatible class name
     */
    private String toSourceName(String binaryName) {
        return binaryName.replace('$', '.');
    }

    /**
     * Extracts the package name from a fully qualified class name.
     * Factory names have format: io.github.pkg.Class$$VeldFactory$H<hash>
     *
     * @param className the fully qualified class name
     * @return the package name, or empty string if no package
     */
    private String getPackageName(String className) {
        // Factory class names are like: io.github.pkg.Class$$VeldFactory$H<hash>
        // We need to extract: io.github.pkg

        // Find the last segment before $$VeldFactory
        int factorySuffix = className.indexOf("$$VeldFactory");
        if (factorySuffix > 0) {
            String baseName = className.substring(0, factorySuffix);
            // Now find the last dot in the base name
            int lastDot = baseName.lastIndexOf('.');
            return lastDot > 0 ? baseName.substring(0, lastDot) : "";
        }

        // Fallback: find last dot
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
