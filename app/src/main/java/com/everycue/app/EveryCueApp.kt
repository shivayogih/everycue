package com.everycue.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.core.content.FileProvider
import com.everycue.core.attachments.AttachmentError
import com.everycue.core.attachments.AttachmentException
import com.everycue.core.attachments.AttachmentImportRequest
import com.everycue.core.attachments.AttachmentOwner
import com.everycue.core.attachments.AttachmentOwnerType
import com.everycue.core.attachments.AttachmentSource
import com.everycue.core.navigation.Navigator
import com.everycue.core.extraction.ExtractionSourceType
import com.everycue.core.vision.OnDeviceVision
import com.everycue.core.vision.VisionFailure
import com.everycue.core.navigation.rememberNavigationState
import com.everycue.core.designsystem.rememberKeyboardDismissAction
import com.everycue.feature.pack.PackTripsRoute
import com.everycue.feature.pack.PackTripDetailRoute
import com.everycue.feature.pack.PackEffect
import com.everycue.feature.pack.PackViewModel
import com.everycue.feature.pack.packEntryBuilder
import com.everycue.feature.renew.RenewHomeRoute
import com.everycue.feature.renew.RenewDetailRoute
import com.everycue.feature.renew.RenewViewModel
import com.everycue.feature.renew.RenewEffect
import com.everycue.feature.renew.RenewIntent
import com.everycue.feature.renew.RenewalAttachment
import com.everycue.feature.renew.renewEntryBuilder
import com.everycue.feature.track.TrackHomeRoute
import com.everycue.feature.track.TrackDetailRoute
import com.everycue.feature.track.TrackViewModel
import com.everycue.feature.track.TrackEffect
import com.everycue.feature.track.TrackIntent
import com.everycue.feature.track.SmartAddFailure
import com.everycue.feature.track.trackEntryBuilder
import java.io.File
import kotlinx.coroutines.launch

private data class TopDestination(
    val route: NavKey,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(HomeRoute, R.string.nav_home, Icons.Default.Home),
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
    // Pass the observable State holders into Navigation 3 entries. Decorated entries are
    // retained, so capturing only their current values would leave an active screen stale.
    val trackState = trackViewModel.state.collectAsStateWithLifecycle()
    val packState = packViewModel.state.collectAsStateWithLifecycle()
    val renewState = renewViewModel.state.collectAsStateWithLifecycle()
    val settingsState = settingsViewModel.state.collectAsStateWithLifecycle()
    val profileState = profileViewModel.state.collectAsStateWithLifecycle()

    val navigationState = rememberNavigationState(
        startRoute = HomeRoute,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val application = context.applicationContext as EveryCueApplication
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    var renewalAttachmentTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var renewalCapturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var trackLabelCapturePath by rememberSaveable { mutableStateOf<String?>(null) }
    val currentRoute = navigationState.currentRoute
    val dismissKeyboard = rememberKeyboardDismissAction()
    val vision = remember(context) { OnDeviceVision(context) }
    DisposableEffect(vision) { onDispose(vision::close) }
    val importLabelImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vision.recognizeText(
                uri = uri,
                onResult = { trackViewModel.onIntent(TrackIntent.ApplyRecognizedText(it, ExtractionSourceType.IMAGE_OCR)) },
                onFailure = { trackViewModel.onIntent(TrackIntent.SmartAddFailed(it.toSmartAddFailure())) },
            )
        }
    }
    val captureLabel = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val capture = trackLabelCapturePath?.let(::File)
        trackLabelCapturePath = null
        if (capture != null && saved) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", capture)
            vision.recognizeText(
                uri = uri,
                onResult = {
                    capture.delete()
                    trackViewModel.onIntent(TrackIntent.ApplyRecognizedText(it, ExtractionSourceType.CAMERA_OCR))
                },
                onFailure = {
                    capture.delete()
                    trackViewModel.onIntent(TrackIntent.SmartAddFailed(it.toSmartAddFailure()))
                },
            )
        } else {
            capture?.delete()
        }
    }
    val chooseRenewalAttachments = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val renewalId = renewalAttachmentTargetId
        renewalAttachmentTargetId = null
        if (renewalId != null && uris.isNotEmpty()) {
            coroutineScope.launch {
                uris.forEach { uri ->
                    val source = if (context.contentResolver.getType(uri)?.startsWith("image/") == true) {
                        AttachmentSource.GALLERY
                    } else {
                        AttachmentSource.DOCUMENT
                    }
                    runCatching {
                        application.attachmentStore.import(
                            AttachmentImportRequest(
                                owner = AttachmentOwner(AttachmentOwnerType.RENEWAL, renewalId),
                                sourceUri = uri,
                                source = source,
                            ),
                        )
                    }.onSuccess { attachment ->
                        renewViewModel.onIntent(RenewIntent.AddAttachment(attachment))
                    }.onFailure { error ->
                        renewViewModel.onIntent(
                            RenewIntent.AttachmentImportFailed(
                                (error as? AttachmentException)?.error ?: AttachmentError.COPY_FAILED,
                            ),
                        )
                    }
                }
            }
        }
    }
    val captureRenewalPhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val renewalId = renewalAttachmentTargetId
        val capture = renewalCapturePath?.let(::File)
        renewalAttachmentTargetId = null
        renewalCapturePath = null
        if (renewalId != null && capture != null && saved) {
            coroutineScope.launch {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", capture)
                    val attachment = application.attachmentStore.import(
                        AttachmentImportRequest(
                            owner = AttachmentOwner(AttachmentOwnerType.RENEWAL, renewalId),
                            sourceUri = uri,
                            source = AttachmentSource.CAMERA,
                            displayNameHint = resources.getString(R.string.renewal_photo_name),
                            mimeTypeHint = "image/jpeg",
                        ),
                    )
                    renewViewModel.onIntent(RenewIntent.AddAttachment(attachment))
                } catch (error: Throwable) {
                    renewViewModel.onIntent(
                        RenewIntent.AttachmentImportFailed(
                            (error as? AttachmentException)?.error ?: AttachmentError.COPY_FAILED,
                        ),
                    )
                } finally {
                    runCatching { application.attachmentStore.removeTemporaryCapture(capture) }
                }
            }
        } else if (capture != null) {
            runCatching { application.attachmentStore.removeTemporaryCapture(capture) }
        }
    }

    // Retained navigation entries can retain text-field focus. Never carry the IME to a
    // different screen, and also close it after Activity recreation/rotation.
    LaunchedEffect(currentRoute, dismissKeyboard) { dismissKeyboard() }

    LaunchedEffect(trackViewModel, navigator, resources) {
        trackViewModel.effects.collect { effect ->
            when (effect) {
                is TrackEffect.Saved -> {
                    if (effect.wasEditing) navigator.goBack()
                    else navigator.openInTopLevel(TrackHomeRoute, TrackDetailRoute(effect.itemId))
                    snackbar.showSnackbar(resources.getString(R.string.track_item_saved))
                }
                TrackEffect.OutcomeRecorded -> {
                    navigator.selectTopLevel(TrackHomeRoute)
                    snackbar.showSnackbar(resources.getString(R.string.track_outcome_saved))
                }
                TrackEffect.Deleted -> {
                    navigator.selectTopLevel(TrackHomeRoute)
                    snackbar.showSnackbar(resources.getString(R.string.track_item_deleted))
                }
                is TrackEffect.ShowError -> snackbar.showSnackbar(resources.getString(effect.messageResource))
            }
        }
    }
    LaunchedEffect(packViewModel, navigator, resources) {
        packViewModel.effects.collect { effect ->
            when (effect) {
                is PackEffect.TripCreated -> navigator.openInTopLevel(PackTripsRoute, PackTripDetailRoute(effect.tripId))
                PackEffect.ItemAdded -> snackbar.showSnackbar(resources.getString(R.string.pack_item_added))
                PackEffect.TripUpdated -> navigator.goBack()
                PackEffect.TripDeleted, PackEffect.Reset -> navigator.selectTopLevel(PackTripsRoute)
                is PackEffect.ShowError -> snackbar.showSnackbar(resources.getString(effect.messageResource))
            }
        }
    }
    LaunchedEffect(renewViewModel, navigator, resources) {
        renewViewModel.effects.collect { effect ->
            when (effect) {
                is RenewEffect.Saved -> {
                    if (effect.wasEditing) navigator.goBack()
                    else navigator.openInTopLevel(RenewHomeRoute, RenewDetailRoute(effect.renewalId))
                    snackbar.showSnackbar(resources.getString(R.string.renewal_saved))
                }
                RenewEffect.MarkedRenewed -> {
                    navigator.goBack()
                    snackbar.showSnackbar(resources.getString(R.string.renewal_completed))
                }
                RenewEffect.Deleted -> {
                    navigator.selectTopLevel(RenewHomeRoute)
                    snackbar.showSnackbar(resources.getString(R.string.renewal_deleted))
                }
                RenewEffect.AttachmentAdded -> snackbar.showSnackbar(
                    resources.getString(com.everycue.feature.renew.R.string.attachment_added),
                )
                RenewEffect.AttachmentRemoved -> snackbar.showSnackbar(
                    resources.getString(com.everycue.feature.renew.R.string.attachment_removed),
                )
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
        entry<HomeRoute> {
            HomeScreen(
                profile = profileState.value.profile,
                track = trackState.value,
                pack = packState.value.data,
                renew = renewState.value,
                onTrack = { navigator.selectTopLevel(TrackHomeRoute) },
                onPack = { navigator.selectTopLevel(PackTripsRoute) },
                onRenew = { navigator.selectTopLevel(RenewHomeRoute) },
                onTrackItem = { navigator.openInTopLevel(TrackHomeRoute, TrackDetailRoute(it)) },
                onPackTrip = { navigator.openInTopLevel(PackTripsRoute, PackTripDetailRoute(it)) },
                onRenewal = { navigator.openInTopLevel(RenewHomeRoute, RenewDetailRoute(it)) },
            )
        }
        trackEntryBuilder(
            state = trackState,
            viewModel = trackViewModel,
            navigator = navigator,
            onScanBarcode = {
                vision.scanBarcode(
                    onResult = { trackViewModel.onIntent(TrackIntent.ApplyBarcode(it)) },
                    onFailure = { trackViewModel.onIntent(TrackIntent.SmartAddFailed(it.toSmartAddFailure())) },
                )
            },
            onScanLabel = {
                runCatching { createTrackLabelCapture(context.cacheDir) }
                    .onSuccess { capture ->
                        trackLabelCapturePath = capture.absolutePath
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", capture)
                        captureLabel.launch(uri)
                    }
                    .onFailure {
                        trackViewModel.onIntent(TrackIntent.SmartAddFailed(SmartAddFailure.IMAGE_UNREADABLE))
                    }
            },
            onImportImage = { importLabelImage.launch(arrayOf("image/*")) },
        )
        packEntryBuilder(packState, packViewModel, navigator)
        renewEntryBuilder(
            state = renewState,
            viewModel = renewViewModel,
            navigator = navigator,
            onAddFiles = { renewalId ->
                renewalAttachmentTargetId = renewalId
                chooseRenewalAttachments.launch(
                    arrayOf(
                        "application/pdf",
                        "image/jpeg",
                        "image/png",
                        "image/webp",
                        "image/heic",
                        "image/heif",
                        "text/plain",
                    ),
                )
            },
            onTakePhoto = { renewalId ->
                runCatching {
                    application.attachmentStore.createTemporaryCaptureFile()
                }.onSuccess { capture ->
                    renewalAttachmentTargetId = renewalId
                    renewalCapturePath = capture.absolutePath
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", capture)
                    captureRenewalPhoto.launch(uri)
                }.onFailure { error ->
                    renewViewModel.onIntent(
                        RenewIntent.AttachmentImportFailed(
                            (error as? AttachmentException)?.error ?: AttachmentError.COPY_FAILED,
                        ),
                    )
                }
            },
            onViewAttachment = { attachment ->
                openRenewalAttachment(
                    attachment = attachment,
                    application = application,
                    context = context,
                    chooserTitle = resources.getString(com.everycue.feature.renew.R.string.view_attachment_chooser),
                    onFailure = {
                        coroutineScope.launch {
                            snackbar.showSnackbar(resources.getString(com.everycue.feature.renew.R.string.attachment_open_failed))
                        }
                    },
                )
            },
            onShareAttachment = { attachment ->
                shareRenewalAttachment(
                    attachment = attachment,
                    application = application,
                    context = context,
                    chooserTitle = resources.getString(com.everycue.feature.renew.R.string.share_attachment_chooser),
                    onFailure = {
                        coroutineScope.launch {
                            snackbar.showSnackbar(resources.getString(com.everycue.feature.renew.R.string.attachment_share_failed))
                        }
                    },
                )
            },
        )
        entry<SettingsRoute> {
            val track = trackState.value
            val pack = packState.value
            val renew = renewState.value
            SettingsScreen(
                trackCount = track.items.size,
                tripCount = pack.data.trips.size,
                renewalCount = renew.renewals.size,
                state = settingsState.value,
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
                state = profileState.value,
                onIntent = profileViewModel::onIntent,
                onBack = { navigator.goBack() },
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
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

private fun openRenewalAttachment(
    attachment: RenewalAttachment,
    application: EveryCueApplication,
    context: android.content.Context,
    chooserTitle: String,
    onFailure: () -> Unit,
) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            application.attachmentStore.contentFile(attachment.asLocalAttachment()),
            attachment.displayName,
        )
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            clipData = ClipData.newUri(context.contentResolver, attachment.displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(view, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        onFailure()
    } catch (_: RuntimeException) {
        onFailure()
    }
}

private fun shareRenewalAttachment(
    attachment: RenewalAttachment,
    application: EveryCueApplication,
    context: android.content.Context,
    chooserTitle: String,
    onFailure: () -> Unit,
) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            application.attachmentStore.contentFile(attachment.asLocalAttachment()),
            attachment.displayName,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = attachment.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, attachment.displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        onFailure()
    } catch (_: RuntimeException) {
        onFailure()
    }
}

private fun createTrackLabelCapture(cacheDir: File): File {
    val captureDirectory = File(cacheDir, "everycue_captures")
    check(captureDirectory.exists() || captureDirectory.mkdirs())
    return File.createTempFile("track_label_", ".jpg", captureDirectory)
}

private fun VisionFailure.toSmartAddFailure(): SmartAddFailure = when (this) {
    VisionFailure.MODEL_UNAVAILABLE -> SmartAddFailure.MODEL_UNAVAILABLE
    VisionFailure.IMAGE_UNREADABLE -> SmartAddFailure.IMAGE_UNREADABLE
    VisionFailure.NO_RESULT -> SmartAddFailure.NO_RESULT
    VisionFailure.CANCELLED -> SmartAddFailure.CANCELLED
}

