package io.github.yasmramos.veld.validation;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates method parameters annotated with @Valid.
 */
public class ValidHandler implements AspectHandler {

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Valid.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        Parameter[] parameters = invocation.getMethod().getParameters();
        Object[] args = invocation.getArguments();
        List<String> violations = new ArrayList<>();
        
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Valid.class) && args[i] != null) {
                validateObject(args[i], violations);
            }
        }
        
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        
        return invocation.proceed();
    }

    private void validateObject(Object obj, List<String> violations) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                String fieldName = field.getName();
                
                if (field.isAnnotationPresent(NotNull.class) && value == null) {
                    NotNull ann = field.getAnnotation(NotNull.class);
                    violations.add(fieldName + ": " + ann.message());
                }
                
                if (field.isAnnotationPresent(NotEmpty.class)) {
                    NotEmpty ann = field.getAnnotation(NotEmpty.class);
                    if (value == null || (value instanceof String && ((String) value).isEmpty()) ||
                        (value instanceof Collection && ((Collection<?>) value).isEmpty())) {
                        violations.add(fieldName + ": " + ann.message());
                    }
                }
                
                if (field.isAnnotationPresent(Size.class) && value != null) {
                    Size ann = field.getAnnotation(Size.class);
                    int size = getSize(value);
                    if (size < ann.min() || size > ann.max()) {
                        violations.add(fieldName + ": " + ann.message());
                    }
                }
                
                if (field.isAnnotationPresent(Min.class) && value instanceof Number) {
                    Min ann = field.getAnnotation(Min.class);
                    if (((Number) value).longValue() < ann.value()) {
                        violations.add(fieldName + ": " + ann.message());
                    }
                }
                
                if (field.isAnnotationPresent(Max.class) && value instanceof Number) {
                    Max ann = field.getAnnotation(Max.class);
                    if (((Number) value).longValue() > ann.value()) {
                        violations.add(fieldName + ": " + ann.message());
                    }
                }
                
                if (field.isAnnotationPresent(Email.class) && value instanceof String) {
                    Email ann = field.getAnnotation(Email.class);
                    if (!Pattern.matches(ann.regexp(), (String) value)) {
                        violations.add(fieldName + ": " + ann.message());
                    }
                }
                
                if (field.isAnnotationPresent(io.github.yasmramos.veld.annotation.Pattern.class) && value instanceof String) {
                    io.github.yasmramos.veld.annotation.Pattern ann = field.getAnnotation(io.github.yasmramos.veld.annotation.Pattern.class);
                    if (!Pattern.matches(ann.regexp(), (String) value)) {
                        violations.add(fieldName + ": " + ann.message());
                    }
                }
            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }
    }

    private int getSize(Object value) {
        if (value instanceof String) return ((String) value).length();
        if (value instanceof Collection) return ((Collection<?>) value).size();
        if (value.getClass().isArray()) return java.lang.reflect.Array.getLength(value);
        return 0;
    }

    public static class ValidationException extends RuntimeException {
        private final List<String> violations;
        
        public ValidationException(List<String> violations) {
            super("Validation failed: " + String.join(", ", violations));
            this.violations = violations;
        }
        
        public List<String> getViolations() { return violations; }
    }
}
