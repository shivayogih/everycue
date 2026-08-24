package com.everycue.feature.renew

import kotlinx.coroutines.flow.Flow

/** Domain-facing persistence contract. Presentation depends on this abstraction, not Room. */
interface RenewStore {
    val renewals: Flow<List<RenewalItem>>
    val events: Flow<List<RenewalEvent>>
    suspend fun save(draft: RenewalDraft, renewalId: String? = null): String
    suspend fun markRenewed(renewalId: String, newDueEpochDay: Long, notes: String = "")
    suspend fun delete(renewalId: String)
    suspend fun clearAll()
}

