package com.everycue.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Color(0xFF276EF1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F2FF),
    onPrimaryContainer = Color(0xFF0B2C5F),
    secondary = Color(0xFF00A86B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F9F1),
    onSecondaryContainer = Color(0xFF053D2B),
    tertiary = Color(0xFF6D5CE7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0EDFF),
    onTertiaryContainer = Color(0xFF281D73),
    error = Color(0xFFDC315B),
    errorContainer = Color(0xFFFFE8EE),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF181C2D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C2D),
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF5F687B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F3F8),
    surfaceContainerHigh = Color(0xFFECEFF5),
    surfaceContainerHighest = Color(0xFFE5E8F0),
    outline = Color(0xFFAEB7C6),
    outlineVariant = Color(0xFFDDE2EA),
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8EB5FF),
    onPrimary = Color(0xFF002B69),
    primaryContainer = Color(0xFF174B9C),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFF51D8A4),
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF005137),
    onSecondaryContainer = Color(0xFFB7F5DB),
    tertiary = Color(0xFFBFB4FF),
    onTertiary = Color(0xFF33277D),
    tertiaryContainer = Color(0xFF4B4097),
    onTertiaryContainer = Color(0xFFE4DFFF),
    error = Color(0xFFFFB1C0),
    errorContainer = Color(0xFF8C1538),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE7EAF1),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE7EAF1),
    surfaceVariant = Color(0xFF30394A),
    onSurfaceVariant = Color(0xFFC1C8D6),
    surfaceContainerLowest = Color(0xFF080E19),
    surfaceContainerLow = Color(0xFF111A2B),
    surfaceContainer = Color(0xFF172237),
    surfaceContainerHigh = Color(0xFF202C42),
    surfaceContainerHighest = Color(0xFF29364D),
    outline = Color(0xFF8B95A8),
    outlineVariant = Color(0xFF3D4759),
)

private val EveryCueShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val BaseTypography = Typography()
private val EveryCueTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.25).sp,
    ),
    displayMedium = BaseTypography.displayMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.9).sp,
    ),
    displaySmall = BaseTypography.displaySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.35).sp,
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
)

@Composable
fun EveryCueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = EveryCueShapes,
        typography = EveryCueTypography,
        content = content,
    )
}
