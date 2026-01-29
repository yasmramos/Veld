package io.github.yasmramos.veld.processor.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Representa una condición compuesta que combina múltiples condiciones usando operadores lógicos.
 * 
 * <p>Soporta los siguientes operadores:</p>
 * <ul>
 *   <li>{@code AND} - Ambas condiciones deben ser true</li>
 *   <li>{@code OR} - Al menos una condición debe ser true</li>
 *   <li>{@code NOT} - Niega una condición</li>
 * </ul>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * @ConditionalOnProperty("feature.x.enabled")
 * @ConditionalOnBean(DataSource.class)
 * }</pre>
 * 
 * <p>Genera:</p>
 * <pre>{@code
 * properties.hasProperty("feature.x.enabled") && hasBean_DataSource
 * }</pre>
 */
public final class CompositeCondition implements ConditionExpression {

    public enum Operator {
        AND,
        OR,
        NOT
    }

    private final ConditionExpression left;
    private final ConditionExpression right;
    private final Operator operator;

    /**
     * Crea una condición NOT unaria.
     */
    public CompositeCondition(ConditionExpression operand) {
        this(null, operand, Operator.NOT);
    }

    /**
     * Crea una condición binaria (AND u OR).
     */
    public CompositeCondition(ConditionExpression left, ConditionExpression right, Operator operator) {
        if (operator != Operator.NOT && (left == null || right == null)) {
            throw new IllegalArgumentException("AND y OR requieren ambas expresiones");
        }
        if (operator == Operator.NOT && right == null) {
            throw new IllegalArgumentException("NOT requiere una expresión");
        }
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public String toJavaCode(GenerationContext context) {
        return switch (operator) {
            case AND -> {
                String leftCode = left.toJavaCode(context);
                String rightCode = right.toJavaCode(context);
                yield "(" + leftCode + " && " + rightCode + ")";
            }
            case OR -> {
                String leftCode = left.toJavaCode(context);
                String rightCode = right.toJavaCode(context);
                yield "(" + leftCode + " || " + rightCode + ")";
            }
            case NOT -> {
                String operandCode = right.toJavaCode(context);
                yield "!(" + operandCode + ")";
            }
        };
    }

    @Override
    public Set<String> getRequiredFlags() {
        java.util.Set<String> flags = new java.util.LinkedHashSet<>();
        
        if (left != null) {
            flags.addAll(left.getRequiredFlags());
        }
        if (right != null) {
            flags.addAll(right.getRequiredFlags());
        }
        
        return flags;
    }

    /**
     * Obtiene la expresión izquierda (para AND/OR).
     */
    public ConditionExpression getLeft() {
        return left;
    }

    /**
     * Obtiene la expresión derecha.
     */
    public ConditionExpression getRight() {
        return right;
    }

    /**
     * Obtiene el operador.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Verifica si esta condición es una negación simple.
     */
    public boolean isNegation() {
        return operator == Operator.NOT;
    }

    /**
     * Si esta condición es una negación, obtiene la expresión negada.
     */
    public ConditionExpression getNegatedExpression() {
        if (operator != Operator.NOT) {
            throw new IllegalStateException("No es una negación");
        }
        return right;
    }

    /**
     * Descompone una condición compuesta en sus partes.
     * 
     * @param cond Condición a descomponer
     * @return Lista de condiciones hoja (sin CompositeCondition)
     */
    public static List<ConditionExpression> flatten(ConditionExpression cond) {
        List<ConditionExpression> result = new ArrayList<>();
        flattenRecursive(cond, result);
        return result;
    }

    private static void flattenRecursive(ConditionExpression cond, List<ConditionExpression> result) {
        if (cond instanceof CompositeCondition composite) {
            if (composite.operator == Operator.AND || composite.operator == Operator.OR) {
                flattenRecursive(composite.left, result);
                flattenRecursive(composite.right, result);
            } else {
                // NOT - no aplanar
                result.add(cond);
            }
        } else {
            result.add(cond);
        }
    }

    /**
     * Crea una condición AND a partir de múltiples condiciones.
     */
    public static ConditionExpression andAll(List<ConditionExpression> conditions) {
        if (conditions.isEmpty()) {
            return new ConstantCondition(true);
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        
        ConditionExpression result = conditions.get(0);
        for (int i = 1; i < conditions.size(); i++) {
            result = new CompositeCondition(result, conditions.get(i), Operator.AND);
        }
        return result;
    }

    /**
     * Crea una condición OR a partir de múltiples condiciones.
     */
    public static ConditionExpression orAll(List<ConditionExpression> conditions) {
        if (conditions.isEmpty()) {
            return new ConstantCondition(false);
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        
        ConditionExpression result = conditions.get(0);
        for (int i = 1; i < conditions.size(); i++) {
            result = new CompositeCondition(result, conditions.get(i), Operator.OR);
        }
        return result;
    }

    @Override
    public String toString() {
        return switch (operator) {
            case AND -> left + " && " + right;
            case OR -> left + " || " + right;
            case NOT -> "!(" + right + ")";
        };
    }
}
