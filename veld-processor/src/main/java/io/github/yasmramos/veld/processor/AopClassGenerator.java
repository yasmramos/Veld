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

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Generates AOP wrapper classes at compile-time.
 * 
 * <p>For each component with intercepted methods, generates a subclass
 * that inlines the interception logic, eliminating runtime proxies.
 *
 * @author Veld Framework Team
 * @since 1.0.4
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
        "io.github.yasmramos.veld.annotation.Retry"
    );

    private final Filer filer;
    private final Messager messager;
    private final Elements elementUtils;
    private final Types typeUtils;
    
    // Maps original class to its AOP wrapper class name
    private final Map<String, String> aopClassMap = new HashMap<>();

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

        JavaFileObject sourceFile = filer.createSourceFile(aopClassName);
        try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
            // Package declaration
            if (!packageName.isEmpty()) {
                out.println("package " + packageName + ";");
                out.println();
            }

            // Imports
            out.println("import io.github.yasmramos.veld.aop.InvocationContext;");
            out.println("import io.github.yasmramos.veld.aop.JoinPoint;");
            out.println("import io.github.yasmramos.veld.aop.MethodInvocation;");
            out.println("import io.github.yasmramos.veld.runtime.async.AsyncExecutor;");
            out.println("import io.github.yasmramos.veld.runtime.async.SchedulerService;");
            out.println("import java.lang.reflect.Method;");
            out.println("import java.util.concurrent.CompletableFuture;");
            out.println("import java.util.concurrent.TimeUnit;");
            out.println();

            // Class declaration
            out.println("/**");
            out.println(" * AOP wrapper for {@link " + simpleClassName + "}.");
            out.println(" * Generated by Veld Framework - DO NOT EDIT.");
            out.println(" */");
            out.println("public class " + aopSimpleClassName + " extends " + simpleClassName + " {");
            out.println();

            // Generate interceptor fields
            generateInterceptorFields(out, typeElement);

            // Generate constructor
            boolean hasScheduled = hasScheduledMethods(typeElement);
            generateConstructor(out, aopSimpleClassName, typeElement, hasScheduled);

            // Generate scheduled tasks initializer
            if (hasScheduled) {
                generateScheduledInitializer(out, typeElement);
            }

            // Generate intercepted methods
            generateInterceptedMethods(out, typeElement, simpleClassName);

            out.println("}");
        }

        messager.printMessage(Diagnostic.Kind.NOTE, "Generated AOP class: " + aopClassName);
    }

    /**
     * Generates static fields for interceptors.
     */
    private void generateInterceptorFields(PrintWriter out, TypeElement typeElement) {
        Set<String> interceptorTypes = collectInterceptorTypes(typeElement);
        
        for (String interceptorType : interceptorTypes) {
            String fieldName = getInterceptorFieldName(interceptorType);
            out.println("    private static final " + interceptorType + " " + fieldName + " = new " + interceptorType + "();");
        }
        
        if (!interceptorTypes.isEmpty()) {
            out.println();
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
    private void generateConstructor(PrintWriter out, String aopSimpleClassName, TypeElement typeElement, boolean hasScheduled) {
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

            StringBuilder params = new StringBuilder();
            StringBuilder superArgs = new StringBuilder();
            
            List<? extends VariableElement> parameters = constructor.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                VariableElement param = parameters.get(i);
                if (i > 0) {
                    params.append(", ");
                    superArgs.append(", ");
                }
                params.append(param.asType().toString()).append(" ").append(param.getSimpleName());
                superArgs.append(param.getSimpleName());
            }

            out.println("    public " + aopSimpleClassName + "(" + params + ") {");
            out.println("        super(" + superArgs + ");");
            if (hasScheduled) {
                out.println("        initScheduledTasks();");
            }
            out.println("    }");
            out.println();
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
    private void generateScheduledInitializer(PrintWriter out, TypeElement typeElement) {
        out.println("    private void initScheduledTasks() {");
        out.println("        SchedulerService scheduler = SchedulerService.getInstance();");
        
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
            out.println("        Runnable task_" + methodName + " = () -> {");
            out.println("            try {");
            out.println("                this." + methodName + "();");
            out.println("            } catch (Exception e) {");
            out.println("                System.err.println(\"[Veld] Scheduled task failed: " + methodName + " - \" + e.getMessage());");
            out.println("            }");
            out.println("        };");
            
            if (!cron.isEmpty()) {
                // Cron-based scheduling
                out.println("        scheduler.scheduleCron(task_" + methodName + ", \"" + cron + "\", \"" + zone + "\");");
            } else if (!fixedRate.equals("-1") && Long.parseLong(fixedRate) > 0) {
                // Fixed rate scheduling
                out.println("        scheduler.scheduleAtFixedRate(task_" + methodName + ", " + initialDelay + "L, " + fixedRate + "L, TimeUnit.MILLISECONDS);");
            } else if (!fixedDelay.equals("-1") && Long.parseLong(fixedDelay) > 0) {
                // Fixed delay scheduling
                out.println("        scheduler.scheduleWithFixedDelay(task_" + methodName + ", " + initialDelay + "L, " + fixedDelay + "L, TimeUnit.MILLISECONDS);");
            }
        }
        
        out.println("    }");
        out.println();
    }

    /**
     * Generates intercepted method overrides.
     */
    private void generateInterceptedMethods(PrintWriter out, TypeElement typeElement, String simpleClassName) {
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
                generateAsyncMethod(out, method, simpleClassName);
                continue;
            }

            // Check for @Retry annotation
            if (hasAnnotation(method, "io.github.yasmramos.veld.annotation.Retry")) {
                generateRetryMethod(out, method, simpleClassName);
                continue;
            }

            // Get method-level interceptors
            Set<String> methodInterceptors = new LinkedHashSet<>(classLevelInterceptors);
            addInterceptorType(methodInterceptors, method);

            if (methodInterceptors.isEmpty()) {
                continue; // No interceptors for this method
            }

            generateInterceptedMethod(out, method, methodInterceptors, simpleClassName);
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
     * Generates an async method wrapper.
     */
    private void generateAsyncMethod(PrintWriter out, ExecutableElement method, String simpleClassName) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        String returnTypeName = returnType.toString();
        boolean isVoid = returnTypeName.equals("void");
        boolean isCompletableFuture = returnTypeName.startsWith("java.util.concurrent.CompletableFuture");

        // Get executor name from annotation
        String executorName = getAnnotationValue(method, "io.github.yasmramos.veld.annotation.Async", "value", "");

        // Build parameter list
        StringBuilder params = new StringBuilder();
        StringBuilder args = new StringBuilder();
        List<? extends VariableElement> parameters = method.getParameters();
        
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            if (i > 0) {
                params.append(", ");
                args.append(", ");
            }
            params.append(param.asType().toString()).append(" ").append(param.getSimpleName());
            args.append(param.getSimpleName());
        }

        // Generate method override
        out.println("    @Override");
        out.println("    public " + returnTypeName + " " + methodName + "(" + params + ") {");

        if (isVoid) {
            // Fire and forget
            out.println("        AsyncExecutor.getInstance().submit(() -> {");
            out.println("            try {");
            out.println("                super." + methodName + "(" + args + ");");
            out.println("            } catch (Exception e) {");
            out.println("                System.err.println(\"[Veld] Async method failed: " + methodName + " - \" + e.getMessage());");
            out.println("            }");
            out.println("        }" + (executorName.isEmpty() ? "" : ", \"" + executorName + "\"") + ");");
        } else if (isCompletableFuture) {
            // Return CompletableFuture
            out.println("        return AsyncExecutor.getInstance().submit(() -> {");
            out.println("            try {");
            out.println("                return super." + methodName + "(" + args + ").join();");
            out.println("            } catch (Exception e) {");
            out.println("                throw new RuntimeException(e);");
            out.println("            }");
            out.println("        }" + (executorName.isEmpty() ? "" : ", \"" + executorName + "\"") + ");");
        } else {
            // Other return types - wrap in CompletableFuture and block (not recommended)
            out.println("        try {");
            out.println("            return AsyncExecutor.getInstance().submit(() -> {");
            out.println("                try {");
            out.println("                    return super." + methodName + "(" + args + ");");
            out.println("                } catch (Exception e) {");
            out.println("                    throw new RuntimeException(e);");
            out.println("                }");
            out.println("            }" + (executorName.isEmpty() ? "" : ", \"" + executorName + "\"") + ").get();");
            out.println("        } catch (Exception e) {");
            out.println("            throw new RuntimeException(e);");
            out.println("        }");
        }

        out.println("    }");
        out.println();
    }

    /**
     * Generates a retry method wrapper.
     */
    private void generateRetryMethod(PrintWriter out, ExecutableElement method, String simpleClassName) {
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
        StringBuilder params = new StringBuilder();
        StringBuilder args = new StringBuilder();
        List<? extends VariableElement> parameters = method.getParameters();
        
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            if (i > 0) {
                params.append(", ");
                args.append(", ");
            }
            params.append(param.asType().toString()).append(" ").append(param.getSimpleName());
            args.append(param.getSimpleName());
        }

        // Build throws clause
        StringBuilder throwsClause = new StringBuilder();
        List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        if (!thrownTypes.isEmpty()) {
            throwsClause.append(" throws ");
            for (int i = 0; i < thrownTypes.size(); i++) {
                if (i > 0) throwsClause.append(", ");
                throwsClause.append(thrownTypes.get(i).toString());
            }
        }

        // Generate method override
        out.println("    @Override");
        out.println("    public " + returnTypeName + " " + methodName + "(" + params + ")" + throwsClause + " {");
        out.println("        int __maxAttempts__ = " + maxAttempts + ";");
        out.println("        long __delay__ = " + delay + "L;");
        out.println("        double __multiplier__ = " + multiplier + ";");
        out.println("        long __maxDelay__ = " + maxDelay + "L;");
        out.println("        Throwable __lastException__ = null;");
        out.println("        for (int __attempt__ = 1; __attempt__ <= __maxAttempts__; __attempt__++) {");
        out.println("            try {");
        if (isVoid) {
            out.println("                super." + methodName + "(" + args + ");");
            out.println("                return;");
        } else {
            out.println("                return super." + methodName + "(" + args + ");");
        }
        out.println("            } catch (Throwable __ex__) {");
        out.println("                __lastException__ = __ex__;");
        out.println("                if (__attempt__ < __maxAttempts__) {");
        out.println("                    System.err.println(\"[Veld] Retry \" + __attempt__ + \"/\" + __maxAttempts__ + \" for " + methodName + ": \" + __ex__.getMessage());");
        out.println("                    try { Thread.sleep(__delay__); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }");
        out.println("                    __delay__ = Math.min((long)(__delay__ * __multiplier__), __maxDelay__);");
        out.println("                }");
        out.println("            }");
        out.println("        }");
        out.println("        if (__lastException__ instanceof RuntimeException) throw (RuntimeException) __lastException__;");
        out.println("        throw new RuntimeException(\"Retry exhausted for " + methodName + "\", __lastException__);");
        out.println("    }");
        out.println();
    }

    /**
     * Generates a single intercepted method.
     */
    private void generateInterceptedMethod(PrintWriter out, ExecutableElement method, 
                                           Set<String> interceptors, String simpleClassName) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        String returnTypeName = returnType.toString();
        boolean isVoid = returnTypeName.equals("void");

        // Build parameter list
        StringBuilder params = new StringBuilder();
        StringBuilder args = new StringBuilder();
        List<? extends VariableElement> parameters = method.getParameters();
        
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            if (i > 0) {
                params.append(", ");
                args.append(", ");
            }
            params.append(param.asType().toString()).append(" ").append(param.getSimpleName());
            args.append(param.getSimpleName());
        }

        // Build throws clause
        StringBuilder throwsClause = new StringBuilder();
        List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        if (!thrownTypes.isEmpty()) {
            throwsClause.append(" throws ");
            for (int i = 0; i < thrownTypes.size(); i++) {
                if (i > 0) throwsClause.append(", ");
                throwsClause.append(thrownTypes.get(i).toString());
            }
        }

        // Generate method override
        out.println("    @Override");
        out.println("    public " + returnTypeName + " " + methodName + "(" + params + ")" + throwsClause + " {");

        // Generate interception code
        String resultVar = isVoid ? null : "__result__";
        
        // Before advice
        for (String interceptor : interceptors) {
            String fieldName = getInterceptorFieldName(interceptor);
            out.println("        " + fieldName + ".beforeMethod(\"" + methodName + "\", new Object[]{" + args + "});");
        }

        // Try block for around/after
        out.println("        try {");
        
        // Call super method
        if (isVoid) {
            out.println("            super." + methodName + "(" + args + ");");
        } else {
            out.println("            " + returnTypeName + " " + resultVar + " = super." + methodName + "(" + args + ");");
        }

        // After returning advice
        List<String> interceptorList = new ArrayList<>(interceptors);
        Collections.reverse(interceptorList);
        for (String interceptor : interceptorList) {
            String fieldName = getInterceptorFieldName(interceptor);
            if (isVoid) {
                out.println("            " + fieldName + ".afterMethod(\"" + methodName + "\", null);");
            } else {
                out.println("            " + fieldName + ".afterMethod(\"" + methodName + "\", " + resultVar + ");");
            }
        }

        if (!isVoid) {
            out.println("            return " + resultVar + ";");
        }

        // Catch block for after throwing
        out.println("        } catch (Throwable __ex__) {");
        for (String interceptor : interceptorList) {
            String fieldName = getInterceptorFieldName(interceptor);
            out.println("            " + fieldName + ".afterThrowing(\"" + methodName + "\", __ex__);");
        }
        out.println("            throw __ex__;");
        out.println("        }");

        out.println("    }");
        out.println();
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
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Gets the simple class name from a fully qualified class name.
     */
    private String getSimpleClassName(String className) {
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
