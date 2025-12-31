package io.github.yasmramos.veld.benchmark.features.lifecycle;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.OnStart;
import io.github.yasmramos.veld.annotation.OnStop;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;

@Singleton
public class LifecycleBean {
    private boolean initialized = false;
    private boolean started = false;
    private boolean stopped = false;
    private boolean destroyed = false;

    public LifecycleBean() {
        // Public no-arg constructor required by Veld
    }

    @PostConstruct
    public void init() {
        initialized = true;
    }

    @OnStart
    public void start() {
        started = true;
    }

    @OnStop
    public void stop() {
        stopped = true;
    }

    @PreDestroy
    public void destroy() {
        destroyed = true;
    }

    public void doWork() {
        // Empty method for benchmarking
    }

    public boolean isInitialized() { return initialized; }
    public boolean isStarted() { return started; }
    public boolean isStopped() { return stopped; }
    public boolean isDestroyed() { return destroyed; }
}
