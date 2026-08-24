package com.everycue.feature.renew

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu")
private val historyFormatter = DateTimeFormatter.ofPattern("dd MMM uuuu, h:mm a")

fun Long.asRenewDateLabel(): String = LocalDate.ofEpochDay(this).format(dateFormatter)
fun Long.asRenewHistoryLabel(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(historyFormatter)
fun renewPickerMillisToEpochDay(value: Long?): Long? = value?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
fun renewEpochDayToPickerMillis(value: Long?): Long? = value?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }

