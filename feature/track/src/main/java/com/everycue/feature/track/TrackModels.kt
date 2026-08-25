package com.everycue.feature.track

import androidx.annotation.StringRes
import com.everycue.core.extraction.SmartAddDraft
import com.everycue.core.recommendation.UseNextInput
import com.everycue.core.recommendation.UseNextRanker
import com.everycue.core.recommendation.UseNextScore
import com.everycue.core.recommendation.WasteCoach
import com.everycue.core.recommendation.WasteCoachExclusions
import com.everycue.core.recommendation.WasteCoachResult
import com.everycue.core.recommendation.WasteEventInput
import com.everycue.core.recommendation.WasteOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val DEFAULT_TRACK_WARNING_DAYS = 7

enum class TrackCategory(val emoji: String) {
    GROCERY("🥬"), MEDICINE("💊"), COSMETIC("🧴"), HOUSEHOLD("🏠"), SUPPLEMENT("🥤"), OTHER("📦"),
}

enum class TrackOutcome {
    CONSUMED, DISCARDED, DONATED,
}

enum class ExpiryState {
    FRESH, EXPIRING_SOON, EXPIRES_TODAY, EXPIRED,
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
    val barcode: String? = null,
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
    val category: TrackCategory = TrackCategory.OTHER,
)

enum class TrackCoachPreferenceType { HIDDEN_SUBJECT, HIDDEN_CATEGORY, DISMISSED_INSIGHT }

data class TrackCoachPreference(
    val key: String,
    val type: TrackCoachPreferenceType,
    val value: String,
    val createdAtMillis: Long,
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
    val barcode: String? = null,
) {
    fun validationErrors(): TrackValidationErrors = TrackValidationErrors(
        name = if (name.trim().length in 2..80 && name.any(Char::isLetterOrDigit)) null else R.string.error_product_name,
        quantity = if (quantity.isFinite() && quantity in 0.001..1_000_000.0) null else R.string.error_quantity,
        unit = if (unit.trim().matches(Regex("[\\p{L}][\\p{L}\\p{M} .]{0,19}"))) null else R.string.error_unit,
        dates = if (purchaseEpochDay == null || purchaseEpochDay <= expiryEpochDay) null else R.string.error_purchase_after_expiry,
        location = if (storageLocation.length <= 100 && (storageLocation.isBlank() || storageLocation.any(Char::isLetterOrDigit))) null else R.string.error_storage_location,
        notes = if (notes.length <= 500) null else R.string.error_notes_length,
        reminderDays = if (reminderDays in 0..3650) null else R.string.error_warning_days,
    )

    fun validate() {
        validationErrors().firstError()?.let { throw TrackValidationException(it) }
    }
}

data class TrackValidationErrors(
    @StringRes val name: Int? = null,
    @StringRes val quantity: Int? = null,
    @StringRes val unit: Int? = null,
    @StringRes val dates: Int? = null,
    @StringRes val location: Int? = null,
    @StringRes val notes: Int? = null,
    @StringRes val reminderDays: Int? = null,
) {
    val isValid: Boolean get() = firstError() == null
    fun firstError(): Int? = name ?: quantity ?: unit ?: dates ?: location ?: notes ?: reminderDays
}

class TrackValidationException(@StringRes val messageResource: Int) : IllegalArgumentException()

data class TrackUiState(
    val items: List<TrackItem> = emptyList(),
    val events: List<TrackEvent> = emptyList(),
    val isBusy: Boolean = false,
    val smartAddDraft: SmartAddDraft? = null,
    val coachPreferences: List<TrackCoachPreference> = emptyList(),
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
    val useNext: List<TrackUseNextEntry> by lazy(LazyThreadSafetyMode.NONE) {
        val today = LocalDate.now().toEpochDay()
        val byId = items.associateBy(TrackItem::id)
        UseNextRanker.rank(
            items = items.map { item ->
                UseNextInput(item.id, item.name, item.category.name, item.expiryEpochDay, item.reminderDays)
            },
            todayEpochDay = today,
        ).mapNotNull { score -> byId[score.itemId]?.let { TrackUseNextEntry(it, score) } }
    }
    val wasteCoach: WasteCoachResult by lazy(LazyThreadSafetyMode.NONE) {
            val hiddenSubjects = coachPreferences
                .filter { it.type == TrackCoachPreferenceType.HIDDEN_SUBJECT }
                .mapTo(mutableSetOf(), TrackCoachPreference::value)
            val hiddenCategories = coachPreferences
                .filter { it.type == TrackCoachPreferenceType.HIDDEN_CATEGORY }
                .mapTo(mutableSetOf(), TrackCoachPreference::value)
            val dismissed = coachPreferences
                .filter { it.type == TrackCoachPreferenceType.DISMISSED_INSIGHT }
                .mapTo(mutableSetOf(), TrackCoachPreference::value)
            WasteCoach.analyze(
                events = events.map { event ->
                    WasteEventInput(
                        subjectId = event.itemId,
                        subjectLabel = event.itemName,
                        category = event.category.name,
                        outcome = when (event.outcome) {
                            TrackOutcome.CONSUMED -> WasteOutcome.CONSUMED
                            TrackOutcome.DISCARDED -> WasteOutcome.DISCARDED
                            TrackOutcome.DONATED -> WasteOutcome.DONATED
                        },
                        occurredEpochDay = Instant.ofEpochMilli(event.timestampMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toEpochDay(),
                    )
                },
                todayEpochDay = LocalDate.now().toEpochDay(),
                exclusions = WasteCoachExclusions(hiddenSubjects, hiddenCategories, dismissed),
            )
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

data class TrackUseNextEntry(
    val item: TrackItem,
    val score: UseNextScore,
)
