package com.everycue.app

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun nextReminderUsesSameDayWhenTimeIsAhead() {
        val now = ZonedDateTime.of(2026, 8, 24, 8, 30, 0, 0, zone)
        assertEquals(TimeUnit.MINUTES.toMillis(45), delayUntilTimeMillis(now, 9, 15))
    }

    @Test
    fun nextReminderRollsToTomorrowWhenHourPassed() {
        val now = ZonedDateTime.of(2026, 8, 24, 10, 0, 0, 0, zone)
        assertEquals(TimeUnit.HOURS.toMillis(23), delayUntilTimeMillis(now, 9, 0))
    }

    @Test
    fun reminderTimeLabelUsesTwelveHourAmPmFormat() {
        assertEquals("9:05 PM", AppSettings(reminderHour = 21, reminderMinute = 5).reminderTimeLabel(java.util.Locale.US))
    }
}
