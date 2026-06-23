package io.github.yasmramos.veld;

import java.util.Properties;

/**
 * Proporciona acceso a propiedades del sistema para evaluación de condiciones en tiempo de compilación.
 * 
 * <p>Esta clase es utilizada por el contenedor Veld generado para evaluar
 * condiciones @ConditionalOnProperty durante la inicialización estática.</p>
 * 
 * <p>En el modelo determinístico de Veld, las propiedades se evalúan una vez
 * durante la inicialización del contenedor, no en cada acceso.</p>
 */
public final class VeldProperties {

    private final Properties properties;

    /**
     * Crea un nuevo VeldProperties cargando las propiedades del sistema.
     */
    public VeldProperties() {
        this.properties = new Properties();
        // Cargar propiedades del sistema
        this.properties.putAll(System.getProperties());
    }

    /**
     * Crea un VeldProperties con propiedades pre-cargadas.
     */
    public VeldProperties(Properties properties) {
        this.properties = new Properties(properties);
    }

    /**
     * Verifica si una propiedad existe.
     * 
     * @param name Nombre de la propiedad
     * @return true si la propiedad existe y tiene un valor no vacío
     */
    public boolean hasProperty(String name) {
        String value = properties.getProperty(name);
        return value != null && !value.isEmpty();
    }

    /**
     * Obtiene el valor de una propiedad.
     * 
     * @param name Nombre de la propiedad
     * @param defaultValue Valor por defecto si la propiedad no existe
     * @return El valor de la propiedad o el valor por defecto
     */
    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    /**
     * Verifica si una propiedad tiene un valor específico.
     * 
     * @param name Nombre de la propiedad
     * @param havingValue Valor esperado
     * @return true si la propiedad tiene el valor esperado
     */
    public boolean hasPropertyWithValue(String name, String havingValue) {
        String value = properties.getProperty(name);
        if (value == null || value.isEmpty()) {
            return false;
        }
        // Normalizar valores booleanos
        if ("true".equalsIgnoreCase(havingValue) || "false".equalsIgnoreCase(havingValue)) {
            return Boolean.parseBoolean(value) == Boolean.parseBoolean(havingValue);
        }
        return value.equals(havingValue);
    }

    /**
     * Obtiene todas las propiedades como un Properties copia.
     */
    public Properties asProperties() {
        return new Properties(properties);
    }

    @Override
    public String toString() {
        return "VeldProperties{size=" + properties.size() + "}";
    }
}
