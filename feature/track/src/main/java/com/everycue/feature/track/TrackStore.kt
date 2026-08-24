package com.everycue.feature.track

import kotlinx.coroutines.flow.Flow

/** Domain-facing persistence contract. Presentation depends on this abstraction, not Room. */
interface TrackStore {
    val items: Flow<List<TrackItem>>
    val events: Flow<List<TrackEvent>>
    suspend fun save(draft: TrackDraft, itemId: String? = null): String
    suspend fun markOutcome(itemId: String, outcome: TrackOutcome, note: String = "")
    suspend fun delete(itemId: String)
    suspend fun clearAll()
}

