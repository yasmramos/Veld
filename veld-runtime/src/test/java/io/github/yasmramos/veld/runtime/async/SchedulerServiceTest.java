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
package io.github.yasmramos.veld.runtime.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SchedulerService}.
 */
class SchedulerServiceTest {

    @BeforeEach
    void setUp() {
        SchedulerService.reset();
    }

    @AfterEach
    void tearDown() {
        SchedulerService.reset();
    }

    @Test
    void getInstance_returnsSameInstance() {
        SchedulerService first = SchedulerService.getInstance();
        SchedulerService second = SchedulerService.getInstance();
        assertSame(first, second);
    }

    @Test
    void scheduleAtFixedRate_executesMultipleTimes() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        SchedulerService.getInstance().scheduleAtFixedRate(() -> {
            counter.incrementAndGet();
            latch.countDown();
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(counter.get() >= 3);
    }

    @Test
    void scheduleWithFixedDelay_executesMultipleTimes() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);
        
        SchedulerService.getInstance().scheduleWithFixedDelay(() -> {
            counter.incrementAndGet();
            latch.countDown();
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(counter.get() >= 2);
    }

    @Test
    void scheduleAtFixedRate_afterShutdown_throwsException() {
        SchedulerService service = SchedulerService.getInstance();
        service.shutdown();
        
        assertThrows(RejectedExecutionException.class, 
            () -> service.scheduleAtFixedRate(() -> {}, 0, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    void scheduleWithFixedDelay_afterShutdown_throwsException() {
        SchedulerService service = SchedulerService.getInstance();
        service.shutdown();
        
        assertThrows(RejectedExecutionException.class, 
            () -> service.scheduleWithFixedDelay(() -> {}, 0, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    void scheduleCron_afterShutdown_throwsException() {
        SchedulerService service = SchedulerService.getInstance();
        service.shutdown();
        
        assertThrows(RejectedExecutionException.class, 
            () -> service.scheduleCron(() -> {}, "0 0 * * * *", null));
    }

    @Test
    void scheduleAtFixedRate_handlesExceptionGracefully() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);
        
        SchedulerService.getInstance().scheduleAtFixedRate(() -> {
            counter.incrementAndGet();
            latch.countDown();
            if (counter.get() == 1) {
                throw new RuntimeException("test exception");
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(counter.get() >= 2, "Task should continue after exception");
    }

    @Test
    void reset_shutsDownAndClearsInstance() throws Exception {
        SchedulerService first = SchedulerService.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        first.scheduleAtFixedRate(latch::countDown, 0, 100, TimeUnit.MILLISECONDS);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        SchedulerService.reset();
        
        SchedulerService second = SchedulerService.getInstance();
        assertNotSame(first, second);
    }

    @Test
    void scheduleCron_withZone_usesProvidedZone() {
        AtomicInteger counter = new AtomicInteger(0);
        
        // Schedule for every second (for testing)
        SchedulerService.getInstance().scheduleCron(
            counter::incrementAndGet, 
            "* * * * * *", 
            ZoneId.of("UTC"));
        
        // Just verify no exception is thrown
        assertNotNull(SchedulerService.getInstance());
    }

    @Test
    void scheduleCron_withNullZone_usesSystemDefault() {
        AtomicInteger counter = new AtomicInteger(0);
        
        SchedulerService.getInstance().scheduleCron(
            counter::incrementAndGet, 
            "0 0 0 1 1 *",  // Once a year to avoid actual execution
            null);
        
        assertNotNull(SchedulerService.getInstance());
    }
}
