package io.github.yasmramos.veld.processor.condition;

import io.github.yasmramos.veld.processor.VeldNode;
import io.github.yasmramos.veld.processor.condition.BeanExistenceGraph.ResolutionResult;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validador de condiciones en tiempo de compilación.
 * 
 * <p>Implementa las reglas estrictas del modelo determinístico de Veld:</p>
 * <ul>
 *   <li>Dependencias circulares condicionales</li>
 *   <li>Beans condicionales referenciados por beans incondicionales</li>
 *   <li>Condiciones que no pueden traducirse a Java simple</li>
 * </ul>
 */
public final class ConditionalValidator {

    private final Messager messager;

    public ConditionalValidator(Messager messager) {
        this.messager = messager;
    }

    /**
     * Valida todas las condiciones del grafo de beans.
     */
    public void validate(BeanExistenceGraph graph) {
        ResolutionResult result = graph.resolve();
        
        validateConditionalDependencyCycles(graph, result);
        validateUnconditionalDependsOnConditional(graph, result);
        validateUnresolvableConditions(graph, result);
        validateAmbiguousConditions(graph, result);
    }

    /**
     * Valida que no haya ciclos de dependencia condicional.
     * 
     * Un ciclo condicional ocurre cuando:
     * - Bean A depende de Bean B (condicionalmente)
     * - Bean B depende de Bean A (condicionalmente)
     * - Ninguno de los dos puede garantizarse sin el otro
     */
    private void validateConditionalDependencyCycles(BeanExistenceGraph graph, ResolutionResult result) {
        Set<String> presentBeans = result.presentBeans();
        Set<String> absentBeans = result.absentBeans();
        
        for (String bean : presentBeans) {
            VeldNode node = graph.getNodes().get(bean);
            if (node == null) continue;
            
            Set<String> deps = getAllDependencies(node);
            
            for (String dep : deps) {
                // Si A (presente) depende de B (ausente), es un problema
                if (absentBeans.contains(dep)) {
                    VeldNode depNode = graph.getNodes().get(dep);
                    boolean depIsConditional = depNode != null && graph.hasConditions(dep);
                    
                    if (depIsConditional) {
                        error(node,
                            "Conditional dependency %s is not satisfied. " +
                            "Conditional bean %s cannot be a dependency of %s because it does not exist. " +
                            "Consider using @Optional or making this dependency nullable.",
                            dep, dep, bean);
                    }
                }
            }
        }
    }

    /**
     * Valida que beans incondicionales no dependan de beans condicionales.
     * 
     * Esto es un warning porque puede ser válido en algunos casos,
     * pero generalmente indica un problema de diseño.
     */
    private void validateUnconditionalDependsOnConditional(BeanExistenceGraph graph, ResolutionResult result) {
        for (String bean : result.presentBeans()) {
            VeldNode node = graph.getNodes().get(bean);
            if (node == null) continue;
            
            // Verificar si el bean es incondicional
            if (graph.hasConditions(bean)) {
                continue; // Es condicional, skip
            }
            
            Set<String> deps = getAllDependencies(node);
            
            for (String dep : deps) {
                if (result.isConditional(dep)) {
                    warning(node,
                        "Unconditional bean %s depends on conditional bean %s. " +
                        "If the condition for %s is not met, %s will receive null. " +
                        "Consider using @Optional, @Nullable, or making %s conditional as well.",
                        bean, dep, dep, bean, bean);
                }
            }
        }
    }

    /**
     * Valida que todas las condiciones puedan resolverse.
     */
    private void validateUnresolvableConditions(BeanExistenceGraph graph, ResolutionResult result) {
        // Beans ausentes que podrían indicar condiciones malformadas
        for (String absentBean : result.absentBeans()) {
            VeldNode node = graph.getNodes().get(absentBean);
            if (node == null) continue;
            
            if (!graph.hasConditions(absentBean)) {
                warning(node,
                    "Bean %s is marked as absent but has no conditions. " +
                    "This may indicate an error in the dependency graph.",
                    absentBean);
            }
        }
    }

    /**
     * Valida condiciones ambiguas o conflictivas.
     */
    private void validateAmbiguousConditions(BeanExistenceGraph graph, ResolutionResult result) {
        for (String bean : result.presentBeans()) {
            VeldNode node = graph.getNodes().get(bean);
            if (node == null) continue;
            
            // Verificar @ConditionalOnBean y @ConditionalOnMissingBean simultáneos
            if (hasConflictingConditions(node)) {
                error(node,
                    "Conflicting conditions on %s. " +
                    "Cannot have both @ConditionalOnBean and @ConditionalOnMissingBean for the same bean.",
                    bean);
            }
        }
    }

    /**
     * Verifica si un nodo tiene condiciones conflictivas.
     */
    private boolean hasConflictingConditions(VeldNode node) {
        if (node.getConditionInfo() == null) {
            return false;
        }
        
        var info = node.getConditionInfo();
        boolean hasOnBean = !info.getPresentBeanConditions().isEmpty();
        boolean hasOnMissingBean = !info.getMissingBeanConditions().isEmpty();
        
        return hasOnBean && hasOnMissingBean;
    }

    /**
     * Obtiene todas las dependencias de un nodo (constructor, campos, métodos).
     */
    private Set<String> getAllDependencies(VeldNode node) {
        java.util.Set<String> deps = new java.util.LinkedHashSet<>();
        
        if (node.hasConstructorInjection()) {
            for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
                if (!param.isValueInjection() && !param.isOptionalWrapper() && !param.isProvider()) {
                    deps.add(param.getActualTypeName());
                }
            }
        }
        
        if (node.hasFieldInjections()) {
            for (VeldNode.FieldInjection field : node.getFieldInjections()) {
                if (!field.isValueInjection()) {
                    deps.add(field.getActualTypeName());
                }
            }
        }
        
        if (node.hasMethodInjections()) {
            for (VeldNode.MethodInjection method : node.getMethodInjections()) {
                for (VeldNode.ParameterInfo param : method.getParameters()) {
                    if (!param.isValueInjection() && !param.isOptionalWrapper() && !param.isProvider()) {
                        deps.add(param.getActualTypeName());
                    }
                }
            }
        }
        
        return deps;
    }

    /**
     * Genera un error de compilación.
     */
    private void error(VeldNode node, String message, Object... args) {
        String formatted = String.format(message, args);
        messager.printMessage(Diagnostic.Kind.ERROR, "[Veld] " + formatted, node.getElement());
    }

    /**
     * Genera un warning de compilación.
     */
    private void warning(VeldNode node, String message, Object... args) {
        String formatted = String.format(message, args);
        messager.printMessage(Diagnostic.Kind.WARNING, "[Veld] " + formatted, node.getElement());
    }

    /**
     * Resultado de la validación.
     */
    public record ValidationResult(
        boolean passed,
        List<String> errors,
        List<String> warnings
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of(), List.of());
        }

        public static ValidationResult failure(List<String> errors, List<String> warnings) {
            return new ValidationResult(errors.isEmpty(), errors, warnings);
        }
    }
}
