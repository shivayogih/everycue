package com.everycue.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    trackCount: Int,
    tripCount: Int,
    renewalCount: Int,
    onAbout: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text("Private and offline first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "The foundation stores Track and Renew records in a local Room database and Pack lists in local DataStore. No account, ads or analytics SDK is included.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Local data summary") },
                        supportingContent = { Text("$trackCount active tracked items • $tripCount trips • $renewalCount renewals") },
                        leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                    )
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Reminders") },
                        supportingContent = { Text("WorkManager notifications and deep links are the next milestone.") },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    )
                }
            }
            item {
                ElevatedCard(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("About EveryCue") },
                        supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("EveryCue", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Track it. Pack it. Renew it.", style = MaterialTheme.typography.titleMedium)
            Text(
                "An offline-first everyday organizer built with Kotlin, Jetpack Compose, Room, DataStore and Navigation 3.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("Version ${BuildConfig.VERSION_NAME}")
        }
    }
}
