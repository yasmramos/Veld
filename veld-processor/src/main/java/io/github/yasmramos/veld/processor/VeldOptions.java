package io.github.yasmramos.veld.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Map;

/**
 * Veld Processor Options Manager
 * 
 * Handles compilation options for the Veld annotation processor.
 * 
 * Supported options:
 * - veld.profile: Profile for compile-time class generation (dev, test, prod)
 * - veld.strict: Enable strict mode - warnings become errors
 * - veld.extensions.disabled: Disable SPI extensions
 * - veld.debug: Enable debug logging
 * 
 * Usage:
 * -Aveld.profile=dev
 * -Aveld.strict=true
 * -Aveld.extensions.disabled
 * -Aveld.debug=true
 */
public final class VeldOptions {
    
    private final ProcessingEnvironment processingEnv;
    private final Map<String, String> options;
    
    // Option values
    private final String profile;
    private final boolean strictMode;
    private final boolean extensionsEnabled;
    private final boolean debugEnabled;
    
    private VeldOptions(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.options = processingEnv.getOptions();
        
        // Read profile option (default: prod)
        this.profile = options.getOrDefault("veld.profile", "prod").toLowerCase();
        
        // Read strict mode option (default: false)
        this.strictMode = isOptionEnabled("veld.strict");
        
        // Read extensions option (default: enabled)
        this.extensionsEnabled = !options.containsKey("veld.extensions.disabled");
        
        // Read debug option (default: false)
        this.debugEnabled = isOptionEnabled("veld.debug");
    }
    
    /**
     * Creates a new VeldOptions instance from the processing environment.
     */
    public static VeldOptions create(ProcessingEnvironment processingEnv) {
        return new VeldOptions(processingEnv);
    }
    
    /**
     * Checks if a boolean option is enabled.
     * Accepts: "true", "1", "yes" (case-insensitive)
     */
    private boolean isOptionEnabled(String optionName) {
        String value = options.get(optionName);
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(value) || 
               "1".equals(value) || 
               "yes".equalsIgnoreCase(value);
    }
    
    /**
     * Gets the profile for compile-time class generation.
     * Default: "prod"
     */
    public String getProfile() {
        return profile;
    }
    
    /**
     * Returns true if strict mode is enabled.
     * In strict mode, warnings are treated as errors.
     */
    public boolean isStrictMode() {
        return strictMode;
    }
    
    /**
     * Returns true if SPI extensions are enabled.
     */
    public boolean areExtensionsEnabled() {
        return extensionsEnabled;
    }
    
    /**
     * Returns true if debug mode is enabled.
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * Reports an issue to the compiler messager.
     * In strict mode, warnings are upgraded to errors.
     * 
     * @param element The element related to the issue (can be null)
     * @param kind The diagnostic kind (ERROR or WARNING)
     * @param message The message to display
     */
    public void reportIssue(Element element, Diagnostic.Kind kind, String message) {
        Diagnostic.Kind effectiveKind = kind;
        
        // In strict mode, upgrade WARNING to ERROR
        if (strictMode && kind == Diagnostic.Kind.WARNING) {
            effectiveKind = Diagnostic.Kind.ERROR;
        }
        
        String prefix = effectiveKind == Diagnostic.Kind.ERROR ? "[Veld ERROR]" : "[Veld]";
        if (strictMode && kind == Diagnostic.Kind.WARNING) {
            prefix = "[Veld STRICT]";
        }
        
        processingEnv.getMessager().printMessage(
            effectiveKind, 
            prefix + " " + message, 
            element
        );
    }
    
    /**
     * Reports an error.
     */
    public void error(Element element, String message) {
        reportIssue(element, Diagnostic.Kind.ERROR, message);
    }
    
    /**
     * Reports a warning. In strict mode, this becomes an error.
     */
    public void warning(Element element, String message) {
        reportIssue(element, Diagnostic.Kind.WARNING, message);
    }
    
    /**
     * Reports a note/informational message.
     */
    public void note(String message) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE, 
            "[Veld] " + message
        );
    }
    
    /**
     * Logs a debug message (only shown in debug mode).
     */
    public void debug(String message) {
        if (debugEnabled) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE, 
                "[Veld DEBUG] " + message
            );
        }
    }
    
    /**
     * Returns a summary of the current options for logging purposes.
     */
    public String getSummary() {
        return String.format(
            "Veld Options: profile=%s, strict=%s, extensions=%s, debug=%s",
            profile, strictMode, extensionsEnabled, debugEnabled
        );
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}
