package io.github.yasmramos.veld.processor.analyzer;

import io.github.yasmramos.veld.annotation.DependsOn;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;
import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.InjectionPoint;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes lifecycle annotations and explicit dependencies on components.
 * Handles @PostConstruct, @PreDestroy, and @DependsOn annotations.
 */
public final class LifecycleAnalyzer {
    
    private final Messager messager;
    
    public LifecycleAnalyzer(Messager messager) {
        this.messager = messager;
    }
    
    /**
     * Analyzes lifecycle methods (PostConstruct/PreDestroy) on a component.
     * 
     * @param typeElement the component type element
     * @param info the component info to update with lifecycle information
     */
    public void analyzeLifecycle(TypeElement typeElement, ComponentInfo info) {
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            
            ExecutableElement method = (ExecutableElement) enclosed;
            
            if (method.getAnnotation(PostConstruct.class) != null) {
                validateLifecycleMethod(method, "@PostConstruct");
                String descriptor = getMethodDescriptor(method);
                info.setPostConstruct(method.getSimpleName().toString(), descriptor);
            }
            
            if (method.getAnnotation(PreDestroy.class) != null) {
                validateLifecycleMethod(method, "@PreDestroy");
                String descriptor = getMethodDescriptor(method);
                info.setPreDestroy(method.getSimpleName().toString(), descriptor);
            }
        }
    }
    
    /**
     * Analyzes @DependsOn annotation for explicit initialization and destruction dependencies.
     * 
     * @param typeElement the component type element
     * @param info the component info to update with dependency information
     */
    public void analyzeDependsOn(TypeElement typeElement, ComponentInfo info) {
        DependsOn dependsOn = typeElement.getAnnotation(DependsOn.class);
        if (dependsOn == null) {
            return;
        }
        
        // Parse initialization dependencies
        String[] dependencies = dependsOn.value();
        if (dependencies.length > 0) {
            for (String dependency : dependencies) {
                if (dependency != null && !dependency.trim().isEmpty()) {
                    info.addExplicitDependency(dependency.trim());
                    messager.printMessage(Diagnostic.Kind.NOTE,
                        "[Veld]   -> Depends on bean: " + dependency.trim());
                }
            }
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Explicit dependencies: " + String.join(", ", dependencies));
        }
        
        // Parse destruction dependencies
        String[] destroyOrder = dependsOn.destroyOrder();
        if (destroyOrder.length > 0) {
            for (String dependency : destroyOrder) {
                if (dependency != null && !dependency.trim().isEmpty()) {
                    info.addExplicitDestructionDependency(dependency.trim());
                    messager.printMessage(Diagnostic.Kind.NOTE,
                        "[Veld]   -> Must outlive bean: " + dependency.trim());
                }
            }
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Destruction order dependencies: " + String.join(", ", destroyOrder));
        }
        
        // Parse destruction order value
        int destroyOrderValue = dependsOn.destroyOrderValue();
        if (destroyOrderValue != 0) {
            info.setDestroyOrderValue(destroyOrderValue);
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Destruction order value: " + destroyOrderValue);
        }
    }
    
    /**
     * Validates a lifecycle method for common errors.
     */
    private void validateLifecycleMethod(ExecutableElement method, String annotation) {
        if (!method.getParameters().isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "[Veld] " + annotation + " methods must have no parameters: " + method.getSimpleName());
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "[Veld] " + annotation + " cannot be applied to static methods: " + method.getSimpleName());
        }
    }
    
    /**
     * Gets the JVM descriptor for a method.
     */
    private String getMethodDescriptor(ExecutableElement method) {
        StringBuilder sb = new StringBuilder("(");
        for (javax.lang.model.element.VariableElement param : method.getParameters()) {
            sb.append(getTypeDescriptor(param.asType()));
        }
        sb.append(")");
        sb.append(getTypeDescriptor(method.getReturnType()));
        return sb.toString();
    }
    
    private String getTypeDescriptor(javax.lang.model.type.TypeMirror typeMirror) {
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
                javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) typeMirror;
                javax.lang.model.element.TypeElement typeElement = 
                    (javax.lang.model.element.TypeElement) declaredType.asElement();
                String internalName = typeElement.getQualifiedName().toString().replace('.', '/');
                return "L" + internalName + ";";
            default:
                return "L" + typeMirror.toString().replace('.', '/') + ";";
        }
    }
}
