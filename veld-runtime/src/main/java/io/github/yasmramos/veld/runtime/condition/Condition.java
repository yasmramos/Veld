package io.github.yasmramos.veld.runtime.condition;

public interface Condition {

    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);

    default ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean match = matches(context, metadata);
        String message = getClass().getSimpleName() + (match ? " matched" : " did not match");
        return match ? ConditionOutcome.match(message) : ConditionOutcome.noMatch(message);
    }
}
