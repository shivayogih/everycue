package com.everycue.feature.track

import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.State
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.everycue.core.navigation.Navigator

fun EntryProviderScope<NavKey>.trackEntryBuilder(
    state: State<TrackUiState>,
    viewModel: TrackViewModel,
    navigator: Navigator,
    onScanBarcode: () -> Unit,
    onScanLabel: () -> Unit,
    onImportImage: () -> Unit,
) {
    entry<TrackHomeRoute> {
        val currentState = state.value
        TrackHomeScreen(
            state = currentState,
            onAdd = { navigator.navigate(TrackEditorRoute()) },
            onOpenInventory = { navigator.navigate(TrackInventoryRoute) },
            onOpenItem = { navigator.navigate(TrackDetailRoute(it)) },
            onOpenHistory = { navigator.navigate(TrackHistoryRoute) },
            onOpenInsights = { navigator.navigate(TrackInsightsRoute) },
        )
    }
    entry<TrackInventoryRoute> {
        TrackInventoryScreen(
            items = state.value.items,
            onBack = { navigator.goBack() },
            onAdd = { navigator.navigate(TrackEditorRoute()) },
            onOpenItem = { navigator.navigate(TrackDetailRoute(it)) },
        )
    }
    entry<TrackEditorRoute> { route ->
        val currentState = state.value
        TrackEditorScreen(
            existing = route.itemId?.let { id -> currentState.items.firstOrNull { it.id == id } },
            smartDraft = currentState.smartAddDraft.takeIf { route.itemId == null },
            onBack = { navigator.goBack() },
            onScanBarcode = onScanBarcode,
            onScanLabel = onScanLabel,
            onImportImage = onImportImage,
            onClearSmartAdd = { viewModel.onIntent(TrackIntent.ClearSmartAdd) },
            onSave = { draft ->
                viewModel.onIntent(TrackIntent.Save(draft, route.itemId))
            },
        )
    }
    entry<TrackDetailRoute> { route ->
        val item = state.value.items.firstOrNull { it.id == route.itemId }
        TrackDetailScreen(
            item = item,
            onBack = { navigator.goBack() },
            onEdit = { navigator.navigate(TrackEditorRoute(route.itemId)) },
            onOutcome = { navigator.navigate(TrackOutcomeDialogRoute(route.itemId, it)) },
            onDelete = {
                navigator.navigate(
                    DeleteTrackItemDialogRoute(
                        itemId = route.itemId,
                        itemName = item?.name.orEmpty(),
                    ),
                )
            },
        )
    }
    entry<TrackHistoryRoute> {
        TrackHistoryScreen(events = state.value.events, onBack = { navigator.goBack() })
    }
    entry<TrackInsightsRoute> {
        TrackInsightsScreen(state = state.value, onBack = { navigator.goBack() })
    }
    entry<TrackOutcomeDialogRoute>(
        metadata = DialogSceneStrategy.dialog(DialogProperties()),
    ) { route ->
        val item = state.value.items.firstOrNull { it.id == route.itemId }
        TrackConfirmDialog(
            title = stringResource(R.string.mark_outcome_title, route.outcome.displayNameForEntry().lowercase()),
            message = stringResource(R.string.outcome_message, item?.name ?: stringResource(R.string.this_item)),
            confirmLabel = route.outcome.displayNameForEntry(),
            destructive = route.outcome == TrackOutcome.DISCARDED,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.onIntent(TrackIntent.MarkOutcome(route.itemId, route.outcome))
            },
        )
    }
    entry<DeleteTrackItemDialogRoute>(
        metadata = DialogSceneStrategy.dialog(DialogProperties()),
    ) { route ->
        TrackConfirmDialog(
            title = stringResource(R.string.delete_item_title, route.itemName.ifBlank { stringResource(R.string.delete_item_fallback) }),
            message = stringResource(R.string.delete_item_message),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.onIntent(TrackIntent.Delete(route.itemId))
            },
        )
    }
}

@androidx.compose.runtime.Composable
private fun TrackOutcome.displayNameForEntry(): String = stringResource(
    when (this) {
        TrackOutcome.CONSUMED -> R.string.consumed
        TrackOutcome.DISCARDED -> R.string.discarded
        TrackOutcome.DONATED -> R.string.donated
    },
)
