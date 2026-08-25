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

    @Test
    fun `extracts quantity unit category and storage as reviewable suggestions`() {
        val draft = SmartAddTextExtractor.extract(
            text = "Product name: Gentle Shampoo\nNet volume: 500 ml\nStorage location: Bathroom\nEXP 09/27",
            sourceType = ExtractionSourceType.CAMERA_OCR,
            token = 9,
        )

        assertEquals("Gentle Shampoo", draft.productName?.value)
        assertEquals(500.0, draft.quantity?.value)
        assertEquals("ml", draft.unit?.value)
        assertEquals("COSMETIC", draft.categoryName?.value)
        assertEquals("Bathroom", draft.storageLocation?.value)
        assertTrue(draft.quantity?.requiresConfirmation == true)
        assertTrue(draft.unit?.requiresConfirmation == true)
    }

    @Test
    fun `ignores nutrition metadata when selecting a fallback name`() {
        val draft = SmartAddTextExtractor.extract(
            text = "NUTRITION FACTS\nGreek Yogurt\nIngredients: milk cultures\nNet wt 400 g",
            sourceType = ExtractionSourceType.IMAGE_OCR,
            token = 11,
        )

        assertEquals("Greek Yogurt", draft.productName?.value)
        assertEquals(400.0, draft.quantity?.value)
        assertEquals("g", draft.unit?.value)
    }
}

