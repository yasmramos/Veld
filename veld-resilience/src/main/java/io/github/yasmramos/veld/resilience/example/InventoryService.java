package io.github.yasmramos.veld.resilience.example;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class InventoryService {

    private final CircuitBreaker circuitBreaker;
    private final AtomicBoolean remoteHealthy = new AtomicBoolean(true);

    public InventoryService() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker("inventoryService");
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setRemoteHealthy(boolean healthy) {
        this.remoteHealthy.set(healthy);
    }

    public CircuitBreaker.State getState() {
        return circuitBreaker.getState();
    }

    public int checkStock(String productId) {
        Supplier<Integer> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> remoteCheckStock(productId));
        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            return fallbackStock(productId);
        } catch (Exception e) {
            return fallbackStock(productId);
        }
    }

    private int remoteCheckStock(String productId) {
        if (!remoteHealthy.get()) {
            throw new IllegalStateException("Remote inventory service unavailable");
        }
        return Math.abs(productId.hashCode() % 100);
    }

    private int fallbackStock(String productId) {
        return 0;
    }
}
