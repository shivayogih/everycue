package com.everycue.core.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WasteCoachTest {
    private val today = 20_000L

    @Test
    fun `stays silent below the evidence threshold`() {
        val result = WasteCoach.analyze(
            events = listOf(discard("milk", 1), discard("milk", 8)),
            todayEpochDay = today,
        )

        assertTrue(result.insights.isEmpty())
    }

    @Test
    fun `explains repeated product discards with structured evidence`() {
        val result = WasteCoach.analyze(
            events = listOf(discard("milk", 1), discard("milk", 8), discard("milk", 20)),
            todayEpochDay = today,
        )

        val insight = result.insights.single { it.pattern.type == WastePatternType.REPEATED_PRODUCT_DISCARD }
        assertEquals(3, insight.pattern.evidence.occurrences)
        assertEquals(WasteSuggestionCode.TRY_A_SMALLER_AMOUNT, insight.suggestionCode)
        assertTrue(WasteReasonCode.MINIMUM_EVIDENCE_MET in insight.reasonCodes)
    }

    @Test
    fun `hidden subjects and dismissed insights are removed`() {
        val events = listOf(discard("milk", 1), discard("milk", 8), discard("milk", 20))

        assertTrue(
            WasteCoach.analyze(
                events,
                today,
                WasteCoachExclusions(hiddenSubjectIds = setOf("milk")),
            ).insights.isEmpty(),
        )
        assertTrue(
            WasteCoach.analyze(
                events,
                today,
                WasteCoachExclusions(dismissedInsightKeys = setOf("product:milk:90")),
            ).insights.isEmpty(),
        )
    }

    @Test
    fun `reports fixed local windows and a sufficiently supported rising trend`() {
        val current = listOf(
            discard("a", 1),
            discard("b", 2),
            discard("c", 3),
            consumed("d", 4),
        )
        val previous = listOf(
            discard("e", 31),
            consumed("f", 32),
            consumed("g", 33),
            consumed("h", 34),
        )

        val result = WasteCoach.analyze(current + previous, today)

        assertEquals(
            listOf(WasteWindow.THIRTY_DAYS, WasteWindow.THREE_MONTHS, WasteWindow.TWELVE_MONTHS),
            result.summaries.map(WasteWindowSummary::window),
        )
        val trend = result.insights.single { it.pattern.type == WastePatternType.DISCARD_RATE_INCREASED }
        assertEquals(75, trend.pattern.evidence.discardRatePercent)
        assertEquals(25, trend.pattern.evidence.previousDiscardRatePercent)
    }

    private fun discard(id: String, daysAgo: Long) = event(id, daysAgo, WasteOutcome.DISCARDED)
    private fun consumed(id: String, daysAgo: Long) = event(id, daysAgo, WasteOutcome.CONSUMED)

    private fun event(id: String, daysAgo: Long, outcome: WasteOutcome) = WasteEventInput(
        subjectId = id,
        subjectLabel = id.replaceFirstChar(Char::uppercase),
        category = "GROCERY",
        outcome = outcome,
        occurredEpochDay = today - daysAgo,
    )
}
