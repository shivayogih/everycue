package com.everycue.core.recommendation

import java.util.Locale

enum class TripReadySeverity { CRITICAL, NEEDS_ATTENTION, READY }

enum class TripReadySource { PACK, TRACK, RENEW }

enum class TripReadyReasonCode {
    TRIP_DATES_MISSING,
    PACKING_LIST_EMPTY,
    PACKING_INCOMPLETE,
    PACKING_COMPLETE,
    TRACK_EXPIRES_BEFORE_TRIP,
    TRACK_EXPIRES_DURING_TRIP,
    TRACK_VALID_THROUGH_TRIP,
    RENEWAL_DUE_BEFORE_TRIP,
    RENEWAL_DUE_DURING_TRIP,
    RENEWAL_VALID_THROUGH_TRIP,
}

data class TripReadyRecordInput(
    val id: String,
    val displayName: String,
    val dateEpochDay: Long,
)

data class TripReadyInput(
    val tripStartEpochDay: Long?,
    val tripEndEpochDay: Long?,
    val packedCount: Int,
    val totalCount: Int,
    val linkedTrackItems: List<TripReadyRecordInput>,
    val linkedRenewals: List<TripReadyRecordInput>,
)

data class ReadinessFinding(
    val severity: TripReadySeverity,
    val source: TripReadySource,
    val sourceId: String?,
    val displayName: String,
    val reasonCode: TripReadyReasonCode,
    val relevantEpochDay: Long? = null,
)

data class TripReadiness(
    val findings: List<ReadinessFinding>,
) {
    val criticalCount: Int get() = findings.count { it.severity == TripReadySeverity.CRITICAL }
    val attentionCount: Int get() = findings.count { it.severity == TripReadySeverity.NEEDS_ATTENTION }
    val readyCount: Int get() = findings.count { it.severity == TripReadySeverity.READY }
    val isReady: Boolean get() = criticalCount == 0 && attentionCount == 0
}

/** Deterministic, framework-free Trip Ready rules. Dates must already be user-confirmed. */
object TripReadyEvaluator {
    fun evaluate(input: TripReadyInput): TripReadiness {
        require(input.packedCount in 0..input.totalCount) { "Packed count must be within the total count." }
        val findings = mutableListOf<ReadinessFinding>()

        findings += when {
            input.totalCount == 0 -> finding(
                TripReadySeverity.NEEDS_ATTENTION,
                TripReadySource.PACK,
                TripReadyReasonCode.PACKING_LIST_EMPTY,
            )
            input.packedCount < input.totalCount -> finding(
                TripReadySeverity.NEEDS_ATTENTION,
                TripReadySource.PACK,
                TripReadyReasonCode.PACKING_INCOMPLETE,
            )
            else -> finding(
                TripReadySeverity.READY,
                TripReadySource.PACK,
                TripReadyReasonCode.PACKING_COMPLETE,
            )
        }

        val start = input.tripStartEpochDay
        if (start == null) {
            findings += finding(
                TripReadySeverity.NEEDS_ATTENTION,
                TripReadySource.PACK,
                TripReadyReasonCode.TRIP_DATES_MISSING,
            )
            return TripReadiness(sort(findings))
        }
        val end = input.tripEndEpochDay?.coerceAtLeast(start) ?: start

        input.linkedTrackItems.forEach { record ->
            findings += recordFinding(
                record = record,
                source = TripReadySource.TRACK,
                start = start,
                end = end,
                before = TripReadyReasonCode.TRACK_EXPIRES_BEFORE_TRIP,
                during = TripReadyReasonCode.TRACK_EXPIRES_DURING_TRIP,
                valid = TripReadyReasonCode.TRACK_VALID_THROUGH_TRIP,
            )
        }
        input.linkedRenewals.forEach { record ->
            findings += recordFinding(
                record = record,
                source = TripReadySource.RENEW,
                start = start,
                end = end,
                before = TripReadyReasonCode.RENEWAL_DUE_BEFORE_TRIP,
                during = TripReadyReasonCode.RENEWAL_DUE_DURING_TRIP,
                valid = TripReadyReasonCode.RENEWAL_VALID_THROUGH_TRIP,
            )
        }
        return TripReadiness(sort(findings))
    }

    private fun recordFinding(
        record: TripReadyRecordInput,
        source: TripReadySource,
        start: Long,
        end: Long,
        before: TripReadyReasonCode,
        during: TripReadyReasonCode,
        valid: TripReadyReasonCode,
    ): ReadinessFinding = when {
        record.dateEpochDay < start -> ReadinessFinding(
            TripReadySeverity.CRITICAL, source, record.id, record.displayName, before, record.dateEpochDay,
        )
        record.dateEpochDay <= end -> ReadinessFinding(
            TripReadySeverity.CRITICAL, source, record.id, record.displayName, during, record.dateEpochDay,
        )
        else -> ReadinessFinding(
            TripReadySeverity.READY, source, record.id, record.displayName, valid, record.dateEpochDay,
        )
    }

    private fun finding(
        severity: TripReadySeverity,
        source: TripReadySource,
        reasonCode: TripReadyReasonCode,
    ) = ReadinessFinding(severity, source, null, "", reasonCode)

    private fun sort(findings: List<ReadinessFinding>): List<ReadinessFinding> = findings.sortedWith(
        compareBy<ReadinessFinding> { it.severity.ordinal }
            .thenBy { it.source.ordinal }
            .thenBy { it.displayName.lowercase(Locale.ROOT) }
            .thenBy { it.sourceId.orEmpty() },
    )
}
