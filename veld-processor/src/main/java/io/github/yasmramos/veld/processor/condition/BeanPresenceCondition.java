package io.github.yasmramos.veld.processor.condition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa una condición basada en la presencia o ausencia de otros beans.
 * 
 * <p>Traduce las anotaciones @ConditionalOnBean y @ConditionalOnMissingBean a código Java:</p>
 * <ul>
 *   <li>{@code mustBePresent = true} - @ConditionalOnBean: el bean DEBE existir</li>
 *   <li>{@code mustBePresent = false} - @ConditionalOnMissingBean: el bean NO debe existir</li>
 * </ul>
 * 
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * @ConditionalOnBean(DataSource.class)
 * @ConditionalOnMissingBean(name = "mockDataSource")
 * }</pre>
 * 
 * <p>Genera código como:</p>
 * <pre>{@code
 * hasBean_io_github_veld_example_DataSource && !hasBean_mockDataSource
 * }</pre>
 */
public record BeanPresenceCondition(
    Set<String> beanTypes,
    Set<String> beanNames,
    boolean mustBePresent
) implements ConditionExpression {

    // Note: Using same prefix as GenerationContext.getFlagForBeanType()
    // The flag names are generated with HAS_BEAN_ prefix and sanitized class names
    private static final String BEAN_TYPE_PREFIX = "HAS_BEAN_";
    private static final String BEAN_NAME_PREFIX = "HAS_BEAN_";

    public BeanPresenceCondition {
        if (beanTypes == null) {
            beanTypes = Set.of();
        }
        if (beanNames == null) {
            beanNames = Set.of();
        }
        if (beanTypes.isEmpty() && beanNames.isEmpty()) {
            throw new IllegalArgumentException("Al menos beanTypes o beanNames debe tener elementos");
        }
    }

    /**
     * Crea una condición para @ConditionalOnBean.
     */
    public static BeanPresenceCondition onBean(Set<String> beanTypes, Set<String> beanNames) {
        return new BeanPresenceCondition(beanTypes, beanNames, true);
    }

    /**
     * Crea una condición para @ConditionalOnMissingBean.
     */
    public static BeanPresenceCondition onMissingBean(Set<String> beanTypes, Set<String> beanNames) {
        return new BeanPresenceCondition(beanTypes, beanNames, false);
    }

    @Override
    public String toJavaCode(GenerationContext context) {
        List<String> checks = new ArrayList<>();

        // Checks por tipo de bean
        for (String type : beanTypes) {
            String flag = context.getFlagForBeanType(type);
            checks.add(mustBePresent ? flag : "!" + flag);
        }

        // Checks por nombre de bean
        for (String name : beanNames) {
            String flag = context.getFlagForBeanName(name);
            checks.add(mustBePresent ? flag : "!" + flag);
        }

        if (checks.isEmpty()) {
            return mustBePresent ? "false" : "true";
        }

        // Si matchAll es true (por defecto), usar AND
        return String.join(" && ", checks);
    }

    /**
     * Genera código para verificar si ALGUNO de los beans está presente (lógica OR).
     */
    public String toJavaCodeAnyPresent(GenerationContext context) {
        List<String> checks = new ArrayList<>();

        for (String type : beanTypes) {
            String flag = context.getFlagForBeanType(type);
            checks.add(flag);
        }

        for (String name : beanNames) {
            String flag = context.getFlagForBeanName(name);
            checks.add(flag);
        }

        if (checks.isEmpty()) {
            return "false";
        }

        return String.join(" || ", checks);
    }

    @Override
    public Set<String> getRequiredFlags() {
        Set<String> flags = new LinkedHashSet<>();

        for (String type : beanTypes) {
            // Use same sanitization as GenerationContext.getFlagForBeanType()
            flags.add(GenerationContext.getExistenceFlagName(type));
        }

        for (String name : beanNames) {
            // Use GenerationContext.sanitize() for names
            flags.add(BEAN_NAME_PREFIX + GenerationContext.sanitize(name));
        }

        return flags;
    }

    /**
     * Genera el código de inicialización del flag para esta condición de bean.
     * 
     * @param context Contexto de generación
     * @param beanClassName Nombre completo de la clase del bean
     * @param exists true si el bean existe, false si no
     * @return Código Java para inicializar el flag
     */
    public String initializeFlagCode(GenerationContext context, String beanClassName, boolean exists) {
        String flag = context.getFlagForBeanType(beanClassName);
        return flag + " = " + exists + ";";
    }

    /**
     * Verifica si esta condición involucra un bean específico.
     */
    public boolean involvesBean(String beanClassName) {
        return beanTypes.contains(beanClassName);
    }

    /**
     * Verifica si esta condición involucra un bean por nombre.
     */
    public boolean involvesBeanName(String beanName) {
        return beanNames.contains(beanName);
    }

    /**
     * Sanitiza un nombre de clase para usarlo como nombre de flag.
     * Uses same logic as GenerationContext.getExistenceFlagName() and GenerationContext.sanitize().
     */
    private String sanitize(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c == '.' ? '_' : c);
            }
        }
        return result.toString().toUpperCase();
    }
}
