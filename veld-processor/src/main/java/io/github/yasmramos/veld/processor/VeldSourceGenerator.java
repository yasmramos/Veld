package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.*;
import io.github.yasmramos.veld.processor.condition.*;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Veld.java with a revolutionary static dependency graph.
 *
 * <p>This generator creates a pure static DI container with lifecycle support:</p>
 * <ul>
 *   <li>No if statements for dependency resolution</li>
 *   <li>No null values (all dependencies resolved at compile time)</li>
 *   <li>No runtime reflection for component resolution</li>
 *   <li>@PostConstruct invoked immediately after singleton initialization</li>
 *   <li>@PreDestroy invoked in shutdown() method (reverse dependency order)</li>
 *   <li>Conditions evaluated once during static initialization - deterministic decisions</li>
 * </ul>
 *
 * <p><b>Conditional Architecture:</b></p>
 * <ul>
 *   <li>Existence flags generated: <code>private static boolean HAS_BEAN_BeanName;</code></li>
 *   <li>Static block evaluates conditions and sets flags</li>
 *   <li>Conditional beans wrapped in <code>if (HAS_BEAN_Other) { ... }</code></li>
 *   <li>Dependencies injected as ternary expressions: <code>HAS_BEAN_Other ? other : null</code></li>
 * </ul>
 */
public final class VeldSourceGenerator {
    private final List<VeldNode> nodes;
    private final Map<String, VeldNode> nodeMap;
    private final Messager messager;
    private final String veldClassName;
    private final ClassName veldClass;
    private final Map<String, Integer> levelCache = new HashMap<>();
    private String lastSectionComment = "";
    
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
        this.lastSectionComment = "";
    }
    
    private Map<String, VeldNode> buildNodeMap() {
        Map<String, VeldNode> map = new HashMap<>();
        for (VeldNode node : nodes) {
            map.put(node.getClassName(), node);
        }
        return map;
    }
    
    /**
     * Generates the complete Veld.java file with full condition support.
     *
     * <p>Generation flow:</p>
     * <ol>
     *   <li>Validate dependencies and build bean graph</li>
     *   <li>Resolve graph to determine bean existence</li>
     *   <li>Create generation context</li>
     *   <li>Generate existence flag fields</li>
     *   <li>Generate static block with condition evaluation</li>
     *   <li>Generate bean fields and conditional initialization</li>
     *   <li>Generate accessor and factory methods</li>
     * </ol>
     */
    public JavaFile generate(String packageName) {
        // Basic dependency validation
        List<DependencyError> errors = validateDependencies();
        if (!errors.isEmpty()) {
            for (DependencyError error : errors) {
                error(error.getMessage());
            }
            return null;
        }
        
        // Basic topological sorting
        List<VeldNode> sortedNodes = topologicalSort();
        Map<String, Integer> dependencyLevels = calculateDependencyLevels(sortedNodes);
        
        // Separate singletons and prototypes
        List<VeldNode> singletons = new ArrayList<>();
        List<VeldNode> prototypes = new ArrayList<>();
        for (VeldNode node : sortedNodes) {
            if (node.isSingleton()) {
                singletons.add(node);
            } else {
                prototypes.add(node);
            }
        }
        
        // ===== BEAN EXISTENCE GRAPH CONSTRUCTION =====
        BeanExistenceGraph.Builder graphBuilder = BeanExistenceGraph.builder();
        
        // Add all nodes to the graph
        for (VeldNode node : singletons) {
            graphBuilder.addNode(node.getClassName(), node);
            
            // Convert ConditionInfo to ConditionExpression
            if (node.getConditionInfo() != null) {
                ConditionExpression condition = convertToConditionExpression(node.getConditionInfo());
                if (condition != null) {
                    graphBuilder.addCondition(node.getClassName(), condition);
                }
            }
        }
        
        BeanExistenceGraph graph = graphBuilder.build();
        
        // Resolve the graph
        BeanExistenceGraph.ResolutionResult resolutionResult = graph.resolve();
        
        // Validate the condition graph
        ConditionalValidator validator = new ConditionalValidator(messager);
        validator.validate(graph);
        
        // Create generation context from the result
        GenerationContext context = GenerationContext.fromResolutionResult(resolutionResult);
        
        // ===== CLASS CONSTRUCTION =====
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(veldClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(buildClassJavadoc(singletons, prototypes))
                .addAnnotation(createGeneratedAnnotation())
                .addField(createLifecycleStateField())
                .addMethod(createPrivateConstructor());
        
        // Lifecycle tracking field
        classBuilder.addField(FieldSpec.builder(
            ClassName.get("java.lang", "String"), "lifecycleComment")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", "Lifecycle tracking: PostConstruct invoked during static initialization, PreDestroy via shutdown()")
            .build());
        
        // ===== SECTION 1: EXISTENCE FLAG FIELDS =====
        addExistenceFlagFields(classBuilder, graph, resolutionResult);
        
        // ===== SECTION 2: PROPERTIES FIELD (if property conditions exist) =====
        boolean hasPropertyConditions = sortedNodes.stream()
            .anyMatch(node -> node.getConditionInfo() != null && 
                !node.getConditionInfo().getPropertyConditions().isEmpty());
        if (hasPropertyConditions) {
            addPropertyLoaderField(classBuilder);
        }
        
        // ===== SECTION 3: STATIC INITIALIZATION BLOCK =====
        CodeBlock.Builder staticInitBuilder = CodeBlock.builder();
        
        // Load properties if needed
        if (hasPropertyConditions) {
            addPropertyLoadingCode(staticInitBuilder);
        }
        
        // Generate condition evaluation and flag setup
        addConditionEvaluationAndFlagSetup(staticInitBuilder, graph, resolutionResult, context);
        
        // ===== SECTION 4: BEAN FIELDS AND INITIALIZATION =====
        for (VeldNode node : singletons) {
            addSingletonFieldWithLifecycle(classBuilder, node, staticInitBuilder, graph, resolutionResult, context);
        }
        
        // Add the single static block
        classBuilder.addStaticBlock(staticInitBuilder.build());
        
        // ===== SECTION 5: ACCESSOR METHODS =====
        for (VeldNode node : singletons) {
            addSingletonAccessor(classBuilder, node, resolutionResult);
        }
        
        // ===== SECTION 6: FACTORY METHODS FOR PROTOTYPES =====
        for (VeldNode node : prototypes) {
            addPrototypeComponent(classBuilder, node, graph, resolutionResult, context);
        }
        
        // ===== SECTION 7: SHUTDOWN METHOD =====
        addShutdownMethod(classBuilder, singletons);
        
        // ===== SECTION 8: LIFECYCLE STATE GETTER =====
        addLifecycleStateGetter(classBuilder);
        
        return JavaFile.builder(packageName, classBuilder.build()).build();
    }

    /**
     * Converts ConditionInfo to ConditionExpression.
     */
    private ConditionExpression convertToConditionExpression(ConditionInfo info) {
        if (info == null) {
            return null;
        }
        
        List<ConditionExpression> conditions = new ArrayList<>();
        
        // Property conditions
        for (ConditionInfo.PropertyConditionInfo propCond : info.getPropertyConditions()) {
            conditions.add(new PropertyCondition(propCond.name(), propCond.havingValue(), propCond.matchIfMissing()));
        }
        
        // Class conditions (always true if it compiles)
        for (ConditionInfo.ClassConditionInfo classCond : info.getClassConditions()) {
            for (String className : classCond.classNames()) {
                conditions.add(ClassCondition.of(className));
            }
        }
        
        // Present bean conditions
        for (ConditionInfo.PresentBeanConditionInfo beanCond : info.getPresentBeanConditions()) {
            Set<String> types = new LinkedHashSet<>(beanCond.beanTypes());
            Set<String> names = new LinkedHashSet<>(beanCond.beanNames());
            if (!types.isEmpty() || !names.isEmpty()) {
                conditions.add(new BeanPresenceCondition(types, names, true));
            }
        }
        
        // Missing bean conditions
        for (ConditionInfo.MissingBeanConditionInfo beanCond : info.getMissingBeanConditions()) {
            Set<String> types = new LinkedHashSet<>(beanCond.beanTypes());
            Set<String> names = new LinkedHashSet<>(beanCond.beanNames());
            if (!types.isEmpty() || !names.isEmpty()) {
                conditions.add(new BeanPresenceCondition(types, names, false));
            }
        }
        
        if (conditions.isEmpty()) {
            return null;
        }
        
        return CompositeCondition.andAll(conditions);
    }

    /**
     * Generates all bean existence flag fields.
     * Format: private static boolean HAS_BEAN_BeanName;
     */
    private void addExistenceFlagFields(TypeSpec.Builder classBuilder, 
                                          BeanExistenceGraph graph,
                                          BeanExistenceGraph.ResolutionResult result) {
        for (String beanClassName : result.getEvaluatedBeans()) {
            String flagName = graph.getExistenceFlagName(beanClassName);
            
            classBuilder.addField(FieldSpec.builder(
                TypeName.get(boolean.class), flagName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addJavadoc("Existence flag for: $L\n", beanClassName)
                .build());
        }
        
        note("Generated " + result.getEvaluatedBeans().size() + " bean existence flags");
    }
    
    /**
     * Generates condition evaluation code in the static block.
     */
    private void addConditionEvaluationAndFlagSetup(CodeBlock.Builder staticInitBuilder, 
                                                      BeanExistenceGraph graph,
                                                      BeanExistenceGraph.ResolutionResult result,
                                                      GenerationContext context) {
        staticInitBuilder.add("// ===== CONDITION EVALUATION AND FLAG SETUP =====\n");
        staticInitBuilder.add("// This block deterministically sets up the bean existence graph\n");
        staticInitBuilder.add("// Flags determine which beans are created and which are omitted\n\n");
        
        // Generate for all evaluated beans
        for (String beanClassName : result.getCreationOrder()) {
            VeldNode node = graph.getNode(beanClassName);
            if (node == null) continue;
            
            String flagName = graph.getExistenceFlagName(beanClassName);
            
            // Section comment if needed
            String sectionComment = getSectionForNode(node);
            if (!sectionComment.equals(lastSectionComment)) {
                staticInitBuilder.add("\n$L\n", sectionComment);
                lastSectionComment = sectionComment;
            }
            
            // Generate the flag for this bean
            staticInitBuilder.add("// Bean: $N\n", node.getVeldName());
            
            ConditionExpression condition = result.getCondition(beanClassName);
            if (condition != null) {
                // Conditional bean - evaluate conditions
                String conditionCode = condition.toJavaCode(context);
                staticInitBuilder.add("$N = $L;\n", flagName, conditionCode);
            } else {
                // Unconditional bean - always exists
                staticInitBuilder.add("$N = true; // Unconditional bean\n", flagName);
            }
            
            staticInitBuilder.add("\n");
        }
        
        staticInitBuilder.add("// ===== END OF CONDITION EVALUATION =====\n\n");
    }
    
    /**
     * Adds the property loader field if property conditions exist.
     */
    private void addPropertyLoaderField(TypeSpec.Builder classBuilder) {
        classBuilder.addField(FieldSpec.builder(
            ClassName.get("io.github.yasmramos.veld", "VeldProperties"), "properties")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addJavadoc("Property loader for @ConditionalOnProperty evaluation\n")
            .build());
    }
    
    /**
     * Generates code to load properties in the static initialization block.
     */
    private void addPropertyLoadingCode(CodeBlock.Builder staticInitBuilder) {
        staticInitBuilder.add("// Load properties for @ConditionalOnProperty evaluation\n");
        staticInitBuilder.add("properties = new $T();\n", 
            ClassName.get("io.github.yasmramos.veld", "VeldProperties"));
        staticInitBuilder.add("\n");
    }

    /**
     * Adds a singleton field with lifecycle support and conditional initialization.
     */
    private void addSingletonFieldWithLifecycle(TypeSpec.Builder classBuilder, VeldNode node, 
                                                  CodeBlock.Builder staticInitBuilder,
                                                  BeanExistenceGraph graph,
                                                  BeanExistenceGraph.ResolutionResult result,
                                                  GenerationContext context) {
        String actualClassName = node.getActualClassName();
        ClassName type = ClassName.bestGuess(actualClassName);
        String fieldName = node.getVeldName();
        String flagName = graph.getExistenceFlagName(node.getClassName());
        boolean exists = result.exists(node.getClassName());
        boolean isConditional = result.isConditional(node.getClassName());
        
        // Build instantiation code
        CodeBlock initialization = buildInstantiationCode(node, context);
        
        // Build field injection code using accessors
        CodeBlock fieldInjectionCode = buildFieldInjectionCode(node, fieldName, context);
        
        // Build method injection code using accessors
        CodeBlock methodInjectionCode = buildMethodInjectionCode(node, fieldName, context);
        
        // Build lifecycle code
        CodeBlock lifecycleCode = buildPostConstructInvocation(node, fieldName);
        
        // Add field declaration
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(type, fieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        
        if (isConditional) {
            // Conditional bean - start as null
            fieldBuilder.initializer("null");
            staticInitBuilder.add("// Conditional bean: $N\n", fieldName);
        } else {
            // Unconditional bean - final
            fieldBuilder.addModifiers(Modifier.FINAL);
        }
        
        classBuilder.addField(fieldBuilder.build());
        
        // Accumulate initialization code in the shared static block
        String sectionComment = getSectionForNode(node);
        if (!sectionComment.equals(lastSectionComment)) {
            staticInitBuilder.add("\n$L\n", sectionComment);
            lastSectionComment = sectionComment;
        }
        
        // Header comment for this component
        staticInitBuilder.add("// --- $N ($N) ---\n", fieldName, actualClassName);
        
        if (isConditional) {
            // Conditional bean - wrap instantiation in if
            staticInitBuilder.add("if ($N) {\n", flagName);
            staticInitBuilder.indent();
            
            // First assign the field
            staticInitBuilder.addStatement("$N = $L", fieldName, initialization);
            
            // Add field injections using accessor
            if (!fieldInjectionCode.isEmpty()) {
                staticInitBuilder.add(fieldInjectionCode);
            }
            
            // Add method injections
            if (!methodInjectionCode.isEmpty()) {
                staticInitBuilder.add(methodInjectionCode);
            }
            
            // Add PostConstruct call
            if (node.hasPostConstruct()) {
                if (node.hasPrivateFieldInjections() || node.hasPrivateMethodInjections()) {
                    ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                    staticInitBuilder.addStatement("$T.postConstruct($N)", accessorClass, fieldName);
                } else {
                    staticInitBuilder.addStatement("$N.$N()", fieldName, node.getPostConstructMethod());
                }
            }
            
            staticInitBuilder.unindent();
            staticInitBuilder.add("}\n");
        } else {
            // Unconditional bean - direct instantiation
            staticInitBuilder.addStatement("$N = $L", fieldName, initialization);
            
            // Add field injections
            if (!fieldInjectionCode.isEmpty()) {
                staticInitBuilder.add(fieldInjectionCode);
            }
            
            // Add method injections
            if (!methodInjectionCode.isEmpty()) {
                staticInitBuilder.add(methodInjectionCode);
            }
            
            // Add PostConstruct call
            if (node.hasPostConstruct()) {
                if (node.hasPrivateFieldInjections() || node.hasPrivateMethodInjections()) {
                    ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                    staticInitBuilder.addStatement("$T.postConstruct($N)", accessorClass, fieldName);
                } else {
                    staticInitBuilder.addStatement("$N.$N()", fieldName, node.getPostConstructMethod());
                }
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
     */
    private CodeBlock buildFieldInjectionCode(VeldNode node, String fieldName,
                                                GenerationContext context) {
        if (!node.hasFieldInjections()) {
            return CodeBlock.of("");
        }

        List<VeldNode.FieldInjection> privateFields = new ArrayList<>();
        List<VeldNode.FieldInjection> packagePrivateFields = new ArrayList<>();
        List<VeldNode.FieldInjection> publicFields = new ArrayList<>();

        for (VeldNode.FieldInjection field : node.getFieldInjections()) {
            if (field.isValueInjection()) {
                continue;
            }

            if (field.isPrivate()) {
                privateFields.add(field);
            } else if (field.isPublic()) {
                publicFields.add(field);
            } else {
                packagePrivateFields.add(field);
            }
        }

        CodeBlock.Builder builder = CodeBlock.builder();
        ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
        
        for (VeldNode.FieldInjection field : privateFields) {
            String depAccess = getVeldAccessExpression(field.getActualTypeName(), context);
            if (depAccess != null) {
                builder.addStatement("$T.inject$N($N, $N)", accessorClass, 
                    capitalize(field.getFieldName()), fieldName, depAccess);
            } else {
                builder.addStatement("$T.inject$N($N, null)", accessorClass, 
                    capitalize(field.getFieldName()), fieldName);
            }
        }
        
        for (VeldNode.FieldInjection field : packagePrivateFields) {
            String depAccess = getVeldAccessExpression(field.getActualTypeName(), context);
            if (depAccess != null) {
                builder.addStatement("$T.inject$N($N, $N)", accessorClass, 
                    capitalize(field.getFieldName()), fieldName, depAccess);
            } else {
                builder.addStatement("$T.inject$N($N, null)", accessorClass, 
                    capitalize(field.getFieldName()), fieldName);
            }
        }

        for (VeldNode.FieldInjection field : publicFields) {
            String depAccess = getVeldAccessExpression(field.getActualTypeName(), context);
            if (depAccess != null) {
                builder.addStatement("$N.$N = $N", fieldName, field.getFieldName(), depAccess);
            } else {
                builder.addStatement("$N.$N = null", fieldName, field.getFieldName());
            }
        }

        return builder.build();
    }

    /**
     * Builds code for method injections.
     */
    private CodeBlock buildMethodInjectionCode(VeldNode node, String instanceName,
                                                  GenerationContext context) {
        if (!node.hasMethodInjections()) {
            return CodeBlock.of("");
        }

        CodeBlock.Builder builder = CodeBlock.builder();

        for (VeldNode.MethodInjection method : node.getMethodInjections()) {
            if (method.isPrivate()) {
                ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                String accessorMethod = "call" + capitalize(method.getMethodName());

                if (method.getParameters().isEmpty()) {
                    builder.addStatement("$T.$N($N)", accessorClass, accessorMethod, instanceName);
                } else {
                    List<String> paramNames = method.getParameters().stream()
                            .map(p -> getVeldNameForParameter(p, context))
                            .collect(Collectors.toList());
                    builder.addStatement("$T.$N($N, $L)", accessorClass, accessorMethod, instanceName,
                            String.join(", ", paramNames));
                }
            } else {
                if (method.getParameters().isEmpty()) {
                    builder.addStatement("$N.$N()", instanceName, method.getMethodName());
                } else {
                    List<String> paramNames = method.getParameters().stream()
                            .map(p -> getVeldNameForParameter(p, context))
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
                            "<p>@PreDestroy methods are called in reverse dependency order,\n" +
                            "ensuring dependents are destroyed before their dependencies.</p>\n")
                .addStatement("$N.set(true)", "shutdownInitiated");
        
        if (preDestroyNodes.isEmpty()) {
            shutdownBuilder.addStatement("// No @PreDestroy methods to invoke");
        } else {
            shutdownBuilder.addStatement("// Invoke @PreDestroy methods in reverse order");
            for (VeldNode node : preDestroyNodes) {
                shutdownBuilder.addStatement("// $S", node.getClassName());
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
    
    /**
     * Adds accessor method for singleton.
     */
    private void addSingletonAccessor(TypeSpec.Builder classBuilder, VeldNode node,
                                        BeanExistenceGraph.ResolutionResult result) {
        ClassName returnType = ClassName.bestGuess(node.getActualClassName());
        String methodName = node.getVeldName();
        boolean isConditional = result.isConditional(node.getClassName());
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType);
        
        if (isConditional) {
            methodBuilder.addStatement("if ($N == null) return null", methodName);
        }
        
        methodBuilder.addStatement("return $N", methodName);
        
        StringBuilder javadoc = new StringBuilder("Returns the singleton instance of $N.\n");
        if (node.hasPostConstruct()) {
            javadoc.append("\n<p><b>Lifecycle:</b> @PostConstruct has been invoked.</p>\n");
        }
        if (node.hasPreDestroy()) {
            javadoc.append("<p><b>Lifecycle:</b> @PreDestroy will be invoked on shutdown().</p>\n");
        }
        if (isConditional) {
            javadoc.append("\n<p><b>Conditional:</b> May return null if condition is not met.</p>\n");
        }
        
        classBuilder.addMethod(methodBuilder.build());
    }
    
    /**
     * Adds prototype component (factory method).
     */
    private void addPrototypeComponent(TypeSpec.Builder classBuilder, VeldNode node,
                                         BeanExistenceGraph graph,
                                         BeanExistenceGraph.ResolutionResult result,
                                         GenerationContext context) {
        ClassName returnType = ClassName.bestGuess(node.getActualClassName());
        String methodName = node.getVeldName();
        String instanceVar = methodName + "Instance";
        boolean isConditional = result.isConditional(node.getClassName());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(returnType);

        String sectionComment = getSectionForNode(node);
        if (!sectionComment.equals(lastSectionComment)) {
            methodBuilder.addCode("\n$L\n", sectionComment);
            lastSectionComment = sectionComment;
        }
        
        methodBuilder.addCode("// Factory: $N\n", methodName);

        if (isConditional) {
            String flagName = graph.getExistenceFlagName(node.getClassName());
            methodBuilder.beginControlFlow("if ($N)", flagName);
        }

        methodBuilder.addStatement("$T $N = $L", returnType, instanceVar, 
            buildInstantiationCode(node, context));

        CodeBlock fieldInjectionCode = buildFieldInjectionCode(node, instanceVar, context);
        if (!fieldInjectionCode.isEmpty()) {
            methodBuilder.addCode(fieldInjectionCode);
        }

        CodeBlock methodInjectionCode = buildMethodInjectionCode(node, instanceVar, context);
        if (!methodInjectionCode.isEmpty()) {
            methodBuilder.addCode(methodInjectionCode);
        }

        if (node.hasPostConstruct()) {
            if (node.hasPrivateFieldInjections() || node.hasPrivateMethodInjections()) {
                ClassName accessorClass = ClassName.bestGuess(node.getAccessorClassName());
                methodBuilder.addStatement("$T.postConstruct($N)", accessorClass, instanceVar);
            } else {
                methodBuilder.addStatement("$N.$N()", instanceVar, node.getPostConstructMethod());
            }
        }

        methodBuilder.addStatement("return $N", instanceVar);

        if (isConditional) {
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("return null");
            methodBuilder.endControlFlow();
        }

        classBuilder.addMethod(methodBuilder.build());
    }
    
    /**
     * Builds instantiation code for a node.
     */
    private CodeBlock buildInstantiationCode(VeldNode node, GenerationContext context) {
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
            argsBuilder.add("$L", buildDependencyExpression(params.get(i), context));
        }

        return CodeBlock.of("new $T($L)", type, argsBuilder.build());
    }
    
    /**
     * Builds dependency expression for a parameter.
     */
    private CodeBlock buildDependencyExpression(VeldNode.ParameterInfo param, GenerationContext context) {
        if (param.isValueInjection()) {
            return CodeBlock.of("null /* @Value */");
        }
        if (param.isOptionalWrapper()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                return CodeBlock.of("$T.ofNullable($N)", 
                    ClassName.get("java.util", "Optional"), depNode.getVeldName());
            }
            return CodeBlock.of("$T.empty()", ClassName.get("java.util", "Optional"));
        }
        if (param.isProvider()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                if (depNode.isPrototype()) {
                    return CodeBlock.of("() -> new $T()", ClassName.bestGuess(param.getActualTypeName()));
                }
                return CodeBlock.of("() -> $N", depNode.getVeldName());
            }
            return CodeBlock.of("() -> null");
        }
        
        VeldNode depNode = nodeMap.get(param.getActualTypeName());
        if (depNode != null) {
            String flagName = GenerationContext.getExistenceFlagName(depNode.getClassName());
            return CodeBlock.of("($N ? $N : null)", flagName, depNode.getVeldName());
        }
        return CodeBlock.of("null /* UNRESOLVED */");
    }
    
    /**
     * Gets the Veld access expression for a given type.
     */
    private String getVeldAccessExpression(String typeName, GenerationContext context) {
        VeldNode depNode = nodeMap.get(typeName);
        if (depNode != null) {
            if (depNode.isSingleton()) {
                return depNode.getVeldName();
            } else {
                return depNode.getVeldName() + "()";
            }
        }
        return null;
    }
    
    /**
     * Gets the Veld name for a parameter.
     */
    private String getVeldNameForParameter(VeldNode.ParameterInfo param, GenerationContext context) {
        VeldNode depNode = nodeMap.get(param.getActualTypeName());
        if (depNode != null) {
            String flagName = GenerationContext.getExistenceFlagName(depNode.getClassName());
            return "(" + flagName + " ? " + depNode.getVeldName() + " : null)";
        }
        return null;
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
    
    private FieldSpec createLifecycleStateField() {
        return FieldSpec.builder(ClassName.get("java.util.concurrent.atomic", "AtomicBoolean"), "shutdownInitiated")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T(false)", ClassName.get("java.util.concurrent.atomic", "AtomicBoolean"))
                .build();
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
        
        if (node.hasConstructorInjection()) {
            for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
                VeldNode depNode = nodeMap.get(param.getActualTypeName());
                if (depNode != null) {
                    visit(depNode, visited, visiting, sorted);
                }
            }
        }
        
        if (node.hasFieldInjections()) {
            for (VeldNode.FieldInjection field : node.getFieldInjections()) {
                if (field.isValueInjection()) continue;
                VeldNode depNode = nodeMap.get(field.getActualTypeName());
                if (depNode != null) {
                    visit(depNode, visited, visiting, sorted);
                }
            }
        }
        
        if (node.hasMethodInjections()) {
            for (VeldNode.MethodInjection method : node.getMethodInjections()) {
                for (VeldNode.ParameterInfo param : method.getParameters()) {
                    if (param.isValueInjection() || param.isOptionalWrapper() || param.isProvider()) {
                        continue;
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
    
    private CodeBlock buildClassJavadoc(List<VeldNode> singletons, List<VeldNode> prototypes) {
        return CodeBlock.builder()
                .add("Static Veld Dependency Injection Container.\n\n")
                .add("<p>Generated by the Veld annotation processor.</p>\n\n")
                .add("<p><b>Features:</b></p>\n")
                .add("<ul>\n")
                .add("<li>Singletons: $L static fields</li>\n", singletons.size())
                .add("<li>Prototypes: $L factory methods</li>\n", prototypes.size())
                .add("<li>Conditions evaluated at static initialization (deterministic)</li>\n")
                .add("</ul>\n\n")
                .add("<p><b>Lifecycle:</b></p>\n")
                .add("<ul>\n")
                .add("<li>@PostConstruct: invoked immediately after singleton initialization</li>\n")
                .add("<li>@PreDestroy: invoked in shutdown() method (reverse order)</li>\n")
                .add("</ul>\n")
                .build();
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
                "Solution: Add @Component for %s or use @Named",
                componentName, paramPosition + 1, paramType, actualType, actualType);
        }
    }
    
    // ===== ACCESSOR CLASS GENERATION METHODS =====
    
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

    private JavaFile generateAccessorClass(VeldNode node) {
        TypeSpec.Builder accessorBuilder = TypeSpec.classBuilder(node.getAccessorSimpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(buildAccessorJavadoc(node))
                .addAnnotation(createGeneratedAnnotation());

        List<VeldNode.FieldInjection> nonPublicFieldInjections = new ArrayList<>();
        for (VeldNode.FieldInjection field : node.getFieldInjections()) {
            if (!field.isPublic() && !field.isValueInjection()) {
                nonPublicFieldInjections.add(field);
            }
        }

        if (!nonPublicFieldInjections.isEmpty()) {
            addConsolidatedInjectMethod(accessorBuilder, node, nonPublicFieldInjections);
        }

        for (VeldNode.FieldInjection field : node.getFieldInjections()) {
            if (!field.isPublic() && !field.isValueInjection()) {
                addFieldInjectionMethod(accessorBuilder, node, field);
            }
        }

        for (VeldNode.MethodInjection method : node.getMethodInjections()) {
            if (method.isPrivate()) {
                addMethodInjectionMethod(accessorBuilder, node, method);
            }
        }

        if (node.hasPostConstruct()) {
            addPostConstructMethod(accessorBuilder, node);
        }

        if (node.hasPreDestroy()) {
            addPreDestroyMethod(accessorBuilder, node);
        }

        return JavaFile.builder(node.getPackageName(), accessorBuilder.build()).build();
    }

    private void addConsolidatedInjectMethod(TypeSpec.Builder accessorBuilder, VeldNode node,
                                              List<VeldNode.FieldInjection> nonPublicFields) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        ClassName fieldClass = ClassName.get("java.lang.reflect", "Field");
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance");

        for (VeldNode.FieldInjection field : nonPublicFields) {
            TypeName fieldType = getTypeName(field.getActualTypeName());
            String fieldName = field.getFieldName();
            methodBuilder.addParameter(fieldType, fieldName);
        }

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
                .addCode("    throw new $T(\"Fallo al invocar método @PostConstruct: $N\", e);\n",
                        ClassName.get("java.lang", "RuntimeException"), node.getPostConstructMethod())
                .addCode("}\n");

        methodBuilder.addJavadoc("Invokes the @PostConstruct method $N on $N using reflection.\n",
                node.getPostConstructMethod(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

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
                .addCode("    throw new $T(\"Fallo al invocar método @PreDestroy: $N\", e);\n",
                        ClassName.get("java.lang", "RuntimeException"), node.getPreDestroyMethod())
                .addCode("}\n");

        methodBuilder.addJavadoc("Invoca el método @PreDestroy $N en $N usando reflexión.\n",
                node.getPreDestroyMethod(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

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

        methodBuilder.addJavadoc("Injects the private field $N into $N using reflection.\n",
                field.getFieldName(), node.getSimpleName());

        accessorBuilder.addMethod(methodBuilder.build());
    }

    private void addMethodInjectionMethod(TypeSpec.Builder accessorBuilder, VeldNode node, VeldNode.MethodInjection method) {
        ClassName componentType = ClassName.bestGuess(node.getClassName());
        ClassName methodClass = ClassName.get("java.lang.reflect", "Method");
        ClassName objectClass = ClassName.get("java.lang", "Object");

        String methodName = "call" + capitalize(method.getMethodName());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addParameter(componentType, "instance");

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
     * Converts a type name to TypeName, handling primitive types.
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
}
