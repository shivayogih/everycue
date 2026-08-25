package com.everycue.feature.renew

import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.State
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.everycue.core.navigation.Navigator

fun EntryProviderScope<NavKey>.renewEntryBuilder(
    state: State<RenewUiState>,
    viewModel: RenewViewModel,
    navigator: Navigator,
    onAddFiles: (String) -> Unit,
    onTakePhoto: (String) -> Unit,
    onCaptureRenewal: () -> Unit,
    onImportRenewalImage: () -> Unit,
    onViewAttachment: (RenewalAttachment) -> Unit,
    onShareAttachment: (RenewalAttachment) -> Unit,
) {
    entry<RenewHomeRoute> {
        RenewHomeScreen(
            state = state.value,
            onAdd = {
                viewModel.onIntent(RenewIntent.ClearCaptureDraft)
                navigator.navigate(RenewEditorRoute())
            },
            onOpenAll = { navigator.navigate(RenewListRoute) },
            onOpenRenewal = { navigator.navigate(RenewDetailRoute(it)) },
            onOpenHistory = { navigator.navigate(RenewHistoryRoute) },
            onOpenInsights = { navigator.navigate(RenewInsightsRoute) },
        )
    }
    entry<RenewListRoute> {
        RenewListScreen(
            renewals = state.value.renewals,
            onBack = { navigator.goBack() },
            onAdd = {
                viewModel.onIntent(RenewIntent.ClearCaptureDraft)
                navigator.navigate(RenewEditorRoute())
            },
            onOpen = { navigator.navigate(RenewDetailRoute(it)) },
        )
    }
    entry<RenewEditorRoute> { route ->
        RenewEditorScreen(
            existing = route.renewalId?.let { id -> state.value.renewals.firstOrNull { it.id == id } },
            captureDraft = state.value.captureDraft,
            onBack = {
                viewModel.onIntent(RenewIntent.ClearCaptureDraft)
                navigator.goBack()
            },
            onSave = { draft ->
                viewModel.onIntent(RenewIntent.Save(draft, route.renewalId))
            },
            onCaptureRenewal = onCaptureRenewal,
            onImportRenewalImage = onImportRenewalImage,
            onClearCaptureDraft = { viewModel.onIntent(RenewIntent.ClearCaptureDraft) },
        )
    }
    entry<RenewDetailRoute> { route ->
        val item = state.value.renewals.firstOrNull { it.id == route.renewalId }
        RenewDetailScreen(
            item = item,
            attachments = state.value.attachmentsFor(route.renewalId),
            onBack = { navigator.goBack() },
            onEdit = {
                viewModel.onIntent(RenewIntent.ClearCaptureDraft)
                navigator.navigate(RenewEditorRoute(route.renewalId))
            },
            onRenew = { navigator.navigate(MarkRenewedRoute(route.renewalId)) },
            onDelete = {
                navigator.navigate(
                    DeleteRenewalDialogRoute(
                        renewalId = route.renewalId,
                        title = item?.title.orEmpty(),
                    ),
                )
            },
            onAddFiles = { onAddFiles(route.renewalId) },
            onTakePhoto = { onTakePhoto(route.renewalId) },
            onViewAttachment = onViewAttachment,
            onShareAttachment = onShareAttachment,
            onRemoveAttachment = { viewModel.onIntent(RenewIntent.RemoveAttachment(it)) },
        )
    }
    entry<MarkRenewedRoute> { route ->
        MarkRenewedScreen(
            item = state.value.renewals.firstOrNull { it.id == route.renewalId },
            onBack = { navigator.goBack() },
            onConfirm = { newDate, notes ->
                viewModel.onIntent(RenewIntent.MarkRenewed(route.renewalId, newDate, notes))
            },
        )
    }
    entry<RenewHistoryRoute> {
        RenewHistoryScreen(state.value.events, onBack = { navigator.goBack() })
    }
    entry<RenewInsightsRoute> {
        RenewInsightsScreen(state.value, onBack = { navigator.goBack() })
    }
    entry<DeleteRenewalDialogRoute>(
        metadata = DialogSceneStrategy.dialog(DialogProperties()),
    ) { route ->
        DeleteRenewalDialog(
            title = route.title,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.onIntent(RenewIntent.Delete(route.renewalId))
            },
        )
    }
}
