package com.everycue.core.extraction

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RenewalTextExtractorTest {
    @Test
    fun `extracts renewal candidates as unconfirmed fields`() {
        val draft = RenewalTextExtractor.extract(
            """
            Document type: Vehicle insurance
            Provider: Example Assurance
            Policy number: AB-1234/56
            Effective date: 2026-09-01
            Valid until: 2027-08-31
            """.trimIndent(),
        )

        assertEquals("Vehicle insurance", draft.title?.value)
        assertEquals("INSURANCE", draft.typeName?.value)
        assertEquals("Example Assurance", draft.provider?.value)
        assertEquals("AB-1234/56", draft.referenceNumber?.value)
        assertEquals(LocalDate.of(2026, 9, 1), draft.startDate?.field?.value)
        assertEquals(LocalDate.of(2027, 8, 31), draft.dueDate?.field?.value)
        assertTrue(draft.dueDate?.field?.requiresConfirmation == true)
        assertTrue(draft.hasCandidates)
    }

    @Test
    fun `uses a document heading as a low confidence reviewable title`() {
        val draft = RenewalTextExtractor.extract(
            "PASSPORT RENEWAL\nDocument no: P-123456\nValid until: 2031-04-20",
        )

        assertEquals("PASSPORT RENEWAL", draft.title?.value)
        assertEquals("DOCUMENT", draft.typeName?.value)
        assertEquals("P-123456", draft.referenceNumber?.value)
        assertEquals(LocalDate.of(2031, 4, 20), draft.dueDate?.field?.value)
        assertTrue(draft.title?.requiresConfirmation == true)
    }

    @Test
    fun `returns no candidates for unrelated or blank OCR text`() {
        val draft = RenewalTextExtractor.extract("12345\n---")

        assertTrue(!draft.hasCandidates)
    }
}
