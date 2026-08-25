package com.everycue.feature.renew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
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
import java.time.LocalDate

@Composable
private fun RenewalType.displayName(): String = stringResource(
    when (this) {
        RenewalType.DOCUMENT -> R.string.type_document
        RenewalType.INSURANCE -> R.string.type_insurance
        RenewalType.WARRANTY -> R.string.type_warranty
        RenewalType.MEMBERSHIP -> R.string.type_membership
        RenewalType.SUBSCRIPTION -> R.string.type_subscription
        RenewalType.CERTIFICATE -> R.string.type_certificate
        RenewalType.OTHER -> R.string.type_other
    },
)

@Composable
private fun RenewalItem.localizedDueMessage(): String {
    val remaining = daysRemaining()
    return when {
        remaining < -1 -> stringResource(R.string.overdue_days, -remaining)
        remaining == -1L -> stringResource(R.string.overdue_one_day)
        remaining == 0L -> stringResource(R.string.due_today)
        remaining == 1L -> stringResource(R.string.due_tomorrow)
        else -> stringResource(R.string.due_in_days, remaining)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewHomeScreen(
    state: RenewUiState,
    onAdd: () -> Unit,
    onOpenAll: () -> Unit,
    onOpenRenewal: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.renew_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.renew_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInsights) { Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.renewal_insights)) }
                    IconButton(onClick = onOpenAll) { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = stringResource(R.string.all_renewals)) }
                    IconButton(onClick = onOpenHistory) { Icon(Icons.Default.History, contentDescription = stringResource(R.string.renewal_history)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringResource(R.string.add_renewal)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(stringResource(R.string.overdue), state.overdueCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.due_soon), state.dueSoonCount.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.upcoming), state.upcomingCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                SectionHeader(
                    title = stringResource(R.string.needs_attention),
                    action = { TextButton(onClick = onOpenAll) { Text(stringResource(R.string.view_all)) } },
                )
            }
            if (state.renewals.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.no_renewals_title),
                        message = stringResource(R.string.no_renewals_body),
                        action = { Button(onClick = onAdd) { Text(stringResource(R.string.add_first_renewal)) } },
                    )
                }
            } else if (state.urgent.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.nothing_due), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.nothing_due_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun RenewInsightsScreen(state: RenewUiState, onBack: () -> Unit) {
    val mostCommon = state.renewals.groupingBy(RenewalItem::type).eachCount().entries
        .sortedByDescending { it.value }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.renewal_insights)) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(stringResource(R.string.completed), state.events.size.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.active), state.renewals.size.toString(), Modifier.weight(1f))
                    MetricCard(stringResource(R.string.at_risk), (state.overdueCount + state.dueSoonCount).toString(), Modifier.weight(1f))
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.active_by_type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (mostCommon.isEmpty()) Text(stringResource(R.string.add_renewals_breakdown), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else mostCommon.forEach { (type, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${type.emoji} ${type.displayName()}")
                                Text(count.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.schedule_health), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.schedule_summary, state.overdueCount, state.dueSoonCount, state.upcomingCount))
                        Text(stringResource(R.string.local_calculation_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
    val visible = remember(renewals, query, selectedType) {
        renewals.filter {
            (query.isBlank() || it.title.contains(query, true) || it.provider.contains(query, true)) &&
                (selectedType == null || it.type == selectedType)
        }
    }
    val listState = rememberLazyListState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    DismissKeyboardOnScroll(listState)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.all_renewals)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Default.Add, null) }, text = { Text(stringResource(R.string.add)) }) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.search_title_provider)) }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }), modifier = Modifier.fillMaxWidth()) }
            item {
                TypeChips(
                    selected = selectedType,
                    onSelected = { type ->
                        dismissKeyboard()
                        selectedTypeName = if (selectedType == type) null else type.name
                    },
                )
            }
            if (visible.isEmpty()) {
                item { EmptyState(stringResource(if (renewals.isEmpty()) R.string.no_renewals else R.string.no_matching_renewals), stringResource(R.string.renewals_empty_body)) }
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
                        label = { Text("${type.emoji} ${type.displayName()}") },
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
                Text(item.localizedDueMessage())
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
    var attemptedSubmit by rememberSaveable(existing?.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    val reminder = reminderDays.toIntOrNull()
    val candidate = RenewalDraft(
        title = title,
        type = RenewalType.valueOf(typeName),
        dueEpochDay = dueEpochDay,
        reminderDays = reminder ?: -1,
        provider = provider,
        referenceNumber = reference,
        notes = notes,
    )
    val validation = candidate.validationErrors()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(if (existing == null) R.string.add_renewal else R.string.edit_renewal)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        attemptedSubmit = true
                        if (validation.isValid) {
                            dismissKeyboard()
                            onSave(candidate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) { Text(stringResource(if (existing == null) R.string.save_renewal else R.string.save_changes)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                title,
                { title = it.take(100) },
                label = { Text(stringResource(R.string.title)) },
                placeholder = { Text(stringResource(R.string.title_hint)) },
                singleLine = true,
                isError = attemptedSubmit && validation.title != null,
                supportingText = validation.title.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.type), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TypeChips(selected = RenewalType.valueOf(typeName), onSelected = { typeName = it.name })
            ElevatedCard(onClick = { dismissKeyboard(); showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.due_date), style = MaterialTheme.typography.labelMedium)
                    Text(dueEpochDay.asRenewDateLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
            OutlinedTextField(
                reminderDays,
                { reminderDays = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.remind_before_days)) },
                singleLine = true,
                isError = attemptedSubmit && validation.reminderDays != null,
                supportingText = validation.reminderDays.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                provider,
                { provider = it.take(100) },
                label = { Text(stringResource(R.string.provider_issuer)) },
                singleLine = true,
                isError = attemptedSubmit && validation.provider != null,
                supportingText = validation.provider.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                reference,
                { reference = it.take(80).filter { character -> character.isLetterOrDigit() || character.isWhitespace() || character in setOf('.', '/', '_', '-') } },
                label = { Text(stringResource(R.string.reference_number)) },
                singleLine = true,
                isError = attemptedSubmit && validation.reference != null,
                supportingText = validation.reference.takeIf { attemptedSubmit }?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
            TextButton(onClick = { renewPickerMillisToEpochDay(state.selectedDateMillis)?.let(onSelected) }) { Text(stringResource(R.string.select)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.renewal_unavailable)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) }) { padding ->
            EmptyState(stringResource(R.string.renewal_unavailable_title), stringResource(R.string.renewal_unavailable_body), Modifier.padding(padding))
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    containerColor = when (item.dueState()) {
                        DueState.OVERDUE -> MaterialTheme.colorScheme.errorContainer
                        DueState.DUE_TODAY, DueState.DUE_SOON -> MaterialTheme.colorScheme.tertiaryContainer
                        DueState.UPCOMING -> MaterialTheme.colorScheme.primaryContainer
                    },
                ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.type.emoji} ${item.type.displayName()}", style = MaterialTheme.typography.labelLarge)
                    Text(item.localizedDueMessage(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.due_date_format, item.dueEpochDay.asRenewDateLabel()))
                }
            }
            RenewDetailRow(stringResource(R.string.reminder), stringResource(R.string.days_before, item.reminderDays))
            if (item.provider.isNotBlank()) RenewDetailRow(stringResource(R.string.provider_issuer), item.provider)
            if (item.referenceNumber.isNotBlank()) RenewDetailRow(stringResource(R.string.reference), item.referenceNumber)
            item.lastRenewedEpochDay?.let { RenewDetailRow(stringResource(R.string.last_renewed), it.asRenewDateLabel()) }
            if (item.notes.isNotBlank()) RenewDetailRow(stringResource(R.string.notes), item.notes)
            Button(onClick = onRenew, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(stringResource(R.string.mark_renewed))
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
    var attemptedSubmit by rememberSaveable(item.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val dismissKeyboard = rememberKeyboardDismissAction()
    val newDateValid = newDueEpochDay > LocalDate.now().toEpochDay()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.mark_renewed)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(onClick = {
                    attemptedSubmit = true
                    if (newDateValid) {
                        dismissKeyboard()
                        onConfirm(newDueEpochDay, notes)
                    }
                }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) { Text(stringResource(R.string.save_new_due_date)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.currently_due, item.title, item.dueEpochDay.asRenewDateLabel()))
            ElevatedCard(onClick = { dismissKeyboard(); showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.new_due_date), style = MaterialTheme.typography.labelMedium)
                    Text(newDueEpochDay.asRenewDateLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (attemptedSubmit && !newDateValid) {
                Text(stringResource(R.string.error_new_due_date), color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(notes, { notes = it.take(500) }, label = { Text(stringResource(R.string.renewal_notes)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.renewal_history)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) },
    ) { padding ->
        if (events.isEmpty()) {
            EmptyState(stringResource(R.string.no_renewals_recorded), stringResource(R.string.no_renewals_recorded_body), Modifier.fillMaxSize().padding(padding))
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
                            Text(stringResource(R.string.history_date_range, event.previousDueEpochDay.asRenewDateLabel(), event.newDueEpochDay.asRenewDateLabel()))
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
        title = { Text(stringResource(R.string.delete_renewal_title, title.ifBlank { stringResource(R.string.renewal_fallback) })) },
        text = { Text(stringResource(R.string.delete_renewal_body)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
