package com.everycue.core.extraction

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTextExtractorTest {
    @Test
    fun `extracts multiple labeled exact dates without auto confirmation`() {
        val result = DateTextExtractor.extract("MFG 12/08/2026   EXP 11/08/2027")

        assertEquals(2, result.dates.size)
        assertEquals(LocalDate.of(2026, 8, 12), result.dates[0].field.value)
        assertEquals(ExtractedDateKind.MANUFACTURED, result.dates[0].kind)
        assertEquals(LocalDate.of(2027, 8, 11), result.dates[1].field.value)
        assertEquals(ExtractedDateKind.EXPIRY, result.dates[1].kind)
        assertTrue(result.dates.all { it.field.requiresConfirmation })
    }

    @Test
    fun `normalizes expiry month to end and manufacturing month to start`() {
        val result = DateTextExtractor.extract("MFG 02/2026\nBest before 02/2028")

        assertEquals(LocalDate.of(2026, 2, 1), result.dates[0].field.value)
        assertEquals(DateNormalizationRule.START_OF_MONTH, result.dates[0].normalizationRule)
        assertEquals(LocalDate.of(2028, 2, 29), result.dates[1].field.value)
        assertEquals(DateNormalizationRule.END_OF_MONTH, result.dates[1].normalizationRule)
    }

    @Test
    fun `rejects invalid calendar dates`() {
        val result = DateTextExtractor.extract("EXP 31/02/2027")

        assertTrue(result.dates.isEmpty())
    }

    @Test
    fun `does not infer an unlabeled or manufactured date as expiry`() {
        val result = DateTextExtractor.extract("Batch 2026-08-12\nManufactured 12 Aug 2026")

        assertTrue(result.expiryCandidates.isEmpty())
        assertEquals(listOf(ExtractedDateKind.MANUFACTURED), result.dates.map { it.kind })
    }
}
