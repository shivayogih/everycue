package com.everycue.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.everycue.core.designsystem.EveryCueTheme
import com.everycue.feature.pack.PackViewModel
import com.everycue.feature.renew.RenewViewModel
import com.everycue.feature.track.TrackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as EveryCueApplication

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

            EveryCueTheme {
                EveryCueApp(
                    trackViewModel = trackViewModel,
                    packViewModel = packViewModel,
                    renewViewModel = renewViewModel,
                    onExit = { finish() },
                )
            }
        }
    }
}
