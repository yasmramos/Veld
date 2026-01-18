package io.github.yasmramos.veld.processor;
import com.squareup.javapoet.*;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Generates Veld.java with revolutionary static dependency graph.
 *
 * <p>This generator creates a pure static DI container with lifecycle support:</p>
 * <ul>
 *   <li>No if statements for dependency resolution</li>
 *   <li>No null values (all dependencies resolved at compile-time)</li>
 *   <li>No runtime reflection for component resolution</li>
 *   <li>@PostConstruct invoked immediately after singleton initialization</li>
 *   <li>@PreDestroy invoked in shutdown() method (reverse dependency order)</li>
 * </ul>
 */
public final class VeldSourceGenerator {
    private final List<VeldNode> nodes;
    private final Map<String, VeldNode> nodeMap;
    private final Messager messager;
    private final String veldClassName;
    private final ClassName veldClass;
    private final Map<String, Integer> levelCache = new HashMap<>();
    
    private static final Map<String, String> SECTION_COMMENTS = new LinkedHashMap<>();
    static {
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.infrastructure", "// ===== Infrastructure =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.config", "// ===== Configuration =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.persistence", "// ===== Persistence =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.data", "// ===== Data Access =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.repository", "// ===== Repositories =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.domain", "// ===== Domain =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.model", "// ===== Domain Models =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.service", "// ===== Services =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.application", "// ===== Application =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.usecase", "// ===== Use Cases =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.facade", "// ===== Facades =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.ui", "// ===== UI =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.web", "// ===== Web =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.controller", "// ===== Controllers =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.integration", "// ===== Integration =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.external", "// ===== External Services =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.adapter", "// ===== Adapters =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.event", "// ===== Events =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.handler", "// ===== Event Handlers =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.util", "// ===== Utilities =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.helper", "// ===== Helpers =====");
    }
    
    public VeldSourceGenerator(List<VeldNode> nodes, Messager messager, String veldPackageName, String className) {
        this.nodes = nodes;
        this.messager = messager;
        this.veldClassName = className;
        this.veldClass = ClassName.get(veldPackageName, veldClassName);
        this.nodeMap = buildNodeMap();
    }
    
    private Map<String, VeldNode> buildNodeMap() {
        Map<String, VeldNode> map = new HashMap<>();
        for (VeldNode node : nodes) {
            map.put(node.getClassName(), node);
        }
        return map;
    }
    
    public JavaFile generate(String packageName) {
        List<DependencyError> errors = validateDependencies();
        if (!errors.isEmpty()) {
            for (DependencyError error : errors) {
                error(error.getMessage());
            }
            return null;
        }
        
        List<VeldNode> sortedNodes = topologicalSort();
        Map<String, Integer> dependencyLevels = calculateDependencyLevels(sortedNodes);
        
        List<VeldNode> singletons = new ArrayList<>();
        List<VeldNode> prototypes = new ArrayList<>();
        for (VeldNode node : sortedNodes) {
            if (node.isSingleton()) {
                singletons.add(node);
            } else {
                prototypes.add(node);
            }
        }
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(veldClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(buildClassJavadoc(singletons, prototypes))
                .addAnnotation(createGeneratedAnnotation())
                .addField(createLifecycleStateField())
                .addMethod(createPrivateConstructor());
        
        // Add lifecycle tracking field
        classBuilder.addField(FieldSpec.builder(
            ClassName.get("java.lang", "String"), "lifecycleComment")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "Lifecycle tracking: PostConstruct invoked during initialization, PreDestroy via shutdown()")
            .build());
        
        // Accumulate all initialization code in a single static block for cleaner output
        CodeBlock.Builder staticInitBuilder = CodeBlock.builder();
        
        // Generate fields and accumulate initialization code
        for (VeldNode node : sortedNodes) {
            if (node.isSingleton()) {
                addSingletonFieldWithLifecycle(classBuilder, node, staticInitBuilder);
            }
        }
        
        // Add the single static initialization block
        classBuilder.addStaticBlock(staticInitBuilder.build());
        
        // Generate accessor methods
        for (VeldNode node : singletons) {
            addSingletonAccessor(classBuilder, node);
        }
        
        // Generate prototype methods
        for (VeldNode node : prototypes) {
            addPrototypeComponent(classBuilder, node);
        }
        
        // Generate shutdown method
        addShutdownMethod(classBuilder, singletons);
        
        // Generate lifecycle state getter
        addLifecycleStateGetter(classBuilder);
        
        return JavaFile.builder(packageName, classBuilder.build()).build();
    }

    /**
     * Generates all accessor classes needed for private field/method injection.
     * Each accessor class is placed in the same package as its target component.
     */
    public Map<String, JavaFile> generateAccessorClasses() {
        Map<String, JavaFile> accessorFiles = new LinkedHashMap<>();

        for (VeldNode node : nodes) {
            if (!node.needsAccessorClass()) {
                continue;
            }

            JavaFile accessorFile = generateAccessorClass(node);
            if (accessorFile != null) {
                accessorFiles.put(node.getAccessorClassName(), accessorFile);
            }
        }

        return accessorFiles;
    }

    /**
     * Generates an accessor class for a single component.
     * The accessor class is public so Veld can access from different package.
     * Generates accessors for all non-public fields (private and package-private).
     */
    private JavaFile generateAccessorClass(VeldNode node) {
        TypeSpec.Builder accessorBuilder = TypeSpec.classBuilder(node.getAccessorSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(buildAccessorJavadoc(node))
                .addAnnotation(createGeneratedAnnotation());

        // Collect all non-public field injections (private and package-private)
        List<VeldNode.FieldInjection> nonPublicFieldInjections = new ArrayList<>();
        for (VeldNode.FieldInjection field : node.getFieldInjections()) {
            if (!field.isPublic() && !field.isValueInjection()) {
                nonPublicFieldInjections.add(field);
            }
        }

        // Generate consolidated inject() method for all non-public fields
        if (!nonPublicFieldInjections.isEmpty()) {
            addConsolidatedInjectMethod(accessorBuilder, node, nonPublicFieldInjections);
        }

        // Generate individual field injection methods (for flexibility)
        for (VeldNode.FieldInjection field : node.getFieldInjections()) {
            if (!field.isPublic() && !field.isValueInjection()) {
                addFieldInjectionMethod(accessorBuilder, node, field);
            }
        }

        // Generate method injection methods for private methods
        for (VeldNode.MethodInjection method : node.getMethodInjections()) {
            if (method.isPrivate()) {
                addMethodInjectionMethod(accessorBuilder, node, method);
            }
        }

        // Generate postConstruct method if needed
        if (node.hasPostConstruct()) {
            addPostConstructMethod(accessorBuilder, node);
        }

        // Generate preDestroy method if needed
        if (node.hasPreDestroy()) {
            addPreDestroyMethod(accessorBuilder, node);
        }

        return JavaFile.builder(node.getPackageName(), accessorBuilder.build()).build();
    }

    /**
     * Adds a consolidated inject() method that injects all non-public fields at once using reflection.
     * Non-public includes private and package-private fields.
     */
    private void addConsolidatedInjectMethod(TypeSpec.Builder accessorBuilder, VeldNode node,
                                              List<VeldNode.FieldInjection> nonPublicFields) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        ClassName fieldClass = ClassName.get("java.lang.reflect", "Field");
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance");

        // Add parameters for each non-public field injection
        for (VeldNode.FieldInjection field : nonPublicFields) {
            TypeName fieldType = getTypeName(field.getActualTypeName());
            String fieldName = field.getFieldName();
            methodBuilder.addParameter(fieldType, fieldName);
        }

        // Generate reflection-based field injection code
        methodBuilder.addCode("// Inject non-public fields using reflection\n");
        for (VeldNode.FieldInjection field : nonPublicFields) {
            String fieldName = field.getFieldName();
            methodBuilder.addCode("try {\n");
            methodBuilder.addCode("    $T $NField = $N.class.getDeclaredField(\"$N\");\n",
                    fieldClass, fieldName, componentType.simpleName(), fieldName);
            methodBuilder.addCode("    $NField.setAccessible(true);\n", fieldName);
            methodBuilder.addCode("    $NField.set($N, $N);\n", fieldName, "instance", fieldName);
            methodBuilder.addCode("} catch ($T e) {\n", ClassName.get("java.lang", "Exception"));
            methodBuilder.addCode("    throw new $T(\"Failed to inject field: $N\", e);\n",
                    ClassName.get("java.lang", "RuntimeException"), fieldName);
            methodBuilder.addCode("}\n");
        }

        methodBuilder.addJavadoc("Injects all non-public fields into $N using reflection.\n", node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Adds a postConstruct method to invoke @PostConstruct lifecycle method using reflection.
     */
    private void addPostConstructMethod(TypeSpec.Builder accessorBuilder, VeldNode node) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("postConstruct")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance")
                .addCode("try {\n")
                .addCode("    $T postConstructMethod = $N.class.getDeclaredMethod(\"$N\");\n",
                        methodClass, componentType.simpleName(), node.getPostConstructMethod())
                .addCode("    postConstructMethod.setAccessible(true);\n")
                .addCode("    postConstructMethod.invoke($N);\n", "instance")
                .addCode("} catch ($T e) {\n", ClassName.get("java.lang", "Exception"))
                .addCode("    throw new $T(\"Failed to invoke @PostConstruct method: $N\", e);\n",
                        ClassName.get("java.lang", "RuntimeException"), node.getPostConstructMethod())
                .addCode("}\n");

        methodBuilder.addJavadoc("Invokes @PostConstruct method $N on $N using reflection.\n",
                node.getPostConstructMethod(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Adds a preDestroy method to invoke @PreDestroy lifecycle method using reflection.
     */
    private void addPreDestroyMethod(TypeSpec.Builder accessorBuilder, VeldNode node) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("preDestroy")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance")
                .addCode("try {\n")
                .addCode("    $T preDestroyMethod = $N.class.getDeclaredMethod(\"$N\");\n",
                        methodClass, componentType.simpleName(), node.getPreDestroyMethod())
                .addCode("    preDestroyMethod.setAccessible(true);\n")
                .addCode("    preDestroyMethod.invoke($N);\n", "instance")
                .addCode("} catch ($T e) {\n", ClassName.get("java.lang", "Exception"))
                .addCode("    throw new $T(\"Failed to invoke @PreDestroy method: $N\", e);\n",
                        ClassName.get("java.lang", "RuntimeException"), node.getPreDestroyMethod())
                .addCode("}\n");

        methodBuilder.addJavadoc("Invokes @PreDestroy method $N on $N using reflection.\n",
                node.getPreDestroyMethod(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Adds a static method to inject a private field using reflection.
     * Format: injectFieldName(Component instance, Dependency value)
     */
    private void addFieldInjectionMethod(TypeSpec.Builder accessorBuilder, VeldNode node, VeldNode.FieldInjection field) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        TypeName dependencyType = getTypeName(field.getActualTypeName());
        ClassName fieldClass = ClassName.get("java.lang.reflect", "Field");

        String methodName = "inject" + capitalize(field.getFieldName());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance")
                .addParameter(dependencyType, "value")
                .addCode("try {\n")
                .addCode("    $T field = $N.class.getDeclaredField(\"$N\");\n",
                        fieldClass, componentType.simpleName(), field.getFieldName())
                .addCode("    field.setAccessible(true);\n")
                .addCode("    field.set($N, $N);\n", "instance", "value")
                .addCode("} catch ($T e) {\n", ClassName.get("java.lang", "Exception"))
                .addCode("    throw new $T(\"Failed to inject field: $N\", e);\n",
                        ClassName.get("java.lang", "RuntimeException"), field.getFieldName())
                .addCode("}\n");

        // Add javadoc
        methodBuilder.addJavadoc("Injects the private field $N into $N using reflection.\n",
                field.getFieldName(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Adds a static method to invoke a private method with parameters using reflection.
     */
    private void addMethodInjectionMethod(TypeSpec.Builder accessorBuilder, VeldNode node, VeldNode.MethodInjection method) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");
        ClassName objectClass = ClassName.get("java.lang", "Object");

        String methodName = "call" + capitalize(method.getMethodName());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance");

        // Build parameter types array for getDeclaredMethod
        methodBuilder.addCode("try {\n");
        methodBuilder.addCode("    $T[] paramTypes = new $T[] { ", objectClass, objectClass);
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) methodBuilder.addCode(", ");
            VeldNode.ParameterInfo param = method.getParameters().get(i);
            methodBuilder.addCode("$T.class", getTypeName(param.getActualTypeName()));
        }
        methodBuilder.addCode(" };\n");
        methodBuilder.addCode("    $T injectionMethod = $N.class.getDeclaredMethod(\"$N\", paramTypes);\n",
                methodClass, componentType.simpleName(), method.getMethodName());
        methodBuilder.addCode("    injectionMethod.setAccessible(true);\n");

        // Build invoke statement
        methodBuilder.addCode("    injectionMethod.invoke($N", "instance");
        for (VeldNode.ParameterInfo param : method.getParameters()) {
            String paramName = param.getParameterName();
            if (param.isOptionalWrapper()) {
                methodBuilder.addCode(", $T.ofNullable($N).orElse(null)",
                        ClassName.get("java.util", "Optional"), paramName);
            } else if (param.isProvider()) {
                methodBuilder.addCode(", $N", paramName);
            } else {
                methodBuilder.addCode(", $N", paramName);
            }
        }
        methodBuilder.addCode(");\n");
        methodBuilder.addCode("} catch ($T e) {\n", ClassName.get("java.lang", "Exception"));
        methodBuilder.addCode("    throw new $T(\"Failed to invoke method: $N\", e);\n",
                ClassName.get("java.lang", "RuntimeException"), method.getMethodName());
        methodBuilder.addCode("}\n");

        // Add parameters to method signature
        for (VeldNode.ParameterInfo param : method.getParameters()) {
            TypeName paramType = getTypeName(param.getActualTypeName());
            String paramName = param.getParameterName();
            methodBuilder.addParameter(paramType, paramName);
        }

        methodBuilder.addJavadoc("Invokes the private method $N on $N using reflection.\n",
                method.getMethodName(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

    private CodeBlock buildAccessorJavadoc(VeldNode node) {
        return CodeBlock.builder()
                .add("Accessor class for $N.\n\n", node.getSimpleName())
                .add("<p>This class provides package-private access to private members\n")
                .add("of $N for dependency injection purposes.</p>\n\n", node.getClassName())
                .add("<p><b>Generated by:</b> Veld annotation processor</p>\n")
                .build();
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Creates the @Generated annotation for the generated class.
     */
    private AnnotationSpec createGeneratedAnnotation() {
        return AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
                .addMember("value", "$S", "io.github.yasmramos.veld.processor.VeldProcessor")
                .addMember("date", "$S", java.time.Instant.now().toString())
                .build();
    }

    /**
     * Converts a type name to a TypeName, handling primitive types.
     * JavaPoet's ClassName.bestGuess() doesn't work with primitive types,
     * so we need to handle them separately using TypeName.get().
     */
    private TypeName getTypeName(String typeName) {
        switch (typeName) {
            case "int":
                return TypeName.get(int.class);
            case "boolean":
                return TypeName.get(boolean.class);
            case "long":
                return TypeName.get(long.class);
            case "double":
                return TypeName.get(double.class);
            case "float":
                return TypeName.get(float.class);
            case "char":
                return TypeName.get(char.class);
            case "byte":
                return TypeName.get(byte.class);
            case "short":
                return TypeName.get(short.class);
            default:
                return ClassName.bestGuess(typeName);
        }
    }
    
    private FieldSpec createLifecycleStateField() {
        return FieldSpec.builder(ClassName.get("java.util.concurrent.atomic", "AtomicBoolean"), "shutdownInitiated")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T(false)", ClassName.get("java.util.concurrent.atomic", "AtomicBoolean"))
                .build();
    }
    
    private void addSingletonFieldWithLifecycle(TypeSpec.Builder classBuilder, VeldNode node, 
                                                  CodeBlock.Builder staticInitBuilder) {
        String actualClassName = node.getActualClassName();
        ClassName type = ClassName.bestGuess(actualClassName);
        String fieldName = node.getVeldName();

        CodeBlock initialization = buildInstantiationCode(node);

        // Build field injection code using accessors for non-public fields
        CodeBlock fieldInjectionCode = buildFieldInjectionCode(node, fieldName, true);

        // Build method injection code using accessors for private methods
        CodeBlock methodInjectionCode = buildMethodInjectionCode(node, fieldName, true);

        // Build lifecycle code
        CodeBlock lifecycleCode = buildPostConstructInvocation(node, fieldName);

        // Add the field declaration
        FieldSpec field = FieldSpec.builder(type, fieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .build();

        classBuilder.addField(field);

        // Accumulate initialization code in the shared static block
        // Comment header for this component
        staticInitBuilder.add("// --- $N ---\n", fieldName);

        // First assign the field
        staticInitBuilder.addStatement("$N = $L", fieldName, initialization);

        // Add field injections using accessor for non-public fields
        if (!fieldInjectionCode.isEmpty()) {
            staticInitBuilder.add(fieldInjectionCode);
        }

        // Add method injections
        if (!methodInjectionCode.isEmpty()) {
            staticInitBuilder.add(methodInjectionCode);
        }

        // Add PostConstruct call
        if (node.hasPostConstruct()) {
            // Use accessor for private lifecycle, direct call for public
            if (node.hasPrivateFieldInjections() || node.hasPrivateMethodInjections()) {
                ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                staticInitBuilder.addStatement("$T.postConstruct($N)", accessorClass, fieldName);
            } else {
                staticInitBuilder.addStatement("$N.$N()", fieldName, node.getPostConstructMethod());
            }
        }
    }
    
    private CodeBlock buildPostConstructInvocation(VeldNode node, String fieldName) {
        if (!node.hasPostConstruct()) {
            return CodeBlock.of("");
        }
        return CodeBlock.of("$N.$N()", fieldName, node.getPostConstructMethod());
    }

    /**
     * Builds code for field injections.
     * Uses accessor class for private and package-private fields (anything not public).
     * Public fields can be accessed directly from Veld.java.
     * Skips @Value injections as they are handled by the weaver.
     */
    private CodeBlock buildFieldInjectionCode(VeldNode node, String fieldName, boolean isSingleton) {
        if (!node.hasFieldInjections()) {
            return CodeBlock.of("");
        }

        // Collect field injections by visibility
        List<VeldNode.FieldInjection> privateFields = new ArrayList<>();
        List<VeldNode.FieldInjection> packagePrivateFields = new ArrayList<>();
        List<VeldNode.FieldInjection> publicFields = new ArrayList<>();

        for (VeldNode.FieldInjection field : node.getFieldInjections()) {
            // Skip @Value injections - they are handled by the weaver
            if (field.isValueInjection()) {
                continue;
            }

            if (field.isPrivate()) {
                privateFields.add(field);
            } else if (field.isPublic()) {
                publicFields.add(field);
            } else {
                // Package-private fields also need accessor since Veld is in different package
                packagePrivateFields.add(field);
            }
        }

        CodeBlock.Builder builder = CodeBlock.builder();
        String instanceName = fieldName;

        // Generate individual injection calls for each non-public field
        // This respects topological order better than consolidated inject()
        ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
        
        for (VeldNode.FieldInjection field : privateFields) {
            String depAccess = getVeldAccessExpression(field.getActualTypeName());
            if (depAccess != null) {
                builder.addStatement("$T.inject$N($N, $N)", accessorClass, 
                    capitalize(field.getFieldName()), instanceName, depAccess);
            } else {
                builder.addStatement("$T.inject$N($N, null)", accessorClass, 
                    capitalize(field.getFieldName()), instanceName);
            }
        }
        
        for (VeldNode.FieldInjection field : packagePrivateFields) {
            String depAccess = getVeldAccessExpression(field.getActualTypeName());
            if (depAccess != null) {
                builder.addStatement("$T.inject$N($N, $N)", accessorClass, 
                    capitalize(field.getFieldName()), instanceName, depAccess);
            } else {
                builder.addStatement("$T.inject$N($N, null)", accessorClass, 
                    capitalize(field.getFieldName()), instanceName);
            }
        }

        // Direct assignment only for truly public fields
        // Use normalized access expression for beans
        for (VeldNode.FieldInjection field : publicFields) {
            String depAccess = getVeldAccessExpression(field.getActualTypeName());
            if (depAccess != null) {
                builder.addStatement("$N.$N = $N", instanceName, field.getFieldName(), depAccess);
            } else {
                builder.addStatement("$N.$N = null", instanceName, field.getFieldName());
            }
        }

        return builder.build();
    }

    /**
     * Builds code for method injections.
     * Uses accessor class for private methods, direct invocation for package-private/public methods.
     */
    private CodeBlock buildMethodInjectionCode(VeldNode node, String instanceName, boolean isSingleton) {
        if (!node.hasMethodInjections()) {
            return CodeBlock.of("");
        }

        CodeBlock.Builder builder = CodeBlock.builder();

        for (VeldNode.MethodInjection method : node.getMethodInjections()) {
            if (method.isPrivate()) {
                // Use accessor class for private methods
                ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                String accessorMethod = "call" + capitalize(method.getMethodName());

                if (method.getParameters().isEmpty()) {
                    builder.addStatement("$T.$N($N)", accessorClass, accessorMethod, instanceName);
                } else {
                    // Build parameter list
                    List<String> paramNames = method.getParameters().stream()
                            .map(p -> {
                                String name = getVeldNameForParameter(p);
                                return name != null ? name : "null";
                            })
                            .collect(Collectors.toList());
                    builder.addStatement("$T.$N($N, $L)", accessorClass, accessorMethod, instanceName,
                            String.join(", ", paramNames));
                }
            } else {
                // Direct invocation for package-private/public methods
                if (method.getParameters().isEmpty()) {
                    builder.addStatement("$N.$N()", instanceName, method.getMethodName());
                } else {
                    List<String> paramNames = method.getParameters().stream()
                            .map(p -> {
                                String name = getVeldNameForParameter(p);
                                return name != null ? name : "null";
                            })
                            .collect(Collectors.toList());
                    builder.addStatement("$N.$N($L)", instanceName, method.getMethodName(),
                            String.join(", ", paramNames));
                }
            }
        }

        return builder.build();
    }
    
    private void addShutdownMethod(TypeSpec.Builder classBuilder, List<VeldNode> singletons) {
        List<VeldNode> preDestroyNodes = new ArrayList<>();
        for (int i = singletons.size() - 1; i >= 0; i--) {
            VeldNode node = singletons.get(i);
            if (node.hasPreDestroy()) {
                preDestroyNodes.add(node);
            }
        }
        
        MethodSpec.Builder shutdownBuilder = MethodSpec.methodBuilder("shutdown")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Shuts down the Veld container and invokes @PreDestroy methods.\n\n" +
                            "<p>PreDestroy methods are called in reverse dependency order,\n" +
                            "ensuring that dependents are destroyed before their dependencies.</p>\n")
                .addStatement("$N.set(true)", "shutdownInitiated");
        
        if (preDestroyNodes.isEmpty()) {
            shutdownBuilder.addStatement("// No @PreDestroy methods to invoke");
        } else {
            shutdownBuilder.addStatement("// Invoke @PreDestroy methods in reverse dependency order");
            for (VeldNode node : preDestroyNodes) {
                shutdownBuilder.addStatement("// $S", node.getClassName());
                // Use accessor for private lifecycle, direct call for public
                if (node.hasPrivateFieldInjections() || node.hasPrivateMethodInjections()) {
                    ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                    shutdownBuilder.addStatement("$T.preDestroy($N)", accessorClass, node.getVeldName());
                } else {
                    shutdownBuilder.addStatement("$N.$N()", node.getVeldName(), node.getPreDestroyMethod());
                }
            }
        }
        
        classBuilder.addMethod(shutdownBuilder.build());
    }
    
    private void addLifecycleStateGetter(TypeSpec.Builder classBuilder) {
        classBuilder.addMethod(MethodSpec.methodBuilder("isShutdown")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addStatement("return $N.get()", "shutdownInitiated")
                .build());
    }
    
    private void addSingletonAccessor(TypeSpec.Builder classBuilder, VeldNode node) {
        ClassName returnType = ClassName.bestGuess(node.getActualClassName());
        String methodName = node.getVeldName();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType)
                .addStatement("return $N", methodName);
        
        StringBuilder javadoc = new StringBuilder("Returns the $N singleton instance.\n");
        if (node.hasPostConstruct()) {
            javadoc.append("\n<p><b>Lifecycle:</b> @PostConstruct has been invoked.</p>\n");
        }
        if (node.hasPreDestroy()) {
            javadoc.append("<p><b>Lifecycle:</b> @PreDestroy will be invoked on shutdown().</p>\n");
        }
        
        classBuilder.addMethod(methodBuilder.build());
    }
    
    private void addPrototypeComponent(TypeSpec.Builder classBuilder, VeldNode node) {
        ClassName returnType = ClassName.bestGuess(node.getActualClassName());
        String methodName = node.getVeldName();
        String instanceVar = methodName + "Instance";

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType);

        // Declare local variable for the instance
        methodBuilder.addStatement("$T $N = $L", returnType, instanceVar, buildInstantiationCode(node));

        // Add field injections using accessor if needed
        CodeBlock fieldInjectionCode = buildFieldInjectionCode(node, instanceVar, false);
        if (!fieldInjectionCode.isEmpty()) {
            methodBuilder.addCode(fieldInjectionCode);
        }

        // Add method injections using accessor if needed
        CodeBlock methodInjectionCode = buildMethodInjectionCode(node, instanceVar, false);
        if (!methodInjectionCode.isEmpty()) {
            methodBuilder.addCode(methodInjectionCode);
        }

        // Add @PostConstruct call
        if (node.hasPostConstruct()) {
            // Use accessor for private lifecycle, direct call for public
            if (node.hasPrivateFieldInjections() || node.hasPrivateMethodInjections()) {
                ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                methodBuilder.addStatement("$T.postConstruct($N)", accessorClass, instanceVar);
            } else {
                methodBuilder.addStatement("$N.$N()", instanceVar, node.getPostConstructMethod());
            }
        }

        // Return the instance
        methodBuilder.addStatement("return $N", instanceVar);

        classBuilder.addMethod(methodBuilder.build());
    }
    
    private CodeBlock buildInstantiationCode(VeldNode node) {
        ClassName type = ClassName.bestGuess(node.getActualClassName());

        if (!node.hasConstructorInjection()) {
            return CodeBlock.of("new $T()", type);
        }

        CodeBlock.Builder argsBuilder = CodeBlock.builder();
        VeldNode.ConstructorInfo ctor = node.getConstructorInfo();
        List<VeldNode.ParameterInfo> params = ctor.getParameters();

        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                argsBuilder.add(", ");
            }
            argsBuilder.add("$L", buildDependencyExpression(params.get(i)));
        }

        return CodeBlock.of("new $T($L)", type, argsBuilder.build());
    }
    
    private CodeBlock buildDependencyExpression(VeldNode.ParameterInfo param) {
        if (param.isValueInjection()) {
            return CodeBlock.of("null /* @Value */");
        }
        if (param.isOptionalWrapper()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                return CodeBlock.of("$T.ofNullable($N)", ClassName.get("java.util", "Optional"), depNode.getVeldName());
            }
            return CodeBlock.of("$T.empty()", ClassName.get("java.util", "Optional"));
        }
        if (param.isProvider()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                if (depNode.isPrototype()) {
                    // For prototype beans, instantiate directly in the lambda
                    String simpleClassName = depNode.getVeldName();
                    // Capitalize first letter to get class name
                    String className = simpleClassName.substring(0, 1).toUpperCase() + simpleClassName.substring(1);
                    return CodeBlock.of("() -> new $T()", ClassName.bestGuess(param.getActualTypeName()));
                }
                return CodeBlock.of("() -> $N", depNode.getVeldName());
            }
            return CodeBlock.of("() -> null");
        }
        
        VeldNode depNode = nodeMap.get(param.getActualTypeName());
        if (depNode != null) {
            return CodeBlock.of("$N", depNode.getVeldName());
        }
        return CodeBlock.of("null /* UNRESOLVED */");
    }
    
    private TypeElement getTypeElement(String typeName) {
        return null;
    }
    
    /**
     * Gets the Veld access expression for a given type.
     * Returns the correct expression based on bean scope:
     * - Singleton: field reference (e.g., "cacheService")
     * - Prototype: factory method call (e.g., "cache()")
     * Returns null if the bean is not available (e.g., filtered by profile).
     */
    private String getVeldAccessExpression(String typeName) {
        VeldNode depNode = nodeMap.get(typeName);
        if (depNode != null) {
            if (depNode.isSingleton()) {
                // Singleton: use field reference
                return depNode.getVeldName();
            } else {
                // Prototype: use factory method call
                return depNode.getVeldName() + "()";
            }
        }
        // Bean not available - return null to generate null reference
        return null;
    }
    
    /**
     * Gets the Veld name for a given type by looking it up in the node map.
     * Returns the veldName (e.g., "userService") for the given type class name.
     * Returns null if the bean is not available (e.g., filtered by profile).
     * @deprecated Use getVeldAccessExpression() for proper normalization
     */
    private String getVeldNameForType(String typeName) {
        VeldNode depNode = nodeMap.get(typeName);
        if (depNode != null) {
            return depNode.getVeldName();
        }
        // Bean not available - return null to generate null reference
        return null;
    }
    
    /**
     * Gets the Veld name for a parameter.
     * Uses the parameter's veld name if available, otherwise returns null.
     */
    private String getVeldNameForParameter(VeldNode.ParameterInfo param) {
        // Try to find the node for this parameter's type
        VeldNode depNode = nodeMap.get(param.getActualTypeName());
        if (depNode != null) {
            return depNode.getVeldName();
        }
        // Parameter bean not available - return null
        return null;
    }

    /**
     * Decapitalizes the first letter of a string (same as java.beans.Introspector.decapitalize).
     */
    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
    
    private List<VeldNode> topologicalSort() {
        List<VeldNode> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (VeldNode node : nodes) {
            if (!visited.contains(node.getClassName())) {
                visit(node, visited, visiting, sorted);
            }
        }
        return sorted;
    }
    
    private void visit(VeldNode node, Set<String> visited, Set<String> visiting, List<VeldNode> sorted) {
        if (visited.contains(node.getClassName())) return;
        if (visiting.contains(node.getClassName())) {
            note("Cycle detected: " + node.getClassName());
            return;
        }
        
        visiting.add(node.getClassName());
        
        // Visit constructor injection dependencies
        if (node.hasConstructorInjection()) {
            for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
                VeldNode depNode = nodeMap.get(param.getActualTypeName());
                if (depNode != null) {
                    visit(depNode, visited, visiting, sorted);
                }
            }
        }
        
        // Visit field injection dependencies (for ordering)
        if (node.hasFieldInjections()) {
            for (VeldNode.FieldInjection field : node.getFieldInjections()) {
                if (field.isValueInjection()) continue; // Skip @Value injections
                VeldNode depNode = nodeMap.get(field.getActualTypeName());
                if (depNode != null) {
                    visit(depNode, visited, visiting, sorted);
                }
            }
        }
        
        // Visit method injection dependencies (for ordering)
        if (node.hasMethodInjections()) {
            for (VeldNode.MethodInjection method : node.getMethodInjections()) {
                for (VeldNode.ParameterInfo param : method.getParameters()) {
                    if (param.isValueInjection() || param.isOptionalWrapper() || param.isProvider()) {
                        continue; // Skip @Value, Optional, Provider
                    }
                    VeldNode depNode = nodeMap.get(param.getActualTypeName());
                    if (depNode != null) {
                        visit(depNode, visited, visiting, sorted);
                    }
                }
            }
        }
        
        visiting.remove(node.getClassName());
        visited.add(node.getClassName());
        sorted.add(node);
    }
    
    private Map<String, Integer> calculateDependencyLevels(List<VeldNode> sortedNodes) {
        Map<String, Integer> levels = new HashMap<>();
        for (VeldNode node : sortedNodes) {
            levels.put(node.getClassName(), calculateNodeLevel(node, levels));
        }
        return levels;
    }
    
    private int calculateNodeLevel(VeldNode node, Map<String, Integer> levels) {
        if (!node.hasConstructorInjection()) return 0;
        int maxLevel = 0;
        for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                Integer level = levels.get(depNode.getClassName());
                if (level != null && level > maxLevel) {
                    maxLevel = level;
                }
            }
        }
        return maxLevel + 1;
    }
    
    private String getSectionForNode(VeldNode node) {
        String className = node.getClassName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            String packagePrefix = className.substring(0, lastDot);
            for (Map.Entry<String, String> entry : SECTION_COMMENTS.entrySet()) {
                if (packagePrefix.equals(entry.getKey()) || packagePrefix.startsWith(entry.getKey() + ".")) {
                    return entry.getValue();
                }
            }
            String lastPackage = packagePrefix.substring(packagePrefix.lastIndexOf('.') + 1);
            String capitalized = lastPackage.substring(0, 1).toUpperCase() + lastPackage.substring(1);
            return "// ===== " + capitalized + " =====";
        }
        return "// ===== Default =====";
    }
    
    private void addSingletonFieldJavadoc(FieldSpec.Builder fieldBuilder, VeldNode node) {
        int level = getDependencyLevel(node);
        StringBuilder javadoc = new StringBuilder();
        javadoc.append("Level: ").append(level).append("\n");
        javadoc.append("Type: ").append(node.getClassName());
        if (node.hasPostConstruct()) {
            javadoc.append("\n@PostConstruct: ").append(node.getPostConstructMethod());
        }
        if (node.hasPreDestroy()) {
            javadoc.append("\n@PreDestroy: ").append(node.getPreDestroyMethod());
        }
        fieldBuilder.addJavadoc("$L\n", javadoc.toString());
    }
    
    private CodeBlock buildClassJavadoc(List<VeldNode> singletons, List<VeldNode> prototypes) {
        return CodeBlock.builder()
                .add("Veld Static Dependency Injection Container.\n\n")
                .add("<p>Generated by Veld annotation processor.</p>\n\n")
                .add("<p><b>Components:</b></p>\n")
                .add("<ul>\n")
                .add("<li>Singletons: $L static fields</li>\n", singletons.size())
                .add("<li>Prototypes: $L factory methods</li>\n", prototypes.size())
                .add("</ul>\n\n")
                .add("<p><b>Lifecycle:</b></p>\n")
                .add("<ul>\n")
                .add("<li>@PostConstruct: invoked immediately after singleton initialization</li>\n")
                .add("<li>@PreDestroy: invoked in shutdown() method (reverse order)</li>\n")
                .add("</ul>\n")
                .build();
    }
    
    private int getDependencyLevel(VeldNode node) {
        return levelCache.getOrDefault(node.getClassName(), 0);
    }
    
    private MethodSpec createPrivateConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("throw new $T()", AssertionError.class)
                .build();
    }
    
    private List<DependencyError> validateDependencies() {
        List<DependencyError> errors = new ArrayList<>();
        for (VeldNode node : nodes) {
            if (node.hasConstructorInjection()) {
                VeldNode.ConstructorInfo ctor = node.getConstructorInfo();
                for (int i = 0; i < ctor.getParameters().size(); i++) {
                    VeldNode.ParameterInfo param = ctor.getParameters().get(i);
                    if (param.isValueInjection() || param.isOptionalWrapper() || param.isProvider()) {
                        continue;
                    }
                    if (nodeMap.get(param.getActualTypeName()) == null) {
                        errors.add(new DependencyError(
                            node.getClassName(), node.getVeldName(),
                            param.getTypeName(), param.getActualTypeName(), i));
                    }
                }
            }
        }
        return errors;
    }
    
    private void note(String message) {
        if (messager != null) {
            messager.printMessage(Diagnostic.Kind.NOTE, "[Veld] " + message);
        }
    }
    
    private void error(String message) {
        if (messager != null) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[Veld] " + message);
        }
    }
    
    private static class DependencyError {
        final String componentClass;
        final String componentName;
        final String paramType;
        final String actualType;
        final int paramPosition;
        
        DependencyError(String componentClass, String componentName,
                       String paramType, String actualType, int paramPosition) {
            this.componentClass = componentClass;
            this.componentName = componentName;
            this.paramType = paramType;
            this.actualType = actualType;
            this.paramPosition = paramPosition;
        }
        
        String getMessage() {
            return String.format(
                "Compilation failed.\n\nUnresolved dependency:\n" +
                "- Component: %s\n" +
                "- Parameter #%d: %s (%s)\n\n" +
                "Fix: Add @Component for %s or use @Named",
                componentName, paramPosition + 1, paramType, actualType, actualType);
        }
    }
}
