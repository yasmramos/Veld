package io.github.yasmramos.veld.test.context;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.test.mock.MockFactory;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contenedor de pruebas ligero que gestiona el ciclo de vida
 * de beans Veld para propósitos de testing.
 * 
 * <p>Esta clase proporciona un wrapper alrededor del contenedor Veld
 * con funcionalidades adicionales específicas para pruebas:</p>
 * <ul>
 *   <li>Registro de mocks antes del inicio del contenedor</li>
 *   <li>Gestión del ciclo de vida (inicio/parada)</li>
 *   <li>Inyección de beans en campos de prueba</li>
 *   <li>Acceso a beans registrados y mocks</li>
 * </ul>
 * 
 * <h2>Uso Básico</h2>
 * <pre>{@code
 * TestContext context = TestContextBuilder.create()
 *     .withProfile("test")
 *     .withMock("myMock", mockObject)
 *     .build();
 * 
 * try {
 *     MyService service = context.getBean(MyService.class);
 *     // Ejecutar pruebas...
 * } finally {
 *     context.close();
 * }
 * }</pre>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public final class TestContext implements AutoCloseable {
    
    private final Map<Class<?>, Object> mocks;
    private final Map<String, Object> namedMocks;
    private final String activeProfile;
    private boolean closed = false;
    
    private TestContext(Map<Class<?>, Object> mocks, Map<String, Object> namedMocks, String profile) {
        this.mocks = mocks;
        this.namedMocks = namedMocks;
        this.activeProfile = profile;
    }
    
    /**
     * Obtiene un bean del contenedor por tipo.
     * 
     * @param <T> tipo del bean
     * @param type clase del bean
     * @return bean del tipo especificado
     * @throws IllegalStateException si el contexto está cerrado
     * @throws RuntimeException si el bean no existe
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        checkOpen();
        // Check if we have a mock for this type
        if (mocks.containsKey(type)) {
            return (T) mocks.get(type);
        }
        // Check if we have a named mock that can be cast
        for (Object mock : namedMocks.values()) {
            if (type.isInstance(mock)) {
                return (T) mock;
            }
        }
        // Try to get from Veld container
        try {
            return Veld.get(type);
        } catch (Exception e) {
            throw new TestContextException(
                "No se pudo obtener bean de tipo: " + type.getName(), e);
        }
    }
    
    /**
     * Obtiene un bean de forma opcional.
     * 
     * @param <T> tipo del bean
     * @param type clase del bean
     * @return Optional con el bean o vacío si no existe
     */
    public <T> Optional<T> findBean(Class<T> type) {
        checkOpen();
        try {
            T bean = Veld.get(type);
            return Optional.ofNullable(bean);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Resetea todos los mocks registrados.
     * 
     * <p>Este método es útil para limpiar el estado de los mocks
     * entre pruebas sin reiniciar todo el contenedor.</p>
     */
    public void resetMocks() {
        mocks.values().forEach(Mockito::reset);
        namedMocks.values().forEach(Mockito::reset);
    }
    
    /**
     * Inyecta beans en los campos anotados de un objeto.
     * 
     * @param instance objeto a inyectar
     * @param <T> tipo del objeto
     * @return el objeto con los campos inyectados
     */
    public <T> T injectFields(T instance) {
        checkOpen();
        
        Class<?> clazz = instance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                injectField(instance, field);
            }
            clazz = clazz.getSuperclass();
        }
        
        return instance;
    }
    
    private <T> void injectField(T instance, Field field) {
        field.setAccessible(true);
        
        Class<?> fieldType = field.getType();
        
        // Verificar si existe un mock para este tipo
        if (mocks.containsKey(fieldType)) {
            try {
                field.set(instance, mocks.get(fieldType));
                return;
            } catch (IllegalAccessException e) {
                throw new TestContextException(
                    "No se pudo inyectar mock en campo: " + field.getName(), e);
            }
        }
        
        // Verificar si existe un mock con nombre que coincida con el campo
        String fieldName = field.getName();
        if (namedMocks.containsKey(fieldName)) {
            try {
                Object mock = namedMocks.get(fieldName);
                if (fieldType.isInstance(mock)) {
                    field.set(instance, mock);
                    return;
                }
            } catch (IllegalAccessException e) {
                throw new TestContextException(
                    "No se pudo inyectar mock en campo: " + field.getName(), e);
            }
        }
        
        // Intentar obtener bean por tipo del contenedor Veld
        try {
            Object bean = Veld.get(fieldType);
            field.set(instance, bean);
        } catch (Exception e) {
            // Campo opcional o no inyectable, ignorar
        }
    }
    
    /**
     * Verifica si el contexto está abierto.
     */
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException(
                "El TestContext está cerrado y no puede ser usado");
        }
    }
    
    /**
     * Cierra el contexto y libera recursos.
     * 
     * <p>Este método limpia todos los mocks registrados.</p>
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            mocks.clear();
            namedMocks.clear();
        }
    }
    
    /**
     * Obtiene el perfil activo del contexto.
     * 
     * @return nombre del perfil activo
     */
    public String getActiveProfile() {
        return activeProfile;
    }
    
    /**
     * Verifica si un mock existe para el tipo especificado.
     * 
     * @param type tipo a verificar
     * @return true si existe un mock para el tipo
     */
    public boolean hasMock(Class<?> type) {
        return mocks.containsKey(type);
    }
    
    /**
     * Obtiene el mock para el tipo especificado.
     * 
     * @param <T> tipo del mock
     * @param type clase del mock
     * @return Optional con el mock o vacío si no existe
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMock(Class<T> type) {
        return Optional.ofNullable((T) mocks.get(type));
    }
    
    /**
     * Builder para crear instancias de TestContext.
     */
    public static final class Builder {
        private final Map<Class<?>, Object> mocks = new HashMap<>();
        private final Map<String, Object> namedMocks = new HashMap<>();
        private final Map<String, String> properties = new HashMap<>();
        private String profile = "test";
        
        private Builder() {}
        
        /**
         * Crea un nuevo builder.
         * 
         * @return builder configurado
         */
        public static Builder create() {
            return new Builder();
        }
        
        /**
         * Establece el perfil de pruebas.
         * 
         * @param profile nombre del perfil
         * @return este builder
         */
        public Builder withProfile(String profile) {
            this.profile = profile;
            return this;
        }
        
        /**
         * Agrega propiedades del sistema.
         * 
         * @param key clave de la propiedad
         * @param value valor de la propiedad
         * @return este builder
         */
        public Builder withProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }
        
        /**
         * Agrega múltiples propiedades.
         * 
         * @param props propiedades en formato "key=value"
         * @return este builder
         */
        public Builder withProperties(String... props) {
            for (String prop : props) {
                String[] parts = prop.split("=", 2);
                if (parts.length == 2) {
                    withProperty(parts[0], parts[1]);
                }
            }
            return this;
        }
        
        /**
         * Registra un mock para un tipo específico.
         * 
         * @param type tipo del mock
         * @param mock instancia del mock
         * @param <T> tipo del mock
         * @return este builder
         */
        public <T> Builder withMock(Class<T> type, T mock) {
            this.mocks.put(type, mock);
            return this;
        }
        
        /**
         * Registra un mock para un tipo específico (acepta wildcards).
         * 
         * @param type tipo del mock
         * @param mock instancia del mock
         * @return este builder
         */
        @SuppressWarnings("unchecked")
        public Builder withMockRaw(Class<?> type, Object mock) {
            this.mocks.put(type, mock);
            return this;
        }
        
        /**
         * Registra un mock con nombre específico.
         * 
         * @param name nombre del mock
         * @param mock instancia del mock
         * @return este builder
         */
        public Builder withMock(String name, Object mock) {
            this.namedMocks.put(name, mock);
            return this;
        }
        
        /**
         * Crea y registra un mock para un tipo específico.
         * 
         * @param type tipo del mock
         * @param <T> tipo del mock
         * @return mock creado
         */
        public <T> T withAutoMock(Class<T> type) {
            T mock = MockFactory.createMock(type);
            this.mocks.put(type, mock);
            return mock;
        }
        
        /**
         * Construye el TestContext.
         * 
         * @return contexto de pruebas configurado
         */
        public TestContext build() {
            // Configurar propiedades del sistema
            properties.forEach(System::setProperty);
            
            // Configurar perfil en Veld
            Veld.setActiveProfiles(profile);
            
            // El builder retorna un contexto que maneja mocks manualmente
            return new TestContext(new HashMap<>(mocks), 
                                  new HashMap<>(namedMocks), profile);
        }
    }
    
    /**
     * Excepción específica para errores del contexto de pruebas.
     */
    public static class TestContextException extends RuntimeException {
        public TestContextException(String message) {
            super(message);
        }
        
        public TestContextException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
