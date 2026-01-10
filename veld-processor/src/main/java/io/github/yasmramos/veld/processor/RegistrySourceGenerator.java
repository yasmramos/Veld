package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.AnnotationSpec;
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
                .superclass(ClassName.get(ComponentRegistry.class))
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

        // Methods
        addGetIndexByType(classBuilder);
        addGetIndexByName(classBuilder);
        addGetComponentCount(classBuilder);
        addGetScope(classBuilder);
        addIsLazy(classBuilder);
        addCreate(classBuilder);
        addGetIndicesForType(classBuilder);
        addInvokePostConstruct(classBuilder);
        addInvokePreDestroy(classBuilder);
        addGetAllFactories(classBuilder);
        addGetFactoryByType(classBuilder);
        addGetFactoryByName(classBuilder);
        addGetFactoriesForType(classBuilder);

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
        FieldSpec factoriesField = FieldSpec.builder(ParameterizedTypeName.get(
                ClassName.get(ComponentFactory.class), TypeVariableName.get("?")), "factories")
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
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addStatement("factories = new $T[$L]", ComponentFactory.class, components.size());

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

    private void addGetIndexByType(TypeSpec.Builder classBuilder) {
        MethodSpec getIndexByType = MethodSpec.methodBuilder("getIndex")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(ClassName.get(Class.class), "type")
                .addStatement("$T idx = TYPE_INDICES.get(type)", Integer.class)
                .addStatement("return idx != null ? idx : -1")
                .build();
        classBuilder.addMethod(getIndexByType);
    }

    private void addGetIndexByName(TypeSpec.Builder classBuilder) {
        MethodSpec getIndexByName = MethodSpec.methodBuilder("getIndex")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(ClassName.get(String.class), "name")
                .addStatement("$T idx = NAME_INDICES.get(name)", Integer.class)
                .addStatement("return idx != null ? idx : -1")
                .build();
        classBuilder.addMethod(getIndexByName);
    }

    private void addGetComponentCount(TypeSpec.Builder classBuilder) {
        MethodSpec getComponentCount = MethodSpec.methodBuilder("getComponentCount")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", components.size())
                .build();
        classBuilder.addMethod(getComponentCount);
    }

    private void addGetScope(TypeSpec.Builder classBuilder) {
        MethodSpec getScope = MethodSpec.methodBuilder("getScope")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ScopeType.class)
                .addParameter(int.class, "index")
                .addStatement("return $T.fromScopeId(SCOPES[index])", ScopeType.class)
                .build();
        classBuilder.addMethod(getScope);
    }

    private void addIsLazy(TypeSpec.Builder classBuilder) {
        MethodSpec isLazy = MethodSpec.methodBuilder("isLazy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(int.class, "index")
                .addStatement("return LAZY_FLAGS[index]")
                .build();
        classBuilder.addMethod(isLazy);
    }

    private void addCreate(TypeSpec.Builder classBuilder) {
        MethodSpec create = MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeVariableName.get("T"))
                .addParameter(int.class, "index")
                .addStatement("return ($T) factories[index].create()", TypeVariableName.get("T"))
                .build();
        classBuilder.addMethod(create);
    }

    private void addGetIndicesForType(TypeSpec.Builder classBuilder) {
        MethodSpec getIndicesForType = MethodSpec.methodBuilder("getIndicesForType")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int[].class)
                .addParameter(ClassName.get(Class.class), "type")
                .addStatement("int[] indices = SUPERTYPE_INDICES.get(type)")
                .addStatement("return indices != null ? indices : new int[0]")
                .build();
        classBuilder.addMethod(getIndicesForType);
    }

    private void addInvokePostConstruct(TypeSpec.Builder classBuilder) {
        MethodSpec invokePostConstruct = MethodSpec.methodBuilder("invokePostConstruct")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(int.class, "index")
                .addParameter(ClassName.get(Object.class), "instance")
                .addStatement("((ComponentFactory<Object>) factories[index]).invokePostConstruct(instance)")
                .build();
        classBuilder.addMethod(invokePostConstruct);
    }

    private void addInvokePreDestroy(TypeSpec.Builder classBuilder) {
        MethodSpec invokePreDestroy = MethodSpec.methodBuilder("invokePreDestroy")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(int.class, "index")
                .addParameter(ClassName.get(Object.class), "instance")
                .addStatement("((ComponentFactory<Object>) factories[index]).invokePreDestroy(instance)")
                .build();
        classBuilder.addMethod(invokePreDestroy);
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
        MethodSpec getFactoriesForType = MethodSpec.methodBuilder("getFactoriesForType")
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class),
                        ParameterizedTypeName.get(ClassName.get(ComponentFactory.class),
                                TypeVariableName.get("T"))))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type")
                .addStatement("$T<?> list = factoriesBySupertype.get(type)", List.class)
                .beginControlFlow("if (list == null)")
                .addStatement("return $T.emptyList()", java.util.Collections.class)
                .endControlFlow()
                .addStatement("return ($T) new $T<>(list)", ParameterizedTypeName.get(
                        ClassName.get(List.class), ParameterizedTypeName.get(
                                ClassName.get(ComponentFactory.class), TypeVariableName.get("T"))), ArrayList.class)
                .build();
        classBuilder.addMethod(getFactoriesForType);
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
