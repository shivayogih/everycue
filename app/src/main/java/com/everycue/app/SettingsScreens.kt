package com.everycue.app

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    trackCount: Int,
    tripCount: Int,
    renewalCount: Int,
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onProfile: () -> Unit,
    onAbout: () -> Unit,
) {
    val settings = state.settings
    val busy = state.isBusy
    val context = LocalContext.current
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) onIntent(SettingsIntent.SetReminders(true)) }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { uri -> onIntent(SettingsIntent.Export(uri)) } }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) pendingImport = it }
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.restore_backup_title)) },
            text = { Text(stringResource(R.string.restore_backup_message)) },
            confirmButton = { TextButton(onClick = { onIntent(SettingsIntent.Import(uri)); pendingImport = null }) { Text(stringResource(R.string.restore)) } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            onDismiss = { showTimePicker = false },
            onSelected = { hour, minute ->
                onIntent(SettingsIntent.SetReminderTime(hour, minute))
                showTimePicker = false
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.privacy_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SummaryCard(trackCount, tripCount, renewalCount) }
            item {
                ElevatedCard(onClick = onProfile, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.local_profile)) },
                        supportingContent = { Text(stringResource(R.string.local_profile_summary)) },
                        leadingContent = { Icon(Icons.Default.Person, null) },
                    )
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        ListItem(headlineContent = { Text(stringResource(R.string.appearance)) }, leadingContent = { Icon(Icons.Default.Palette, null) })
                        ThemePreference.entries.forEach { value ->
                            ListItem(
                                headlineContent = { Text(stringResource(when (value) { ThemePreference.SYSTEM -> R.string.theme_system; ThemePreference.LIGHT -> R.string.theme_light; ThemePreference.DARK -> R.string.theme_dark })) },
                                leadingContent = { RadioButton(settings.theme == value, { onIntent(SettingsIntent.SetTheme(value)) }) },
                            )
                        }
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.device_colors)) },
                            supportingContent = { Text(stringResource(R.string.device_colors_summary)) },
                            trailingContent = { Switch(settings.dynamicColor, { onIntent(SettingsIntent.SetDynamicColor(it)) }) },
                        )
                    }
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.daily_reminders)) },
                            supportingContent = { Text(stringResource(R.string.daily_reminders_summary, settings.reminderTimeLabel())) },
                            leadingContent = { Icon(Icons.Default.Notifications, null) },
                            trailingContent = {
                                Switch(checked = settings.remindersEnabled, onCheckedChange = { enabled ->
                                    if (!enabled) onIntent(SettingsIntent.SetReminders(false))
                                    else if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else onIntent(SettingsIntent.SetReminders(true))
                                })
                            },
                        )
                        if (settings.remindersEnabled) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(settings.reminderTimeLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = { showTimePicker = true }) {
                                    Text(stringResource(R.string.change_reminder_time))
                                }
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.backup_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { exportFile.launch("everycue-backup-${LocalDate.now()}.json") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Icon(Icons.Default.Backup, null); Text(stringResource(R.string.export_backup))
                        }
                        OutlinedButton({ importFile.launch(arrayOf("application/json", "text/plain")) }, Modifier.fillMaxWidth(), enabled = !busy) {
                            Icon(Icons.Default.Restore, null); Text(stringResource(R.string.restore_backup))
                        }
                        if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
            item {
                ElevatedCard(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.about_everycue)) },
                        supportingContent = { Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME)) },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onSelected: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_reminder_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onSelected(state.hour, state.minute) }) {
                Text(stringResource(R.string.select))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SummaryCard(trackCount: Int, tripCount: Int, renewalCount: Int) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.local_data_summary)) },
            supportingContent = { Text(stringResource(R.string.local_counts_format, trackCount, tripCount, renewalCount)) },
            leadingContent = { Icon(Icons.Default.Storage, null) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.about_title)) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
        }
    }
}
