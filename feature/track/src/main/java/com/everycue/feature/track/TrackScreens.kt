package com.everycue.feature.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
fun TrackHomeScreen(
    state: TrackUiState,
    onAdd: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Track", fontWeight = FontWeight.Bold)
                        Text(
                            "Use what expires first",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add item") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Fresh", state.freshCount.toString(), Modifier.weight(1f))
                    MetricCard("Soon", state.expiringSoonCount.toString(), Modifier.weight(1f))
                    MetricCard("Expired", state.expiredCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("Inventory", Icons.Default.Inventory2, onOpenInventory, Modifier.weight(1f))
                    QuickAction("History", Icons.Default.History, onOpenHistory, Modifier.weight(1f))
                    QuickAction("Insights", Icons.Default.BarChart, onOpenInsights, Modifier.weight(1f))
                }
            }
            item {
                SectionHeader(
                    title = "Use ahead",
                    action = { TextButton(onClick = onOpenInventory) { Text("View all") } },
                )
            }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing is being tracked",
                        message = "Add groceries, medicines, cosmetics or household products and EveryCue will order them by urgency.",
                        action = { Button(onClick = onAdd) { Text("Add first item") } },
                    )
                }
            } else if (state.urgentItems.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Everything looks fresh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("No product is inside its warning window.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.urgentItems, key = TrackItem::id) { item ->
                    TrackItemCard(item = item, onClick = { onOpenItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInventoryScreen(
    items: List<TrackItem>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<TrackCategory?>(null) }
    val visible = items.filter { item ->
        (query.isBlank() || item.name.contains(query, ignoreCase = true) || item.storageLocation.contains(query, ignoreCase = true)) &&
            (selectedCategory == null || item.category == selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search name or location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyCategoryChips(
                    selected = selectedCategory,
                    onSelected = { selectedCategory = if (selectedCategory == it) null else it },
                )
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        title = if (items.isEmpty()) "Your inventory is empty" else "No matching items",
                        message = if (items.isEmpty()) "Add the first product to begin tracking." else "Clear the search or category filter.",
                    )
                }
            } else {
                items(visible, key = TrackItem::id) { item -> TrackItemCard(item, { onOpenItem(item.id) }) }
            }
        }
    }
}

@Composable
private fun LazyCategoryChips(selected: TrackCategory?, onSelected: (TrackCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TrackCategory.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    FilterChip(
                        selected = selected == category,
                        onClick = { onSelected(category) },
                        label = { Text("${category.emoji} ${category.label}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackItemCard(item: TrackItem, onClick: () -> Unit) {
    val state = item.expiryState()
    val container = when (state) {
        ExpiryState.EXPIRED -> MaterialTheme.colorScheme.errorContainer
        ExpiryState.EXPIRES_TODAY, ExpiryState.EXPIRING_SOON -> MaterialTheme.colorScheme.tertiaryContainer
        ExpiryState.FRESH -> MaterialTheme.colorScheme.surfaceContainer
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
            Text(item.category.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.expiryMessage(), style = MaterialTheme.typography.bodyMedium)
                val meta = buildList {
                    add(item.quantity.quantityLabel(item.unit))
                    if (item.storageLocation.isNotBlank()) add(item.storageLocation)
                }.joinToString(" • ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackEditorScreen(
    existing: TrackItem?,
    onBack: () -> Unit,
    onSave: (TrackDraft) -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var categoryName by rememberSaveable(existing?.id) { mutableStateOf(existing?.category?.name ?: TrackCategory.GROCERY.name) }
    var quantity by rememberSaveable(existing?.id) { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var unit by rememberSaveable(existing?.id) { mutableStateOf(existing?.unit ?: "item") }
    var purchaseEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.purchaseEpochDay) }
    var expiryEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.expiryEpochDay ?: LocalDate.now().plusDays(7).toEpochDay()) }
    var location by rememberSaveable(existing?.id) { mutableStateOf(existing?.storageLocation.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var reminderDays by rememberSaveable(existing?.id) { mutableStateOf((existing?.reminderDays ?: DEFAULT_TRACK_WARNING_DAYS).toString()) }
    var showPurchasePicker by rememberSaveable { mutableStateOf(false) }
    var showExpiryPicker by rememberSaveable { mutableStateOf(false) }

    val parsedQuantity = quantity.toDoubleOrNull()
    val parsedReminder = reminderDays.toIntOrNull()
    val valid = name.isNotBlank() && parsedQuantity != null && parsedQuantity > 0 && parsedReminder != null && parsedReminder >= 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add item" else "Edit item") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        onSave(
                            TrackDraft(
                                name = name,
                                category = TrackCategory.valueOf(categoryName),
                                quantity = parsedQuantity ?: 1.0,
                                unit = unit,
                                purchaseEpochDay = purchaseEpochDay,
                                expiryEpochDay = expiryEpochDay,
                                storageLocation = location,
                                notes = notes,
                                reminderDays = parsedReminder ?: DEFAULT_TRACK_WARNING_DAYS,
                            ),
                        )
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) { Text(if (existing == null) "Save item" else "Save changes") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Product name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyCategoryChips(
                selected = TrackCategory.valueOf(categoryName),
                onSelected = { categoryName = it.name },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateCard("Purchased", purchaseEpochDay?.asEpochDayLabel() ?: "Optional", { showPurchasePicker = true }, Modifier.weight(1f))
                DateCard("Expires", expiryEpochDay.asEpochDayLabel(), { showExpiryPicker = true }, Modifier.weight(1f))
            }
            OutlinedTextField(location, { location = it }, label = { Text("Storage location") }, placeholder = { Text("Fridge, pantry, bathroom…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(reminderDays, { reminderDays = it }, label = { Text("Warning days") }, supportingText = { Text("Item becomes ‘expiring soon’ inside this window") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showPurchasePicker) {
        TrackDatePickerDialog(
            initialEpochDay = purchaseEpochDay,
            onDismiss = { showPurchasePicker = false },
            onSelected = { purchaseEpochDay = it; showPurchasePicker = false },
            allowClear = true,
        )
    }
    if (showExpiryPicker) {
        TrackDatePickerDialog(
            initialEpochDay = expiryEpochDay,
            onDismiss = { showExpiryPicker = false },
            onSelected = { selected -> if (selected != null) expiryEpochDay = selected; showExpiryPicker = false },
        )
    }
}

@Composable
private fun DateCard(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackDatePickerDialog(
    initialEpochDay: Long?,
    onDismiss: () -> Unit,
    onSelected: (Long?) -> Unit,
    allowClear: Boolean = false,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochDayToDatePickerMillis(initialEpochDay))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSelected(datePickerMillisToEpochDay(pickerState.selectedDateMillis)) }) { Text("Select") } },
        dismissButton = {
            Row {
                if (allowClear) TextButton(onClick = { onSelected(null) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    ) { DatePicker(state = pickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    item: TrackItem?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOutcome: (TrackOutcome) -> Unit,
    onDelete: () -> Unit,
) {
    if (item == null) {
        MissingTrackItemScreen(onBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    containerColor = when (item.expiryState()) {
                        ExpiryState.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                        ExpiryState.EXPIRING_SOON, ExpiryState.EXPIRES_TODAY -> MaterialTheme.colorScheme.tertiaryContainer
                        ExpiryState.FRESH -> MaterialTheme.colorScheme.primaryContainer
                    },
                ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.category.emoji} ${item.category.label}", style = MaterialTheme.typography.labelLarge)
                    Text(item.expiryMessage(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Expiry date: ${item.expiryEpochDay.asEpochDayLabel()}")
                }
            }
            DetailRow("Quantity", item.quantity.quantityLabel(item.unit))
            item.purchaseEpochDay?.let { DetailRow("Purchase date", it.asEpochDayLabel()) }
            if (item.storageLocation.isNotBlank()) DetailRow("Stored at", item.storageLocation)
            DetailRow("Warning window", "${item.reminderDays} days")
            if (item.notes.isNotBlank()) DetailRow("Notes", item.notes)
            Text("Record outcome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOutcome(TrackOutcome.CONSUMED) }, modifier = Modifier.weight(1f)) { Text("Consumed") }
                OutlinedButton(onClick = { onOutcome(TrackOutcome.DISCARDED) }, modifier = Modifier.weight(1f)) { Text("Discarded") }
            }
            FilledTonalButton(onClick = { onOutcome(TrackOutcome.DONATED) }, modifier = Modifier.fillMaxWidth()) { Text("Donated / given away") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingTrackItemScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Item unavailable") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
    ) { padding -> EmptyState("This item is no longer active", "It may have been completed or deleted.", Modifier.padding(padding)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackHistoryScreen(events: List<TrackEvent>, onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Usage history") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
    ) { padding ->
        if (events.isEmpty()) {
            EmptyState("No outcomes yet", "Items marked consumed, discarded or donated will appear here.", Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = TrackEvent::id) { event ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                when (event.outcome) {
                                    TrackOutcome.CONSUMED -> "✅"
                                    TrackOutcome.DISCARDED -> "🗑️"
                                    TrackOutcome.DONATED -> "🤝"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(event.itemName, fontWeight = FontWeight.SemiBold)
                                Text("${event.outcome.label} • ${event.quantity.quantityLabel(event.unit)}")
                                Text(event.timestampMillis.asEventDateLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInsightsScreen(state: TrackUiState, onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Usage insights") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MetricCard("Use score", "${state.usageEfficiency}%", supportingText = "Consumed ÷ consumed and discarded outcomes") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Consumed", state.consumedCount.toString(), Modifier.weight(1f))
                    MetricCard("Wasted", state.discardedCount.toString(), Modifier.weight(1f))
                    MetricCard("Donated", state.donatedCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current risk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${state.expiredCount} expired and ${state.expiringSoonCount} expiring soon")
                        Text(
                            "More useful trends will appear after enough outcomes are recorded. The first release intentionally keeps analytics local and explainable.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        icon = if (destructive) ({ Icon(Icons.Default.Delete, contentDescription = null) }) else null,
    )
}
