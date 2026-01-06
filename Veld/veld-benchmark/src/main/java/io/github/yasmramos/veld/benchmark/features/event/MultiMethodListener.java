package io.github.yasmramos.veld.benchmark.features.event;

import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.annotation.Singleton;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-method event listener for EventBus benchmarking.
 */
@Singleton
public class MultiMethodListener {
    private final AtomicInteger event1Count = new AtomicInteger(0);
    private final AtomicInteger event2Count = new AtomicInteger(0);
    private final AtomicInteger event3Count = new AtomicInteger(0);

    @Subscribe
    public void handleEvent1(SimpleEvent event) {
        event1Count.incrementAndGet();
    }

    @Subscribe
    public void handleEvent2(SimpleEvent event) {
        event2Count.incrementAndGet();
    }

    @Subscribe(priority = 10)
    public void handleEvent3(SimpleEvent event) {
        event3Count.incrementAndGet();
    }

    public void reset() {
        event1Count.set(0);
        event2Count.set(0);
        event3Count.set(0);
    }
}
