package com.everycue.core.recommendation

enum class WasteOutcome { CONSUMED, DISCARDED, DONATED }

enum class WasteWindow(val days: Long) {
    THIRTY_DAYS(30),
    THREE_MONTHS(90),
    TWELVE_MONTHS(365),
}

enum class WastePatternType {
    REPEATED_PRODUCT_DISCARD,
    REPEATED_CATEGORY_DISCARD,
    DISCARD_RATE_INCREASED,
}

enum class WasteReasonCode {
    MINIMUM_EVIDENCE_MET,
    REPEATED_DISCARD,
    DISCARD_RATE_INCREASED,
}

enum class WasteSuggestionCode {
    TRY_A_SMALLER_AMOUNT,
    PLAN_AN_EARLIER_USE,
    REVIEW_CATEGORY_BUYING,
}

data class WasteEventInput(
    val subjectId: String,
    val subjectLabel: String,
    val category: String,
    val outcome: WasteOutcome,
    val occurredEpochDay: Long,
)

data class WasteWindowSummary(
    val window: WasteWindow,
    val windowStartEpochDay: Long,
    val windowEndEpochDay: Long,
    val consumedCount: Int,
    val discardedCount: Int,
    val donatedCount: Int,
)

data class WasteEvidence(
    val occurrences: Int,
    val decisions: Int,
    val discardRatePercent: Int,
    val previousDiscardRatePercent: Int? = null,
    val windowStartEpochDay: Long,
    val windowEndEpochDay: Long,
)

data class WastePattern(
    val type: WastePatternType,
    val subjectId: String?,
    val subjectLabel: String?,
    val category: String?,
    val evidence: WasteEvidence,
)

data class CoachInsight(
    val key: String,
    val pattern: WastePattern,
    val reasonCodes: List<WasteReasonCode>,
    val suggestionCode: WasteSuggestionCode,
)

data class WasteCoachExclusions(
    val hiddenSubjectIds: Set<String> = emptySet(),
    val hiddenCategories: Set<String> = emptySet(),
    val dismissedInsightKeys: Set<String> = emptySet(),
)

data class WasteCoachResult(
    val summaries: List<WasteWindowSummary>,
    val insights: List<CoachInsight>,
)

/**
 * Local, deterministic coaching. Thresholds deliberately prefer silence over weak claims.
 * Prices are not accepted, so this policy can never imply an unsupported wasted-value estimate.
 */
object WasteCoach {
    fun analyze(
        events: List<WasteEventInput>,
        todayEpochDay: Long,
        exclusions: WasteCoachExclusions = WasteCoachExclusions(),
    ): WasteCoachResult {
        val eligible = events.filter {
            it.occurredEpochDay <= todayEpochDay &&
                it.subjectId !in exclusions.hiddenSubjectIds &&
                it.category !in exclusions.hiddenCategories
        }
        val summaries = WasteWindow.entries.map { window ->
            val windowEvents = eligible.inWindow(todayEpochDay - window.days + 1, todayEpochDay)
            WasteWindowSummary(
                window = window,
                windowStartEpochDay = todayEpochDay - window.days + 1,
                windowEndEpochDay = todayEpochDay,
                consumedCount = windowEvents.count { it.outcome == WasteOutcome.CONSUMED },
                discardedCount = windowEvents.count { it.outcome == WasteOutcome.DISCARDED },
                donatedCount = windowEvents.count { it.outcome == WasteOutcome.DONATED },
            )
        }

        val current90 = eligible.inWindow(todayEpochDay - WasteWindow.THREE_MONTHS.days + 1, todayEpochDay)
        val insights = buildList {
            current90.groupBy(WasteEventInput::subjectId).forEach { (subjectId, subjectEvents) ->
                val discards = subjectEvents.count { it.outcome == WasteOutcome.DISCARDED }
                val decisions = subjectEvents.decisionCount()
                if (discards >= 3 && decisions >= 3) {
                    val label = subjectEvents.first().subjectLabel
                    add(
                        CoachInsight(
                            key = "product:$subjectId:90",
                            pattern = WastePattern(
                                type = WastePatternType.REPEATED_PRODUCT_DISCARD,
                                subjectId = subjectId,
                                subjectLabel = label,
                                category = subjectEvents.first().category,
                                evidence = evidence(discards, decisions, todayEpochDay - 89, todayEpochDay),
                            ),
                            reasonCodes = listOf(
                                WasteReasonCode.MINIMUM_EVIDENCE_MET,
                                WasteReasonCode.REPEATED_DISCARD,
                            ),
                            suggestionCode = WasteSuggestionCode.TRY_A_SMALLER_AMOUNT,
                        ),
                    )
                }
            }

            current90.groupBy(WasteEventInput::category).forEach { (category, categoryEvents) ->
                val discards = categoryEvents.count { it.outcome == WasteOutcome.DISCARDED }
                val decisions = categoryEvents.decisionCount()
                if (discards >= 4 && decisions >= 5) {
                    add(
                        CoachInsight(
                            key = "category:$category:90",
                            pattern = WastePattern(
                                type = WastePatternType.REPEATED_CATEGORY_DISCARD,
                                subjectId = null,
                                subjectLabel = null,
                                category = category,
                                evidence = evidence(discards, decisions, todayEpochDay - 89, todayEpochDay),
                            ),
                            reasonCodes = listOf(
                                WasteReasonCode.MINIMUM_EVIDENCE_MET,
                                WasteReasonCode.REPEATED_DISCARD,
                            ),
                            suggestionCode = WasteSuggestionCode.REVIEW_CATEGORY_BUYING,
                        ),
                    )
                }
            }

            val current30 = eligible.inWindow(todayEpochDay - 29, todayEpochDay)
            val previous30 = eligible.inWindow(todayEpochDay - 59, todayEpochDay - 30)
            val currentDecisions = current30.decisionCount()
            val previousDecisions = previous30.decisionCount()
            val currentDiscards = current30.count { it.outcome == WasteOutcome.DISCARDED }
            val previousDiscards = previous30.count { it.outcome == WasteOutcome.DISCARDED }
            val currentRate = rate(currentDiscards, currentDecisions)
            val previousRate = rate(previousDiscards, previousDecisions)
            if (
                currentDecisions >= 4 &&
                previousDecisions >= 4 &&
                currentDiscards >= 3 &&
                currentRate - previousRate >= 20
            ) {
                add(
                    CoachInsight(
                        key = "trend:all:30",
                        pattern = WastePattern(
                            type = WastePatternType.DISCARD_RATE_INCREASED,
                            subjectId = null,
                            subjectLabel = null,
                            category = null,
                            evidence = WasteEvidence(
                                occurrences = currentDiscards,
                                decisions = currentDecisions,
                                discardRatePercent = currentRate,
                                previousDiscardRatePercent = previousRate,
                                windowStartEpochDay = todayEpochDay - 29,
                                windowEndEpochDay = todayEpochDay,
                            ),
                        ),
                        reasonCodes = listOf(
                            WasteReasonCode.MINIMUM_EVIDENCE_MET,
                            WasteReasonCode.DISCARD_RATE_INCREASED,
                        ),
                        suggestionCode = WasteSuggestionCode.PLAN_AN_EARLIER_USE,
                    ),
                )
            }
        }
            .filterNot { it.key in exclusions.dismissedInsightKeys }
            .sortedWith(compareByDescending<CoachInsight> { it.pattern.evidence.occurrences }.thenBy { it.key })

        return WasteCoachResult(summaries = summaries, insights = insights)
    }

    private fun evidence(discards: Int, decisions: Int, start: Long, end: Long) = WasteEvidence(
        occurrences = discards,
        decisions = decisions,
        discardRatePercent = rate(discards, decisions),
        windowStartEpochDay = start,
        windowEndEpochDay = end,
    )

    private fun List<WasteEventInput>.inWindow(start: Long, end: Long) =
        filter { it.occurredEpochDay in start..end }

    private fun List<WasteEventInput>.decisionCount() =
        count { it.outcome == WasteOutcome.CONSUMED || it.outcome == WasteOutcome.DISCARDED }

    private fun rate(discards: Int, decisions: Int): Int =
        if (decisions == 0) 0 else (discards * 100f / decisions).toInt()
}
