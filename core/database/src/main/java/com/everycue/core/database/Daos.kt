package com.everycue.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query(
        """
        SELECT * FROM track_items
        WHERE lifecycleStatus = 'ACTIVE'
        ORDER BY expiryEpochDay ASC, name COLLATE NOCASE ASC
        """,
    )
    fun observeActiveItems(): Flow<List<TrackItemEntity>>

    @Query("SELECT * FROM track_items WHERE id = :id LIMIT 1")
    fun observeItem(id: String): Flow<TrackItemEntity?>

    @Query("SELECT * FROM track_items WHERE id = :id LIMIT 1")
    suspend fun getItem(id: String): TrackItemEntity?

    @Query("SELECT * FROM track_items ORDER BY createdAtMillis ASC")
    suspend fun getAllItems(): List<TrackItemEntity>

    @Query("SELECT * FROM track_events ORDER BY timestampMillis ASC")
    suspend fun getAllEvents(): List<TrackEventEntity>

    @Upsert
    suspend fun upsertItem(item: TrackItemEntity)

    @Upsert
    suspend fun upsertItems(items: List<TrackItemEntity>)

    @Delete
    suspend fun deleteItem(item: TrackItemEntity)

    @Insert
    suspend fun insertEvent(event: TrackEventEntity)

    @Upsert
    suspend fun upsertEvents(events: List<TrackEventEntity>)

    @Query("SELECT * FROM track_events ORDER BY timestampMillis DESC")
    fun observeEvents(): Flow<List<TrackEventEntity>>

    @Query("SELECT * FROM track_coach_preferences ORDER BY createdAtMillis ASC")
    fun observeCoachPreferences(): Flow<List<TrackCoachPreferenceEntity>>

    @Query("SELECT * FROM track_coach_preferences ORDER BY createdAtMillis ASC")
    suspend fun getCoachPreferences(): List<TrackCoachPreferenceEntity>

    @Upsert
    suspend fun upsertCoachPreference(preference: TrackCoachPreferenceEntity)

    @Upsert
    suspend fun upsertCoachPreferences(preferences: List<TrackCoachPreferenceEntity>)

    @Query("DELETE FROM track_coach_preferences WHERE key = :key")
    suspend fun deleteCoachPreference(key: String)

    @Query("DELETE FROM track_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM track_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM track_coach_preferences")
    suspend fun deleteAllCoachPreferences()
}

@Dao
interface RenewalDao {
    @Query(
        """
        SELECT * FROM renewals
        WHERE lifecycleStatus = 'ACTIVE'
        ORDER BY dueEpochDay ASC, title COLLATE NOCASE ASC
        """,
    )
    fun observeActiveRenewals(): Flow<List<RenewalEntity>>

    @Query("SELECT * FROM renewals WHERE id = :id LIMIT 1")
    fun observeRenewal(id: String): Flow<RenewalEntity?>

    @Query("SELECT * FROM renewals WHERE id = :id LIMIT 1")
    suspend fun getRenewal(id: String): RenewalEntity?

    @Query("SELECT * FROM renewals ORDER BY createdAtMillis ASC")
    suspend fun getAllRenewals(): List<RenewalEntity>

    @Query("SELECT * FROM renewal_events ORDER BY renewedAtMillis ASC")
    suspend fun getAllEvents(): List<RenewalEventEntity>

    @Upsert
    suspend fun upsertRenewal(renewal: RenewalEntity)

    @Upsert
    suspend fun upsertRenewals(renewals: List<RenewalEntity>)

    @Delete
    suspend fun deleteRenewal(renewal: RenewalEntity)

    @Insert
    suspend fun insertEvent(event: RenewalEventEntity)

    @Upsert
    suspend fun upsertEvents(events: List<RenewalEventEntity>)

    @Query("SELECT * FROM renewal_events ORDER BY renewedAtMillis DESC")
    fun observeEvents(): Flow<List<RenewalEventEntity>>

    @Query("SELECT * FROM renewal_attachments ORDER BY createdAtMillis DESC")
    fun observeAttachments(): Flow<List<RenewalAttachmentEntity>>

    @Query("SELECT * FROM renewal_attachments WHERE renewalId = :renewalId ORDER BY createdAtMillis DESC")
    suspend fun getAttachments(renewalId: String): List<RenewalAttachmentEntity>

    @Query("SELECT * FROM renewal_attachments ORDER BY createdAtMillis ASC")
    suspend fun getAllAttachments(): List<RenewalAttachmentEntity>

    @Query("SELECT * FROM renewal_attachments WHERE id = :attachmentId LIMIT 1")
    suspend fun getAttachment(attachmentId: String): RenewalAttachmentEntity?

    @Upsert
    suspend fun upsertAttachment(attachment: RenewalAttachmentEntity)

    @Query("DELETE FROM renewal_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    @Query("DELETE FROM renewal_attachments WHERE renewalId = :renewalId")
    suspend fun deleteAttachmentsForRenewal(renewalId: String)

    @Query("DELETE FROM renewal_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM renewals")
    suspend fun deleteAllRenewals()

    @Query("DELETE FROM renewal_attachments")
    suspend fun deleteAllAttachments()
}

