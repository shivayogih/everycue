package com.everycue.core.extraction

object SmartAddTextExtractor {
    private val productLabel = Regex(
        "(?im)^\\s*(?:product|item)(?:\\s+name)?\\s*[:#-]\\s*([^\\r\\n]{2,80})$",
    )
    private val ignoredLine = Regex(
        "(?i)^(?:ingredients|nutrition|manufactured|mfg|mfd|exp|expiry|best before|use by|batch|lot)\\b",
    )

    fun extract(text: String, sourceType: ExtractionSourceType, token: Long): SmartAddDraft {
        val dates = DateTextExtractor.extract(text).dates
        val labeledName = productLabel.find(text)?.groupValues?.getOrNull(1)?.trim()
        val fallbackName = text.lineSequence()
            .map(String::trim)
            .filter { it.length in 2..80 && it.any(Char::isLetter) && !ignoredLine.containsMatchIn(it) }
            .firstOrNull()
        val name = labeledName ?: fallbackName
        return SmartAddDraft(
            token = token,
            sourceType = sourceType,
            productName = name?.let {
                ExtractedField(
                    value = it,
                    confidence = if (labeledName != null) 0.92f else 0.58f,
                    sourceText = it.take(160),
                    requiresConfirmation = true,
                )
            },
            expiryDate = dates.firstOrNull { it.kind in EXPIRY_KINDS },
        )
    }

    fun fromBarcode(
        barcode: String,
        token: Long,
        knownName: String? = null,
        knownCategoryName: String? = null,
        knownLocation: String? = null,
        duplicateIds: List<String> = emptyList(),
    ) = SmartAddDraft(
        token = token,
        sourceType = ExtractionSourceType.BARCODE,
        productName = knownName?.let { ExtractedField(it, 0.99f, null, true) },
        categoryName = knownCategoryName?.let { ExtractedField(it, 0.99f, null, true) },
        storageLocation = knownLocation?.let { ExtractedField(it, 0.99f, null, true) },
        barcode = ExtractedField(barcode, 1f, null, false),
        possibleDuplicateIds = duplicateIds,
    )

    private val EXPIRY_KINDS = setOf(
        ExtractedDateKind.EXPIRY,
        ExtractedDateKind.BEST_BEFORE,
        ExtractedDateKind.USE_BY,
    )
}
