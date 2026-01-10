package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.ScopeType;
import io.github.yasmramos.veld.runtime.ComponentFactory;

import java.lang.SuppressWarnings;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates ComponentFactory source code instead of bytecode.
 */
public final class ComponentFactorySourceGenerator {

    private final ComponentInfo component;
    private final int componentIndex;

    public ComponentFactorySourceGenerator(ComponentInfo component) {
        this(component, -1);
    }

    public ComponentFactorySourceGenerator(ComponentInfo component, int componentIndex) {
        this.component = component;
        this.componentIndex = componentIndex;
    }

    public String getFactoryClassName() {
        return component.getFactoryClassName();
    }

    public JavaFile generate() {
        // Factory is generated in .veld subpackage of the original component's package
        String packageName = component.getPackageName();
        String factoryPackageName = packageName.isEmpty() ? "veld" : packageName + ".veld";
        String factoryClassName = component.getFactoryClassName();
        TypeName componentType = ClassName.bestGuess(component.getClassName());

        // Build class declaration with Javadoc
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(getSimpleName(factoryClassName))
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(ComponentFactory.class),
                        TypeVariableName.get(component.getClassName())))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Generated factory for $T.\n", ClassName.bestGuess(component.getClassName()))
                .addAnnotation(createSuppressWarningsAnnotation());

        // Add constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
        classBuilder.addMethod(constructor);

        // Add create() method
        MethodSpec createMethod = generateCreateMethod(componentType);
        classBuilder.addMethod(createMethod);

        // Add getComponentType() method
        MethodSpec getComponentTypeMethod = MethodSpec.methodBuilder("getComponentType")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get(component.getClassName())))
                .addStatement("return $T.class", component.getClassName())
                .build();
        classBuilder.addMethod(getComponentTypeMethod);

        // Add getComponentName() method
        MethodSpec getComponentNameMethod = MethodSpec.methodBuilder("getComponentName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", component.getComponentName())
                .build();
        classBuilder.addMethod(getComponentNameMethod);

        // Add getScope() method
        String scopeName = component.getScope() == ScopeType.SINGLETON ? "SINGLETON" : "PROTOTYPE";
        MethodSpec getScopeMethod = MethodSpec.methodBuilder("getScope")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ScopeType.class)
                .addStatement("return $T.$N", ScopeType.class, scopeName)
                .build();
        classBuilder.addMethod(getScopeMethod);

        // Add getScopeId() method - supports custom scopes
        MethodSpec getScopeIdMethod = MethodSpec.methodBuilder("getScopeId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", component.getScopeId())
                .build();
        classBuilder.addMethod(getScopeIdMethod);

        // Add isLazy() method
        MethodSpec isLazyMethod = MethodSpec.methodBuilder("isLazy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return $L", component.isLazy())
                .build();
        classBuilder.addMethod(isLazyMethod);

        // Add invokePostConstruct() method
        MethodSpec invokePostConstructMethod = generateInvokePostConstructMethod(componentType);
        classBuilder.addMethod(invokePostConstructMethod);

        // Add invokePreDestroy() method
        MethodSpec invokePreDestroyMethod = generateInvokePreDestroyMethod(componentType);
        classBuilder.addMethod(invokePreDestroyMethod);

        // Add getIndex() method - for ultra-fast array-based lookups
        MethodSpec getIndexMethod = MethodSpec.methodBuilder("getIndex")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", componentIndex)
                .build();
        classBuilder.addMethod(getIndexMethod);

        // Add getDependencyTypes() method - for dependency graph visualization
        MethodSpec getDependencyTypesMethod = generateGetDependencyTypesMethod();
        classBuilder.addMethod(getDependencyTypesMethod);

        // Build JavaFile
        JavaFile.Builder javaFileBuilder = JavaFile.builder(factoryPackageName, classBuilder.build());

        return javaFileBuilder.build();
    }

    private MethodSpec generateCreateMethod(TypeName componentType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(componentType);

        // Create instance with constructor dependencies
        methodBuilder.addStatement("$T instance = new $T($L)",
                componentType, componentType, generateConstructorArgs());

        // Field injections - use synthetic setters for private fields
        for (InjectionPoint field : component.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                if (dep.isValueInjection()) {
                    // @Value injection - skip for now
                    continue;
                }

                String fieldName = field.getName();
                String setterName;
                if (field.getVisibility() == InjectionPoint.Visibility.PRIVATE) {
                    setterName = "__di_set_" + fieldName;
                } else {
                    setterName = "set" + capitalize(fieldName);
                }
                methodBuilder.addStatement("instance.$N($L)", setterName, generateDependencyGetExpression(dep));
            }
        }

        // Method injections
        for (InjectionPoint method : component.getMethodInjections()) {
            List<String> args = new ArrayList<>();
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                args.add(generateDependencyGetExpression(dep));
            }
            methodBuilder.addStatement("instance.$N($L)", method.getName(), String.join(", ", args));
        }

        methodBuilder.addStatement("return instance");

        return methodBuilder.build();
    }

    private String generateConstructorArgs() {
        InjectionPoint ctor = component.getConstructorInjection();
        if (ctor == null || ctor.getDependencies().isEmpty()) {
            return "";
        }

        List<String> args = new ArrayList<>();
        for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
            args.add(generateDependencyGetExpression(dep));
        }
        return String.join(", ", args);
    }

    /**
     * Generates the appropriate Veld.get() expression for a dependency.
     * Handles Provider<T> and Optional<T> types correctly.
     */
    private String generateDependencyGetExpression(InjectionPoint.Dependency dep) {
        TypeName depType = ClassName.bestGuess(dep.getTypeName());
        if (dep.isProvider()) {
            // Provider<T> injection - use Veld.getProvider()
            return "$T.getProvider(" + dep.getActualTypeName() + ".class)";
        } else if (dep.isOptionalWrapper()) {
            // Optional<T> injection - use Veld.getOptional()
            return "$T.getOptional(" + dep.getActualTypeName() + ".class)";
        } else {
            // Regular injection
            return "$T.get(" + dep.getTypeName() + ".class)";
        }
    }

    private MethodSpec generateInvokePostConstructMethod(TypeName componentType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("invokePostConstruct")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(componentType, "instance").build())
                .returns(void.class);

        if (component.hasPostConstruct()) {
            methodBuilder.addStatement("instance.$N()", component.getPostConstructMethod());
        }

        return methodBuilder.build();
    }

    private MethodSpec generateInvokePreDestroyMethod(TypeName componentType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("invokePreDestroy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(componentType, "instance").build())
                .returns(void.class);

        if (component.hasPreDestroy()) {
            methodBuilder.addStatement("instance.$N()", component.getPreDestroyMethod());
        }

        return methodBuilder.build();
    }

    private MethodSpec generateGetDependencyTypesMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getDependencyTypes")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)));

        List<String> dependencies = new ArrayList<>();

        // Constructor dependencies
        InjectionPoint ctor = component.getConstructorInjection();
        if (ctor != null) {
            for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
                dependencies.add("\"" + dep.getActualTypeName() + "\"");
            }
        }

        // Field dependencies
        for (InjectionPoint field : component.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                if (dep.isValueInjection()) {
                    continue;
                }
                dependencies.add("\"" + dep.getActualTypeName() + "\"");
            }
        }

        // Method dependencies
        for (InjectionPoint method : component.getMethodInjections()) {
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                dependencies.add("\"" + dep.getActualTypeName() + "\"");
            }
        }

        if (dependencies.isEmpty()) {
            methodBuilder.addStatement("return $T.of()", List.class);
        } else {
            methodBuilder.addStatement("return $T.asList($L)", Arrays.class, String.join(", ", dependencies));
        }

        return methodBuilder.build();
    }

    private AnnotationSpec createSuppressWarningsAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .addMember("value", "$S", "rawtypes")
                .build();
    }

    private String getSimpleName(String fullClassName) {
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
