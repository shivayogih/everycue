package com.everycue.feature.track

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackInsightsTest {
    @Test
    fun useScoreExcludesDonationsAndRoundsDown() {
        val events = listOf(
            event("1", TrackOutcome.CONSUMED),
            event("2", TrackOutcome.CONSUMED),
            event("3", TrackOutcome.DISCARDED),
            event("4", TrackOutcome.DONATED),
        )
        assertEquals(66, TrackUiState(events = events).usageEfficiency)
    }

    private fun event(id: String, outcome: TrackOutcome) = TrackEvent(id, id, "Item", outcome, 1.0, "item", 0, "")
}

