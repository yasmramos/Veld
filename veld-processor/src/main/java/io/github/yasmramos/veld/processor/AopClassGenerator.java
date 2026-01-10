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
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.JoinPoint;
import io.github.yasmramos.veld.aop.MethodInvocation;
import io.github.yasmramos.veld.aop.interceptor.LoggingInterceptor;
import io.github.yasmramos.veld.runtime.async.AsyncExecutor;
import io.github.yasmramos.veld.runtime.async.SchedulerService;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
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
 * @author Veld Framework Team
 * @since 1.0.3
 */
public class AopClassGenerator {

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
        "io.github.yasmramos.veld.annotation.RateLimiter"
    );

    private final Filer filer;
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;

    // Maps original class to its AOP wrapper class name
    private final Map<String, String> aopClassMap = new HashMap<>();

    // Track generated AOP class names to avoid recreating files in different rounds
    // Using static field to persist across multiple instances of AopClassGenerator
    private static final Set<String> generatedAopClasses = Collections.synchronizedSet(new HashSet<>());

    public AopClassGenerator(Filer filer, Messager messager, Elements elementUtils, Types typeUtils) {
        this.filer = filer;
        this.messager = messager;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    /**
     * Generates AOP wrapper classes for components with intercepted methods.
     *
     * @param components the components to process
     * @return map of original class name to AOP wrapper class name
     */
    public Map<String, String> generateAopClasses(List<ComponentInfo> components) {
        for (ComponentInfo component : components) {
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
                    messager.printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate AOP class for " + component.getClassName() + ": " + e.getMessage());
                }
            }
        }
        return aopClassMap;
    }

    /**
     * Checks if a component has any intercepted methods.
     */
    private boolean hasInterceptedMethods(ComponentInfo component) {
        TypeElement typeElement = component.getTypeElement();
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
    private void generateAopClass(ComponentInfo component) throws IOException {
        TypeElement typeElement = component.getTypeElement();
        String originalClassName = component.getClassName();
        String packageName = getPackageName(originalClassName);
        String simpleClassName = getSimpleClassName(originalClassName);
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
                            ClassName.get(packageName, simpleClassName));

            // Generate interceptor fields
            Set<String> interceptorTypes = collectInterceptorTypes(typeElement);
            generateInterceptorFields(classBuilder, interceptorTypes);

            // Generate async executor fields
            generateAsyncExecutorFields(classBuilder, typeElement);

            // Generate constructor
            boolean hasScheduled = hasScheduledMethods(typeElement);
            generateConstructor(classBuilder, aopSimpleClassName, typeElement, hasScheduled);

            // Generate scheduled tasks initializer
            if (hasScheduled) {
                generateScheduledInitializer(classBuilder, typeElement);
            }

            // Generate intercepted methods
            generateInterceptedMethods(classBuilder, typeElement, simpleClassName, packageName);

            // Build JavaFile and write
            JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
            javaFile.writeTo(filer);
        } catch (javax.annotation.processing.FilerException e) {
            // File already exists (e.g., from previous processing round) - skip
            messager.printMessage(Diagnostic.Kind.NOTE,
                "AOP class already exists, skipping: " + aopClassName);
            return;
        }

        messager.printMessage(Diagnostic.Kind.NOTE, "Generated AOP class: " + aopClassName);
    }

    /**
     * Generates static fields for interceptors.
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

        if (!interceptorTypes.isEmpty()) {
            classBuilder.addField(FieldSpec.builder(TypeName.VOID, "_dummy") // Placeholder for newline
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .build());
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
            }
        }
    }

    /**
     * Generates constructor that calls super constructor.
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

            // Generate Runnable
            methodBuilder.addStatement("$T task_$N = () -> {\n            try {\n                this.$N();\n            } catch ($T e) {\n                $T.err.println(\"[Veld] Scheduled task failed: $N - \" + e.getMessage());\n            }\n        }",
                    Runnable.class, methodName, methodName, Exception.class, System.class, methodName);

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
                                            String simpleClassName, String packageName) {
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

            generateInterceptedMethod(classBuilder, method, methodInterceptors, simpleClassName);
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
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(ClassName.get(returnType));

        if (isVoid) {
            // Fire and forget with optimized executor
            methodBuilder.addStatement("$T.runAsync(() -> {\n            try {\n                super.$N($L);\n            } catch ($T e) {\n                $T.err.println(\"[Veld] Async method failed: $N - \" + e.getMessage());\n            }\n        }, $L)",
                    CompletableFuture.class, methodName, String.join(", ", args), Exception.class, System.class, methodName, executorAccess);
        } else if (isCompletableFuture) {
            // Return CompletableFuture with optimized executor
            methodBuilder.addStatement("return $T.supplyAsync(() -> {\n            try {\n                return super.$N($L).join();\n            } catch ($T e) {\n                throw new $T(e);\n            }\n        }, $L)",
                    CompletableFuture.class, methodName, String.join(", ", args), Exception.class, CompletionException.class, executorAccess);
        } else {
            // Other return types - wrap in CompletableFuture and block
            methodBuilder.beginControlFlow("try")
                    .addStatement("return $T.supplyAsync(() -> {\n                try {\n                    return super.$N($L);\n                } catch ($T e) {\n                    throw new $T(e);\n                }\n            }, $L).get()",
                    CompletableFuture.class, methodName, String.join(", ", args), Exception.class, CompletionException.class, executorAccess)
                    .endControlFlow()
                    .beginControlFlow("catch ($T e)", Exception.class)
                    .addStatement("throw new $T(e)", RuntimeException.class)
                    .endControlFlow();
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
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(TypeName.get(returnType));

        if (!thrownExceptions.isEmpty()) {
            for (ClassName thrownException : thrownExceptions) {
                methodBuilder.addException(thrownException);
            }
        }

        methodBuilder
                .addStatement("int __maxAttempts__ = $L", maxAttempts)
                .addStatement("long __delay__ = $LL", delay)
                .addStatement("double __multiplier__ = $L", multiplier)
                .addStatement("long __maxDelay__ = $LL", maxDelay)
                .addStatement("$T __lastException__ = null", Throwable.class)
                .beginControlFlow("for (int __attempt__ = 1; __attempt__ <= __maxAttempts__; __attempt__++)")
                .beginControlFlow("try")
                .addStatement(isVoid ? "super.$N($L); return;" : "return super.$N($L)", methodName, String.join(", ", args))
                .endControlFlow()
                .beginControlFlow("catch ($T __ex__)", Throwable.class)
                .addStatement("__lastException__ = __ex__")
                .beginControlFlow("if (__attempt__ < __maxAttempts__)")
                .addStatement("$T.err.println(\"[Veld] Retry \" + __attempt__ + \"/\" + __maxAttempts__ + \" for $N: \" + __ex__.getMessage())", System.class, methodName)
                .addStatement("try { $T.sleep(__delay__); } catch ($T ie) { $T.currentThread().interrupt(); }", Thread.class, InterruptedException.class, Thread.class)
                .addStatement("__delay__ = $T.min((long)(__delay__ * __multiplier__), __maxDelay__)", Math.class)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("if (__lastException__ instanceof $T) throw ($T) __lastException__", RuntimeException.class, RuntimeException.class)
                .addStatement("throw new $T(\"Retry exhausted for $N\", __lastException__)", RuntimeException.class, methodName);

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
                                           Set<String> interceptors, String simpleClassName) {
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
                .addParameters(params)
                .addAnnotation(Override.class)
                .returns(TypeName.get(returnType));

        if (!thrownExceptions.isEmpty()) {
            for (ClassName thrownException : thrownExceptions) {
                methodBuilder.addException(thrownException);
            }
        }

        // Generate interception code
        String resultVar = isVoid ? null : "__result__";

        // Before advice
        for (String interceptor : interceptors) {
            String fieldName = getInterceptorFieldName(interceptor);
            methodBuilder.addStatement("$N.beforeMethod($S, new Object[]{$L})",
                    fieldName, methodName, String.join(", ", args));
        }

        // Try block for around/after
        methodBuilder.beginControlFlow("try");

        // Call super method
        if (isVoid) {
            methodBuilder.addStatement("super.$N($L)", methodName, String.join(", ", args));
        } else {
            methodBuilder.addStatement("$T $N = super.$N($L)", TypeName.get(returnType), resultVar, methodName, String.join(", ", args));
        }

        // After returning advice
        List<String> interceptorList = new ArrayList<>(interceptors);
        Collections.reverse(interceptorList);
        for (String interceptor : interceptorList) {
            String fieldName = getInterceptorFieldName(interceptor);
            if (isVoid) {
                methodBuilder.addStatement("$N.afterMethod($S, null)", fieldName, methodName);
            } else {
                methodBuilder.addStatement("$N.afterMethod($S, $N)", fieldName, methodName, resultVar);
            }
        }

        if (!isVoid) {
            methodBuilder.addStatement("return $N", resultVar);
        }

        // Catch block for after throwing
        methodBuilder.endControlFlow()
                .beginControlFlow("catch ($T __ex__)", Throwable.class);
        for (String interceptor : interceptorList) {
            String fieldName = getInterceptorFieldName(interceptor);
            methodBuilder.addStatement("$N.afterThrowing($S, __ex__)", fieldName, methodName);
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
     * Gets the package name from a fully qualified class name.
     */
    private String getPackageName(String className) {
        // For inner classes (Outer$Inner), find the package by looking for $ first
        int dollarSign = className.lastIndexOf('$');
        if (dollarSign > 0) {
            // It's an inner class, find the last dot before the $
            int lastDot = className.lastIndexOf('.', dollarSign - 1);
            return lastDot > 0 ? className.substring(0, lastDot) : "";
        }
        // Regular class - find the last dot
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Gets the simple class name from a fully qualified class name.
     */
    private String getSimpleClassName(String className) {
        // For inner classes (Outer$Inner), get just the inner class name
        int lastDollar = className.lastIndexOf('$');
        if (lastDollar >= 0) {
            return className.substring(lastDollar + 1);
        }
        // For regular classes, get the class name after the last dot
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
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
}
