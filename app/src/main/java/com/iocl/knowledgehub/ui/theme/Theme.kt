package com.iocl.knowledgehub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val IOCLLightColorScheme = lightColorScheme(
    primary = IOCLNavy,
    onPrimary = IOCLWhite,
    primaryContainer = IOCLBluePale,
    onPrimaryContainer = IOCLNavyDark,
    secondary = IOCLSaffron,
    onSecondary = IOCLWhite,
    secondaryContainer = IOCLOrangePale,
    onSecondaryContainer = IOCLSaffronDark,
    tertiary = IOCLRed,
    onTertiary = IOCLWhite,
    tertiaryContainer = IOCLRedSoft,
    onTertiaryContainer = IOCLRed,
    error = Color(0xFFBA1A1A),
    onError = IOCLWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = IOCLOffWhite,
    onBackground = IOCLBlack,
    surface = IOCLWhite,
    onSurface = IOCLBlack,
    surfaceVariant = IOCLSnow,
    onSurfaceVariant = IOCLGrayDark,
    surfaceTint = IOCLNavy,
    outline = IOCLGrayLight,
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = IOCLInk,
    inverseOnSurface = IOCLSnow,
    inversePrimary = IOCLBlueAccent,
    surfaceBright = IOCLWhite,
    surfaceDim = IOCLSnow,
    surfaceContainer = IOCLGrayUltraLight,
    surfaceContainerHigh = IOCLSnow,
    surfaceContainerHighest = IOCLGrayLight,
    surfaceContainerLow = IOCLOffWhite,
    surfaceContainerLowest = IOCLWhite
)

private val IOCLDarkColorScheme = darkColorScheme(
    primary = IOCLBlueAccent,
    onPrimary = IOCLNavyDark,
    primaryContainer = IOCLNavyLight,
    onPrimaryContainer = IOCLBluePale,
    secondary = IOCLSaffronLight,
    onSecondary = IOCLSaffronDark,
    secondaryContainer = Color(0xFF5C3D00),
    onSecondaryContainer = IOCLOrangePale,
    tertiary = IOCLRedLight,
    onTertiary = Color(0xFF680003),
    tertiaryContainer = Color(0xFF930006),
    onTertiaryContainer = IOCLRedSoft,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkSurface,
    onBackground = IOCLSnow,
    surface = DarkSurface,
    onSurface = IOCLSnow,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = IOCLGrayMedium,
    surfaceTint = IOCLBlueAccent,
    outline = IOCLGray,
    outlineVariant = Color(0xFF49454F),
    inverseSurface = IOCLSnow,
    inverseOnSurface = IOCLInk,
    inversePrimary = IOCLNavy,
    surfaceBright = DarkSurfaceOverlay,
    surfaceDim = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceCard,
    surfaceContainerHighest = DarkSurfaceCardElevated,
    surfaceContainerLow = Color(0xFF0E1525),
    surfaceContainerLowest = Color(0xFF060A14)
)

@Composable
fun IOCLKnowledgeHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> IOCLDarkColorScheme
        else -> IOCLLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IOCLTypography,
        shapes = IOCLShapes,
        content = content
    )
}
