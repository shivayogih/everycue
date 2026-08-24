package com.everycue.app

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.everycue.core.database.EveryCueDatabase
import com.everycue.core.database.RenewalEntity
import com.everycue.core.database.RenewalEventEntity
import com.everycue.core.database.TrackEventEntity
import com.everycue.core.database.TrackItemEntity
import com.everycue.core.security.TextCipher
import com.everycue.feature.pack.PackData
import com.everycue.feature.pack.PackStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
)

@Serializable
data class TrackItemBackup(
    val id: String, val name: String, val category: String, val quantity: Double, val unit: String,
    val purchaseEpochDay: Long?, val expiryEpochDay: Long, val storageLocation: String, val notes: String,
    val reminderDays: Int, val lifecycleStatus: String, val createdAtMillis: Long, val updatedAtMillis: Long,
)

@Serializable
data class TrackEventBackup(
    val id: String, val itemId: String, val itemNameSnapshot: String, val outcome: String,
    val quantity: Double, val unit: String, val timestampMillis: Long, val notes: String,
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

const val CURRENT_BACKUP_VERSION = 1

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
) : BackupStore {
    private val resolver = context.applicationContext.contentResolver
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; prettyPrint = true }

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
        )
        resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json.encodeToString(payload)) }
            ?: error("Could not open the selected backup file.")
    }

    override suspend fun importFrom(uri: Uri) {
        val raw = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read the selected backup file.")
        val payload = json.decodeFromString<EveryCueBackup>(raw)
        require(payload.formatVersion in 1..CURRENT_BACKUP_VERSION) {
            "This backup format is newer than this EveryCue version supports."
        }
        require(payload.trackItems.distinctBy { it.id }.size == payload.trackItems.size) { "Backup contains duplicate Track IDs." }
        require(payload.renewals.distinctBy { it.id }.size == payload.renewals.size) { "Backup contains duplicate Renew IDs." }

        database.withTransaction {
            val trackDao = database.trackDao()
            val renewalDao = database.renewalDao()
            trackDao.deleteAllEvents()
            trackDao.deleteAllItems()
            renewalDao.deleteAllEvents()
            renewalDao.deleteAllRenewals()
            trackDao.upsertItems(payload.trackItems.map { it.toEntity(cipher) })
            trackDao.upsertEvents(payload.trackEvents.map { it.toEntity(cipher) })
            renewalDao.upsertRenewals(payload.renewals.map { it.toEntity(cipher) })
            renewalDao.upsertEvents(payload.renewalEvents.map { it.toEntity(cipher) })
        }
        packRepository.replaceAll(payload.pack)
        settingsRepository.replaceAll(payload.settings)
        payload.profile?.let { userProfileStore.replace(it) }
    }
}

private fun TrackItemEntity.toBackup(cipher: TextCipher) = TrackItemBackup(id, cipher.decrypt(name), category, quantity, cipher.decrypt(unit), purchaseEpochDay, expiryEpochDay, cipher.decrypt(storageLocation), cipher.decrypt(notes), reminderDays, lifecycleStatus, createdAtMillis, updatedAtMillis)
private fun TrackItemBackup.toEntity(cipher: TextCipher) = TrackItemEntity(id, cipher.encrypt(name), category, quantity, cipher.encrypt(unit), purchaseEpochDay, expiryEpochDay, cipher.encrypt(storageLocation), cipher.encrypt(notes), reminderDays, lifecycleStatus, createdAtMillis, updatedAtMillis)
private fun TrackEventEntity.toBackup(cipher: TextCipher) = TrackEventBackup(id, itemId, cipher.decrypt(itemNameSnapshot), outcome, quantity, cipher.decrypt(unit), timestampMillis, cipher.decrypt(notes))
private fun TrackEventBackup.toEntity(cipher: TextCipher) = TrackEventEntity(id, itemId, cipher.encrypt(itemNameSnapshot), outcome, quantity, cipher.encrypt(unit), timestampMillis, cipher.encrypt(notes))
private fun RenewalEntity.toBackup(cipher: TextCipher) = RenewalBackup(id, cipher.decrypt(title), type, dueEpochDay, reminderDays, cipher.decrypt(provider), cipher.decrypt(referenceNumber), cipher.decrypt(notes), lastRenewedEpochDay, lifecycleStatus, createdAtMillis, updatedAtMillis)
private fun RenewalBackup.toEntity(cipher: TextCipher) = RenewalEntity(id, cipher.encrypt(title), type, dueEpochDay, reminderDays, cipher.encrypt(provider), cipher.encrypt(referenceNumber), cipher.encrypt(notes), lastRenewedEpochDay, lifecycleStatus, createdAtMillis, updatedAtMillis)
private fun RenewalEventEntity.toBackup(cipher: TextCipher) = RenewalEventBackup(id, renewalId, cipher.decrypt(titleSnapshot), previousDueEpochDay, newDueEpochDay, renewedAtMillis, cipher.decrypt(notes))
private fun RenewalEventBackup.toEntity(cipher: TextCipher) = RenewalEventEntity(id, renewalId, cipher.encrypt(titleSnapshot), previousDueEpochDay, newDueEpochDay, renewedAtMillis, cipher.encrypt(notes))
