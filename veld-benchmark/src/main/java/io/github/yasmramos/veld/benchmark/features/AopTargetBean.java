package io.github.yasmramos.veld.benchmark.features;

import io.github.yasmramos.veld.annotation.Component;

@Component
public class AopTargetBean {
    public void doWork() {
        // Intentionally empty method to measure pure AOP overhead
    }
}
