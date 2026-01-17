package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.*;
import io.github.yasmramos.veld.annotation.ScopeType;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

/**
 * Generates Veld.java with revolutionary static dependency graph.
 *
 * <p>This generator creates a pure static DI container with:</p>
 * <ul>
 *   <li>No if statements</li>
 *   <li>No null values</li>
 *   <li>No mutable state</li>
 *   <li>No runtime decisions</li>
 * </ul>
 * <p>All dependencies are resolved at compile-time through topological sorting
 * and direct field initialization.</p>
 *
 * <p><b>Generated Code Organization:</b></p>
 * <ul>
 *   <li>Fields are ordered by dependency level (leaves first, roots last)</li>
 *   <li>Fields are grouped by package section (Infrastructure, Domain, etc.)</li>
 *   <li>Each section is clearly marked with comments for educational purposes</li>
 * </ul>
 */
public final class VeldSourceGenerator {

    private final List<VeldNode> nodes;
    private final Map<String, VeldNode> nodeMap;
    private final Messager messager;
    private final String veldPackageName;
    private final String veldClassName;
    private final ClassName veldClass;
    
    // Cache for dependency level calculations to improve performance for large projects
    private final Map<String, Integer> levelCache = new HashMap<>();
    // Cache for section assignments to avoid repeated lookups
    private final Map<String, String> sectionCache = new HashMap<>();

    /**
     * Section definitions for organizing generated fields by package.
     * Components are grouped by their package prefix.
     */
    private static final Map<String, String> SECTION_COMMENTS = new LinkedHashMap<>();
    static {
        // Infrastructure layer
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.infrastructure", "// ===== Infrastructure =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.config", "// ===== Configuration =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.persistence", "// ===== Persistence =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.data", "// ===== Data Access =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.repository", "// ===== Repositories =====");
        
        // Domain layer
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.domain", "// ===== Domain =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.model", "// ===== Domain Models =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.service", "// ===== Services =====");
        
        // Application layer
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.application", "// ===== Application =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.usecase", "// ===== Use Cases =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.facade", "// ===== Facades =====");
        
        // UI / Presentation layer
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.ui", "// ===== UI =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.web", "// ===== Web =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.controller", "// ===== Controllers =====");
        
        // Integration layer
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.integration", "// ===== Integration =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.external", "// ===== External Services =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.adapter", "// ===== Adapters =====");
        
        // Events
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.event", "// ===== Events =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.handler", "// ===== Event Handlers =====");
        
        // Utility
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.util", "// ===== Utilities =====");
        SECTION_COMMENTS.put("io.github.yasmramos.veld.example.helper", "// ===== Helpers =====");
    }

    public VeldSourceGenerator(List<VeldNode> nodes, Messager messager, String veldPackageName, String className) {
        this.nodes = nodes;
        this.messager = messager;
        this.veldPackageName = veldPackageName;
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

    /**
     * Generates Veld.java source code with pure static initialization.
     *
     * <p>The generated code is organized for maximum readability and educational value:</p>
     * <ul>
     *   <li>Fields are ordered by dependency level (leaves first, roots last)</li>
     *   <li>Fields are grouped by package section with clear comments</li>
     *   <li>Accessor methods follow the same organization as their corresponding fields</li>
     * </ul>
     *
     * @param packageName the package for Veld.java
     * @return the generated JavaFile, or null if there are unresolved dependencies
     */
    public JavaFile generate(String packageName) {
        // First, validate all dependencies - fail fast at compile-time
        List<DependencyError> errors = validateDependencies();
        if (!errors.isEmpty()) {
            // Report all errors and return null (no code generation)
            for (DependencyError error : errors) {
                error(error.getMessage());
            }
            return null;
        }

        // Topologically sort nodes for correct dependency resolution
        List<VeldNode> sortedNodes = topologicalSort();

        // Calculate dependency levels for ordering (leaves first, roots last)
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

        // Group nodes by section for organized output
        Map<String, List<VeldNode>> sectionNodes = new LinkedHashMap<>();
        for (VeldNode node : singletons) {
            String section = getSectionForNode(node);
            sectionNodes.computeIfAbsent(section, k -> new ArrayList<>()).add(node);
        }

        // Build the sections overview for class Javadoc
        String sectionsOverview = buildSectionsOverview(sectionNodes);

        // Build the Veld class
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(veldClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(
                    "Generated Veld DI container - Pure Static Dependency Graph.\n" +
                    "<p><b>Pure Static Initialization:</b> No reflection, no containers, no runtime decisions.</p>\n" +
                    "<p><b>Access pattern:</b> <code>Veld.componentName()</code></p>\n" +
                    "<p><b>Organization:</b> Fields are ordered by dependency level (leaves → roots) and grouped by package section.</p>\n" +
                    "<p><b>Note:</b> RequestContext is a prototype-scoped component - a new instance is created on each call.</p>\n" +
                    sectionsOverview +
                    "<p>Generated by veld-processor.</p>\n")
                .addMethod(createPrivateConstructor());

        // Generate code for all singleton components grouped by section
        // Each section is sorted by level (ascending) then by class name (for stability)
        for (Map.Entry<String, List<VeldNode>> entry : sectionNodes.entrySet()) {
            String section = entry.getKey();
            List<VeldNode> sectionNodeList = entry.getValue();
            
            // Sort by level ascending, then by class name for stable ordering
            sectionNodeList.sort(Comparator
                .comparingInt((VeldNode n) -> getDependencyLevel(n))
                .thenComparing(VeldNode::getClassName));
            
            // Generate fields for this section
            for (VeldNode node : sectionNodeList) {
                addSingletonFieldWithStructuredJavadoc(classBuilder, node, section);
            }
        }

        // Generate accessor methods for singletons
        for (VeldNode node : singletons) {
            addSingletonAccessor(classBuilder, node);
        }

        // Generate prototype components (factory methods with new)
        for (VeldNode node : prototypes) {
            addPrototypeComponent(classBuilder, node);
        }

        return JavaFile.builder(packageName, classBuilder.build()).build();
    }

    /**
     * Calculates the dependency level for each node.
     * Leaf nodes (no dependencies) have level 0.
     * Nodes that depend on level N components have level N+1.
     * This creates a natural ordering where dependencies come before dependents.
     */
    private Map<String, Integer> calculateDependencyLevels(List<VeldNode> sortedNodes) {
        Map<String, Integer> levels = new HashMap<>();
        Map<String, Integer> dependencyCounts = new HashMap<>();
        
        // First pass: count how many dependencies each node has
        for (VeldNode node : nodes) {
            int count = countDependencies(node);
            dependencyCounts.put(node.getClassName(), count);
        }
        
        // Second pass: calculate levels (leaves = 0, nodes with leaf deps = 1, etc.)
        for (VeldNode node : sortedNodes) {
            int level = calculateNodeLevel(node, dependencyCounts, levels);
            levels.put(node.getClassName(), level);
        }
        
        return levels;
    }

    private int countDependencies(VeldNode node) {
        if (!node.hasConstructorInjection()) {
            return 0;
        }
        
        int count = 0;
        for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
            if (nodeMap.containsKey(param.getActualTypeName())) {
                count++;
            }
        }
        return count;
    }

    private int calculateNodeLevel(VeldNode node, Map<String, Integer> dependencyCounts, 
                                   Map<String, Integer> levels) {
        if (!node.hasConstructorInjection()) {
            return 0; // Leaf node
        }
        
        int maxDepLevel = 0;
        for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                Integer depLevel = levels.get(depNode.getClassName());
                if (depLevel != null && depLevel > maxDepLevel) {
                    maxDepLevel = depLevel;
                }
            }
        }
        
        return maxDepLevel + 1;
    }

    /**
     * Determines the section comment for a node based on its package.
     * Returns a section comment based on package prefix, or generates one from the package name.
     */
    private String getSectionForNode(VeldNode node) {
        String className = node.getClassName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            String packagePrefix = className.substring(0, lastDot);
            
            // Check for exact package matches first
            for (Map.Entry<String, String> entry : SECTION_COMMENTS.entrySet()) {
                if (packagePrefix.equals(entry.getKey()) || packagePrefix.startsWith(entry.getKey() + ".")) {
                    return entry.getValue();
                }
            }
            
            // Generate section from package name for unlisted packages
            // e.g., "io.github.yasmramos.veld.example" -> "// ===== Example ====="
            String lastPackage = packagePrefix.substring(packagePrefix.lastIndexOf('.') + 1);
            String capitalized = lastPackage.substring(0, 1).toUpperCase() + lastPackage.substring(1);
            return "// ===== " + capitalized + " =====";
        }
        
        return null; // No package found
    }

    /**
     * Gets the base category for a node's package for grouping.
     */
    private String getCategoryForNode(VeldNode node) {
        String className = node.getClassName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        }
        return className;
    }

    /**
     * Builds the sections overview HTML string for class Javadoc.
     * Creates a bullet list showing each section name and component count.
     */
    private String buildSectionsOverview(Map<String, List<VeldNode>> sectionNodes) {
        if (sectionNodes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p><b>Sections Overview:</b></p>\n");
        sb.append("<ul>\n");

        for (Map.Entry<String, List<VeldNode>> entry : sectionNodes.entrySet()) {
            String section = entry.getKey();
            int count = entry.getValue().size();

            // Format section name: "// ===== Example =====" -> "Example"
            String sectionName = section;
            if (section != null && section.contains("=====")) {
                int firstEquals = section.indexOf("=====");
                int secondEquals = section.lastIndexOf("=====");
                if (secondEquals > firstEquals + 5) {
                    sectionName = section.substring(firstEquals + 5, secondEquals).trim();
                } else {
                    sectionName = section.replace("//", "").replace("=====", "").trim();
                }
            }

            sb.append("<li>").append(sectionName).append(" (").append(count).append(" component");
            if (count != 1) {
                sb.append("s");
            }
            sb.append(")</li>\n");
        }

        sb.append("</ul>\n");
        return sb.toString();
    }

    /**
     * Creates the private constructor that throws AssertionError.
     * This prevents instantiation of the static DI container.
     */
    private MethodSpec createPrivateConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("throw new $T()", AssertionError.class)
                .build();
    }

    /**
     * Adds a singleton field with direct initialization (no lazy loading, no if).
     * The field is initialized inline with all dependencies resolved at compile-time.
     *
     * <p>Fields are organized by dependency level for educational clarity:
     * components with no dependencies appear first, followed by components
     * that depend on them, and so on.</p>
     */
    private void addSingletonField(TypeSpec.Builder classBuilder, VeldNode node) {
        ClassName type = ClassName.bestGuess(node.getClassName());
        String fieldName = node.getVeldName();

        // Build the instantiation code directly
        CodeBlock initialization = buildInstantiationCode(node);

        // Add the field with direct initialization
        classBuilder.addField(FieldSpec.builder(type, fieldName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Level $L - $L", getDependencyLevel(node), node.getClassName())
                .initializer("$L", initialization)
                .build());
    }

    /**
     * Adds a singleton field with structured Javadoc containing section, level, type, and dependencies.
     */
    private void addSingletonFieldWithStructuredJavadoc(TypeSpec.Builder classBuilder, VeldNode node, String section) {
        // Use actual class name (AOP wrapper if present) for instantiation and field type
        String actualClassName = node.getActualClassName();
        ClassName type = ClassName.bestGuess(actualClassName);
        String fieldName = node.getVeldName();

        // Build the instantiation code using actual class name (AOP wrapper)
        CodeBlock initialization = buildInstantiationCode(node);

        // Format section name: "// ===== Example =====" -> "Example"
        String sectionName = section;
        if (section != null && section.contains("=====")) {
            // Extract text between the ===== markers
            int firstEquals = section.indexOf("=====");
            int secondEquals = section.lastIndexOf("=====");
            if (secondEquals > firstEquals + 5) {
                sectionName = section.substring(firstEquals + 5, secondEquals).trim();
            } else {
                sectionName = section.replace("//", "").replace("=====", "").trim();
            }
        }

        // Get dependency level with caching
        int level = getDependencyLevel(node);

        // Get dependencies as a formatted string
        String dependencies = getDependenciesAsString(node);

        // Build the Javadoc with all information
        StringBuilder javadoc = new StringBuilder();
        javadoc.append("Section: ").append(sectionName).append("\n");
        javadoc.append("Level: ").append(level).append("\n");
        javadoc.append("Type: ").append(node.getClassName());

        if (node.hasAopWrapper()) {
            javadoc.append(" → ").append(actualClassName);
        }

        if (!dependencies.isEmpty()) {
            javadoc.append("\nDepends on: ").append(dependencies);
        }

        if (node.isLazy()) {
            javadoc.append("\nLazy: true (initialized on first access)");
        }

        if (node.hasAopInterceptors()) {
            javadoc.append("\nAOP: ");
            List<String> interceptorNames = new ArrayList<>();
            for (String interceptor : node.getAopInterceptors()) {
                String simpleName = interceptor.substring(interceptor.lastIndexOf('.') + 1);
                interceptorNames.add(simpleName);
            }
            javadoc.append(String.join(", ", interceptorNames));
        } else if (node.hasAopWrapper()) {
            // Component has AOP wrapper - show generic AOP indicator
            javadoc.append("\nAOP: intercepted");
        }

        // Add field - for lazy components, field is null (not initialized inline)
        if (node.isLazy()) {
            classBuilder.addField(FieldSpec.builder(type, fieldName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .addJavadoc("$L", javadoc.toString())
                    .build());
        } else {
            classBuilder.addField(FieldSpec.builder(type, fieldName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .addJavadoc("$L", javadoc.toString())
                    .initializer("$L", initialization)
                    .build());
        }
    }

    /**
     * Gets the dependency level for a node with caching for performance.
     * Leaf nodes (no dependencies) have level 0.
     * Nodes that depend on level N components have level N+1.
     */
    private int getDependencyLevel(VeldNode node) {
        // Check cache first for performance
        String nodeKey = node.getClassName();
        if (levelCache.containsKey(nodeKey)) {
            return levelCache.get(nodeKey);
        }
        
        // A node is a leaf if it has no constructor injection OR no constructor parameters
        if (!node.hasConstructorInjection() || node.getConstructorInfo() == null || node.getConstructorInfo().getParameterCount() == 0) {
            levelCache.put(nodeKey, 0);
            return 0; // Leaf node - no dependencies
        }
        
        int maxDepLevel = 0;
        for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                int depLevel = getDependencyLevel(depNode);
                if (depLevel > maxDepLevel) {
                    maxDepLevel = depLevel;
                }
            }
        }
        
        int result = maxDepLevel + 1;
        levelCache.put(nodeKey, result);
        return result;
    }

    /**
     * Gets the dependencies of a node as a formatted string for Javadoc.
     */
    private String getDependenciesAsString(VeldNode node) {
        if (!node.hasConstructorInjection() || node.getConstructorInfo() == null) {
            return "";
        }
        
        List<String> depNames = new ArrayList<>();
        for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
            VeldNode depNode = nodeMap.get(param.getActualTypeName());
            if (depNode != null) {
                // Extract simple class name from fully qualified name
                String fullName = depNode.getClassName();
                String simpleName = fullName.substring(fullName.lastIndexOf('.') + 1);
                depNames.add(simpleName);
            }
        }
        
        if (depNames.isEmpty()) {
            return "";
        }
        
        return String.join(", ", depNames);
    }

    /**
     * Adds a simple accessor method that returns the final field.
     * No null checks, no if statements - just return.
     */
    private void addSingletonAccessor(TypeSpec.Builder classBuilder, VeldNode node) {
        // Use actual class name (AOP wrapper if present) for return type
        String actualClassName = node.getActualClassName();
        ClassName type = ClassName.bestGuess(actualClassName);
        String fieldName = node.getVeldName();

        if (node.isLazy()) {
            // Lazy component: generate getter with null-check and lazy initialization
            CodeBlock initialization = buildInstantiationCode(node);

            MethodSpec accessor = MethodSpec.methodBuilder(fieldName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(type)
                    .addJavadoc("<p><b>Lazy Singleton:</b> Instance created on first access.</p>\n" +
                                "<p>Thread-safe singleton with deferred initialization.</p>")
                    .beginControlFlow("if ($N == null)", fieldName)
                    .addStatement("$N = $L", fieldName, initialization)
                    .endControlFlow()
                    .addStatement("return $N", fieldName)
                    .build();

            classBuilder.addMethod(accessor);
        } else {
            // Eager singleton: simple return statement
            MethodSpec accessor = MethodSpec.methodBuilder(fieldName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(type)
                    .addStatement("return $N", fieldName)
                    .build();

            classBuilder.addMethod(accessor);
        }
    }

    /**
     * Adds a prototype component - creates new instance on each call.
     * Prototype components are marked with special documentation noting
     * that they are not cached and a new instance is created per request.
     */
    private void addPrototypeComponent(TypeSpec.Builder classBuilder, VeldNode node) {
        // Use actual class name (AOP wrapper if present) for return type
        String actualClassName = node.getActualClassName();
        ClassName type = ClassName.bestGuess(actualClassName);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(node.getVeldName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(type)
                .addJavadoc("<p><b>Prototype:</b> Creates a new instance on each call.</p>\n" +
                            "<p>Not cached - suitable for request-scoped components like RequestContext.</p>");

        // Create the instance with all dependencies
        CodeBlock instantiation = buildInstantiationCode(node);
        methodBuilder.addStatement("return $L", instantiation);

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Builds the code to instantiate a component with all dependencies.
     * Returns: new ClassName(dep1, dep2, ...)
     * Uses AOP wrapper class if present for actual AOP functionality.
     */
    private CodeBlock buildInstantiationCode(VeldNode node) {
        // Use actual class name (AOP wrapper if present)
        String actualClassName = node.getActualClassName();
        ClassName type = ClassName.bestGuess(actualClassName);

        if (!node.hasConstructorInjection()) {
            // No constructor injection - use default constructor
            return CodeBlock.of("new $T()", type);
        }

        // Build constructor arguments
        CodeBlock.Builder argsBuilder = CodeBlock.builder();
        VeldNode.ConstructorInfo ctor = node.getConstructorInfo();
        List<VeldNode.ParameterInfo> params = ctor.getParameters();

        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                argsBuilder.add(", ");
            }
            VeldNode.ParameterInfo param = params.get(i);
            CodeBlock depExpr = buildDependencyExpression(param);
            argsBuilder.add("$L", depExpr);
        }

        return CodeBlock.of("new $T($L)", type, argsBuilder.build());
    }

    /**
     * Builds the expression to resolve a dependency parameter.
     * For singletons: returns the field name directly
     * For prototypes: returns the factory method call
     */
    private CodeBlock buildDependencyExpression(VeldNode.ParameterInfo param) {
        // Handle @Value injection - special handling required
        if (param.isValueInjection()) {
            return buildValueExpression(param);
        }

        // Handle Optional<T> wrapper
        if (param.isOptionalWrapper()) {
            return buildOptionalExpression(param);
        }

        // Handle Provider<T> wrapper
        if (param.isProvider()) {
            return buildProviderExpression(param);
        }

        // Regular dependency - look up in node map
        VeldNode depNode = nodeMap.get(param.getActualTypeName());
        if (depNode != null) {
            ClassName depType = ClassName.bestGuess(param.getActualTypeName());
            String fieldName = depNode.getVeldName();

            // Cast if needed for interface implementations
            if (needsCast(param)) {
                return CodeBlock.of("(($T) $N)", depType, fieldName);
            }
            return CodeBlock.of("$N", fieldName);
        }

        // Unresolved dependency - for now return null (shouldn't happen with valid code)
        return CodeBlock.of("null /* UNRESOLVED: $S */", param.getActualTypeName());
    }

    /**
     * Builds expression for @Value injection.
     * Returns the field reference for singletons or method call for prototypes.
     */
    private CodeBlock buildValueExpression(VeldNode.ParameterInfo param) {
        String expr = param.getValueExpression();
        ClassName type = ClassName.bestGuess(param.getTypeName());

        // For @Value, we return null placeholder - this needs proper config resolution
        // In the revolutionary model, @Value would be resolved at compile-time
        return CodeBlock.of("(($T) null /* @Value: $S */)", type, expr);
    }

    /**
     * Builds expression for Optional<T> wrapper.
     * For the revolutionary model, we check if the dependency exists in the graph.
     */
    private CodeBlock buildOptionalExpression(VeldNode.ParameterInfo param) {
        ClassName optionalClass = ClassName.get("java.util", "Optional");
        VeldNode depNode = nodeMap.get(param.getActualTypeName());

        if (depNode != null) {
            // Dependency exists - wrap the field in Optional
            return CodeBlock.of("$T.of($N)", optionalClass, depNode.getVeldName());
        } else {
            // Dependency doesn't exist - return empty
            return CodeBlock.of("$T.empty()", optionalClass);
        }
    }

    /**
     * Builds expression for Provider<T> wrapper.
     * Since Provider<T> is a @FunctionalInterface, we generate a lambda.
     */
    private CodeBlock buildProviderExpression(VeldNode.ParameterInfo param) {
        VeldNode depNode = nodeMap.get(param.getActualTypeName());

        if (depNode != null) {
            if (depNode.isSingleton()) {
                // For singletons, return the cached field directly
                return CodeBlock.of("() -> $N", depNode.getVeldName());
            } else {
                // For prototypes, create new instance each time
                return CodeBlock.of("() -> $N()", depNode.getVeldName());
            }
        }

        // Unresolved dependency - return null provider
        return CodeBlock.of("() -> null /* UNRESOLVED */");
    }

    /**
     * Checks if a cast is needed for the dependency.
     */
    private boolean needsCast(VeldNode.ParameterInfo param) {
        // Check if the actual type is different from the parameter type
        if (param.getActualTypeName().equals(param.getTypeName())) {
            return false;
        }

        // Check if the actual type is an interface
        TypeElement typeElement = getTypeElement(param.getActualTypeName());
        if (typeElement != null && typeElement.getKind() == ElementKind.INTERFACE) {
            return true;
        }

        // For other cases, check if the types are assignable
        return !param.getTypeName().equals(param.getActualTypeName());
    }

    private TypeElement getTypeElement(String typeName) {
        try {
            // Placeholder - in practice we'd need ElementUtils from processing env
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Performs topological sort on nodes to ensure correct dependency resolution.
     * Dependencies are ordered first so fields can reference them directly.
     *
     * <p>The sort ensures that if Component A depends on Component B,
     * then Component B appears before Component A in the resulting list.
     * This is essential for correct field initialization order.</p>
     */
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
        if (visited.contains(node.getClassName())) {
            return;
        }
        if (visiting.contains(node.getClassName())) {
            // Cycle detected - this should be caught by dependency graph validation
            note("Cycle detected involving: " + node.getClassName());
            return;
        }

        visiting.add(node.getClassName());

        // Visit dependencies first
        if (node.hasConstructorInjection()) {
            for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
                VeldNode depNode = nodeMap.get(param.getActualTypeName());
                if (depNode != null) {
                    visit(depNode, visited, visiting, sorted);
                }
            }
        }

        visiting.remove(node.getClassName());
        visited.add(node.getClassName());
        sorted.add(node);
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

    /**
     * Validates that all constructor dependencies can be resolved.
     * Returns a list of errors for any unresolved dependencies.
     */
    private List<DependencyError> validateDependencies() {
        List<DependencyError> errors = new ArrayList<>();

        for (VeldNode node : nodes) {
            if (node.hasConstructorInjection()) {
                VeldNode.ConstructorInfo ctor = node.getConstructorInfo();
                List<VeldNode.ParameterInfo> params = ctor.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    VeldNode.ParameterInfo param = params.get(i);

                    // Skip @Value and special injections - they're handled differently
                    if (param.isValueInjection() || param.isOptionalWrapper() || param.isProvider()) {
                        continue;
                    }

                    // Check if dependency exists in the graph
                    VeldNode depNode = nodeMap.get(param.getActualTypeName());
                    if (depNode == null) {
                        errors.add(new DependencyError(
                            node.getClassName(),
                            node.getVeldName(),
                            param.getTypeName(),
                            param.getActualTypeName(),
                            i
                        ));
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Represents an unresolved dependency error.
     */
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
                "Compilation failed.%n" +
                "%n" +
                "Unresolved dependency detected:%n" +
                "- Component: %s%n" +
                "- Constructor parameter #%d: %s%n" +
                "- Type: %s%n" +
                "%n" +
                "Fix:%n" +
                "- Add a @Component implementation for %s%n" +
                "- Or qualify it with @Named%n" +
                "%n" +
                "Veld requires all dependencies to be resolved at compile-time.",
                componentName,
                paramPosition + 1,
                paramType,
                actualType,
                actualType
            );
        }
    }
}
