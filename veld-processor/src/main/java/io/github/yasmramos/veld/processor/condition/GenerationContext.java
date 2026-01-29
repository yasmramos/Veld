package io.github.yasmramos.veld.processor.condition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contexto de generación para la evaluación y traducción de condiciones a código Java.
 * 
 * <p>Mantiene el estado de los flags de existencia y proporciona métodos utilitarios
 * para la generación de código determinístico.</p>
 */
public final class GenerationContext {

    private final Map<String, Boolean> flagValues;
    private final Set<String> presentBeans;
    private final Set<String> absentBeans;
    private final Map<String, String> beanTypeToFlag;
    private final Map<String, String> beanNameToFlag;

    private GenerationContext(Builder builder) {
        this.flagValues = Map.copyOf(builder.flagValues);
        this.presentBeans = Set.copyOf(builder.presentBeans);
        this.absentBeans = Set.copyOf(builder.absentBeans);
        this.beanTypeToFlag = Map.copyOf(builder.beanTypeToFlag);
        this.beanNameToFlag = Map.copyOf(builder.beanNameToFlag);
    }

    /**
     * Verifica si un flag está disponible.
     */
    public boolean hasFlag(String flagName) {
        return flagValues.containsKey(flagName);
    }

    /**
     * Obtiene el valor de un flag.
     */
    public boolean getFlagValue(String flagName) {
        Boolean value = flagValues.get(flagName);
        if (value == null) {
            throw new IllegalArgumentException("Flag no encontrado: " + flagName);
        }
        return value;
    }

    /**
     * Obtiene el flag para un tipo de bean, incluyendo el prefijo HAS_BEAN_.
     */
    public String getFlagForBeanType(String beanType) {
        String existing = beanTypeToFlag.get(beanType);
        if (existing != null) {
            return existing;
        }
        // Generar flag con prefijo
        StringBuilder result = new StringBuilder("HAS_BEAN_");
        for (int i = 0; i < beanType.length(); i++) {
            char c = beanType.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            } else if (c == '.') {
                result.append('_');
            }
        }
        return result.toString();
    }

    /**
     * Obtiene el flag para un nombre de bean, incluyendo el prefijo HAS_BEAN_.
     */
    public String getFlagForBeanName(String beanName) {
        String existing = beanNameToFlag.get(beanName);
        if (existing != null) {
            return existing;
        }
        // Generar flag con prefijo
        StringBuilder result = new StringBuilder("HAS_BEAN_");
        for (int i = 0; i < beanName.length(); i++) {
            char c = beanName.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            } else if (c == '.') {
                result.append('_');
            }
        }
        return result.toString();
    }

    /**
     * Obtiene el flag para cualquier bean (tipo o nombre).
     */
    public String getFlagForBean(String beanIdentifier) {
        if (beanIdentifier.startsWith("bean:")) {
            return getFlagForBeanName(beanIdentifier.substring(5));
        }
        return getFlagForBeanType(beanIdentifier);
    }

    /**
     * Verifica si un bean está presente.
     */
    public boolean isBeanPresent(String beanType) {
        return presentBeans.contains(beanType);
    }

    /**
     * Verifica si un bean está ausente.
     */
    public boolean isBeanAbsent(String beanType) {
        return absentBeans.contains(beanType);
    }

    /**
     * Obtiene todos los flags registrados.
     */
    public Set<String> getAllFlags() {
        return flagValues.keySet();
    }

    /**
     * Obtiene el valor de un flag o el valor por defecto si no existe.
     */
    public boolean getFlagOrDefault(String flagName, boolean defaultValue) {
        Boolean value = flagValues.get(flagName);
        return value != null ? value : defaultValue;
    }

    /**
     * Sanitiza un identificador para usarlo como nombre de flag.
     */
    public static String sanitize(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "UNKNOWN";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            } else if (c == '.') {
                result.append('_');
            }
        }
        return result.toString().toUpperCase();
    }

    /**
     * Genera el nombre del flag de existencia para un bean.
     * El nombre incluye el prefijo "HAS_BEAN_" y usa solo el nombre simple de la clase.
     */
    public static String getExistenceFlagName(String beanClassName) {
        // Extraer solo el nombre simple de la clase (sin el paquete)
        String simpleName = beanClassName.substring(beanClassName.lastIndexOf('.') + 1);
        return "HAS_BEAN_" + sanitize(simpleName);
    }

    /**
     * Crea un contexto de generación desde el resultado de resolución del grafo.
     */
    public static GenerationContext fromResolutionResult(BeanExistenceGraph.ResolutionResult result) {
        Builder builder = builder();

        // Añadir flags para beans presentes
        for (String bean : result.presentBeans()) {
            String flagName = getExistenceFlagName(bean);
            builder.addFlag(flagName, true);
            builder.addPresentBean(bean);
            builder.addBeanTypeFlag(bean, flagName);
        }

        // Añadir flags para beans ausentes
        for (String bean : result.absentBeans()) {
            String flagName = getExistenceFlagName(bean);
            builder.addFlag(flagName, false);
            builder.addAbsentBean(bean);
            builder.addBeanTypeFlag(bean, flagName);
        }

        return builder.build();
    }

    /**
     * Crea un nuevo builder para esta clase.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Boolean> flagValues = new LinkedHashMap<>();
        private final Set<String> presentBeans = new java.util.LinkedHashSet<>();
        private final Set<String> absentBeans = new java.util.LinkedHashSet<>();
        private final Map<String, String> beanTypeToFlag = new LinkedHashMap<>();
        private final Map<String, String> beanNameToFlag = new LinkedHashMap<>();

        private Builder() {}

        public Builder addFlag(String flagName, boolean value) {
            this.flagValues.put(flagName, value);
            return this;
        }

        public Builder addFlags(Map<String, Boolean> flags) {
            this.flagValues.putAll(flags);
            return this;
        }

        public Builder addPresentBean(String beanType) {
            this.presentBeans.add(beanType);
            return this;
        }

        public Builder addAbsentBean(String beanType) {
            this.absentBeans.add(beanType);
            return this;
        }

        public Builder addBeanTypeFlag(String beanType, String flagName) {
            this.beanTypeToFlag.put(beanType, flagName);
            return this;
        }

        public Builder addBeanNameFlag(String beanName, String flagName) {
            this.beanNameToFlag.put(beanName, flagName);
            return this;
        }

        public Builder addPresentBeans(Set<String> beans) {
            this.presentBeans.addAll(beans);
            return this;
        }

        public Builder addAbsentBeans(Set<String> beans) {
            this.absentBeans.addAll(beans);
            return this;
        }

        public GenerationContext build() {
            return new GenerationContext(this);
        }
    }
}
