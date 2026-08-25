package com.everycue.app

import com.everycue.feature.pack.PackData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupFormatTest {
    @Test
    fun currentBackupRoundTripsWithoutDataLoss() {
        val source = EveryCueBackup(
            exportedAtMillis = 1234,
            trackItems = listOf(TrackItemBackup("t1", "Milk", "GROCERY", 1.0, "litre", null, 20, "Fridge", "", 3, "ACTIVE", 1, 2)),
            trackEvents = emptyList(),
            renewals = emptyList(),
            renewalEvents = emptyList(),
            pack = PackData(),
            settings = AppSettings(theme = ThemePreference.DARK, remindersEnabled = true, reminderHour = 8),
            profile = LocalProfile("Asha", "Rao", "+91", "9876543210", "asha@example.com", "1 Green Road", "560001"),
        )
        val json = Json { encodeDefaults = true }
        assertEquals(source, json.decodeFromString<EveryCueBackup>(json.encodeToString(source)))
    }
}
