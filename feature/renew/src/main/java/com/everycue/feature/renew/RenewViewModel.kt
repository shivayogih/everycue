package com.everycue.feature.renew

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

sealed interface RenewIntent {
    data class Save(val draft: RenewalDraft, val renewalId: String?) : RenewIntent
    data class MarkRenewed(val renewalId: String, val newDueEpochDay: Long, val notes: String) : RenewIntent
    data class Delete(val renewalId: String) : RenewIntent
}

sealed interface RenewEffect {
    data class Saved(val renewalId: String, val wasEditing: Boolean) : RenewEffect
    data object MarkedRenewed : RenewEffect
    data object Deleted : RenewEffect
    data class ShowError(val messageResource: Int) : RenewEffect
}

class RenewViewModel(private val repository: RenewStore) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val effectChannel = Channel<RenewEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val state = combine(repository.renewals, repository.events, busy) { renewals, events, isBusy ->
        RenewUiState(renewals = renewals, events = events, isBusy = isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RenewUiState())

    fun onIntent(intent: RenewIntent) {
        when (intent) {
            is RenewIntent.Save -> execute {
                val id = repository.save(intent.draft, intent.renewalId)
                effectChannel.send(RenewEffect.Saved(id, intent.renewalId != null))
            }
            is RenewIntent.MarkRenewed -> execute {
                repository.markRenewed(intent.renewalId, intent.newDueEpochDay, intent.notes)
                effectChannel.send(RenewEffect.MarkedRenewed)
            }
            is RenewIntent.Delete -> execute {
                repository.delete(intent.renewalId)
                effectChannel.send(RenewEffect.Deleted)
            }
        }
    }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }.onFailure { error ->
                effectChannel.send(
                    RenewEffect.ShowError(
                        (error as? RenewalValidationException)?.messageResource ?: R.string.renew_generic_error,
                    ),
                )
            }
            busy.value = false
        }
    }

    class Factory(private val repository: RenewStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RenewViewModel::class.java))
            return RenewViewModel(repository) as T
        }
    }
}
