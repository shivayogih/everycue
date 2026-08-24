package com.everycue.feature.pack

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    trips: List<Trip>,
    onTripClick: (Long) -> Unit,
    onCreateTrip: () -> Unit,
    onTemplates: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EveryCue Pack", fontWeight = FontWeight.Bold)
                        Text(
                            "Plan once. Travel calmly.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTemplates) {
                        Icon(Icons.Default.Checklist, contentDescription = "Packing templates")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTrip,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New trip") },
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
                    Text(
                        "Your trips",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(trips, key = Trip::id) { trip ->
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
            Text("Your next trip starts here", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Create a packing list or start from a practical template.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onCreateTrip) { Text("Create first trip") }
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
                    "$tripCount active ${if (tripCount == 1) "trip" else "trips"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                if (total == 0) "Add items to begin packing" else "$packed of $total items packed",
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
                Text(trip.packingSummary(), style = MaterialTheme.typography.labelLarge)
            }
            Text(
                trip.dateRangeLabel(),
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
    onBack: () -> Unit,
    onCreate: (TripDraft) -> Unit,
) {
    val template = TemplateCatalog.find(templateId)
    var name by rememberSaveable(templateId) {
        mutableStateOf(template?.let { "${it.title} trip" }.orEmpty())
    }
    var destination by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (template == null) "Create trip" else "Use template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        onCreate(
                            TripDraft(
                                name = name,
                                destination = destination,
                                startDateMillis = startDate,
                                endDateMillis = endDate,
                                templateId = templateId,
                            ),
                        )
                    },
                    enabled = name.isNotBlank() && (endDate == null || startDate == null || endDate!! >= startDate!!),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) {
                    Text("Create packing list")
                }
            }
        },
    ) { padding ->
        LazyColumn(
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
                                Text(template.title, fontWeight = FontWeight.SemiBold)
                                Text("${template.items.size} suggested items will be added")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip name") },
                    placeholder = { Text("Example: Goa holiday") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("Travel dates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(
                        label = "Starts",
                        value = startDate.asDateLabel(),
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = "Ends",
                        value = endDate.asDateLabel(),
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (endDate != null && startDate != null && endDate!! < startDate!!) {
                item {
                    Text(
                        "End date must be on or after the start date.",
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
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
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
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
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
    onAddItem: () -> Unit,
    onTogglePacked: (PackingItem, Boolean) -> Unit,
    onDeleteItem: (PackingItem) -> Unit,
    onUnpackAll: () -> Unit,
    onDeleteTrip: () -> Unit,
) {
    if (trip == null) {
        MissingTripScreen(onBack = onBack)
        return
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trip.packedCount > 0) {
                        IconButton(onClick = onUnpackAll) {
                            Icon(Icons.Default.Refresh, contentDescription = "Mark all unpacked")
                        }
                    }
                    IconButton(onClick = onDeleteTrip) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete trip")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItem,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add item") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TripProgressHeader(trip)
            }

            if (trip.items.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Nothing to pack yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Add the first item to this trip.")
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = onAddItem) { Text("Add item") }
                        }
                    }
                }
            } else {
                PackingCategory.entries.forEach { category ->
                    val categoryItems = trip.items
                        .filter { it.category == category }
                        .sortedWith(compareBy<PackingItem> { it.isPacked }.thenBy { it.position })
                    if (categoryItems.isNotEmpty()) {
                        item(key = "header-${category.name}") {
                            Text(
                                "${category.emoji} ${category.label}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(categoryItems, key = PackingItem::id) { item ->
                            PackingItemRow(
                                item = item,
                                onCheckedChange = { checked -> onTogglePacked(item, checked) },
                                onDelete = { onDeleteItem(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripProgressHeader(trip: Trip) {
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
                    Text(trip.packingSummary(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(trip.dateRangeLabel(), style = MaterialTheme.typography.bodySmall)
                }
                Text("${(trip.progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
            }
            LinearProgressIndicator(
                progress = { trip.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
        }
    }
}

@Composable
private fun PackingItemRow(
    item: PackingItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                    Text("Quantity: ${item.quantity}", style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${item.name}")
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
                title = { Text("Trip not found") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("This trip may have been deleted.")
        }
    }
}
