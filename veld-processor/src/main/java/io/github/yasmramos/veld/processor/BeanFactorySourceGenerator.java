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
import java.util.Arrays;
import java.util.List;

/**
 * Generates BeanFactory source code for @Bean methods in @Factory classes.
 *
 * <p>Each @Bean method gets its own factory class that invokes the factory method
 * to produce the bean instance. The generated factory implements ComponentFactory
 * and handles all aspects of bean creation including lifecycle callbacks.</p>
 */
public final class BeanFactorySourceGenerator {

    private final FactoryInfo factory;
    private final FactoryInfo.BeanMethod beanMethod;
    private final int beanIndex;

    public BeanFactorySourceGenerator(FactoryInfo factory, FactoryInfo.BeanMethod beanMethod, int beanIndex) {
        this.factory = factory;
        this.beanMethod = beanMethod;
        this.beanIndex = beanIndex;
    }

    public String getFactoryClassName() {
        // Generate unique factory class name: ReturnType$$VeldBeanFactory$index
        String returnTypeSimple = getSimpleName(beanMethod.getReturnType());
        return beanMethod.getReturnType() + "$$VeldBeanFactory$" + beanIndex;
    }

    public JavaFile generate() {
        String packageName = getPackageName(beanMethod.getReturnType());
        String returnTypeSimple = getSimpleName(beanMethod.getReturnType());
        String factorySimpleName = returnTypeSimple + "$$VeldBeanFactory$" + beanIndex;
        String factoryClassName = beanMethod.getReturnType() + "$$VeldBeanFactory$" + beanIndex;
        TypeName beanType = getClassName(beanMethod.getReturnType());
        TypeName factoryClassType = getClassName(factory.getFactoryClassName());

        // Build class declaration with Javadoc
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(factorySimpleName)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(ComponentFactory.class),
                        beanType))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(
                        "Generated factory for @Bean method: $1S\n" +
                        "Factory class: $2S\n" +
                        "Bean type: $3S\n",
                        beanMethod.getMethodName(),
                        factory.getFactoryClassName(),
                        beanMethod.getReturnType())
                .addAnnotation(createSuppressWarningsAnnotation());

        // Add factory instance field (singleton pattern)
        FieldSpec factoryField = FieldSpec.builder(factoryClassType, "factoryInstance")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.VOLATILE)
                .build();
        classBuilder.addField(factoryField);

        // Add constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
        classBuilder.addMethod(constructor);

        // Add getFactoryClass() method
        MethodSpec getFactoryClassMethod = MethodSpec.methodBuilder("getFactoryClass")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(Class.class)
                .addStatement("return $T.class", ClassName.bestGuess(factory.getFactoryClassName()))
                .build();
        classBuilder.addMethod(getFactoryClassMethod);

        // Add getBeanMethodName() method
        MethodSpec getBeanMethodNameMethod = MethodSpec.methodBuilder("getBeanMethodName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", beanMethod.getMethodName())
                .build();
        classBuilder.addMethod(getBeanMethodNameMethod);

        // Add create() method
        MethodSpec createMethod = generateCreateMethod(factorySimpleName, factoryClassName, beanType);
        classBuilder.addMethod(createMethod);

        // Add getComponentType() method
        MethodSpec getComponentTypeMethod = MethodSpec.methodBuilder("getComponentType")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), beanType))
                .addStatement("return $T.class", ClassName.bestGuess(beanMethod.getReturnType()))
                .build();
        classBuilder.addMethod(getComponentTypeMethod);

        // Add getComponentName() method
        MethodSpec getComponentNameMethod = MethodSpec.methodBuilder("getComponentName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", beanMethod.getBeanName())
                .build();
        classBuilder.addMethod(getComponentNameMethod);

        // Add getScope() method
        String scopeName = beanMethod.getScope() == ScopeType.PROTOTYPE ? "PROTOTYPE" : "SINGLETON";
        MethodSpec getScopeMethod = MethodSpec.methodBuilder("getScope")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ScopeType.class)
                .addStatement("return $T.$N", ScopeType.class, scopeName)
                .build();
        classBuilder.addMethod(getScopeMethod);

        // Add getQualifier() method if present
        if (beanMethod.hasQualifier()) {
            MethodSpec getQualifierMethod = MethodSpec.methodBuilder("getQualifier")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addStatement("return $S", beanMethod.getQualifier())
                    .build();
            classBuilder.addMethod(getQualifierMethod);
        }

        // Add isLazy() method
        MethodSpec isLazyMethod = MethodSpec.methodBuilder("isLazy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return false")
                .build();
        classBuilder.addMethod(isLazyMethod);

        // Add isPrimary() method if primary
        if (beanMethod.isPrimary()) {
            MethodSpec isPrimaryMethod = MethodSpec.methodBuilder("isPrimary")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(boolean.class)
                    .addStatement("return true")
                    .build();
            classBuilder.addMethod(isPrimaryMethod);
        }

        // Add getFactoryMethodParameters() method
        MethodSpec getFactoryMethodParametersMethod = generateGetFactoryMethodParametersMethod();
        classBuilder.addMethod(getFactoryMethodParametersMethod);

        // Add invokePostConstruct() method
        MethodSpec invokePostConstructMethod = generateInvokePostConstructMethod(beanType);
        classBuilder.addMethod(invokePostConstructMethod);

        // Add invokePreDestroy() method
        MethodSpec invokePreDestroyMethod = generateInvokePreDestroyMethod(beanType);
        classBuilder.addMethod(invokePreDestroyMethod);

        // Build JavaFile
        JavaFile.Builder javaFileBuilder = JavaFile.builder(packageName != null ? packageName : "", classBuilder.build());

        return javaFileBuilder.build();
    }

    private MethodSpec generateCreateMethod(String factorySimpleName, String factoryClassName, TypeName beanType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(beanType);

        // Get or create factory instance (singleton pattern)
        methodBuilder.addComment("Get or create the factory class instance");
        methodBuilder.beginControlFlow("if (factoryInstance == null)");
        methodBuilder.beginControlFlow("synchronized ($L.class)", factorySimpleName);
        methodBuilder.beginControlFlow("if (factoryInstance == null)");
        methodBuilder.addStatement("factoryInstance = new $T()", ClassName.bestGuess(factory.getFactoryClassName()));
        methodBuilder.addComment("Inject dependencies into the factory instance");
        methodBuilder.addStatement("$T.inject(factoryInstance)", Veld.class);
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        // Invoke the @Bean method
        methodBuilder.addComment("Invoke the @Bean method to produce the bean");
        methodBuilder.addCode("return factoryInstance.$N(", beanMethod.getMethodName());

        List<String> paramTypes = beanMethod.getParameterTypes();
        if (paramTypes.isEmpty()) {
            methodBuilder.addStatement(")");
        } else {
            methodBuilder.addCode("\n");
            for (int i = 0; i < paramTypes.size(); i++) {
                String paramType = paramTypes.get(i);
                methodBuilder.addStatement("$T.get($T.class)$L",
                        Veld.class,
                        ClassName.bestGuess(paramType),
                        i < paramTypes.size() - 1 ? "," : "");
            }
            methodBuilder.addStatement(")");
        }

        return methodBuilder.build();
    }

    private MethodSpec generateGetFactoryMethodParametersMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getFactoryMethodParameters")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Class.class)));

        if (beanMethod.getParameterTypes().isEmpty()) {
            methodBuilder.addStatement("return $T.of()", List.class);
        } else {
            methodBuilder.addStatement("return $T.asList($T.class", Arrays.class, ClassName.bestGuess(beanMethod.getParameterTypes().get(0)));
            for (int i = 1; i < beanMethod.getParameterTypes().size(); i++) {
                String paramType = beanMethod.getParameterTypes().get(i);
                methodBuilder.addStatement("    , $T.class", ClassName.bestGuess(paramType));
            }
            methodBuilder.addStatement(")");
        }

        return methodBuilder.build();
    }

    private MethodSpec generateInvokePostConstructMethod(TypeName beanType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("invokePostConstruct")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(beanType, "instance").build())
                .returns(void.class);

        if (beanMethod.hasPostConstruct()) {
            methodBuilder.addStatement("instance.$N()", beanMethod.getPostConstructMethodName());
        }

        return methodBuilder.build();
    }

    private MethodSpec generateInvokePreDestroyMethod(TypeName beanType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("invokePreDestroy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(beanType, "instance").build())
                .returns(void.class);

        if (beanMethod.hasPreDestroy()) {
            methodBuilder.addStatement("instance.$N()", beanMethod.getPreDestroyMethodName());
        }

        return methodBuilder.build();
    }

    private AnnotationSpec createSuppressWarningsAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .addMember("value", "$S", "rawtypes")
                .build();
    }

    private String getPackageName(String fullClassName) {
        // For inner classes (Outer$Inner), find the package by looking for $ first
        int dollarSign = fullClassName.lastIndexOf('$');
        if (dollarSign > 0) {
            // It's an inner class, find the last dot before the $
            int lastDot = fullClassName.lastIndexOf('.', dollarSign - 1);
            return lastDot > 0 ? fullClassName.substring(0, lastDot) : null;
        }
        // Regular class - find the last dot
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(0, lastDot) : null;
    }

    private String getSimpleName(String fullClassName) {
        // For inner classes (Outer$Inner), get just the inner class name
        int lastDollar = fullClassName.lastIndexOf('$');
        if (lastDollar >= 0) {
            return fullClassName.substring(lastDollar + 1);
        }
        // For regular classes, get the class name after the last dot
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }

    /**
     * Creates a ClassName from a fully qualified class name string.
     * This method doesn't require the class to exist on the classpath.
     *
     * @param fullClassName the fully qualified class name (e.g., "com.example.MyClass" or "com.example.Outer$Inner")
     * @return a ClassName representing the class
     */
    private ClassName getClassName(String fullClassName) {
        String packageName = getPackageName(fullClassName);
        String simpleName = getSimpleName(fullClassName);
        if (packageName == null || packageName.isEmpty()) {
            return ClassName.bestGuess(simpleName);
        }
        return ClassName.get(packageName, simpleName);
    }
}
