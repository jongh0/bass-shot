package com.jongh0.bassshot.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LeicaAmber,
    onPrimary = MatteBlack,
    secondary = TacticalCyan,
    onSecondary = MatteBlack,
    background = MatteBlack,
    onBackground = OpticalWhite,
    surface = DarkSurface,
    onSurface = OpticalWhite,
    surfaceVariant = SmokeGlass,
    onSurfaceVariant = CoolGrey,
    outline = HairlineBorder,
    error = ErrorRed,
    onError = OpticalWhite
)

@Composable
fun BassShotTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = MatteBlack.toArgb()
            window.navigationBarColor = MatteBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
