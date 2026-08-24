package com.everycue.app

import android.content.Intent
import androidx.annotation.StringRes
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
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
import com.everycue.feature.pack.PackTripDetailRoute
import com.everycue.feature.pack.PackEffect
import com.everycue.feature.pack.PackViewModel
import com.everycue.feature.pack.packEntryBuilder
import com.everycue.feature.renew.RenewHomeRoute
import com.everycue.feature.renew.RenewDetailRoute
import com.everycue.feature.renew.RenewViewModel
import com.everycue.feature.renew.RenewEffect
import com.everycue.feature.renew.renewEntryBuilder
import com.everycue.feature.track.TrackHomeRoute
import com.everycue.feature.track.TrackDetailRoute
import com.everycue.feature.track.TrackViewModel
import com.everycue.feature.track.TrackEffect
import com.everycue.feature.track.trackEntryBuilder

private data class TopDestination(
    val route: NavKey,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(TrackHomeRoute, R.string.nav_track, Icons.Default.Inventory2),
    TopDestination(PackTripsRoute, R.string.nav_pack, Icons.Default.Luggage),
    TopDestination(RenewHomeRoute, R.string.nav_renew, Icons.Default.EventRepeat),
    TopDestination(SettingsRoute, R.string.nav_settings, Icons.Default.Settings),
)

private val topLevelRoutes: Set<NavKey> = topDestinations.mapTo(linkedSetOf()) { it.route }

@Composable
fun EveryCueApp(
    trackViewModel: TrackViewModel,
    packViewModel: PackViewModel,
    renewViewModel: RenewViewModel,
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    deepLink: AppDeepLink?,
    onDeepLinkConsumed: () -> Unit,
    onExit: () -> Unit,
) {
    val trackState by trackViewModel.state.collectAsStateWithLifecycle()
    val packState by packViewModel.state.collectAsStateWithLifecycle()
    val packData = packState.data
    val renewState by renewViewModel.state.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    val profileState by profileViewModel.state.collectAsStateWithLifecycle()

    val navigationState = rememberNavigationState(
        startRoute = TrackHomeRoute,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(trackViewModel, navigator, resources) {
        trackViewModel.effects.collect { effect ->
            when (effect) {
                is TrackEffect.Saved -> if (effect.wasEditing) navigator.goBack()
                    else navigator.openInTopLevel(TrackHomeRoute, TrackDetailRoute(effect.itemId))
                TrackEffect.OutcomeRecorded, TrackEffect.Deleted -> navigator.selectTopLevel(TrackHomeRoute)
                is TrackEffect.ShowError -> snackbar.showSnackbar(resources.getString(effect.messageResource))
            }
        }
    }
    LaunchedEffect(packViewModel, navigator, resources) {
        packViewModel.effects.collect { effect ->
            when (effect) {
                is PackEffect.TripCreated -> navigator.openInTopLevel(PackTripsRoute, PackTripDetailRoute(effect.tripId))
                PackEffect.ItemAdded -> navigator.goBack()
                PackEffect.TripUpdated -> navigator.goBack()
                PackEffect.TripDeleted, PackEffect.Reset -> navigator.selectTopLevel(PackTripsRoute)
                is PackEffect.ShowError -> snackbar.showSnackbar(resources.getString(effect.messageResource))
            }
        }
    }
    LaunchedEffect(renewViewModel, navigator, resources) {
        renewViewModel.effects.collect { effect ->
            when (effect) {
                is RenewEffect.Saved -> if (effect.wasEditing) navigator.goBack()
                    else navigator.openInTopLevel(RenewHomeRoute, RenewDetailRoute(effect.renewalId))
                RenewEffect.MarkedRenewed -> navigator.goBack()
                RenewEffect.Deleted -> navigator.selectTopLevel(RenewHomeRoute)
                is RenewEffect.ShowError -> snackbar.showSnackbar(resources.getString(effect.messageResource))
            }
        }
    }
    LaunchedEffect(settingsViewModel, resources) {
        settingsViewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> snackbar.showSnackbar(resources.getString(effect.message))
            }
        }
    }
    LaunchedEffect(profileViewModel, navigator, resources) {
        profileViewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.Saved -> {
                    navigator.goBack()
                    snackbar.showSnackbar(resources.getString(R.string.profile_saved))
                }
                is ProfileEffect.Share -> {
                    val profile = effect.profile
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.profile_share_subject, profile.fullName))
                        putExtra(
                            Intent.EXTRA_TEXT,
                            profile.asShareText(
                                phone = resources.getString(R.string.profile_share_phone, profile.countryCode, profile.mobileNumber),
                                emailLabel = resources.getString(R.string.profile_share_email, profile.email),
                                addressLabel = resources.getString(R.string.profile_share_address),
                                pincodeLabel = resources.getString(R.string.profile_share_pincode, profile.pincode),
                            ),
                        )
                    }
                    context.startActivity(Intent.createChooser(send, resources.getString(R.string.share_profile_chooser)))
                }
            }
        }
    }

    LaunchedEffect(deepLink) {
        when (deepLink?.destination) {
            DESTINATION_TRACK -> navigator.openInTopLevel(TrackHomeRoute, TrackDetailRoute(deepLink.itemId))
            DESTINATION_RENEW -> navigator.openInTopLevel(RenewHomeRoute, RenewDetailRoute(deepLink.itemId))
        }
        if (deepLink != null) onDeepLinkConsumed()
    }

    val entries = entryProvider {
        trackEntryBuilder(trackState, trackViewModel, navigator)
        packEntryBuilder(packData, packViewModel, navigator)
        renewEntryBuilder(renewState, renewViewModel, navigator)
        entry<SettingsRoute> {
            SettingsScreen(
                trackCount = trackState.items.size,
                tripCount = packData.trips.size,
                renewalCount = renewState.renewals.size,
                state = settingsState,
                onIntent = settingsViewModel::onIntent,
                onProfile = { navigator.navigate(ProfileRoute) },
                onAbout = { navigator.navigate(AboutRoute) },
            )
        }
        entry<AboutRoute> {
            AboutScreen(onBack = { navigator.goBack() })
        }
        entry<ProfileRoute> {
            ProfileScreen(
                state = profileState,
                onIntent = profileViewModel::onIntent,
                onBack = { navigator.goBack() },
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (navigationState.currentRoute in topLevelRoutes) {
                NavigationBar {
                    topDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = navigationState.topLevelRoute == destination.route,
                            onClick = { navigator.selectTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = stringResource(destination.label)) },
                            label = { Text(stringResource(destination.label)) },
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

