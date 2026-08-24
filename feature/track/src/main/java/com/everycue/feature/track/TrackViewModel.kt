package com.everycue.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackViewModel(
    private val repository: TrackRepository,
) : ViewModel() {
    val state = combine(repository.items, repository.events, ::TrackUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrackUiState(),
        )

    fun save(draft: TrackDraft, itemId: String?, onSaved: (String) -> Unit) {
        viewModelScope.launch { onSaved(repository.save(draft, itemId)) }
    }

    fun markOutcome(itemId: String, outcome: TrackOutcome, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.markOutcome(itemId, outcome)
            onDone()
        }
    }

    fun delete(itemId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(itemId)
            onDone()
        }
    }

    class Factory(private val repository: TrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TrackViewModel::class.java))
            return TrackViewModel(repository) as T
        }
    }
}
