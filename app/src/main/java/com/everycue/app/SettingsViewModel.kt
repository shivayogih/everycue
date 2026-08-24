package com.everycue.app

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val settings: AppSettings = AppSettings(), val isBusy: Boolean = false)

sealed interface SettingsIntent {
    data class SetTheme(val value: ThemePreference) : SettingsIntent
    data class SetDynamicColor(val enabled: Boolean) : SettingsIntent
    data class SetReminders(val enabled: Boolean) : SettingsIntent
    data class SetReminderHour(val hour: Int) : SettingsIntent
    data class Export(val uri: Uri) : SettingsIntent
    data class Import(val uri: Uri) : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowMessage(@StringRes val message: Int) : SettingsEffect
}

class SettingsViewModel(
    private val settingsRepository: SettingsStore,
    private val backupRepository: BackupStore,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    val state = combine(settingsRepository.settings, busy, ::SettingsUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> execute { settingsRepository.setTheme(intent.value) }
            is SettingsIntent.SetDynamicColor -> execute { settingsRepository.setDynamicColor(intent.enabled) }
            is SettingsIntent.SetReminders -> execute { settingsRepository.setRemindersEnabled(intent.enabled) }
            is SettingsIntent.SetReminderHour -> execute { settingsRepository.setReminderHour(intent.hour) }
            is SettingsIntent.Export -> execute(R.string.backup_exported) { backupRepository.exportTo(intent.uri) }
            is SettingsIntent.Import -> execute(R.string.backup_restored) { backupRepository.importFrom(intent.uri) }
        }
    }

    private fun execute(@StringRes success: Int? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }
                .onSuccess { success?.let { effectChannel.send(SettingsEffect.ShowMessage(it)) } }
                .onFailure { effectChannel.send(SettingsEffect.ShowMessage(R.string.generic_error)) }
            busy.value = false
        }
    }

    class Factory(
        private val settingsRepository: SettingsStore,
        private val backupRepository: BackupStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(settingsRepository, backupRepository) as T
        }
    }
}

