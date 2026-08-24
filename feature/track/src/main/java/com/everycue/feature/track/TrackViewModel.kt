package com.everycue.feature.track

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

sealed interface TrackIntent {
    data class Save(val draft: TrackDraft, val itemId: String?) : TrackIntent
    data class MarkOutcome(val itemId: String, val outcome: TrackOutcome) : TrackIntent
    data class Delete(val itemId: String) : TrackIntent
}

sealed interface TrackEffect {
    data class Saved(val itemId: String, val wasEditing: Boolean) : TrackEffect
    data object OutcomeRecorded : TrackEffect
    data object Deleted : TrackEffect
    data class ShowError(val messageResource: Int) : TrackEffect
}

class TrackViewModel(private val repository: TrackStore) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val effectChannel = Channel<TrackEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val state = combine(repository.items, repository.events, busy) { items, events, isBusy ->
        TrackUiState(items = items, events = events, isBusy = isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    fun onIntent(intent: TrackIntent) {
        when (intent) {
            is TrackIntent.Save -> execute {
                val id = repository.save(intent.draft, intent.itemId)
                effectChannel.send(TrackEffect.Saved(id, intent.itemId != null))
            }
            is TrackIntent.MarkOutcome -> execute {
                repository.markOutcome(intent.itemId, intent.outcome)
                effectChannel.send(TrackEffect.OutcomeRecorded)
            }
            is TrackIntent.Delete -> execute {
                repository.delete(intent.itemId)
                effectChannel.send(TrackEffect.Deleted)
            }
        }
    }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }.onFailure { effectChannel.send(TrackEffect.ShowError(R.string.track_generic_error)) }
            busy.value = false
        }
    }

    class Factory(private val repository: TrackStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TrackViewModel::class.java))
            return TrackViewModel(repository) as T
        }
    }
}

