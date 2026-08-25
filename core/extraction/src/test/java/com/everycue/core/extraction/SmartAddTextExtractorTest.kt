package com.everycue.core.extraction

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAddTextExtractorTest {
    @Test
    fun `creates a reviewable draft and never confirms extracted expiry`() {
        val draft = SmartAddTextExtractor.extract(
            text = "Product name: Greek yogurt\nMFG 01/08/2026\nUse by 15/08/2026",
            sourceType = ExtractionSourceType.IMAGE_OCR,
            token = 7,
        )

        assertEquals("Greek yogurt", draft.productName?.value)
        assertEquals(LocalDate.of(2026, 8, 15), draft.expiryDate?.field?.value)
        assertTrue(draft.expiryDate?.field?.requiresConfirmation == true)
    }

    @Test
    fun `does not invent an expiry when only manufacturing date exists`() {
        val draft = SmartAddTextExtractor.extract("MFG 08/2026", ExtractionSourceType.CAMERA_OCR, 1)

        assertEquals(null, draft.expiryDate)
    }
}
