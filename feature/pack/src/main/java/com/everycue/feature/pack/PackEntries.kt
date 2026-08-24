package com.everycue.feature.pack

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.everycue.core.navigation.Navigator

fun EntryProviderScope<NavKey>.packEntryBuilder(
    data: PackData,
    viewModel: PackViewModel,
    navigator: Navigator,
) {
    entry<PackTripsRoute> {
        TripsScreen(
            trips = data.trips,
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
                viewModel.createTrip(draft) { tripId ->
                    navigator.openInTopLevel(PackTripsRoute, PackTripDetailRoute(tripId))
                }
            },
        )
    }
    entry<PackTripDetailRoute> { route ->
        val trip = data.trips.firstOrNull { it.id == route.tripId }
        TripDetailScreen(
            trip = trip,
            onBack = { navigator.goBack() },
            onAddItem = { navigator.navigate(AddPackItemDialogRoute(route.tripId)) },
            onTogglePacked = { item, packed -> viewModel.setPacked(route.tripId, item.id, packed) },
            onDeleteItem = { item -> viewModel.deleteItem(route.tripId, item.id) },
            onUnpackAll = { viewModel.unpackAll(route.tripId) },
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
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(windowTitle = "Add packing item"),
        ),
    ) { route ->
        AddItemDialogScreen(
            onDismiss = { navigator.goBack() },
            onAdd = { name, category, quantity ->
                viewModel.addItem(route.tripId, name, category, quantity) {
                    navigator.goBack()
                }
            },
        )
    }
    entry<DeletePackTripDialogRoute>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(windowTitle = "Delete trip"),
        ),
    ) { route ->
        DeleteTripDialogScreen(
            tripName = route.tripName,
            onDismiss = { navigator.goBack() },
            onConfirm = {
                viewModel.deleteTrip(route.tripId) {
                    navigator.selectTopLevel(PackTripsRoute)
                }
            },
        )
    }
}
