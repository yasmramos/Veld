package io.github.yasmramos.veld.runtime.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.ArrayList;
import java.util.List;

public class OnPropertyCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName()));
        if (attributes == null) {
            return ConditionOutcome.noMatch("@ConditionalOnProperty annotation not found");
        }

        String prefix = normalizePrefix(attributes.getString("prefix"));
        String havingValue = attributes.getString("havingValue");
        boolean matchIfMissing = attributes.getBoolean("matchIfMissing");
        String[] names = resolveNames(attributes);

        Environment environment = context.getEnvironment();
        List<String> mismatches = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String name : names) {
            String key = prefix + name;
            String actual = environment.getProperty(key);
            if (actual == null) {
                if (!matchIfMissing) {
                    missing.add("Property " + key + ": property is missing (matchIfMissing=" + matchIfMissing + ")");
                }
            } else if (havingValue != null && !havingValue.isEmpty()) {
                if (!havingValue.equals(actual)) {
                    mismatches.add("Property " + key + ": expected \"" + havingValue + "\" but found \"" + actual + "\"");
                }
            } else if ("false".equalsIgnoreCase(actual)) {
                mismatches.add("Property " + key + ": expected \"true\" but found \"" + actual + "\"");
            }
        }

        if (mismatches.isEmpty() && missing.isEmpty()) {
            return ConditionOutcome.match();
        }

        List<String> reasons = new ArrayList<>(mismatches.size() + missing.size());
        reasons.addAll(mismatches);
        reasons.addAll(missing);
        return ConditionOutcome.noMatch(String.join("; ", reasons));
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    private String[] resolveNames(AnnotationAttributes attributes) {
        String[] value = attributes.getStringArray("value");
        String[] name = attributes.getStringArray("name");
        if (value.length > 0 && name.length > 0) {
            throw new IllegalStateException(
                    "@ConditionalOnProperty must specify either 'value' or 'name', not both");
        }
        if (value.length == 0 && name.length == 0) {
            throw new IllegalStateException(
                    "@ConditionalOnProperty must specify at least one property via 'value' or 'name'");
        }
        return value.length > 0 ? value : name;
    }
}
