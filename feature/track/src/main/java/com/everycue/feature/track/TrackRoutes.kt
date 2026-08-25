package com.everycue.feature.track

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object TrackHomeRoute : NavKey
@Serializable data object TrackInventoryRoute : NavKey
@Serializable data object TrackHistoryRoute : NavKey
@Serializable data object TrackInsightsRoute : NavKey
@Serializable data object TrackUseNextRoute : NavKey
@Serializable data class TrackEditorRoute(val itemId: String? = null) : NavKey
@Serializable data class TrackDetailRoute(val itemId: String) : NavKey
@Serializable data class TrackOutcomeDialogRoute(val itemId: String, val outcome: TrackOutcome) : NavKey
@Serializable data class DeleteTrackItemDialogRoute(val itemId: String, val itemName: String) : NavKey
