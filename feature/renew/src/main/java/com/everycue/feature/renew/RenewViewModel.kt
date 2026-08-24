package com.everycue.feature.renew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RenewViewModel(
    private val repository: RenewRepository,
) : ViewModel() {
    val state = combine(repository.renewals, repository.events, ::RenewUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RenewUiState())

    fun save(draft: RenewalDraft, id: String?, onSaved: (String) -> Unit) {
        viewModelScope.launch { onSaved(repository.save(draft, id)) }
    }

    fun markRenewed(id: String, newDueEpochDay: Long, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.markRenewed(id, newDueEpochDay, notes)
            onDone()
        }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(id)
            onDone()
        }
    }

    class Factory(private val repository: RenewRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RenewViewModel::class.java))
            return RenewViewModel(repository) as T
        }
    }
}
