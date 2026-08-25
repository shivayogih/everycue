package com.everycue.feature.renew

import kotlinx.coroutines.flow.Flow
import com.everycue.core.attachments.LocalAttachment

/** Domain-facing persistence contract. Presentation depends on this abstraction, not Room. */
interface RenewStore {
    val renewals: Flow<List<RenewalItem>>
    val events: Flow<List<RenewalEvent>>
    val attachments: Flow<List<RenewalAttachment>>
    suspend fun save(draft: RenewalDraft, renewalId: String? = null): String
    suspend fun markRenewed(renewalId: String, newDueEpochDay: Long, notes: String = "")
    suspend fun delete(renewalId: String)
    suspend fun addAttachment(attachment: LocalAttachment)
    suspend fun removeAttachment(attachmentId: String)
    suspend fun clearAll()
}

