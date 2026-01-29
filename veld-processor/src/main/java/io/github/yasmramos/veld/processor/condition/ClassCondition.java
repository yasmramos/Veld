package io.github.yasmramos.veld.processor.condition;

import java.util.Set;

/**
 * Representa una condición basada en la presencia de una clase en el classpath.
 * 
 * <p>Traduce la anotación @ConditionalOnClass a código Java. En el modelo determinístico de Veld,
 * esta condición se evalúa en tiempo de compilación ya que el código referencia la clase directamente:</p>
 * <ul>
 *   <li>Si el código compila, la clase existe en el classpath</li>
 *   <li>Si el código no compila, la clase no está disponible</li>
 * </ul>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * @ConditionalOnClass(ObjectMapper.class)
 * }</pre>
 * 
 * <p>En Veld, esto genera:</p>
 * <pre>{@code
 * true // @ConditionalOnClass: com.fasterxml.jackson.databind.ObjectMapper está en el classpath
 * }</pre>
 * 
 * <p>La evaluación es determinística: si la clase está referenciada en el código,
 * debe estar disponible en tiempo de compilación.</p>
 */
public record ClassCondition(
    Set<String> classNames
) implements ConditionExpression {

    private static final String CLASS_PREFIX = "CLASS_";

    public ClassCondition {
        if (classNames == null || classNames.isEmpty()) {
            throw new IllegalArgumentException("classNames no puede ser null o vacío");
        }
    }

    /**
     * Crea una ClassCondition con un solo nombre de clase.
     */
    public static ClassCondition of(String className) {
        return new ClassCondition(Set.of(className));
    }

    /**
     * Crea una ClassCondition con múltiples nombres de clases.
     */
    public static ClassCondition of(Set<String> classNames) {
        return new ClassCondition(classNames);
    }

    @Override
    public String toJavaCode(GenerationContext context) {
        // En el modelo determinístico de Veld, si el código compila, la clase existe
        // Por lo tanto, la condición siempre es true
        
        if (classNames.size() == 1) {
            String className = classNames.iterator().next();
            return "true; // @ConditionalOnClass: " + className;
        }
        
        // Múltiples clases: todas deben estar presentes
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String className : classNames) {
            if (i > 0) {
                sb.append(" && ");
            }
            sb.append("true // ").append(className);
            i++;
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public Set<String> getRequiredFlags() {
        // @ConditionalOnClass no requiere flags porque siempre es true si compila
        return Set.of();
    }

    /**
     * Genera un comentario informativo sobre la evaluación.
     */
    public String getEvaluationComment() {
        if (classNames.size() == 1) {
            return "@ConditionalOnClass: " + classNames.iterator().next() + " está en el classpath";
        }
        
        return "@ConditionalOnClass: todas las clases [" + 
            String.join(", ", classNames) + "] están en el classpath";
    }

    /**
     * Verifica si esta condición involucra una clase específica.
     */
    public boolean involvesClass(String className) {
        return classNames.contains(className);
    }
}
