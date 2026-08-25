package com.everycue.core.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripReadyEvaluatorTest {
    @Test
    fun `missing dates and empty packing list need attention without evaluating linked records`() {
        val result = TripReadyEvaluator.evaluate(
            TripReadyInput(
                tripStartEpochDay = null,
                tripEndEpochDay = null,
                packedCount = 0,
                totalCount = 0,
                linkedTrackItems = listOf(record("medicine", 50)),
                linkedRenewals = listOf(record("passport", 60)),
            ),
        )

        assertEquals(
            listOf(TripReadyReasonCode.PACKING_LIST_EMPTY, TripReadyReasonCode.TRIP_DATES_MISSING),
            result.findings.map(ReadinessFinding::reasonCode),
        )
        assertFalse(result.isReady)
    }

    @Test
    fun `records expiring before or during trip are critical`() {
        val result = TripReadyEvaluator.evaluate(
            TripReadyInput(
                tripStartEpochDay = 100,
                tripEndEpochDay = 105,
                packedCount = 2,
                totalCount = 2,
                linkedTrackItems = listOf(record("before", 99), record("during", 103), record("after", 106)),
                linkedRenewals = listOf(record("renew-before", 90), record("renew-during", 105)),
            ),
        )

        assertEquals(4, result.criticalCount)
        assertEquals(2, result.readyCount)
        assertEquals(
            listOf(
                TripReadyReasonCode.TRACK_EXPIRES_BEFORE_TRIP,
                TripReadyReasonCode.TRACK_EXPIRES_DURING_TRIP,
                TripReadyReasonCode.RENEWAL_DUE_BEFORE_TRIP,
                TripReadyReasonCode.RENEWAL_DUE_DURING_TRIP,
            ),
            result.findings.filter { it.severity == TripReadySeverity.CRITICAL }.map(ReadinessFinding::reasonCode),
        )
    }

    @Test
    fun `complete packing and records valid beyond trip are ready with stable ordering`() {
        val input = TripReadyInput(
            tripStartEpochDay = 100,
            tripEndEpochDay = 101,
            packedCount = 1,
            totalCount = 1,
            linkedTrackItems = listOf(record("Zulu", 120), record("alpha", 115)),
            linkedRenewals = listOf(record("Passport", 500)),
        )

        val first = TripReadyEvaluator.evaluate(input)
        val second = TripReadyEvaluator.evaluate(input)

        assertTrue(first.isReady)
        assertEquals(first, second)
        assertEquals(listOf("", "alpha", "Zulu", "Passport"), first.findings.map(ReadinessFinding::displayName))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid packing counts are rejected`() {
        TripReadyEvaluator.evaluate(
            TripReadyInput(1, 2, packedCount = 3, totalCount = 2, emptyList(), emptyList()),
        )
    }

    @Test
    fun `hundreds of linked records produce complete stable findings`() {
        val records = (1..500).map { index -> record("item-$index", 100L + index) }
        val input = TripReadyInput(100, 200, 100, 100, records, emptyList())

        val first = TripReadyEvaluator.evaluate(input)
        val second = TripReadyEvaluator.evaluate(input)

        assertEquals(501, first.findings.size)
        assertEquals(first, second)
        assertEquals(100, first.criticalCount)
        assertEquals(401, first.readyCount)
    }

    private fun record(id: String, day: Long) = TripReadyRecordInput(id, id, day)
}
