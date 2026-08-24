package com.everycue.feature.track

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiryStateTest {
    private fun item(expiry: Long, warning: Int = 7) = TrackItem(
        id = "id",
        name = "Milk",
        category = TrackCategory.GROCERY,
        quantity = 1.0,
        unit = "pack",
        purchaseEpochDay = null,
        expiryEpochDay = expiry,
        storageLocation = "Fridge",
        notes = "",
        reminderDays = warning,
        createdAtMillis = 0,
        updatedAtMillis = 0,
    )

    @Test fun classifiesUsingLocalCalendarDays() {
        val today = 20_000L
        assertEquals(ExpiryState.EXPIRED, item(today - 1).expiryState(today))
        assertEquals(ExpiryState.EXPIRES_TODAY, item(today).expiryState(today))
        assertEquals(ExpiryState.EXPIRING_SOON, item(today + 7).expiryState(today))
        assertEquals(ExpiryState.FRESH, item(today + 8).expiryState(today))
    }
}

