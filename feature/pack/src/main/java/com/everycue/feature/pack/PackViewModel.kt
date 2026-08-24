package com.everycue.feature.pack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.everycue.feature.pack.PackData
import com.everycue.feature.pack.PackingCategory
import com.everycue.feature.pack.TripDraft
import com.everycue.feature.pack.PackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackViewModel(
    private val repository: PackRepository,
) : ViewModel() {

    val data = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PackData(),
    )

    fun createTrip(draft: TripDraft, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createTrip(draft))
        }
    }

    fun addItem(
        tripId: Long,
        name: String,
        category: PackingCategory,
        quantity: Int,
        onAdded: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.addItem(tripId, name, category, quantity)
            onAdded()
        }
    }

    fun setPacked(tripId: Long, itemId: Long, packed: Boolean) {
        viewModelScope.launch { repository.setPacked(tripId, itemId, packed) }
    }

    fun deleteItem(tripId: Long, itemId: Long) {
        viewModelScope.launch { repository.deleteItem(tripId, itemId) }
    }

    fun unpackAll(tripId: Long) {
        viewModelScope.launch { repository.unpackAll(tripId) }
    }

    fun deleteTrip(tripId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
            onDeleted()
        }
    }

    fun addDemoTrip(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repository.addDemoTrip()) }
    }

    fun resetAll(onReset: () -> Unit) {
        viewModelScope.launch {
            repository.resetAll()
            onReset()
        }
    }

    class Factory(
        private val repository: PackRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PackViewModel::class.java))
            return PackViewModel(repository) as T
        }
    }
}
