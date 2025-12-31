package io.github.yasmramos.veld.benchmark.features.events;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.runtime.event.EventBus;

@Singleton
public class BenchmarkEventListener {
    private int handledCount = 0;

    @Subscribe
    public void onBenchmarkEvent(BenchmarkEvent event) {
        handledCount++;
    }

    public int getHandledCount() {
        return handledCount;
    }

    public void reset() {
        handledCount = 0;
    }
}
