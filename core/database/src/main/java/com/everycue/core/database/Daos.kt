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

    @Upsert
    suspend fun upsertItem(item: TrackItemEntity)

    @Delete
    suspend fun deleteItem(item: TrackItemEntity)

    @Insert
    suspend fun insertEvent(event: TrackEventEntity)

    @Query("SELECT * FROM track_events ORDER BY timestampMillis DESC")
    fun observeEvents(): Flow<List<TrackEventEntity>>

    @Query("DELETE FROM track_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM track_items")
    suspend fun deleteAllItems()
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

    @Upsert
    suspend fun upsertRenewal(renewal: RenewalEntity)

    @Delete
    suspend fun deleteRenewal(renewal: RenewalEntity)

    @Insert
    suspend fun insertEvent(event: RenewalEventEntity)

    @Query("SELECT * FROM renewal_events ORDER BY renewedAtMillis DESC")
    fun observeEvents(): Flow<List<RenewalEventEntity>>

    @Query("DELETE FROM renewal_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM renewals")
    suspend fun deleteAllRenewals()
}
