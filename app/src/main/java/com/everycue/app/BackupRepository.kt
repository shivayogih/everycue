package com.everycue.app

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.room.withTransaction
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.database.RenewalAttachmentEntity
import com.everycue.core.database.RenewalEntity
import com.everycue.core.database.RenewalEventEntity
import com.everycue.core.database.TrackEventEntity
import com.everycue.core.database.TrackItemEntity
import com.everycue.core.database.TrackCoachPreferenceEntity
import com.everycue.core.attachments.AttachmentOwner
import com.everycue.core.attachments.AttachmentOwnerType
import com.everycue.core.attachments.AttachmentPolicy
import com.everycue.core.attachments.AttachmentRestoreRequest
import com.everycue.core.attachments.AttachmentSource
import com.everycue.core.attachments.AttachmentStore
import com.everycue.core.attachments.LocalAttachment
import com.everycue.core.security.TextCipher
import com.everycue.feature.pack.PackData
import com.everycue.feature.pack.PackStore
import com.everycue.feature.renew.RenewalDraft
import com.everycue.feature.renew.RenewalType
import com.everycue.feature.track.TrackCategory
import com.everycue.feature.track.TrackDraft
import com.everycue.feature.track.TrackCoachPreferenceType
import java.io.BufferedInputStream
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class EveryCueBackup(
    val formatVersion: Int = CURRENT_BACKUP_VERSION,
    val exportedAtMillis: Long,
    val trackItems: List<TrackItemBackup>,
    val trackEvents: List<TrackEventBackup>,
    val renewals: List<RenewalBackup>,
    val renewalEvents: List<RenewalEventBackup>,
    val pack: PackData,
    val settings: AppSettings,
    val profile: LocalProfile? = null,
    val trackCoachPreferences: List<TrackCoachPreferenceBackup> = emptyList(),
    val renewalAttachments: List<RenewalAttachmentBackup> = emptyList(),
)

@Serializable
data class TrackItemBackup(
    val id: String, val name: String, val category: String, val quantity: Double, val unit: String,
    val purchaseEpochDay: Long?, val expiryEpochDay: Long, val storageLocation: String, val notes: String,
    val reminderDays: Int, val lifecycleStatus: String, val createdAtMillis: Long, val updatedAtMillis: Long,
    val barcode: String? = null,
)

@Serializable
data class TrackEventBackup(
    val id: String, val itemId: String, val itemNameSnapshot: String, val outcome: String,
    val quantity: Double, val unit: String, val timestampMillis: Long, val notes: String,
    val categorySnapshot: String = TrackCategory.OTHER.name,
)

@Serializable
data class TrackCoachPreferenceBackup(
    val key: String,
    val type: String,
    val value: String,
    val createdAtMillis: Long,
)

@Serializable
data class RenewalBackup(
    val id: String, val title: String, val type: String, val dueEpochDay: Long, val reminderDays: Int,
    val provider: String, val referenceNumber: String, val notes: String, val lastRenewedEpochDay: Long?,
    val lifecycleStatus: String, val createdAtMillis: Long, val updatedAtMillis: Long,
)

@Serializable
data class RenewalEventBackup(
    val id: String, val renewalId: String, val titleSnapshot: String, val previousDueEpochDay: Long,
    val newDueEpochDay: Long, val renewedAtMillis: Long, val notes: String,
)

@Serializable
data class RenewalAttachmentBackup(
    val id: String,
    val renewalId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val source: String,
    val archiveEntry: String = "",
    val sha256: String = "",
)

const val ATTACHMENT_ARCHIVE_BACKUP_VERSION = 2
const val CURRENT_BACKUP_VERSION = ATTACHMENT_ARCHIVE_BACKUP_VERSION

class BackupException(@StringRes val messageResource: Int) : IllegalArgumentException()

interface BackupStore {
    suspend fun exportTo(uri: Uri)
    suspend fun importFrom(uri: Uri)
}

class BackupRepository(
    context: Context,
    private val database: EveryCueDatabase,
    private val packRepository: PackStore,
    private val settingsRepository: SettingsStore,
    private val userProfileStore: UserProfileStore,
    private val cipher: TextCipher,
    private val attachmentStore: AttachmentStore,
) : BackupStore {
    private val applicationContext = context.applicationContext
    private val resolver = applicationContext.contentResolver
    override suspend fun exportTo(uri: Uri) {
        val trackDao = database.trackDao()
        val renewalDao = database.renewalDao()
        val payload = EveryCueBackup(
            exportedAtMillis = System.currentTimeMillis(),
            trackItems = trackDao.getAllItems().map { it.toBackup(cipher) },
            trackEvents = trackDao.getAllEvents().map { it.toBackup(cipher) },
            renewals = renewalDao.getAllRenewals().map { it.toBackup(cipher) },
            renewalEvents = renewalDao.getAllEvents().map { it.toBackup(cipher) },
            pack = packRepository.snapshot(),
            settings = settingsRepository.snapshot(),
            profile = userProfileStore.snapshot(),
            trackCoachPreferences = trackDao.getCoachPreferences().map(TrackCoachPreferenceEntity::toBackup),
        )
        val attachmentSources = renewalDao.getAllAttachments().map { entity ->
            val attachment = entity.toLocalAttachment(cipher)
            BackupAttachmentSource(
                metadata = attachment.toBackup(),
                file = attachmentStore.contentFile(attachment),
            )
        }
        resolver.openOutputStream(uri, "w")?.use { output ->
            BackupArchiveIO.write(output, payload, attachmentSources)
        } ?: error("Could not open the selected backup file.")
    }

    override suspend fun importFrom(uri: Uri) {
        val stagingDirectory = File(
            applicationContext.cacheDir,
            "everycue_backup_restore/${UUID.randomUUID()}",
        )
        val archive = resolver.openInputStream(uri)?.use { source ->
            val input = BufferedInputStream(source)
            input.mark(4)
            val signature = ByteArray(4)
            val signatureSize = input.read(signature)
            input.reset()
            if (signatureSize >= 2 && signature[0] == 'P'.code.toByte() && signature[1] == 'K'.code.toByte()) {
                BackupArchiveIO.read(input, stagingDirectory)
            } else {
                BackupArchiveIO.readLegacyJson(input)
            }
        } ?: error("Could not read the selected backup file.")
        val payload = archive.payload
        require(payload.formatVersion in 1..CURRENT_BACKUP_VERSION) {
            "This backup format is newer than this EveryCue version supports."
        }
        require(payload.trackItems.distinctBy { it.id }.size == payload.trackItems.size) { "Backup contains duplicate Track IDs." }
        require(payload.renewals.distinctBy { it.id }.size == payload.renewals.size) { "Backup contains duplicate Renew IDs." }
        payload.validateUserData()

        val renewalDao = database.renewalDao()
        val currentAttachments = renewalDao.getAllAttachments().map { it.toLocalAttachment(cipher) }
        require(archive.isAttachmentArchive || payload.formatVersion < ATTACHMENT_ARCHIVE_BACKUP_VERSION) {
            "Attachment backup format requires an EveryCue archive."
        }
        val isAttachmentArchive = archive.isAttachmentArchive
        if (!isAttachmentArchive) {
            val restoredRenewalIds = payload.renewals.mapTo(hashSetOf(), RenewalBackup::id)
            if (currentAttachments.any { it.owner.id !in restoredRenewalIds }) {
                stagingDirectory.deleteRecursively()
                throw BackupException(R.string.backup_legacy_attachment_conflict)
            }
        }

        val restoredAttachments = mutableListOf<LocalAttachment>()
        try {
            if (isAttachmentArchive) {
                payload.renewalAttachments.forEach { metadata ->
                    val stagedFile = archive.stagedFilesByAttachmentId[metadata.id]
                        ?: error("Backup attachment staging file is missing.")
                    restoredAttachments += attachmentStore.restore(
                        request = AttachmentRestoreRequest(
                            id = metadata.id,
                            owner = AttachmentOwner(AttachmentOwnerType.RENEWAL, metadata.renewalId),
                            displayName = metadata.displayName,
                            mimeType = metadata.mimeType,
                            sizeBytes = metadata.sizeBytes,
                            createdAtMillis = metadata.createdAtMillis,
                            source = AttachmentSource.valueOf(metadata.source),
                        ),
                        input = stagedFile.inputStream(),
                    )
                }
            } else {
                restoredAttachments += currentAttachments
            }

            val previousPack = packRepository.snapshot()
            val previousSettings = settingsRepository.snapshot()
            val previousProfile = userProfileStore.snapshot()
            try {
                packRepository.replaceAll(payload.pack)
                settingsRepository.replaceAll(payload.settings)
                userProfileStore.replace(payload.profile)
                database.withTransaction {
                    val trackDao = database.trackDao()
                    trackDao.deleteAllEvents()
                    trackDao.deleteAllItems()
                    trackDao.deleteAllCoachPreferences()
                    renewalDao.deleteAllEvents()
                    renewalDao.deleteAllAttachments()
                    renewalDao.deleteAllRenewals()
                    trackDao.upsertItems(payload.trackItems.map { it.toEntity(cipher) })
                    trackDao.upsertEvents(payload.trackEvents.map { it.toEntity(cipher) })
                    trackDao.upsertCoachPreferences(payload.trackCoachPreferences.map(TrackCoachPreferenceBackup::toEntity))
                    renewalDao.upsertRenewals(payload.renewals.map { it.toEntity(cipher) })
                    renewalDao.upsertEvents(payload.renewalEvents.map { it.toEntity(cipher) })
                    renewalDao.upsertAttachments(restoredAttachments.map { it.toEntity(cipher) })
                }
            } catch (error: Throwable) {
                runCatching { packRepository.replaceAll(previousPack) }
                runCatching { settingsRepository.replaceAll(previousSettings) }
                runCatching { userProfileStore.replace(previousProfile) }
                throw error
            }

            if (isAttachmentArchive) {
                currentAttachments.forEach { attachment -> runCatching { attachmentStore.remove(attachment) } }
            }
        } catch (error: Throwable) {
            if (isAttachmentArchive) {
                restoredAttachments.forEach { attachment -> runCatching { attachmentStore.remove(attachment) } }
            }
            throw error
        } finally {
            stagingDirectory.deleteRecursively()
        }
    }
}

private fun EveryCueBackup.validateUserData() {
    require(
        trackItems.size <= 50_000 &&
            renewals.size <= 50_000 &&
            pack.trips.size <= 10_000 &&
            trackEvents.size <= 100_000 &&
            renewalEvents.size <= 100_000 &&
            trackCoachPreferences.size <= 100_000 &&
            renewalAttachments.size <= renewals.size * 20,
    ) {
        "Backup contains more records than EveryCue supports."
    }
    trackItems.forEach { item ->
        TrackDraft(
            name = item.name,
            category = runCatching { TrackCategory.valueOf(item.category) }.getOrElse { error("Invalid Track category.") },
            quantity = item.quantity,
            unit = item.unit,
            purchaseEpochDay = item.purchaseEpochDay,
            expiryEpochDay = item.expiryEpochDay,
            storageLocation = item.storageLocation,
            notes = item.notes,
            reminderDays = item.reminderDays,
            barcode = item.barcode,
        ).validate()
    }
    require(trackCoachPreferences.distinctBy { it.key }.size == trackCoachPreferences.size) {
        "Backup contains duplicate coaching preferences."
    }
    trackEvents.forEach { event ->
        require(event.id.isNotBlank() && event.itemId.isNotBlank() && event.itemNameSnapshot.length in 1..80)
        require(event.categorySnapshot in TrackCategory.entries.map(TrackCategory::name))
    }
    trackCoachPreferences.forEach { preference ->
        require(preference.key.length in 1..200 && preference.value.length in 1..200)
        require(preference.type in TrackCoachPreferenceType.entries.map(TrackCoachPreferenceType::name))
    }
    renewals.forEach { item ->
        RenewalDraft(
            title = item.title,
            type = runCatching { RenewalType.valueOf(item.type) }.getOrElse { error("Invalid renewal type.") },
            dueEpochDay = item.dueEpochDay,
            reminderDays = item.reminderDays,
            provider = item.provider,
            referenceNumber = item.referenceNumber,
            notes = item.notes,
        ).validate()
    }
    val renewalIds = renewals.mapTo(hashSetOf(), RenewalBackup::id)
    require(renewalAttachments.distinctBy { it.id }.size == renewalAttachments.size) {
        "Backup contains duplicate renewal attachment IDs."
    }
    renewalAttachments.forEach { attachment ->
        require(attachment.id.isNotBlank() && attachment.renewalId in renewalIds)
        require(attachment.displayName.length in 1..120 && attachment.displayName.none(Char::isISOControl))
        require(AttachmentPolicy.isSupportedMimeType(attachment.mimeType))
        require(AttachmentPolicy.validateSize(attachment.sizeBytes) == null)
        require(attachment.source in AttachmentSource.entries.map(AttachmentSource::name))
    }
    pack.validate()
    profile?.validate()
}

private fun TrackItemEntity.toBackup(cipher: TextCipher) = TrackItemBackup(id, cipher.decrypt(name), category, quantity, cipher.decrypt(unit), purchaseEpochDay, expiryEpochDay, cipher.decrypt(storageLocation), cipher.decrypt(notes), reminderDays, lifecycleStatus, createdAtMillis, updatedAtMillis, barcode?.let(cipher::decrypt))
private fun TrackItemBackup.toEntity(cipher: TextCipher) = TrackItemEntity(id, cipher.encrypt(name), category, quantity, cipher.encrypt(unit), purchaseEpochDay, expiryEpochDay, cipher.encrypt(storageLocation), cipher.encrypt(notes), reminderDays, lifecycleStatus, createdAtMillis, updatedAtMillis, barcode?.let(cipher::encrypt))
private fun TrackEventEntity.toBackup(cipher: TextCipher) = TrackEventBackup(id, itemId, cipher.decrypt(itemNameSnapshot), outcome, quantity, cipher.decrypt(unit), timestampMillis, cipher.decrypt(notes), categorySnapshot)
private fun TrackEventBackup.toEntity(cipher: TextCipher) = TrackEventEntity(id, itemId, cipher.encrypt(itemNameSnapshot), outcome, quantity, cipher.encrypt(unit), timestampMillis, cipher.encrypt(notes), categorySnapshot)
private fun TrackCoachPreferenceEntity.toBackup() = TrackCoachPreferenceBackup(key, type, value, createdAtMillis)
private fun TrackCoachPreferenceBackup.toEntity() = TrackCoachPreferenceEntity(key, type, value, createdAtMillis)
private fun RenewalEntity.toBackup(cipher: TextCipher) = RenewalBackup(id, cipher.decrypt(title), type, dueEpochDay, reminderDays, cipher.decrypt(provider), cipher.decrypt(referenceNumber), cipher.decrypt(notes), lastRenewedEpochDay, lifecycleStatus, createdAtMillis, updatedAtMillis)
private fun RenewalBackup.toEntity(cipher: TextCipher) = RenewalEntity(id, cipher.encrypt(title), type, dueEpochDay, reminderDays, cipher.encrypt(provider), cipher.encrypt(referenceNumber), cipher.encrypt(notes), lastRenewedEpochDay, lifecycleStatus, createdAtMillis, updatedAtMillis)
private fun RenewalEventEntity.toBackup(cipher: TextCipher) = RenewalEventBackup(id, renewalId, cipher.decrypt(titleSnapshot), previousDueEpochDay, newDueEpochDay, renewedAtMillis, cipher.decrypt(notes))
private fun RenewalEventBackup.toEntity(cipher: TextCipher) = RenewalEventEntity(id, renewalId, cipher.encrypt(titleSnapshot), previousDueEpochDay, newDueEpochDay, renewedAtMillis, cipher.encrypt(notes))
private fun RenewalAttachmentEntity.toLocalAttachment(cipher: TextCipher) = LocalAttachment(
    id = id,
    owner = AttachmentOwner(AttachmentOwnerType.RENEWAL, renewalId),
    displayName = cipher.decrypt(displayName),
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localReference = cipher.decrypt(localReference),
    createdAtMillis = createdAtMillis,
    source = runCatching { AttachmentSource.valueOf(source) }.getOrDefault(AttachmentSource.DOCUMENT),
)
private fun LocalAttachment.toBackup() = RenewalAttachmentBackup(
    id = id,
    renewalId = owner.id,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    createdAtMillis = createdAtMillis,
    source = source.name,
)
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
