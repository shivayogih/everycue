package com.everycue.feature.pack

import com.everycue.feature.pack.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Long?.asDateLabel(notSelected: String = ""): String {
    if (this == null) return notSelected
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(this))
}

fun Trip.dateRangeLabel(datesNotSet: String = ""): String = when {
    startDateMillis != null && endDateMillis != null ->
        "${startDateMillis.asDateLabel()} – ${endDateMillis.asDateLabel()}"
    startDateMillis != null -> startDateMillis.asDateLabel()
    else -> datesNotSet
}

fun Trip.packingSummary(noItems: String = "", ready: String = "", packed: (Int, Int) -> String = { packedCount, totalCount -> "$packedCount/$totalCount" }): String = when {
    totalCount == 0 -> noItems
    packedCount == totalCount -> ready
    else -> packed(packedCount, totalCount)
}

internal data class PackingSection(
    val category: PackingCategory,
    val items: List<PackingItem>,
)

/** Builds the visible list in one pass, even when a trip contains hundreds of items. */
internal fun buildPackingSections(
    items: List<PackingItem>,
    query: String,
): List<PackingSection> {
    val buckets = Array(PackingCategory.entries.size) { mutableListOf<PackingItem>() }
    val search = query.trim()
    items.forEach { item ->
        if (search.isEmpty() || item.name.contains(search, ignoreCase = true)) {
            buckets[item.category.ordinal].add(item)
        }
    }
    val itemOrder = compareBy<PackingItem>(PackingItem::isPacked).thenBy(PackingItem::position)
    return PackingCategory.entries.mapIndexedNotNull { index, category ->
        buckets[index]
            .takeIf(MutableList<PackingItem>::isNotEmpty)
            ?.also { it.sortWith(itemOrder) }
            ?.let { PackingSection(category, it) }
    }
}
