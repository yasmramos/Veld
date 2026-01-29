package io.github.yasmramos.veld.processor.condition;

import java.util.Set;

/**
 * Representa una expresión condicional que se traduce a Java compilable en tiempo de inicialización estática.
 * 
 * <p>Esta es la interfaz base para todas las condiciones soportadas por Veld:</p>
 * <ul>
 *   <li>{@link PropertyCondition} - @ConditionalOnProperty</li>
 *   <li>{@link BeanPresenceCondition} - @ConditionalOnBean / @ConditionalOnMissingBean</li>
 *   <li>{@link ClassCondition} - @ConditionalOnClass</li>
 *   <li>{@link CompositeCondition} - AND / OR / NOT combinators</li>
 * </ul>
 * 
 * <p>El principio fundamental es que cada condición debe poder expresarse como código Java simple
 * que se evalúa durante la inicialización estática del contenedor generado, sin reflection ni lambdas.</p>
 */
public sealed interface ConditionExpression 
    permits PropertyCondition, BeanPresenceCondition, ClassCondition, CompositeCondition, ConstantCondition {

    /**
     * Genera código Java que evalúa esta condición.
     * 
     * @param context Contexto de generación con acceso a flags y properties
     * @return String con código Java evaluable en tiempo de inicialización estática
     */
    String toJavaCode(GenerationContext context);

    /**
     * Dependencias de flags que esta condición necesita para evaluarse.
     * 
     * @return Set de nombres de flags requeridos
     */
    Set<String> getRequiredFlags();

    /**
     * Verifica si esta condición puede evaluarse con los flags disponibles.
     * 
     * @param context Contexto de generación
     * @return true si todos los flags requeridos están disponibles
     */
    default boolean canEvaluate(GenerationContext context) {
        Set<String> required = getRequiredFlags();
        for (String flag : required) {
            if (!context.hasFlag(flag)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convierte esta condición en su negación.
     * 
     * @return Nueva condición negada
     */
    default ConditionExpression negate() {
        return new CompositeCondition(this);
    }

    /**
     * Combina esta condición con otra usando AND.
     * 
     * @param other Otra condición
     * @return Nueva condición combinada con AND
     */
    default ConditionExpression and(ConditionExpression other) {
        return new CompositeCondition(this, other, CompositeCondition.Operator.AND);
    }

    /**
     * Combina esta condición con otra usando OR.
     * 
     * @param other Otra condición
     * @return Nueva condición combinada con OR
     */
    default ConditionExpression or(ConditionExpression other) {
        return new CompositeCondition(this, other, CompositeCondition.Operator.OR);
    }
}
