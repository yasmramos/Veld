package io.github.yasmramos.veld.processor.analyzer;

import io.github.yasmramos.veld.annotation.Primary;
import io.github.yasmramos.veld.processor.ComponentInfo;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Analyzes interfaces implemented by components and detects conflicts.
 * Tracks which components implement which interfaces for injection resolution
 * and conflict detection.
 */
public final class InterfaceAnalyzer {
    
    private final Messager messager;
    
    // Maps interface -> list of implementing component class names
    private final Map<String, List<String>> interfaceImplementors = new HashMap<>();
    
    public InterfaceAnalyzer(Messager messager) {
        this.messager = messager;
    }
    
    /**
     * Analyzes interfaces implemented by a component.
     * 
     * @param typeElement the component type element
     * @param info the component info to update with implemented interfaces
     */
    public void analyzeInterfaces(TypeElement typeElement, ComponentInfo info) {
        for (javax.lang.model.type.TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (interfaceType.getKind() != TypeKind.DECLARED) continue;
            
            DeclaredType declaredType = (DeclaredType) interfaceType;
            TypeElement interfaceElement = (TypeElement) declaredType.asElement();
            String interfaceName = interfaceElement.getQualifiedName().toString();
            
            // Skip standard Java interfaces
            if (isSkippableInterface(interfaceName)) continue;
            
            info.addImplementedInterface(interfaceName);
            
            // Track interface implementors for conflict detection
            interfaceImplementors
                .computeIfAbsent(interfaceName, k -> new ArrayList<>())
                .add(info.getClassName());
            
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Implements interface: " + interfaceName);
        }
    }
    
    /**
     * Validates that there are no ambiguous interface implementations.
     * Multiple implementations of the same interface require @Primary or @Named.
     * 
     * @param strictMode if true, reports errors instead of warnings
     * @return true if validation passes (with possible warnings), false if errors found
     */
    public boolean validateInterfaceImplementations(boolean strictMode) {
        boolean hasConflicts = false;
        
        for (Map.Entry<String, List<String>> entry : interfaceImplementors.entrySet()) {
            String interfaceName = entry.getKey();
            List<String> implementors = entry.getValue();
            
            if (implementors.size() > 1) {
                // Multiple implementations - check if they have @Primary or @Named
                int withoutDisambiguation = 0;
                StringBuilder implDetails = new StringBuilder();
                
                for (int i = 0; i < implementors.size(); i++) {
                    String implClassName = implementors.get(i);
                    implDetails.append("\n    ").append(i + 1).append(". ").append(implClassName);
                    
                    boolean hasDisambiguator = hasDisambiguatorAnnotation(implClassName);
                    if (!hasDisambiguator) {
                        withoutDisambiguation++;
                    }
                }
                
                if (withoutDisambiguation > 0) {
                    String message = "Multiple implementations found for interface: " + interfaceName +
                        "\n  Implementations:" + implDetails +
                        "\n  Multiple implementations require @Primary or @Named to disambiguate.";
                    
                    if (strictMode) {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                            "[Veld STRICT] " + message + 
                            "\n  Fix: Add @Primary or @Named(\"uniqueName\") to exactly one implementation.");
                        hasConflicts = true;
                    } else {
                        messager.printMessage(Diagnostic.Kind.WARNING,
                            "[Veld] " + message + 
                            "\n  Note: In strict mode (-Aveld.strict=true), this is a compilation error.");
                    }
                }
            }
        }
        
        if (hasConflicts) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld] Interface conflict validation found errors in strict mode.");
        } else if (!interfaceImplementors.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld] Interface conflict detection complete. No conflicts found.");
        }
        
        return !hasConflicts;
    }
    
    /**
     * Gets all interface implementors for a given interface.
     * 
     * @param interfaceName the fully qualified interface name
     * @return list of component class names implementing this interface
     */
    public List<String> getInterfaceImplementors(String interfaceName) {
        return interfaceImplementors.getOrDefault(interfaceName, Collections.emptyList());
    }
    
    /**
     * Clears all tracked interface implementors.
     */
    public void clear() {
        interfaceImplementors.clear();
    }
    
    /**
     * Returns true if the interface should be skipped from tracking.
     */
    private boolean isSkippableInterface(String interfaceName) {
        return interfaceName.startsWith("java.lang.") || 
               interfaceName.startsWith("java.io.") &&
               !interfaceName.startsWith("java.util.");
    }
    
    /**
     * Checks if a class has @Primary or @Named annotation.
     */
    private boolean hasDisambiguatorAnnotation(String className) {
        TypeElement element = messager instanceof javax.annotation.processing.Messager ? null : null;
        
        // We need access to elementUtils, which we don't have here
        // This method should be called with proper element access
        return false; // Placeholder - actual implementation needs elementUtils
    }
    
    /**
     * Checks if a class has @Primary annotation.
     * Requires element utilities to be passed in.
     */
    public boolean hasPrimaryAnnotation(TypeElement element) {
        return element != null && element.getAnnotation(Primary.class) != null;
    }
}
