package io.github.yasmramos.veld.benchmark.features.event;

import io.github.yasmramos.veld.runtime.event.Event;

/**
 * Simple event for EventBus benchmarking.
 */
public class SimpleEvent extends Event {
    private final String data;

    public SimpleEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
