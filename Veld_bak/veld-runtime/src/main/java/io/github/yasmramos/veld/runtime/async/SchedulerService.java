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

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages scheduled task execution for @Scheduled methods.
 *
 * <p>Supports fixed-rate, fixed-delay, and cron-based scheduling.
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
public final class SchedulerService {
    
    private static volatile SchedulerService instance;
    
    private final ScheduledExecutorService scheduler;
    private final List<ScheduledFuture<?>> scheduledTasks;
    private volatile boolean shutdown = false;
    
    private SchedulerService() {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.scheduler = Executors.newScheduledThreadPool(cores, new VeldSchedulerThreadFactory());
        this.scheduledTasks = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return the SchedulerService instance
     */
    public static SchedulerService getInstance() {
        if (instance == null) {
            synchronized (SchedulerService.class) {
                if (instance == null) {
                    instance = new SchedulerService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Schedules a task at a fixed rate.
     *
     * @param task the task to execute
     * @param initialDelay initial delay before first execution
     * @param period period between executions
     * @param unit time unit
     * @return the scheduled future
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (shutdown) {
            throw new RejectedExecutionException("SchedulerService has been shut down");
        }
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            wrapTask(task), initialDelay, period, unit);
        scheduledTasks.add(future);
        return future;
    }
    
    /**
     * Schedules a task with fixed delay between completions.
     *
     * @param task the task to execute
     * @param initialDelay initial delay before first execution
     * @param delay delay between completion and next execution
     * @param unit time unit
     * @return the scheduled future
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (shutdown) {
            throw new RejectedExecutionException("SchedulerService has been shut down");
        }
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
            wrapTask(task), initialDelay, delay, unit);
        scheduledTasks.add(future);
        return future;
    }
    
    /**
     * Schedules a task based on a cron expression.
     *
     * @param task the task to execute
     * @param cronExpression the cron expression
     * @param zone the time zone (null for system default)
     */
    public void scheduleCron(Runnable task, String cronExpression, ZoneId zone) {
        if (shutdown) {
            throw new RejectedExecutionException("SchedulerService has been shut down");
        }
        ZoneId effectiveZone = zone != null ? zone : ZoneId.systemDefault();
        CronScheduler cronScheduler = new CronScheduler(task, cronExpression, effectiveZone);
        cronScheduler.scheduleNext();
    }
    
    /**
     * Wraps a task to handle exceptions gracefully.
     */
    private Runnable wrapTask(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("[Veld] Scheduled task failed: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    /**
     * Shuts down the scheduler gracefully.
     */
    public void shutdown() {
        shutdown = true;
        scheduledTasks.forEach(f -> f.cancel(false));
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Resets the singleton (for testing).
     */
    public static void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
    
    /**
     * Thread factory for scheduler threads.
     */
    private static class VeldSchedulerThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "veld-scheduler-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
    
    /**
     * Internal cron scheduler that reschedules after each execution.
     */
    private class CronScheduler {
        private final Runnable task;
        private final CronExpression cron;
        private final ZoneId zone;
        
        CronScheduler(Runnable task, String expression, ZoneId zone) {
            this.task = task;
            this.cron = CronExpression.parse(expression);
            this.zone = zone;
        }
        
        void scheduleNext() {
            if (shutdown) return;
            
            ZonedDateTime now = ZonedDateTime.now(zone);
            ZonedDateTime next = cron.next(now);
            if (next == null) return;
            
            long delay = Duration.between(now, next).toMillis();
            scheduler.schedule(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    System.err.println("[Veld] Cron task failed: " + e.getMessage());
                } finally {
                    scheduleNext(); // Reschedule for next execution
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Simple cron expression parser.
     * Supports: second minute hour day-of-month month day-of-week
     */
    static class CronExpression {
        private final int[] seconds;
        private final int[] minutes;
        private final int[] hours;
        private final int[] daysOfMonth;
        private final int[] months;
        private final int[] daysOfWeek;
        
        private CronExpression(int[] seconds, int[] minutes, int[] hours,
                               int[] daysOfMonth, int[] months, int[] daysOfWeek) {
            this.seconds = seconds;
            this.minutes = minutes;
            this.hours = hours;
            this.daysOfMonth = daysOfMonth;
            this.months = months;
            this.daysOfWeek = daysOfWeek;
        }
        
        static CronExpression parse(String expression) {
            String[] parts = expression.trim().split("\\s+");
            if (parts.length != 6) {
                throw new IllegalArgumentException(
                    "Cron expression must have 6 fields: second minute hour day-of-month month day-of-week");
            }
            return new CronExpression(
                parseField(parts[0], 0, 59),
                parseField(parts[1], 0, 59),
                parseField(parts[2], 0, 23),
                parseField(parts[3], 1, 31),
                parseField(parts[4], 1, 12),
                parseField(parts[5], 0, 6)
            );
        }
        
        private static int[] parseField(String field, int min, int max) {
            if ("*".equals(field) || "?".equals(field)) {
                return range(min, max);
            }
            if (field.contains("/")) {
                String[] parts = field.split("/");
                int step = Integer.parseInt(parts[1]);
                int start = "*".equals(parts[0]) ? min : Integer.parseInt(parts[0]);
                return range(start, max, step);
            }
            if (field.contains("-")) {
                String[] parts = field.split("-");
                return range(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
            if (field.contains(",")) {
                return Arrays.stream(field.split(","))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            }
            return new int[]{Integer.parseInt(field)};
        }
        
        private static int[] range(int start, int end) {
            return range(start, end, 1);
        }
        
        private static int[] range(int start, int end, int step) {
            List<Integer> values = new ArrayList<>();
            for (int i = start; i <= end; i += step) {
                values.add(i);
            }
            return values.stream().mapToInt(Integer::intValue).toArray();
        }
        
        ZonedDateTime next(ZonedDateTime from) {
            ZonedDateTime candidate = from.plusSeconds(1).withNano(0);
            
            for (int i = 0; i < 366 * 24 * 60; i++) { // Max 1 year search
                if (matches(candidate)) {
                    return candidate;
                }
                candidate = candidate.plusMinutes(1).withSecond(seconds[0]);
            }
            return null;
        }
        
        private boolean matches(ZonedDateTime dt) {
            return contains(seconds, dt.getSecond())
                && contains(minutes, dt.getMinute())
                && contains(hours, dt.getHour())
                && contains(daysOfMonth, dt.getDayOfMonth())
                && contains(months, dt.getMonthValue())
                && contains(daysOfWeek, dt.getDayOfWeek().getValue() % 7);
        }
        
        private boolean contains(int[] arr, int value) {
            for (int v : arr) {
                if (v == value) return true;
            }
            return false;
        }
    }
}
