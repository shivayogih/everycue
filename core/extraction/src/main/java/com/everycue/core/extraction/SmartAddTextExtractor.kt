package com.everycue.core.extraction

object SmartAddTextExtractor {
    private val productLabel = Regex(
        "(?im)^\\s*(?:product|item)(?:\\s+name)?\\s*(?:[:#-]\\s*|\\s+)([^\\r\\n]{2,80})$",
    )
    private val ignoredLine = Regex(
        "(?i)^(?:ingredients?|nutrition(?:al)?(?:\\s+facts)?|manufactured|mfg|mfd|exp|expiry|best before|use by|batch|lot|mrp|price|packed|net\\s+(?:wt|weight|volume)|quantity|qty|storage|store|keep|warning|directions?|customer|www\\.|https?://)\\b",
    )
    private val quantityLabel = Regex(
        "(?i)\\b(?:net\\s*(?:wt|weight|volume)|quantity|qty|contents?)\\s*[:#-]?\\s*(\\d+(?:[.,]\\d{1,3})?)\\s*(kg|g|mg|mcg|l|ml|cl|oz|lb|lbs|pcs|pieces|tablets?|capsules?|count|ct|items?)\\b",
    )
    private val quantityToken = Regex(
        "(?i)\\b(\\d+(?:[.,]\\d{1,3})?)\\s*(kg|g|mg|mcg|l|ml|cl|oz|lb|lbs|pcs|pieces|tablets?|capsules?|count|ct|items?)\\b",
    )
    private val storageLabel = Regex(
        "(?im)^\\s*(?:storage(?:\\s+location)?|stored?\\s+(?:at|in))\\s*[:#-]\\s*([^\\r\\n]{2,100})$",
    )

    fun extract(text: String, sourceType: ExtractionSourceType, token: Long): SmartAddDraft {
        val dates = DateTextExtractor.extract(text).dates
        val labeledName = productLabel.find(text)?.groupValues?.getOrNull(1)?.trim()
        val fallbackName = text.lineSequence()
            .map(String::trim)
            .filter(::isLikelyProductName)
            .mapIndexed { index, line -> line to productNameScore(line, index) }
            .maxByOrNull { it.second }
            ?.first
        val name = labeledName ?: fallbackName
        val quantityMatch = quantityLabel.find(text) ?: quantityToken.find(text)
        val parsedQuantity = quantityMatch?.groupValues?.getOrNull(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?.takeIf { it > 0.0 }
        val parsedUnit = quantityMatch?.groupValues?.getOrNull(2)?.let(::normalizeUnit)
        val category = inferCategory(text)
        val storageMatch = storageLabel.find(text)
        val storage = storageMatch?.groupValues?.getOrNull(1)?.trim()
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
            categoryName = category?.let { ExtractedField(it, 0.76f, null, true) },
            quantity = parsedQuantity?.let {
                ExtractedField(it, if (quantityMatch.value.contains(Regex("(?i)net|qty|quantity|contents"))) 0.9f else 0.72f, quantityMatch.value, true)
            },
            unit = parsedUnit?.let { ExtractedField(it, 0.86f, quantityMatch.value, true) },
            purchaseDate = dates.firstOrNull { it.kind == ExtractedDateKind.PURCHASED },
            expiryDate = dates.firstOrNull { it.kind in EXPIRY_KINDS },
            storageLocation = storage?.let { ExtractedField(it, 0.88f, storageMatch.value, true) },
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

    private fun isLikelyProductName(line: String): Boolean =
        line.length in 2..80 &&
            line.any(Char::isLetter) &&
            !ignoredLine.containsMatchIn(line) &&
            line.count(Char::isDigit) <= line.length / 2 &&
            !line.contains('@')

    private fun productNameScore(line: String, index: Int): Int {
        val letters = line.count(Char::isLetter)
        val words = line.split(Regex("\\s+")).size
        return letters * 2 + words.coerceAtMost(5) * 3 - index.coerceAtMost(20) -
            if (line == line.uppercase() && line.length > 35) 12 else 0
    }

    private fun normalizeUnit(value: String): String = when (value.lowercase()) {
        "lbs" -> "lb"
        "pcs", "pieces", "count", "ct", "items" -> "item"
        "tablets" -> "tablet"
        "capsules" -> "capsule"
        else -> value.lowercase()
    }

    private fun inferCategory(text: String): String? {
        val normalized = text.lowercase()
        return when {
            CATEGORY_MEDICINE.any(normalized::contains) -> "MEDICINE"
            CATEGORY_SUPPLEMENT.any(normalized::contains) -> "SUPPLEMENT"
            CATEGORY_COSMETIC.any(normalized::contains) -> "COSMETIC"
            CATEGORY_HOUSEHOLD.any(normalized::contains) -> "HOUSEHOLD"
            CATEGORY_GROCERY.any(normalized::contains) -> "GROCERY"
            else -> null
        }
    }

    private val CATEGORY_MEDICINE = listOf("medicine", "tablet", "capsule", "syrup", "dosage", "pharmaceutical")
    private val CATEGORY_SUPPLEMENT = listOf("supplement", "multivitamin", "whey protein", "dietary")
    private val CATEGORY_COSMETIC = listOf("shampoo", "conditioner", "lotion", "cosmetic", "serum", "moisturizer")
    private val CATEGORY_HOUSEHOLD = listOf("detergent", "cleaner", "bleach", "dishwash", "disinfectant")
    private val CATEGORY_GROCERY = listOf("ingredients", "nutrition facts", "food", "beverage")
}

