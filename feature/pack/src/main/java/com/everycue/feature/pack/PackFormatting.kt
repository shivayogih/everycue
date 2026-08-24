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

