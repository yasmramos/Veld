package io.github.yasmramos.veld.resilience.example;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FlakyExternalService {

    public enum FailureMode {
        RANDOM,
        ALWAYS_FAIL,
        FAIL_FIRST_N,
        INTERMITTENT
    }

    public static class ExternalServiceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ExternalServiceException(String message) {
            super(message);
        }

        public ExternalServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final AtomicInteger callCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicBoolean outage = new AtomicBoolean(false);
    private final AtomicLong latencyMs = new AtomicLong(0L);

    private volatile FailureMode failureMode = FailureMode.RANDOM;
    private volatile double failureRate = 0.0;
    private volatile int failFirstN = 0;
    private volatile int intermittentEvery = 3;

    public FlakyExternalService() {
    }

    public FlakyExternalService(FailureMode failureMode) {
        this.failureMode = failureMode;
    }

    public static FlakyExternalService alwaysFailing() {
        FlakyExternalService service = new FlakyExternalService(FailureMode.ALWAYS_FAIL);
        service.setFailureRate(1.0);
        return service;
    }

    public static FlakyExternalService flakyWithRate(double rate) {
        FlakyExternalService service = new FlakyExternalService(FailureMode.RANDOM);
        service.setFailureRate(rate);
        return service;
    }

    public static FlakyExternalService recoveringAfter(int n) {
        FlakyExternalService service = new FlakyExternalService(FailureMode.FAIL_FIRST_N);
        service.setFailFirstN(n);
        return service;
    }

    public static FlakyExternalService intermittent(int every) {
        FlakyExternalService service = new FlakyExternalService(FailureMode.INTERMITTENT);
        service.setIntermittentEvery(every);
        return service;
    }

    public String callApi() {
        int attempt = callCount.incrementAndGet();
        System.out.println("[FlakyExternalService] callApi() attempt #" + attempt);

        long delay = latencyMs.get();
        if (delay > 0L) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExternalServiceException("Call interrupted while simulating latency", e);
            }
        }

        if (outage.get()) {
            failureCount.incrementAndGet();
            System.out.println("[FlakyExternalService] Outage active, failing call #" + attempt);
            throw new ExternalServiceException("Service is currently unavailable (outage)");
        }

        if (shouldFail(attempt)) {
            failureCount.incrementAndGet();
            System.out.println("[FlakyExternalService] Failing call #" + attempt + " (mode=" + failureMode + ")");
            throw new ExternalServiceException("Simulated failure on call #" + attempt);
        }

        successCount.incrementAndGet();
        System.out.println("[FlakyExternalService] Success on call #" + attempt);
        return "OK#" + attempt;
    }

    private boolean shouldFail(int attempt) {
        switch (failureMode) {
            case ALWAYS_FAIL:
                return true;
            case FAIL_FIRST_N:
                return attempt <= failFirstN;
            case INTERMITTENT:
                return intermittentEvery > 0 && (attempt % intermittentEvery == 0);
            case RANDOM:
            default:
                return failureRate > 0.0 && ThreadLocalRandom.current().nextDouble() < failureRate;
        }
    }

    public void simulateOutage(boolean active) {
        outage.set(active);
        System.out.println("[FlakyExternalService] Outage " + (active ? "started" : "ended"));
    }

    public void setFailureRate(double rate) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("failureRate must be between 0.0 and 1.0");
        }
        this.failureRate = rate;
    }

    public void setLatencyMs(long ms) {
        if (ms < 0L) {
            throw new IllegalArgumentException("latencyMs must be >= 0");
        }
        this.latencyMs.set(ms);
    }

    public void setFailureMode(FailureMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("failureMode must not be null");
        }
        this.failureMode = mode;
    }

    public void setFailFirstN(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("failFirstN must be >= 0");
        }
        this.failFirstN = n;
    }

    public void setIntermittentEvery(int every) {
        if (every < 1) {
            throw new IllegalArgumentException("intermittentEvery must be >= 1");
        }
        this.intermittentEvery = every;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public FailureMode getFailureMode() {
        return failureMode;
    }

    public void reset() {
        callCount.set(0);
        failureCount.set(0);
        successCount.set(0);
        outage.set(false);
        latencyMs.set(0L);
        System.out.println("[FlakyExternalService] State reset");
    }
}
