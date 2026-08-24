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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everycue.core.designsystem.EmptyState
import com.everycue.core.designsystem.MetricCard
import com.everycue.core.designsystem.SectionHeader
import java.time.LocalDate

@Composable
private fun TrackCategory.displayName(): String = stringResource(
    when (this) {
        TrackCategory.GROCERY -> R.string.category_grocery
        TrackCategory.MEDICINE -> R.string.category_medicine
        TrackCategory.COSMETIC -> R.string.category_cosmetic
        TrackCategory.HOUSEHOLD -> R.string.category_household
        TrackCategory.SUPPLEMENT -> R.string.category_supplement
        TrackCategory.OTHER -> R.string.category_other
    },
)

@Composable
private fun TrackOutcome.displayName(): String = stringResource(
    when (this) {
        TrackOutcome.CONSUMED -> R.string.consumed
        TrackOutcome.DISCARDED -> R.string.discarded
        TrackOutcome.DONATED -> R.string.donated
    },
)

@Composable
private fun TrackItem.expiryMessageResource(): String {
    val remaining = daysRemaining()
    return when {
        remaining < -1 -> stringResource(R.string.expires_days_ago, -remaining)
        remaining == -1L -> stringResource(R.string.expired_yesterday)
        remaining == 0L -> stringResource(R.string.expires_today)
        remaining == 1L -> stringResource(R.string.expires_tomorrow)
        else -> stringResource(R.string.expires_in_days, remaining)
    }
}

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
                        Text(stringResource(R.string.track_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.track_tagline),
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
                text = { Text(stringResource(R.string.add_item)) },
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
                    MetricCard(stringResource(R.string.fresh), state.freshCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.soon), state.expiringSoonCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.expired), state.expiredCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(stringResource(R.string.inventory), Icons.Default.Inventory2, onOpenInventory, Modifier.weight(1f))
                    QuickAction(stringResource(R.string.history), Icons.Default.History, onOpenHistory, Modifier.weight(1f))
                    QuickAction(stringResource(R.string.insights), Icons.Default.BarChart, onOpenInsights, Modifier.weight(1f))
                }
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.use_ahead),
                    action = { TextButton(onClick = onOpenInventory) { Text(stringResource(R.string.view_all)) } },
                )
            }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.nothing_tracked),
                        message = stringResource(R.string.nothing_tracked_message),
                        action = { Button(onClick = onAdd) { Text(stringResource(R.string.add_first_item)) } },
                    )
                }
            } else if (state.urgentItems.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.everything_fresh), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.nothing_in_warning_window), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                title = { Text(stringResource(R.string.inventory)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringResource(R.string.add)) })
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
                    label = { Text(stringResource(R.string.search_name_location)) },
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
                        title = stringResource(if (items.isEmpty()) R.string.inventory_empty else R.string.no_matching_items),
                        message = stringResource(if (items.isEmpty()) R.string.inventory_empty_message else R.string.clear_filter_message),
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
                        label = { Text("${category.emoji} ${category.displayName()}") },
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
                Text(item.expiryMessageResource(), style = MaterialTheme.typography.bodyMedium)
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
    val defaultUnit = stringResource(R.string.default_unit)
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var categoryName by rememberSaveable(existing?.id) { mutableStateOf(existing?.category?.name ?: TrackCategory.GROCERY.name) }
    var quantity by rememberSaveable(existing?.id) { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var unit by rememberSaveable(existing?.id) { mutableStateOf(existing?.unit ?: defaultUnit) }
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
                title = { Text(stringResource(if (existing == null) R.string.add_item else R.string.edit_item)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
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
                ) { Text(stringResource(if (existing == null) R.string.save_item else R.string.save_changes)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.product_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyCategoryChips(
                selected = TrackCategory.valueOf(categoryName),
                onSelected = { categoryName = it.name },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(quantity, { quantity = it }, label = { Text(stringResource(R.string.quantity)) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(R.string.unit)) }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateCard(stringResource(R.string.purchased), purchaseEpochDay?.asEpochDayLabel() ?: stringResource(R.string.optional), { showPurchasePicker = true }, Modifier.weight(1f))
                DateCard(stringResource(R.string.expires), expiryEpochDay.asEpochDayLabel(), { showExpiryPicker = true }, Modifier.weight(1f))
            }
            OutlinedTextField(location, { location = it }, label = { Text(stringResource(R.string.storage_location)) }, placeholder = { Text(stringResource(R.string.storage_location_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(reminderDays, { reminderDays = it }, label = { Text(stringResource(R.string.warning_days)) }, supportingText = { Text(stringResource(R.string.warning_days_help)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
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
        confirmButton = { TextButton(onClick = { onSelected(datePickerMillisToEpochDay(pickerState.selectedDateMillis)) }) { Text(stringResource(R.string.select)) } },
        dismissButton = {
            Row {
                if (allowClear) TextButton(onClick = { onSelected(null) }) { Text(stringResource(R.string.clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
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
                    Text("${item.category.emoji} ${item.category.displayName()}", style = MaterialTheme.typography.labelLarge)
                    Text(item.expiryMessageResource(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.expiry_date_format, item.expiryEpochDay.asEpochDayLabel()))
                }
            }
            DetailRow(stringResource(R.string.quantity), item.quantity.quantityLabel(item.unit))
            item.purchaseEpochDay?.let { DetailRow(stringResource(R.string.purchase_date), it.asEpochDayLabel()) }
            if (item.storageLocation.isNotBlank()) DetailRow(stringResource(R.string.stored_at), item.storageLocation)
            DetailRow(stringResource(R.string.warning_window), stringResource(R.string.days_format, item.reminderDays))
            if (item.notes.isNotBlank()) DetailRow(stringResource(R.string.notes), item.notes)
            Text(stringResource(R.string.record_outcome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOutcome(TrackOutcome.CONSUMED) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.consumed)) }
                OutlinedButton(onClick = { onOutcome(TrackOutcome.DISCARDED) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.discarded)) }
            }
            FilledTonalButton(onClick = { onOutcome(TrackOutcome.DONATED) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.donated_given_away)) }
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.item_unavailable)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
    ) { padding -> EmptyState(stringResource(R.string.item_no_longer_active), stringResource(R.string.item_missing_message), Modifier.padding(padding)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackHistoryScreen(events: List<TrackEvent>, onBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.usage_history)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
    ) { padding ->
        if (events.isEmpty()) {
            EmptyState(stringResource(R.string.no_outcomes), stringResource(R.string.no_outcomes_message), Modifier.fillMaxSize().padding(padding))
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
                                Text(stringResource(R.string.event_summary, event.outcome.displayName(), event.quantity.quantityLabel(event.unit)))
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
    val categories = state.items.groupingBy(TrackItem::category).eachCount().entries.sortedByDescending { it.value }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.usage_insights)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MetricCard(stringResource(R.string.use_score), "${state.usageEfficiency}%", supportingText = stringResource(R.string.use_score_help)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(stringResource(R.string.consumed), state.consumedCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.wasted), state.discardedCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.donated), state.donatedCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.active_by_category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (categories.isEmpty()) Text(stringResource(R.string.add_items_breakdown), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else categories.forEach { (category, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${category.emoji} ${category.displayName()}")
                                Text(count.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.current_risk), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.current_risk_summary, state.expiredCount, state.expiringSoonCount))
                        Text(
                            stringResource(R.string.insights_local_help),
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        icon = if (destructive) ({ Icon(Icons.Default.Delete, contentDescription = null) }) else null,
    )
}

