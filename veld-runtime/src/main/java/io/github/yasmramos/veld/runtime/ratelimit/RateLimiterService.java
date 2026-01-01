/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.runtime.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter service using token bucket algorithm.
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
public class RateLimiterService {

    private static final RateLimiterService INSTANCE = new RateLimiterService();

    private final Map<String, TokenBucket> limiters = new ConcurrentHashMap<>();

    private RateLimiterService() {}

    public static RateLimiterService getInstance() {
        return INSTANCE;
    }

    /**
     * Acquires a permit, blocking if necessary.
     *
     * @param key limiter key
     * @param permits max permits per period
     * @param periodMs period in milliseconds
     * @param timeoutMs max wait time (0 = indefinite)
     * @return true if acquired, false if timeout
     */
    public boolean acquire(String key, int permits, long periodMs, long timeoutMs) {
        TokenBucket bucket = limiters.computeIfAbsent(key, 
            k -> new TokenBucket(permits, periodMs));
        
        return bucket.tryAcquire(timeoutMs);
    }

    /**
     * Tries to acquire a permit without blocking.
     *
     * @param key limiter key
     * @param permits max permits per period
     * @param periodMs period in milliseconds
     * @return true if acquired immediately, false otherwise
     */
    public boolean tryAcquire(String key, int permits, long periodMs) {
        TokenBucket bucket = limiters.computeIfAbsent(key, 
            k -> new TokenBucket(permits, periodMs));
        
        return bucket.tryAcquireNow();
    }

    /**
     * Token bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final long refillPeriodMs;
        private final Semaphore semaphore;
        private volatile long lastRefillTime;

        TokenBucket(int maxTokens, long refillPeriodMs) {
            this.maxTokens = maxTokens;
            this.refillPeriodMs = refillPeriodMs;
            this.semaphore = new Semaphore(maxTokens, true);
            this.lastRefillTime = System.currentTimeMillis();
        }

        boolean tryAcquire(long timeoutMs) {
            refillIfNeeded();
            try {
                if (timeoutMs <= 0) {
                    semaphore.acquire();
                    return true;
                }
                return semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        boolean tryAcquireNow() {
            refillIfNeeded();
            return semaphore.tryAcquire();
        }

        private synchronized void refillIfNeeded() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            
            if (elapsed >= refillPeriodMs) {
                int tokensToAdd = (int) (elapsed / refillPeriodMs) * maxTokens;
                int currentTokens = semaphore.availablePermits();
                int tokensNeeded = Math.min(tokensToAdd, maxTokens - currentTokens);
                
                if (tokensNeeded > 0) {
                    semaphore.release(tokensNeeded);
                }
                lastRefillTime = now;
            }
        }
    }

    /**
     * Exception thrown when rate limit is exceeded.
     */
    public static class RateLimitExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
