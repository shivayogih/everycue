package com.everycue.feature.renew

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object RenewHomeRoute : NavKey
@Serializable data object RenewListRoute : NavKey
@Serializable data object RenewHistoryRoute : NavKey
@Serializable data object RenewInsightsRoute : NavKey
@Serializable data class RenewEditorRoute(val renewalId: String? = null) : NavKey
@Serializable data class RenewDetailRoute(val renewalId: String) : NavKey
@Serializable data class MarkRenewedRoute(val renewalId: String) : NavKey
@Serializable data class DeleteRenewalDialogRoute(val renewalId: String, val title: String) : NavKey
