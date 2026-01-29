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
package io.github.yasmramos.veld.annotation;

import java.lang.annotation.*;

/**
 * Marks a method to be executed on a schedule.
 *
 * <p>The annotated method must have no parameters and return void.
 * Scheduled methods are executed by a managed scheduler service.
 *
 * <p>Scheduling can be configured using:
 * <ul>
 *   <li>{@code fixedRate} - Execute at a fixed interval</li>
 *   <li>{@code fixedDelay} - Execute with a fixed delay between completions</li>
 *   <li>{@code cron} - Execute according to a cron expression</li>
 *   <li>{@code initialDelay} - Delay before first execution</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class CleanupService {
 *     
 *     @Scheduled(fixedRate = 60000) // Every minute
 *     public void cleanupTempFiles() {
 *         // Runs every 60 seconds
 *     }
 *     
 *     @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
 *     public void dailyBackup() {
 *         // Runs daily at 2:00 AM
 *     }
 *     
 *     @Scheduled(fixedDelay = 5000, initialDelay = 10000)
 *     public void processQueue() {
 *         // First run after 10s, then 5s after each completion
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {
    
    /**
     * Execute at a fixed rate in milliseconds.
     * The next execution starts regardless of when the previous one finished.
     *
     * @return fixed rate in milliseconds, -1 if not used
     */
    long fixedRate() default -1;
    
    /**
     * Execute with a fixed delay in milliseconds after completion.
     * The next execution starts after the previous one completes plus this delay.
     *
     * @return fixed delay in milliseconds, -1 if not used
     */
    long fixedDelay() default -1;
    
    /**
     * Cron expression for scheduling.
     * Format: "second minute hour day-of-month month day-of-week"
     *
     * <p>Examples:
     * <ul>
     *   <li>"0 0 * * * ?" - Every hour</li>
     *   <li>"0 0 2 * * ?" - Daily at 2 AM</li>
     *   <li>"0 0/30 * * * ?" - Every 30 minutes</li>
     *   <li>"0 0 9-17 * * MON-FRI" - Every hour 9-5 on weekdays</li>
     * </ul>
     *
     * @return cron expression, empty if not used
     */
    String cron() default "";
    
    /**
     * Initial delay in milliseconds before first execution.
     *
     * @return initial delay in milliseconds
     */
    long initialDelay() default 0;
    
    /**
     * Time zone for cron expressions.
     * Uses system default if empty.
     *
     * @return time zone ID (e.g., "UTC", "America/New_York")
     */
    String zone() default "";
}
