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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TipsAndUpdates
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everycue.core.designsystem.EmptyState
import com.everycue.core.designsystem.MetricCard
import com.everycue.core.designsystem.SectionHeader
import com.everycue.core.designsystem.DismissKeyboardOnScroll
import com.everycue.core.designsystem.rememberKeyboardDismissAction
import com.everycue.core.extraction.ExtractionSourceType
import com.everycue.core.extraction.SmartAddDraft
import com.everycue.core.recommendation.UseNextReasonCode
import com.everycue.core.recommendation.WastePatternType
import com.everycue.core.recommendation.WasteSuggestionCode
import com.everycue.core.recommendation.WasteWindow
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
    onOpenUseNext: () -> Unit,
    onOpenWasteCoach: () -> Unit,
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
                    title = stringResource(R.string.use_next),
                    action = { TextButton(onClick = onOpenUseNext) { Text(stringResource(R.string.view_all)) } },
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
            } else if (state.useNext.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.nothing_safe_to_prioritize), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.nothing_safe_to_prioritize_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.useNext.take(6), key = { it.item.id }) { entry ->
                    TrackItemCard(item = entry.item, onClick = { onOpenItem(entry.item.id) })
                }
            }
            item {
                QuickAction(
                    label = stringResource(R.string.waste_coach),
                    icon = Icons.Default.TipsAndUpdates,
                    onClick = onOpenWasteCoach,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackWasteCoachScreen(
    state: TrackUiState,
    onBack: () -> Unit,
    onIntent: (TrackIntent) -> Unit,
) {
    val result = state.wasteCoach
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.waste_coach)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.waste_coach_explanation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(result.summaries, key = { it.window.name }) { summary ->
                val label = when (summary.window) {
                    WasteWindow.THIRTY_DAYS -> stringResource(R.string.last_30_days)
                    WasteWindow.THREE_MONTHS -> stringResource(R.string.last_3_months)
                    WasteWindow.TWELVE_MONTHS -> stringResource(R.string.last_12_months)
                }
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.discard_count, summary.discardedCount))
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.coaching_observations)) }
            if (result.insights.isEmpty()) {
                item {
                    EmptyState(
                        stringResource(R.string.no_coaching_patterns),
                        stringResource(R.string.no_coaching_patterns_help),
                    )
                }
            } else {
                items(result.insights, key = { it.key }) { insight ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val title = when (insight.pattern.type) {
                                WastePatternType.REPEATED_PRODUCT_DISCARD ->
                                    stringResource(R.string.repeated_product_pattern, insight.pattern.subjectLabel.orEmpty())
                                WastePatternType.REPEATED_CATEGORY_DISCARD ->
                                    stringResource(R.string.repeated_category_pattern, insight.pattern.category.orEmpty())
                                WastePatternType.DISCARD_RATE_INCREASED ->
                                    stringResource(R.string.discard_trend_pattern)
                            }
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(
                                    R.string.coach_evidence,
                                    insight.pattern.evidence.occurrences,
                                    insight.pattern.evidence.decisions,
                                    insight.pattern.evidence.discardRatePercent,
                                ),
                            )
                            Text(
                                stringResource(
                                    when (insight.suggestionCode) {
                                        WasteSuggestionCode.TRY_A_SMALLER_AMOUNT -> R.string.suggestion_smaller_amount
                                        WasteSuggestionCode.PLAN_AN_EARLIER_USE -> R.string.suggestion_earlier_use
                                        WasteSuggestionCode.REVIEW_CATEGORY_BUYING -> R.string.suggestion_review_category
                                    },
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    onIntent(TrackIntent.DismissCoachInsight(insight.key))
                                }) { Text(stringResource(R.string.dismiss)) }
                                insight.pattern.subjectId?.let { subjectId ->
                                    TextButton(onClick = {
                                        onIntent(TrackIntent.HideCoachSubject(subjectId))
                                    }) { Text(stringResource(R.string.mute_product)) }
                                }
                                insight.pattern.category?.let { category ->
                                    TextButton(onClick = {
                                        onIntent(TrackIntent.HideCoachCategory(category))
                                    }) { Text(stringResource(R.string.mute_category)) }
                                }
                            }
                        }
                    }
                }
            }
            if (state.coachPreferences.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.hidden_coaching)) }
                items(state.coachPreferences, key = TrackCoachPreference::key) { preference ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            when (preference.type) {
                                TrackCoachPreferenceType.HIDDEN_SUBJECT -> stringResource(R.string.hidden_product)
                                TrackCoachPreferenceType.HIDDEN_CATEGORY -> stringResource(R.string.hidden_category, preference.value)
                                TrackCoachPreferenceType.DISMISSED_INSIGHT -> stringResource(R.string.dismissed_observation)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            onIntent(TrackIntent.RestoreCoachPreference(preference.key))
                        }) { Text(stringResource(R.string.restore)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackUseNextScreen(
    entries: List<TrackUseNextEntry>,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
) {
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = categoryName?.let { runCatching { TrackCategory.valueOf(it) }.getOrNull() }
    val visible = remember(entries, selectedCategory) {
        if (selectedCategory == null) entries else entries.filter { it.item.category == selectedCategory }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.use_next)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(stringResource(R.string.use_next_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                LazyCategoryChips(
                    selected = selectedCategory,
                    onSelected = { categoryName = if (selectedCategory == it) null else it.name },
                )
            }
            if (visible.isEmpty()) {
                item { EmptyState(stringResource(R.string.no_use_next_items), stringResource(R.string.no_use_next_items_help)) }
            } else {
                items(visible, key = { it.item.id }) { entry ->
                    ElevatedCard(onClick = { onOpenItem(entry.item.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.item.category.emoji, style = MaterialTheme.typography.headlineMedium)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.item.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(
                                        when (entry.score.reasonCodes.first()) {
                                            UseNextReasonCode.EXPIRES_TODAY -> R.string.reason_expires_today
                                            UseNextReasonCode.EXPIRING_SOON -> R.string.reason_expiring_soon
                                            UseNextReasonCode.EXPIRY_UPCOMING -> R.string.reason_expiry_upcoming
                                        },
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(entry.item.expiryMessageResource(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
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
    val visible = remember(items, query, selectedCategory) {
        items.filter { item ->
            (query.isBlank() || item.name.contains(query, ignoreCase = true) || item.storageLocation.contains(query, ignoreCase = true)) &&
                (selectedCategory == null || item.category == selectedCategory)
        }
    }
    val listState = rememberLazyListState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    DismissKeyboardOnScroll(listState)

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
            state = listState,
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyCategoryChips(
                    selected = selectedCategory,
                    onSelected = {
                        dismissKeyboard()
                        selectedCategory = if (selectedCategory == it) null else it
                    },
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
    smartDraft: SmartAddDraft?,
    onBack: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanLabel: () -> Unit,
    onImportImage: () -> Unit,
    onClearSmartAdd: () -> Unit,
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
    var attemptedSubmit by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var barcode by rememberSaveable(existing?.id) { mutableStateOf(existing?.barcode) }
    var expiryConfirmed by rememberSaveable(existing?.id) { mutableStateOf(existing != null || smartDraft == null) }
    var smartAddReviewed by rememberSaveable(existing?.id) { mutableStateOf(existing != null || smartDraft == null) }
    val scrollState = rememberScrollState()
    val dismissKeyboard = rememberKeyboardDismissAction()

    LaunchedEffect(smartDraft?.token, existing?.id) {
        if (existing == null && smartDraft != null) {
            smartAddReviewed = false
            // A barcode or OCR result cannot establish the physical item's expiry with
            // certainty. Always require an explicit date review, even when no date was found.
            expiryConfirmed = false
            smartDraft.productName?.value?.let { name = it.take(80) }
            smartDraft.categoryName?.value?.let { value ->
                runCatching { TrackCategory.valueOf(value) }.getOrNull()?.let { categoryName = it.name }
            }
            smartDraft.quantity?.value?.let { quantity = it.toEditableNumber() }
            smartDraft.unit?.value?.let { unit = it.take(20) }
            smartDraft.purchaseDate?.field?.value?.let { purchaseEpochDay = it.toEpochDay() }
            smartDraft.expiryDate?.field?.value?.let {
                expiryEpochDay = it.toEpochDay()
            }
            smartDraft.storageLocation?.value?.let { location = it.take(100) }
            smartDraft.notes?.value?.let { notes = it.take(500) }
            barcode = smartDraft.barcode?.value
        }
    }

    val parsedQuantity = quantity.toDoubleOrNull()
    val parsedReminder = reminderDays.toIntOrNull()
    val candidate = TrackDraft(
        name = name,
        category = TrackCategory.valueOf(categoryName),
        quantity = parsedQuantity ?: Double.NaN,
        unit = unit,
        purchaseEpochDay = purchaseEpochDay,
        expiryEpochDay = expiryEpochDay,
        storageLocation = location,
        notes = notes,
        reminderDays = parsedReminder ?: -1,
        barcode = barcode,
    )
    val validation = candidate.validationErrors()

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
                        attemptedSubmit = true
                        if (validation.isValid && expiryConfirmed && smartAddReviewed) {
                            dismissKeyboard()
                            onSave(candidate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) { Text(stringResource(if (existing == null) R.string.save_item else R.string.save_changes)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (existing == null) {
                Text(stringResource(R.string.smart_add), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onScanBarcode, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.scan_barcode))
                    }
                    FilledTonalButton(onClick = onScanLabel, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.scan_label))
                    }
                }
                OutlinedButton(onClick = onImportImage, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.import_label_image))
                }
                smartDraft?.let { draft ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.review_extracted_fields), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(
                                    when (draft.sourceType) {
                                        ExtractionSourceType.BARCODE -> R.string.smart_add_source_barcode
                                        ExtractionSourceType.CAMERA_OCR -> R.string.smart_add_source_camera
                                        ExtractionSourceType.IMAGE_OCR -> R.string.smart_add_source_image
                                        else -> R.string.smart_add_source_other
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (draft.possibleDuplicateIds.isNotEmpty()) {
                                Text(stringResource(R.string.possible_duplicate), color = MaterialTheme.colorScheme.error)
                            }
                            draft.barcode?.value?.let { value ->
                                Text(stringResource(R.string.scanned_barcode_value, value), fontWeight = FontWeight.Medium)
                            }
                            Text(stringResource(R.string.smart_add_review_help), style = MaterialTheme.typography.bodySmall)
                            if (!smartAddReviewed) {
                                Button(onClick = { smartAddReviewed = true }) {
                                    Text(stringResource(R.string.confirm_smart_add_review))
                                }
                            }
                            if (attemptedSubmit && !smartAddReviewed) {
                                Text(stringResource(R.string.smart_add_review_required), color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(
                                onClick = {
                                    name = ""
                                    categoryName = TrackCategory.GROCERY.name
                                    quantity = "1"
                                    unit = defaultUnit
                                    purchaseEpochDay = null
                                    expiryEpochDay = LocalDate.now().plusDays(7).toEpochDay()
                                    location = ""
                                    notes = ""
                                    smartAddReviewed = true
                                    expiryConfirmed = true
                                    barcode = null
                                    onClearSmartAdd()
                                },
                            ) { Text(stringResource(R.string.clear_smart_add)) }
                        }
                    }
                }
            }
            OutlinedTextField(
                name,
                { name = it.take(80) },
                label = { Text(stringResource(R.string.product_name)) },
                singleLine = true,
                isError = attemptedSubmit && validation.name != null,
                supportingText = validation.name.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyCategoryChips(
                selected = TrackCategory.valueOf(categoryName),
                onSelected = { categoryName = it.name },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    quantity,
                    { quantity = it.toDecimalInput(10) },
                    label = { Text(stringResource(R.string.quantity)) },
                    singleLine = true,
                    isError = attemptedSubmit && validation.quantity != null,
                    supportingText = validation.quantity.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    unit,
                    { unit = it.take(20).filter { character -> character.isLetter() || character.isWhitespace() || character == '.' } },
                    label = { Text(stringResource(R.string.unit)) },
                    singleLine = true,
                    isError = attemptedSubmit && validation.unit != null,
                    supportingText = validation.unit.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateCard(stringResource(R.string.purchased), purchaseEpochDay?.asEpochDayLabel() ?: stringResource(R.string.optional), { dismissKeyboard(); showPurchasePicker = true }, Modifier.weight(1f))
                DateCard(stringResource(R.string.expires), expiryEpochDay.asEpochDayLabel(), { dismissKeyboard(); showExpiryPicker = true }, Modifier.weight(1f))
            }
            validation.dates.takeIf { attemptedSubmit }?.let { error ->
                Text(stringResource(error), color = MaterialTheme.colorScheme.error)
            }
            if (!expiryConfirmed) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.confirm_extracted_expiry), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.confirm_extracted_expiry_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { expiryConfirmed = true }) { Text(stringResource(R.string.confirm_date)) }
                    }
                }
            }
            OutlinedTextField(
                location,
                { location = it.take(100) },
                label = { Text(stringResource(R.string.storage_location)) },
                placeholder = { Text(stringResource(R.string.storage_location_hint)) },
                singleLine = true,
                isError = attemptedSubmit && validation.location != null,
                supportingText = validation.location.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                reminderDays,
                { reminderDays = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.warning_days)) },
                supportingText = {
                    Text(stringResource(validation.reminderDays.takeIf { attemptedSubmit } ?: R.string.warning_days_help))
                },
                isError = attemptedSubmit && validation.reminderDays != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                notes,
                { notes = it.take(500) },
                label = { Text(stringResource(R.string.notes)) },
                minLines = 3,
                isError = attemptedSubmit && validation.notes != null,
                supportingText = validation.notes.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                modifier = Modifier.fillMaxWidth(),
            )
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
            onSelected = { selected -> if (selected != null) { expiryEpochDay = selected; expiryConfirmed = true }; showExpiryPicker = false },
        )
    }
}

private fun String.toDecimalInput(maxLength: Int): String {
    var decimalSeen = false
    return filter { character ->
        character.isDigit() || (character == '.' && !decimalSeen.also { decimalSeen = true })
    }.take(maxLength)
}

private fun Double.toEditableNumber(): String =
    if (isFinite() && this % 1.0 == 0.0) toLong().toString() else toString()

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

