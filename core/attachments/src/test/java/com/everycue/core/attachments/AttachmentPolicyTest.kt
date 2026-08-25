package com.everycue.core.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPolicyTest {
    @Test
    fun `accepts phase one local document types`() {
        assertTrue(AttachmentPolicy.isSupportedMimeType("application/pdf"))
        assertTrue(AttachmentPolicy.isSupportedMimeType("IMAGE/JPEG; charset=binary"))
        assertFalse(AttachmentPolicy.isSupportedMimeType("application/x-executable"))
    }

    @Test
    fun `enforces nonempty bounded files`() {
        assertEquals(AttachmentError.EMPTY_FILE, AttachmentPolicy.validateSize(0))
        assertNull(AttachmentPolicy.validateSize(AttachmentPolicy.MAX_ATTACHMENT_BYTES))
        assertEquals(
            AttachmentError.FILE_TOO_LARGE,
            AttachmentPolicy.validateSize(AttachmentPolicy.MAX_ATTACHMENT_BYTES + 1),
        )
    }

    @Test
    fun `sanitizes imported display names`() {
        assertEquals("policy_2027.pdf", AttachmentPolicy.safeDisplayName("../policy:2027.pdf", "file.pdf"))
        assertEquals("file.pdf", AttachmentPolicy.safeDisplayName("  .. ", "file.pdf"))
    }
}
