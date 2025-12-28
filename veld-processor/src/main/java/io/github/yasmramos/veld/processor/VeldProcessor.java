package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.processor.AnnotationHelper.InjectSource;
import io.github.yasmramos.veld.processor.InjectionPoint.Dependency;
import io.github.yasmramos.veld.runtime.Scope;

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
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 * - Factory pattern (@Factory, @Bean)
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
    "io.github.yasmramos.veld.annotation.Qualifier",
    "io.github.yasmramos.veld.annotation.Factory",
    "io.github.yasmramos.veld.annotation.Bean",
    "javax.inject.Singleton",
    "jakarta.inject.Singleton"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class VeldProcessor extends AbstractProcessor {
    
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;
    
    private final List<ComponentInfo> discoveredComponents = new ArrayList<>();
    private final List<FactoryInfo> discoveredFactories = new ArrayList<>();
    private final DependencyGraph dependencyGraph = new DependencyGraph();
    
    // Maps interface -> list of implementing components (for conflict detection)
    private final Map<String, List<String>> interfaceImplementors = new HashMap<>();
    
    // Track already processed classes to avoid duplicates
    private final Set<String> processedClasses = new HashSet<>();
    
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (!discoveredComponents.isEmpty()) {
                // Check for circular dependencies before generating code
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
        
        // Process all component elements
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
            
            if (typeElement.getNestingKind() == NestingKind.LOCAL || 
                typeElement.getNestingKind() == NestingKind.ANONYMOUS) {
                error(typeElement, "Component annotation cannot be applied to local or anonymous classes");
                continue;
            }
            
            try {
                ComponentInfo info = analyzeComponent(typeElement);
                discoveredComponents.add(info);
                
                // Build dependency graph for cycle detection
                buildDependencyGraph(info);
                
                generateFactory(info);
                note("Generated factory for: " + info.getClassName());
            } catch (ProcessingException e) {
                error(typeElement, e.getMessage());
            }
        }

        // Process @Factory classes and @Bean methods
        processFactories(roundEnv);

        // Generate source code for @Bean methods
        generateBeanFactories();

        return true;
    }

    /**
     * Processes classes annotated with @Factory and their @Bean methods.
     */
    private void processFactories(RoundEnvironment roundEnv) {
        // Find all @Factory classes
        Set<TypeElement> factoryElements = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Factory.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                factoryElements.add((TypeElement) element);
            }
        }

        // Also find @Bean methods (which might be in non-@Factory classes)
        Set<ExecutableElement> beanMethods = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(io.github.yasmramos.veld.annotation.Bean.class)) {
            if (element.getKind() == ElementKind.METHOD) {
                beanMethods.add((ExecutableElement) element);
            }
        }

        // Process each factory class
        for (TypeElement factoryElement : factoryElements) {
            String factoryClassName = factoryElement.getQualifiedName().toString();

            // Skip if already processed
            if (processedClasses.contains(factoryClassName)) {
                continue;
            }
            processedClasses.add(factoryClassName);

            // Validate class
            if (factoryElement.getModifiers().contains(Modifier.ABSTRACT)) {
                error(factoryElement, "@Factory cannot be applied to abstract classes");
                continue;
            }

            try {
                FactoryInfo factoryInfo = analyzeFactory(factoryElement, beanMethods);
                discoveredFactories.add(factoryInfo);
                note("Discovered factory: " + factoryClassName + " with " +
                     factoryInfo.getBeanMethods().size() + " @Bean methods");
            } catch (ProcessingException e) {
                error(factoryElement, e.getMessage());
            }
        }

        // Process standalone @Bean methods (not inside a @Factory)
        for (ExecutableElement beanMethod : beanMethods) {
            TypeElement enclosingClass = (TypeElement) beanMethod.getEnclosingElement();
            if (enclosingClass.getAnnotation(Factory.class) == null) {
                // This @Bean is in a non-@Factory class
                // For now, we can log a warning or handle differently
                note("Found @Bean method outside @Factory class: " +
                     enclosingClass.getQualifiedName() + "." + beanMethod.getSimpleName());
            }
        }
    }

    /**
     * Generates source code for all @Bean methods discovered in @Factory classes.
     * Each @Bean method gets its own factory class that implements ComponentFactory.
     */
    private void generateBeanFactories() {
        int globalBeanIndex = 0;

        for (FactoryInfo factory : discoveredFactories) {
            int factoryBeanIndex = 0;

            for (FactoryInfo.BeanMethod beanMethod : factory.getBeanMethods()) {
                try {
                    // Create the source generator for this @Bean method
                    BeanFactorySourceGenerator generator = new BeanFactorySourceGenerator(
                        factory, beanMethod, globalBeanIndex);

                    // Generate the source code
                    String sourceCode = generator.generate();
                    String factoryClassName = generator.getFactoryClassName();

                    // Write the generated source file
                    writeJavaSource(factoryClassName, sourceCode);

                    note("Generated BeanFactory for @Bean method: " + beanMethod.getMethodName() +
                         " (index: " + globalBeanIndex + ")");

                    globalBeanIndex++;
                    factoryBeanIndex++;
                } catch (IOException e) {
                    error(null, "Failed to generate BeanFactory for @Bean method " +
                          beanMethod.getMethodName() + ": " + e.getMessage());
                }
            }
        }

        if (globalBeanIndex > 0) {
            note("Generated " + globalBeanIndex + " BeanFactory classes for @Bean methods");
        }
    }

    /**
     * Analyzes a @Factory class and extracts @Bean methods.
     */
    private FactoryInfo analyzeFactory(TypeElement factoryElement,
                                       Set<ExecutableElement> allBeanMethods) throws ProcessingException {
        String factoryClassName = factoryElement.getQualifiedName().toString();

        // Get factory name from annotation
        io.github.yasmramos.veld.annotation.Factory factoryAnnotation =
            factoryElement.getAnnotation(io.github.yasmramos.veld.annotation.Factory.class);
        String factoryName = factoryAnnotation != null && !factoryAnnotation.name().isEmpty()
            ? factoryAnnotation.name()
            : decapitalize(factoryElement.getSimpleName().toString());

        FactoryInfo factoryInfo = new FactoryInfo(factoryClassName, factoryName);
        factoryInfo.setTypeElement(factoryElement);

        // Find @Bean methods in this factory class
        for (Element enclosed : factoryElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            io.github.yasmramos.veld.annotation.Bean beanAnnotation =
                method.getAnnotation(io.github.yasmramos.veld.annotation.Bean.class);

            if (beanAnnotation == null) continue;

            // Validate @Bean method
            if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new ProcessingException("@Bean cannot be applied to abstract methods: " +
                    method.getSimpleName());
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                throw new ProcessingException("@Bean cannot be applied to static methods: " +
                    method.getSimpleName());
            }

            // Get bean name from annotation
            String beanName = !beanAnnotation.name().isEmpty()
                ? beanAnnotation.name()
                : method.getSimpleName().toString();

            boolean isPrimary = beanAnnotation.primary();

            // Get scope from annotation (as string, then convert to enum)
            String scopeString = beanAnnotation.scope();
            io.github.yasmramos.veld.runtime.Scope scope =
                "prototype".equalsIgnoreCase(scopeString)
                    ? io.github.yasmramos.veld.runtime.Scope.PROTOTYPE
                    : io.github.yasmramos.veld.runtime.Scope.SINGLETON;

            // Get return type
            TypeMirror returnType = method.getReturnType();
            String returnTypeName = getTypeName(returnType);
            String returnTypeDescriptor = getTypeDescriptor(returnType);

            // Get method descriptor
            String methodDescriptor = getMethodDescriptor(method);

            // Create BeanMethod
            FactoryInfo.BeanMethod beanMethod = new FactoryInfo.BeanMethod(
                method.getSimpleName().toString(),
                methodDescriptor,
                returnTypeName,
                returnTypeDescriptor,
                beanName,
                isPrimary
            );

            // Set scope
            beanMethod.setScope(scope);
            if (scope == io.github.yasmramos.veld.runtime.Scope.PROTOTYPE) {
                note("  -> @Bean scope: PROTOTYPE");
            }

            // Analyze qualifier from method parameters
            analyzeBeanQualifier(method, beanMethod);

            // Add parameter types (for dependency resolution)
            for (VariableElement param : method.getParameters()) {
                beanMethod.addParameterType(getTypeName(param.asType()));
            }

            // Analyze lifecycle methods in the return type class
            analyzeBeanLifecycle(returnType, beanMethod);

            factoryInfo.addBeanMethod(beanMethod);
            note("  -> @Bean method: " + method.getSimpleName() +
                 " produces: " + returnTypeName +
                 " (name: " + beanName + ")" +
                 (isPrimary ? " [PRIMARY]" : ""));
        }

        if (!factoryInfo.hasBeanMethods()) {
            warning(null, "@Factory class " + factoryClassName +
                 " has no @Bean methods. It will be registered but won't produce any beans.");
        }

        return factoryInfo;
    }

    /**
     * Analyzes lifecycle methods (@PostConstruct, @PreDestroy) in the bean return type class.
     * These methods will be called by the generated BeanFactory during bean lifecycle.
     *
     * @param returnType the return type of the @Bean method
     * @param beanMethod the BeanMethod to store lifecycle information
     */
    private void analyzeBeanLifecycle(TypeMirror returnType, FactoryInfo.BeanMethod beanMethod) {
        if (returnType.getKind() != TypeKind.DECLARED) {
            return; // Only analyze declared types (classes/interfaces)
        }

        DeclaredType declaredType = (DeclaredType) returnType;
        TypeElement returnElement = (TypeElement) declaredType.asElement();

        // Find @PostConstruct method
        for (Element enclosed : returnElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            if (method.getAnnotation(PostConstruct.class) != null) {
                validateLifecycleMethod(method, "@PostConstruct");
                beanMethod.setPostConstruct(method.getSimpleName().toString(), getMethodDescriptor(method));
                note("  -> @PostConstruct method: " + method.getSimpleName());
                break; // Only one @PostConstruct allowed
            }
        }

        // Find @PreDestroy method
        for (Element enclosed : returnElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            if (method.getAnnotation(PreDestroy.class) != null) {
                validateLifecycleMethod(method, "@PreDestroy");
                beanMethod.setPreDestroy(method.getSimpleName().toString(), getMethodDescriptor(method));
                note("  -> @PreDestroy method: " + method.getSimpleName());
                break; // Only one @PreDestroy allowed
            }
        }
    }

    /**
     * Analyzes qualifier annotations on the @Bean method itself.
     * Supports @Named, @Qualifier, and custom qualifier annotations.
     *
     * @param beanMethod the @Bean method element
     * @param beanMethodInfo the BeanMethod to store qualifier information
     */
    private void analyzeBeanQualifier(ExecutableElement beanMethod, FactoryInfo.BeanMethod beanMethodInfo) {
        // Check for @Named annotation (Veld, javax.inject, or jakarta.inject)
        Optional<String> qualifier = AnnotationHelper.getQualifierValue(beanMethod);
        if (qualifier.isPresent()) {
            beanMethodInfo.setQualifier(qualifier.get());
            note("  -> @Bean qualifier: @" + qualifier.get());
        }

        // Check for @Qualifier annotation
        TypeElement qualifierAnnotation = elementUtils.getTypeElement("io.github.yasmramos.veld.annotation.Qualifier");
        if (qualifierAnnotation != null) {
            io.github.yasmramos.veld.annotation.Qualifier qualAnn =
                beanMethod.getAnnotation(io.github.yasmramos.veld.annotation.Qualifier.class);
            if (qualAnn != null && !qualAnn.value().isEmpty()) {
                beanMethodInfo.setQualifier(qualAnn.value());
                note("  -> @Bean @Qualifier: " + qualAnn.value());
            }
        }
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
        Optional<List<String>> cycle = dependencyGraph.detectCycle();
        
        if (cycle.isPresent()) {
            String cyclePath = DependencyGraph.formatCycle(cycle.get());
            error(null, "Circular dependency detected: " + cyclePath + 
                "\n  Circular dependencies are not allowed. " +
                "Consider using:\n" +
                "    - Setter/method injection instead of constructor injection\n" +
                "    - @Lazy injection (if supported)\n" +
                "    - Refactoring to break the cycle");
            return false;
        }
        
        note("Dependency graph validated: no circular dependencies found");
        return true;
    }
    
    private ComponentInfo analyzeComponent(TypeElement typeElement) throws ProcessingException {
        String className = typeElement.getQualifiedName().toString();
        
        // Get component name - check all possible sources
        String componentName = getComponentName(typeElement);
        if (componentName == null || componentName.isEmpty()) {
            componentName = decapitalize(typeElement.getSimpleName().toString());
        }
        
        // Determine scope and check for @Lazy
        Scope scope = determineScope(typeElement);
        boolean isLazy = typeElement.getAnnotation(Lazy.class) != null;
        
        // Check for @Primary annotation
        boolean isPrimary = typeElement.getAnnotation(Primary.class) != null;
        if (isPrimary) {
            note("  -> Primary bean selected");
        }
        
        ComponentInfo info = new ComponentInfo(className, componentName, scope, isLazy, isPrimary);
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
     * All other scope annotations (Veld @Singleton, javax/jakarta @Singleton) result in SINGLETON.
     * Default is SINGLETON if no explicit scope is specified.
     */
    private Scope determineScope(TypeElement typeElement) {
        // Check for @Prototype first - it's the only way to get prototype scope
        if (typeElement.getAnnotation(Prototype.class) != null) {
            note("  -> Scope: PROTOTYPE");
            return Scope.PROTOTYPE;
        }
        
        // Check for explicit singleton annotations
        if (typeElement.getAnnotation(Singleton.class) != null ||
            AnnotationHelper.hasSingletonAnnotation(typeElement)) {
            note("  -> Scope: SINGLETON (explicit)");
            return Scope.SINGLETON;
        }
        
        // Check for @Lazy alone (implies singleton)
        if (typeElement.getAnnotation(Lazy.class) != null) {
            note("  -> Scope: SINGLETON (from @Lazy)");
            return Scope.SINGLETON;
        }
        
        // Default scope
        note("  -> Scope: SINGLETON (default)");
        return Scope.SINGLETON;
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
        
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) continue;
            
            ExecutableElement constructor = (ExecutableElement) enclosed;
            
            // Check for any @Inject annotation (Veld, javax.inject, or jakarta.inject)
            if (AnnotationHelper.hasInjectAnnotation(constructor)) {
                if (injectConstructor != null) {
                    throw new ProcessingException("Only one constructor can be annotated with @Inject");
                }
                injectConstructor = constructor;
                injectSource = AnnotationHelper.getInjectSource(constructor);
            } else if (constructor.getParameters().isEmpty()) {
                defaultConstructor = constructor;
            }
        }
        
        ExecutableElement chosenConstructor = injectConstructor != null ? injectConstructor : defaultConstructor;
        
        if (chosenConstructor == null) {
            throw new ProcessingException("No suitable constructor found. " +
                    "Must have either @Inject constructor or no-arg constructor");
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
     * Supports @ConditionalOnProperty, @ConditionalOnClass, @ConditionalOnMissingBean.
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
                    
                    if (!profiles.isEmpty()) {
                        conditionInfo.addProfileCondition(profiles);
                        note("  -> Profile: " + String.join(", ", profiles));
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
     * Analyzes @DependsOn annotation for explicit initialization dependencies.
     * 
     * @param typeElement the component type element
     * @param info the component info to update
     */
    private void analyzeDependsOn(TypeElement typeElement, ComponentInfo info) {
        DependsOn dependsOn = typeElement.getAnnotation(DependsOn.class);
        if (dependsOn != null) {
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
        Optional<String> qualifier = AnnotationHelper.getQualifierValue(element);
        
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
    
    private void generateFactory(ComponentInfo info) {
        try {
            // Use the current index based on position in discoveredComponents
            // The component was just added, so index = size - 1
            int componentIndex = discoveredComponents.size() - 1;
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(info, componentIndex);
            byte[] bytecode = generator.generate();
            
            writeClassFile(info.getFactoryClassName(), bytecode);
            note("  -> Factory index: " + componentIndex);
        } catch (IOException e) {
            error(null, "Failed to generate factory for " + info.getClassName() + ": " + e.getMessage());
        }
    }
    
    private void generateRegistry() {
        try {
            // Generate AOP wrapper classes for components with interceptors
            AopClassGenerator aopGen = new AopClassGenerator(filer, messager, elementUtils, typeUtils);
            Map<String, String> aopClassMap = aopGen.generateAopClasses(discoveredComponents);

            if (!aopClassMap.isEmpty()) {
                note("Generated " + aopClassMap.size() + " AOP wrapper classes");
            }

            // Generate VeldRegistry bytecode (including factory beans)
            RegistryGenerator registryGen = new RegistryGenerator(discoveredComponents, discoveredFactories);
            byte[] registryBytecode = registryGen.generate();
            writeClassFile(registryGen.getRegistryClassName(), registryBytecode);
            note("Generated VeldRegistry with " + discoveredComponents.size() + " components and " +
                 discoveredFactories.size() + " factories");

            // Generate factory metadata for weaver
            if (!discoveredFactories.isEmpty()) {
                writeFactoryMetadata();
                note("Wrote factory metadata for " + discoveredFactories.size() + " factories");
            }

            // Generate Veld.java source code (passing AOP class map for wrapper instantiation)
            VeldSourceGenerator veldGen = new VeldSourceGenerator(discoveredComponents, aopClassMap);
            String veldSource = veldGen.generate();
            writeJavaSource("io.github.yasmramos.veld.Veld", veldSource);
            note("Generated Veld.java with " + discoveredComponents.size() + " components");

            // Write component metadata for weaver
            writeComponentMetadata();
            note("Wrote component metadata for weaver (" + discoveredComponents.size() + " components)");
        } catch (IOException e) {
            error(null, "Failed to generate VeldRegistry: " + e.getMessage());
        }
    }

    /**
     * Writes factory metadata to a file that the weaver will read.
     * Format: One factory per line with fields separated by ||
     */
    private void writeFactoryMetadata() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Veld Factory Metadata - DO NOT EDIT\n");
        sb.append("# Generated by VeldProcessor\n");
        sb.append("# Format: factoryClassName||factoryName||beanCount\n");

        for (FactoryInfo factory : discoveredFactories) {
            sb.append(factory.getFactoryClassName()).append("||");
            sb.append(factory.getFactoryName()).append("||");
            sb.append(factory.getBeanMethods().size());
            sb.append("\n");
        }

        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
            "META-INF/veld/factories.meta");
        try (Writer writer = file.openWriter()) {
            writer.write(sb.toString());
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
        
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            
            ExecutableElement method = (ExecutableElement) enclosed;
            
            // Check for @Subscribe annotation
            for (AnnotationMirror annotation : method.getAnnotationMirrors()) {
                String annotationName = annotation.getAnnotationType().asElement().toString();
                if (annotationName.equals("io.github.yasmramos.veld.annotation.Subscribe")) {
                    info.setHasSubscribeMethods(true);
                    note("  -> EventBus subscriber: " + method.getSimpleName());
                    return; // Only need to find one
                }
            }
        }
    }
    
    private static class ProcessingException extends Exception {
        ProcessingException(String message) {
            super(message);
        }
    }
}
