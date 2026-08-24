package com.everycue.feature.track

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu")
private val eventFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu, h:mm a")

fun Long.asEpochDayLabel(): String = LocalDate.ofEpochDay(this).format(dateFormatter)

fun Long.asEventDateLabel(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(eventFormatter)

fun datePickerMillisToEpochDay(value: Long?): Long? = value?.let {
    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
}

fun epochDayToDatePickerMillis(value: Long?): Long? = value?.let {
    LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun Double.quantityLabel(unit: String): String {
    val value = if (this % 1.0 == 0.0) toLong().toString() else String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    return "$value $unit"
}

