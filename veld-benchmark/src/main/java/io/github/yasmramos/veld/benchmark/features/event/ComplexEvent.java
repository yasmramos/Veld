package io.github.yasmramos.veld.benchmark.features.event;

import io.github.yasmramos.veld.runtime.event.Event;

import java.util.List;

/**
 * Complex event for EventBus benchmarking.
 */
public class ComplexEvent extends Event {
    private final String id;
    private final String type;
    private final int priority;
    private final List<String> tags;

    public ComplexEvent(String id, String type, int priority, List<String> tags) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.tags = tags;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public int getPriority() { return priority; }
    public List<String> getTags() { return tags; }
}
