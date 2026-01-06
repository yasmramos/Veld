package io.github.yasmramos.veld.benchmark.features.lifecycle;

import io.github.yasmramos.veld.annotation.Singleton;

@Singleton
public class PlainLifecycleBean {
    private boolean initialized = false;

    public void init() {
        initialized = true;
    }

    public void doWork() {
        // Empty method for benchmarking
    }

    public boolean isInitialized() { return initialized; }
}
