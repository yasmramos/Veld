package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.JavaFile;
import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.processor.AnnotationHelper.InjectSource;
import io.github.yasmramos.veld.processor.InjectionPoint.Dependency;
import io.github.yasmramos.veld.processor.analyzer.ConditionAnalyzer;
import io.github.yasmramos.veld.processor.analyzer.LifecycleAnalyzer;
import io.github.yasmramos.veld.processor.spi.SpiExtensionExecutor;
import io.github.yasmramos.veld.processor.spi.ProcessorToSpiConverter;
import io.github.yasmramos.veld.aop.spi.SpiAopExtensionExecutor;
import io.github.yasmramos.veld.spi.extension.VeldGraph;
import io.github.yasmramos.veld.spi.extension.VeldProcessingContext;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Annotation Processor that generates ComponentFactory implementations and VeldRegistry
 * using ASM bytecode generation. NO REFLECTION is used at runtime.
 * 
 * This processor runs at compile-time and generates .class files directly.
 * 
 * Features:
 * - Circular dependency detection at compile-time
 * - Constructor, field, and method injection
 * - Lifecycle callbacks (@PostConstruct, @PreDestroy)
 * - Singleton and Prototype scopes
 * - JSR-330 compatibility (javax.inject.*)
 * - Jakarta Inject compatibility (jakarta.inject.*)
 * - Primary bean selection (@Primary)
 * - Qualifier-based injection (@Qualifier, @Named)
 * - Factory pattern removed in static model
 * 
 * Supported Component Annotations (use only ONE - they are mutually exclusive):
 * - @io.github.yasmramos.veld.annotation.Component (Veld native - requires scope annotation)
 * - @io.github.yasmramos.veld.annotation.Singleton (Veld native - singleton scope, implies @Component)
 * - @io.github.yasmramos.veld.annotation.Prototype (Veld native - prototype scope, implies @Component)
 * - @io.github.yasmramos.veld.annotation.Lazy (Veld native - lazy singleton, implies @Component)
 * - @io.github.yasmramos.veld.annotation.Factory (Veld native - factory class)
 * - @javax.inject.Singleton (JSR-330 - singleton scope)
 * - @jakarta.inject.Singleton (Jakarta EE - singleton scope)
 * 
 * Supported Injection Annotations:
 * - @io.github.yasmramos.veld.annotation.Inject (Veld native)
 * - @javax.inject.Inject (JSR-330)
 * - @jakarta.inject.Inject (Jakarta EE)
 * 
 * Supported Qualifier Annotations:
 * - @io.github.yasmramos.veld.annotation.Named (Veld native)
 * - @io.github.yasmramos.veld.annotation.Qualifier (Veld native)
 * - @javax.inject.Named (JSR-330)
 * - @jakarta.inject.Named (Jakarta EE)
 * 
 * Supported Bean Selection Annotations:
 * - @io.github.yasmramos.veld.annotation.Primary (Veld native - marks primary bean)
 */
@SupportedAnnotationTypes({
    "io.github.yasmramos.veld.annotation.Component",
    "io.github.yasmramos.veld.annotation.Singleton",
    "io.github.yasmramos.veld.annotation.Prototype",
    "io.github.yasmramos.veld.annotation.Lazy",
    "io.github.yasmramos.veld.annotation.Eager",
    "io.github.yasmramos.veld.annotation.DependsOn",
    "io.github.yasmramos.veld.annotation.Primary",
    "io.github.yasmramos.veld.annotation.Order",
    "io.github.yasmramos.veld.annotation.Qualifier",
    "io.github.yasmramos.veld.annotation.Lookup",
    "io.github.yasmramos.veld.annotation.Profile",
    "io.github.yasmramos.veld.annotation.Value",
    "io.github.yasmramos.veld.annotation.ConditionalOnProperty",
    "io.github.yasmramos.veld.annotation.ConditionalOnClass",
    "io.github.yasmramos.veld.annotation.ConditionalOnMissingBean",
    "io.github.yasmramos.veld.annotation.ConditionalOnBean",
    "io.github.yasmramos.veld.annotation.PostConstruct",
    "io.github.yasmramos.veld.annotation.PreDestroy",
    "io.github.yasmramos.veld.annotation.Optional",
    "io.github.yasmramos.veld.annotation.Scope",
    "javax.inject.Inject",
    "javax.inject.Singleton",
    "jakarta.inject.Inject",
    "jakarta.inject.Singleton"
})
@SupportedOptions({
    "veld.profile",
    "veld.strict",
    "veld.extensions.disabled",
    "veld.debug"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class VeldProcessor extends AbstractProcessor {
    
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;
    
    private final List<ComponentInfo> discoveredComponents = new ArrayList<>();
    private final DependencyGraph dependencyGraph = new DependencyGraph();
    
    // Maps interface -> list of implementing components (for conflict detection)
    private final Map<String, List<String>> interfaceImplementors = new HashMap<>();
    
    // Track already processed classes to avoid duplicates
    private final Set<String> processedClasses = new HashSet<>();

    // Event subscriptions for zero-reflection event registration
    private final List<EventRegistryGenerator.SubscriptionInfo> eventSubscriptions = new ArrayList<>();

    // Static dependency graph
    private final List<VeldNode> veldNodes = new ArrayList<>();
    
    // Discovered profiles from @Profile annotations - for compile-time class generation
    private final Set<String> discoveredProfiles = new LinkedHashSet<>();
    
    // SPI Extension Executor
    private SpiExtensionExecutor extensionExecutor;
    private SpiAopExtensionExecutor aopExtensionExecutor;
    private VeldGraph spiGraph;
    private VeldProcessingContext spiContext;
    
    // Options manager for compile-time configuration
    private VeldOptions options;
    
    // Profile setting for compile-time class name generation
    private String profile = "prod";
    
    // Modular analyzers for component analysis
    private ConditionAnalyzer conditionAnalyzer;
    private LifecycleAnalyzer lifecycleAnalyzer;
    
    public VeldProcessor() {
    }
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        
        // Initialize options manager
        this.options = VeldOptions.create(processingEnv);
        this.profile = options.getProfile();
        
        // Initialize modular analyzers
        this.conditionAnalyzer = new ConditionAnalyzer(messager, elementUtils);
        this.lifecycleAnalyzer = new LifecycleAnalyzer(messager);
        
        // Log Veld configuration
        note("Veld " + options.getSummary());
        
        if (options.isStrictMode()) {
            note("STRICT MODE ENABLED: All warnings will be treated as errors");
        }
        
        // Initialize SPI Extension Executor
        this.extensionExecutor = new SpiExtensionExecutor(options.areExtensionsEnabled());
        
        if (extensionExecutor.hasExtensions()) {
            note("SPI Extensions loaded: " + extensionExecutor.getExtensionCount());
        }
        
        // Initialize AOP SPI Extension Executor (uses SPI discovery, no manual AopGenerator needed)
        this.aopExtensionExecutor = new SpiAopExtensionExecutor(
            options.areExtensionsEnabled(), messager, elementUtils, typeUtils, filer);
        
        if (aopExtensionExecutor.hasExtensions()) {
            note("AOP Extensions loaded: " + aopExtensionExecutor.getExtensionCount());
        }
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            // Check for circular dependencies before generating code
            if (!discoveredComponents.isEmpty()) {
                if (validateNoCyclicDependencies()) {
                    // Validate interface implementations (warnings only)
                    validateInterfaceImplementations();
                    generateRegistry();
                }
            }
            return true;
        }
        
        // Collect all elements that should be components
        Set<TypeElement> componentElements = new HashSet<>();
        
        // Find all directly annotated @Component classes
        for (Element element : roundEnv.getElementsAnnotatedWith(Component.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                componentElements.add((TypeElement) element);
            }
        }
        
        // Find classes annotated with @Singleton (which has @Component as meta-annotation)
        for (Element element : roundEnv.getElementsAnnotatedWith(Singleton.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                componentElements.add((TypeElement) element);
            }
        }
        
        // Find classes annotated with @Prototype (which has @Component as meta-annotation)
        for (Element element : roundEnv.getElementsAnnotatedWith(Prototype.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                componentElements.add((TypeElement) element);
            }
        }
        
        // Find classes annotated with @Lazy (which has @Component as meta-annotation)
        for (Element element : roundEnv.getElementsAnnotatedWith(Lazy.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                componentElements.add((TypeElement) element);
            }
        }
        
        // Find classes annotated with @Eager (eager initialization, does NOT have @Component meta-annotation)
        for (Element element : roundEnv.getElementsAnnotatedWith(Eager.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                componentElements.add((TypeElement) element);
                note("Found @Eager component: " + ((TypeElement) element).getQualifiedName());
            }
        }
        
        // Find classes annotated with javax.inject.Singleton
        TypeElement javaxSingleton = elementUtils.getTypeElement("javax.inject.Singleton");
        if (javaxSingleton != null) {
            for (Element element : roundEnv.getElementsAnnotatedWith(javaxSingleton)) {
                if (element.getKind() == ElementKind.CLASS) {
                    componentElements.add((TypeElement) element);
                }
            }
        }
        
        // Find classes annotated with jakarta.inject.Singleton
        TypeElement jakartaSingleton = elementUtils.getTypeElement("jakarta.inject.Singleton");
        if (jakartaSingleton != null) {
            for (Element element : roundEnv.getElementsAnnotatedWith(jakartaSingleton)) {
                if (element.getKind() == ElementKind.CLASS) {
                    componentElements.add((TypeElement) element);
                }
            }
        }
        
        // PHASE 1: DISCOVERY - Analyze all components first without generating factories
        for (TypeElement typeElement : componentElements) {
            String className = typeElement.getQualifiedName().toString();
            
            // Skip if already processed
            if (processedClasses.contains(className)) {
                continue;
            }
            processedClasses.add(className);
            
            // Validate class
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                error(typeElement, "Component annotation cannot be applied to abstract classes");
                continue;
            }
            
            // Check nesting kind for proper inner class handling
            NestingKind nestingKind = typeElement.getNestingKind();
            if (nestingKind == NestingKind.LOCAL || nestingKind == NestingKind.ANONYMOUS) {
                error(typeElement, "Component annotation cannot be applied to local or anonymous classes");
                continue;
            }
            
            // Log nesting kind for debugging inner class processing
            if (nestingKind == NestingKind.MEMBER) {
                // Static inner class - get enclosing element for context
                Element enclosing = typeElement.getEnclosingElement();
                String enclosingName = enclosing instanceof TypeElement 
                    ? ((TypeElement) enclosing).getQualifiedName().toString()
                    : enclosing.toString();
                note("Processing static inner class: " + className + " (enclosing: " + enclosingName + ")");
            }
            
            try {
                ComponentInfo info = analyzeComponent(typeElement);
                
                // For inner classes, ensure the className is fully qualified
                if (nestingKind == NestingKind.MEMBER) {
                    String computedClassName = typeElement.getQualifiedName().toString();
                    if (!computedClassName.equals(info.getClassName())) {
                        info.setClassName(computedClassName);
                    }
                }
                
                discoveredComponents.add(info);
                
                // Build dependency graph for cycle detection
                buildDependencyGraph(info);
                
                note("Discovered component: " + info.getClassName() + " (name: " + info.getComponentName() + ")");
            } catch (ProcessingException e) {
                error(typeElement, e.getMessage());
            }
        }

        // PHASE 2: GENERATION - Now that we have all components, identify unresolved dependencies

        return true;
    }
    /**
     * Identifies dependencies that are interfaces without implementing components.
     * These are tracked for runtime resolution (e.g., via mocks in tests).
     */
    /**
     * Checks if a dependency type is an interface without a component implementation,
     * and adds it to the unresolved list if so.
     */
    
    
    
    /**
     * Checks if a type is potentially injectable.
     * Returns false for primitives, arrays, and wildcard types.
     * 
     * @param type the type to check
     * @return true if the type can be injected
     */
    private boolean isInjectableType(TypeMirror type) {
        if (type == null) {
            return false;
        }
        
        TypeKind kind = type.getKind();
        
        // Primitives are not directly injectable (box them first)
        if (kind.isPrimitive()) {
            return false;
        }
        
        // Arrays can only be injected if element type is injectable
        if (kind == TypeKind.ARRAY) {
            javax.lang.model.type.ArrayType arrayType = (javax.lang.model.type.ArrayType) type;
            return isInjectableType(arrayType.getComponentType());
        }
        
        // Wildcards are not directly injectable
        if (kind == TypeKind.WILDCARD) {
            return false;
        }
        
        // Void is not injectable
        if (kind == TypeKind.VOID) {
            return false;
        }
        
        return true;
    }
    
    
    
    /**
     * Builds the dependency graph for a component.
     * Adds the component and all its dependencies to the graph.
     * Optional dependencies are excluded from cycle detection since they can be null.
     */
    private void buildDependencyGraph(ComponentInfo info) {
        String componentName = info.getClassName();
        dependencyGraph.addComponent(componentName);
        
        // Add constructor dependencies (skip optional ones)
        if (info.getConstructorInjection() != null) {
            for (InjectionPoint.Dependency dep : info.getConstructorInjection().getDependencies()) {
                if (!dep.allowsMissing()) {
                    dependencyGraph.addDependency(componentName, dep.getActualTypeName());
                }
            }
        }
        
        // Add field dependencies (skip optional ones and @Value injections)
        for (InjectionPoint field : info.getFieldInjections()) {
            for (InjectionPoint.Dependency dep : field.getDependencies()) {
                if (!dep.allowsMissing() && !dep.isValueInjection()) {
                    dependencyGraph.addDependency(componentName, dep.getActualTypeName());
                }
            }
        }
        
        // Add method dependencies (skip optional ones)
        for (InjectionPoint method : info.getMethodInjections()) {
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                if (!dep.allowsMissing()) {
                    dependencyGraph.addDependency(componentName, dep.getActualTypeName());
                }
            }
        }

        // Add explicit dependencies from @DependsOn
        if (info.hasExplicitDependencies()) {
            for (String beanName : info.getExplicitDependencies()) {
                String resolvedType = resolveBeanNameToType(beanName);
                if (resolvedType != null) {
                    dependencyGraph.addDependency(componentName, resolvedType);
                    note("    -> Explicit dependency: " + beanName + " -> " + resolvedType);
                } else {

                    String suggestion = findBestFuzzyMatch(beanName);

                    StringBuilder msg = new StringBuilder();
                    msg.append("@DependsOn references unknown bean: \"")
                            .append(beanName).append("\"\n")
                            .append("   Component: ").append(componentName);

                    if (suggestion != null) {
                        msg.append("\n   Did you mean \"").append(suggestion).append("\"?");
                    }

                    error(info.getTypeElement(), msg.toString());
                }
            }
        }
    }
    
    /**
     * Resolves a bean name to its corresponding type name.
     * Supports multiple resolution strategies:
     * 1. Explicit @Named value (highest priority)
     * 2. @Component value (explicit name)
     * 3. Decapitalized simple class name
     * 4. Fully qualified class name
     * 5. Aliases with kebab-case, snake_case variations
     * 
     * @param beanName the bean name to resolve
     * @return the fully qualified type name, or null if not found
     */
    private String resolveBeanNameToType(String beanName) {
        if (beanName == null || beanName.isEmpty()) {
            return null;
        }
        
        // Strategy 1: Check if this is an explicit @Named value (highest priority)
        for (ComponentInfo component : discoveredComponents) {
            if (hasExplicitNamedValue(component, beanName)) {
                debug("Resolved '" + beanName + "' -> " + component.getClassName() + " (@Named)");
                return component.getClassName();
            }
        }
        
        // Strategy 2: Check explicit component name (@Component value)
        for (ComponentInfo component : discoveredComponents) {
            if (beanName.equals(component.getComponentName())) {
                debug("Resolved '" + beanName + "' -> " + component.getClassName() + " (component name)");
                return component.getClassName();
            }
        }
        
        // Strategy 3: Decapitalized simple class name (JavaBeans convention)
        String decapitalizedName = decapitalize(beanName);
        for (ComponentInfo component : discoveredComponents) {
            String simpleName = getSimpleClassName(component.getClassName());
            if (decapitalizedName.equals(simpleName) || decapitalizedName.equals(simpleName.toLowerCase())) {
                debug("Resolved '" + beanName + "' -> " + component.getClassName() + " (simple name)");
                return component.getClassName();
            }
        }
        
        // Strategy 4: Fully qualified class name
        for (ComponentInfo component : discoveredComponents) {
            if (beanName.equals(component.getClassName())) {
                debug("Resolved '" + beanName + "' -> " + component.getClassName() + " (full name)");
                return component.getClassName();
            }
        }
        
        // Strategy 5: Try variations (kebab-case, snake_case)
        String kebabName = toKebabCase(beanName);
        String snakeName = toSnakeCase(beanName);
        
        for (ComponentInfo component : discoveredComponents) {
            // Check component name variations
            String compName = component.getComponentName();
            if (compName.equalsIgnoreCase(beanName) || 
                compName.equalsIgnoreCase(kebabName) || 
                compName.equalsIgnoreCase(snakeName)) {
                debug("Resolved '" + beanName + "' -> " + component.getClassName() + " (case-insensitive)");
                return component.getClassName();
            }
            
            // Check simple name variations
            String simpleName = getSimpleClassName(component.getClassName());
            if (simpleName.equalsIgnoreCase(beanName) || 
                toKebabCase(simpleName).equalsIgnoreCase(kebabName) ||
                toSnakeCase(simpleName).equalsIgnoreCase(snakeName)) {
                debug("Resolved '" + beanName + "' -> " + component.getClassName() + " (variation)");
                return component.getClassName();
            }
        }
        
        // Strategy 6: Fuzzy matching for typos (Levenshtein distance)
        String bestMatch = findBestFuzzyMatch(beanName);
        if (bestMatch != null) {
            warning(null, "Bean name '" + beanName + "' not found. Did you mean '" + bestMatch + "'?");
            return null;
        }
        
        debug("Could not resolve bean name: " + beanName);
        return null;
    }
    
    /**
     * Checks if a component has an explicit @Named value that matches the given name.
     */
    private boolean hasExplicitNamedValue(ComponentInfo component, String name) {
        TypeElement element = component.getTypeElement();
        if (element == null) {
            return false;
        }
        
        // Check Veld @Named
        io.github.yasmramos.veld.annotation.Named vNamed = 
            element.getAnnotation(io.github.yasmramos.veld.annotation.Named.class);
        if (vNamed != null && !vNamed.value().isEmpty() && vNamed.value().equals(name)) {
            return true;
        }
        
        // Check javax.inject.Named
        try {
            Class<?> javaxNamed = Class.forName("javax.inject.Named");
            Object annotation = element.getAnnotation(javaxNamed.asSubclass(java.lang.annotation.Annotation.class));
            if (annotation != null) {
                java.lang.reflect.Method valueMethod = annotation.getClass().getMethod("value");
                String value = (String) valueMethod.invoke(annotation);
                if (name.equals(value)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Annotation not available or not applicable
        }
        
        // Check jakarta.inject.Named
        try {
            Class<?> jakartaNamed = Class.forName("jakarta.inject.Named");
            Object annotation = element.getAnnotation(jakartaNamed.asSubclass(java.lang.annotation.Annotation.class));
            if (annotation != null) {
                java.lang.reflect.Method valueMethod = annotation.getClass().getMethod("value");
                String value = (String) valueMethod.invoke(annotation);
                if (name.equals(value)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Annotation not available or not applicable
        }
        
        return false;
    }
    
    /**
     * Gets the simple class name from a fully qualified class name.
     */
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "";
        }
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
    
    /**
     * Converts a name to kebab-case.
     */
    private String toKebabCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
    
    /**
     * Converts a name to snake_case.
     */
    private String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * Finds the best fuzzy match for a bean name using Levenshtein distance.
     * Returns the closest matching component name if similarity > 0.7.
     */
    private String findBestFuzzyMatch(String beanName) {
        String bestMatch = null;
        double bestSimilarity = 0.0;
        
        for (ComponentInfo component : discoveredComponents) {
            String[] candidates = {
                component.getComponentName(),
                getSimpleClassName(component.getClassName())
            };
            
            for (String candidate : candidates) {
                double similarity = calculateSimilarity(beanName, candidate);
                if (similarity > bestSimilarity && similarity > 0.7) {
                    bestSimilarity = similarity;
                    bestMatch = component.getComponentName();
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Calculates similarity between two strings (0.0 to 1.0).
     * Uses Levenshtein distance for calculation.
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equalsIgnoreCase(s2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * Calculates Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Validates that there are no circular dependencies.
     * 
     * @return true if no cycles found, false otherwise
     */
    private boolean validateNoCyclicDependencies() {
        java.util.Optional<List<String>> cycle = dependencyGraph.detectCycle();
        
        if (cycle.isPresent()) {
            String cyclePath = DependencyGraph.formatCycle(cycle.get());
            String cycleDetail = buildCycleDetailMessage(cycle.get());
            
            error(null, "Circular dependency detected: " + cyclePath + "\n" +
                cycleDetail + "\n" +
                "  Circular dependencies are not allowed in Veld.\n" +
                "  Possible solutions:\n" +
                "    - Use setter/method injection instead of constructor injection\n" +
                "    - Break the dependency cycle by refactoring\n" +
                "    - Use @Lazy on one of the dependencies.\n" +
                "  Fix: Refactor your code to break the cycle.");
            return false;
        }
        
        note("Dependency graph validated: no circular dependencies found");
        return true;
    }
    /**
     * Builds a detailed message explaining the circular dependency.
     * 
     * @param cycle the cycle path
     * @return detailed message about the cycle
     */
    private String buildCycleDetailMessage(List<String> cycle) {
        if (cycle == null || cycle.size() < 2) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("  Dependency chain:\n");

        for (int i = 0; i < cycle.size(); i++) {
            String current = cycle.get(i);
            String next = cycle.get((i + 1) % cycle.size());

            // Extract simple class name
            String currentName = current.substring(current.lastIndexOf('.') + 1);
            String nextName = next.substring(next.lastIndexOf('.') + 1);

            sb.append("    ").append(i + 1).append(". ").append(currentName);
            
            // Try to identify the injection point
            if (i < cycle.size() - 1) {
                sb.append(" → ");
            } else {
                sb.append(" → (back to start)");
            }
        }

        return sb.toString();
    }
    
    private ComponentInfo analyzeComponent(TypeElement typeElement) throws ProcessingException {
        String className = typeElement.getQualifiedName().toString();
        
        // Get component name - check all possible sources
        String componentName = getComponentName(typeElement);
        if (componentName == null || componentName.isEmpty()) {
            componentName = decapitalize(typeElement.getSimpleName().toString());
        }
        
        // Determine scope and check for @Lazy
        String scope = determineScope(typeElement);
        boolean isLazy = typeElement.getAnnotation(Lazy.class) != null;
        boolean isEager = typeElement.getAnnotation(Eager.class) != null;
        
        if (isEager) {
            note("  -> Scope: singleton (from @Eager)");
        }
        
        // Check for @Primary annotation
        boolean isPrimary = typeElement.getAnnotation(Primary.class) != null;
        if (isPrimary) {
            note("  -> Primary bean selected");
        }
        
        ComponentInfo info = new ComponentInfo(className, componentName, scope, null, isLazy, isEager, isPrimary);
        
        // Check for @Order annotation (must be after info is created)
        Order orderAnnotation = typeElement.getAnnotation(Order.class);
        if (orderAnnotation != null) {
            int orderValue = orderAnnotation.value();
            info.setOrder(orderValue);
            note("  -> Order: " + orderValue);
        }
        
        info.setTypeElement(typeElement);
        
        // Find injection points
        analyzeConstructors(typeElement, info);
        analyzeFields(typeElement, info);
        analyzeMethods(typeElement, info);
        analyzeLifecycle(typeElement, info);
        analyzeEventSubscribers(typeElement, info);
        
        // Analyze implemented interfaces for interface-based injection
        analyzeInterfaces(typeElement, info);
        
        // Analyze conditional annotations
        analyzeConditions(typeElement, info);
        
        // Analyze explicit dependencies (@DependsOn)
        analyzeDependsOn(typeElement, info);
        
        return info;
    }
    
    /**
     * Gets the component name from any applicable annotation.
     * Priority: @Component > @Singleton > @Prototype > @Lazy
     */
    private String getComponentName(TypeElement typeElement) {
        // Check @Component first
        Component componentAnnotation = typeElement.getAnnotation(Component.class);
        if (componentAnnotation != null && !componentAnnotation.value().isEmpty()) {
            return componentAnnotation.value();
        }
        
        // Check @Singleton
        Singleton singletonAnnotation = typeElement.getAnnotation(Singleton.class);
        if (singletonAnnotation != null && !singletonAnnotation.value().isEmpty()) {
            return singletonAnnotation.value();
        }
        
        // Check @Prototype
        Prototype prototypeAnnotation = typeElement.getAnnotation(Prototype.class);
        if (prototypeAnnotation != null && !prototypeAnnotation.value().isEmpty()) {
            return prototypeAnnotation.value();
        }
        
        // Check @Lazy
        Lazy lazyAnnotation = typeElement.getAnnotation(Lazy.class);
        if (lazyAnnotation != null && !lazyAnnotation.value().isEmpty()) {
            return lazyAnnotation.value();
        }
        
        return null;
    }
    
    /**
     * Determines the scope of a component based on its annotations.
     * @Prototype takes precedence for prototype scope.
     * @RequestScoped and @SessionScoped are recognized for web scopes.
     * All other scope annotations (Veld @Singleton, javax/jakarta @Singleton) result in "singleton".
     * Default is "singleton" if no explicit scope is specified.
     */
    private String determineScope(TypeElement typeElement) {
        // Check for @Prototype first - it's the only way to get prototype scope
        if (typeElement.getAnnotation(Prototype.class) != null) {
            note("  -> Scope: prototype");
            return "prototype";
        }
        
        // Check for @RequestScoped
        if (typeElement.getAnnotation(io.github.yasmramos.veld.annotation.RequestScoped.class) != null) {
            note("  -> Scope: request");
            return "request";
        }
        
        // Check for @SessionScoped
        if (typeElement.getAnnotation(io.github.yasmramos.veld.annotation.SessionScoped.class) != null) {
            note("  -> Scope: session");
            return "session";
        }
        
        // Check for explicit singleton annotations
        if (typeElement.getAnnotation(Singleton.class) != null ||
            AnnotationHelper.hasSingletonAnnotation(typeElement)) {
            note("  -> Scope: singleton (explicit)");
            return "singleton";
        }
        
        // Check for @Lazy alone (implies singleton)
        if (typeElement.getAnnotation(Lazy.class) != null) {
            note("  -> Scope: singleton (from @Lazy)");
            return "singleton";
        }
        
        // Check for @Scope annotation with custom value
        java.util.Optional<String> scopeValue = AnnotationHelper.getScopeValue(typeElement);
        if (scopeValue.isPresent()) {
            note("  -> Scope: " + scopeValue.get());
            return scopeValue.get();
        }
        
        // Default scope
        note("  -> Scope: singleton (default)");
        return "singleton";
    }
    
    /**
     * Analyzes all interfaces implemented by the component.
     * These interfaces will be used for interface-based injection.
     */
    private void analyzeInterfaces(TypeElement typeElement, ComponentInfo info) {
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (interfaceType.getKind() == TypeKind.DECLARED) {
                DeclaredType declaredType = (DeclaredType) interfaceType;
                TypeElement interfaceElement = (TypeElement) declaredType.asElement();
                String interfaceName = interfaceElement.getQualifiedName().toString();
                
                // Skip standard Java interfaces that are unlikely to be injection targets
                if (!interfaceName.startsWith("java.lang.") && 
                    !interfaceName.startsWith("java.io.") &&
                    !interfaceName.startsWith("java.util.")) {
                    info.addImplementedInterface(interfaceName);
                    
                    // Track interface implementors for conflict detection
                    interfaceImplementors
                        .computeIfAbsent(interfaceName, k -> new ArrayList<>())
                        .add(info.getClassName());
                    
                    note("  -> Implements interface: " + interfaceName);
                }
            }
        }
    }
    
    /**
     * Validates that there are no ambiguous interface implementations.
     * Multiple implementations of the same interface require @Named or @Primary to disambiguate.
     * In strict mode, this method will report errors instead of warnings.
     * 
     * @return true if validation passes (with possible warnings), false if strict mode errors found
     */
    private boolean validateInterfaceImplementations() {
        boolean hasConflicts = false;
        
        for (Map.Entry<String, List<String>> entry : interfaceImplementors.entrySet()) {
            String interfaceName = entry.getKey();
            List<String> implementors = entry.getValue();
            
            if (implementors.size() > 1) {
                // Multiple implementations - check if at least one has @Primary or @Named
                int withDisambiguation = 0;
                int withProfile = 0;
                StringBuilder implDetails = new StringBuilder();
                
                for (int i = 0; i < implementors.size(); i++) {
                    String implClassName = implementors.get(i);
                    implDetails.append("\n    ").append(i + 1).append(". ").append(implClassName);
                    
                    // Check if this implementation has @Primary or @Named
                    boolean hasPrimary = hasPrimaryAnnotation(implClassName);
                    boolean hasNamed = hasNamedAnnotation(implClassName);
                    boolean hasProfile = hasProfileAnnotation(implClassName);
                    
                    if (hasPrimary || hasNamed) {
                        withDisambiguation++;
                    }
                    if (hasProfile) {
                        withProfile++;
                    }
                }
                
                // Validation passes if:
                // 1. At least one implementation has @Primary or @Named, OR
                // 2. All implementations have @Profile (they're conditionally registered)
                boolean validationPassed = (withDisambiguation > 0) || (withProfile == implementors.size());
                
                if (!validationPassed) {
                    // Conflict: multiple implementations without proper disambiguation
                    // STATIC MODEL: This is always an error, never a warning
                    String message = "Multiple implementations found for interface: " + interfaceName +
                        "\n  Implementations:" + implDetails +
                        "\n  Multiple implementations of the same interface require @Primary or @Named to disambiguate." +
                        "\n  Static compile-time model: Ambiguous dependencies are not allowed.";

                    error(null, message + "\n  Fix: Add @Primary or @Named(\"uniqueName\") to exactly one implementation.");
                    hasConflicts = true;
                }
            }
        }

        if (hasConflicts) {
            note("Interface conflict validation found errors.");
        } else if (!interfaceImplementors.isEmpty()) {
            note("Interface conflict detection complete. No conflicts found.");
        }

        return !hasConflicts;
    }
    
    /**
     * Checks if a class has @Primary annotation.
     */
    private boolean hasPrimaryAnnotation(String className) {
        TypeElement element = elementUtils.getTypeElement(className);
        return element != null && element.getAnnotation(Primary.class) != null;
    }
    
    /**
     * Checks if a class has @Named annotation (any variant).
     */
    private boolean hasNamedAnnotation(String className) {
        TypeElement element = elementUtils.getTypeElement(className);
        if (element == null) {
            return false;
        }
        
        // Check for @Named (Veld)
        if (element.getAnnotation(io.github.yasmramos.veld.annotation.Named.class) != null) {
            return true;
        }
        
        // Check javax.inject.Named using reflection - handle safely if not on classpath
        try {
            Class<?> javaxNamedClass = Class.forName("javax.inject.Named");
            Object annotation = element.getAnnotation(javaxNamedClass.asSubclass(java.lang.annotation.Annotation.class));
            if (annotation != null) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // javax.inject.Named not available on classpath
        } catch (ClassCastException e) {
            // Not an annotation
        }
        
        // Check jakarta.inject.Named using reflection - handle safely if not on classpath
        try {
            Class<?> jakartaNamedClass = Class.forName("jakarta.inject.Named");
            Object annotation = element.getAnnotation(jakartaNamedClass.asSubclass(java.lang.annotation.Annotation.class));
            if (annotation != null) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // jakarta.inject.Named not available on classpath
        } catch (ClassCastException e) {
            // Not an annotation
        }
        
        return false;
    }
    
    /**
     * Checks if a class has @Profile annotation.
     * Profile-annotated components are conditionally registered.
     */
    private boolean hasProfileAnnotation(String className) {
        TypeElement element = elementUtils.getTypeElement(className);
        return element != null && element.getAnnotation(io.github.yasmramos.veld.annotation.Profile.class) != null;
    }
    
    /**
     * Checks if a class has @Primary, @Named, or @Profile annotation for disambiguation.
     * Profile-annotated components are conditionally registered and don't conflict
     * with other implementations at runtime when different profiles are active.
     */
    private boolean hasDisambiguatorAnnotation(String className) {
        return hasPrimaryAnnotation(className) || hasNamedAnnotation(className) || hasProfileAnnotation(className);
    }
    
    private void analyzeConstructors(TypeElement typeElement, ComponentInfo info) throws ProcessingException {
        ExecutableElement injectConstructor = null;
        ExecutableElement defaultConstructor = null;
        InjectSource injectSource = InjectSource.NONE;
        int injectConstructorCount = 0;
        
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            
            ExecutableElement constructor = (ExecutableElement) enclosed;
            
            // Check for any @Inject annotation (Veld, javax.inject, or jakarta.inject)
            if (AnnotationHelper.hasInjectAnnotation(constructor)) {
                injectConstructorCount++;
                if (injectConstructorCount > 1) {
                    throw new ProcessingException(
                        "Only one constructor can be annotated with @Inject in class " + 
                        typeElement.getQualifiedName() + ".\n" +
                        "  Found " + injectConstructorCount + " @Inject constructors.\n" +
                        "  Fix: Remove @Inject from all but one constructor, or use @Inject on exactly one constructor."
                    );
                }
                injectConstructor = constructor;
                injectSource = AnnotationHelper.getInjectSource(constructor);
            } else if (constructor.getParameters().isEmpty()) {
                defaultConstructor = constructor;
            }
        }
        
        ExecutableElement chosenConstructor = injectConstructor != null ? injectConstructor : defaultConstructor;
        
        if (chosenConstructor == null) {
            String className = typeElement.getQualifiedName().toString();
            throw new ProcessingException(
                "No suitable constructor found in class: " + className + ".\n" +
                "  Veld requires either:\n" +
                "  1. A constructor annotated with @Inject (recommended), or\n" +
                "  2. A public no-argument constructor\n" +
                "  Fix: Add @Inject to your preferred constructor, or ensure a public no-arg constructor exists."
            );
        }
        
        // Log which annotation specification is being used
        if (injectSource.isStandard()) {
            note("  -> Using " + injectSource.getPackageName() + ".Inject for constructor injection");
        }
        
        List<Dependency> dependencies = new ArrayList<>();
        for (VariableElement param : chosenConstructor.getParameters()) {
            dependencies.add(createDependency(param));
        }
        
        String descriptor = getMethodDescriptor(chosenConstructor);
        info.setConstructorInjection(new InjectionPoint(
                InjectionPoint.Type.CONSTRUCTOR, "<init>", descriptor, dependencies));
    }
    
    private void analyzeFields(TypeElement typeElement, ComponentInfo info) throws ProcessingException {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;
            
            VariableElement field = (VariableElement) enclosed;
            
            // Check for @Value annotation first
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                if (field.getModifiers().contains(Modifier.FINAL)) {
                    throw new ProcessingException("@Value cannot be applied to final fields: " + field.getSimpleName());
                }
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    throw new ProcessingException("@Value cannot be applied to static fields: " + field.getSimpleName());
                }
                
                String typeName = getTypeName(field.asType());
                String descriptor = getTypeDescriptor(field.asType());
                String valueExpression = valueAnnotation.value();
                InjectionPoint.Visibility visibility = getFieldVisibility(field);
                
                Dependency dep = Dependency.forValue(typeName, descriptor, valueExpression);
                
                if (visibility == InjectionPoint.Visibility.PRIVATE) {
                    note("  -> @Value(\"" + valueExpression + "\") for private field: " + field.getSimpleName() + " (requires weaving)");
                } else {
                    note("  -> @Value(\"" + valueExpression + "\") for field: " + field.getSimpleName());
                }
                
                info.addFieldInjection(new InjectionPoint(
                        InjectionPoint.Type.FIELD,
                        field.getSimpleName().toString(),
                        descriptor,
                        List.of(dep),
                        visibility));
                continue;
            }
            
            // Check for @Lookup annotation (dynamic bean lookup)
            Lookup lookupAnnotation = field.getAnnotation(Lookup.class);
            if (lookupAnnotation != null) {
                if (field.getModifiers().contains(Modifier.FINAL)) {
                    throw new ProcessingException("@Lookup cannot be applied to final fields: " + field.getSimpleName());
                }
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    throw new ProcessingException("@Lookup cannot be applied to static fields: " + field.getSimpleName());
                }
                
                String typeName = getTypeName(field.asType());
                String descriptor = getTypeDescriptor(field.asType());
                String lookupName = lookupAnnotation.value();
                boolean byType = lookupAnnotation.byType();
                boolean byName = lookupAnnotation.byName();
                boolean byQualifiedName = lookupAnnotation.byQualifiedName();
                boolean optional = lookupAnnotation.optional();
                
                // Determine lookup strategy
                String lookupStrategy = "BY_TYPE";
                if (byName) lookupStrategy = "BY_NAME";
                else if (byQualifiedName) lookupStrategy = "BY_QUALIFIED_NAME";
                else if (byType) lookupStrategy = "BY_TYPE";
                
                note("  -> @Lookup(" + lookupStrategy + (lookupName.isEmpty() ? "" : ", \"" + lookupName + "\"") + 
                     (optional ? ", optional" : "") + ") for field: " + field.getSimpleName());
                
                // Create dependency with lookup information
                Dependency dep = Dependency.forLookup(typeName, descriptor, lookupAnnotation);
                
                InjectionPoint.Visibility visibility = getFieldVisibility(field);
                info.addFieldInjection(new InjectionPoint(
                        InjectionPoint.Type.FIELD,
                        field.getSimpleName().toString(),
                        descriptor,
                        List.of(dep),
                        visibility));
                continue;
            }
            
            // Check for any @Inject annotation (Veld, javax.inject, or jakarta.inject)
            if (!AnnotationHelper.hasInjectAnnotation(field)) continue;
            
            InjectSource injectSource = AnnotationHelper.getInjectSource(field);
            
            if (field.getModifiers().contains(Modifier.FINAL)) {
                throw new ProcessingException("@Inject cannot be applied to final fields: " + field.getSimpleName());
            }
            if (field.getModifiers().contains(Modifier.STATIC)) {
                throw new ProcessingException("@Inject cannot be applied to static fields: " + field.getSimpleName());
            }
            
            InjectionPoint.Visibility visibility = getFieldVisibility(field);
            
            // Log which annotation specification is being used
            if (injectSource.isStandard()) {
                note("  -> Using " + injectSource.getPackageName() + ".Inject for field: " + field.getSimpleName());
            }
            
            if (visibility == InjectionPoint.Visibility.PRIVATE) {
                note("  -> Private field injection: " + field.getSimpleName() + " (requires accessor class)");
            }
            
            Dependency dep = createDependency(field);
            String descriptor = getTypeDescriptor(field.asType());
            
            info.addFieldInjection(new InjectionPoint(
                    InjectionPoint.Type.FIELD, 
                    field.getSimpleName().toString(),
                    descriptor,
                    List.of(dep),
                    visibility));
        }
    }
    
    /**
     * Determines the visibility of a field.
     */
    private InjectionPoint.Visibility getFieldVisibility(VariableElement field) {
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            return InjectionPoint.Visibility.PRIVATE;
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            return InjectionPoint.Visibility.PROTECTED;
        } else if (modifiers.contains(Modifier.PUBLIC)) {
            return InjectionPoint.Visibility.PUBLIC;
        } else {
            return InjectionPoint.Visibility.PACKAGE_PRIVATE;
        }
    }
    
    /**
     * Determines the visibility of a method.
     */
    private InjectionPoint.Visibility getMethodVisibility(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            return InjectionPoint.Visibility.PRIVATE;
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            return InjectionPoint.Visibility.PROTECTED;
        } else if (modifiers.contains(Modifier.PUBLIC)) {
            return InjectionPoint.Visibility.PUBLIC;
        } else {
            return InjectionPoint.Visibility.PACKAGE_PRIVATE;
        }
    }
    
    private void analyzeMethods(TypeElement typeElement, ComponentInfo info) throws ProcessingException {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            
            ExecutableElement method = (ExecutableElement) enclosed;
            
            // Check for any @Inject annotation (Veld, javax.inject, or jakarta.inject)
            if (!AnnotationHelper.hasInjectAnnotation(method)) continue;
            
            InjectSource injectSource = AnnotationHelper.getInjectSource(method);
            
            if (method.getModifiers().contains(Modifier.STATIC)) {
                throw new ProcessingException("@Inject cannot be applied to static methods: " + method.getSimpleName());
            }
            if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new ProcessingException("@Inject cannot be applied to abstract methods: " + method.getSimpleName());
            }
            
            // Get visibility for private method detection
            InjectionPoint.Visibility visibility = getMethodVisibility(method);
            
            // Log which annotation specification is being used
            if (injectSource.isStandard()) {
                note("  -> Using " + injectSource.getPackageName() + ".Inject for method: " + method.getSimpleName());
            }
            
            if (visibility == InjectionPoint.Visibility.PRIVATE) {
                note("  -> Private method injection: " + method.getSimpleName() + " (requires accessor class)");
            }
            
            List<Dependency> dependencies = new ArrayList<>();
            for (VariableElement param : method.getParameters()) {
                dependencies.add(createDependency(param));
            }
            
            String descriptor = getMethodDescriptor(method);
            info.addMethodInjection(new InjectionPoint(
                    InjectionPoint.Type.METHOD,
                    method.getSimpleName().toString(),
                    descriptor,
                    dependencies,
                    visibility));
        }
    }
    
    /**
     * Analyzes conditional annotations on the component using ConditionAnalyzer.
     * Supports @ConditionalOnProperty, @ConditionalOnClass, @ConditionalOnMissingBean,
     * @ConditionalOnBean, and @Profile.
     */
    private void analyzeConditions(TypeElement typeElement, ComponentInfo info) {
        ConditionInfo conditionInfo = conditionAnalyzer.analyze(typeElement);
        if (conditionInfo != null) {
            info.setConditionInfo(conditionInfo);
            
            // Track discovered profiles for compile-time class generation
            if (conditionInfo.hasProfileConditions()) {
                for (String profile : conditionInfo.getProfiles()) {
                    // Normalize profile name (remove negation prefix)
                    String normalized = profile.startsWith("!") ? profile.substring(1) : profile;
                    if (!normalized.isEmpty()) {
                        discoveredProfiles.add(normalized);
                    }
                }
            }
        }
    }
    
    /**
     * Analyzes lifecycle methods and explicit dependencies using LifecycleAnalyzer.
     */
    private void analyzeLifecycle(TypeElement typeElement, ComponentInfo info) {
        lifecycleAnalyzer.analyzeLifecycle(typeElement, info);
    }
    
    /**
     * Analyzes @DependsOn annotation for explicit initialization dependencies.
     */
    private void analyzeDependsOn(TypeElement typeElement, ComponentInfo info) {
        lifecycleAnalyzer.analyzeDependsOn(typeElement, info);
    }
    
    private Dependency createDependency(VariableElement element) {
        TypeMirror typeMirror = element.asType();
        String typeName = getTypeName(typeMirror);
        String typeDescriptor = getTypeDescriptor(typeMirror);
        
        // Use AnnotationHelper to get qualifier from any @Named annotation 
        // (Veld, javax.inject, or jakarta.inject)
        java.util.Optional<String> qualifier = AnnotationHelper.getQualifierValue(element);
        
        if (qualifier.isPresent()) {
            note("    -> Qualifier: @Named(\"" + qualifier.get() + "\")");
        }
        
        // Check for @Lazy annotation
        boolean isLazy = element.getAnnotation(Lazy.class) != null;
        if (isLazy) {
            note("    -> Lazy injection for: " + element.getSimpleName());
        }
        
        // Check for @Optional annotation
        boolean isOptional = element.getAnnotation(io.github.yasmramos.veld.annotation.Optional.class) != null;
        if (isOptional) {
            note("    -> Optional injection for: " + element.getSimpleName());
        }
        
        // Check if this is an Optional<T> type
        OptionalInfo optionalInfo = checkForOptionalWrapper(typeMirror);
        if (optionalInfo != null) {
            note("    -> Optional<T> wrapper injection for: " + optionalInfo.actualTypeName());
            return new Dependency(
                typeName, typeDescriptor, qualifier.orElse(null),
                false, isLazy, isOptional, true,
                optionalInfo.actualTypeName(), optionalInfo.actualTypeDescriptor()
            );
        }
        
        // Check if this is a Provider<T>
        ProviderInfo providerInfo = checkForProvider(typeMirror);
        if (providerInfo != null) {
            note("    -> Provider injection for: " + providerInfo.actualTypeName());
            return new Dependency(
                typeName, typeDescriptor, qualifier.orElse(null),
                true, isLazy, isOptional, false,
                providerInfo.actualTypeName(), providerInfo.actualTypeDescriptor()
            );
        }
        
        return new Dependency(typeName, typeDescriptor, qualifier.orElse(null),
                false, isLazy, isOptional, false, typeName, typeDescriptor);
    }
    
    /**
     * Checks if a type is a Provider (Veld, javax.inject, or jakarta.inject).
     * Returns the actual type T from Provider<T>, or null if not a Provider.
     */
    private ProviderInfo checkForProvider(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        DeclaredType declaredType = (DeclaredType) typeMirror;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String providerTypeName = typeElement.getQualifiedName().toString();
        
        // Check for any Provider type (Veld, javax.inject, jakarta.inject)
        boolean isProvider = 
            "io.github.yasmramos.veld.runtime.Provider".equals(providerTypeName) ||
            "javax.inject.Provider".equals(providerTypeName) ||
            "jakarta.inject.Provider".equals(providerTypeName);
        
        if (!isProvider) {
            return null;
        }
        
        // Get the type argument T from Provider<T>
        List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
        if (typeArgs.isEmpty()) {
            return null; // Raw Provider type, not supported
        }
        
        TypeMirror actualType = typeArgs.get(0);
        String actualTypeName = getTypeName(actualType);
        String actualTypeDescriptor = getTypeDescriptor(actualType);
        
        return new ProviderInfo(actualTypeName, actualTypeDescriptor);
    }
    
    private record ProviderInfo(String actualTypeName, String actualTypeDescriptor) {}
    
    /**
     * Checks if a type is java.util.Optional.
     * Returns the actual type T from Optional<T>, or null if not an Optional.
     */
    private OptionalInfo checkForOptionalWrapper(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        DeclaredType declaredType = (DeclaredType) typeMirror;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String optionalTypeName = typeElement.getQualifiedName().toString();
        
        // Check for java.util.Optional
        if (!"java.util.Optional".equals(optionalTypeName)) {
            return null;
        }
        
        // Get the type argument T from Optional<T>
        List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
        if (typeArgs.isEmpty()) {
            return null; // Raw Optional type, not supported
        }
        
        TypeMirror actualType = typeArgs.get(0);
        String actualTypeName = getTypeName(actualType);
        String actualTypeDescriptor = getTypeDescriptor(actualType);
        
        return new OptionalInfo(actualTypeName, actualTypeDescriptor);
    }
    
    private record OptionalInfo(String actualTypeName, String actualTypeDescriptor) {}
    
    private String getTypeName(TypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString();
        }
        return typeMirror.toString();
    }
    
    private String getTypeDescriptor(TypeMirror typeMirror) {
        switch (typeMirror.getKind()) {
            case BOOLEAN: return "Z";
            case BYTE: return "B";
            case SHORT: return "S";
            case INT: return "I";
            case LONG: return "J";
            case CHAR: return "C";
            case FLOAT: return "F";
            case DOUBLE: return "D";
            case VOID: return "V";
            case ARRAY:
                javax.lang.model.type.ArrayType arrayType = (javax.lang.model.type.ArrayType) typeMirror;
                return "[" + getTypeDescriptor(arrayType.getComponentType());
            case DECLARED:
                DeclaredType declaredType = (DeclaredType) typeMirror;
                TypeElement typeElement = (TypeElement) declaredType.asElement();
                String internalName = typeElement.getQualifiedName().toString().replace('.', '/');
                return "L" + internalName + ";";
            default:
                return "L" + typeMirror.toString().replace('.', '/') + ";";
        }
    }
    
    private String getMethodDescriptor(ExecutableElement method) {
        StringBuilder sb = new StringBuilder("(");
        for (VariableElement param : method.getParameters()) {
            sb.append(getTypeDescriptor(param.asType()));
        }
        sb.append(")");
        sb.append(getTypeDescriptor(method.getReturnType()));
        return sb.toString();
    }
    
    /**
     * Generates factories for all discovered components using the ComponentResolver
     * to properly resolve interface dependencies to their concrete implementations.
     * This is called after the discovery phase is complete.
     */
    /**
     * Gets a unique module identifier for metadata file naming.
     * Uses groupId:artifactId format if available, otherwise uses package name.
     */
    
    /**
     * Exports bean metadata for multi-module support.
     * Writes metadata to META-INF/veld/ for consumption by dependent modules.
     */
    
    /**
     * Checks if a component has any @Value injections.
     */
    private boolean hasValueInjection(ComponentInfo component) {
        // Check constructor injections
        InjectionPoint ctor = component.getConstructorInjection();
        if (ctor != null) {
            for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
                if (dep.isValueInjection()) {
                    return true;
                }
            }
        }
        
        // Check field injections
        for (InjectionPoint field : component.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                if (dep.isValueInjection()) {
                    return true;
                }
            }
        }
        
        // Check method injections
        for (InjectionPoint method : component.getMethodInjections()) {
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                if (dep.isValueInjection()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void generateRegistry() {
        try {
            // Generate AOP wrapper classes using SPI executor
            // Convert ComponentInfo to ComponentData for SPI compatibility
            Map<String, SpiAopExtensionExecutor.ComponentData> componentDataMap = 
                ProcessorToSpiConverter.toComponentDataMap(discoveredComponents);
            
            Map<String, String> aopClassMap = aopExtensionExecutor.generateAopClasses(componentDataMap);
            if (!aopClassMap.isEmpty()) {
                note("Generated " + aopClassMap.size() + " AOP wrapper classes");
                // Update ComponentInfo with AOP wrapper information
                for (ComponentInfo component : discoveredComponents) {
                    String aopClassName = aopClassMap.get(component.getClassName());
                    if (aopClassName != null) {
                        // Mark component as having AOP wrapper
                        component.setHasAopWrapper(true);
                    }
                }
            }
            // Factory metadata generation removed in static model
            // Build the full static dependency graph with ALL components
            // Build SPI graph and context for extensions
            this.spiGraph = extensionExecutor.buildGraph(discoveredComponents, typeUtils);
            this.spiContext = SpiExtensionExecutor.createContext(
                messager, elementUtils, typeUtils, null, filer,
                processingEnv.getOptions().keySet(), 
                processingEnv.getOptions().containsKey("veld.debug"));
            // Execute INIT phase extensions
            extensionExecutor.executeInitPhase(spiGraph, spiContext);
            // Build the internal VeldNode graph
            buildVeldNodeGraph();
            // Execute VALIDATION phase extensions
            extensionExecutor.executeValidationPhase(spiGraph, spiContext);
            // Execute ANALYSIS phase extensions
            extensionExecutor.executeAnalysisPhase(spiGraph, spiContext);
            // Execute GENERATION phase extensions (before generating code)
            extensionExecutor.executeGenerationPhase(spiGraph, spiContext);
            // Generate Veld class files based on discovered profiles
            generateVeldClasses();
            // Write component metadata for weaver
            writeComponentMetadata();
            note("Wrote component metadata for weaver (" + discoveredComponents.size() + " components)");
            // Generate EventRegistry for zero-reflection event registration
            generateEventRegistry();
            // Export bean metadata for multi-module support
        } catch (IOException e) {
            error(null, "Failed to generate Veld sources: " + e.getMessage());
        }
    }
    
    /**
     * Generates Veld class files based on discovered profiles.
     * For each unique profile found, generates Veld<Profile> class containing
     * only components applicable to that profile.
     * Also generates base Veld class for components without profile.
     * 
     * CRITICAL: Validates strict visibility rules:
     * - Components without profile are in Veld only
     * - Components with profile are in their specific VeldX class
     * - Cross-profile dependencies are NOT allowed (compile error)
     * - Duplicate implementations in same profile require @Named (compile error)
     */
    private void generateVeldClasses() {
        if (veldNodes.isEmpty()) {
            note("No components to generate for Veld");
            return;
        }
        
        String veldPackage = "io.github.yasmramos.veld";
        
        // Build map of profile -> nodes applicable to that profile
        Map<String, List<VeldNode>> profileNodes = new LinkedHashMap<>();
        
        // Initialize with empty lists for each discovered profile
        for (String profile : discoveredProfiles) {
            profileNodes.put(profile, new ArrayList<>());
        }
        
        // Classify each node by its profile
        for (VeldNode node : veldNodes) {
            List<String> nodeProfiles = getNodeProfiles(node);
            
            if (nodeProfiles.isEmpty()) {
                // No profile - goes to base Veld class only
            } else {
                // Has profiles - add to each applicable profile's class
                for (String profile : nodeProfiles) {
                    List<VeldNode> nodesForProfile = profileNodes.get(profile);
                    if (nodesForProfile != null) {
                        nodesForProfile.add(node);
                    }
                }
            }
        }
        
        // Validate each profile's graph for strict visibility rules
        boolean validationPassed = true;
        
        // Validate default Veld class
        List<VeldNode> defaultNodes = new ArrayList<>();
        for (VeldNode node : veldNodes) {
            if (getNodeProfiles(node).isEmpty()) {
                defaultNodes.add(node);
            }
        }
        validationPassed &= validateProfileGraph("Veld", defaultNodes, new HashSet<>());
        
        // Validate each profile-specific class
        for (String profile : discoveredProfiles) {
            List<VeldNode> profileNodeList = profileNodes.get(profile);
            if (profileNodeList != null && !profileNodeList.isEmpty()) {
                Set<String> availableTypes = buildAvailableTypes(profileNodeList);
                validationPassed &= validateProfileGraph(getVeldClassName(profile), profileNodeList, availableTypes);
                validationPassed &= validateNoDuplicateImplementations(profile, profileNodeList);
            }
        }
        
        if (!validationPassed) {
            error(null, "Compilation failed due to profile visibility violations. Fix the errors above.");
            return;
        }
        
        try {
            // Generate Veld class for components without profiles (base/default)
            if (!defaultNodes.isEmpty()) {
                generateVeldClass(veldPackage, "Veld", defaultNodes);
            } else {
                note("No components without profile - base Veld class will be minimal");
                generateVeldClass(veldPackage, "Veld", defaultNodes);
            }
            
            // Generate Veld<Profile> class for each discovered profile
            for (Map.Entry<String, List<VeldNode>> entry : profileNodes.entrySet()) {
                String profile = entry.getKey();
                List<VeldNode> profileNodeList = entry.getValue();
                
                if (!profileNodeList.isEmpty()) {
                    String className = getVeldClassName(profile);
                    generateVeldClass(veldPackage, className, profileNodeList);
                }
            }
            
            note("Generated " + (profileNodes.size() + 1) + " Veld class files");
            
            // Generate accessor classes for components with private field/method injection
            generateAccessorClasses();
            
        } catch (IOException e) {
            error(null, "Failed to generate Veld class files: " + e.getMessage());
        }
    }
    
    /**
     * Generates accessor classes for components with private field/method injection.
     * Each accessor class is placed in the same package as its target component.
     */
    private void generateAccessorClasses() {
        int accessorCount = 0;
        
        for (VeldNode node : veldNodes) {
            if (!node.needsAccessorClass()) {
                continue;
            }
            
            try {
                VeldSourceGenerator sourceGen = new VeldSourceGenerator(
                    List.of(node), messager, node.getPackageName(), node.getAccessorSimpleName());
                Map<String, com.squareup.javapoet.JavaFile> accessorFiles = sourceGen.generateAccessorClasses();
                
                for (com.squareup.javapoet.JavaFile accessorFile : accessorFiles.values()) {
                    writeJavaSource(accessorFile);
                    accessorCount++;
                    note("Generated accessor class: " + accessorFile.typeSpec.name);
                }
            } catch (Exception e) {
                error(null, "Failed to generate accessor class for " + node.getClassName() + ": " + e.getMessage());
            }
        }
        
        if (accessorCount > 0) {
            note("Generated " + accessorCount + " accessor class(es) for private member injection");
        }
    }
    
    /**
     * Validates that all dependencies in a profile's graph exist within that profile.
     * Components without profile (default) can only depend on other default components.
     * Components with profile can only depend on components with the SAME profile.
     * 
     * @param className the class being validated (for error messages)
     * @param nodes nodes in this profile's graph
     * @param availableTypes types that are available in this profile
     * @return true if validation passes
     */
    private boolean validateProfileGraph(String className, List<VeldNode> nodes, Set<String> availableTypes) {
        // Build set of all types available in this profile
        Set<String> available = new HashSet<>();
        Set<String> availableClassNames = new HashSet<>(); // Track class names specifically
        for (VeldNode node : nodes) {
            available.add(node.getClassName());
            available.add(node.getVeldName());
            availableClassNames.add(node.getClassName());
        }
        available.addAll(availableTypes);
        
        for (VeldNode node : nodes) {
            List<String> nodeProfiles = getNodeProfiles(node);
            boolean isDefault = nodeProfiles.isEmpty();
            String nodeProfile = isDefault ? null : nodeProfiles.get(0); // Get first profile for validation
            
            // Check constructor dependencies
            if (node.hasConstructorInjection()) {
                for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
                    if (!isValidDependency(param.getActualTypeName(), available, availableClassNames, isDefault, nodeProfile, param.isOptional())) {
                        error(null, 
                            "Profile visibility violation in " + className + ":\n" +
                            "  Component " + node.getClassName() + " depends on " + param.getActualTypeName() + "\n" +
                            "  " + (isDefault 
                                ? "Default components can only depend on other default components"
                                : "Profile-specific components can only depend on components with the SAME profile") + "\n" +
                            "  Fix: Ensure both components have the same @Profile annotation, " +
                            "or move the dependency to a default component.");
                        return false;
                    }
                }
            }
            
            // Check field dependencies
            for (VeldNode.FieldInjection field : node.getFieldInjections()) {
                if (!isValidDependency(field.getActualTypeName(), available, availableClassNames, isDefault, nodeProfile, field.isOptional())) {
                    error(null,
                        "Profile visibility violation in " + className + ":\n" +
                        "  Component " + node.getClassName() + " (field " + field.getFieldName() + ") depends on " + field.getActualTypeName() + "\n" +
                        "  " + (isDefault 
                            ? "Default components can only depend on other default components"
                            : "Profile-specific components can only depend on components with the SAME profile"));
                    return false;
                }
            }
            
            // Check method dependencies
            for (VeldNode.MethodInjection method : node.getMethodInjections()) {
                for (VeldNode.ParameterInfo param : method.getParameters()) {
                    if (!isValidDependency(param.getActualTypeName(), available, availableClassNames, isDefault, nodeProfile, param.isOptional())) {
                        error(null,
                            "Profile visibility violation in " + className + ":\n" +
                            "  Component " + node.getClassName() + " (method " + method.getMethodName() + ") depends on " + param.getActualTypeName() + "\n" +
                            "  " + (isDefault 
                                ? "Default components can only depend on other default components"
                                : "Profile-specific components can only depend on components with the SAME profile"));
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a dependency is valid within a profile's graph.
     * Default components can only depend on default components.
     * Profile components can only depend on components with the SAME profile.
     * Optional dependencies are always valid (they may not be available in all profiles).
     * 
     * @param dependencyType the type being depended upon
     * @param available types available in the current profile
     * @param availableClassNames class names available in the current profile
     * @param isDependentDefault whether the dependent component has no profile (is default)
     * @param dependentProfile the profile of the dependent component (null if default)
     * @param isOptional whether the dependency is marked as @Optional
     * @return true if the dependency is valid
     */
    private boolean isValidDependency(String dependencyType, Set<String> available, 
                                       Set<String> availableClassNames, boolean isDependentDefault,
                                       String dependentProfile, boolean isOptional) {
        // Optional dependencies are always valid - they may not be available in all profiles
        if (isOptional) {
            return true;
        }
        
        // If dependency is in available set, it exists in this profile
        if (available.contains(dependencyType)) {
            return true;
        }
        
        // Check if it's a well-known type that doesn't need registration
        if (isWellKnownType(dependencyType)) {
            return true;
        }
        
        // If dependency is not in available types and not well-known, check if it's another component
        // by looking up its profile in the global veldNodes list
        List<String> dependencyProfiles = getProfileForTypeFromNodes(dependencyType);
        
        // For default components: only default dependencies are allowed
        if (isDependentDefault) {
            // Default can only depend on other default components
            // If dependency has profiles, it's not a default component
            if (!dependencyProfiles.isEmpty()) {
                return false;
            }
            // If we get here, dependency doesn't exist as a component - that's a different error
            // handled elsewhere
            return true;
        }
        
        // For profile components: only same-profile dependencies are allowed
        // If dependency is not a component (no profiles), it can't be used
        if (dependencyProfiles.isEmpty()) {
            // Dependency doesn't exist as a component - return true, 
            // the main validation will catch unresolved dependencies
            return true;
        }
        
        // Both have profiles - check if they match
        // The dependent component is in 'dependentProfile', dependency must be in the same profile
        for (String depProfile : dependencyProfiles) {
            if (!depProfile.equals(dependentProfile)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets the profiles for a given type by looking it up in the global veldNodes list.
     * This provides access to profile information during validation.
     */
    private List<String> getProfileForTypeFromNodes(String typeName) {
        for (VeldNode node : veldNodes) {
            if (node.getClassName().equals(typeName)) {
                return getNodeProfiles(node);
            }
        }
        return new ArrayList<>();
    }
    
    /**
     * Gets the profiles for a given type by looking it up in the available class names.
     * @deprecated Use getProfileForTypeFromNodes instead for full profile access
     */
    private List<String> getProfileForType(String typeName, Set<String> availableClassNames) {
        return getProfileForTypeFromNodes(typeName);
    }
    
    /**
     * Checks if a type is a well-known type that doesn't need component registration.
     * This includes primitives, their wrappers, and common Java types.
     */
    private boolean isWellKnownType(String typeName) {
        // Primitive types
        if (typeName.equals("int") || typeName.equals("long") || typeName.equals("short") ||
            typeName.equals("byte") || typeName.equals("float") || typeName.equals("double") ||
            typeName.equals("char") || typeName.equals("boolean") || typeName.equals("void")) {
            return true;
        }
        // Primitive wrappers
        if (typeName.equals("java.lang.Integer") || typeName.equals("java.lang.Long") ||
            typeName.equals("java.lang.Short") || typeName.equals("java.lang.Byte") ||
            typeName.equals("java.lang.Float") || typeName.equals("java.lang.Double") ||
            typeName.equals("java.lang.Character") || typeName.equals("java.lang.Boolean") ||
            typeName.equals("java.lang.String")) {
            return true;
        }
        // Common Java types
        return typeName.startsWith("java.lang.") || 
               typeName.startsWith("java.util.");
    }
    
    /**
     * Validates that there are no duplicate implementations of the same interface
     * within the same profile without @Named qualifier.
     * 
     * @param profileName the profile being validated
     * @param nodes nodes in this profile
     * @return true if no conflicts found
     */
    private boolean validateNoDuplicateImplementations(String profileName, List<VeldNode> nodes) {
        // Build map of interface -> list of implementations
        Map<String, List<VeldNode>> interfaceImpls = new HashMap<>();
        
        for (VeldNode node : nodes) {
            // Get implemented interfaces from VeldNode's type element if available
            if (node.getTypeElement() != null) {
                for (javax.lang.model.type.TypeMirror iface : node.getTypeElement().getInterfaces()) {
                    if (iface.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                        javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) iface;
                        javax.lang.model.element.TypeElement ifaceElement = (javax.lang.model.element.TypeElement) declaredType.asElement();
                        String ifaceName = ifaceElement.getQualifiedName().toString();
                        
                        // Skip java.lang interfaces
                        if (ifaceName.startsWith("java.lang.")) {
                            continue;
                        }
                        
                        interfaceImpls.computeIfAbsent(ifaceName, k -> new ArrayList<>()).add(node);
                    }
                }
            }
        }
        
        // Check for conflicts
        for (Map.Entry<String, List<VeldNode>> entry : interfaceImpls.entrySet()) {
            String ifaceName = entry.getKey();
            List<VeldNode> impls = entry.getValue();
            
            if (impls.size() > 1) {
                // Multiple implementations - check if they have @Named qualifiers
                int withoutNamed = 0;
                VeldNode withoutNamedNode = null;
                
                for (VeldNode impl : impls) {
                    // Check if this implementation has a @Named qualifier
                    boolean hasNamed = false;
                    if (impl.getTypeElement() != null) {
                        io.github.yasmramos.veld.annotation.Named namedAnn = 
                            impl.getTypeElement().getAnnotation(io.github.yasmramos.veld.annotation.Named.class);
                        hasNamed = namedAnn != null && !namedAnn.value().isEmpty();
                        
                        // Also check jakarta.inject.Named and javax.inject.Named
                        if (!hasNamed) {
                            // Check javax.inject.Named via reflection
                            try {
                                Class<?> javaxNamedClass = Class.forName("javax.inject.Named");
                                Object annotation = impl.getTypeElement().getAnnotation(javaxNamedClass.asSubclass(java.lang.annotation.Annotation.class));
                                hasNamed = annotation != null;
                            } catch (ClassNotFoundException e) {
                                // javax.inject.Named not available
                            } catch (ClassCastException e) {
                                // Not an annotation
                            }
                        }
                    }
                    
                    if (!hasNamed) {
                        withoutNamed++;
                        withoutNamedNode = impl;
                    }
                }
                
                if (withoutNamed > 0) {
                    // Conflict: multiple implementations without @Named
                    StringBuilder implNames = new StringBuilder();
                    for (int i = 0; i < impls.size(); i++) {
                        if (i > 0) implNames.append(", ");
                        implNames.append(impls.get(i).getClassName());
                    }
                    
                    error(null,
                        "Duplicate implementation conflict in profile '" + profileName + "' for interface " + ifaceName + ":\n" +
                        "  Found " + impls.size() + " implementations: " + implNames.toString() + "\n" +
                        "  Multiple implementations of the same interface in the same profile require @Named qualifier.\n" +
                        "  Fix: Add @Named(\"uniqueName\") to disambiguate the implementations.");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Builds the set of available types from a list of nodes.
     */
    private Set<String> buildAvailableTypes(List<VeldNode> nodes) {
        Set<String> types = new HashSet<>();
        for (VeldNode node : nodes) {
            types.add(node.getClassName());
            types.add(node.getVeldName());
        }
        return types;
    }
    
    /**
     * Capitalizes the first letter of a string, but handles special case for "prod".
     * Naming convention:
     * - "prod" → "Veld" (NOT "VeldProd")
     * - "dev" → "VeldDev"
     * - "test" → "VeldTest"
     * - Any other profile → "Veld" + capitalized profile name
     */
    private String getVeldClassName(String profile) {
        if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
            return "Veld";
        }
        // Normalize to lowercase first, then capitalize only the first letter
        String normalized = profile.toLowerCase();
        return "Veld" + normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }
    
    /**
     * Gets the profiles applicable to a node.
     * Returns list of profile names from @Profile annotation.
     */
    private List<String> getNodeProfiles(VeldNode node) {
        List<String> profiles = new ArrayList<>();
        
        // Check condition info for profile conditions
        if (node.getConditionInfo() != null && node.getConditionInfo().hasProfileConditions()) {
            // Get profiles from condition info
            profiles.addAll(node.getConditionInfo().getProfiles());
        }
        
        return profiles;
    }
    
    /**
     * Generates a single Veld class file with the given nodes.
     */
    private void generateVeldClass(String packageName, String className, List<VeldNode> nodes) throws IOException {
        if (nodes.isEmpty() && "Veld".equals(className)) {
            // Generate minimal Veld class with no components
            VeldSourceGenerator generator = new VeldSourceGenerator(nodes, messager, packageName, className);
            JavaFile veldFile = generator.generate(packageName);
            if (veldFile != null) {
                writeJavaSource(veldFile);
                note("Generated minimal " + className + ".java (no components)");
            }
            return;
        }
        
        VeldSourceGenerator generator = new VeldSourceGenerator(nodes, messager, packageName, className);
        JavaFile veldFile = generator.generate(packageName);

        if (veldFile == null) {
            note(className + ".java generation aborted due to unresolved dependencies");
            return;
        }

        writeJavaSource(veldFile);
        note("Generated " + className + ".java with " + nodes.size() + " components");
    }
    /**
     * Generates the EventRegistry implementation for zero-reflection event registration.
     */
    private void generateEventRegistry() {
        if (eventSubscriptions.isEmpty()) {
            note("No @Subscribe methods found - skipping EventRegistry generation");
            return;
        }

        try {
            EventRegistryGenerator generator = new EventRegistryGenerator(eventSubscriptions);
            JavaFile javaFile = generator.generate();
            writeJavaSource(javaFile);
            note("Generated EventRegistry with " + eventSubscriptions.size() + " event handlers");
        } catch (IOException e) {
            error(null, "Failed to generate EventRegistry: " + e.getMessage());
        }
    }
    
    
    /**
     * Writes component metadata to a file that the weaver will read.
     * Format: One component per line with fields separated by ||
     */
    private void writeComponentMetadata() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Veld Component Metadata - DO NOT EDIT\n");
        sb.append("# Generated by VeldProcessor\n");
        sb.append("# Format: className||scope||lazy||constructorDeps||fieldInjections||methodInjections||interfaces||postConstruct||preDestroy||hasSubscribeMethods||explicitDependencies||componentName\n");
        
        for (ComponentInfo comp : discoveredComponents) {
            sb.append(serializeComponent(comp)).append("\n");
        }
        
        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", 
            "META-INF/veld/components.meta");
        try (Writer writer = file.openWriter()) {
            writer.write(sb.toString());
        }
    }
    
    private String serializeComponent(ComponentInfo comp) {
        StringBuilder sb = new StringBuilder();
        
        // className
        sb.append(comp.getClassName()).append("||");
        
        // scope
        sb.append(comp.getScope()).append("||");
        
        // lazy
        sb.append(comp.isLazy()).append("||");
        
        // constructor dependencies: type1,type2,type3
        InjectionPoint ctor = comp.getConstructorInjection();
        if (ctor != null && !ctor.getDependencies().isEmpty()) {
            List<String> deps = new ArrayList<>();
            for (InjectionPoint.Dependency dep : ctor.getDependencies()) {
                deps.add(dep.getTypeName());
            }
            sb.append(String.join(",", deps));
        }
        sb.append("||");
        
        // field injections: fieldName~actualType~descriptor~visibility~isOptional~isProvider;...
        List<String> fields = new ArrayList<>();
        for (InjectionPoint field : comp.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                // Use actualTypeName for Optional<T>/Provider<T>, otherwise use typeName
                String actualType = dep.getActualTypeName();
                fields.add(field.getName() + "~" + actualType + "~" + 
                    field.getDescriptor() + "~" + field.getVisibility().name() + "~" +
                    dep.isOptionalWrapper() + "~" + dep.isProvider());
            }
        }
        sb.append(String.join("@", fields)).append("||");
        
        // method injections: methodName~descriptor~dep1,dep2;...
        List<String> methods = new ArrayList<>();
        for (InjectionPoint method : comp.getMethodInjections()) {
            List<String> deps = new ArrayList<>();
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                deps.add(dep.getTypeName());
            }
            methods.add(method.getName() + "~" + method.getDescriptor() + "~" + String.join(",", deps));
        }
        sb.append(String.join("@", methods)).append("||");
        
        // interfaces: iface1,iface2
        sb.append(String.join(",", comp.getImplementedInterfaces())).append("||");
        
        // postConstruct: methodName~descriptor (or empty)
        if (comp.hasPostConstruct()) {
            sb.append(comp.getPostConstructMethod()).append("~").append(comp.getPostConstructDescriptor());
        }
        sb.append("||");
        
        // preDestroy: methodName~descriptor (or empty)
        if (comp.hasPreDestroy()) {
            sb.append(comp.getPreDestroyMethod()).append("~").append(comp.getPreDestroyDescriptor());
        }
        sb.append("||");
        
        // hasSubscribeMethods: true/false
        sb.append(comp.hasSubscribeMethods()).append("||");
        
        // explicitDependencies: bean1,bean2 (from @DependsOn)
        if (comp.hasExplicitDependencies()) {
            sb.append(String.join(",", comp.getExplicitDependencies()));
        }
        sb.append("||");
        
        // componentName (for @Named lookup)
        sb.append(comp.getComponentName());
        
        return sb.toString();
    }
    
    private void writeJavaSource(String className, String sourceCode) throws IOException {
        try {
            JavaFileObject sourceFile = filer.createSourceFile(className);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(sourceCode);
            }
        } catch (javax.annotation.processing.FilerException e) {
            // File already exists (e.g., manual Veld.java for benchmarks) - skip
            note("Skipping " + className + " generation - file already exists");
        }
    }

    private void writeJavaSource(JavaFile javaFile) throws IOException {
        try {
            javaFile.writeTo(filer);
        } catch (javax.annotation.processing.FilerException e) {
            // File already exists - skip
            note("Skipping " + javaFile.typeSpec.name + " generation - file already exists");
        }
    }
    
    private void writeClassFile(String className, byte[] bytecode) throws IOException {
        String resourcePath = className.replace('.', '/') + ".class";
        FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);
        try (OutputStream os = fileObject.openOutputStream()) {
            os.write(bytecode);
        }
    }
    
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
    
    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "[Veld] " + message, element);
    }

    /**
     * Reports a warning, or an error if strict mode is enabled.
     * In strict mode, all warnings are treated as compilation errors.
     */
    private void warning(Element element, String message) {
        Diagnostic.Kind kind = options.isStrictMode() ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        String prefix = options.isStrictMode() ? "[Veld STRICT]" : "[Veld]";
        messager.printMessage(kind, prefix + " " + message, element);
    }

    private void note(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[Veld] " + message);
    }
    
    /**
     * Logs a debug message. Only shown when debug mode is enabled (-Aveld.debug=true).
     */
    private void debug(String message) {
        if (options.isDebugEnabled()) {
            messager.printMessage(Diagnostic.Kind.NOTE, "[Veld DEBUG] " + message);
        }
    }
    
    /**
     * Analyzes methods annotated with @Subscribe for EventBus registration.
     */
    private void analyzeEventSubscribers(TypeElement typeElement, ComponentInfo info) {
        TypeElement subscribeAnnotation = elementUtils.getTypeElement("io.github.yasmramos.veld.annotation.Subscribe");
        if (subscribeAnnotation == null) {
            return; // @Subscribe annotation not available
        }

        String componentClassName = typeElement.getQualifiedName().toString();
        String componentSimpleName = typeElement.getSimpleName().toString();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;

            // Check for @Subscribe annotation
            for (AnnotationMirror annotation : method.getAnnotationMirrors()) {
                String annotationName = annotation.getAnnotationType().asElement().toString();
                if (annotationName.equals("io.github.yasmramos.veld.annotation.Subscribe")) {
                    info.setHasSubscribeMethods(true);

                    // Validate method signature
                    List<? extends VariableElement> params = method.getParameters();
                    if (params.size() != 1) {
                        error(method, "@Subscribe method must have exactly one parameter");
                        continue;
                    }

                    // Get event type
                    VariableElement param = params.get(0);
                    String eventTypeName = getTypeName(param.asType());

                    // Get annotation values
                    io.github.yasmramos.veld.annotation.Subscribe subAnn =
                        method.getAnnotation(io.github.yasmramos.veld.annotation.Subscribe.class);

                    int eventId = EventRegistryGenerator.computeEventId(eventTypeName);
                    boolean async = subAnn != null && subAnn.async();
                    int priority = subAnn != null ? subAnn.priority() : 0;

                    // Collect subscription info for code generation
                    EventRegistryGenerator.SubscriptionInfo subscription =
                        new EventRegistryGenerator.SubscriptionInfo(
                            componentClassName,
                            componentSimpleName,
                            method.getSimpleName().toString(),
                            eventTypeName,
                            eventId,
                            async,
                            priority
                        );
                    eventSubscriptions.add(subscription);

                    note("  -> EventBus subscriber: " + method.getSimpleName() +
                         " (eventId=" + eventId + ", async=" + async + ")");
                }
            }
        }
    }

    // =========================================================================
    // Static Dependency Graph Methods
    // =========================================================================

    /**
     * Converts all discovered components to VeldNodes and builds the dependency graph.
     * This is called after all components have been discovered and validated.
     */
    private void buildVeldNodeGraph() {
        // Clear any existing nodes
        veldNodes.clear();

        // Create VeldNode for each ComponentInfo with full dependency info
        for (ComponentInfo comp : discoveredComponents) {
            VeldNode node = convertToVeldNode(comp);
            veldNodes.add(node);
            note("VeldNode: " + node.getVeldName() + " (" + node.getClassName() + ") -> " + node.getScope() +
                 (node.hasConstructorInjection() ? " (ctor params: " + node.getConstructorInfo().getParameterCount() + ")" : ""));
        }

        note("Built static dependency graph with " + veldNodes.size() + " nodes");
    }
    /**
     * Converts a ComponentInfo to a VeldNode with full injection information.
     */
    private VeldNode convertToVeldNode(ComponentInfo info) {
        VeldNode node = new VeldNode(
            info.getClassName(),
            info.getComponentName(),
            info.getScope()
        );
        node.setTypeElement(info.getTypeElement());

        // Convert constructor injection
        if (info.getConstructorInjection() != null) {
            VeldNode.ConstructorInfo ctorInfo = new VeldNode.ConstructorInfo();
            for (InjectionPoint.Dependency dep : info.getConstructorInjection().getDependencies()) {
                VeldNode.ParameterInfo paramInfo = convertDependencyToParameter(dep);
                ctorInfo.addParameter(paramInfo);
            }
            node.setConstructorInfo(ctorInfo);
        }

        // Convert field injections - pass visibility info
        for (InjectionPoint field : info.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                boolean isPrivate = field.getVisibility() == InjectionPoint.Visibility.PRIVATE;
                boolean isPublic = field.getVisibility() == InjectionPoint.Visibility.PUBLIC;
                VeldNode.FieldInjection fieldInjection = new VeldNode.FieldInjection(
                    field.getName(),
                    dep.getTypeName(),
                    dep.isProvider(),
                    dep.isOptional(),
                    dep.isOptionalWrapper(),
                    dep.getActualTypeName(),
                    dep.getQualifierName(),
                    isPrivate,
                    isPublic,
                    dep.isValueInjection()
                );
                node.addFieldInjection(fieldInjection);
            }
        }

        // Convert method injections - pass isPrivate flag
        for (InjectionPoint method : info.getMethodInjections()) {
            boolean isPrivate = method.getVisibility() == InjectionPoint.Visibility.PRIVATE;
            VeldNode.MethodInjection methodInjection = new VeldNode.MethodInjection(method.getName(), isPrivate);
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                VeldNode.ParameterInfo paramInfo = convertDependencyToParameter(dep);
                methodInjection.addParameter(paramInfo);
            }
            node.addMethodInjection(methodInjection);
        }

        // Convert lifecycle methods
        if (info.hasPostConstruct()) {
            node.setPostConstruct(info.getPostConstructMethod());
        }
        if (info.hasPreDestroy()) {
            node.setPreDestroy(info.getPreDestroyMethod());
        }
        
        // Convert condition info (for profile-based filtering)
        if (info.getConditionInfo() != null) {
            node.setConditionInfo(info.getConditionInfo());
        }

        // Set lazy initialization flag
        node.setLazy(info.isLazy());

        // Transfer AOP interceptors
        if (info.hasAopInterceptors()) {
            for (String interceptor : info.getAopInterceptors()) {
                node.addAopInterceptor(interceptor);
            }
        }

        // Transfer AOP wrapper flag (set during AOP class generation)
        if (info.hasAopWrapper()) {
            node.setHasAopWrapper(true);
        }

        // Auto-closeable detection: Check if the component type implements AutoCloseable
        TypeElement typeElement = info.getTypeElement();
        if (typeElement != null) {
            TypeMirror componentType = typeElement.asType();
            TypeElement autoCloseableType = elementUtils.getTypeElement("java.lang.AutoCloseable");
            if (autoCloseableType != null && typeUtils.isSubtype(componentType, autoCloseableType.asType())) {
                node.setAutoCloseable(true);
                note("  -> AutoCloseable bean detected");
            }
        }

        return node;
    }
    /**
     * Converts an InjectionPoint.Dependency to a VeldNode.ParameterInfo.
     */
    private VeldNode.ParameterInfo convertDependencyToParameter(InjectionPoint.Dependency dep) {
        return new VeldNode.ParameterInfo(
            dep.getTypeName(),
            "param_" + dep.getTypeName().substring(dep.getTypeName().lastIndexOf('.') + 1).toLowerCase(),
            dep.isProvider(),
            dep.isOptional(),
            dep.isOptionalWrapper(),
            dep.getActualTypeName(),
            dep.getQualifierName(),
            dep.getValueExpression()
        );
    }
    /**
     * Generates Veld.java using the static dependency graph.
     * This replaces the old factory-based generation.
     */
    private void generateVeld() {
        // This method is now replaced by generateVeldClasses()
        note("generateVeld() is deprecated - use generateVeldClasses() instead");
    }
    /**
     * Determines the package for Veld.java based on discovered components.
     * Uses the package of the first component.
     */
    private String determineVeldPackage() {
        if (discoveredComponents.isEmpty()) {
            return "io.github.yasmramos.veld.generated";
        }

        // Simply use the package of the first component
        String pkg = getPackageName(discoveredComponents.get(0).getClassName());
        note("Veld package determined: " + pkg);
        
        return pkg.isEmpty() ? "io.github.yasmramos.veld.generated" : pkg;
    }
    /**
     * Gets the package name from a fully qualified class name.
     */
    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    private static class ProcessingException extends Exception {
        ProcessingException(String message) {
            super(message);
        }
    }
}
