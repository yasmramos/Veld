package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.yasmramos.veld.VeldException;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.ScopeType;
import io.github.yasmramos.veld.annotation.Value;
import io.github.yasmramos.veld.runtime.ComponentFactory;
import io.github.yasmramos.veld.runtime.ComponentRegistry;
import io.github.yasmramos.veld.runtime.ConditionalRegistry;
import io.github.yasmramos.veld.runtime.Provider;
import io.github.yasmramos.veld.runtime.event.EventBus;
import io.github.yasmramos.veld.runtime.lifecycle.LifecycleProcessor;
import io.github.yasmramos.veld.runtime.value.ValueResolver;

import java.lang.SuppressWarnings;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generates Veld.java source code instead of bytecode.
 * Uses the Initialization-on-demand holder idiom for lock-free singleton access.
 */
public final class VeldSourceGenerator {

    private final List<ComponentInfo> components;
    private final Map<String, String> aopClassMap;

    public VeldSourceGenerator(List<ComponentInfo> components) {
        this(components, java.util.Collections.emptyMap());
    }

    public VeldSourceGenerator(List<ComponentInfo> components, Map<String, String> aopClassMap) {
        this.components = components;
        this.aopClassMap = aopClassMap;
    }

    public String getClassName() {
        return "io.github.yasmramos.veld.Veld";
    }

    public JavaFile generate() {
        // Build class declaration with Javadoc
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("Veld")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(
                        "Generated service locator for Veld DI container.\n" +
                        "Uses Initialization-on-demand holder idiom for lock-free singleton access.\n")
                .addAnnotation(createSuppressWarningsAnnotation());

        // Add static fields
        addStaticFields(classBuilder);

        // Add static initializer
        addStaticInitializer(classBuilder);

        // Add computeActiveProfiles method
        addComputeActiveProfilesMethod(classBuilder);

        // Add Holder classes for singletons
        generateHolderClasses(classBuilder);

        // Add getter methods for each component
        generateGetMethods(classBuilder);

        // Add generic get by class method
        generateGetByClass(classBuilder);

        // Add additional generic methods
        generateAdditionalGenericMethods(classBuilder);

        // Add registry accessor
        addRegistryAccessorMethod(classBuilder);

        // Add EventBus accessor
        addEventBusAccessorMethod(classBuilder);

        // Add LifecycleProcessor accessor
        addLifecycleProcessorAccessorMethod(classBuilder);

        // Add shutdown method
        addShutdownMethod(classBuilder);

        // Add inject method
        addInjectMethod(classBuilder);

        // Add resolveValue method
        addResolveValueMethod(classBuilder);

        // Add profile management methods
        addProfileManagementMethods(classBuilder);

        // Add sortByOrder method
        addSortByOrderMethod(classBuilder);

        // Build JavaFile
        JavaFile.Builder javaFileBuilder = JavaFile.builder("io.github.yasmramos.veld", classBuilder.build());

        return javaFileBuilder.build();
    }

    private void addStaticFields(TypeSpec.Builder classBuilder) {
        ClassName veldRegistryClass = ClassName.bestGuess("io.github.yasmramos.veld.VeldRegistry");

        // Registry and lifecycle processor (no volatile needed for final fields)
        FieldSpec registryField = FieldSpec.builder(veldRegistryClass, "_registry")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T()", veldRegistryClass)
                .build();
        classBuilder.addField(registryField);

        FieldSpec lifecycleField = FieldSpec.builder(ClassName.get(LifecycleProcessor.class), "_lifecycle")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
        classBuilder.addField(lifecycleField);

        FieldSpec conditionalRegistryField = FieldSpec.builder(ClassName.get(ConditionalRegistry.class), "_conditionalRegistry")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();
        classBuilder.addField(conditionalRegistryField);

        FieldSpec eventBusField = FieldSpec.builder(ClassName.get(EventBus.class), "_eventBus")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.getInstance()", EventBus.class)
                .build();
        classBuilder.addField(eventBusField);

        FieldSpec activeProfilesField = FieldSpec.builder(String[].class, "_activeProfiles")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .initializer("new $T[0]", String.class)
                .build();
        classBuilder.addField(activeProfilesField);
    }

    private void addStaticInitializer(TypeSpec.Builder classBuilder) {
        // Create static initializer block using CodeBlock
        CodeBlock staticInitCode = CodeBlock.builder()
                .addStatement("Set<String> initialProfiles = computeActiveProfiles()")
                .addStatement("_activeProfiles = initialProfiles.toArray(new String[0])")
                .addStatement("_lifecycle = new $T()", LifecycleProcessor.class)
                .addStatement("_lifecycle.setEventBus(_eventBus)")
                .addStatement("_conditionalRegistry = new $_(_registry, initialProfiles)", ConditionalRegistry.class)
                .build();

        classBuilder.addStaticBlock(staticInitCode);
    }

    private void addComputeActiveProfilesMethod(TypeSpec.Builder classBuilder) {
        MethodSpec computeProfiles = MethodSpec.methodBuilder("computeActiveProfiles")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(String.class)))
                .addStatement("$T profiles = $T.getProperty(\"veld.profiles.active\", \n            $T.getenv().getOrDefault(\"VELD_PROFILES_ACTIVE\", \"\"))",
                        String.class, System.class, System.class)
                .beginControlFlow("if (profiles.isEmpty())")
                .addStatement("return $T.of()", Set.class)
                .endControlFlow()
                .addStatement("return $T.of(profiles.split(\",\"))", Set.class)
                .build();
        classBuilder.addMethod(computeProfiles);
    }

    private void generateHolderClasses(TypeSpec.Builder classBuilder) {
        classBuilder.addJavadoc("=== HOLDER CLASSES FOR LOCK-FREE SINGLETON ACCESS ===\n");

        for (ComponentInfo comp : components) {
            if (comp.getScope() == ScopeType.SINGLETON && comp.canUseHolderPattern()) {
                String holderName = getHolderClassName(comp);
                TypeName returnType = ClassName.bestGuess(comp.getClassName());

                TypeSpec holderClass = TypeSpec.classBuilder(holderName)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addField(FieldSpec.builder(returnType, "INSTANCE")
                                .addModifiers(Modifier.STATIC, Modifier.FINAL)
                                .initializer("new $T()", ClassName.bestGuess(comp.getClassName()))
                                .build())
                        .build();

                classBuilder.addType(holderClass);
            }
        }
    }

    private void generateGetMethods(TypeSpec.Builder classBuilder) {
        classBuilder.addJavadoc("=== GETTER METHODS ===\n");

        // Track generated method names to avoid duplicates
        java.util.Set<String> generatedMethodNames = new java.util.HashSet<>();

        for (ComponentInfo comp : components) {
            String methodName = getGetterMethodName(comp);
            TypeName returnType = ClassName.bestGuess(comp.getClassName());

            // Handle duplicate method names by adding a suffix
            String uniqueMethodName = methodName;
            int suffix = 1;
            while (generatedMethodNames.contains(uniqueMethodName)) {
                uniqueMethodName = methodName + "_" + suffix++;
            }
            generatedMethodNames.add(uniqueMethodName);

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(uniqueMethodName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(returnType);

            if (comp.getScope() == ScopeType.SINGLETON) {
                if (comp.canUseHolderPattern()) {
                    // Simple singleton - use holder pattern (lock-free, direct instantiation)
                    String holderName = getHolderClassName(comp);
                    methodBuilder.addStatement("$N.INSTANCE", holderName);
                } else {
                    // Complex singleton - use factory (supports DI, lifecycle, conditions, AOP)
                    methodBuilder.addStatement("($T) _registry.getFactory($T.class).create()", returnType, ClassName.bestGuess(comp.getClassName()));
                }
            } else {
                // Prototype - always create new via factory
                methodBuilder.addStatement("($T) _registry.getFactory($T.class).create()", returnType, ClassName.bestGuess(comp.getClassName()));
            }

            classBuilder.addMethod(methodBuilder.build());
        }
    }

    private void generateGetByClass(TypeSpec.Builder classBuilder) {
        classBuilder.addJavadoc("=== GENERIC GET BY CLASS ===\n");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("get")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeVariableName.get("T"))
                .addParameter(ParameterSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type").build());

        // Generate if-else chain for each component type
        for (ComponentInfo comp : components) {
            methodBuilder.beginControlFlow("if (type == $T.class)", ClassName.bestGuess(comp.getClassName()));

            if (comp.getScope() == ScopeType.SINGLETON && comp.canUseHolderPattern()) {
                // Simple singleton - use holder pattern
                String holderName = getHolderClassName(comp);
                methodBuilder.addStatement("return ($T) $N.INSTANCE", TypeVariableName.get("T"), holderName);
            } else {
                // Complex singleton or prototype - use factory
                methodBuilder.addStatement("return ($T) _registry.getFactory(type).create()", TypeVariableName.get("T"));
            }

            methodBuilder.endControlFlow();
        }

        // Also check interfaces
        for (ComponentInfo comp : components) {
            for (String iface : comp.getImplementedInterfaces()) {
                methodBuilder.beginControlFlow("if (type == $T.class)", ClassName.bestGuess(iface));

                if (comp.getScope() == ScopeType.SINGLETON && comp.canUseHolderPattern()) {
                    String holderName = getHolderClassName(comp);
                    methodBuilder.addStatement("return ($T) $N.INSTANCE", TypeVariableName.get("T"), holderName);
                } else {
                    methodBuilder.addStatement("return ($T) _registry.getFactory(type).create()", TypeVariableName.get("T"));
                }

                methodBuilder.endControlFlow();
            }
        }

        methodBuilder.addStatement("throw new $T(\"No component registered for type: \" + type.getName())", VeldException.class);

        classBuilder.addMethod(methodBuilder.build());
    }

    private void generateAdditionalGenericMethods(TypeSpec.Builder classBuilder) {
        classBuilder.addJavadoc("=== ADDITIONAL GENERIC METHODS ===\n");

        // get(Class<T> type, String name)
        MethodSpec getByName = MethodSpec.methodBuilder("get")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeVariableName.get("T"))
                .addParameter(ParameterSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type").build())
                .addParameter(ParameterSpec.builder(String.class, "name").build())
                .addStatement("$T<?> factory = _registry.getFactory(name)", ComponentFactory.class)
                .beginControlFlow("if (factory != null)")
                .addStatement("return ($T) factory.create()", TypeVariableName.get("T"))
                .endControlFlow()
                .addStatement("throw new $T(\"No component registered with name: \" + name)", VeldException.class)
                .build();
        classBuilder.addMethod(getByName);

        // getAll(Class<T> type)
        MethodSpec getAll = MethodSpec.methodBuilder("getAll")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), TypeVariableName.get("T")))
                .addParameter(ParameterSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type").build())
                .addStatement("$T<$T<? extends $T>> factories = _registry.getFactoriesForType(type)",
                        List.class, ComponentFactory.class, TypeVariableName.get("T"))
                .addStatement("$T<$T> instances = new $T<>()", List.class, TypeVariableName.get("T"), ArrayList.class)
                .beginControlFlow("for ($T<? extends $T> factory : factories)",
                        ComponentFactory.class, TypeVariableName.get("T"))
                .addStatement("instances.add(($T) factory.create())", TypeVariableName.get("T"))
                .endControlFlow()
                .addStatement("return instances")
                .build();
        classBuilder.addMethod(getAll);

        // getProvider(Class<T> type)
        MethodSpec getProvider = MethodSpec.methodBuilder("getProvider")
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Provider.class), TypeVariableName.get("T")))
                .addParameter(ParameterSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type").build())
                .addStatement("$T<$T> factory = _registry.getFactory(type)", ComponentFactory.class, TypeVariableName.get("T"))
                .beginControlFlow("if (factory != null)")
                .addStatement("return () -> ($T) factory.create()", TypeVariableName.get("T"))
                .endControlFlow()
                .addStatement("return () -> { throw new $T(\"No component registered for type: \" + type.getName()); };", VeldException.class)
                .build();
        classBuilder.addMethod(getProvider);

        // getOptional(Class<T> type)
        MethodSpec getOptional = MethodSpec.methodBuilder("getOptional")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), TypeVariableName.get("T")))
                .addParameter(ParameterSpec.builder(
                        ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "type").build())
                .addStatement("$T<$T> factory = _registry.getFactory(type)", ComponentFactory.class, TypeVariableName.get("T"))
                .beginControlFlow("if (factory != null)")
                .addStatement("return $T.of(($T) factory.create())", Optional.class, TypeVariableName.get("T"))
                .endControlFlow()
                .addStatement("return $T.empty()", Optional.class)
                .build();
        classBuilder.addMethod(getOptional);

        // contains(Class<?> type)
        MethodSpec contains = MethodSpec.methodBuilder("contains")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(ParameterSpec.builder(ClassName.get(Class.class), "type").build())
                .addStatement("return _registry.getFactory(type) != null")
                .build();
        classBuilder.addMethod(contains);

        // componentCount()
        MethodSpec componentCount = MethodSpec.methodBuilder("componentCount")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(int.class)
                .addStatement("return _registry.getComponentCount()")
                .build();
        classBuilder.addMethod(componentCount);
    }

    private void addRegistryAccessorMethod(TypeSpec.Builder classBuilder) {
        MethodSpec registryAccessor = MethodSpec.methodBuilder("getRegistry")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(ComponentRegistry.class))
                .addStatement("return _registry")
                .build();
        classBuilder.addMethod(registryAccessor);
    }

    private void addEventBusAccessorMethod(TypeSpec.Builder classBuilder) {
        MethodSpec eventBusAccessor = MethodSpec.methodBuilder("getEventBus")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(EventBus.class))
                .addStatement("return _eventBus")
                .build();
        classBuilder.addMethod(eventBusAccessor);
    }

    private void addLifecycleProcessorAccessorMethod(TypeSpec.Builder classBuilder) {
        MethodSpec lifecycleAccessor = MethodSpec.methodBuilder("getLifecycleProcessor")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(LifecycleProcessor.class))
                .addStatement("return _lifecycle")
                .build();
        classBuilder.addMethod(lifecycleAccessor);
    }

    private void addShutdownMethod(TypeSpec.Builder classBuilder) {
        MethodSpec shutdown = MethodSpec.methodBuilder("shutdown")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("_lifecycle.destroy()")
                .build();
        classBuilder.addMethod(shutdown);
    }

    private void addInjectMethod(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder injectMethod = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(ClassName.get(Object.class), "instance").build())
                .addStatement("if (instance == null) return")
                .addComment("Use reflection to inject fields - this is only called for factory instances")
                .addComment("which are created by the generated factory classes themselves")
                .addStatement("$T<?> clazz = instance.getClass()", Class.class)
                .beginControlFlow("for ($T field : clazz.getDeclaredFields())", java.lang.reflect.Field.class)
                .addStatement("if (!$T.isStatic(field.getModifiers()) && !$T.isFinal(field.getModifiers()))",
                        java.lang.reflect.Modifier.class, java.lang.reflect.Modifier.class)
                .addStatement("$T injectAnn = field.getAnnotation($T.class)", Inject.class, Inject.class)
                .addStatement("$T valueAnn = field.getAnnotation($T.class)", Value.class, Value.class)
                .beginControlFlow("if (injectAnn != null)")
                .addStatement("try")
                .addStatement("field.setAccessible(true)")
                .addStatement("$T<?> fieldType = field.getType()", Class.class)
                .addStatement("$T value = get(fieldType)", Object.class)
                .addStatement("field.set(instance, value)")
                .addStatement("catch ($T e)", Exception.class)
                .addStatement("throw new $T(\"Failed to inject field: \" + field.getName(), e)", VeldException.class)
                .endControlFlow()
                .beginControlFlow("else if (valueAnn != null)")
                .addStatement("try")
                .addStatement("field.setAccessible(true)")
                .addStatement("$T value = resolveValue(valueAnn.value())", Object.class)
                .addStatement("field.set(instance, value)")
                .addStatement("catch ($T e)", Exception.class)
                .addStatement("throw new $T(\"Failed to inject @Value field: \" + field.getName(), e)", VeldException.class)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow();

        classBuilder.addMethod(injectMethod.build());
    }

    private void addResolveValueMethod(TypeSpec.Builder classBuilder) {
        MethodSpec resolveValue = MethodSpec.methodBuilder("resolveValue")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addParameter(ParameterSpec.builder(String.class, "expression").build())
                .addStatement("return $T.getInstance().resolve(expression)", ValueResolver.class)
                .build();
        classBuilder.addMethod(resolveValue);
    }

    private void addProfileManagementMethods(TypeSpec.Builder classBuilder) {
        classBuilder.addJavadoc("=== PROFILE MANAGEMENT ===\n");

        MethodSpec setActiveProfiles = MethodSpec.methodBuilder("setActiveProfiles")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(String[].class, "profiles").build())
                .addStatement("_activeProfiles = profiles != null ? profiles : new $T[0]", String.class)
                .build();
        classBuilder.addMethod(setActiveProfiles);

        MethodSpec getActiveProfiles = MethodSpec.methodBuilder("getActiveProfiles")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String[].class)
                .addStatement("return _activeProfiles.clone()")
                .build();
        classBuilder.addMethod(getActiveProfiles);

        MethodSpec isProfileActive = MethodSpec.methodBuilder("isProfileActive")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(ParameterSpec.builder(String.class, "profile").build())
                .addStatement("if (profile == null) return false")
                .beginControlFlow("for ($T p : _activeProfiles)", String.class)
                .addStatement("if (profile.equals(p)) return true")
                .endControlFlow()
                .addStatement("return false")
                .build();
        classBuilder.addMethod(isProfileActive);
    }

    private void addSortByOrderMethod(TypeSpec.Builder classBuilder) {
        MethodSpec sortByOrder = MethodSpec.methodBuilder("sortByOrder")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(ComponentFactory.class)))
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(ComponentFactory.class)), "factories").build())
                .addStatement("return factories.stream()\n            .sorted($T.comparingInt($T::getOrder))\n            .collect($T.toList())",
                        Comparator.class, ComponentFactory.class, Collectors.class)
                .build();
        classBuilder.addMethod(sortByOrder);
    }

    private AnnotationSpec createSuppressWarningsAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .addMember("value", "$S", "rawtypes")
                .build();
    }

    private String getHolderClassName(ComponentInfo comp) {
        return "Holder_" + getSimpleName(comp);
    }

    private String getGetterMethodName(ComponentInfo comp) {
        String simpleName = getSimpleName(comp);
        return decapitalize(simpleName);
    }

    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String getSimpleName(ComponentInfo comp) {
        String className = comp.getClassName();
        // For inner classes (Outer$Inner), get just the simple name (Inner)
        int lastDollar = className.lastIndexOf('$');
        if (lastDollar >= 0) {
            return className.substring(lastDollar + 1);
        }
        // For regular classes (package.Class), get the class name
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
}
