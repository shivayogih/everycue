package com.everycue.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everycue.feature.pack.PackData
import com.everycue.feature.pack.Trip
import com.everycue.feature.renew.RenewUiState
import com.everycue.feature.renew.RenewalItem
import com.everycue.feature.track.TrackItem
import com.everycue.feature.track.TrackUiState

private sealed interface HomeAlert {
    val stableKey: String
    data class Track(val item: TrackItem) : HomeAlert { override val stableKey = "track-${item.id}" }
    data class Pack(val trip: Trip) : HomeAlert { override val stableKey = "pack-${trip.id}" }
    data class Renew(val item: RenewalItem) : HomeAlert { override val stableKey = "renew-${item.id}" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: LocalProfile?,
    track: TrackUiState,
    pack: PackData,
    renew: RenewUiState,
    onTrack: () -> Unit,
    onPack: () -> Unit,
    onRenew: () -> Unit,
    onTrackItem: (String) -> Unit,
    onPackTrip: (Long) -> Unit,
    onRenewal: (String) -> Unit,
) {
    val unpacked = remember(pack) { pack.trips.sumOf { trip -> trip.items.count { !it.isPacked } } }
    val alerts = remember(track.items, pack.trips, renew.renewals) {
        buildList {
            track.urgentItems.take(2).forEach { add(HomeAlert.Track(it)) }
            renew.urgent.take(2).forEach { add(HomeAlert.Renew(it)) }
            pack.trips.firstOrNull { it.items.any { item -> !item.isPacked } }?.let { add(HomeAlert.Pack(it)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold)
                        Text(
                            profile?.firstName?.takeIf(String::isNotBlank)?.let { stringResource(R.string.home_greeting, it) }
                                ?: stringResource(R.string.home_greeting_fallback),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer),
                            ),
                            MaterialTheme.shapes.extraLarge,
                        )
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.home_overview_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.home_overview_summary, track.expiredCount + track.expiringSoonCount, unpacked, renew.overdueCount + renew.dueSoonCount),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item { Text(stringResource(R.string.home_features), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                FeatureCard(
                    title = stringResource(R.string.nav_track),
                    description = stringResource(R.string.home_track_description),
                    status = stringResource(R.string.home_track_status, track.items.size, track.expiredCount + track.expiringSoonCount),
                    icon = Icons.Default.Inventory2,
                    onClick = onTrack,
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.nav_pack),
                    description = stringResource(R.string.home_pack_description),
                    status = stringResource(R.string.home_pack_status, pack.trips.size, unpacked),
                    icon = Icons.Default.Luggage,
                    onClick = onPack,
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.nav_renew),
                    description = stringResource(R.string.home_renew_description),
                    status = stringResource(R.string.home_renew_status, renew.renewals.size, renew.overdueCount + renew.dueSoonCount),
                    icon = Icons.Default.EventRepeat,
                    onClick = onRenew,
                )
            }
            item {
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.home_attention), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (alerts.isEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.home_all_clear), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.home_all_clear_body), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            } else {
                items(alerts, key = HomeAlert::stableKey) { alert ->
                    when (alert) {
                        is HomeAlert.Track -> AttentionCard(
                            title = alert.item.name,
                            detail = dayStatus(alert.item.daysRemaining()),
                            icon = Icons.Default.Inventory2,
                            onClick = { onTrackItem(alert.item.id) },
                        )
                        is HomeAlert.Pack -> AttentionCard(
                            title = alert.trip.name,
                            detail = stringResource(R.string.home_items_left, alert.trip.items.count { !it.isPacked }),
                            icon = Icons.Default.Luggage,
                            onClick = { onPackTrip(alert.trip.id) },
                        )
                        is HomeAlert.Renew -> AttentionCard(
                            title = alert.item.title,
                            detail = dayStatus(alert.item.daysRemaining()),
                            icon = Icons.Default.EventRepeat,
                            onClick = { onRenewal(alert.item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, description: String, status: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AttentionCard(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun dayStatus(days: Long): String = when {
    days < 0 -> stringResource(R.string.home_overdue_days, -days)
    days == 0L -> stringResource(R.string.home_due_today)
    days == 1L -> stringResource(R.string.home_due_tomorrow)
    else -> stringResource(R.string.home_due_days, days)
}
