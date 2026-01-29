package io.github.yasmramos.veld.processor.condition;

import java.util.Collections;
import java.util.Set;

/**
 * Representa una condición constante (siempre true o siempre false).
 * 
 * <p>Usado internamente para simplificar expresiones y como caso base en combinaciones:</p>
 * <ul>
 *   <li>AND con true = la otra condición</li>
 *   <li>OR con false = la otra condición</li>
 *   <li>AND con false = false</li>
 *   <li>OR con true = true</li>
 * </ul>
 */
public final class ConstantCondition implements ConditionExpression {

    private final boolean value;

    public ConstantCondition(boolean value) {
        this.value = value;
    }

    /**
     * Condición que siempre es true.
     */
    public static final ConstantCondition TRUE = new ConstantCondition(true);

    /**
     * Condición que siempre es false.
     */
    public static final ConstantCondition FALSE = new ConstantCondition(false);

    @Override
    public String toJavaCode(GenerationContext context) {
        return String.valueOf(value);
    }

    @Override
    public Set<String> getRequiredFlags() {
        return Collections.emptySet();
    }

    /**
     * Obtiene el valor de esta condición.
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Niega esta condición constante.
     */
    public ConstantCondition negate() {
        return new ConstantCondition(!value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
