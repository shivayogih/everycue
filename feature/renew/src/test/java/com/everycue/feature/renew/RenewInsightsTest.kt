package com.everycue.feature.renew

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RenewInsightsTest {
    @Test
    fun scheduleBucketsUseEachItemsReminderWindow() {
        val today = LocalDate.now().toEpochDay()
        val state = RenewUiState(
            renewals = listOf(
                renewal("overdue", today - 1, 30),
                renewal("soon", today + 5, 7),
                renewal("upcoming", today + 20, 7),
            ),
        )
        assertEquals(1, state.overdueCount)
        assertEquals(1, state.dueSoonCount)
        assertEquals(1, state.upcomingCount)
    }

    private fun renewal(id: String, due: Long, reminderDays: Int) = RenewalItem(
        id, id, RenewalType.OTHER, due, reminderDays, "", "", "", null, 0, 0,
    )
}

