package com.everycue.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.everycue.core.navigation.Navigator
import com.everycue.core.navigation.rememberNavigationState
import com.everycue.feature.pack.PackTripsRoute
import com.everycue.feature.pack.PackViewModel
import com.everycue.feature.pack.packEntryBuilder
import com.everycue.feature.renew.RenewHomeRoute
import com.everycue.feature.renew.RenewViewModel
import com.everycue.feature.renew.renewEntryBuilder
import com.everycue.feature.track.TrackHomeRoute
import com.everycue.feature.track.TrackViewModel
import com.everycue.feature.track.trackEntryBuilder

private data class TopDestination(
    val route: NavKey,
    val label: String,
    val icon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(TrackHomeRoute, "Track", Icons.Default.Inventory2),
    TopDestination(PackTripsRoute, "Pack", Icons.Default.Luggage),
    TopDestination(RenewHomeRoute, "Renew", Icons.Default.EventRepeat),
    TopDestination(SettingsRoute, "Settings", Icons.Default.Settings),
)

private val topLevelRoutes: Set<NavKey> = topDestinations.mapTo(linkedSetOf()) { it.route }

@Composable
fun EveryCueApp(
    trackViewModel: TrackViewModel,
    packViewModel: PackViewModel,
    renewViewModel: RenewViewModel,
    onExit: () -> Unit,
) {
    val trackState by trackViewModel.state.collectAsStateWithLifecycle()
    val packData by packViewModel.data.collectAsStateWithLifecycle()
    val renewState by renewViewModel.state.collectAsStateWithLifecycle()

    val navigationState = rememberNavigationState(
        startRoute = TrackHomeRoute,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

    val entries = entryProvider {
        trackEntryBuilder(trackState, trackViewModel, navigator)
        packEntryBuilder(packData, packViewModel, navigator)
        renewEntryBuilder(renewState, renewViewModel, navigator)
        entry<SettingsRoute> {
            SettingsScreen(
                trackCount = trackState.items.size,
                tripCount = packData.trips.size,
                renewalCount = renewState.renewals.size,
                onAbout = { navigator.navigate(AboutRoute) },
            )
        }
        entry<AboutRoute> {
            AboutScreen(onBack = { navigator.goBack() })
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (navigationState.currentRoute in topLevelRoutes) {
                NavigationBar {
                    topDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = navigationState.topLevelRoute == destination.route,
                            onClick = { navigator.selectTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            entries = navigationState.toDecoratedEntries(entries),
            onBack = {
                if (!navigator.goBack()) onExit()
            },
            sceneStrategies = listOf(dialogStrategy),
            modifier = Modifier.padding(padding),
        )
    }
}
