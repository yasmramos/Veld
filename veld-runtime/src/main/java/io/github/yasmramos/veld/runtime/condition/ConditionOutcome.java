package io.github.yasmramos.veld.runtime.condition;

import java.util.Objects;

public final class ConditionOutcome {

    private final boolean match;
    private final String message;

    private ConditionOutcome(boolean match, String message) {
        this.match = match;
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    public static ConditionOutcome match() {
        return new ConditionOutcome(true, "");
    }

    public static ConditionOutcome match(String reason) {
        return new ConditionOutcome(true, reason);
    }

    public static ConditionOutcome noMatch(String reason) {
        return new ConditionOutcome(false, reason);
    }

    public boolean isMatch() {
        return match;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConditionOutcome)) {
            return false;
        }
        ConditionOutcome other = (ConditionOutcome) obj;
        return match == other.match && message.equals(other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, message);
    }

    @Override
    public String toString() {
        return message;
    }
}
