package com.tv.upload.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val appTypography = Typography(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 23.sp,
        )
    )
    val darkColorScheme = darkColorScheme().copy(
        background = Black
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
        colorScheme = darkColorScheme,
        typography = appTypography,
        content = content
    )
}