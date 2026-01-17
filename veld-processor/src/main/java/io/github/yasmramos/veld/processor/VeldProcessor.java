package io.github.yasmramos.veld.processor;

import com.squareup.javapoet.JavaFile;
import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.processor.AnnotationHelper.InjectSource;
import io.github.yasmramos.veld.processor.InjectionPoint.Dependency;
import io.github.yasmramos.veld.processor.spi.SpiExtensionExecutor;
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
    "io.github.yasmramos.veld.annotation.DependsOn",
    "io.github.yasmramos.veld.annotation.Primary",
    "io.github.yasmramos.veld.annotation.Order",
    "io.github.yasmramos.veld.annotation.Qualifier",
    "io.github.yasmramos.veld.annotation.Lookup",
    "io.github.yasmramos.veld.annotation.Profile",
    "javax.inject.Singleton",
    "jakarta.inject.Singleton"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class VeldProcessor extends AbstractProcessor {
    
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;
    
    private final List<ComponentInfo> discoveredComponents = new ArrayList<>();
    private final DependencyGraph dependencyGraph = new DependencyGraph();
    
    // External beans loaded from classpath (multi-module support)
    private final List<BeanMetadataReader.ExternalBeanInfo> externalBeans = new ArrayList<>();
    
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
    private VeldGraph spiGraph;
    private VeldProcessingContext spiContext;
    

    // Profile setting for compile-time class name generation
    private String profile = "prod";

    public VeldProcessor() {
    }
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        
        // Read profile compiler option for compile-time class generation
        // Usage: -Aveld.profile=dev | -Aveld.profile=test | -Aveld.profile=prod
        Map<String, String> options = processingEnv.getOptions();
        this.profile = options.getOrDefault("veld.profile", "prod").toLowerCase();
        note("Veld profile: " + profile + " (compile-time class generation)");
        
        // Initialize SPI Extension Executor
        boolean extensionsEnabled = !options.containsKey("veld.extensions.disabled");
        this.extensionExecutor = new SpiExtensionExecutor(extensionsEnabled);
        
        if (extensionExecutor.hasExtensions()) {
            note("SPI Extensions loaded: " + extensionExecutor.getExtensionCount());
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
        identifyUnresolvedInterfaceDependencies();

        return true;
    }
    /**
     * Identifies dependencies that are interfaces without implementing components.
     * These are tracked for runtime resolution (e.g., via mocks in tests).
     */
    private void identifyUnresolvedInterfaceDependencies() {
        // Build a set of all concrete component class names
        Set<String> componentClasses = new HashSet<>();
        for (ComponentInfo comp : discoveredComponents) {
            componentClasses.add(comp.getClassName());
        }

        // Check each component's dependencies
        for (ComponentInfo comp : discoveredComponents) {
            // Check constructor dependencies
            if (comp.getConstructorInjection() != null) {
                for (InjectionPoint.Dependency dep : comp.getConstructorInjection().getDependencies()) {
                    checkAndAddUnresolvedDependency(comp, dep.getActualTypeName(), componentClasses);
                }
            }

            // Check field dependencies
            for (InjectionPoint field : comp.getFieldInjections()) {
                for (InjectionPoint.Dependency dep : field.getDependencies()) {
                    checkAndAddUnresolvedDependency(comp, dep.getActualTypeName(), componentClasses);
                }
            }

            // Check method dependencies
            for (InjectionPoint method : comp.getMethodInjections()) {
                for (InjectionPoint.Dependency dep : method.getDependencies()) {
                    checkAndAddUnresolvedDependency(comp, dep.getActualTypeName(), componentClasses);
                }
            }
        }

        // Report unresolved dependencies
        int totalUnresolved = discoveredComponents.stream()
            .mapToInt(c -> c.getUnresolvedInterfaceDependencies().size())
            .sum();
        if (totalUnresolved > 0) {
            note("Found " + totalUnresolved + " unresolved interface dependencies (will be resolved at runtime)");
        }
    }
    /**
     * Checks if a dependency type is an interface without a component implementation,
     * and adds it to the unresolved list if so.
     */
    private void checkAndAddUnresolvedDependency(ComponentInfo component, String dependencyType, 
                                                  Set<String> componentClasses) {
        // Skip if already in unresolved list
        if (component.getUnresolvedInterfaceDependencies().contains(dependencyType)) {
            return;
        }

        // Skip if this is the component itself (self-reference)
        if (dependencyType.equals(component.getClassName())) {
            return;
        }

        // Check if dependency type is an interface without an implementing component
        TypeElement depElement = elementUtils.getTypeElement(dependencyType);
        if (depElement != null && depElement.getKind() == ElementKind.INTERFACE) {
            // It's an interface - check if any component implements it
            boolean hasImplementation = false;
            for (String compClass : componentClasses) {
                TypeElement compElement = elementUtils.getTypeElement(compClass);
                if (compElement != null) {
                    // Check if this component implements the interface
                    for (TypeMirror iface : compElement.getInterfaces()) {
                        if (iface.toString().equals(dependencyType)) {
                            hasImplementation = true;
                            break;
                        }
                    }
                    // Also check via type utilities for super interfaces
                    if (!hasImplementation && typeUtils.isAssignable(compElement.asType(), depElement.asType())) {
                        hasImplementation = true;
                    }
                }
            }

            if (!hasImplementation) {
                component.addUnresolvedInterfaceDependency(dependencyType);
                note("  -> Unresolved interface dependency: " + dependencyType + 
                     " (for component: " + component.getClassName() + ")");
            }
        }
    }
    
    
    
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
                    warning(null, "Could not resolve bean name '" + beanName + 
                           "' in @DependsOn for component " + componentName + 
                           ". The bean may not exist or may be in a different module.");
                }
            }
        }
    }
    
    /**
     * Resolves a bean name to its corresponding type name.
     * 
     * @param beanName the bean name to resolve
     * @return the fully qualified type name, or null if not found
     */
    private String resolveBeanNameToType(String beanName) {
        // First try to find by component name (@Component value or @Named value)
        for (ComponentInfo component : discoveredComponents) {
            if (beanName.equals(component.getComponentName())) {
                return component.getClassName();
            }
        }
        
        // Then try to find by simple class name (lowercase first letter)
        String simpleClassName = decapitalize(beanName);
        for (ComponentInfo component : discoveredComponents) {
            String componentSimpleName = component.getClassName();
            int lastDot = componentSimpleName.lastIndexOf('.');
            if (lastDot >= 0) {
                componentSimpleName = componentSimpleName.substring(lastDot + 1);
            }
            if (simpleClassName.equals(componentSimpleName)) {
                return component.getClassName();
            }
        }
        
        // Finally try to find by full qualified class name
        for (ComponentInfo component : discoveredComponents) {
            if (beanName.equals(component.getClassName())) {
                return component.getClassName();
            }
        }
        
        return null;
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
        ScopeType scope = determineScope(typeElement);
        boolean isLazy = typeElement.getAnnotation(Lazy.class) != null;
        
        // Check for @Primary annotation
        boolean isPrimary = typeElement.getAnnotation(Primary.class) != null;
        if (isPrimary) {
            note("  -> Primary bean selected");
        }
        
        ComponentInfo info = new ComponentInfo(className, componentName, scope, null, isLazy, isPrimary);
        
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
     * All other scope annotations (Veld @Singleton, javax/jakarta @Singleton) result in SINGLETON.
     * Default is SINGLETON if no explicit scope is specified.
     */
    private ScopeType determineScope(TypeElement typeElement) {
        // Check for @Prototype first - it's the only way to get prototype scope
        if (typeElement.getAnnotation(Prototype.class) != null) {
            note("  -> Scope: PROTOTYPE");
            return ScopeType.PROTOTYPE;
        }
        
        // Check for @RequestScoped
        if (typeElement.getAnnotation(io.github.yasmramos.veld.annotation.RequestScoped.class) != null) {
            note("  -> Scope: REQUEST");
            return ScopeType.REQUEST;
        }
        
        // Check for @SessionScoped
        if (typeElement.getAnnotation(io.github.yasmramos.veld.annotation.SessionScoped.class) != null) {
            note("  -> Scope: SESSION");
            return ScopeType.SESSION;
        }
        
        // Check for explicit singleton annotations
        if (typeElement.getAnnotation(Singleton.class) != null ||
            AnnotationHelper.hasSingletonAnnotation(typeElement)) {
            note("  -> Scope: SINGLETON (explicit)");
            return ScopeType.SINGLETON;
        }
        
        // Check for @Lazy alone (implies singleton)
        if (typeElement.getAnnotation(Lazy.class) != null) {
            note("  -> Scope: SINGLETON (from @Lazy)");
            return ScopeType.SINGLETON;
        }
        
        // Default scope
        note("  -> Scope: SINGLETON (default)");
        return ScopeType.SINGLETON;
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
     * Multiple implementations of the same interface require @Named to disambiguate.
     * 
     * @return true if validation passes (with possible warnings), false if critical errors
     */
    private boolean validateInterfaceImplementations() {
        boolean hasConflicts = false;
        
        for (Map.Entry<String, List<String>> entry : interfaceImplementors.entrySet()) {
            String interfaceName = entry.getKey();
            List<String> implementors = entry.getValue();
            
            if (implementors.size() > 1) {
                // Multiple implementations - warn the user
                StringBuilder sb = new StringBuilder();
                sb.append("Multiple implementations found for interface: ")
                  .append(interfaceName)
                  .append("\n  Implementations: ");
                for (int i = 0; i < implementors.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(implementors.get(i));
                }
                sb.append("\n  Use @Named to disambiguate when injecting this interface.");
                sb.append("\n  Without @Named, the last registered implementation will be used: ")
                  .append(implementors.get(implementors.size() - 1));
                
                warning(null, sb.toString());
                hasConflicts = true;
            }
        }
        
        if (hasConflicts) {
            note("Interface conflict detection complete. Use @Named for explicit selection.");
        }
        
        return true; // Warnings don't stop compilation
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
                note("  -> Private field injection: " + field.getSimpleName() + " (requires veld-weaver plugin)");
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
            
            // Log which annotation specification is being used
            if (injectSource.isStandard()) {
                note("  -> Using " + injectSource.getPackageName() + ".Inject for method: " + method.getSimpleName());
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
                    dependencies));
        }
    }
    
    /**
     * Analyzes conditional annotations on the component.
     * Supports @ConditionalOnProperty, @ConditionalOnClass, @ConditionalOnMissingBean,
     * @ConditionalOnBean, and @Profile.
     */
    private void analyzeConditions(TypeElement typeElement, ComponentInfo info) {
        ConditionInfo conditionInfo = new ConditionInfo();
        
        try {
            // Check for @ConditionalOnProperty
            ConditionalOnProperty propertyCondition = typeElement.getAnnotation(ConditionalOnProperty.class);
            if (propertyCondition != null) {
                try {
                    conditionInfo.addPropertyCondition(
                        propertyCondition.name(),
                        propertyCondition.havingValue(),
                        propertyCondition.matchIfMissing()
                    );
                    note("  -> Conditional on property: " + propertyCondition.name());
                } catch (Exception e) {
                    warning(null, "Could not process @ConditionalOnProperty annotation: " + e.getMessage());
                }
            }
            
            // Check for @ConditionalOnClass
            ConditionalOnClass classCondition = typeElement.getAnnotation(ConditionalOnClass.class);
            if (classCondition != null) {
                List<String> classNames = new ArrayList<>();
                
                try {
                    // Get class names from 'name' attribute
                    for (String name : classCondition.name()) {
                        if (!name.isEmpty()) {
                            classNames.add(name);
                        }
                    }
                    
                    // Get class names from 'value' attribute (Class[] types)
                    // We need to handle this carefully due to MirroredTypeException
                    for (Class<?> clazz : classCondition.value()) {
                        classNames.add(clazz.getName());
                    }
                } catch (javax.lang.model.type.MirroredTypesException e) {
                    for (TypeMirror mirror : e.getTypeMirrors()) {
                        try {
                            String typeName = getTypeName(mirror);
                            if (typeName != null && !typeName.isEmpty()) {
                                classNames.add(typeName);
                            }
                        } catch (Exception ex) {
                            // Handle unresolved type mirrors gracefully
                            warning(null, "Could not resolve type from conditional annotation: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    warning(null, "Could not process @ConditionalOnClass annotation: " + e.getMessage());
                }
                
                if (!classNames.isEmpty()) {
                    conditionInfo.addClassCondition(classNames);
                    note("  -> Conditional on class: " + String.join(", ", classNames));
                }
            }
            
            // Check for @ConditionalOnMissingBean
            ConditionalOnMissingBean missingBeanCondition = typeElement.getAnnotation(ConditionalOnMissingBean.class);
            if (missingBeanCondition != null) {
                List<String> beanTypes = new ArrayList<>();
                List<String> beanNames = new ArrayList<>();
                
                try {
                    // Get bean types
                    for (Class<?> clazz : missingBeanCondition.value()) {
                        beanTypes.add(clazz.getName());
                    }
                } catch (javax.lang.model.type.MirroredTypesException e) {
                    for (TypeMirror mirror : e.getTypeMirrors()) {
                        try {
                            String typeName = getTypeName(mirror);
                            if (typeName != null && !typeName.isEmpty()) {
                                beanTypes.add(typeName);
                            }
                        } catch (Exception ex) {
                            // Handle unresolved type mirrors gracefully
                            warning(null, "Could not resolve type from conditional annotation: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    warning(null, "Could not process @ConditionalOnMissingBean annotation types: " + e.getMessage());
                }
                
                try {
                    // Get bean names
                    for (String name : missingBeanCondition.name()) {
                        if (!name.isEmpty()) {
                            beanNames.add(name);
                        }
                    }
                } catch (Exception e) {
                    warning(null, "Could not process @ConditionalOnMissingBean annotation names: " + e.getMessage());
                }
                
                if (!beanTypes.isEmpty()) {
                    conditionInfo.addMissingBeanTypeCondition(beanTypes);
                    note("  -> Conditional on missing bean types: " + String.join(", ", beanTypes));
                }
                if (!beanNames.isEmpty()) {
                    conditionInfo.addMissingBeanNameCondition(beanNames);
                    note("  -> Conditional on missing bean names: " + String.join(", ", beanNames));
                }
            }
            
            // Check for @ConditionalOnBean
            ConditionalOnBean presentBeanCondition = typeElement.getAnnotation(ConditionalOnBean.class);
            if (presentBeanCondition != null) {
                List<String> presentBeanTypes = new ArrayList<>();
                List<String> presentBeanNames = new ArrayList<>();
                boolean matchAll = presentBeanCondition.strategy() == ConditionalOnBean.Strategy.ALL;
                
                try {
                    // Get bean types
                    for (Class<?> clazz : presentBeanCondition.value()) {
                        presentBeanTypes.add(clazz.getName());
                    }
                } catch (javax.lang.model.type.MirroredTypesException e) {
                    for (TypeMirror mirror : e.getTypeMirrors()) {
                        try {
                            String typeName = getTypeName(mirror);
                            if (typeName != null && !typeName.isEmpty()) {
                                presentBeanTypes.add(typeName);
                            }
                        } catch (Exception ex) {
                            warning(null, "Could not resolve type from @ConditionalOnBean: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    warning(null, "Could not process @ConditionalOnBean annotation types: " + e.getMessage());
                }
                
                try {
                    // Get bean names
                    for (String name : presentBeanCondition.name()) {
                        if (!name.isEmpty()) {
                            presentBeanNames.add(name);
                        }
                    }
                } catch (Exception e) {
                    warning(null, "Could not process @ConditionalOnBean annotation names: " + e.getMessage());
                }
                
                if (!presentBeanTypes.isEmpty() || !presentBeanNames.isEmpty()) {
                    conditionInfo.addPresentBeanCondition(presentBeanTypes, presentBeanNames, matchAll);
                    String strategy = matchAll ? "ALL" : "ANY";
                    note("  -> Conditional on present beans (strategy=" + strategy + "): " + 
                         String.join(", ", presentBeanTypes.isEmpty() ? presentBeanNames : presentBeanTypes));
                }
            }
            
            // Check for @Profile
            Profile profileAnnotation = typeElement.getAnnotation(Profile.class);
            if (profileAnnotation != null) {
                try {
                    List<String> profiles = new ArrayList<>();
                    for (String profile : profileAnnotation.value()) {
                        if (!profile.isEmpty()) {
                            profiles.add(profile);
                        }
                    }
                    
                    // Also check 'name' attribute as alias
                    if (profiles.isEmpty()) {
                        String nameValue = profileAnnotation.name();
                        if (!nameValue.isEmpty() && !nameValue.isEmpty()) {
                            profiles.add(nameValue);
                        }
                    }
                    
                    // Get expression and strategy
                    String expression = profileAnnotation.expression();
                    Profile.MatchStrategy strategy = profileAnnotation.strategy();
                    
                    if (!profiles.isEmpty() || !expression.isEmpty()) {
                        conditionInfo.addProfileCondition(profiles, expression, strategy);
                        
                        // Track discovered profiles for compile-time class generation
                        for (String profile : profiles) {
                            // Normalize profile name (remove negation prefix)
                            String normalized = profile.startsWith("!") ? profile.substring(1) : profile;
                            if (!normalized.isEmpty()) {
                                discoveredProfiles.add(normalized);
                            }
                        }
                        
                        StringBuilder profileNote = new StringBuilder("  -> Profile: ");
                        if (!profiles.isEmpty()) {
                            profileNote.append(String.join(", ", profiles));
                        }
                        if (!expression.isEmpty()) {
                            profileNote.append(" [expression: ").append(expression).append("]");
                        }
                        if (strategy != Profile.MatchStrategy.ALL) {
                            profileNote.append(" [strategy: ").append(strategy).append("]");
                        }
                        note(profileNote.toString());
                    }
                } catch (Exception e) {
                    warning(null, "Could not process @Profile annotation: " + e.getMessage());
                }
            }
            
            if (conditionInfo.hasConditions()) {
                info.setConditionInfo(conditionInfo);
            }
        } catch (Exception e) {
            // Catch any unexpected errors in conditional annotation processing
            warning(null, "Error processing conditional annotations: " + e.getMessage());
        }
    }
    
    /**
     * Analyzes @DependsOn annotation for explicit initialization and destruction dependencies.
     * 
     * @param typeElement the component type element
     * @param info the component info to update
     */
    private void analyzeDependsOn(TypeElement typeElement, ComponentInfo info) {
        DependsOn dependsOn = typeElement.getAnnotation(DependsOn.class);
        if (dependsOn != null) {
            // Parse initialization dependencies
            String[] dependencies = dependsOn.value();
            if (dependencies.length > 0) {
                for (String dependency : dependencies) {
                    if (dependency != null && !dependency.trim().isEmpty()) {
                        info.addExplicitDependency(dependency.trim());
                        note("  -> Depends on bean: " + dependency.trim());
                    }
                }
                note("  -> Explicit dependencies: " + String.join(", ", dependencies));
            }
            
            // Parse destruction dependencies
            String[] destroyOrder = dependsOn.destroyOrder();
            if (destroyOrder.length > 0) {
                for (String dependency : destroyOrder) {
                    if (dependency != null && !dependency.trim().isEmpty()) {
                        info.addExplicitDestructionDependency(dependency.trim());
                        note("  -> Must outlive bean: " + dependency.trim());
                    }
                }
                note("  -> Destruction order dependencies: " + String.join(", ", destroyOrder));
            }
            
            // Parse destruction order value
            int destroyOrderValue = dependsOn.destroyOrderValue();
            if (destroyOrderValue != 0) {
                info.setDestroyOrderValue(destroyOrderValue);
                note("  -> Destruction order value: " + destroyOrderValue);
            }
        }
    }
    
    private void analyzeLifecycle(TypeElement typeElement, ComponentInfo info) throws ProcessingException {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            
            ExecutableElement method = (ExecutableElement) enclosed;
            
            if (method.getAnnotation(PostConstruct.class) != null) {
                validateLifecycleMethod(method, "@PostConstruct");
                info.setPostConstruct(method.getSimpleName().toString(), getMethodDescriptor(method));
            }
            
            if (method.getAnnotation(PreDestroy.class) != null) {
                validateLifecycleMethod(method, "@PreDestroy");
                info.setPreDestroy(method.getSimpleName().toString(), getMethodDescriptor(method));
            }
        }
    }
    
    private void validateLifecycleMethod(ExecutableElement method, String annotation) throws ProcessingException {
        if (!method.getParameters().isEmpty()) {
            throw new ProcessingException(annotation + " methods must have no parameters: " + method.getSimpleName());
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            throw new ProcessingException(annotation + " cannot be applied to static methods: " + method.getSimpleName());
        }
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
    private void generateAllFactories() {
        // Factory generation removed - using static dependency injection model
    }
    /**
     * Gets a unique module identifier for metadata file naming.
     * Uses groupId:artifactId format if available, otherwise uses package name.
     */
    private String getModuleId() {
        // Try to get module info from processing environment
        // This is a best-effort attempt to create a unique module identifier
        try {
            // Check if we can get the source file to determine the module
            // For now, use a combination of package and a hash
            String moduleName = System.getProperty("veld.module.name", "");
            if (!moduleName.isEmpty()) {
                return moduleName;
            }
            
            // Fallback: use a hash based on discovered components/factories
            if (!discoveredComponents.isEmpty()) {
                String firstPackage = discoveredComponents.get(0).getClassName();
                int lastDot = firstPackage.lastIndexOf('.');
                String basePackage = lastDot >= 0 ? firstPackage.substring(0, lastDot) : firstPackage;
                return basePackage;
            }
            

            
            return "veld-module";
        } catch (Exception e) {
            return "veld-module";
        }
    }
    
    /**
     * Exports bean metadata for multi-module support.
     * Writes metadata to META-INF/veld/ for consumption by dependent modules.
     */
    private void exportBeanMetadata() throws IOException {
        String moduleId = getModuleId();
        List<BeanMetadata> beansToExport = new ArrayList<>();

        // Export @Component beans
        for (ComponentInfo component : discoveredComponents) {
            BeanMetadata bean = new BeanMetadata(
                moduleId,
                component.getComponentName(),
                component.getClassName()
            );
            
            // Get factory class name
            String factoryClassName = component.getFactoryClassName();
            if (factoryClassName != null && !factoryClassName.isEmpty()) {
                // Extract method name from class name
                String simpleName = factoryClassName.substring(factoryClassName.lastIndexOf('.') + 1);
                String methodName = "create" + simpleName.replace("Factory", "");
                bean = bean.withFactory(factoryClassName, methodName, "()V");
            }
            
            bean = bean.withScope(component.getScope());
            if (component.isPrimary()) {
                bean = bean.asPrimary();
            }
            
            // Add dependencies
            if (component.getConstructorInjection() != null) {
                for (InjectionPoint.Dependency dep : component.getConstructorInjection().getDependencies()) {
                    bean.addDependency(dep.getActualTypeName());
                }
            }
            
            beansToExport.add(bean);
        }



        // Write metadata file
        int exportedCount = BeanMetadataWriter.writeMetadata(moduleId, beansToExport, filer, this::note);
        
        if (exportedCount > 0) {
            note("Exported " + exportedCount + " bean(s) for multi-module support");
        }
    }
    
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
            // Generate AOP wrapper classes for components with interceptors
            AopClassGenerator aopGen = new AopClassGenerator(filer, messager, elementUtils, typeUtils);
            Map<String, String> aopClassMap = aopGen.generateAopClasses(discoveredComponents);
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
            exportBeanMetadata();
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
            
        } catch (IOException e) {
            error(null, "Failed to generate Veld class files: " + e.getMessage());
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
        sb.append(comp.getScope().name()).append("||");
        
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
    
    private void warning(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, "[Veld] " + message, element);
    }
    
    private void note(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[Veld] " + message);
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

        // Convert field injections
        for (InjectionPoint field : info.getFieldInjections()) {
            if (!field.getDependencies().isEmpty()) {
                InjectionPoint.Dependency dep = field.getDependencies().get(0);
                VeldNode.FieldInjection fieldInjection = new VeldNode.FieldInjection(
                    field.getName(),
                    dep.getTypeName(),
                    dep.isProvider(),
                    dep.isOptional(),
                    dep.isOptionalWrapper(),
                    dep.getActualTypeName(),
                    dep.getQualifierName()
                );
                node.addFieldInjection(fieldInjection);
            }
        }

        // Convert method injections
        for (InjectionPoint method : info.getMethodInjections()) {
            VeldNode.MethodInjection methodInjection = new VeldNode.MethodInjection(method.getName());
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
