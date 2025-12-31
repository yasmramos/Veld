package io.github.yasmramos.veld.benchmark.features.event;

import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.annotation.Singleton;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple event listener for EventBus benchmarking.
 */
@Singleton
public class SimpleListener {
    private final AtomicInteger callCount = new AtomicInteger(0);

    @Subscribe
    public void handleSimpleEvent(SimpleEvent event) {
        callCount.incrementAndGet();
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void reset() {
        callCount.set(0);
    }
}
