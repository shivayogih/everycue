package com.everycue.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.everycue.core.extraction.ExtractionSourceType
import com.everycue.core.extraction.SmartAddDraft
import com.everycue.core.extraction.SmartAddTextExtractor
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
    data class ApplyRecognizedText(val text: String, val sourceType: ExtractionSourceType) : TrackIntent
    data class ApplyBarcode(val value: String) : TrackIntent
    data object ClearSmartAdd : TrackIntent
    data class SmartAddFailed(val failure: SmartAddFailure) : TrackIntent
    data class DismissCoachInsight(val insightKey: String) : TrackIntent
    data class HideCoachSubject(val subjectId: String) : TrackIntent
    data class HideCoachCategory(val category: String) : TrackIntent
    data class RestoreCoachPreference(val key: String) : TrackIntent
}

enum class SmartAddFailure { CANCELLED, MODEL_UNAVAILABLE, IMAGE_UNREADABLE, NO_RESULT }

sealed interface TrackEffect {
    data class Saved(val itemId: String, val wasEditing: Boolean) : TrackEffect
    data object OutcomeRecorded : TrackEffect
    data object Deleted : TrackEffect
    data class ShowError(val messageResource: Int) : TrackEffect
}

class TrackViewModel(private val repository: TrackStore) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val smartAddDraft = MutableStateFlow<SmartAddDraft?>(null)
    private val effectChannel = Channel<TrackEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val state = combine(
        repository.items,
        repository.events,
        repository.coachPreferences,
        busy,
        smartAddDraft,
    ) { items, events, preferences, isBusy, draft ->
        TrackUiState(
            items = items,
            events = events,
            isBusy = isBusy,
            smartAddDraft = draft,
            coachPreferences = preferences,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    fun onIntent(intent: TrackIntent) {
        when (intent) {
            is TrackIntent.Save -> execute {
                val id = repository.save(intent.draft, intent.itemId)
                smartAddDraft.value = null
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
            is TrackIntent.ApplyRecognizedText -> {
                val draft = SmartAddTextExtractor.extract(
                    text = intent.text,
                    sourceType = intent.sourceType,
                    token = System.nanoTime(),
                )
                if (draft.productName != null || draft.quantity != null || draft.expiryDate != null) {
                    smartAddDraft.value = draft
                } else {
                    onIntent(TrackIntent.SmartAddFailed(SmartAddFailure.NO_RESULT))
                }
            }
            is TrackIntent.ApplyBarcode -> {
                val normalized = intent.value.trim()
                if (normalized.isEmpty() || normalized.length > 256 || normalized.any(Char::isISOControl)) {
                    onIntent(TrackIntent.SmartAddFailed(SmartAddFailure.NO_RESULT))
                    return
                }
                val matches = state.value.items.filter { it.barcode == normalized }
                val known = matches.firstOrNull()
                smartAddDraft.value = SmartAddTextExtractor.fromBarcode(
                    barcode = normalized,
                    token = System.nanoTime(),
                    knownName = known?.name,
                    knownCategoryName = known?.category?.name,
                    knownLocation = known?.storageLocation,
                    duplicateIds = matches.map(TrackItem::id),
                )
            }
            TrackIntent.ClearSmartAdd -> smartAddDraft.value = null
            is TrackIntent.SmartAddFailed -> if (intent.failure != SmartAddFailure.CANCELLED) {
                viewModelScope.launch {
                    val message = when (intent.failure) {
                        SmartAddFailure.MODEL_UNAVAILABLE -> R.string.smart_add_model_unavailable
                        SmartAddFailure.IMAGE_UNREADABLE -> R.string.smart_add_image_unreadable
                        SmartAddFailure.NO_RESULT -> R.string.smart_add_no_result
                        SmartAddFailure.CANCELLED -> R.string.smart_add_no_result
                    }
                    effectChannel.send(TrackEffect.ShowError(message))
                }
            }
            is TrackIntent.DismissCoachInsight -> saveCoachPreference(
                key = "insight:${intent.insightKey}",
                type = TrackCoachPreferenceType.DISMISSED_INSIGHT,
                value = intent.insightKey,
            )
            is TrackIntent.HideCoachSubject -> saveCoachPreference(
                key = "subject:${intent.subjectId}",
                type = TrackCoachPreferenceType.HIDDEN_SUBJECT,
                value = intent.subjectId,
            )
            is TrackIntent.HideCoachCategory -> saveCoachPreference(
                key = "category:${intent.category}",
                type = TrackCoachPreferenceType.HIDDEN_CATEGORY,
                value = intent.category,
            )
            is TrackIntent.RestoreCoachPreference -> execute {
                repository.removeCoachPreference(intent.key)
            }
        }
    }

    private fun saveCoachPreference(key: String, type: TrackCoachPreferenceType, value: String) = execute {
        repository.saveCoachPreference(
            TrackCoachPreference(
                key = key,
                type = type,
                value = value,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            runCatching { block() }.onFailure { error ->
                effectChannel.send(
                    TrackEffect.ShowError(
                        (error as? TrackValidationException)?.messageResource ?: R.string.track_generic_error,
                    ),
                )
            }
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

