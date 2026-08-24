package com.everycue.feature.renew

import java.time.LocalDate

const val DEFAULT_RENEW_WARNING_DAYS = 30

enum class RenewalType(val label: String, val emoji: String) {
    DOCUMENT("Document", "🪪"),
    INSURANCE("Insurance", "🛡️"),
    WARRANTY("Warranty", "🧾"),
    MEMBERSHIP("Membership", "🎟️"),
    SUBSCRIPTION("Subscription", "🔁"),
    CERTIFICATE("Certificate", "📜"),
    OTHER("Other", "📅"),
}

enum class DueState(val label: String) {
    UPCOMING("Upcoming"),
    DUE_SOON("Due soon"),
    DUE_TODAY("Due today"),
    OVERDUE("Overdue"),
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
)

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
) {
    val overdueCount: Int get() = renewals.count { it.dueState() == DueState.OVERDUE }
    val dueSoonCount: Int get() = renewals.count { it.dueState() in setOf(DueState.DUE_SOON, DueState.DUE_TODAY) }
    val upcomingCount: Int get() = renewals.count { it.dueState() == DueState.UPCOMING }
    val urgent: List<RenewalItem> get() = renewals.filter { it.dueState() != DueState.UPCOMING }.take(6)
}
