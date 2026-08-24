package com.everycue.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Seed = Color(0xFF2F6657)
private val LightScheme = lightColorScheme(
    primary = Seed,
    secondary = Color(0xFF50645D),
    tertiary = Color(0xFF5D5F7A),
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF96D4BD),
    secondary = Color(0xFFB5CCC2),
    tertiary = Color(0xFFC5C4E7),
)

@Composable
fun EveryCueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

