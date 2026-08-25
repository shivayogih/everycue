package com.everycue.feature.renew

import com.everycue.core.attachments.AttachmentOwnerType
import com.everycue.core.attachments.AttachmentSource
import org.junit.Assert.assertEquals
import org.junit.Test

class RenewAttachmentTest {
    @Test
    fun `attachment converts only to its renewal owner`() {
        val attachment = attachment(id = "file-1", renewalId = "renewal-1")

        val stored = attachment.asLocalAttachment()

        assertEquals(AttachmentOwnerType.RENEWAL, stored.owner.type)
        assertEquals("renewal-1", stored.owner.id)
        assertEquals("renewal/renewal-1/file-1.pdf", stored.localReference)
        assertEquals(AttachmentSource.DOCUMENT, stored.source)
    }

    @Test
    fun `ui state returns attachments for requested renewal only`() {
        val state = RenewUiState(
            attachments = listOf(
                attachment(id = "a", renewalId = "first"),
                attachment(id = "b", renewalId = "second"),
                attachment(id = "c", renewalId = "first"),
            ),
        )

        assertEquals(listOf("a", "c"), state.attachmentsFor("first").map(RenewalAttachment::id))
    }

    private fun attachment(id: String, renewalId: String) = RenewalAttachment(
        id = id,
        renewalId = renewalId,
        displayName = "$id.pdf",
        mimeType = "application/pdf",
        sizeBytes = 128,
        localReference = "renewal/$renewalId/$id.pdf",
        createdAtMillis = 1,
        source = AttachmentSource.DOCUMENT,
    )
}

