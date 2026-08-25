package com.everycue.feature.renew

import androidx.annotation.StringRes
import java.time.LocalDate

const val DEFAULT_RENEW_WARNING_DAYS = 30

enum class RenewalType(val emoji: String) {
    DOCUMENT("🪪"), INSURANCE("🛡️"), WARRANTY("🧾"), MEMBERSHIP("🎟️"), SUBSCRIPTION("🔁"), CERTIFICATE("📜"), OTHER("📅"),
}

enum class DueState {
    UPCOMING, DUE_SOON, DUE_TODAY, OVERDUE,
}

data class RenewalItem(
    val id: String,
    val title: String,
    val type: RenewalType,
    val dueEpochDay: Long,
    val reminderDays: Int,
    val provider: String,
    val referenceNumber: String,
    val notes: String,
    val lastRenewedEpochDay: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun daysRemaining(todayEpochDay: Long = LocalDate.now().toEpochDay()): Long = dueEpochDay - todayEpochDay

    fun dueState(todayEpochDay: Long = LocalDate.now().toEpochDay()): DueState {
        val remaining = daysRemaining(todayEpochDay)
        return when {
            remaining < 0 -> DueState.OVERDUE
            remaining == 0L -> DueState.DUE_TODAY
            remaining <= reminderDays -> DueState.DUE_SOON
            else -> DueState.UPCOMING
        }
    }
}

data class RenewalDraft(
    val title: String,
    val type: RenewalType,
    val dueEpochDay: Long,
    val reminderDays: Int,
    val provider: String,
    val referenceNumber: String,
    val notes: String,
) {
    fun validationErrors(): RenewalValidationErrors = RenewalValidationErrors(
        title = if (title.trim().length in 2..100 && title.any(Char::isLetterOrDigit)) null else R.string.error_renewal_title,
        reminderDays = if (reminderDays in 0..3650) null else R.string.error_reminder_days,
        provider = if (provider.length <= 100 && (provider.isBlank() || provider.any(Char::isLetterOrDigit))) null else R.string.error_provider,
        reference = if (referenceNumber.length <= 80 && (referenceNumber.isBlank() || referenceNumber.matches(Regex("[\\p{L}\\p{N}][\\p{L}\\p{N} ./_-]*")))) null else R.string.error_reference,
        notes = if (notes.length <= 500) null else R.string.error_notes_length,
    )

    fun validate() {
        validationErrors().firstError()?.let { throw RenewalValidationException(it) }
    }
}

data class RenewalValidationErrors(
    @StringRes val title: Int? = null,
    @StringRes val reminderDays: Int? = null,
    @StringRes val provider: Int? = null,
    @StringRes val reference: Int? = null,
    @StringRes val notes: Int? = null,
) {
    val isValid: Boolean get() = firstError() == null
    fun firstError(): Int? = title ?: reminderDays ?: provider ?: reference ?: notes
}

class RenewalValidationException(@StringRes val messageResource: Int) : IllegalArgumentException()

data class RenewalEvent(
    val id: String,
    val renewalId: String,
    val title: String,
    val previousDueEpochDay: Long,
    val newDueEpochDay: Long,
    val renewedAtMillis: Long,
    val notes: String,
)

data class RenewUiState(
    val renewals: List<RenewalItem> = emptyList(),
    val events: List<RenewalEvent> = emptyList(),
    val isBusy: Boolean = false,
) {
    val overdueCount: Int get() = renewals.count { it.dueState() == DueState.OVERDUE }
    val dueSoonCount: Int get() = renewals.count { it.dueState() in setOf(DueState.DUE_SOON, DueState.DUE_TODAY) }
    val upcomingCount: Int get() = renewals.count { it.dueState() == DueState.UPCOMING }
    val urgent: List<RenewalItem> get() = renewals.filter { it.dueState() != DueState.UPCOMING }.take(6)
}
