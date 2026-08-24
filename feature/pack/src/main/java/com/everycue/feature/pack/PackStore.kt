package com.everycue.feature.pack

import kotlinx.coroutines.flow.Flow

/** Domain-facing persistence contract. Presentation depends on this abstraction, not DataStore. */
interface PackStore {
    val data: Flow<PackData>
    suspend fun createTrip(draft: TripDraft): Long
    suspend fun updateTrip(tripId: Long, draft: TripDraft)
    suspend fun addItem(tripId: Long, name: String, category: PackingCategory, quantity: Int)
    suspend fun setPacked(tripId: Long, itemId: Long, packed: Boolean)
    suspend fun deleteItem(tripId: Long, itemId: Long)
    suspend fun moveItem(tripId: Long, itemId: Long, offset: Int)
    suspend fun unpackAll(tripId: Long)
    suspend fun deleteTrip(tripId: Long)
    suspend fun addDemoTrip(): Long
    suspend fun resetAll()
    suspend fun snapshot(): PackData
    suspend fun replaceAll(restored: PackData)
}

