package com.everycue.feature.pack

import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.State
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.everycue.core.navigation.Navigator

fun EntryProviderScope<NavKey>.packEntryBuilder(
    state: State<PackUiState>,
    viewModel: PackViewModel,
    navigator: Navigator,
) {
    entry<PackTripsRoute> {
        TripsScreen(
            trips = state.value.data.trips,
            onTripClick = { navigator.navigate(PackTripDetailRoute(it)) },
            onCreateTrip = { navigator.navigate(CreatePackTripRoute()) },
            onTemplates = { navigator.navigate(PackTemplatesRoute) },
        )
    }
    entry<CreatePackTripRoute> { route ->
        CreateTripScreen(
            templateId = route.templateId,
            onBack = { navigator.goBack() },
            onCreate = { draft ->
                viewModel.onIntent(PackIntent.CreateTrip(draft))
            },
        )
    }
    entry<EditPackTripRoute> { route ->
        val trip = state.value.data.trips.firstOrNull { it.id == route.tripId }
        CreateTripScreen(
            templateId = null,
            existing = trip,
            onBack = { navigator.goBack() },
            onCreate = { draft -> viewModel.onIntent(PackIntent.UpdateTrip(route.tripId, draft)) },
        )
    }
    entry<PackTripDetailRoute> { route ->
        val trip = state.value.data.trips.firstOrNull { it.id == route.tripId }
        TripDetailScreen(
            trip = trip,
            onBack = { navigator.goBack() },
            onAddItem = { name, category, quantity ->
                viewModel.onIntent(PackIntent.AddItem(route.tripId, name, category, quantity))
            },
            onEditTrip = { navigator.navigate(EditPackTripRoute(route.tripId)) },
            onTogglePacked = { item, packed -> viewModel.onIntent(PackIntent.SetPacked(route.tripId, item.id, packed)) },
            onDeleteItem = { item -> viewModel.onIntent(PackIntent.DeleteItem(route.tripId, item.id)) },
            onMoveItem = { item, offset -> viewModel.onIntent(PackIntent.MoveItem(route.tripId, item.id, offset)) },
            onUnpackAll = { viewModel.onIntent(PackIntent.UnpackAll(route.tripId)) },
            onDeleteTrip = {
                navigator.navigate(
                    DeletePackTripDialogRoute(
                        tripId = route.tripId,
                        tripName = trip?.name.orEmpty(),
                    ),
                )
            },
        )
    }
    entry<PackTemplatesRoute> {
        TemplatesScreen(onTemplateClick = { navigator.navigate(PackTemplateDetailRoute(it)) })
    }
    entry<PackTemplateDetailRoute> { route ->
        TemplateDetailScreen(
            templateId = route.templateId,
            onBack = { navigator.goBack() },
            onUseTemplate = { navigator.navigate(CreatePackTripRoute(route.templateId)) },
        )
    }
    entry<AddPackItemDialogRoute>(
        metadata = DialogSceneStrategy.dialog(DialogProperties()),
    ) { route ->
        AddItemBottomSheetScreen(
            onDismiss = { navigator.goBack() },
            onAdd = { name, category, quantity ->
                viewModel.onIntent(PackIntent.AddItem(route.tripId, name, category, quantity))
                navigator.goBack()
            },
        )
    }
    entry<DeletePackTripDialogRoute>(
        metadata = DialogSceneStrategy.dialog(DialogProperties()),
    ) { route ->
        DeleteTripDialogScreen(
            tripName = route.tripName,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.onIntent(PackIntent.DeleteTrip(route.tripId))
            },
        )
    }
}
