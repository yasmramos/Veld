package io.github.yasmramos.veld.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.Optional;

/**
 * Helper class that provides unified access to DI annotations from multiple specifications:
 * - Veld native annotations (io.github.yasmramos.veld.annotation.*)
 * - JSR-330 (javax.inject.*)
 * - Jakarta Inject (jakarta.inject.*)
 * 
 * This allows Veld to be fully compatible with standard Java DI annotations while
 * also supporting its own native annotations.
 */
public final class AnnotationHelper {
    
    // Veld native annotations
    private static final String VELD_INJECT = "io.github.yasmramos.veld.annotation.Inject";
    private static final String VELD_NAMED = "io.github.yasmramos.veld.annotation.Named";
    private static final String VELD_SINGLETON = "io.github.yasmramos.veld.annotation.Singleton";
    private static final String VELD_PRIMARY = "io.github.yasmramos.veld.annotation.Primary";
    private static final String VELD_QUALIFIER = "io.github.yasmramos.veld.annotation.Qualifier";
    private static final String VELD_FACTORY = "io.github.yasmramos.veld.annotation.Factory";
    private static final String VELD_BEAN = "io.github.yasmramos.veld.annotation.Bean";
    
    // JSR-330 annotations (javax.inject)
    private static final String JAVAX_INJECT = "javax.inject.Inject";
    private static final String JAVAX_NAMED = "javax.inject.Named";
    private static final String JAVAX_SINGLETON = "javax.inject.Singleton";
    private static final String JAVAX_QUALIFIER = "javax.inject.Qualifier";
    private static final String JAVAX_PROVIDER = "javax.inject.Provider";
    
    // Jakarta Inject annotations (jakarta.inject)
    private static final String JAKARTA_INJECT = "jakarta.inject.Inject";
    private static final String JAKARTA_NAMED = "jakarta.inject.Named";
    private static final String JAKARTA_SINGLETON = "jakarta.inject.Singleton";
    private static final String JAKARTA_QUALIFIER = "jakarta.inject.Qualifier";
    private static final String JAKARTA_PROVIDER = "jakarta.inject.Provider";
    
    private AnnotationHelper() {
        // Utility class
    }
    
    /**
     * Checks if an element has any @Inject annotation (Veld, JSR-330, or Jakarta).
     */
    public static boolean hasInjectAnnotation(Element element) {
        return hasAnnotation(element, VELD_INJECT, JAVAX_INJECT, JAKARTA_INJECT);
    }
    
    /**
     * Checks if an element has any @Singleton annotation (Veld, JSR-330, or Jakarta).
     */
    public static boolean hasSingletonAnnotation(Element element) {
        return hasAnnotation(element, VELD_SINGLETON, JAVAX_SINGLETON, JAKARTA_SINGLETON);
    }
    
    /**
     * Checks if an element has @Primary annotation (Veld native).
     */
    public static boolean hasPrimaryAnnotation(Element element) {
        return hasAnnotation(element, VELD_PRIMARY);
    }
    
    /**
     * Checks if an element has @Qualifier annotation (Veld native).
     */
    public static boolean hasQualifierAnnotation(Element element) {
        return hasAnnotation(element, VELD_QUALIFIER);
    }
    
    /**
     * Gets the value from @Named annotation if present (Veld, JSR-330, or Jakarta).
     * Returns Optional.empty() if no @Named annotation is present.
     */
    public static Optional<String> getNamedValue(Element element) {
        // Check Veld @Named
        Optional<String> value = getAnnotationValue(element, VELD_NAMED, "value");
        if (value.isPresent()) return value;
        
        // Check JSR-330 @Named
        value = getAnnotationValue(element, JAVAX_NAMED, "value");
        if (value.isPresent()) return value;
        
        // Check Jakarta @Named
        return getAnnotationValue(element, JAKARTA_NAMED, "value");
    }
    
    /**
     * Determines which specification the @Inject annotation comes from.
     */
    public static InjectSource getInjectSource(Element element) {
        if (hasAnnotation(element, VELD_INJECT)) {
            return InjectSource.VELD;
        } else if (hasAnnotation(element, JAVAX_INJECT)) {
            return InjectSource.JAVAX;
        } else if (hasAnnotation(element, JAKARTA_INJECT)) {
            return InjectSource.JAKARTA;
        }
        return InjectSource.NONE;
    }
    
    /**
     * Gets the qualifier value from @Qualifier or @Named annotation.
     */
    public static Optional<String> getQualifierValue(Element element) {
        // First check @Qualifier annotation (Veld native)
        Optional<String> qualifierValue = getAnnotationValue(element, VELD_QUALIFIER, "value");
        if (qualifierValue.isPresent() && !qualifierValue.get().isEmpty()) {
            return qualifierValue;
        }
        
        // Then try to get @Named value
        Optional<String> named = getNamedValue(element);
        if (named.isPresent()) {
            return named;
        }
        
        // Check for custom qualifiers (meta-annotated with @Qualifier)
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            String annotationType = annotationMirror.getAnnotationType().toString();
            
            // Skip standard annotations
            if (annotationType.startsWith("java.") || 
                annotationType.startsWith("javax.lang.") ||
                annotationType.startsWith("io.github.yasmramos.veld.annotation.") ||
                annotationType.equals(JAVAX_INJECT) ||
                annotationType.equals(JAKARTA_INJECT)) {
                continue;
            }
            
            // Check if this annotation is meta-annotated with @Qualifier
            Element annotationElement = annotationMirror.getAnnotationType().asElement();
            if (hasAnnotation(annotationElement, JAVAX_QUALIFIER, JAKARTA_QUALIFIER)) {
                // Use the annotation's simple name as qualifier
                return Optional.of(getSimpleName(annotationType));
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Checks if a type is a Provider type (JSR-330 or Jakarta).
     */
    public static boolean isProviderType(String typeName) {
        return JAVAX_PROVIDER.equals(typeName) || JAKARTA_PROVIDER.equals(typeName);
    }
    
    /**
     * Checks if an element has any of the specified annotations.
     */
    private static boolean hasAnnotation(Element element, String... annotationNames) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationType = mirror.getAnnotationType().toString();
            for (String name : annotationNames) {
                if (name.equals(annotationType)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Gets a String value from an annotation attribute.
     */
    private static Optional<String> getAnnotationValue(Element element, String annotationName, String attributeName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (annotationName.equals(mirror.getAnnotationType().toString())) {
                return mirror.getElementValues().entrySet().stream()
                    .filter(e -> attributeName.equals(e.getKey().getSimpleName().toString()))
                    .map(e -> {
                        Object value = e.getValue().getValue();
                        return value != null ? value.toString() : null;
                    })
                    .filter(v -> v != null && !v.isEmpty())
                    .findFirst();
            }
        }
        return Optional.empty();
    }
    
    /**
     * Gets the simple name from a fully qualified class name.
     */
    private static String getSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
    
    /**
     * Enum representing the source of @Inject annotation.
     */
    public enum InjectSource {
        VELD("io.github.yasmramos.veld.annotation"),
        JAVAX("javax.inject"),
        JAKARTA("jakarta.inject"),
        NONE(null);
        
        private final String packageName;
        
        InjectSource(String packageName) {
            this.packageName = packageName;
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public boolean isStandard() {
            return this == JAVAX || this == JAKARTA;
        }
    }
}
