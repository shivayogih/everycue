package com.everycue.core.extraction

import java.time.DateTimeException
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

/** Deterministic, local-only date extraction for OCR text. */
object DateTextExtractor {
    private data class LabelRule(
        val kind: ExtractedDateKind,
        val regex: Regex,
    )

    private val labelRules = listOf(
        LabelRule(ExtractedDateKind.BEST_BEFORE, Regex("\\b(?:best\\s+before|best\\s+by|bb)\\b", RegexOption.IGNORE_CASE)),
        LabelRule(ExtractedDateKind.USE_BY, Regex("\\b(?:use\\s+by|use\\s+before)\\b", RegexOption.IGNORE_CASE)),
        LabelRule(ExtractedDateKind.MANUFACTURED, Regex("\\b(?:mfg|mfd|manufactured|manufacturing)(?:\\s+date)?\\b", RegexOption.IGNORE_CASE)),
        LabelRule(ExtractedDateKind.PURCHASED, Regex("\\b(?:purchased?|purchase)(?:\\s+date)?\\b", RegexOption.IGNORE_CASE)),
        LabelRule(ExtractedDateKind.START, Regex("\\b(?:start|issued|effective)(?:\\s+date)?\\b", RegexOption.IGNORE_CASE)),
        LabelRule(ExtractedDateKind.DUE, Regex("\\b(?:due|valid\\s+until|renew(?:al)?(?:\\s+date)?)\\b", RegexOption.IGNORE_CASE)),
        LabelRule(ExtractedDateKind.EXPIRY, Regex("\\b(?:exp|expiry|expires)(?:\\s+date)?\\b", RegexOption.IGNORE_CASE)),
    )

    private const val MONTH_NAME =
        "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"

    private val dateToken = Regex(
        pattern = """(?ix)
            (?:\d{4}[./-]\d{1,2}[./-]\d{1,2})
            |(?:\d{1,2}[./-]\d{1,2}[./-]\d{4})
            |(?:\d{1,2}\s+$MONTH_NAME\s+\d{4})
            |(?:$MONTH_NAME\s+\d{1,2},?\s+\d{4})
            |(?:$MONTH_NAME\s+\d{2,4})
            |(?:\d{1,2}[./-]\d{2,4})
        """.trimIndent(),
    )

    private val namedMonths = Month.entries.associateBy { it.name.lowercase(Locale.ROOT) } +
        Month.entries.associateBy { it.name.take(3).lowercase(Locale.ROOT) }

    private val dayMonthYear = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM uuuu")
        .toFormatter(Locale.ENGLISH)
        .withResolverStyle(ResolverStyle.STRICT)

    private val monthDayYear = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM d uuuu")
        .toFormatter(Locale.ENGLISH)
        .withResolverStyle(ResolverStyle.STRICT)

    fun extract(text: String): DateExtractionResult {
        if (text.isBlank()) return DateExtractionResult(emptyList())

        val candidates = buildList {
            val lines = text.lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .toList()
            lines.forEachIndexed { lineIndex, originalLine ->
                    // OCR commonly places the value on the line after a short label such as
                    // "EXP". Include that adjacent line only when the label line has no date.
                    val line = if (
                        labelRules.any { it.regex.containsMatchIn(originalLine) } &&
                        dateToken.find(originalLine) == null
                    ) {
                        "$originalLine ${lines.getOrNull(lineIndex + 1).orEmpty()}".trim()
                    } else {
                        originalLine
                    }
                    val labels = labelRules.mapNotNull { rule ->
                        rule.regex.find(line)?.let { match -> rule to match.range.first }
                    }.sortedBy { it.second }

                    labels.forEachIndexed { index, (rule, start) ->
                        val end = labels.getOrNull(index + 1)?.second ?: line.length
                        val segment = line.substring(start, end)
                        val match = dateToken.find(segment) ?: return@forEachIndexed
                        parseToken(match.value, rule.kind)?.let { parsed ->
                            add(
                                ExtractedDate(
                                    kind = rule.kind,
                                    field = ExtractedField(
                                        value = parsed.date,
                                        confidence = if (parsed.precision == DatePrecision.DAY) 0.96f else 0.86f,
                                        sourceText = segment.trim().take(160),
                                        requiresConfirmation = true,
                                    ),
                                    precision = parsed.precision,
                                    normalizationRule = parsed.rule,
                                ),
                            )
                        }
                    }
                }
        }

        return DateExtractionResult(candidates.distinctBy { it.kind to it.field.value })
    }

    private data class ParsedDate(
        val date: LocalDate,
        val precision: DatePrecision,
        val rule: DateNormalizationRule,
    )

    private fun parseToken(raw: String, kind: ExtractedDateKind): ParsedDate? {
        val token = raw.trim().replace(Regex("\\s+"), " ").removeSuffix(",")
        return try {
            when {
                token.matches(Regex("\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}")) -> {
                    val parts = token.split(Regex("[./-]"))
                    exactDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }
                token.matches(Regex("\\d{1,2}[./-]\\d{1,2}[./-]\\d{4}")) -> {
                    val parts = token.split(Regex("[./-]"))
                    exactDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
                token.matches(Regex("\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4}")) ->
                    ParsedDate(LocalDate.parse(token, dayMonthYear), DatePrecision.DAY, DateNormalizationRule.EXACT_DATE)
                token.matches(Regex("[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4}")) -> {
                    val normalized = token.replace(",", "")
                    ParsedDate(LocalDate.parse(normalized, monthDayYear), DatePrecision.DAY, DateNormalizationRule.EXACT_DATE)
                }
                token.matches(Regex("[A-Za-z]{3,9}\\s+\\d{2,4}")) -> {
                    val parts = token.split(" ")
                    monthDate(namedMonth(parts[0]), normalizeYear(parts[1].toInt()), kind)
                }
                token.matches(Regex("\\d{1,2}[./-]\\d{2,4}")) -> {
                    val parts = token.split(Regex("[./-]"))
                    monthDate(parts[0].toInt(), normalizeYear(parts[1].toInt()), kind)
                }
                else -> null
            }
        } catch (_: DateTimeException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun exactDate(year: Int, month: Int, day: Int) = ParsedDate(
        date = LocalDate.of(year, month, day),
        precision = DatePrecision.DAY,
        rule = DateNormalizationRule.EXACT_DATE,
    )

    private fun monthDate(month: Int, year: Int, kind: ExtractedDateKind): ParsedDate {
        val value = YearMonth.of(year, month)
        val useStart = kind in setOf(ExtractedDateKind.MANUFACTURED, ExtractedDateKind.START)
        return ParsedDate(
            date = if (useStart) value.atDay(1) else value.atEndOfMonth(),
            precision = DatePrecision.MONTH,
            rule = if (useStart) DateNormalizationRule.START_OF_MONTH else DateNormalizationRule.END_OF_MONTH,
        )
    }

    private fun namedMonth(value: String): Int = namedMonths[value.lowercase(Locale.ROOT)]?.value
        ?: throw DateTimeException("Unsupported month")

    private fun normalizeYear(value: Int): Int = if (value in 0..99) 2000 + value else value
}

