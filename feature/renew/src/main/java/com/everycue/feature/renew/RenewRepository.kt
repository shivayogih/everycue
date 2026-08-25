package com.everycue.feature.renew

import androidx.room.withTransaction
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.database.RenewalEntity
import com.everycue.core.database.RenewalAttachmentEntity
import com.everycue.core.database.RenewalEventEntity
import com.everycue.core.attachments.AttachmentOwnerType
import com.everycue.core.attachments.AttachmentSource
import com.everycue.core.attachments.AttachmentStore
import com.everycue.core.attachments.LocalAttachment
import com.everycue.core.security.TextCipher
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RenewRepository(
    private val database: EveryCueDatabase,
    private val cipher: TextCipher,
    private val attachmentStore: AttachmentStore,
) : RenewStore {
    private val dao = database.renewalDao()

    override val renewals: Flow<List<RenewalItem>> = dao.observeActiveRenewals().map { rows -> rows.map { it.toModel(cipher) } }
    override val events: Flow<List<RenewalEvent>> = dao.observeEvents().map { rows -> rows.map { it.toModel(cipher) } }
    override val attachments: Flow<List<RenewalAttachment>> =
        dao.observeAttachments().map { rows -> rows.map { it.toModel(cipher) } }

    override suspend fun save(draft: RenewalDraft, renewalId: String?): String {
        draft.validate()

        val now = System.currentTimeMillis()
        val existing = if (renewalId == null) null else dao.getRenewal(renewalId)
        val id = existing?.id ?: UUID.randomUUID().toString()
        dao.upsertRenewal(
            RenewalEntity(
                id = id,
                title = cipher.encrypt(draft.title.trim()),
                type = draft.type.name,
                dueEpochDay = draft.dueEpochDay,
                reminderDays = draft.reminderDays,
                provider = cipher.encrypt(draft.provider.trim()),
                referenceNumber = cipher.encrypt(draft.referenceNumber.trim()),
                notes = cipher.encrypt(draft.notes.trim()),
                lastRenewedEpochDay = existing?.lastRenewedEpochDay,
                lifecycleStatus = "ACTIVE",
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            ),
        )
        return id
    }

    override suspend fun markRenewed(renewalId: String, newDueEpochDay: Long, notes: String) {
        require(newDueEpochDay > java.time.LocalDate.now().toEpochDay()) { "New due date must be in the future." }
        require(notes.length <= 500) { "Renewal notes are too long." }
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
                    notes = cipher.encrypt(notes.trim()),
                ),
            )
        }
    }

    override suspend fun delete(renewalId: String) {
        val attachments = dao.getAttachments(renewalId)
        attachments.forEach { attachmentStore.remove(it.toModel(cipher).asLocalAttachment()) }
        database.withTransaction {
            dao.deleteAttachmentsForRenewal(renewalId)
            dao.getRenewal(renewalId)?.let { dao.deleteRenewal(it) }
        }
    }

    override suspend fun addAttachment(attachment: LocalAttachment) {
        require(attachment.owner.type == AttachmentOwnerType.RENEWAL)
        val renewalId = attachment.owner.id
        try {
            database.withTransaction {
                require(dao.getRenewal(renewalId) != null)
                if (dao.getAttachments(renewalId).size >= MAX_RENEWAL_ATTACHMENTS) {
                    throw RenewalAttachmentLimitException()
                }
                dao.upsertAttachment(attachment.toEntity(cipher))
            }
        } catch (error: Throwable) {
            attachmentStore.remove(attachment)
            throw error
        }
    }

    override suspend fun removeAttachment(attachmentId: String) {
        val entity = dao.getAttachment(attachmentId) ?: return
        attachmentStore.remove(entity.toModel(cipher).asLocalAttachment())
        dao.deleteAttachment(attachmentId)
    }

    override suspend fun clearAll() {
        val attachments = dao.getAllAttachments()
        attachments.forEach { attachmentStore.remove(it.toModel(cipher).asLocalAttachment()) }
        database.withTransaction {
            dao.deleteAllAttachments()
            dao.deleteAllEvents()
            dao.deleteAllRenewals()
        }
    }
}

private fun LocalAttachment.toEntity(cipher: TextCipher) = RenewalAttachmentEntity(
    id = id,
    renewalId = owner.id,
    displayName = cipher.encrypt(displayName),
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localReference = cipher.encrypt(localReference),
    createdAtMillis = createdAtMillis,
    source = source.name,
)

private fun RenewalAttachmentEntity.toModel(cipher: TextCipher) = RenewalAttachment(
    id = id,
    renewalId = renewalId,
    displayName = cipher.decrypt(displayName),
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localReference = cipher.decrypt(localReference),
    createdAtMillis = createdAtMillis,
    source = runCatching { AttachmentSource.valueOf(source) }.getOrDefault(AttachmentSource.DOCUMENT),
)

private fun RenewalEntity.toModel(cipher: TextCipher) = RenewalItem(
    id = id,
    title = cipher.decrypt(title),
    type = runCatching { RenewalType.valueOf(type) }.getOrDefault(RenewalType.OTHER),
    dueEpochDay = dueEpochDay,
    reminderDays = reminderDays,
    provider = cipher.decrypt(provider),
    referenceNumber = cipher.decrypt(referenceNumber),
    notes = cipher.decrypt(notes),
    lastRenewedEpochDay = lastRenewedEpochDay,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun RenewalEventEntity.toModel(cipher: TextCipher) = RenewalEvent(
    id = id,
    renewalId = renewalId,
    title = cipher.decrypt(titleSnapshot),
    previousDueEpochDay = previousDueEpochDay,
    newDueEpochDay = newDueEpochDay,
    renewedAtMillis = renewedAtMillis,
    notes = cipher.decrypt(notes),
)

