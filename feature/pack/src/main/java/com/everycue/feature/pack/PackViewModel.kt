package com.everycue.feature.pack

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

sealed interface PackIntent {
    data class CreateTrip(val draft: TripDraft) : PackIntent
    data class UpdateTrip(val tripId: Long, val draft: TripDraft) : PackIntent
    data class AddItem(val tripId: Long, val name: String, val category: PackingCategory, val quantity: Int) : PackIntent
    data class SetPacked(val tripId: Long, val itemId: Long, val packed: Boolean) : PackIntent
    data class DeleteItem(val tripId: Long, val itemId: Long) : PackIntent
    data class MoveItem(val tripId: Long, val itemId: Long, val offset: Int) : PackIntent
    data class UnpackAll(val tripId: Long) : PackIntent
    data class DeleteTrip(val tripId: Long) : PackIntent
    data object AddDemoTrip : PackIntent
    data object ResetAll : PackIntent
}

sealed interface PackEffect {
    data class TripCreated(val tripId: Long) : PackEffect
    data object ItemAdded : PackEffect
    data object TripUpdated : PackEffect
    data object TripDeleted : PackEffect
    data object Reset : PackEffect
    data class ShowError(val messageResource: Int) : PackEffect
}

class PackViewModel(private val repository: PackStore) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val effectChannel = Channel<PackEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val state = combine(repository.data, busy) { data, isBusy -> PackUiState(data, isBusy) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PackUiState())

    fun onIntent(intent: PackIntent) {
        when (intent) {
            is PackIntent.CreateTrip -> execute {
                effectChannel.send(PackEffect.TripCreated(repository.createTrip(intent.draft)))
            }
            is PackIntent.UpdateTrip -> execute {
                repository.updateTrip(intent.tripId, intent.draft)
                effectChannel.send(PackEffect.TripUpdated)
            }
            is PackIntent.AddItem -> execute {
                repository.addItem(intent.tripId, intent.name, intent.category, intent.quantity)
                effectChannel.send(PackEffect.ItemAdded)
            }
            is PackIntent.SetPacked -> execute { repository.setPacked(intent.tripId, intent.itemId, intent.packed) }
            is PackIntent.DeleteItem -> execute { repository.deleteItem(intent.tripId, intent.itemId) }
            is PackIntent.MoveItem -> execute { repository.moveItem(intent.tripId, intent.itemId, intent.offset) }
            is PackIntent.UnpackAll -> execute { repository.unpackAll(intent.tripId) }
            is PackIntent.DeleteTrip -> execute {
                repository.deleteTrip(intent.tripId)
                effectChannel.send(PackEffect.TripDeleted)
            }
            PackIntent.AddDemoTrip -> execute {
                effectChannel.send(PackEffect.TripCreated(repository.addDemoTrip()))
            }
            PackIntent.ResetAll -> execute {
                repository.resetAll()
                effectChannel.send(PackEffect.Reset)
            }
        }
    }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }.onFailure { effectChannel.send(PackEffect.ShowError(R.string.pack_generic_error)) }
            busy.value = false
        }
    }

    class Factory(private val repository: PackStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PackViewModel::class.java))
            return PackViewModel(repository) as T
        }
    }
}

