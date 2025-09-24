package com.tv.upload.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = onPrimary,
    secondary = Secondary,
    onSecondary = onSecondary,
    tertiary = Tertiary,
    surfaceVariant = Primary,
    primaryContainer = Primary,
    secondaryContainer = Primary,
    onPrimaryContainer = Primary,
    onSecondaryContainer = Primary,
    tertiaryContainer = Primary,
    onTertiaryContainer = Primary
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val appTypography = Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(
            fontSize = 27.sp,
        ),
        displayMedium = MaterialTheme.typography.displayMedium.copy(
            fontSize = 25.sp,
        ),
        displaySmall = MaterialTheme.typography.displaySmall.copy(
            fontSize = 23.sp,
        ),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 27.sp,
        ),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 19.sp,
        ),
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontSize = 27.sp,
        ),
        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontSize = 21.sp,
        ),
        titleSmall = MaterialTheme.typography.titleSmall.copy(
            fontSize = 19.sp,
        ),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 23.sp,
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 19.sp,
        ),
        bodySmall = MaterialTheme.typography.bodySmall.copy(
            fontSize = 16.sp,
        ),
        labelLarge = MaterialTheme.typography.labelLarge.copy(
            fontSize = 16.sp,
        ),
        labelMedium = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
        ),
        labelSmall = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
        ),
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = appTypography,
        content = content
    )
}