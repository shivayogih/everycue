package com.everycue.feature.renew

import org.junit.Assert.assertEquals
import org.junit.Test

class DueStateTest {
    private fun renewal(due: Long, warning: Int = 30) = RenewalItem(
        id = "id",
        title = "Passport",
        type = RenewalType.DOCUMENT,
        dueEpochDay = due,
        reminderDays = warning,
        provider = "",
        referenceNumber = "",
        notes = "",
        lastRenewedEpochDay = null,
        createdAtMillis = 0,
        updatedAtMillis = 0,
    )

    @Test fun classifiesDueDatesAtBoundaries() {
        val today = 20_000L
        assertEquals(DueState.OVERDUE, renewal(today - 1).dueState(today))
        assertEquals(DueState.DUE_TODAY, renewal(today).dueState(today))
        assertEquals(DueState.DUE_SOON, renewal(today + 30).dueState(today))
        assertEquals(DueState.UPCOMING, renewal(today + 31).dueState(today))
    }
}

