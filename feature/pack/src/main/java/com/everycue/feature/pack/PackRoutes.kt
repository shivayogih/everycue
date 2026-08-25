package com.everycue.feature.pack

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object PackTripsRoute : NavKey
@Serializable data class CreatePackTripRoute(val templateId: String? = null) : NavKey
@Serializable data class EditPackTripRoute(val tripId: Long) : NavKey
@Serializable data class PackTripDetailRoute(val tripId: Long) : NavKey
@Serializable data object PackTemplatesRoute : NavKey
@Serializable data class PackTemplateDetailRoute(val templateId: String) : NavKey
@Serializable data class AddPackItemDialogRoute(val tripId: Long) : NavKey
@Serializable data class DeletePackTripDialogRoute(val tripId: Long, val tripName: String) : NavKey
