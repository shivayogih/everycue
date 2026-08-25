package com.everycue.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2DD),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635B),
    secondaryContainer = Color(0xFFCDE8DF),
    tertiary = Color(0xFF53627A),
    tertiaryContainer = Color(0xFFD9E3FF),
    background = Color(0xFFF6FBF8),
    surface = Color(0xFFF6FBF8),
    surfaceVariant = Color(0xFFDBE5E0),
    outline = Color(0xFF6F7975),
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF82D5C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005143),
    secondary = Color(0xFFB2CCC3),
    secondaryContainer = Color(0xFF344B44),
    tertiary = Color(0xFFBBC7E4),
    tertiaryContainer = Color(0xFF3B4961),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceVariant = Color(0xFF3F4945),
)

private val EveryCueShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
        shapes = EveryCueShapes,
        content = content,
    )
}
