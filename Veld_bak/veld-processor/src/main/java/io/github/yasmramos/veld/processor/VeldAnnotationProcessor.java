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

import io.github.yasmramos.veld.annotation.OnStart;
import io.github.yasmramos.veld.annotation.OnStop;
import io.github.yasmramos.veld.annotation.PostInitialize;
import io.github.yasmramos.veld.annotation.Subscribe;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation processor that generates zero-reflection registration code at compile time.
 *
 * <p>This processor scans for annotations like {@link Subscribe}, {@link OnStart},
 * {@link OnStop}, and {@link PostInitialize}, and generates a registration class
 * that can be used at runtime without reflection.</p>
 *
 * <h2>Generated Output</h2>
 * <p>For each annotated class, this processor generates a static registration
 * method that can be called from the application startup to register all callbacks
 * without using reflection.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Instead of:
 * eventBus.register(subscriber); // Uses reflection
 *
 * // Use generated registration:
 * VeldRegistry.registerSubscribers(eventBus);
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0
 */
@SupportedAnnotationTypes({
    "io.github.yasmramos.veld.annotation.Subscribe",
    "io.github.yasmramos.veld.annotation.OnStart",
    "io.github.yasmramos.veld.annotation.OnStop",
    "io.github.yasmramos.veld.annotation.PostInitialize"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class VeldAnnotationProcessor extends AbstractProcessor {

    public VeldAnnotationProcessor() {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }

        // Collect all annotated elements
        List<AnnotatedMethod> subscribers = new ArrayList<>();
        List<AnnotatedMethod> lifecycleCallbacks = new ArrayList<>();

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof ExecutableElement method) {
                    TypeElement classElement = (TypeElement) method.getEnclosingElement();

                    String annotationName = annotation.getQualifiedName().toString();

                    if (annotationName.endsWith(".Subscribe")) {
                        processSubscribeAnnotation(method, classElement, subscribers);
                    } else if (annotationName.endsWith(".OnStart") ||
                               annotationName.endsWith(".OnStop") ||
                               annotationName.endsWith(".PostInitialize")) {
                        processLifecycleAnnotation(method, classElement, lifecycleCallbacks);
                    }
                }
            }
        }

        // Generate registration classes
        if (!subscribers.isEmpty() || !lifecycleCallbacks.isEmpty()) {
            generateSubscriberRegistry(subscribers);
            generateLifecycleRegistry(lifecycleCallbacks);
        }

        return true;
    }

    private void processSubscribeAnnotation(ExecutableElement method, TypeElement classElement,
                                            List<AnnotatedMethod> subscribers) {
        Subscribe subscribe = method.getAnnotation(Subscribe.class);

        // Get parameter type
        if (method.getParameters().size() != 1) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "@Subscribe method must have exactly one parameter", method);
            return;
        }

        VariableElement param = method.getParameters().get(0);
        TypeMirror paramType = param.asType();

        if (paramType.getKind() != TypeKind.DECLARED) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "@Subscribe parameter must be an Event type", param);
            return;
        }

        String eventClass = paramType.toString();
        String methodName = method.getSimpleName().toString();
        String className = classElement.getQualifiedName().toString();
        String simpleClassName = classElement.getSimpleName().toString();
        String packageName = getPackageName(classElement);

        subscribers.add(new AnnotatedMethod(
            className,
            simpleClassName,
            packageName,
            methodName,
            eventClass,
            subscribe.async(),
            subscribe.priority(),
            subscribe.filter(),
            subscribe.catchExceptions()
        ));
    }

    private void processLifecycleAnnotation(ExecutableElement method, TypeElement classElement,
                                            List<AnnotatedMethod> callbacks) {
        String annotationName = null;
        int order = 0;

        if (method.getAnnotation(OnStart.class) != null) {
            annotationName = "OnStart";
            order = method.getAnnotation(OnStart.class).order();
        } else if (method.getAnnotation(OnStop.class) != null) {
            annotationName = "OnStop";
            order = method.getAnnotation(OnStop.class).order();
        } else if (method.getAnnotation(PostInitialize.class) != null) {
            annotationName = "PostInitialize";
            order = method.getAnnotation(PostInitialize.class).order();
        }

        if (annotationName != null) {
            String methodName = method.getSimpleName().toString();
            String className = classElement.getQualifiedName().toString();
            String simpleClassName = classElement.getSimpleName().toString();
            String packageName = getPackageName(classElement);

            callbacks.add(new AnnotatedMethod(
                className,
                simpleClassName,
                packageName,
                methodName,
                annotationName,
                false,
                order,
                "",
                false
            ));
        }
    }

    private void generateSubscriberRegistry(List<AnnotatedMethod> subscribers) {
        StringBuilder builder = new StringBuilder();

        builder.append("package io.github.yasmramos.veld.runtime.internal;\n\n");
        builder.append("import io.github.yasmramos.veld.runtime.event.*;\n");
        builder.append("import io.github.yasmramos.veld.runtime.lifecycle.*;\n\n");
        builder.append("/**\n");
        builder.append(" * Auto-generated registry for event subscribers.\n");
        builder.append(" * This class is generated by VeldAnnotationProcessor at compile time.\n");
        builder.append(" * Do not modify manually.\n");
        builder.append(" */\n");
        builder.append("public final class VeldSubscriberRegistry {\n\n");
        builder.append("    private VeldSubscriberRegistry() {}\n\n");
        builder.append("    /**\n");
        builder.append("     * Registers all subscribers to the given EventBus.\n");
        builder.append("     */\n");
        builder.append("    public static void register(EventBus eventBus) {\n");

        for (AnnotatedMethod method : subscribers) {
            builder.append("        register").append(method.simpleClassName)
                   .append("Subscriber(eventBus);\n");
        }

        builder.append("    }\n\n");

        for (AnnotatedMethod method : subscribers) {
            builder.append("    private static void register")
                   .append(method.simpleClassName)
                   .append("Subscriber(EventBus eventBus) {\n");
            builder.append("        // Class: ").append(method.className).append("\n");
            builder.append("        // Method: ").append(method.methodName).append("\n");
            builder.append("        // Event: ").append(method.eventClass).append("\n");
            builder.append("        // Priority: ").append(method.priority).append("\n");
            builder.append("        // Async: ").append(method.isAsync).append("\n");
            builder.append("        eventBus.registerEventHandler(0, ")
                   .append(method.eventClass.replace('$', '.'))
                   .append(".class, event -> {\n");
            builder.append("            // TODO: Call ").append(method.className)
                   .append(".").append(method.methodName).append("(event);\n");
            builder.append("        }, ").append(method.priority).append(");\n");
            builder.append("    }\n\n");
        }

        builder.append("}\n");

        try {
            JavaFileObject file = processingEnv.getFiler()
                .createSourceFile("io.github.yasmramos.veld.runtime.internal.VeldSubscriberRegistry");
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.print(builder.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate registry: " + e.getMessage());
        }
    }

    private void generateLifecycleRegistry(List<AnnotatedMethod> callbacks) {
        StringBuilder builder = new StringBuilder();

        builder.append("package io.github.yasmramos.veld.runtime.internal;\n\n");
        builder.append("import io.github.yasmramos.veld.runtime.lifecycle.*;\n\n");
        builder.append("/**\n");
        builder.append(" * Auto-generated registry for lifecycle callbacks.\n");
        builder.append(" * This class is generated by VeldAnnotationProcessor at compile time.\n");
        builder.append(" * Do not modify manually.\n");
        builder.append(" */\n");
        builder.append("public final class VeldLifecycleRegistry {\n\n");
        builder.append("    private VeldLifecycleRegistry() {}\n\n");
        builder.append("    /**\n");
        builder.append("     * Registers all lifecycle callbacks to the given processor.\n");
        builder.append("     */\n");
        builder.append("    public static void register(LifecycleProcessor processor) {\n");

        for (AnnotatedMethod method : callbacks) {
            builder.append("        register").append(method.simpleClassName)
                   .append("Callback(processor);\n");
        }

        builder.append("    }\n\n");

        for (AnnotatedMethod method : callbacks) {
            builder.append("    private static void register")
                   .append(method.simpleClassName)
                   .append("Callback(LifecycleProcessor processor) {\n");
            builder.append("        // Class: ").append(method.className).append("\n");
            builder.append("        // Method: ").append(method.methodName).append("\n");
            builder.append("        // Callback type: ").append(method.eventClass).append("\n");
            builder.append("        // Order: ").append(method.priority).append("\n");
            builder.append("        // TODO: processor.register").append(method.eventClass)
                   .append("(bean, beanName, method, ").append(method.priority).append(");\n");
            builder.append("    }\n\n");
        }

        builder.append("}\n");

        try {
            JavaFileObject file = processingEnv.getFiler()
                .createSourceFile("io.github.yasmramos.veld.runtime.internal.VeldLifecycleRegistry");
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.print(builder.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate lifecycle registry: " + e.getMessage());
        }
    }

    private String getPackageName(TypeElement classElement) {
        String fullName = classElement.getQualifiedName().toString();
        // For inner classes (Outer$Inner), find the package by looking for $ first
        int dollarSign = fullName.lastIndexOf('$');
        if (dollarSign > 0) {
            // It's an inner class, find the last dot before the $
            int lastDot = fullName.lastIndexOf('.', dollarSign - 1);
            return lastDot > 0 ? fullName.substring(0, lastDot) : "";
        }
        // Regular class - find the last dot
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot) : "";
    }

    private static class AnnotatedMethod {
        final String className;
        final String simpleClassName;
        final String packageName;
        final String methodName;
        final String eventClass;
        final boolean isAsync;
        final int priority;
        final String filter;
        final boolean catchExceptions;

        AnnotatedMethod(String className, String simpleClassName, String packageName,
                       String methodName, String eventClass, boolean isAsync,
                       int priority, String filter, boolean catchExceptions) {
            this.className = className;
            this.simpleClassName = simpleClassName;
            this.packageName = packageName;
            this.methodName = methodName;
            this.eventClass = eventClass;
            this.isAsync = isAsync;
            this.priority = priority;
            this.filter = filter;
            this.catchExceptions = catchExceptions;
        }
    }
}
