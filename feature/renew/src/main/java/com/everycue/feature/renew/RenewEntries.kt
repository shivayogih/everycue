package com.everycue.feature.renew

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.everycue.core.navigation.Navigator

fun EntryProviderScope<NavKey>.renewEntryBuilder(
    state: RenewUiState,
    viewModel: RenewViewModel,
    navigator: Navigator,
) {
    entry<RenewHomeRoute> {
        RenewHomeScreen(
            state = state,
            onAdd = { navigator.navigate(RenewEditorRoute()) },
            onOpenAll = { navigator.navigate(RenewListRoute) },
            onOpenRenewal = { navigator.navigate(RenewDetailRoute(it)) },
            onOpenHistory = { navigator.navigate(RenewHistoryRoute) },
        )
    }
    entry<RenewListRoute> {
        RenewListScreen(
            renewals = state.renewals,
            onBack = { navigator.goBack() },
            onAdd = { navigator.navigate(RenewEditorRoute()) },
            onOpen = { navigator.navigate(RenewDetailRoute(it)) },
        )
    }
    entry<RenewEditorRoute> { route ->
        RenewEditorScreen(
            existing = route.renewalId?.let { id -> state.renewals.firstOrNull { it.id == id } },
            onBack = { navigator.goBack() },
            onSave = { draft ->
                viewModel.save(draft, route.renewalId) { savedId ->
                    if (route.renewalId == null) {
                        navigator.openInTopLevel(RenewHomeRoute, RenewDetailRoute(savedId))
                    } else {
                        navigator.goBack()
                    }
                }
            },
        )
    }
    entry<RenewDetailRoute> { route ->
        val item = state.renewals.firstOrNull { it.id == route.renewalId }
        RenewDetailScreen(
            item = item,
            onBack = { navigator.goBack() },
            onEdit = { navigator.navigate(RenewEditorRoute(route.renewalId)) },
            onRenew = { navigator.navigate(MarkRenewedRoute(route.renewalId)) },
            onDelete = {
                navigator.navigate(
                    DeleteRenewalDialogRoute(
                        renewalId = route.renewalId,
                        title = item?.title.orEmpty(),
                    ),
                )
            },
        )
    }
    entry<MarkRenewedRoute> { route ->
        MarkRenewedScreen(
            item = state.renewals.firstOrNull { it.id == route.renewalId },
            onBack = { navigator.goBack() },
            onConfirm = { newDate, notes ->
                viewModel.markRenewed(route.renewalId, newDate, notes) {
                    navigator.goBack()
                }
            },
        )
    }
    entry<RenewHistoryRoute> {
        RenewHistoryScreen(state.events, onBack = { navigator.goBack() })
    }
    entry<DeleteRenewalDialogRoute>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(windowTitle = "Delete renewal"),
        ),
    ) { route ->
        DeleteRenewalDialog(
            title = route.title,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.delete(route.renewalId) {
                    navigator.selectTopLevel(RenewHomeRoute)
                }
            },
        )
    }
}
