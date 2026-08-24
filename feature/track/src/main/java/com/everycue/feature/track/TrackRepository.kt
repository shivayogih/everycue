package com.everycue.feature.track

import androidx.room.withTransaction
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.database.TrackEventEntity
import com.everycue.core.database.TrackItemEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackRepository(
    private val database: EveryCueDatabase,
) : TrackStore {
    private val dao = database.trackDao()

    override val items: Flow<List<TrackItem>> = dao.observeActiveItems().map { entities -> entities.map(TrackItemEntity::toModel) }
    override val events: Flow<List<TrackEvent>> = dao.observeEvents().map { entities -> entities.map(TrackEventEntity::toModel) }

    override suspend fun save(draft: TrackDraft, itemId: String?): String {
        require(draft.name.isNotBlank()) { "Item name is required." }
        require(draft.quantity > 0) { "Quantity must be greater than zero." }
        require(draft.reminderDays >= 0) { "Reminder days cannot be negative." }

        val now = System.currentTimeMillis()
        val existing = if (itemId == null) null else dao.getItem(itemId)
        val id = existing?.id ?: UUID.randomUUID().toString()
        dao.upsertItem(
            TrackItemEntity(
                id = id,
                name = draft.name.trim(),
                category = draft.category.name,
                quantity = draft.quantity,
                unit = draft.unit.trim().ifBlank { "item" },
                purchaseEpochDay = draft.purchaseEpochDay,
                expiryEpochDay = draft.expiryEpochDay,
                storageLocation = draft.storageLocation.trim(),
                notes = draft.notes.trim(),
                reminderDays = draft.reminderDays,
                lifecycleStatus = "ACTIVE",
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            ),
        )
        return id
    }

    override suspend fun markOutcome(itemId: String, outcome: TrackOutcome, note: String) {
        database.withTransaction {
            val item = dao.getItem(itemId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            dao.upsertItem(
                item.copy(
                    lifecycleStatus = outcome.name,
                    updatedAtMillis = now,
                ),
            )
            dao.insertEvent(
                TrackEventEntity(
                    id = UUID.randomUUID().toString(),
                    itemId = item.id,
                    itemNameSnapshot = item.name,
                    outcome = outcome.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    timestampMillis = now,
                    notes = note.trim(),
                ),
            )
        }
    }

    override suspend fun delete(itemId: String) {
        dao.getItem(itemId)?.let { dao.deleteItem(it) }
    }

    override suspend fun clearAll() {
        database.withTransaction {
            dao.deleteAllEvents()
            dao.deleteAllItems()
        }
    }
}

private fun TrackItemEntity.toModel() = TrackItem(
    id = id,
    name = name,
    category = enumValueOrDefault(category, TrackCategory.OTHER),
    quantity = quantity,
    unit = unit,
    purchaseEpochDay = purchaseEpochDay,
    expiryEpochDay = expiryEpochDay,
    storageLocation = storageLocation,
    notes = notes,
    reminderDays = reminderDays,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun TrackEventEntity.toModel() = TrackEvent(
    id = id,
    itemId = itemId,
    itemName = itemNameSnapshot,
    outcome = enumValueOrDefault(outcome, TrackOutcome.DISCARDED),
    quantity = quantity,
    unit = unit,
    timestampMillis = timestampMillis,
    notes = notes,
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

