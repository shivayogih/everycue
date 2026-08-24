package com.everycue.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_items",
    indices = [
        Index("expiryEpochDay"),
        Index("lifecycleStatus"),
        Index("category"),
    ],
)
data class TrackItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val purchaseEpochDay: Long?,
    val expiryEpochDay: Long,
    val storageLocation: String,
    val notes: String,
    val reminderDays: Int,
    val lifecycleStatus: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "track_events",
    indices = [Index("itemId"), Index("timestampMillis"), Index("outcome")],
)
data class TrackEventEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val itemNameSnapshot: String,
    val outcome: String,
    val quantity: Double,
    val unit: String,
    val timestampMillis: Long,
    val notes: String,
)

@Entity(
    tableName = "renewals",
    indices = [Index("dueEpochDay"), Index("lifecycleStatus"), Index("type")],
)
data class RenewalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val dueEpochDay: Long,
    val reminderDays: Int,
    val provider: String,
    val referenceNumber: String,
    val notes: String,
    val lastRenewedEpochDay: Long?,
    val lifecycleStatus: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "renewal_events",
    indices = [Index("renewalId"), Index("renewedAtMillis")],
)
data class RenewalEventEntity(
    @PrimaryKey val id: String,
    val renewalId: String,
    val titleSnapshot: String,
    val previousDueEpochDay: Long,
    val newDueEpochDay: Long,
    val renewedAtMillis: Long,
    val notes: String,
)

