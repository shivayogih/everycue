package com.everycue.feature.renew

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenewValidationTest {
    @Test
    fun validRenewalPassesAllRules() {
        val draft = RenewalDraft("Vehicle insurance", RenewalType.INSURANCE, LocalDate.now().plusMonths(2).toEpochDay(), 14, "Example Insurance", "POL-123", "")
        assertTrue(draft.validationErrors().isValid)
        draft.validate()
    }

    @Test
    fun dummyAndInvalidNumericValuesAreRejected() {
        val draft = RenewalDraft("@@", RenewalType.OTHER, 0, -1, "!!!", "@@@", "x".repeat(501))
        val errors = draft.validationErrors()
        assertFalse(errors.isValid)
        assertTrue(errors.title != null)
        assertTrue(errors.reminderDays != null)
        assertTrue(errors.provider != null)
        assertTrue(errors.reference != null)
        assertTrue(errors.notes != null)
    }
}
