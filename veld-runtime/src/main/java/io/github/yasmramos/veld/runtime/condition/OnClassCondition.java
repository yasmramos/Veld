package io.github.yasmramos.veld.runtime.condition;

import io.github.yasmramos.veld.runtime.condition.annotation.ConditionalOnClass;
import io.github.yasmramos.veld.runtime.condition.annotation.ConditionalOnMissingClass;

import java.util.ArrayList;
import java.util.List;

public class OnClassCondition implements Condition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context) {
        ClassLoader classLoader = context.getClassLoader();

        ConditionalOnClass onClass = context.getAnnotation(ConditionalOnClass.class);
        if (onClass != null) {
            List<String> required = collectClassNames(onClass.value(), onClass.name());
            List<String> missing = new ArrayList<>();
            for (String className : required) {
                if (!isPresent(className, classLoader)) {
                    missing.add(className);
                }
            }
            if (!missing.isEmpty()) {
                return ConditionOutcome.noMatch(formatMissing(missing));
            }
        }

        ConditionalOnMissingClass onMissingClass = context.getAnnotation(ConditionalOnMissingClass.class);
        if (onMissingClass != null) {
            List<String> forbidden = collectClassNames(new Class<?>[0], onMissingClass.value());
            List<String> present = new ArrayList<>();
            for (String className : forbidden) {
                if (isPresent(className, classLoader)) {
                    present.add(className);
                }
            }
            if (!present.isEmpty()) {
                return ConditionOutcome.noMatch(formatPresent(present));
            }
        }

        return ConditionOutcome.match();
    }

    private List<String> collectClassNames(Class<?>[] classes, String[] names) {
        List<String> result = new ArrayList<>();
        if (classes != null) {
            for (Class<?> c : classes) {
                result.add(c.getName());
            }
        }
        if (names != null) {
            for (String n : names) {
                if (n != null && !n.isEmpty()) {
                    result.add(n);
                }
            }
        }
        return result;
    }

    private boolean isPresent(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    private String formatMissing(List<String> missing) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append("Required class '").append(missing.get(i))
                    .append("' not found on classpath (add the corresponding dependency)");
        }
        return sb.toString();
    }

    private String formatPresent(List<String> present) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < present.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append("Class '").append(present.get(i))
                    .append("' was found on classpath but expected to be absent");
        }
        return sb.toString();
    }
}
