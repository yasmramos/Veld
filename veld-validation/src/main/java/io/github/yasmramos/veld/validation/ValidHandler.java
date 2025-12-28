package io.github.yasmramos.veld.validation;

import io.github.yasmramos.veld.annotation.*;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class ValidHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        Parameter[] parameters = ctx.getMethod().getParameters();
        Object[] args = ctx.getParameters();
        List<String> violations = new ArrayList<>();
        
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Valid.class) && args[i] != null) {
                validateObject(args[i], violations);
            }
        }
        if (!violations.isEmpty()) throw new ValidationException(violations);
        return ctx.proceed();
    }

    private void validateObject(Object obj, List<String> violations) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                String name = field.getName();
                if (field.isAnnotationPresent(NotNull.class) && value == null)
                    violations.add(name + ": " + field.getAnnotation(NotNull.class).message());
                if (field.isAnnotationPresent(NotEmpty.class)) {
                    if (value == null || (value instanceof String && ((String) value).isEmpty()) ||
                        (value instanceof Collection && ((Collection<?>) value).isEmpty()))
                        violations.add(name + ": " + field.getAnnotation(NotEmpty.class).message());
                }
                if (field.isAnnotationPresent(Size.class) && value != null) {
                    Size s = field.getAnnotation(Size.class);
                    int size = getSize(value);
                    if (size < s.min() || size > s.max()) violations.add(name + ": " + s.message());
                }
                if (field.isAnnotationPresent(Min.class) && value instanceof Number) {
                    Min m = field.getAnnotation(Min.class);
                    if (((Number) value).longValue() < m.value()) violations.add(name + ": " + m.message());
                }
                if (field.isAnnotationPresent(Max.class) && value instanceof Number) {
                    Max m = field.getAnnotation(Max.class);
                    if (((Number) value).longValue() > m.value()) violations.add(name + ": " + m.message());
                }
                if (field.isAnnotationPresent(Email.class) && value instanceof String) {
                    Email e = field.getAnnotation(Email.class);
                    if (!Pattern.matches(e.regexp(), (String) value)) violations.add(name + ": " + e.message());
                }
            } catch (IllegalAccessException ignored) {}
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
