package com.everycue.feature.track

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.everycue.core.navigation.Navigator

fun EntryProviderScope<NavKey>.trackEntryBuilder(
    state: TrackUiState,
    viewModel: TrackViewModel,
    navigator: Navigator,
) {
    entry<TrackHomeRoute> {
        TrackHomeScreen(
            state = state,
            onAdd = { navigator.navigate(TrackEditorRoute()) },
            onOpenInventory = { navigator.navigate(TrackInventoryRoute) },
            onOpenItem = { navigator.navigate(TrackDetailRoute(it)) },
            onOpenHistory = { navigator.navigate(TrackHistoryRoute) },
            onOpenInsights = { navigator.navigate(TrackInsightsRoute) },
        )
    }
    entry<TrackInventoryRoute> {
        TrackInventoryScreen(
            items = state.items,
            onBack = { navigator.goBack() },
            onAdd = { navigator.navigate(TrackEditorRoute()) },
            onOpenItem = { navigator.navigate(TrackDetailRoute(it)) },
        )
    }
    entry<TrackEditorRoute> { route ->
        TrackEditorScreen(
            existing = route.itemId?.let { id -> state.items.firstOrNull { it.id == id } },
            onBack = { navigator.goBack() },
            onSave = { draft ->
                viewModel.save(draft, route.itemId) { savedId ->
                    if (route.itemId == null) {
                        navigator.openInTopLevel(TrackHomeRoute, TrackDetailRoute(savedId))
                    } else {
                        navigator.goBack()
                    }
                }
            },
        )
    }
    entry<TrackDetailRoute> { route ->
        val item = state.items.firstOrNull { it.id == route.itemId }
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
        TrackHistoryScreen(events = state.events, onBack = { navigator.goBack() })
    }
    entry<TrackInsightsRoute> {
        TrackInsightsScreen(state = state, onBack = { navigator.goBack() })
    }
    entry<TrackOutcomeDialogRoute>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(windowTitle = "Record item outcome"),
        ),
    ) { route ->
        val item = state.items.firstOrNull { it.id == route.itemId }
        TrackConfirmDialog(
            title = "Mark as ${route.outcome.label.lowercase()}?",
            message = "${item?.name ?: "This item"} will leave active inventory and be added to usage history.",
            confirmLabel = route.outcome.label,
            destructive = route.outcome == TrackOutcome.DISCARDED,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.markOutcome(route.itemId, route.outcome) {
                    navigator.selectTopLevel(TrackHomeRoute)
                }
            },
        )
    }
    entry<DeleteTrackItemDialogRoute>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(windowTitle = "Delete tracked item"),
        ),
    ) { route ->
        TrackConfirmDialog(
            title = "Delete ${route.itemName.ifBlank { "item" }}?",
            message = "This removes the item without recording a consumed or waste outcome.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.delete(route.itemId) {
                    navigator.selectTopLevel(TrackHomeRoute)
                }
            },
        )
    }
}
