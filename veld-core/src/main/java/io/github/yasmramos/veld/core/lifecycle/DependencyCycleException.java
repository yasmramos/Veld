package io.github.yasmramos.veld.core.lifecycle;

import io.github.yasmramos.veld.core.exception.BeanCreationException;

import java.util.List;

public class DependencyCycleException extends BeanCreationException {

    private final List<String> cyclePath;

    public DependencyCycleException(List<String> cyclePath) {
        super(buildMessage(cyclePath));
        this.cyclePath = List.copyOf(cyclePath);
    }

    public DependencyCycleException(List<String> cyclePath, Throwable cause) {
        super(buildMessage(cyclePath), cause);
        this.cyclePath = List.copyOf(cyclePath);
    }

    public List<String> getCyclePath() {
        return cyclePath;
    }

    private static String buildMessage(List<String> cyclePath) {
        return "Dependency cycle detected: " + String.join(" -> ", cyclePath)
                + ". Hint: mark one participant with @Lazy or refactor to break the cycle.";
    }
}
