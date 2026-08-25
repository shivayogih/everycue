package com.everycue.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object SettingsRoute : NavKey
@Serializable data object HomeRoute : NavKey
@Serializable data object AboutRoute : NavKey
@Serializable data object ProfileRoute : NavKey
