package com.everycue.feature.renew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everycue.core.designsystem.EmptyState
import com.everycue.core.designsystem.MetricCard
import com.everycue.core.designsystem.SectionHeader
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewHomeScreen(
    state: RenewUiState,
    onAdd: () -> Unit,
    onOpenAll: () -> Unit,
    onOpenRenewal: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Renew", fontWeight = FontWeight.Bold)
                        Text(
                            "Keep important dates in view",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAll) { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "All renewals") }
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Default.History, contentDescription = "Renewal history") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add renewal") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Overdue", state.overdueCount.toString(), Modifier.weight(1f))
                    MetricCard("Due soon", state.dueSoonCount.toString(), Modifier.weight(1f))
                    MetricCard("Upcoming", state.upcomingCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                SectionHeader(
                    title = "Needs attention",
                    action = { TextButton(onClick = onOpenAll) { Text("View all") } },
                )
            }
            if (state.renewals.isEmpty()) {
                item {
                    EmptyState(
                        title = "No renewal dates yet",
                        message = "Track passports, licences, insurance, warranties, memberships and other important due dates.",
                        action = { Button(onClick = onAdd) { Text("Add first renewal") } },
                    )
                }
            } else if (state.urgent.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nothing is due soon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Your next renewal is outside its reminder window.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.urgent, key = RenewalItem::id) { item -> RenewalCard(item, { onOpenRenewal(item.id) }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewListScreen(
    renewals: List<RenewalItem>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedType = selectedTypeName?.let { RenewalType.valueOf(it) }
    val visible = renewals.filter {
        (query.isBlank() || it.title.contains(query, true) || it.provider.contains(query, true)) &&
            (selectedType == null || it.type == selectedType)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("All renewals") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { OutlinedTextField(query, { query = it }, label = { Text("Search title or provider") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                TypeChips(
                    selected = selectedType,
                    onSelected = { type -> selectedTypeName = if (selectedType == type) null else type.name },
                )
            }
            if (visible.isEmpty()) {
                item { EmptyState(if (renewals.isEmpty()) "No renewals" else "No matching renewals", "Add a due date or clear the current filters.") }
            } else {
                items(visible, key = RenewalItem::id) { RenewalCard(it, { onOpen(it.id) }) }
            }
        }
    }
}

@Composable
private fun TypeChips(selected: RenewalType?, onSelected: (RenewalType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RenewalType.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    FilterChip(
                        selected = selected == type,
                        onClick = { onSelected(type) },
                        label = { Text("${type.emoji} ${type.label}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun RenewalCard(item: RenewalItem, onClick: () -> Unit) {
    val state = item.dueState()
    val container = when (state) {
        DueState.OVERDUE -> MaterialTheme.colorScheme.errorContainer
        DueState.DUE_TODAY, DueState.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer
        DueState.UPCOMING -> MaterialTheme.colorScheme.surfaceContainer
    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(item.type.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.dueMessage())
                Text(
                    buildList {
                        add(item.dueEpochDay.asRenewDateLabel())
                        if (item.provider.isNotBlank()) add(item.provider)
                    }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewEditorScreen(
    existing: RenewalItem?,
    onBack: () -> Unit,
    onSave: (RenewalDraft) -> Unit,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var typeName by rememberSaveable(existing?.id) { mutableStateOf(existing?.type?.name ?: RenewalType.DOCUMENT.name) }
    var dueEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueEpochDay ?: LocalDate.now().plusMonths(1).toEpochDay()) }
    var reminderDays by rememberSaveable(existing?.id) { mutableStateOf((existing?.reminderDays ?: DEFAULT_RENEW_WARNING_DAYS).toString()) }
    var provider by rememberSaveable(existing?.id) { mutableStateOf(existing?.provider.orEmpty()) }
    var reference by rememberSaveable(existing?.id) { mutableStateOf(existing?.referenceNumber.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val reminder = reminderDays.toIntOrNull()
    val valid = title.isNotBlank() && reminder != null && reminder >= 0

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existing == null) "Add renewal" else "Edit renewal") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        onSave(
                            RenewalDraft(
                                title = title,
                                type = RenewalType.valueOf(typeName),
                                dueEpochDay = dueEpochDay,
                                reminderDays = reminder ?: DEFAULT_RENEW_WARNING_DAYS,
                                provider = provider,
                                referenceNumber = reference,
                                notes = notes,
                            ),
                        )
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) { Text(if (existing == null) "Save renewal" else "Save changes") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, placeholder = { Text("Passport, vehicle insurance…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TypeChips(selected = RenewalType.valueOf(typeName), onSelected = { typeName = it.name })
            ElevatedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Due date", style = MaterialTheme.typography.labelMedium)
                    Text(dueEpochDay.asRenewDateLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
            OutlinedTextField(reminderDays, { reminderDays = it }, label = { Text("Remind before (days)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(provider, { provider = it }, label = { Text("Provider / issuer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(reference, { reference = it }, label = { Text("Reference number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showDatePicker) {
        RenewDatePickerDialog(
            initialEpochDay = dueEpochDay,
            onDismiss = { showDatePicker = false },
            onSelected = { selected -> dueEpochDay = selected; showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenewDatePickerDialog(
    initialEpochDay: Long,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = renewEpochDayToPickerMillis(initialEpochDay))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { renewPickerMillisToEpochDay(state.selectedDateMillis)?.let(onSelected) }) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewDetailScreen(
    item: RenewalItem?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRenew: () -> Unit,
    onDelete: () -> Unit,
) {
    if (item == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Renewal unavailable") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
            EmptyState("This renewal is unavailable", "It may have been deleted.", Modifier.padding(padding))
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when (item.dueState()) {
                        DueState.OVERDUE -> MaterialTheme.colorScheme.errorContainer
                        DueState.DUE_TODAY, DueState.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer
                        DueState.UPCOMING -> MaterialTheme.colorScheme.primaryContainer
                    },
                ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.type.emoji} ${item.type.label}", style = MaterialTheme.typography.labelLarge)
                    Text(item.dueMessage(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Due: ${item.dueEpochDay.asRenewDateLabel()}")
                }
            }
            RenewDetailRow("Reminder", "${item.reminderDays} days before")
            if (item.provider.isNotBlank()) RenewDetailRow("Provider / issuer", item.provider)
            if (item.referenceNumber.isNotBlank()) RenewDetailRow("Reference", item.referenceNumber)
            item.lastRenewedEpochDay?.let { RenewDetailRow("Last renewed", it.asRenewDateLabel()) }
            if (item.notes.isNotBlank()) RenewDetailRow("Notes", item.notes)
            Button(onClick = onRenew, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("  Mark renewed")
            }
        }
    }
}

@Composable
private fun RenewDetailRow(label: String, value: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkRenewedScreen(
    item: RenewalItem?,
    onBack: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    if (item == null) {
        RenewDetailScreen(null, onBack, {}, {}, {})
        return
    }
    var newDueEpochDay by rememberSaveable(item.id) { mutableStateOf(LocalDate.ofEpochDay(item.dueEpochDay).plusYears(1).toEpochDay()) }
    var notes by rememberSaveable(item.id) { mutableStateOf("") }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mark renewed") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(onClick = { onConfirm(newDueEpochDay, notes) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) { Text("Save new due date") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("${item.title} is currently due ${item.dueEpochDay.asRenewDateLabel()}.")
            ElevatedCard(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("New due date", style = MaterialTheme.typography.labelMedium)
                    Text(newDueEpochDay.asRenewDateLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Renewal notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        }
    }
    if (showPicker) {
        RenewDatePickerDialog(newDueEpochDay, { showPicker = false }) { newDueEpochDay = it; showPicker = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewHistoryScreen(events: List<RenewalEvent>, onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Renewal history") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
    ) { padding ->
        if (events.isEmpty()) {
            EmptyState("No renewals recorded", "When you mark an item renewed, its old and new due dates appear here.", Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = RenewalEvent::id) { event ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${event.previousDueEpochDay.asRenewDateLabel()} → ${event.newDueEpochDay.asRenewDateLabel()}")
                            Text(event.renewedAtMillis.asRenewHistoryLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (event.notes.isNotBlank()) Text(event.notes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteRenewalDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text("Delete ${title.ifBlank { "renewal" }}?") },
        text = { Text("This removes the due date. Existing renewal-history entries remain local.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
