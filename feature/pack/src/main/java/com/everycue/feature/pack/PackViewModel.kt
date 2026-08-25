package com.everycue.feature.pack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val packedOverrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    private val mutationMutex = Mutex()
    private val effectChannel = Channel<PackEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val state = combine(repository.data, busy, packedOverrides) { data, isBusy, overrides ->
        PackUiState(data.withPackedOverrides(overrides), isBusy)
    }
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
            is PackIntent.SetPacked -> setPacked(intent)
            is PackIntent.DeleteItem -> execute(markBusy = false) { repository.deleteItem(intent.tripId, intent.itemId) }
            is PackIntent.MoveItem -> execute(markBusy = false) { repository.moveItem(intent.tripId, intent.itemId, intent.offset) }
            is PackIntent.UnpackAll -> execute(markBusy = false) { repository.unpackAll(intent.tripId) }
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

    private fun setPacked(intent: PackIntent.SetPacked) {
        packedOverrides.update { it + (intent.itemId to intent.packed) }
        viewModelScope.launch {
            mutationMutex.withLock {
                runCatching {
                    repository.setPacked(intent.tripId, intent.itemId, intent.packed)
                    repository.data.first { data ->
                        data.trips
                            .firstOrNull { it.id == intent.tripId }
                            ?.items
                            ?.firstOrNull { it.id == intent.itemId }
                            ?.isPacked == intent.packed
                    }
                }.onFailure {
                    effectChannel.send(PackEffect.ShowError(R.string.pack_generic_error))
                }
                packedOverrides.update { overrides ->
                    if (overrides[intent.itemId] == intent.packed) overrides - intent.itemId else overrides
                }
            }
        }
    }

    private fun execute(markBusy: Boolean = true, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (markBusy) busy.value = true
            mutationMutex.withLock {
                runCatching { block() }.onFailure { error ->
                    effectChannel.send(
                        PackEffect.ShowError(
                            (error as? PackValidationException)?.messageResource ?: R.string.pack_generic_error,
                        ),
                    )
                }
            }
            if (markBusy) busy.value = false
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
