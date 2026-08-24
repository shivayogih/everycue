package com.everycue.feature.pack

import com.everycue.feature.pack.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Long?.asDateLabel(): String {
    if (this == null) return "Not selected"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(this))
}

fun Trip.dateRangeLabel(): String = when {
    startDateMillis != null && endDateMillis != null ->
        "${startDateMillis.asDateLabel()} – ${endDateMillis.asDateLabel()}"
    startDateMillis != null -> startDateMillis.asDateLabel()
    else -> "Dates not set"
}

fun Trip.packingSummary(): String = when {
    totalCount == 0 -> "No items yet"
    packedCount == totalCount -> "Ready to go"
    else -> "$packedCount of $totalCount packed"
}
