package com.everycue.core.extraction

import java.time.LocalDate

enum class ExtractionSourceType {
    BARCODE,
    CAMERA_OCR,
    IMAGE_OCR,
    DOCUMENT_OCR,
    MANUAL,
}

/**
 * A value proposed by an extraction pipeline. Important values remain drafts until the user
 * confirms them; confidence is context for review and never permission to persist silently.
 */
data class ExtractedField<T>(
    val value: T,
    val confidence: Float,
    val sourceText: String?,
    val requiresConfirmation: Boolean = true,
) {
    init {
        require(confidence in 0f..1f)
    }

    fun confirmed(value: T = this.value): ExtractedField<T> = copy(
        value = value,
        requiresConfirmation = false,
    )
}

enum class ExtractedDateKind {
    EXPIRY,
    BEST_BEFORE,
    USE_BY,
    MANUFACTURED,
    START,
    DUE,
}

enum class DatePrecision {
    DAY,
    MONTH,
}

enum class DateNormalizationRule {
    EXACT_DATE,
    END_OF_MONTH,
    START_OF_MONTH,
}

data class ExtractedDate(
    val kind: ExtractedDateKind,
    val field: ExtractedField<LocalDate>,
    val precision: DatePrecision,
    val normalizationRule: DateNormalizationRule,
)

data class DateExtractionResult(
    val dates: List<ExtractedDate>,
) {
    val expiryCandidates: List<ExtractedDate>
        get() = dates.filter { it.kind in EXPIRY_KINDS }

    companion object {
        private val EXPIRY_KINDS = setOf(
            ExtractedDateKind.EXPIRY,
            ExtractedDateKind.BEST_BEFORE,
            ExtractedDateKind.USE_BY,
            ExtractedDateKind.DUE,
        )
    }
}

data class RenewalExtractionDraft(
    val title: ExtractedField<String>? = null,
    val provider: ExtractedField<String>? = null,
    val referenceNumber: ExtractedField<String>? = null,
    val startDate: ExtractedDate? = null,
    val dueDate: ExtractedDate? = null,
)
