package com.everycue.app

import androidx.lifecycle.ViewModel
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: LocalProfile? = null,
    val isBusy: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val isLoaded: Boolean = false,
)

sealed interface ProfileIntent {
    data class Save(val profile: LocalProfile, val finishAfterSave: Boolean) : ProfileIntent
    data object Share : ProfileIntent
    data object ClearError : ProfileIntent
}

sealed interface ProfileEffect {
    data object Saved : ProfileEffect
    data class Share(val profile: LocalProfile) : ProfileEffect
}

class ProfileViewModel(private val repository: UserProfileStore) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<Int?>(null)
    private val effectChannel = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    val state = combine(repository.profile, busy, error) { profile, isBusy, errorMessage ->
        ProfileUiState(profile, isBusy, errorMessage, isLoaded = true)
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.ClearError -> error.value = null
            ProfileIntent.Share -> state.value.profile?.let { profile ->
                viewModelScope.launch {
                    effectChannel.send(ProfileEffect.Share(profile))
                }
            }
            is ProfileIntent.Save -> viewModelScope.launch {
                busy.value = true
                error.value = null
                runCatching { repository.save(intent.profile) }
                    .onSuccess { if (intent.finishAfterSave) effectChannel.send(ProfileEffect.Saved) }
                    .onFailure { error.value = (it as? ProfileValidationException)?.messageResource ?: R.string.generic_error }
                busy.value = false
            }
        }
    }

    class Factory(private val repository: UserProfileStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
            return ProfileViewModel(repository) as T
        }
    }
}

