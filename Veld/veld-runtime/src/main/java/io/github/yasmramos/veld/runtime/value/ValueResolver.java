package io.github.yasmramos.veld.runtime.value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves configuration values from multiple sources.
 * 
 * <p>Resolution order (first match wins):
 * <ol>
 *   <li>System properties (-Dproperty=value)</li>
 *   <li>Environment variables</li>
 *   <li>Configuration files (application.properties)</li>
 *   <li>Default value (if specified)</li>
 * </ol>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public class ValueResolver {
    
    /** Pattern to match ${property} or ${property:default} */
    private static final Pattern PLACEHOLDER_PATTERN = 
        Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");
    
    /** Singleton instance */
    private static volatile ValueResolver instance;
    
    /** Loaded properties from configuration files */
    private final Map<String, String> properties;
    
    /** Configuration file names to search for */
    private static final String[] CONFIG_FILES = {
        "application.properties",
        "config/application.properties",
        "veld.properties",
        "config/veld.properties"
    };
    
    /**
     * Gets the singleton instance of the ValueResolver.
     * 
     * @return the ValueResolver instance
     */
    public static ValueResolver getInstance() {
        if (instance == null) {
            synchronized (ValueResolver.class) {
                if (instance == null) {
                    instance = new ValueResolver();
                }
            }
        }
        return instance;
    }
    
    /**
     * Creates a new ValueResolver and loads configuration files.
     */
    private ValueResolver() {
        this.properties = new HashMap<>();
        loadConfigurationFiles();
    }
    
    /**
     * Loads configuration from various property files.
     */
    private void loadConfigurationFiles() {
        // Try to load from classpath first
        for (String configFile : CONFIG_FILES) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    props.forEach((key, value) -> properties.put(key.toString(), value.toString()));
                    System.out.println("[Veld] Loaded configuration from classpath: " + configFile);
                }
            } catch (IOException e) {
                // Ignore, file not found or not readable
            }
        }
        
        // Try to load from filesystem
        for (String configFile : CONFIG_FILES) {
            Path path = Paths.get(configFile);
            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    Properties props = new Properties();
                    props.load(is);
                    props.forEach((key, value) -> properties.put(key.toString(), value.toString()));
                    System.out.println("[Veld] Loaded configuration from file: " + path.toAbsolutePath());
                } catch (IOException e) {
                    // Ignore, file not readable
                }
            }
        }
    }
    
    /**
     * Resolves a value expression.
     * 
     * @param expression the value expression (e.g., "${app.name}", "${app.name:default}", or literal)
     * @return the resolved value
     * @throws ValueResolutionException if the value cannot be resolved and no default is provided
     */
    public String resolve(String expression) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }
        
        // Check if it's a placeholder expression
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
        if (matcher.matches()) {
            String propertyName = matcher.group(1);
            String defaultValue = matcher.group(2);
            
            String value = resolveProperty(propertyName);
            
            if (value != null) {
                return value;
            }
            
            if (defaultValue != null) {
                return defaultValue;
            }
            
            throw new ValueResolutionException(
                "Cannot resolve value for property: " + propertyName + 
                ". No value found and no default specified.");
        }
        
        // Check for embedded placeholders in literal strings
        StringBuffer result = new StringBuffer();
        Matcher embeddedMatcher = PLACEHOLDER_PATTERN.matcher(expression);
        boolean found = false;
        
        while (embeddedMatcher.find()) {
            found = true;
            String propertyName = embeddedMatcher.group(1);
            String defaultValue = embeddedMatcher.group(2);
            
            String value = resolveProperty(propertyName);
            if (value == null) {
                value = defaultValue;
            }
            if (value == null) {
                throw new ValueResolutionException(
                    "Cannot resolve embedded property: " + propertyName);
            }
            
            embeddedMatcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        
        if (found) {
            embeddedMatcher.appendTail(result);
            return result.toString();
        }
        
        // Return as literal value
        return expression;
    }
    
    /**
     * Resolves a property name from various sources.
     * 
     * @param propertyName the property name to resolve
     * @return the resolved value or null if not found
     */
    private String resolveProperty(String propertyName) {
        // 1. Check system properties first
        String value = System.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        
        // 2. Check environment variables (with various naming conventions)
        value = System.getenv(propertyName);
        if (value != null) {
            return value;
        }
        
        // Try uppercase with underscores (e.g., app.name -> APP_NAME)
        String envName = propertyName.toUpperCase().replace('.', '_').replace('-', '_');
        value = System.getenv(envName);
        if (value != null) {
            return value;
        }
        
        // 3. Check loaded properties
        value = properties.get(propertyName);
        if (value != null) {
            return value;
        }
        
        return null;
    }
    
    /**
     * Resolves a value and converts it to the specified type.
     * 
     * @param expression the value expression
     * @param targetType the target type class
     * @param <T> the target type
     * @return the resolved and converted value
     * @throws ValueResolutionException if resolution or conversion fails
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(String expression, Class<T> targetType) {
        String stringValue = resolve(expression);
        
        if (stringValue == null) {
            return null;
        }
        
        try {
            return (T) convertValue(stringValue, targetType);
        } catch (Exception e) {
            throw new ValueResolutionException(
                "Cannot convert value '" + stringValue + "' to type " + targetType.getName(), e);
        }
    }
    
    /**
     * Converts a string value to the specified type.
     * 
     * @param value the string value
     * @param targetType the target type
     * @return the converted value
     */
    private Object convertValue(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        }
        
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        }
        
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        }
        
        if (targetType == char.class || targetType == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException(
                    "Cannot convert '" + value + "' to char: expected single character");
            }
            return value.charAt(0);
        }
        
        throw new IllegalArgumentException(
            "Unsupported target type: " + targetType.getName());
    }
    
    /**
     * Sets a property value programmatically.
     * 
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    /**
     * Gets all loaded properties.
     * 
     * @return a map of all properties
     */
    public Map<String, String> getAllProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * Clears all loaded properties and reloads from configuration files.
     */
    public void reload() {
        properties.clear();
        loadConfigurationFiles();
    }
    
    /**
     * Resets the singleton instance (useful for testing).
     */
    public static void reset() {
        instance = null;
    }
}
