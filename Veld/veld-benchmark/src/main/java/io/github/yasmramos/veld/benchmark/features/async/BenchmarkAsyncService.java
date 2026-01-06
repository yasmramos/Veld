package io.github.yasmramos.veld.benchmark.features.async;

import io.github.yasmramos.veld.annotation.Async;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Servicio con métodos para benchmarking
 */
@Singleton
public class BenchmarkAsyncService {

    @Async
    public void executeAsync() {
        // Empty async method - will be wrapped by generated AOP code
    }

    public void syncMethod() {
        // Empty sync method for baseline comparison
    }
}
