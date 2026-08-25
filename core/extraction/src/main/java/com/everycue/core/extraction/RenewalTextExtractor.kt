package com.everycue.core.extraction

/** Conservative field extraction for renewal documents. Results are always reviewable drafts. */
object RenewalTextExtractor {
    private val providerLabel = Regex(
        "(?im)^\\s*(?:provider|issuer|company|authority)\\s*[:#-]\\s*([^\\r\\n]{2,100})$",
    )
    private val titleLabel = Regex(
        "(?im)^\\s*(?:document|plan|policy|membership|certificate)\\s*(?:type|name|title)?\\s*[:#-]\\s*([^\\r\\n]{2,100})$",
    )
    private val referenceLabel = Regex(
        "(?im)^\\s*(?:reference|ref|policy|member|certificate|document)\\s*(?:number|no\\.?|id)?\\s*[:#-]\\s*([\\p{L}\\p{N}][\\p{L}\\p{N} ./_-]{1,79})$",
    )

    fun extract(text: String): RenewalExtractionDraft {
        val dates = DateTextExtractor.extract(text).dates
        return RenewalExtractionDraft(
            title = extractText(titleLabel, text),
            provider = extractText(providerLabel, text),
            referenceNumber = extractText(referenceLabel, text),
            startDate = dates.firstOrNull { it.kind == ExtractedDateKind.START },
            dueDate = dates.firstOrNull { it.kind in DUE_KINDS },
        )
    }

    private fun extractText(pattern: Regex, text: String): ExtractedField<String>? = pattern.find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { value ->
            ExtractedField(
                value = value,
                confidence = 0.9f,
                sourceText = pattern.find(text)?.value?.trim()?.take(160),
                requiresConfirmation = true,
            )
        }

    private val DUE_KINDS = setOf(
        ExtractedDateKind.DUE,
        ExtractedDateKind.EXPIRY,
        ExtractedDateKind.USE_BY,
        ExtractedDateKind.BEST_BEFORE,
    )
}
