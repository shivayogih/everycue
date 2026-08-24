package com.everycue.feature.track

import java.time.LocalDate

const val DEFAULT_TRACK_WARNING_DAYS = 7

enum class TrackCategory(val label: String, val emoji: String) {
    GROCERY("Grocery", "🥬"),
    MEDICINE("Medicine", "💊"),
    COSMETIC("Cosmetic", "🧴"),
    HOUSEHOLD("Household", "🏠"),
    SUPPLEMENT("Supplement", "🥤"),
    OTHER("Other", "📦"),
}

enum class TrackOutcome(val label: String) {
    CONSUMED("Consumed"),
    DISCARDED("Discarded"),
    DONATED("Donated"),
}

enum class ExpiryState(val label: String) {
    FRESH("Fresh"),
    EXPIRING_SOON("Expiring soon"),
    EXPIRES_TODAY("Expires today"),
    EXPIRED("Expired"),
}

data class TrackItem(
    val id: String,
    val name: String,
    val category: TrackCategory,
    val quantity: Double,
    val unit: String,
    val purchaseEpochDay: Long?,
    val expiryEpochDay: Long,
    val storageLocation: String,
    val notes: String,
    val reminderDays: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun daysRemaining(todayEpochDay: Long = LocalDate.now().toEpochDay()): Long = expiryEpochDay - todayEpochDay

    fun expiryState(todayEpochDay: Long = LocalDate.now().toEpochDay()): ExpiryState {
        val remaining = daysRemaining(todayEpochDay)
        return when {
            remaining < 0 -> ExpiryState.EXPIRED
            remaining == 0L -> ExpiryState.EXPIRES_TODAY
            remaining <= reminderDays -> ExpiryState.EXPIRING_SOON
            else -> ExpiryState.FRESH
        }
    }
}

data class TrackEvent(
    val id: String,
    val itemId: String,
    val itemName: String,
    val outcome: TrackOutcome,
    val quantity: Double,
    val unit: String,
    val timestampMillis: Long,
    val notes: String,
)

data class TrackDraft(
    val name: String,
    val category: TrackCategory,
    val quantity: Double,
    val unit: String,
    val purchaseEpochDay: Long?,
    val expiryEpochDay: Long,
    val storageLocation: String,
    val notes: String,
    val reminderDays: Int,
)

data class TrackUiState(
    val items: List<TrackItem> = emptyList(),
    val events: List<TrackEvent> = emptyList(),
) {
    val freshCount: Int
        get() {
            val today = LocalDate.now().toEpochDay()
            return items.count { it.expiryState(today) == ExpiryState.FRESH }
        }
    val expiringSoonCount: Int
        get() {
            val today = LocalDate.now().toEpochDay()
            return items.count { it.expiryState(today) in setOf(ExpiryState.EXPIRING_SOON, ExpiryState.EXPIRES_TODAY) }
        }
    val expiredCount: Int
        get() {
            val today = LocalDate.now().toEpochDay()
            return items.count { it.expiryState(today) == ExpiryState.EXPIRED }
        }
    val urgentItems: List<TrackItem>
        get() {
            val today = LocalDate.now().toEpochDay()
            return items.filter { it.expiryState(today) != ExpiryState.FRESH }.take(6)
        }
    val consumedCount: Int get() = events.count { it.outcome == TrackOutcome.CONSUMED }
    val discardedCount: Int get() = events.count { it.outcome == TrackOutcome.DISCARDED }
    val donatedCount: Int get() = events.count { it.outcome == TrackOutcome.DONATED }
    val usageEfficiency: Int
        get() {
            val decided = consumedCount + discardedCount
            return if (decided == 0) 0 else (consumedCount * 100f / decided).toInt()
        }
}
