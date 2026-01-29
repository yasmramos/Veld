package io.github.yasmramos.veld.processor.analyzer;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.processor.ComponentInfo;
import io.github.yasmramos.veld.processor.ConditionInfo;
import io.github.yasmramos.veld.processor.InjectionPoint;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes conditional annotations on components.
 * Supports @ConditionalOnProperty, @ConditionalOnClass, @ConditionalOnMissingBean,
 * @ConditionalOnBean, and @Profile.
 */
public final class ConditionAnalyzer {
    
    private final Messager messager;
    private final Elements elementUtils;
    
    public ConditionAnalyzer(Messager messager, Elements elementUtils) {
        this.messager = messager;
        this.elementUtils = elementUtils;
    }
    
    /**
     * Analyzes all conditional annotations on a component type element.
     * 
     * @param typeElement the component type element
     * @return ConditionInfo with all analyzed conditions, or null if no conditions
     */
    public ConditionInfo analyze(TypeElement typeElement) {
        ConditionInfo conditionInfo = new ConditionInfo();
        boolean hasConditions = false;
        
        try {
            // Check for @ConditionalOnProperty
            ConditionalOnProperty propertyCondition = typeElement.getAnnotation(ConditionalOnProperty.class);
            if (propertyCondition != null) {
                hasConditions |= analyzePropertyCondition(propertyCondition, conditionInfo);
            }
            
            // Check for @ConditionalOnClass
            ConditionalOnClass classCondition = typeElement.getAnnotation(ConditionalOnClass.class);
            if (classCondition != null) {
                hasConditions |= analyzeClassCondition(classCondition, conditionInfo);
            }
            
            // Check for @ConditionalOnMissingBean
            ConditionalOnMissingBean missingBeanCondition = typeElement.getAnnotation(ConditionalOnMissingBean.class);
            if (missingBeanCondition != null) {
                hasConditions |= analyzeMissingBeanCondition(missingBeanCondition, conditionInfo);
            }
            
            // Check for @ConditionalOnBean
            ConditionalOnBean presentBeanCondition = typeElement.getAnnotation(ConditionalOnBean.class);
            if (presentBeanCondition != null) {
                hasConditions |= analyzePresentBeanCondition(presentBeanCondition, conditionInfo);
            }
            
            // Check for @Profile
            Profile profileAnnotation = typeElement.getAnnotation(Profile.class);
            if (profileAnnotation != null) {
                hasConditions |= analyzeProfileCondition(profileAnnotation, conditionInfo);
            }
            
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING, 
                "[Veld] Error processing conditional annotations: " + e.getMessage());
        }
        
        return hasConditions ? conditionInfo : null;
    }
    
    private boolean analyzePropertyCondition(ConditionalOnProperty propertyCondition, ConditionInfo conditionInfo) {
        try {
            conditionInfo.addPropertyCondition(
                propertyCondition.name(),
                propertyCondition.havingValue(),
                propertyCondition.matchIfMissing()
            );
            messager.printMessage(Diagnostic.Kind.NOTE, 
                "[Veld]   -> Conditional on property: " + propertyCondition.name());
            return true;
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                "[Veld] Could not process @ConditionalOnProperty annotation: " + e.getMessage());
            return false;
        }
    }
    
    private boolean analyzeClassCondition(ConditionalOnClass classCondition, ConditionInfo conditionInfo) {
        List<String> classNames = new ArrayList<>();
        
        try {
            // Get class names from 'name' attribute
            for (String name : classCondition.name()) {
                if (!name.isEmpty()) {
                    classNames.add(name);
                }
            }
            
            // Get class names from 'value' attribute (Class[] types)
            for (Class<?> clazz : classCondition.value()) {
                classNames.add(clazz.getName());
            }
        } catch (MirroredTypesException e) {
            for (TypeMirror mirror : e.getTypeMirrors()) {
                try {
                    String typeName = getTypeName(mirror);
                    if (typeName != null && !typeName.isEmpty()) {
                        classNames.add(typeName);
                    }
                } catch (Exception ex) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                        "[Veld] Could not resolve type from conditional annotation: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                "[Veld] Could not process @ConditionalOnClass annotation: " + e.getMessage());
        }
        
        if (!classNames.isEmpty()) {
            conditionInfo.addClassCondition(classNames);
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Conditional on class: " + String.join(", ", classNames));
            return true;
        }
        return false;
    }
    
    private boolean analyzeMissingBeanCondition(ConditionalOnMissingBean missingBeanCondition, ConditionInfo conditionInfo) {
        List<String> beanTypes = new ArrayList<>();
        List<String> beanNames = new ArrayList<>();
        
        // Get bean types
        try {
            for (Class<?> clazz : missingBeanCondition.value()) {
                beanTypes.add(clazz.getName());
            }
        } catch (MirroredTypesException e) {
            for (TypeMirror mirror : e.getTypeMirrors()) {
                try {
                    String typeName = getTypeName(mirror);
                    if (typeName != null && !typeName.isEmpty()) {
                        beanTypes.add(typeName);
                    }
                } catch (Exception ex) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                        "[Veld] Could not resolve type from conditional annotation: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                "[Veld] Could not process @ConditionalOnMissingBean annotation types: " + e.getMessage());
        }
        
        // Get bean names
        try {
            for (String name : missingBeanCondition.name()) {
                if (!name.isEmpty()) {
                    beanNames.add(name);
                }
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                "[Veld] Could not process @ConditionalOnMissingBean annotation names: " + e.getMessage());
        }
        
        boolean hasConditions = false;
        if (!beanTypes.isEmpty()) {
            conditionInfo.addMissingBeanTypeCondition(beanTypes);
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Conditional on missing bean types: " + String.join(", ", beanTypes));
            hasConditions = true;
        }
        if (!beanNames.isEmpty()) {
            conditionInfo.addMissingBeanNameCondition(beanNames);
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Conditional on missing bean names: " + String.join(", ", beanNames));
            hasConditions = true;
        }
        
        return hasConditions;
    }
    
    private boolean analyzePresentBeanCondition(ConditionalOnBean presentBeanCondition, ConditionInfo conditionInfo) {
        List<String> presentBeanTypes = new ArrayList<>();
        List<String> presentBeanNames = new ArrayList<>();
        boolean matchAll = presentBeanCondition.strategy() == ConditionalOnBean.Strategy.ALL;
        
        // Get bean types
        try {
            for (Class<?> clazz : presentBeanCondition.value()) {
                presentBeanTypes.add(clazz.getName());
            }
        } catch (MirroredTypesException e) {
            for (TypeMirror mirror : e.getTypeMirrors()) {
                try {
                    String typeName = getTypeName(mirror);
                    if (typeName != null && !typeName.isEmpty()) {
                        presentBeanTypes.add(typeName);
                    }
                } catch (Exception ex) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                        "[Veld] Could not resolve type from @ConditionalOnBean: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                "[Veld] Could not process @ConditionalOnBean annotation types: " + e.getMessage());
        }
        
        // Get bean names
        try {
            for (String name : presentBeanCondition.name()) {
                if (!name.isEmpty()) {
                    presentBeanNames.add(name);
                }
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                "[Veld] Could not process @ConditionalOnBean annotation names: " + e.getMessage());
        }
        
        if (!presentBeanTypes.isEmpty() || !presentBeanNames.isEmpty()) {
            conditionInfo.addPresentBeanCondition(presentBeanTypes, presentBeanNames, matchAll);
            String strategy = matchAll ? "ALL" : "ANY";
            String beans = !presentBeanTypes.isEmpty() ? 
                String.join(", ", presentBeanTypes) : 
                String.join(", ", presentBeanNames);
            messager.printMessage(Diagnostic.Kind.NOTE,
                "[Veld]   -> Conditional on present beans (strategy=" + strategy + "): " + beans);
            return true;
        }
        return false;
    }
    
    private boolean analyzeProfileCondition(Profile profileAnnotation, ConditionInfo conditionInfo) {
        List<String> profiles = new ArrayList<>();
        
        // Get profiles from value attribute
        for (String profile : profileAnnotation.value()) {
            if (!profile.isEmpty()) {
                profiles.add(profile);
            }
        }
        
        // Also check 'name' attribute as alias
        if (profiles.isEmpty()) {
            String nameValue = profileAnnotation.name();
            if (!nameValue.isEmpty()) {
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
            messager.printMessage(Diagnostic.Kind.NOTE, "[Veld] " + profileNote.toString());
            return true;
        }
        return false;
    }
    
    private String getTypeName(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return null;
        }
        if (typeMirror instanceof javax.lang.model.type.DeclaredType) {
            javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) typeMirror;
            javax.lang.model.element.TypeElement typeElement = 
                (javax.lang.model.element.TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString();
        }
        return typeMirror.toString();
    }
}
