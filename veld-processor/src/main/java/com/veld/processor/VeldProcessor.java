package com.veld.processor;

import com.veld.annotation.*;
import com.veld.processor.AnnotationHelper.InjectSource;
import com.veld.processor.InjectionPoint.Dependency;
import com.veld.runtime.Scope;

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
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
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
 * 
 * Supported Injection Annotations:
 * - @com.veld.annotation.Inject (Veld native)
 * - @javax.inject.Inject (JSR-330)
 * - @jakarta.inject.Inject (Jakarta EE)
 * 
 * Supported Qualifier Annotations:
 * - @com.veld.annotation.Named (Veld native)
 * - @javax.inject.Named (JSR-330)
 * - @jakarta.inject.Named (Jakarta EE)
 * 
 * Supported Scope Annotations:
 * - @com.veld.annotation.Singleton (Veld native)
 * - @javax.inject.Singleton (JSR-330)
 * - @jakarta.inject.Singleton (Jakarta EE)
 * - @com.veld.annotation.Prototype (Veld only)
 */
@SupportedAnnotationTypes("com.veld.annotation.Component")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class VeldProcessor extends AbstractProcessor {
    
    private Messager messager;
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;
    
    private final List<ComponentInfo> discoveredComponents = new ArrayList<>();
    private final DependencyGraph dependencyGraph = new DependencyGraph();
    
    // Maps interface -> list of implementing components (for conflict detection)
    private final Map<String, List<String>> interfaceImplementors = new HashMap<>();
    
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
        
        // Find all @Component annotated classes
        for (Element element : roundEnv.getElementsAnnotatedWith(Component.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "@Component can only be applied to classes");
                continue;
            }
            
            TypeElement typeElement = (TypeElement) element;
            
            // Validate class
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                error(element, "@Component cannot be applied to abstract classes");
                continue;
            }
            
            if (typeElement.getNestingKind() == NestingKind.LOCAL || 
                typeElement.getNestingKind() == NestingKind.ANONYMOUS) {
                error(element, "@Component cannot be applied to local or anonymous classes");
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
                error(element, e.getMessage());
            }
        }
        
        return true;
    }
    
    /**
     * Builds the dependency graph for a component.
     * Adds the component and all its dependencies to the graph.
     */
    private void buildDependencyGraph(ComponentInfo info) {
        String componentName = info.getClassName();
        dependencyGraph.addComponent(componentName);
        
        // Add constructor dependencies
        if (info.getConstructorInjection() != null) {
            for (InjectionPoint.Dependency dep : info.getConstructorInjection().getDependencies()) {
                dependencyGraph.addDependency(componentName, dep.getTypeName());
            }
        }
        
        // Add field dependencies
        for (InjectionPoint field : info.getFieldInjections()) {
            for (InjectionPoint.Dependency dep : field.getDependencies()) {
                dependencyGraph.addDependency(componentName, dep.getTypeName());
            }
        }
        
        // Add method dependencies
        for (InjectionPoint method : info.getMethodInjections()) {
            for (InjectionPoint.Dependency dep : method.getDependencies()) {
                dependencyGraph.addDependency(componentName, dep.getTypeName());
            }
        }
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
        
        // Get component name from @Component annotation
        Component componentAnnotation = typeElement.getAnnotation(Component.class);
        String componentName = componentAnnotation.value();
        if (componentName == null || componentName.isEmpty()) {
            componentName = decapitalize(typeElement.getSimpleName().toString());
        }
        
        // Determine scope - check all @Singleton variants (Veld, javax.inject, jakarta.inject)
        Scope scope = Scope.SINGLETON; // default
        if (typeElement.getAnnotation(Prototype.class) != null) {
            scope = Scope.PROTOTYPE;
            note("  -> Scope: PROTOTYPE");
        } else if (AnnotationHelper.hasSingletonAnnotation(typeElement)) {
            scope = Scope.SINGLETON;
            note("  -> Scope: SINGLETON (explicit)");
        } else {
            note("  -> Scope: SINGLETON (default)");
        }
        
        // Check for @Lazy annotation
        boolean isLazy = typeElement.getAnnotation(Lazy.class) != null;
        if (isLazy) {
            note("  -> Lazy initialization enabled");
        }
        
        ComponentInfo info = new ComponentInfo(className, componentName, scope, isLazy);
        
        // Find injection points
        analyzeConstructors(typeElement, info);
        analyzeFields(typeElement, info);
        analyzeMethods(typeElement, info);
        analyzeLifecycle(typeElement, info);
        
        // Analyze implemented interfaces for interface-based injection
        analyzeInterfaces(typeElement, info);
        
        return info;
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
            
            // Check for any @Inject annotation (Veld, javax.inject, or jakarta.inject)
            if (!AnnotationHelper.hasInjectAnnotation(field)) continue;
            
            InjectSource injectSource = AnnotationHelper.getInjectSource(field);
            
            if (field.getModifiers().contains(Modifier.FINAL)) {
                throw new ProcessingException("@Inject cannot be applied to final fields: " + field.getSimpleName());
            }
            if (field.getModifiers().contains(Modifier.STATIC)) {
                throw new ProcessingException("@Inject cannot be applied to static fields: " + field.getSimpleName());
            }
            if (field.getModifiers().contains(Modifier.PRIVATE)) {
                throw new ProcessingException("@Inject cannot be applied to private fields (use package-private, protected, or public): " + field.getSimpleName());
            }
            
            // Log which annotation specification is being used
            if (injectSource.isStandard()) {
                note("  -> Using " + injectSource.getPackageName() + ".Inject for field: " + field.getSimpleName());
            }
            
            Dependency dep = createDependency(field);
            String descriptor = getTypeDescriptor(field.asType());
            
            info.addFieldInjection(new InjectionPoint(
                    InjectionPoint.Type.FIELD, 
                    field.getSimpleName().toString(),
                    descriptor,
                    List.of(dep)));
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
        
        // Check if this is a Provider<T>
        ProviderInfo providerInfo = checkForProvider(typeMirror);
        if (providerInfo != null) {
            note("    -> Provider injection for: " + providerInfo.actualTypeName);
            return new Dependency(
                typeName, typeDescriptor, qualifier.orElse(null),
                true, isLazy,
                providerInfo.actualTypeName, providerInfo.actualTypeDescriptor
            );
        }
        
        return new Dependency(typeName, typeDescriptor, qualifier.orElse(null),
                false, isLazy, typeName, typeDescriptor);
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
            "com.veld.runtime.Provider".equals(providerTypeName) ||
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
    
    private static class ProviderInfo {
        final String actualTypeName;
        final String actualTypeDescriptor;
        
        ProviderInfo(String actualTypeName, String actualTypeDescriptor) {
            this.actualTypeName = actualTypeName;
            this.actualTypeDescriptor = actualTypeDescriptor;
        }
    }
    
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
            ComponentFactoryGenerator generator = new ComponentFactoryGenerator(info);
            byte[] bytecode = generator.generate();
            
            writeClassFile(info.getFactoryClassName(), bytecode);
        } catch (IOException e) {
            error(null, "Failed to generate factory for " + info.getClassName() + ": " + e.getMessage());
        }
    }
    
    private void generateRegistry() {
        try {
            // Generate VeldRegistry bytecode
            RegistryGenerator registryGen = new RegistryGenerator(discoveredComponents);
            byte[] registryBytecode = registryGen.generate();
            writeClassFile(registryGen.getRegistryClassName(), registryBytecode);
            note("Generated VeldRegistry with " + discoveredComponents.size() + " components");
            
            // Generate Veld bootstrap class bytecode (ZERO REFLECTION)
            VeldBootstrapGenerator bootstrapGen = new VeldBootstrapGenerator();
            byte[] bootstrapBytecode = bootstrapGen.generate();
            writeClassFile(bootstrapGen.getClassName(), bootstrapBytecode);
            note("Generated Veld bootstrap class (pure ASM bytecode)");
        } catch (IOException e) {
            error(null, "Failed to generate VeldRegistry: " + e.getMessage());
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
    
    private static class ProcessingException extends Exception {
        ProcessingException(String message) {
            super(message);
        }
    }
}
