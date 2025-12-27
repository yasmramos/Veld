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

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SchedulerService.CronExpression}.
 */
class CronExpressionTest {

    @Test
    void parse_validExpression_succeeds() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 * * *");
        assertNotNull(cron);
    }

    @Test
    void parse_invalidFieldCount_throwsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> SchedulerService.CronExpression.parse("0 0 12 * *"));
    }

    @Test
    void parse_wildcardField_acceptsAll() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("* * * * * *");
        assertNotNull(cron);
    }

    @Test
    void parse_questionMark_acceptsAll() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 ? * ?");
        assertNotNull(cron);
    }

    @Test
    void parse_stepExpression_parsesCorrectly() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("*/15 0 * * * *");
        assertNotNull(cron);
    }

    @Test
    void parse_rangeExpression_parsesCorrectly() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 9-17 * * *");
        assertNotNull(cron);
    }

    @Test
    void parse_listExpression_parsesCorrectly() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0,30 * * * *");
        assertNotNull(cron);
    }

    @Test
    void parse_specificValue_parsesCorrectly() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("30 15 10 1 6 3");
        assertNotNull(cron);
    }

    @Test
    void next_findsNextMatchingTime() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(12, next.getHour());
        assertEquals(0, next.getMinute());
        assertEquals(0, next.getSecond());
    }

    @Test
    void next_everySecond_returnsNextSecond() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("* * * * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 30, 45, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(46, next.getSecond());
    }

    @Test
    void next_everyMinute_returnsNextMinute() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 * * * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 30, 45, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(31, next.getMinute());
        assertEquals(0, next.getSecond());
    }

    @Test
    void next_specificDayOfWeek_findsCorrectDay() {
        // Schedule for Monday (1 in Java DayOfWeek % 7 = 1)
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 * * 1");
        // Start from Sunday January 5, 2025
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 5, 10, 0, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        // Should find Monday January 6, 2025
        assertEquals(6, next.getDayOfMonth());
    }

    @Test
    void next_stepExpression_findsCorrectInterval() {
        // Every 15 minutes
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 */15 * * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 7, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(15, next.getMinute());
    }

    @Test
    void next_rangeExpression_findsWithinRange() {
        // Only hours 9-17
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 9-17 * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 18, 0, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(9, next.getHour());
        assertEquals(2, next.getDayOfMonth()); // Next day
    }

    @Test
    void parse_stepWithStart_parsesCorrectly() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("5/10 * * * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertTrue(next.getSecond() >= 5);
    }

    @Test
    void next_specificMonth_findsCorrectMonth() {
        // Only in June
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 1 6 *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(6, next.getMonthValue());
    }

    @Test
    void next_specificDayOfMonth_findsCorrectDay() {
        // Only on 15th
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 15 * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(15, next.getDayOfMonth());
    }

    @Test
    void parse_multipleValues_parsesCorrectly() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0,15,30,45 * * * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 10, 0, 10, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertTrue(next.getSecond() == 15 || next.getSecond() == 30 || next.getSecond() == 45);
    }

    @Test
    void next_atEndOfDay_wrapsToNextDay() {
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 8 * * *");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 1, 23, 59, 59, 0, ZoneId.of("UTC"));
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(2, next.getDayOfMonth());
        assertEquals(8, next.getHour());
    }

    @Test
    void next_sunday_calculatesCorrectly() {
        // Sunday = 0
        SchedulerService.CronExpression cron = SchedulerService.CronExpression.parse("0 0 12 * * 0");
        ZonedDateTime from = ZonedDateTime.of(2025, 1, 6, 10, 0, 0, 0, ZoneId.of("UTC")); // Monday
        
        ZonedDateTime next = cron.next(from);
        
        assertNotNull(next);
        assertEquals(java.time.DayOfWeek.SUNDAY, next.getDayOfWeek());
    }
}
