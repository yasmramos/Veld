/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.aop;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
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
import io.github.yasmramos.veld.aop.interceptor.LoggingInterceptor;
import io.github.yasmramos.veld.runtime.async.AsyncExecutor;
import io.github.yasmramos.veld.runtime.async.SchedulerService;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Generates AOP wrapper classes at compile-time.
 *
 * <p>For each component with intercepted methods, generates a subclass
 * that inlines the interception logic, eliminating runtime proxies.
 *
 * <p>This class implements {@link AopGenerator} interface to provide
 * AOP code generation as an SPI service. The implementation is discovered
 * at runtime via Java's {@link java.util.ServiceLoader} mechanism.</p>
 *
 * <p>This class works with {@link AopComponentNode} interface, allowing it
 * to be used by the SPI system without direct dependencies on the processor module.</p>
 *
 * @author Veld Framework Team
 * @since 1.0.3
 */
public class AopClassGenerator implements AopGenerator {

    private static final String AOP_SUFFIX = "$$Aop";

    // Supported interceptor annotations
    private static final Set<String> INTERCEPTOR_ANNOTATIONS = Set.of(
        "io.github.yasmramos.veld.aop.interceptor.Logged",
        "io.github.yasmramos.veld.aop.interceptor.Timed",
        "io.github.yasmramos.veld.aop.interceptor.Transactional",
        "io.github.yasmramos.veld.aop.interceptor.Validated",
        "io.github.yasmramos.veld.annotation.Around",
        "io.github.yasmramos.veld.annotation.Before",
        "io.github.yasmramos.veld.annotation.After",
        "io.github.yasmramos.veld.annotation.Async",
        "io.github.yasmramos.veld.annotation.Scheduled",
        "io.github.yasmramos.veld.annotation.Retry",
        "io.github.yasmramos.veld.annotation.RateLimiter",
        "io.github.yasmramos.veld.annotation.Timed",
        "io.github.yasmramos.veld.annotation.Valid"
    );

    private final AopGenerationContext context;
    private final Types typeUtils;

    // Maps original class to its AOP wrapper class name
    private final Map<String, String> aopClassMap = new HashMap<>();

    // Track generated AOP class names to avoid recreating files in different rounds
    // Using static field to persist across multiple instances of AopClassGenerator
    private static final Set<String> generatedAopClasses = Collections.synchronizedSet(new HashSet<>());

    /**
     * Default constructor for SPI instantiation.
     * 
     * <p>When used via SPI, the context will be provided via 
     * {@link #generateAopWrappers(List, AopGenerationContext)}.</p>
     */
    public AopClassGenerator() {
        this.context = null;
        this.typeUtils = null;
    }

    /**
     * Constructor with explicit context for direct instantiation.
     *
     * @param context the AOP generation context
     */
    public AopClassGenerator(AopGenerationContext context) {
        this.context = context;
        this.typeUtils = context.getTypeUtils();
    }

    /**
     * Generates AOP wrapper classes for components with intercepted methods.
     *
     * @param components the components to process
     * @return map of original class name to AOP wrapper class name
     */
    public Map<String, String> generateAopClasses(List<? extends AopComponentNode> components) {
        if (components == null) {
            return aopClassMap;
        }
        
        for (AopComponentNode component : components) {
            if (hasInterceptedMethods(component)) {
                String aopClassName = component.getClassName() + AOP_SUFFIX;

                // Skip if already generated in a previous round
                if (generatedAopClasses.contains(aopClassName)) {
                    continue;
                }

                // Mark as processing to avoid duplicates
                generatedAopClasses.add(aopClassName);

                try {
                    generateAopClass(component);
                } catch (IOException e) {
                    context.reportError(
                        "Failed to generate AOP class for " + component.getClassName() + ": " + e.getMessage(),
                        null);
                }
            }
        }
        return aopClassMap;
    }

    /**
     * Checks if a component has any intercepted methods.
     */
    private boolean hasInterceptedMethods(AopComponentNode component) {
        TypeMirror typeMirror = component.getTypeMirror();
        if (typeMirror == null) return false;
        if (typeUtils == null) return false;

        TypeElement typeElement = (TypeElement) typeUtils.asElement(typeMirror);
        if (typeElement == null) return false;

        // Check class-level annotations
        for (AnnotationMirror annotation : typeElement.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().toString();
            if (INTERCEPTOR_ANNOTATIONS.contains(annotationName)) {
                return true;
            }
        }

        // Check method-level annotations
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                for (AnnotationMirror annotation : enclosed.getAnnotationMirrors()) {
                    String annotationName = annotation.getAnnotationType().toString();
                    if (INTERCEPTOR_ANNOTATIONS.contains(annotationName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Generates an AOP wrapper class for a component.
     */
    private void generateAopClass(AopComponentNode component) throws IOException {
        TypeMirror typeMirror = component.getTypeMirror();
        TypeElement typeElement = (TypeElement) typeUtils.asElement(typeMirror);
        if (typeElement == null) {
            context.reportError("Cannot get TypeElement for component: " + component.getClassName(), null);
            return;
        }

        String originalClassName = component.getClassName();
        String packageName = component.getPackageName();
        String simpleClassName = component.getSimpleName();
        String aopClassName = originalClassName + AOP_SUFFIX;
        String aopSimpleClassName = simpleClassName + AOP_SUFFIX;

        aopClassMap.put(originalClassName, aopClassName);

        try {
            // Build the class using JavaPoet
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(aopSimpleClassName)
                    .superclass(ClassName.get(packageName, simpleClassName))
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc(
                            "AOP wrapper for {@link $T}.\n" +
                            "Generated by Veld Framework - DO NOT EDIT.\n",
                            ClassName.get(packageName, simpleClassName))
                    .addAnnotation(createGeneratedAnnotation());

            // Generate interceptor array - single array with all interceptors
            Set<String> interceptorTypes = collectInterceptorTypes(typeElement);
            boolean hasRealInterceptors = !interceptorTypes.isEmpty();
            generateInterceptorArray(classBuilder, interceptorTypes);

            // Generate helper methods for interceptor calls only if there are real interceptors
            if (hasRealInterceptors) {
                generateInterceptorHelperMethods(classBuilder);
            }

            // Generate async executor fields
            generateAsyncExecutorFields(classBuilder, typeElement);

            // Generate constructor
            boolean hasScheduled = hasScheduledMethods(typeElement);
            generateConstructor(classBuilder, aopSimpleClassName, typeElement, hasScheduled);

            // Generate scheduled tasks initializer
            if (hasScheduled) {
                generateScheduledInitializer(classBuilder, typeElement);
            }

            // Generate intercepted methods using helper methods
            generateInterceptedMethods(classBuilder, typeElement, simpleClassName, packageName, hasRealInterceptors);

            // Build JavaFile and write
            JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
            javaFile.writeTo(context.getFiler());
        } catch (javax.annotation.processing.FilerException e) {
            // File already exists (e.g., from previous processing round) - skip
            context.reportNote("AOP class already exists, skipping: " + aopClassName);
            return;
        }

        context.reportNote("Generated AOP class: " + aopClassName);
    }

    /**
     * Generates a static array containing all interceptors.
     * This replaces individual interceptor fields with a single array.
     */
    private void generateInterceptorArray(TypeSpec.Builder classBuilder, Set<String> interceptorTypes) {
        if (interceptorTypes.isEmpty()) {
            return;
        }

        // Build the initializer using CodeBlock to ensure proper import tracking
        CodeBlock.Builder initializerBuilder = CodeBlock.builder()
            .add("new $T[]{", ClassName.get("io.github.yasmramos.veld.aop", "CompileTimeInterceptor"))
            .add("\n");
        
        boolean first = true;
        for (String interceptorType : interceptorTypes) {
            if (!first) {
                initializerBuilder.add(",\n");
            }
            first = false;
            ClassName interceptorClassName = ClassName.bestGuess(interceptorType);
            initializerBuilder.add("    new $T()", interceptorClassName);
        }
        initializerBuilder.add("\n}");
        
        // Generate the field using the initializer
        classBuilder.addField(FieldSpec.builder(
                ArrayTypeName.of(ClassName.get("io.github.yasmramos.veld.aop", "CompileTimeInterceptor")),
                "__interceptors__")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Array of interceptors for AOP processing.\n")
                .initializer(initializerBuilder.build())
                .build());
    }

    /**
     * Gets simple interceptor class name from full qualified name.
     */
    private String getSimpleInterceptorName(String interceptorType) {
        return interceptorType.substring(interceptorType.lastIndexOf('.') + 1);
    }

    /**
     * Generates helper methods for calling interceptors in sequence.
     */
    private void generateInterceptorHelperMethods(TypeSpec.Builder classBuilder) {
        // __before method - use beginControlFlow for proper for loop syntax
        MethodSpec.Builder beforeMethod = MethodSpec.methodBuilder("__before")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "methodName")
                .addParameter(Object[].class, "args")
                .beginControlFlow("for ($T interceptor : __interceptors__)", ClassName.get("io.github.yasmramos.veld.aop", "CompileTimeInterceptor"))
                .addStatement("interceptor.beforeMethod(methodName, args)")
                .endControlFlow();

        // __after method - use beginControlFlow for proper for loop syntax
        MethodSpec.Builder afterMethod = MethodSpec.methodBuilder("__after")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "methodName")
                .addParameter(Object.class, "result")
                .beginControlFlow("for ($T interceptor : __interceptors__)", ClassName.get("io.github.yasmramos.veld.aop", "CompileTimeInterceptor"))
                .addStatement("interceptor.afterMethod(methodName, result)")
                .endControlFlow();

        // __afterThrowing method - use beginControlFlow for proper for loop syntax
        MethodSpec.Builder afterThrowingMethod = MethodSpec.methodBuilder("__afterThrowing")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "methodName")
                .addParameter(Throwable.class, "ex")
                .beginControlFlow("for ($T interceptor : __interceptors__)", ClassName.get("io.github.yasmramos.veld.aop", "CompileTimeInterceptor"))
                .addStatement("interceptor.afterThrowing(methodName, ex)")
                .endControlFlow();

        classBuilder.addMethod(beforeMethod.build());
        classBuilder.addMethod(afterMethod.build());
        classBuilder.addMethod(afterThrowingMethod.build());
    }

    /**
     * Generates static fields for interceptors (deprecated - use array instead).
     */
    private void generateInterceptorFields(TypeSpec.Builder classBuilder, Set<String> interceptorTypes) {
        for (String interceptorType : interceptorTypes) {
            String fieldName = getInterceptorFieldName(interceptorType);
            FieldSpec field = FieldSpec.builder(ClassName.bestGuess(interceptorType), fieldName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T()", ClassName.bestGuess(interceptorType))
                    .build();
            classBuilder.addField(field);
        }
    }

    /**
     * Generates static fields for async executor optimization.
     */
    private void generateAsyncExecutorFields(TypeSpec.Builder classBuilder, TypeElement typeElement) {
        // Collect all executor names used in @Async annotations
        Set<String> defaultExecutors = new LinkedHashSet<>();
        Map<String, String> customExecutors = new LinkedHashMap<>();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            if (!hasAnnotation(method, "io.github.yasmramos.veld.annotation.Async")) continue;

            String executorName = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Async", "value", "");

            if (executorName.isEmpty()) {
                defaultExecutors.add("default");
            } else {
                // Use short name for field to avoid conflicts
                String fieldName = "EXECUTOR_" + executorName.toUpperCase().replace("-", "_").replace(" ", "_");
                customExecutors.put(executorName, fieldName);
            }
        }

        // Generate default executor constant (constant folding)
        if (!defaultExecutors.isEmpty()) {
            FieldSpec defaultExecutorField = FieldSpec.builder(
                    ClassName.get(ExecutorService.class), "DEFAULT_EXECUTOR")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.getInstance().getDefaultExecutor()", AsyncExecutor.class)
                    .build();
            classBuilder.addField(defaultExecutorField);
        }

        // Generate ThreadLocal cache for custom executors
        for (Map.Entry<String, String> entry : customExecutors.entrySet()) {
            String executorName = entry.getKey();
            String fieldName = entry.getValue();

            FieldSpec customExecutorField = FieldSpec.builder(
                    ParameterizedTypeName.get(ClassName.get(ThreadLocal.class), ClassName.get(ExecutorService.class)),
                    fieldName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.withInitial(() -> $T.getInstance().getExecutor($S))",
                            ThreadLocal.class, AsyncExecutor.class, executorName)
                    .build();
            classBuilder.addField(customExecutorField);
        }
    }

    /**
     * Collects all interceptor types used in the class.
     */
    private Set<String> collectInterceptorTypes(TypeElement typeElement) {
        Set<String> types = new LinkedHashSet<>();

        // Check class-level annotations
        addInterceptorType(types, typeElement);

        // Check method-level annotations
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                addInterceptorType(types, enclosed);
            }
        }

        return types;
    }

    /**
     * Adds interceptor type based on annotations.
     */
    private void addInterceptorType(Set<String> types, Element element) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            String annotationName = annotation.getAnnotationType().toString();
            switch (annotationName) {
                // Old-style annotations
                case "io.github.yasmramos.veld.aop.interceptor.Logged":
                    types.add("io.github.yasmramos.veld.aop.interceptor.LoggingInterceptor");
                    break;
                case "io.github.yasmramos.veld.aop.interceptor.Timed":
                    types.add("io.github.yasmramos.veld.aop.interceptor.TimingInterceptor");
                    break;
                case "io.github.yasmramos.veld.aop.interceptor.Transactional":
                    types.add("io.github.yasmramos.veld.aop.interceptor.TransactionInterceptor");
                    break;
                case "io.github.yasmramos.veld.aop.interceptor.Validated":
                    types.add("io.github.yasmramos.veld.aop.interceptor.ValidationInterceptor");
                    break;
                // New-style annotations
                case "io.github.yasmramos.veld.annotation.Timed":
                    types.add("io.github.yasmramos.veld.aop.interceptor.TimingInterceptor");
                    break;
                case "io.github.yasmramos.veld.annotation.Valid":
                    types.add("io.github.yasmramos.veld.aop.interceptor.ValidationInterceptor");
                    break;
                case "io.github.yasmramos.veld.annotation.Logged":
                    types.add("io.github.yasmramos.veld.aop.interceptor.LoggingInterceptor");
                    break;
                case "io.github.yasmramos.veld.annotation.Transactional":
                    types.add("io.github.yasmramos.veld.aop.interceptor.TransactionInterceptor");
                    break;
            }
        }
    }

    /**
     * Generates constructor that calls super constructor.
     * Constructors are made PUBLIC so Veld.java can instantiate the AOP wrapper.
     */
    private void generateConstructor(TypeSpec.Builder classBuilder, String aopSimpleClassName,
                                     TypeElement typeElement, boolean hasScheduled) {
        // Find constructors
        List<ExecutableElement> constructors = new ArrayList<>();
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                constructors.add((ExecutableElement) enclosed);
            }
        }

        // Generate constructors matching parent
        for (ExecutableElement constructor : constructors) {
            if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
                continue; // Skip private constructors
            }

            List<ParameterSpec> params = new ArrayList<>();
            List<String> superArgs = new ArrayList<>();

            List<? extends VariableElement> parameters = constructor.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                VariableElement param = parameters.get(i);
                params.add(ParameterSpec.builder(ClassName.get(param.asType()), param.getSimpleName().toString()).build());
                superArgs.add(param.getSimpleName().toString());
            }

            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(params)
                    .addStatement("super($L)", String.join(", ", superArgs));

            if (hasScheduled) {
                constructorBuilder.addStatement("initScheduledTasks()");
            }

            classBuilder.addMethod(constructorBuilder.build());
        }
    }

    /**
     * Checks if the class has any @Scheduled methods.
     */
    private boolean hasScheduledMethods(TypeElement typeElement) {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                if (hasAnnotation(enclosed, "io.github.yasmramos.veld.annotation.Scheduled")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates the scheduled tasks initializer method.
     */
    private void generateScheduledInitializer(TypeSpec.Builder classBuilder, TypeElement typeElement) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("initScheduledTasks")
                .addModifiers(Modifier.PRIVATE)
                .addStatement("$T scheduler = $T.getInstance()", SchedulerService.class, SchedulerService.class);

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            if (!hasAnnotation(method, "io.github.yasmramos.veld.annotation.Scheduled")) continue;

            String methodName = method.getSimpleName().toString();
            String cron = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Scheduled", "cron", "");
            String fixedRate = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Scheduled", "fixedRate", "-1");
            String fixedDelay = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Scheduled", "fixedDelay", "-1");
            String initialDelay = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Scheduled", "initialDelay", "0");
            String zone = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Scheduled", "zone", "");

            // Generate Runnable with explicit control flow
            methodBuilder.addStatement("$T task_$N = () -> {", Runnable.class, methodName);
            methodBuilder.beginControlFlow("try");
            methodBuilder.addStatement("this.$N()", methodName);
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("catch ($T e)", Exception.class);
            methodBuilder.addStatement("$T.err.println(\"[Veld] Scheduled task failed: $N - \" + e.getMessage())", System.class, methodName);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("};");

            if (!cron.isEmpty()) {
                // Cron-based scheduling
                methodBuilder.addStatement("scheduler.scheduleCron(task_$N, $S, $S)", methodName, cron, zone);
            } else if (!fixedRate.equals("-1") && Long.parseLong(fixedRate) > 0) {
                // Fixed rate scheduling
                methodBuilder.addStatement("scheduler.scheduleAtFixedRate(task_$N, $NL, $NL, $T.MILLISECONDS)",
                        methodName, initialDelay, fixedRate, TimeUnit.class);
            } else if (!fixedDelay.equals("-1") && Long.parseLong(fixedDelay) > 0) {
                // Fixed delay scheduling
                methodBuilder.addStatement("scheduler.scheduleWithFixedDelay(task_$N, $NL, $NL, $T.MILLISECONDS)",
                        methodName, initialDelay, fixedDelay, TimeUnit.class);
            }
        }

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Generates intercepted method overrides.
     */
    private void generateInterceptedMethods(TypeSpec.Builder classBuilder, TypeElement typeElement,
                                            String simpleClassName, String packageName, boolean hasRealInterceptors) {
        // Get class-level interceptors
        Set<String> classLevelInterceptors = new LinkedHashSet<>();
        addInterceptorType(classLevelInterceptors, typeElement);

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;

            // Skip static, private, final methods
            Set<Modifier> modifiers = method.getModifiers();
            if (modifiers.contains(Modifier.STATIC) ||
                modifiers.contains(Modifier.PRIVATE) ||
                modifiers.contains(Modifier.FINAL)) {
                continue;
            }

            // Check for @Async annotation
            if (hasAnnotation(method, "io.github.yasmramos.veld.annotation.Async")) {
                generateAsyncMethod(classBuilder, method, simpleClassName);
                continue;
            }

            // Check for @Retry annotation
            if (hasAnnotation(method, "io.github.yasmramos.veld.annotation.Retry")) {
                generateRetryMethod(classBuilder, method, simpleClassName);
                continue;
            }

            // Check for @RateLimiter annotation
            if (hasAnnotation(method, "io.github.yasmramos.veld.annotation.RateLimiter")) {
                generateRateLimiterMethod(classBuilder, method, simpleClassName);
                continue;
            }

            // Get method-level interceptors
            Set<String> methodInterceptors = new LinkedHashSet<>(classLevelInterceptors);
            addInterceptorType(methodInterceptors, method);

            if (methodInterceptors.isEmpty()) {
                continue; // No interceptors for this method
            }

            generateInterceptedMethod(classBuilder, method, methodInterceptors, simpleClassName, hasRealInterceptors);
        }
    }

    /**
     * Checks if an element has a specific annotation.
     */
    private boolean hasAnnotation(Element element, String annotationName) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an annotation value from an element.
     */
    private String getAnnotationValue(Element element, String annotationName, String attributeName, String defaultValue) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().toString().equals(annotationName)) {
                for (var entry : annotation.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                        Object value = entry.getValue().getValue();
                        return value != null ? value.toString() : defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    /**
     * Generates an async method wrapper with constant folding optimization.
     */
    private void generateAsyncMethod(TypeSpec.Builder classBuilder, ExecutableElement method, String simpleClassName) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        String returnTypeName = returnType.toString();
        boolean isVoid = returnTypeName.equals("void");
        boolean isCompletableFuture = returnTypeName.startsWith("java.util.concurrent.CompletableFuture");

        // Get executor name from annotation
        String executorName = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Async", "value", "");

        // Determine which optimized path to use
        boolean useDefaultExecutor = executorName.isEmpty();
        String executorFieldName = useDefaultExecutor ? "DEFAULT_EXECUTOR" :
            "EXECUTOR_" + executorName.toUpperCase().replace("-", "_").replace(" ", "_");
        String executorAccess = useDefaultExecutor ? executorFieldName : executorFieldName + ".get()";

        // Build parameter list
        List<ParameterSpec> params = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<? extends VariableElement> parameters = method.getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            params.add(ParameterSpec.builder(ClassName.get(param.asType()), param.getSimpleName().toString()).build());
            args.add(param.getSimpleName().toString());
        }

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(ClassName.get(returnType));

        if (isVoid) {
            // Fire and forget with optimized executor
            methodBuilder.addStatement("$T.runAsync(() -> {", CompletableFuture.class);
            methodBuilder.beginControlFlow("try");
            methodBuilder.addStatement("super.$N($L)", methodName, String.join(", ", args));
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("catch ($T e)", Exception.class);
            methodBuilder.addStatement("$T.err.println(\"[Veld] Async method failed: $N - \" + e.getMessage())", System.class, methodName);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("}, $L)", executorAccess);
        } else if (isCompletableFuture) {
            // Return CompletableFuture with optimized executor
            methodBuilder.addStatement("return $T.supplyAsync(() -> {", CompletableFuture.class);
            methodBuilder.beginControlFlow("try");
            methodBuilder.addStatement("return super.$N($L).join()", methodName, String.join(", ", args));
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("catch ($T e)", Exception.class);
            methodBuilder.addStatement("throw new $T(e)", CompletionException.class);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("}, $L)", executorAccess);
        } else {
            // Other return types - wrap in CompletableFuture and block
            methodBuilder.addStatement("return $T.supplyAsync(() -> {", CompletableFuture.class);
            methodBuilder.beginControlFlow("try");
            methodBuilder.addStatement("return super.$N($L)", methodName, String.join(", ", args));
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("catch ($T e)", Exception.class);
            methodBuilder.addStatement("throw new $T(e)", CompletionException.class);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("}, $L).get()", executorAccess);
        }

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Generates a retry method wrapper.
     */
    private void generateRetryMethod(TypeSpec.Builder classBuilder, ExecutableElement method, String simpleClassName) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        String returnTypeName = returnType.toString();
        boolean isVoid = returnTypeName.equals("void");

        // Get annotation values
        String maxAttempts = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Retry", "maxAttempts", "3");
        String delay = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Retry", "delay", "1000");
        String multiplier = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Retry", "multiplier", "1.0");
        String maxDelay = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Retry", "maxDelay", "30000");

        // Build parameter list
        List<ParameterSpec> params = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<? extends VariableElement> parameters = method.getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            params.add(ParameterSpec.builder(TypeName.get(param.asType()), param.getSimpleName().toString()).build());
            args.add(param.getSimpleName().toString());
        }

        // Build throws clause
        List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        List<ClassName> thrownExceptions = new ArrayList<>();
        for (TypeMirror thrownType : thrownTypes) {
            thrownExceptions.add(ClassName.bestGuess(thrownType.toString()));
        }

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(TypeName.get(returnType));

        if (!thrownExceptions.isEmpty()) {
            for (ClassName thrownException : thrownExceptions) {
                methodBuilder.addException(thrownException);
            }
        }

        // Add retry variables
        methodBuilder.addStatement("int __maxAttempts__ = $L", maxAttempts);
        methodBuilder.addStatement("long __delay__ = $LL", delay);
        methodBuilder.addStatement("double __multiplier__ = $L", multiplier);
        methodBuilder.addStatement("long __maxDelay__ = $LL", maxDelay);
        methodBuilder.addStatement("$T __lastException__ = null", Throwable.class);

        // for loop with try-catch
        methodBuilder.beginControlFlow("for (int __attempt__ = 1; __attempt__ <= __maxAttempts__; __attempt__++)");
        methodBuilder.beginControlFlow("try");
        methodBuilder.addStatement(isVoid ? "super.$N($L);" : "return super.$N($L)", methodName, String.join(", ", args));
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("catch ($T __ex__)", Throwable.class);
        methodBuilder.addStatement("__lastException__ = __ex__");
        methodBuilder.beginControlFlow("if (__attempt__ < __maxAttempts__)");
        methodBuilder.addStatement("$T.err.println(\"[Veld] Retry \" + __attempt__ + \"/\" + __maxAttempts__ + \" for $N: \" + __ex__.getMessage())", System.class, methodName);
        methodBuilder.beginControlFlow("try");
        methodBuilder.addStatement("$T.sleep(__delay__)", Thread.class);
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("catch ($T ie)", InterruptedException.class);
        methodBuilder.addStatement("$T.currentThread().interrupt()", Thread.class);
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("__delay__ = $T.min((long)(__delay__ * __multiplier__), __maxDelay__)", Math.class);
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        // Final throw
        methodBuilder.addStatement("if (__lastException__ instanceof $T) throw ($T) __lastException__", RuntimeException.class, RuntimeException.class);
        methodBuilder.addStatement("throw new $T(\"Retry exhausted for $N\", __lastException__)", RuntimeException.class, methodName);

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Generates a rate-limited method wrapper.
     */
    private void generateRateLimiterMethod(TypeSpec.Builder classBuilder, ExecutableElement method, String simpleClassName) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        String returnTypeName = returnType.toString();
        boolean isVoid = returnTypeName.equals("void");

        // Get annotation values
        String permits = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.RateLimiter", "permits", "10");
        String period = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.RateLimiter", "period", "1000");
        String blocking = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.RateLimiter", "blocking", "true");
        String timeout = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.RateLimiter", "timeout", "5000");
        String key = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.RateLimiter", "key", "");

        String limiterKey = key.isEmpty() ? simpleClassName + "." + methodName : key;

        // Build parameter list
        List<ParameterSpec> params = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<? extends VariableElement> parameters = method.getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            params.add(ParameterSpec.builder(TypeName.get(param.asType()), param.getSimpleName().toString()).build());
            args.add(param.getSimpleName().toString());
        }

        // Build throws clause
        List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        List<ClassName> thrownExceptions = new ArrayList<>();
        for (TypeMirror thrownType : thrownTypes) {
            thrownExceptions.add(ClassName.bestGuess(thrownType.toString()));
        }

        ClassName rateLimiterServiceClass = ClassName.bestGuess("io.github.yasmramos.veld.runtime.ratelimit.RateLimiterService");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(TypeName.get(returnType));

        if (!thrownExceptions.isEmpty()) {
            for (ClassName thrownException : thrownExceptions) {
                methodBuilder.addException(thrownException);
            }
        }

        ClassName rateLimitExceptionClass = ClassName.get("io.github.yasmramos.veld.runtime.ratelimit", "RateLimiterService$RateLimitExceededException");

        if (blocking.equals("true")) {
            methodBuilder
                    .addStatement("boolean __acquired__ = $T.getInstance().acquire($S, $L, $LL, $LL)",
                            rateLimiterServiceClass, limiterKey, permits, period, timeout)
                    .beginControlFlow("if (!__acquired__)")
                    .addStatement("throw new $T(\"Rate limit timeout for $N\")", rateLimitExceptionClass, methodName)
                    .endControlFlow();
        } else {
            methodBuilder
                    .addStatement("boolean __acquired__ = $T.getInstance().tryAcquire($S, $L, $LL)",
                            rateLimiterServiceClass, limiterKey, permits, period)
                    .beginControlFlow("if (!__acquired__)")
                    .addStatement("throw new $T(\"Rate limit exceeded for $N\")", rateLimitExceptionClass, methodName)
                    .endControlFlow();
        }

        methodBuilder.addStatement(isVoid ? "super.$N($L);" : "return super.$N($L)", methodName, String.join(", ", args));

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Generates a single intercepted method.
     */
    private void generateInterceptedMethod(TypeSpec.Builder classBuilder, ExecutableElement method,
                                           Set<String> interceptors, String simpleClassName, boolean hasRealInterceptors) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        String returnTypeName = returnType.toString();
        boolean isVoid = returnTypeName.equals("void");

        // Build parameter list
        List<ParameterSpec> params = new ArrayList<>();
        List<String> args = new ArrayList<>();
        List<? extends VariableElement> parameters = method.getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            params.add(ParameterSpec.builder(TypeName.get(param.asType()), param.getSimpleName().toString()).build());
            args.add(param.getSimpleName().toString());
        }

        // Build throws clause
        List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        List<ClassName> thrownExceptions = new ArrayList<>();
        for (TypeMirror thrownType : thrownTypes) {
            thrownExceptions.add(ClassName.bestGuess(thrownType.toString()));
        }

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(TypeName.get(returnType));

        if (!thrownExceptions.isEmpty()) {
            for (ClassName thrownException : thrownExceptions) {
                methodBuilder.addException(thrownException);
            }
        }

        // Generate interception code using helper methods
        String resultVar = isVoid ? null : "__result__";

        // Before advice - call __before helper (iterates through all interceptors) only if there are real interceptors
        if (hasRealInterceptors) {
            methodBuilder.addStatement("__before($S, new Object[]{$L})", methodName, String.join(", ", args));
        }

        // Try block for around/after
        methodBuilder.beginControlFlow("try");

        // Call super method
        if (isVoid) {
            methodBuilder.addStatement("super.$N($L)", methodName, String.join(", ", args));
        } else {
            methodBuilder.addStatement("$T $N = super.$N($L)", TypeName.get(returnType), resultVar, methodName, String.join(", ", args));
        }

        // After returning advice - call __after helper (already iterates in correct order) only if there are real interceptors
        if (hasRealInterceptors) {
            if (isVoid) {
                methodBuilder.addStatement("__after($S, null)", methodName);
            } else {
                methodBuilder.addStatement("__after($S, $N)", methodName, resultVar);
            }
        }

        if (!isVoid) {
            methodBuilder.addStatement("return $N", resultVar);
        }

        // Catch block for after throwing - call __afterThrowing helper only if there are real interceptors
        methodBuilder.endControlFlow()
                .beginControlFlow("catch ($T __ex__)", Throwable.class);
        if (hasRealInterceptors) {
            methodBuilder.addStatement("__afterThrowing($S, __ex__)", methodName);
        }
        methodBuilder.addStatement("throw __ex__")
                .endControlFlow();

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Gets the field name for an interceptor.
     */
    private String getInterceptorFieldName(String interceptorType) {
        String simpleName = interceptorType.substring(interceptorType.lastIndexOf('.') + 1);
        return "__" + Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1) + "__";
    }

    /**
     * Creates the @Generated annotation for the generated AOP class.
     */
    private AnnotationSpec createGeneratedAnnotation() {
        return AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
                .addMember("value", "$S", "io.github.yasmramos.veld.processor.VeldProcessor")
                .addMember("date", "$S", java.time.Instant.now().toString())
                .build();
    }

    /**
     * Returns the AOP class name for a component, or null if no AOP wrapper.
     */
    public String getAopClassName(String originalClassName) {
        return aopClassMap.get(originalClassName);
    }

    /**
     * Returns true if the component has an AOP wrapper.
     */
    public boolean hasAopWrapper(String originalClassName) {
        return aopClassMap.containsKey(originalClassName);
    }

    /**
     * Generates AOP wrapper classes implementing {@link AopGenerator} interface.
     *
     * <p>This method serves as the SPI entry point for AOP code generation.
     * It creates a new generator instance with the provided context and
     * delegates to {@link #generateAopClasses(List)}.</p>
     *
     * @param components the components to process
     * @param context the generation context (used for type utilities, filer, and logging)
     * @return map of original class name to AOP wrapper class name
     */
    @Override
    public Map<String, String> generateAopWrappers(
            List<? extends AopComponentNode> components,
            AopGenerationContext context) {
        if (components == null || components.isEmpty()) {
            return Map.of();
        }
        // Create a new generator instance with the provided context
        AopClassGenerator generator = new AopClassGenerator(context);
        return generator.generateAopClasses((List<AopComponentNode>) components);
    }
}
