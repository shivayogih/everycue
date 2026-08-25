package com.everycue.feature.pack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everycue.core.designsystem.DismissKeyboardOnScroll
import com.everycue.core.recommendation.ReadinessFinding
import com.everycue.core.recommendation.TripReadyEvaluator
import com.everycue.core.recommendation.TripReadyInput
import com.everycue.core.recommendation.TripReadyReasonCode
import com.everycue.core.recommendation.TripReadyRecordInput
import com.everycue.core.recommendation.TripReadySeverity
import com.everycue.core.recommendation.TripReadySource

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReadyScreen(
    trip: Trip?,
    links: List<TripLink>,
    trackRecords: List<TripReadyTrackRecord>,
    renewalRecords: List<TripReadyRenewalRecord>,
    onBack: () -> Unit,
    onEditTrip: () -> Unit,
    onToggleTrack: (String, Boolean) -> Unit,
    onToggleRenewal: (String, Boolean) -> Unit,
    onOpenTrack: (String) -> Unit,
    onOpenRenewal: (String) -> Unit,
) {
    if (trip == null) {
        TripReadyMissingTrip(onBack)
        return
    }
    var query by rememberSaveable(trip.id) { mutableStateOf("") }
    val trackIds = remember(links, trip.id) {
        links.asSequence()
            .filter { it.tripId == trip.id && it.entityType == TripLinkEntityType.TRACK }
            .mapTo(mutableSetOf(), TripLink::entityId)
    }
    val renewalIds = remember(links, trip.id) {
        links.asSequence()
            .filter { it.tripId == trip.id && it.entityType == TripLinkEntityType.RENEW }
            .mapTo(mutableSetOf(), TripLink::entityId)
    }
    val readiness = remember(trip, trackIds, renewalIds, trackRecords, renewalRecords) {
        TripReadyEvaluator.evaluate(
            TripReadyInput(
                tripStartEpochDay = trip.startDateMillis?.let { Math.floorDiv(it, MILLIS_PER_DAY) },
                tripEndEpochDay = trip.endDateMillis?.let { Math.floorDiv(it, MILLIS_PER_DAY) },
                packedCount = trip.packedCount,
                totalCount = trip.totalCount,
                linkedTrackItems = trackRecords.asSequence()
                    .filter { it.id in trackIds }
                    .map { TripReadyRecordInput(it.id, it.name, it.expiryEpochDay) }
                    .toList(),
                linkedRenewals = renewalRecords.asSequence()
                    .filter { it.id in renewalIds }
                    .map { TripReadyRecordInput(it.id, it.title, it.dueEpochDay) }
                    .toList(),
            ),
        )
    }
    val search = query.trim()
    val visibleTrack = remember(trackRecords, search) {
        trackRecords.filter { search.isEmpty() || it.name.contains(search, ignoreCase = true) }
    }
    val visibleRenewals = remember(renewalRecords, search) {
        renewalRecords.filter { search.isEmpty() || it.title.contains(search, ignoreCase = true) }
    }
    val listState = rememberLazyListState()
    DismissKeyboardOnScroll(listState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.trip_ready_title), fontWeight = FontWeight.Bold)
                        Text(trip.name, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TripReadySummary(readiness.criticalCount, readiness.attentionCount, readiness.isReady) }
            if (trip.startDateMillis == null) {
                item {
                    FilledTonalButton(onClick = onEditTrip, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null)
                        Text(stringResource(R.string.trip_ready_add_dates), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            TripReadySeverity.entries.forEach { severity ->
                val findings = readiness.findings.filter { it.severity == severity }
                if (findings.isNotEmpty()) {
                    item(key = "readiness-${severity.name}") {
                        Text(
                            stringResource(severity.titleResource()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(findings, key = { "${it.source}-${it.sourceId}-${it.reasonCode}" }) { finding ->
                        TripReadyFindingCard(
                            finding = finding,
                            packedCount = trip.packedCount,
                            totalCount = trip.totalCount,
                            onClick = when (finding.source) {
                                TripReadySource.TRACK -> finding.sourceId?.let { id -> { onOpenTrack(id) } }
                                TripReadySource.RENEW -> finding.sourceId?.let { id -> { onOpenRenewal(id) } }
                                TripReadySource.PACK -> null
                            },
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.trip_ready_link_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.trip_ready_link_help),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.trip_ready_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { TripReadyLinkHeading(stringResource(R.string.trip_ready_track_records)) }
            if (visibleTrack.isEmpty()) {
                item { TripReadyEmptyLinkState(stringResource(R.string.trip_ready_no_track_records)) }
            } else {
                items(visibleTrack, key = { "track-${it.id}" }) { record ->
                    TripReadyLinkRow(
                        name = record.name,
                        date = (record.expiryEpochDay * MILLIS_PER_DAY).asDateLabel(),
                        checked = record.id in trackIds,
                        onCheckedChange = { onToggleTrack(record.id, it) },
                    )
                }
            }
            item { TripReadyLinkHeading(stringResource(R.string.trip_ready_renewal_records)) }
            if (visibleRenewals.isEmpty()) {
                item { TripReadyEmptyLinkState(stringResource(R.string.trip_ready_no_renewal_records)) }
            } else {
                items(visibleRenewals, key = { "renew-${it.id}" }) { record ->
                    TripReadyLinkRow(
                        name = record.title,
                        date = (record.dueEpochDay * MILLIS_PER_DAY).asDateLabel(),
                        checked = record.id in renewalIds,
                        onCheckedChange = { onToggleRenewal(record.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TripReadySummary(critical: Int, attention: Int, ready: Boolean) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (ready) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Column {
                Text(
                    stringResource(if (ready) R.string.trip_ready_all_clear else R.string.trip_ready_action_needed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.trip_ready_summary_counts, critical, attention))
            }
        }
    }
}

@Composable
private fun TripReadyFindingCard(
    finding: ReadinessFinding,
    packedCount: Int,
    totalCount: Int,
    onClick: (() -> Unit)?,
) {
    val icon: ImageVector = when (finding.severity) {
        TripReadySeverity.CRITICAL -> Icons.Default.Error
        TripReadySeverity.NEEDS_ATTENTION -> Icons.Default.Warning
        TripReadySeverity.READY -> Icons.Default.CheckCircle
    }
    val modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = finding.severity.color())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(finding.title(), fontWeight = FontWeight.SemiBold)
                Text(
                    finding.message(packedCount, totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onClick != null) {
                    Text(
                        stringResource(R.string.trip_ready_open_record),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadinessFinding.title(): String = if (displayName.isNotBlank()) displayName else when (reasonCode) {
    TripReadyReasonCode.TRIP_DATES_MISSING -> stringResource(R.string.trip_ready_trip_dates)
    else -> stringResource(R.string.trip_ready_packing)
}

@Composable
private fun ReadinessFinding.message(packedCount: Int, totalCount: Int): String {
    val date = relevantEpochDay?.let { (it * MILLIS_PER_DAY).asDateLabel() }.orEmpty()
    return when (reasonCode) {
        TripReadyReasonCode.TRIP_DATES_MISSING -> stringResource(R.string.trip_ready_dates_missing)
        TripReadyReasonCode.PACKING_LIST_EMPTY -> stringResource(R.string.trip_ready_packing_empty)
        TripReadyReasonCode.PACKING_INCOMPLETE -> stringResource(R.string.trip_ready_packing_incomplete, packedCount, totalCount)
        TripReadyReasonCode.PACKING_COMPLETE -> stringResource(R.string.trip_ready_packing_complete)
        TripReadyReasonCode.TRACK_EXPIRES_BEFORE_TRIP -> stringResource(R.string.trip_ready_track_before, date)
        TripReadyReasonCode.TRACK_EXPIRES_DURING_TRIP -> stringResource(R.string.trip_ready_track_during, date)
        TripReadyReasonCode.TRACK_VALID_THROUGH_TRIP -> stringResource(R.string.trip_ready_track_valid, date)
        TripReadyReasonCode.RENEWAL_DUE_BEFORE_TRIP -> stringResource(R.string.trip_ready_renew_before, date)
        TripReadyReasonCode.RENEWAL_DUE_DURING_TRIP -> stringResource(R.string.trip_ready_renew_during, date)
        TripReadyReasonCode.RENEWAL_VALID_THROUGH_TRIP -> stringResource(R.string.trip_ready_renew_valid, date)
    }
}

@Composable
private fun TripReadySeverity.titleResource(): Int = when (this) {
    TripReadySeverity.CRITICAL -> R.string.trip_ready_critical
    TripReadySeverity.NEEDS_ATTENTION -> R.string.trip_ready_needs_attention
    TripReadySeverity.READY -> R.string.trip_ready_ready
}

@Composable
private fun TripReadySeverity.color() = when (this) {
    TripReadySeverity.CRITICAL -> MaterialTheme.colorScheme.error
    TripReadySeverity.NEEDS_ATTENTION -> MaterialTheme.colorScheme.tertiary
    TripReadySeverity.READY -> MaterialTheme.colorScheme.primary
}

@Composable
private fun TripReadyLinkHeading(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Link, contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TripReadyLinkRow(name: String, date: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(name, fontWeight = FontWeight.Medium)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TripReadyEmptyLinkState(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripReadyMissingTrip(onBack: () -> Unit) {
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
