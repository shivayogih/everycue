package com.everycue.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.settingsDataStore by preferencesDataStore(name = "everycue_settings")

@Serializable
enum class ThemePreference { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val dynamicColor: Boolean = true,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 9,
)

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun snapshot(): AppSettings
    suspend fun setTheme(value: ThemePreference)
    suspend fun setDynamicColor(value: Boolean)
    suspend fun setRemindersEnabled(value: Boolean)
    suspend fun setReminderHour(value: Int)
    suspend fun replaceAll(value: AppSettings)
}

class SettingsRepository(context: Context) : SettingsStore {
    private val appContext = context.applicationContext

    override val settings: Flow<AppSettings> = appContext.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            AppSettings(
                theme = preferences[THEME]
                    ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                    ?: ThemePreference.SYSTEM,
                dynamicColor = preferences[DYNAMIC_COLOR] ?: true,
                remindersEnabled = preferences[REMINDERS_ENABLED] ?: false,
                reminderHour = (preferences[REMINDER_HOUR] ?: 9).coerceIn(0, 23),
            )
        }

    override suspend fun snapshot(): AppSettings = settings.first()

    override suspend fun setTheme(value: ThemePreference) = update { copy(theme = value) }
    override suspend fun setDynamicColor(value: Boolean) = update { copy(dynamicColor = value) }
    override suspend fun setRemindersEnabled(value: Boolean) = update { copy(remindersEnabled = value) }
    override suspend fun setReminderHour(value: Int) = update { copy(reminderHour = value.coerceIn(0, 23)) }
    override suspend fun replaceAll(value: AppSettings) = write(value)

    private suspend fun update(transform: AppSettings.() -> AppSettings) {
        write(snapshot().transform())
    }

    private suspend fun write(value: AppSettings) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[THEME] = value.theme.name
            preferences[DYNAMIC_COLOR] = value.dynamicColor
            preferences[REMINDERS_ENABLED] = value.remindersEnabled
            preferences[REMINDER_HOUR] = value.reminderHour.coerceIn(0, 23)
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    }
}

