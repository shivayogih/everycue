package com.everycue.feature.track

import androidx.room.withTransaction
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.database.TrackEventEntity
import com.everycue.core.database.TrackItemEntity
import com.everycue.core.security.TextCipher
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackRepository(
    private val database: EveryCueDatabase,
    private val cipher: TextCipher,
) : TrackStore {
    private val dao = database.trackDao()

    override val items: Flow<List<TrackItem>> = dao.observeActiveItems().map { entities -> entities.map { it.toModel(cipher) } }
    override val events: Flow<List<TrackEvent>> = dao.observeEvents().map { entities -> entities.map { it.toModel(cipher) } }

    override suspend fun save(draft: TrackDraft, itemId: String?): String {
        require(draft.name.isNotBlank()) { "Item name is required." }
        require(draft.unit.isNotBlank()) { "Unit is required." }
        require(draft.quantity > 0) { "Quantity must be greater than zero." }
        require(draft.reminderDays >= 0) { "Reminder days cannot be negative." }

        val now = System.currentTimeMillis()
        val existing = if (itemId == null) null else dao.getItem(itemId)
        val id = existing?.id ?: UUID.randomUUID().toString()
        dao.upsertItem(
            TrackItemEntity(
                id = id,
                name = cipher.encrypt(draft.name.trim()),
                category = draft.category.name,
                quantity = draft.quantity,
                unit = cipher.encrypt(draft.unit.trim()),
                purchaseEpochDay = draft.purchaseEpochDay,
                expiryEpochDay = draft.expiryEpochDay,
                storageLocation = cipher.encrypt(draft.storageLocation.trim()),
                notes = cipher.encrypt(draft.notes.trim()),
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
                    notes = cipher.encrypt(note.trim()),
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

private fun TrackItemEntity.toModel(cipher: TextCipher) = TrackItem(
    id = id,
    name = cipher.decrypt(name),
    category = enumValueOrDefault(category, TrackCategory.OTHER),
    quantity = quantity,
    unit = cipher.decrypt(unit),
    purchaseEpochDay = purchaseEpochDay,
    expiryEpochDay = expiryEpochDay,
    storageLocation = cipher.decrypt(storageLocation),
    notes = cipher.decrypt(notes),
    reminderDays = reminderDays,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun TrackEventEntity.toModel(cipher: TextCipher) = TrackEvent(
    id = id,
    itemId = itemId,
    itemName = cipher.decrypt(itemNameSnapshot),
    outcome = enumValueOrDefault(outcome, TrackOutcome.DISCARDED),
    quantity = quantity,
    unit = cipher.decrypt(unit),
    timestampMillis = timestampMillis,
    notes = cipher.decrypt(notes),
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
