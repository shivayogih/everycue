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
        assertEquals("Example Assurance", draft.provider?.value)
        assertEquals("AB-1234/56", draft.referenceNumber?.value)
        assertEquals(LocalDate.of(2026, 9, 1), draft.startDate?.field?.value)
        assertEquals(LocalDate.of(2027, 8, 31), draft.dueDate?.field?.value)
        assertTrue(draft.dueDate?.field?.requiresConfirmation == true)
    }
}
