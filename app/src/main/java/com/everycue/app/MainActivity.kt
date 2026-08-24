package com.everycue.app

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.everycue.core.designsystem.EveryCueTheme
import com.everycue.feature.pack.PackViewModel
import com.everycue.feature.renew.RenewViewModel
import com.everycue.feature.track.TrackViewModel

class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<AppDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!BuildConfig.ALLOW_INSECURE_DEVICE) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            window.decorView.filterTouchesWhenObscured = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setHideOverlayWindows(true)
        }
        val securityVerdict = DeviceSecurityGuard.verdict()
        if (securityVerdict is DeviceSecurityVerdict.Blocked) {
            setContent {
                EveryCueTheme { SecurityBlockedScreen(securityVerdict.reason, onClose = ::finishAndRemoveTask) }
            }
            return
        }
        val app = application as EveryCueApplication
        deepLink = intent.toEveryCueDeepLink()

        setContent {
            val trackViewModel: TrackViewModel = viewModel(
                factory = TrackViewModel.Factory(app.trackRepository),
            )
            val packViewModel: PackViewModel = viewModel(
                factory = PackViewModel.Factory(app.packRepository),
            )
            val renewViewModel: RenewViewModel = viewModel(
                factory = RenewViewModel.Factory(app.renewRepository),
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.settingsRepository, app.backupRepository),
            )
            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModel.Factory(app.userProfileRepository),
            )
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
            val profileState by profileViewModel.state.collectAsStateWithLifecycle()
            val darkTheme = when (settingsState.settings.theme) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            EveryCueTheme(darkTheme = darkTheme, dynamicColor = settingsState.settings.dynamicColor) {
                if (!profileState.isLoaded) {
                    ProfileLoadingScreen()
                } else if (profileState.profile == null) {
                    FirstRunFlow(state = profileState, onIntent = profileViewModel::onIntent)
                } else EveryCueApp(
                    trackViewModel = trackViewModel,
                    packViewModel = packViewModel,
                    renewViewModel = renewViewModel,
                    settingsViewModel = settingsViewModel,
                    profileViewModel = profileViewModel,
                    deepLink = deepLink,
                    onDeepLinkConsumed = { deepLink = null },
                    onExit = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.toEveryCueDeepLink()
    }
}

data class AppDeepLink(val destination: String, val itemId: String)

private fun Intent.toEveryCueDeepLink(): AppDeepLink? {
    val destination = getStringExtra(EXTRA_DESTINATION) ?: return null
    val itemId = getStringExtra(EXTRA_ITEM_ID) ?: return null
    return AppDeepLink(destination, itemId)
}
