package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.processor.AnnotationHelper.InjectSource;
import io.github.yasmramos.veld.processor.InjectionPoint.Dependency;
import io.github.yasmramos.veld.runtime.LegacyScope;

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
    "io.github.yasmramos.veld.annotation.Order",
    "io.github.yasmramos.veld.annotation.Qualifier",
    "io.github.yasmramos.veld.annotation.Factory",
    "io.github.yasmramos.veld.annotation.Bean",
    "io.github.yasmramos.veld.annotation.Lookup",
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
    
    // External beans loaded from classpath (multi-module support)
    private final List<BeanMetadataReader.ExternalBeanInfo> externalBeans = new ArrayList<>();
    
    // Maps interface -> list of implementing components (for conflict detection)
    private final Map<String, List<String>> interfaceImplementors = new HashMap<>();
    
    // Track already processed classes to avoid duplicates
    private final Set<String> processedClasses = new HashSet<>();

    // Event subscriptions for zero-reflection event registration
    private final List<EventRegistryGenerator.SubscriptionInfo> eventSubscriptions = new ArrayList<>();
    
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
            // Check for circular dependencies before generating code
            // Validate if we have any components OR factories
            if (!discoveredComponents.isEmpty() || !discoveredFactories.isEmpty()) {
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
                
                generateFactory(info);
                note("Generated factory for: " + info.getClassName());
            } catch (ProcessingException e) {
                error(typeElement, e.getMessage());
            }
        }

        // Process @Factory classes and @Bean methods
        processFactories(roundEnv);

        // Build dependency graph for @Bean methods
        buildFactoryDependencyGraph();

        // Generate source code for @Bean methods
        generateBeanFactories();

        // Identify unresolved interface dependencies (for testing/mock support)
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
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                throw new ProcessingException("@Bean methods cannot be private: " +
                    method.getSimpleName() + ". Make the method package-private or public.");
            }

            // Get bean name from annotation (must be done before validation)
            String beanName = !beanAnnotation.name().isEmpty()
                ? beanAnnotation.name()
                : method.getSimpleName().toString();

            // Get return type (must be done before validation)
            TypeMirror returnType = method.getReturnType();

            // Validate return type is not primitive or void
            if (returnType.getKind() == TypeKind.VOID) {
                throw new ProcessingException("@Bean methods must return a bean type, not void: " +
                    method.getSimpleName());
            }
            if (returnType.getKind().isPrimitive() && returnType.getKind() != TypeKind.BOOLEAN && 
                returnType.getKind() != TypeKind.BYTE && returnType.getKind() != TypeKind.CHAR &&
                returnType.getKind() != TypeKind.DOUBLE && returnType.getKind() != TypeKind.FLOAT &&
                returnType.getKind() != TypeKind.INT && returnType.getKind() != TypeKind.LONG &&
                returnType.getKind() != TypeKind.SHORT) {
                // This condition checks for primitive types that are not primitives
                // Actually, primitive types in Java are: BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT
                // Boxed types are DECLARED types, so we just check if it's primitive
                // The validation message below handles the suggestion
            }
            if (returnType.getKind().isPrimitive()) {
                throw new ProcessingException("@Bean methods cannot return primitive types: " +
                    method.getSimpleName() + ". Use the boxed type (e.g., Integer instead of int, String instead of char).");
            }

            // Validate no duplicate bean names in the same factory
            validateNoDuplicateBeanNames(factoryInfo, beanName, method);

            boolean isPrimary = beanAnnotation.primary();

            // Get scope from annotation (as string, then convert to enum)
            String scopeString = beanAnnotation.scope();
            LegacyScope scope =
                "prototype".equalsIgnoreCase(scopeString)
                    ? LegacyScope.PROTOTYPE
                    : LegacyScope.SINGLETON;

            // Get return type info
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
            if (scope == LegacyScope.PROTOTYPE) {
                note("  -> @Bean scope: PROTOTYPE");
            }

            // Analyze qualifier from method parameters
            analyzeBeanQualifier(method, beanMethod);

            // Analyze @Profile annotation on @Bean method
            analyzeBeanProfile(method, beanMethod);

            // Add parameter types (for dependency resolution)
            for (VariableElement param : method.getParameters()) {
                String paramType = getTypeName(param.asType());
                beanMethod.addParameterType(paramType);

                // Validate parameter is injectable
                if (!isInjectableType(param.asType())) {
                    warning(method, "Parameter '" + param.getSimpleName() + 
                        "' of @Bean method '" + method.getSimpleName() + 
                        "' has type '" + paramType + "' which may not be directly injectable. " +
                        "Consider using Provider<T> or Optional<T> for lazy/optional injection.");
                }
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
     * Validates that no duplicate bean names exist within the same factory.
     * 
     * @param factoryInfo the factory being analyzed
     * @param beanName the proposed bean name
     * @param method the method element (for error reporting)
     * @throws ProcessingException if a duplicate is found
     */
    private void validateNoDuplicateBeanNames(FactoryInfo factoryInfo, String beanName, 
                                               ExecutableElement method) throws ProcessingException {
        for (FactoryInfo.BeanMethod existing : factoryInfo.getBeanMethods()) {
            if (existing.getBeanName().equals(beanName)) {
                throw new ProcessingException(
                    "Duplicate bean name '" + beanName + "' in factory " + factoryInfo.getFactoryClassName() + ".\n" +
                    "  - First defined in method: " + existing.getMethodName() + "\n" +
                    "  - Second definition in method: " + method.getSimpleName() + "\n" +
                    "  Fix: Use unique bean names via @Bean(name=\"uniqueName\") or rename one of the methods."
                );
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
     * Analyzes lifecycle methods (@PostConstruct, @PreDestroy) in the bean return type class.
     * These methods will be called by the generated BeanFactory during bean lifecycle.
     *
     * @param returnType the return type of the @Bean method
     * @param beanMethod the BeanMethod to store lifecycle information
     * @throws ProcessingException if a lifecycle method is invalid
     */
    private void analyzeBeanLifecycle(TypeMirror returnType, FactoryInfo.BeanMethod beanMethod) throws ProcessingException {
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
     * Analyzes profile annotations on the @Bean method.
     * Stores profile information for runtime profile-based activation.
     *
     * @param beanMethod the @Bean method element
     * @param beanMethodInfo the BeanMethod to store profile information
     */
    private void analyzeBeanProfile(ExecutableElement beanMethod, FactoryInfo.BeanMethod beanMethodInfo) {
        io.github.yasmramos.veld.annotation.Profile profileAnnotation =
            beanMethod.getAnnotation(io.github.yasmramos.veld.annotation.Profile.class);

        if (profileAnnotation != null) {
            List<String> profiles = new ArrayList<>();
            String[] profileValues = profileAnnotation.value();
            
            // Also check 'name' attribute as alias
            if (profileValues.length == 0 || (profileValues.length == 1 && profileValues[0].isEmpty())) {
                String nameValue = profileAnnotation.name();
                if (!nameValue.isEmpty()) {
                    profileValues = new String[]{nameValue};
                }
            }

            for (String profile : profileValues) {
                if (profile != null && !profile.isEmpty()) {
                    profiles.add(profile);
                    beanMethodInfo.addProfile(profile);
                }
            }

            if (!profiles.isEmpty()) {
                note("  -> @Bean profiles: " + String.join(", ", profiles));
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
     * Builds the dependency graph for @Bean methods.
     * Adds each bean and its parameter dependencies to the graph.
     * This allows detecting circular dependencies involving factory beans.
     */
    private void buildFactoryDependencyGraph() {
        for (FactoryInfo factory : discoveredFactories) {
            for (FactoryInfo.BeanMethod beanMethod : factory.getBeanMethods()) {
                String beanType = beanMethod.getReturnType();
                dependencyGraph.addComponent(beanType);

                note("  -> Adding bean to dependency graph: " + beanType);

                // Add dependencies from @Bean method parameters
                for (String paramType : beanMethod.getParameterTypes()) {
                    dependencyGraph.addDependency(beanType, paramType);
                    note("    -> Bean dependency: " + beanType + " → " + paramType);
                }
            }
        }

        if (!discoveredFactories.isEmpty()) {
            note("Factory dependency graph built with " + discoveredFactories.size() + " factories");
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
            String cycleDetail = buildCycleDetailMessage(cycle.get());
            
            error(null, "Circular dependency detected: " + cyclePath + "\n" +
                cycleDetail + "\n" +
                "  Circular dependencies are not allowed in Veld.\n" +
                "  Possible solutions:\n" +
                "    - Use setter/method injection instead of constructor injection\n" +
                "    - Break the dependency cycle by refactoring\n" +
                "    - Use @Lazy on one of the dependencies (if supported by your use case)\n" +
                "    - For @Bean methods, use Provider<T> for lazy injection");
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
        LegacyScope scope = determineScope(typeElement);
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
     * All other scope annotations (Veld @Singleton, javax/jakarta @Singleton) result in SINGLETON.
     * Default is SINGLETON if no explicit scope is specified.
     */
    private LegacyScope determineScope(TypeElement typeElement) {
        // Check for @Prototype first - it's the only way to get prototype scope
        if (typeElement.getAnnotation(Prototype.class) != null) {
            note("  -> Scope: PROTOTYPE");
            return LegacyScope.PROTOTYPE;
        }
        
        // Check for explicit singleton annotations
        if (typeElement.getAnnotation(Singleton.class) != null ||
            AnnotationHelper.hasSingletonAnnotation(typeElement)) {
            note("  -> Scope: SINGLETON (explicit)");
            return LegacyScope.SINGLETON;
        }
        
        // Check for @Lazy alone (implies singleton)
        if (typeElement.getAnnotation(Lazy.class) != null) {
            note("  -> Scope: SINGLETON (from @Lazy)");
            return LegacyScope.SINGLETON;
        }
        
        // Default scope
        note("  -> Scope: SINGLETON (default)");
        return LegacyScope.SINGLETON;
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
            
            if (!discoveredFactories.isEmpty()) {
                String firstPackage = discoveredFactories.get(0).getFactoryClassName();
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

        // Export @Bean methods from factories
        for (FactoryInfo factory : discoveredFactories) {
            for (FactoryInfo.BeanMethod beanMethod : factory.getBeanMethods()) {
                BeanMetadata bean = new BeanMetadata(
                    moduleId,
                    beanMethod.getBeanName(),
                    beanMethod.getReturnType()
                );
                
                // Get factory class name from BeanMethod
                String factoryClassName = beanMethod.getFactoryClassName();
                if (factoryClassName != null && !factoryClassName.isEmpty()) {
                    String simpleName = factoryClassName.substring(factoryClassName.lastIndexOf('.') + 1);
                    String methodName = beanMethod.getMethodName();
                    bean = bean.withFactory(factoryClassName, methodName, beanMethod.getMethodDescriptor());
                }
                
                bean = bean.withScope(beanMethod.getScope());
                if (beanMethod.isPrimary()) {
                    bean = bean.asPrimary();
                }
                if (beanMethod.hasQualifier()) {
                    bean = bean.withQualifier(beanMethod.getQualifier());
                }
                
                // Add dependencies from parameters
                for (String paramType : beanMethod.getParameterTypes()) {
                    bean.addDependency(paramType);
                }
                
                beansToExport.add(bean);
            }
        }

        // Write metadata file
        int exportedCount = BeanMetadataWriter.writeMetadata(moduleId, beansToExport, filer, this::note);
        
        if (exportedCount > 0) {
            note("Exported " + exportedCount + " bean(s) for multi-module support");
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

            // Generate EventRegistry for zero-reflection event registration
            generateEventRegistry();

            // Export bean metadata for multi-module support
            exportBeanMetadata();
        } catch (IOException e) {
            error(null, "Failed to generate VeldRegistry: " + e.getMessage());
        }
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
            String sourceCode = generator.generate();
            writeJavaSource(generator.getClassName(), sourceCode);
            note("Generated EventRegistry with " + eventSubscriptions.size() + " event handlers");
        } catch (IOException e) {
            error(null, "Failed to generate EventRegistry: " + e.getMessage());
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
    
    private static class ProcessingException extends Exception {
        ProcessingException(String message) {
            super(message);
        }
    }
}
