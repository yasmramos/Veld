package io.github.yasmramos.veld.processor.condition;

import io.github.yasmramos.veld.processor.VeldNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the condition graph between beans to determine:
 * <ul>
 *   <li>Which beans actually exist based on their conditions</li>
 *   <li>In what order they should be created (topological order)</li>
 *   <li>What existence flags are needed</li>
 * </ul>
 *
 * <p>This component implements the deterministic condition resolution algorithm
 * which is the heart of Veld's "Compile-Time First with Deterministic Generated Runtime" model.</p>
 */
public final class BeanExistenceGraph {

    private final Map<String, VeldNode> nodes;
    private final Map<String, ConditionExpression> conditions;
    private final Map<String, Set<String>> dependencies;

    private BeanExistenceGraph(Builder builder) {
        this.nodes = Map.copyOf(builder.nodes);
        this.conditions = Map.copyOf(builder.conditions);
        this.dependencies = new LinkedHashMap<>();
        buildDependencyGraph();
    }

    /**
     * Builds the dependency graph between beans.
     */
    private void buildDependencyGraph() {
        for (Map.Entry<String, VeldNode> entry : nodes.entrySet()) {
            String bean = entry.getKey();
            VeldNode node = entry.getValue();
            Set<String> deps = new LinkedHashSet<>();
            
            // Constructor dependencies
            if (node.hasConstructorInjection()) {
                for (VeldNode.ParameterInfo param : node.getConstructorInfo().getParameters()) {
                    if (!param.isValueInjection() && !param.isOptionalWrapper() && !param.isProvider()) {
                        deps.add(param.getActualTypeName());
                    }
                }
            }
            
            // Field dependencies
            if (node.hasFieldInjections()) {
                for (VeldNode.FieldInjection field : node.getFieldInjections()) {
                    if (!field.isValueInjection()) {
                        deps.add(field.getActualTypeName());
                    }
                }
            }
            
            // Method dependencies
            if (node.hasMethodInjections()) {
                for (VeldNode.MethodInjection method : node.getMethodInjections()) {
                    for (VeldNode.ParameterInfo param : method.getParameters()) {
                        if (!param.isValueInjection() && !param.isOptionalWrapper() && !param.isProvider()) {
                            deps.add(param.getActualTypeName());
                        }
                    }
                }
            }
            
            dependencies.put(bean, deps);
        }
    }

    /**
     * Resolves which beans exist and which don't, based on their conditions.
     *
     * <p>The algorithm iterates to convergence, resolving conditional beans
     * in topological order of dependencies.</p>
     *
     * @return Result with the graph resolution
     */
    public ResolutionResult resolve() {
        // Step 1: Identify unconditional beans (always exist)
        Set<String> present = new LinkedHashSet<>();
        Set<String> absent = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();

        for (String bean : nodes.keySet()) {
            ConditionExpression cond = conditions.get(bean);
            if (cond == null) {
                // Unconditional bean - always exists
                present.add(bean);
            } else {
                unresolved.add(bean);
            }
        }

        // Step 2: Resolve conditional beans in topological order
        boolean changed;
        int maxIterations = unresolved.size() + 1; // Avoid infinite loops
        
        do {
            changed = false;
            Set<String> newlyResolved = new LinkedHashSet<>();
            
            for (String bean : unresolved) {
                if (present.contains(bean) || absent.contains(bean)) {
                    continue;
                }
                
                ConditionExpression cond = conditions.get(bean);
                if (cond == null) {
                    present.add(bean);
                    newlyResolved.add(bean);
                    changed = true;
                } else if (canEvaluate(cond, present, absent)) {
                    // All dependencies are resolved, evaluate the condition
                    if (evaluateCondition(cond, present, absent)) {
                        present.add(bean);
                    } else {
                        absent.add(bean);
                    }
                    newlyResolved.add(bean);
                    changed = true;
                }
            }
            
            unresolved.removeAll(newlyResolved);
            maxIterations--;
            
        } while (changed && maxIterations > 0);

        // Beans that couldn't be resolved are marked as absent (conservative)
        absent.addAll(unresolved);
        unresolved.clear();

        return new ResolutionResult(present, absent, conditions, dependencies);
    }

    /**
     * Checks if a condition can be evaluated.
     */
    private boolean canEvaluate(ConditionExpression cond, Set<String> present, Set<String> absent) {
        for (String flag : cond.getRequiredFlags()) {
            if (isBeanFlag(flag)) {
                String bean = extractBeanFromFlag(flag);
                if (!present.contains(bean) && !absent.contains(bean)) {
                    return false; // Cannot evaluate yet
                }
            }
        }
        return true;
    }

    /**
     * Evaluates a condition deterministically.
     */
    private boolean evaluateCondition(ConditionExpression cond, Set<String> present, Set<String> absent) {
        GenerationContext ctx = GenerationContext.builder()
            .addPresentBeans(present)
            .addAbsentBeans(absent)
            .build();
        
        // For PropertyCondition, we use default values
        // For BeanPresenceCondition, we use the graph resolution
        // For ClassCondition, it's always true
        
        if (cond instanceof PropertyCondition propCond) {
            // At generation time, we assume true (will be evaluated at runtime)
            // But check if there are dependencies that are already absent
            for (String flag : propCond.getRequiredFlags()) {
                if (isBeanFlag(flag)) {
                    String bean = extractBeanFromFlag(flag);
                    if (absent.contains(bean)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        if (cond instanceof BeanPresenceCondition beanCond) {
            // Check if required beans are present
            for (String type : beanCond.beanTypes()) {
                if (beanCond.mustBePresent() && !present.contains(type)) {
                    return false;
                }
                if (!beanCond.mustBePresent() && present.contains(type)) {
                    return false;
                }
            }
            for (String name : beanCond.beanNames()) {
                // By name, assume that if the type is present, the name is too
                if (beanCond.mustBePresent() && !present.contains(name) && !present.contains("bean:" + name)) {
                    return false;
                }
                if (!beanCond.mustBePresent() && (present.contains(name) || present.contains("bean:" + name))) {
                    return false;
                }
            }
            return true;
        }
        
        if (cond instanceof ClassCondition) {
            return true; // Always true if it compiles
        }
        
        if (cond instanceof CompositeCondition composite) {
            return evaluateComposite(composite, present, absent);
        }
        
        return true; // By default, assume it exists
    }

    private boolean evaluateComposite(CompositeCondition cond, Set<String> present, Set<String> absent) {
        return switch (cond.getOperator()) {
            case AND -> evaluateCondition(cond.getLeft(), present, absent) 
                     && evaluateCondition(cond.getRight(), present, absent);
            case OR -> evaluateCondition(cond.getLeft(), present, absent) 
                    || evaluateCondition(cond.getRight(), present, absent);
            case NOT -> !evaluateCondition(cond.getRight(), present, absent);
        };
    }

    /**
     * Checks if a flag corresponds to a bean.
     */
    private boolean isBeanFlag(String flag) {
        return flag.startsWith("HAS_BEAN_") || flag.startsWith("BEAN_") || flag.startsWith("BEAN_NAME_");
    }

    /**
     * Extracts the bean name from a flag.
     */
    private String extractBeanFromFlag(String flag) {
        if (flag.startsWith("BEAN_NAME_")) {
            return "bean:" + flag.substring(10).toLowerCase().replace('_', '.');
        }
        return flag.substring(5).toLowerCase().replace('_', '.');
    }

    /**
     * Gets the bean creation order (topological).
     */
    public List<String> getCreationOrder(Set<String> presentBeans) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> temp = new LinkedHashSet<>();
        
        for (String bean : presentBeans) {
            if (!visited.contains(bean)) {
                topoSort(bean, visited, temp, order, presentBeans);
            }
        }
        
        return order;
    }

    private void topoSort(String bean, Set<String> visited, Set<String> temp, 
                          List<String> order, Set<String> presentBeans) {
        if (temp.contains(bean)) {
            // Cycle detected - ignore for this bean
            return;
        }
        if (visited.contains(bean)) {
            return;
        }
        
        temp.add(bean);
        
        Set<String> deps = dependencies.getOrDefault(bean, Collections.emptySet());
        for (String dep : deps) {
            if (presentBeans.contains(dep)) {
                topoSort(dep, visited, temp, order, presentBeans);
            }
        }
        
        temp.remove(bean);
        visited.add(bean);
        order.add(bean);
    }

    /**
     * Gets all nodes in the graph.
     */
    public Map<String, VeldNode> getNodes() {
        return nodes;
    }

    /**
     * Gets the condition of a specific bean.
     */
    public ConditionExpression getCondition(String beanClassName) {
        return conditions.get(beanClassName);
    }

    /**
     * Checks if a bean has conditions.
     */
    public boolean hasConditions(String beanClassName) {
        return conditions.containsKey(beanClassName);
    }

    /**
     * Generates the existence flag name for a bean.
     * The name includes the "HAS_BEAN_" prefix and uses only the simple class name.
     */
    public String getExistenceFlagName(String beanClassName) {
        // Extract only the simple class name (without package)
        String simpleName = beanClassName.substring(beanClassName.lastIndexOf('.') + 1);
        return "HAS_BEAN_" + sanitizeClassName(simpleName);
    }

    /**
     * Sanitizes a class name for use in a flag name.
     * Converts to uppercase for consistency.
     */
    private String sanitizeClassName(String className) {
        return className.toUpperCase();
    }

    /**
     * Gets all bean class names in the graph.
     */
    public Set<String> getAllBeanClassNames() {
        return nodes.keySet();
    }

    /**
     * Gets the node for a specific bean.
     */
    public VeldNode getNode(String beanClassName) {
        return nodes.get(beanClassName);
    }

    /**
     * Result of the graph resolution.
     */
    public record ResolutionResult(
        Set<String> presentBeans,
        Set<String> absentBeans,
        Map<String, ConditionExpression> conditions,
        Map<String, Set<String>> dependencies
    ) {
        /**
         * Checks if a bean exists.
         */
        public boolean exists(String beanClassName) {
            return presentBeans.contains(beanClassName);
        }

        /**
         * Checks if a bean is conditional (has conditions and was evaluated).
         */
        public boolean isConditional(String beanClassName) {
            return conditions.containsKey(beanClassName);
        }

        /**
         * Gets the condition of a bean.
         */
        public ConditionExpression getCondition(String beanClassName) {
            return conditions.get(beanClassName);
        }

        /**
         * Gets the beans in creation order (topological).
         */
        public List<String> getCreationOrder() {
            List<String> order = new ArrayList<>();
            Set<String> visited = new LinkedHashSet<>();
            Set<String> temp = new LinkedHashSet<>();

            for (String bean : presentBeans) {
                if (!visited.contains(bean)) {
                    topoSort(bean, visited, temp, order);
                }
            }

            return order;
        }

        private void topoSort(String bean, Set<String> visited, Set<String> temp, List<String> order) {
            if (temp.contains(bean)) {
                return; // Cycle - ignore
            }
            if (visited.contains(bean)) {
                return;
            }

            temp.add(bean);

            Set<String> deps = dependencies.getOrDefault(bean, Collections.emptySet());
            for (String dep : deps) {
                if (presentBeans.contains(dep)) {
                    topoSort(dep, visited, temp, order);
                }
            }

            temp.remove(bean);
            visited.add(bean);
            order.add(bean);
        }

        /**
         * Gets all beans that were evaluated (present or absent).
         */
        public Set<String> getEvaluatedBeans() {
            Set<String> evaluated = new LinkedHashSet<>(presentBeans);
            evaluated.addAll(absentBeans);
            return evaluated;
        }
    }

    /**
     * Builder for BeanExistenceGraph.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, VeldNode> nodes = new LinkedHashMap<>();
        private final Map<String, ConditionExpression> conditions = new LinkedHashMap<>();

        private Builder() {}

        public Builder addNode(String beanClassName, VeldNode node) {
            this.nodes.put(beanClassName, node);
            return this;
        }

        public Builder addNodes(Map<String, VeldNode> nodes) {
            this.nodes.putAll(nodes);
            return this;
        }

        public Builder addCondition(String beanClassName, ConditionExpression condition) {
            this.conditions.put(beanClassName, condition);
            return this;
        }

        public Builder addConditions(Map<String, ConditionExpression> conditions) {
            this.conditions.putAll(conditions);
            return this;
        }

        public BeanExistenceGraph build() {
            if (nodes.isEmpty()) {
                throw new IllegalStateException("Must add at least one node");
            }
            return new BeanExistenceGraph(this);
        }
    }
}
