package com.aalaya.pg.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Every ColorScheme slot is assigned explicitly. Slots left at their default
// resolve to Material's *baseline purple* palette, which is how the bottom
// nav's selected-tab pill (secondaryContainer) and the nav-bar/dialog
// backgrounds (surfaceContainer*) end up lavender in an otherwise azure app.
private val LightScheme = lightColorScheme(
    primary = Palette.Brand,
    onPrimary = Palette.Inverse,
    primaryContainer = Palette.BrandSubtle,
    onPrimaryContainer = Palette.Gold,
    inversePrimary = DarkPalette.Brand,

    // secondaryContainer drives the NavigationBar selected indicator.
    secondary = Palette.BrandHover,
    onSecondary = Palette.Inverse,
    secondaryContainer = Palette.BrandSubtle,
    onSecondaryContainer = Palette.Brand,

    tertiary = Palette.Gold,
    onTertiary = Palette.Inverse,
    tertiaryContainer = Palette.GoldBg,
    onTertiaryContainer = Palette.Gold,

    background = Palette.BgSubtle,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.BgSubtle,
    onSurfaceVariant = Palette.Text2,

    // Transparent kills Material's elevation tint overlay, which otherwise
    // washes every raised white card with primary.
    surfaceTint = Color.Transparent,

    inverseSurface = Palette.Toast,
    inverseOnSurface = Palette.Inverse,

    error = Palette.Error,
    onError = Palette.Inverse,
    errorContainer = Palette.ErrorBg,
    onErrorContainer = Palette.Error,

    outline = Palette.Border,
    outlineVariant = Palette.Border,
    scrim = Color(0x730F172A),          // .overlay — rgba(15,23,42,.45)

    surfaceBright = Palette.Surface,
    surfaceDim = Palette.BgSubtle,
    surfaceContainerLowest = Palette.Surface,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.Surface,
    surfaceContainerHighest = Palette.BgSubtle,
)

private val DarkScheme = darkColorScheme(
    primary = DarkPalette.Brand,
    onPrimary = DarkPalette.Inverse,
    primaryContainer = DarkPalette.BrandSubtle,
    onPrimaryContainer = DarkPalette.Brand,
    inversePrimary = Palette.Brand,

    secondary = DarkPalette.BrandHover,
    onSecondary = DarkPalette.Inverse,
    secondaryContainer = DarkPalette.BrandMuted,
    onSecondaryContainer = DarkPalette.Brand,

    tertiary = DarkPalette.Gold,
    onTertiary = DarkPalette.Inverse,
    tertiaryContainer = DarkPalette.GoldBg,
    onTertiaryContainer = DarkPalette.Gold,

    background = DarkPalette.Bg,
    onBackground = DarkPalette.Text,
    surface = DarkPalette.Surface,
    onSurface = DarkPalette.Text,
    surfaceVariant = DarkPalette.BgSubtle,
    onSurfaceVariant = DarkPalette.Text2,

    surfaceTint = Color.Transparent,

    inverseSurface = DarkPalette.Toast,
    inverseOnSurface = DarkPalette.Text,

    error = DarkPalette.Error,
    onError = DarkPalette.Inverse,
    errorContainer = DarkPalette.ErrorBg,
    onErrorContainer = DarkPalette.Error,

    outline = DarkPalette.Border,
    outlineVariant = DarkPalette.Border,
    scrim = Color(0xB3000000),

    surfaceBright = DarkPalette.BorderStrong,
    surfaceDim = DarkPalette.Bg,
    surfaceContainerLowest = DarkPalette.Bg,
    surfaceContainerLow = DarkPalette.BgSubtle,
    surfaceContainer = DarkPalette.Surface,
    surfaceContainerHigh = DarkPalette.Surface,
    surfaceContainerHighest = DarkPalette.BorderStrong,
)

/**
 * Root theme. Selects light/dark by system setting, layers the extended
 * color/spacing/radii/elevation tokens onto CompositionLocals, and keeps the
 * system-bar icon contrast correct for the current mode.
 */
@Composable
fun HamsaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val hamsaColors = if (darkTheme) DarkHamsaColors else LightHamsaColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            // Both bars matter: the app draws edge-to-edge, so light content
            // needs dark icons above *and* below.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalHamsaColors provides hamsaColors,
        LocalSpacing provides Spacing(),
        LocalRadii provides Radii(),
        LocalElevation provides Elevation(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HamsaTypography,
            content = content,
        )
    }
}

/** Ergonomic accessors so screens can write `HamsaTheme.colors.warningFg`, etc. */
object HamsaTheme {
    val colors: HamsaColors
        @Composable @ReadOnlyComposable get() = LocalHamsaColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val radii: Radii
        @Composable @ReadOnlyComposable get() = LocalRadii.current
    val elevation: Elevation
        @Composable @ReadOnlyComposable get() = LocalElevation.current
    val type: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography
}
