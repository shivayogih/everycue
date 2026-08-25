package com.everycue.core.recommendation

import java.util.Locale

enum class UseNextReasonCode {
    EXPIRES_TODAY,
    EXPIRING_SOON,
    EXPIRY_UPCOMING,
}

data class UseNextInput(
    val itemId: String,
    val displayName: String,
    val category: String,
    val expiryEpochDay: Long,
    val warningDays: Int,
)

data class UseNextScore(
    val itemId: String,
    val score: Int,
    val reasonCodes: List<UseNextReasonCode>,
    val evaluatedEpochDay: Long,
)

object UseNextRanker {
    fun rank(items: List<UseNextInput>, todayEpochDay: Long): List<UseNextScore> = items.asSequence()
        .mapNotNull { item -> score(item, todayEpochDay) }
        .sortedWith(
            compareByDescending<Ranked> { it.result.score }
                .thenBy { it.expiryEpochDay }
                .thenBy { it.displayName.lowercase(Locale.ROOT) }
                .thenBy { it.result.itemId },
        )
        .map(Ranked::result)
        .toList()

    private fun score(item: UseNextInput, today: Long): Ranked? {
        val remaining = item.expiryEpochDay - today
        if (remaining < 0) return null
        val reason = when {
            remaining == 0L -> UseNextReasonCode.EXPIRES_TODAY
            remaining <= item.warningDays -> UseNextReasonCode.EXPIRING_SOON
            else -> UseNextReasonCode.EXPIRY_UPCOMING
        }
        val score = when (reason) {
            UseNextReasonCode.EXPIRES_TODAY -> 10_000
            UseNextReasonCode.EXPIRING_SOON -> 8_000 - remaining.coerceAtMost(3_000).toInt()
            UseNextReasonCode.EXPIRY_UPCOMING -> 4_000 - remaining.coerceAtMost(3_000).toInt()
        }
        return Ranked(
            result = UseNextScore(item.itemId, score, listOf(reason), today),
            expiryEpochDay = item.expiryEpochDay,
            displayName = item.displayName,
        )
    }

    private data class Ranked(
        val result: UseNextScore,
        val expiryEpochDay: Long,
        val displayName: String,
    )
}
