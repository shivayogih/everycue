package com.everycue.feature.pack

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.everycue.feature.pack.PackingCategory
import com.everycue.feature.pack.PackingItem
import com.everycue.feature.pack.TemplateCatalog
import com.everycue.feature.pack.Trip
import com.everycue.feature.pack.TripDraft
import com.everycue.feature.pack.asDateLabel
import com.everycue.feature.pack.dateRangeLabel
import com.everycue.feature.pack.packingSummary
import com.everycue.core.designsystem.DismissKeyboardOnScroll
import com.everycue.core.designsystem.rememberKeyboardDismissAction

@Composable
private fun Trip.localizedPackingSummary(): String {
    val packedText = stringResource(R.string.packed_summary, packedCount, totalCount)
    return packingSummary(
        noItems = stringResource(R.string.no_items_yet),
        ready = stringResource(R.string.ready_to_go),
        packed = { _, _ -> packedText },
    )
}

@Composable
private fun Trip.localizedDateRange(): String = dateRangeLabel(stringResource(R.string.dates_not_set))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    trips: List<Trip>,
    onTripClick: (Long) -> Unit,
    onCreateTrip: () -> Unit,
    onTemplates: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    DismissKeyboardOnScroll(listState)
    val visibleTrips = remember(trips, query) {
        trips.filter { query.isBlank() || it.name.contains(query, true) || it.destination.contains(query, true) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.pack_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.pack_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTemplates) {
                        Icon(Icons.Default.Checklist, contentDescription = stringResource(R.string.packing_templates))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTrip,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_trip)) },
            )
        },
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyTripsState(
                modifier = Modifier.padding(padding),
                onCreateTrip = onCreateTrip,
            )
        } else {
            val total = trips.sumOf(Trip::totalCount)
            val packed = trips.sumOf(Trip::packedCount)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OverallProgressCard(
                        tripCount = trips.size,
                        packed = packed,
                        total = total,
                    )
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.search_trips)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        stringResource(R.string.your_trips),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (visibleTrips.isEmpty()) {
                    item { Text(stringResource(R.string.no_trip_matches), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(visibleTrips, key = Trip::id) { trip ->
                    TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyTripsState(
    modifier: Modifier = Modifier,
    onCreateTrip: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Default.Luggage,
                    contentDescription = null,
                    modifier = Modifier.padding(22.dp).size(46.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(stringResource(R.string.next_trip_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.next_trip_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onCreateTrip) { Text(stringResource(R.string.create_first_trip)) }
        }
    }
}

@Composable
private fun OverallProgressCard(tripCount: Int, packed: Int, total: Int) {
    val progress = if (total == 0) 0f else packed.toFloat() / total
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TravelExplore, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (tripCount == 1) R.string.active_trip_one else R.string.active_trips, tripCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                if (total == 0) stringResource(R.string.add_items_to_begin) else stringResource(R.string.packed_summary, packed, total),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
        }
    }
}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (trip.destination.isNotBlank()) {
                        Text(trip.destination, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(trip.localizedPackingSummary(), style = MaterialTheme.typography.labelLarge)
            }
            Text(
                trip.localizedDateRange(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { trip.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    templateId: String?,
    existing: Trip? = null,
    onBack: () -> Unit,
    onCreate: (TripDraft) -> Unit,
) {
    val template = TemplateCatalog.find(templateId)
    val templateTitle = template?.let { stringResource(it.titleResource) }
    val suggestedName = templateTitle?.let { stringResource(R.string.template_trip_name, it) }.orEmpty()
    var name by rememberSaveable(templateId, existing?.id) {
        mutableStateOf(existing?.name ?: suggestedName)
    }
    var destination by rememberSaveable(existing?.id) { mutableStateOf(existing?.destination.orEmpty()) }
    var startDate by rememberSaveable(existing?.id) { mutableStateOf(existing?.startDateMillis) }
    var endDate by rememberSaveable(existing?.id) { mutableStateOf(existing?.endDateMillis) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    val candidate = TripDraft(
        name = name,
        destination = destination,
        startDateMillis = startDate,
        endDateMillis = endDate,
        templateId = templateId,
    )
    val validation = candidate.validationErrors()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing != null) R.string.edit_trip else if (template == null) R.string.create_trip else R.string.use_template)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        attemptedSubmit = true
                        if (validation.isValid) {
                            dismissKeyboard()
                            onCreate(candidate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) {
                    Text(stringResource(if (existing == null) R.string.create_packing_list else R.string.save_trip))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (template != null) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(template.emoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(template.titleResource), fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.suggested_items, template.items.size))
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text(stringResource(R.string.trip_name)) },
                    placeholder = { Text(stringResource(R.string.trip_name_hint)) },
                    singleLine = true,
                    isError = attemptedSubmit && validation.name != null,
                    supportingText = validation.name.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it.take(100) },
                    label = { Text(stringResource(R.string.destination)) },
                    placeholder = { Text(stringResource(R.string.optional)) },
                    singleLine = true,
                    isError = attemptedSubmit && validation.destination != null,
                    supportingText = validation.destination.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(stringResource(R.string.travel_dates), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(
                        label = stringResource(R.string.starts),
                        value = startDate.asDateLabel(stringResource(R.string.not_selected)),
                        onClick = {
                            dismissKeyboard()
                            showStartPicker = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = stringResource(R.string.ends),
                        value = endDate.asDateLabel(stringResource(R.string.not_selected)),
                        onClick = {
                            dismissKeyboard()
                            showEndPicker = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (validation.dates != null && (attemptedSubmit || (endDate != null && startDate != null))) {
                item {
                    Text(
                        stringResource(validation.dates),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = state.selectedDateMillis
                    showStartPicker = false
                }) { Text(stringResource(R.string.select)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate ?: startDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = state.selectedDateMillis
                    showEndPicker = false
                }) { Text(stringResource(R.string.select)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip?,
    onBack: () -> Unit,
    onAddItem: (String, PackingCategory, Int) -> Unit,
    onEditTrip: () -> Unit,
    onTogglePacked: (PackingItem, Boolean) -> Unit,
    onDeleteItem: (PackingItem) -> Unit,
    onMoveItem: (PackingItem, Int) -> Unit,
    onUnpackAll: () -> Unit,
    onDeleteTrip: () -> Unit,
) {
    if (trip == null) {
        MissingTripScreen(onBack = onBack)
        return
    }
    var query by rememberSaveable(trip.id) { mutableStateOf("") }
    var showAddItemSheet by rememberSaveable(trip.id) { mutableStateOf(false) }
    val sections = remember(trip.items, query) { buildPackingSections(trip.items, query) }
    val listState = rememberLazyListState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    DismissKeyboardOnScroll(listState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(trip.name)
                        if (trip.destination.isNotBlank()) {
                            Text(
                                trip.destination,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEditTrip) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_trip_description))
                    }
                    if (trip.packedCount > 0) {
                        IconButton(onClick = onUnpackAll) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.mark_all_unpacked))
                        }
                    }
                    IconButton(onClick = onDeleteTrip) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_trip_description))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddItemSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_item)) },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TripProgressHeader(trip)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_packing_items)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (trip.items.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(R.string.nothing_to_pack), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.nothing_to_pack_body))
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = { showAddItemSheet = true }) { Text(stringResource(R.string.add_item)) }
                        }
                    }
                }
            } else {
                sections.forEach { section ->
                        item(key = "header-${section.category.name}") {
                            Text(
                                "${section.category.emoji} ${section.category.displayName()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(section.items, key = PackingItem::id) { item ->
                            PackingItemRow(
                                item = item,
                                modifier = Modifier.animateItem(),
                                onCheckedChange = { checked ->
                                    dismissKeyboard()
                                    onTogglePacked(item, checked)
                                },
                                onDelete = {
                                    dismissKeyboard()
                                    onDeleteItem(item)
                                },
                                onMoveUp = {
                                    dismissKeyboard()
                                    onMoveItem(item, -1)
                                },
                                onMoveDown = {
                                    dismissKeyboard()
                                    onMoveItem(item, 1)
                                },
                            )
                        }
                }
            }
        }
    }
    if (showAddItemSheet) {
        AddItemBottomSheetScreen(
            onDismiss = { showAddItemSheet = false },
            onAdd = { name, category, quantity ->
                onAddItem(name, category, quantity)
                showAddItemSheet = false
            },
        )
    }
}

@Composable
private fun TripProgressHeader(trip: Trip) {
    val animatedProgress by animateFloatAsState(targetValue = trip.progress, label = "packing-progress")
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (trip.items.isNotEmpty() && trip.packedCount == trip.totalCount) {
                MaterialTheme.colorScheme.primaryContainer
            } else MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.localizedPackingSummary(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(trip.localizedDateRange(), style = MaterialTheme.typography.bodySmall)
                }
                Text("${(trip.progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
        }
    }
}

@Composable
private fun PackingItemRow(
    item: PackingItem,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!item.isPacked) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.isPacked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    item.name,
                    textDecoration = if (item.isPacked) TextDecoration.LineThrough else null,
                    color = if (item.isPacked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (item.quantity > 1) {
                    Text(stringResource(R.string.quantity_format, item.quantity), style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.move_item_up, item.name))
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.move_item_down, item.name))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_item, item.name))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingTripScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_not_found)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.trip_deleted_body))
        }
    }
}
