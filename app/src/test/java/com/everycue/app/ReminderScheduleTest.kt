package com.everycue.app

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun nextReminderUsesSameDayWhenHourIsAhead() {
        val now = ZonedDateTime.of(2026, 8, 24, 8, 30, 0, 0, zone)
        assertEquals(TimeUnit.MINUTES.toMillis(30), delayUntilHourMillis(now, 9))
    }

    @Test
    fun nextReminderRollsToTomorrowWhenHourPassed() {
        val now = ZonedDateTime.of(2026, 8, 24, 10, 0, 0, 0, zone)
        assertEquals(TimeUnit.HOURS.toMillis(23), delayUntilHourMillis(now, 9))
    }
}

