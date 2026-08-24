package com.everycue.feature.pack

import org.junit.Assert.assertEquals
import org.junit.Test

class TripProgressTest {
    @Test
    fun progress_isZero_whenTripHasNoItems() {
        assertEquals(0f, Trip(id = 1, name = "Empty").progress)
    }

    @Test
    fun progress_reflectsPackedItems() {
        val trip = Trip(
            id = 2,
            name = "Test",
            items = listOf(
                PackingItem(id = 1, name = "One", isPacked = true),
                PackingItem(id = 2, name = "Two", isPacked = false),
                PackingItem(id = 3, name = "Three", isPacked = true),
                PackingItem(id = 4, name = "Four", isPacked = false),
            ),
        )
        assertEquals(0.5f, trip.progress)
        assertEquals(2, trip.packedCount)
        assertEquals(4, trip.totalCount)
    }
}
