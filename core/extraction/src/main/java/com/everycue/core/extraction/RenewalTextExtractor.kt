package com.everycue.core.extraction

/** Conservative field extraction for renewal documents. Results are always reviewable drafts. */
object RenewalTextExtractor {
    private val providerLabel = Regex(
        "(?im)^\\s*(?:provider|issuer|company|authority)\\s*(?:[:#-]\\s*|\\s{2,})([^\\r\\n]{2,100})$",
    )
    private val titleLabel = Regex(
        "(?im)^\\s*(?:document|plan|policy|membership|certificate)\\s*(?:type|name|title)?\\s*(?:[:#-]\\s*|\\s{2,})([^\\r\\n]{2,100})$",
    )
    private val referenceLabel = Regex(
        "(?im)^\\s*(?:reference|ref|policy|member|certificate|document)\\s*(?:number|no\\.?|id)?\\s*(?:[:#-]\\s*|\\s{2,})([\\p{L}\\p{N}][\\p{L}\\p{N} ./_-]{1,79})$",
    )
    private val ignoredTitleLine = Regex(
        "(?i)^(?:provider|issuer|company|authority|reference|ref|policy\\s*(?:number|no)|member\\s*(?:number|no|id)|certificate\\s*(?:number|no)|document\\s*(?:number|no)|effective|issued|start|due|expiry|expires|valid\\s+until)\\b",
    )

    fun extract(text: String): RenewalExtractionDraft {
        val dates = DateTextExtractor.extract(text).dates
        val labeledTitle = extractText(titleLabel, text)
        val fallbackTitle = text.lineSequence()
            .map(String::trim)
            .firstOrNull { line ->
                line.length in 2..100 && line.any(Char::isLetter) &&
                    !ignoredTitleLine.containsMatchIn(line) &&
                    DateTextExtractor.extract(line).dates.isEmpty()
            }
            ?.let { ExtractedField(it, 0.65f, it.take(160), true) }
        return RenewalExtractionDraft(
            typeName = inferType(text)?.let { ExtractedField(it, 0.75f, null, true) },
            title = labeledTitle ?: fallbackTitle,
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

    private fun inferType(text: String): String? {
        val normalized = text.lowercase()
        return when {
            listOf("insurance", "insurer", "policy").any(normalized::contains) -> "INSURANCE"
            listOf("warranty", "guarantee").any(normalized::contains) -> "WARRANTY"
            listOf("membership", "member id", "member no").any(normalized::contains) -> "MEMBERSHIP"
            listOf("subscription", "recurring plan").any(normalized::contains) -> "SUBSCRIPTION"
            listOf("certificate", "certification").any(normalized::contains) -> "CERTIFICATE"
            listOf("passport", "licence", "license", "document").any(normalized::contains) -> "DOCUMENT"
            else -> null
        }
    }
}
