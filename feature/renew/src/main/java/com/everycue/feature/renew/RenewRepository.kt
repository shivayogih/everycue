package com.everycue.feature.renew

import androidx.room.withTransaction
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.database.RenewalEntity
import com.everycue.core.database.RenewalEventEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RenewRepository(
    private val database: EveryCueDatabase,
) {
    private val dao = database.renewalDao()

    val renewals: Flow<List<RenewalItem>> = dao.observeActiveRenewals().map { rows -> rows.map(RenewalEntity::toModel) }
    val events: Flow<List<RenewalEvent>> = dao.observeEvents().map { rows -> rows.map(RenewalEventEntity::toModel) }

    suspend fun save(draft: RenewalDraft, renewalId: String? = null): String {
        require(draft.title.isNotBlank()) { "Title is required." }
        require(draft.reminderDays >= 0) { "Reminder days cannot be negative." }

        val now = System.currentTimeMillis()
        val existing = if (renewalId == null) null else dao.getRenewal(renewalId)
        val id = existing?.id ?: UUID.randomUUID().toString()
        dao.upsertRenewal(
            RenewalEntity(
                id = id,
                title = draft.title.trim(),
                type = draft.type.name,
                dueEpochDay = draft.dueEpochDay,
                reminderDays = draft.reminderDays,
                provider = draft.provider.trim(),
                referenceNumber = draft.referenceNumber.trim(),
                notes = draft.notes.trim(),
                lastRenewedEpochDay = existing?.lastRenewedEpochDay,
                lifecycleStatus = "ACTIVE",
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            ),
        )
        return id
    }

    suspend fun markRenewed(renewalId: String, newDueEpochDay: Long, notes: String = "") {
        database.withTransaction {
            val existing = dao.getRenewal(renewalId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            val today = java.time.LocalDate.now().toEpochDay()
            dao.upsertRenewal(
                existing.copy(
                    dueEpochDay = newDueEpochDay,
                    lastRenewedEpochDay = today,
                    updatedAtMillis = now,
                ),
            )
            dao.insertEvent(
                RenewalEventEntity(
                    id = UUID.randomUUID().toString(),
                    renewalId = existing.id,
                    titleSnapshot = existing.title,
                    previousDueEpochDay = existing.dueEpochDay,
                    newDueEpochDay = newDueEpochDay,
                    renewedAtMillis = now,
                    notes = notes.trim(),
                ),
            )
        }
    }

    suspend fun delete(renewalId: String) {
        dao.getRenewal(renewalId)?.let { dao.deleteRenewal(it) }
    }

    suspend fun clearAll() {
        database.withTransaction {
            dao.deleteAllEvents()
            dao.deleteAllRenewals()
        }
    }
}

private fun RenewalEntity.toModel() = RenewalItem(
    id = id,
    title = title,
    type = runCatching { RenewalType.valueOf(type) }.getOrDefault(RenewalType.OTHER),
    dueEpochDay = dueEpochDay,
    reminderDays = reminderDays,
    provider = provider,
    referenceNumber = referenceNumber,
    notes = notes,
    lastRenewedEpochDay = lastRenewedEpochDay,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun RenewalEventEntity.toModel() = RenewalEvent(
    id = id,
    renewalId = renewalId,
    title = titleSnapshot,
    previousDueEpochDay = previousDueEpochDay,
    newDueEpochDay = newDueEpochDay,
    renewedAtMillis = renewedAtMillis,
    notes = notes,
)
