package com.aalaya.pg.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. `screen`/`card`/`row` mirror the design's concrete paddings. */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    /** `main { padding: 20px 16px 40px }` */
    val screenH: Dp = 16.dp,
    val screenTop: Dp = 20.dp,
    val screenBottom: Dp = 40.dp,
    /** `.card { padding: 18px }` */
    val card: Dp = 18.dp,
    /** `.stat { padding: 16px }` */
    val stat: Dp = 16.dp,
    /** `.list-row { padding: 13px 0 }` */
    val rowV: Dp = 13.dp,
    /** Gap between stacked cards (`.stack { gap: 14px }`). */
    val stack: Dp = 14.dp,
)

/** Corner radii — `--r-sm` … `--r-xl` plus the pill used by chips and badges. */
@Immutable
data class Radii(
    val sm: Dp = 8.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val pill: Dp = 999.dp,
)

/**
 * Elevation steps standing in for the design's layered azure drop shadows
 * (`--sh-card`, `--sh-elev`, `--sh-modal`). Compose renders a single shadow per
 * surface, so these are the closest single-layer equivalents; the azure tint
 * comes from [ShadowTint]. Dark mode drops shadows entirely and uses hairline
 * borders instead, matching `body.dark { --sh-card: none }`.
 */
@Immutable
data class Elevation(
    val card: Dp = 2.dp,
    val raised: Dp = 8.dp,
    val modal: Dp = 24.dp,
    val none: Dp = 0.dp,
)

/**
 * The azure the design tints its drop shadows with. Compose renders one shadow
 * layer using this colour at its own alpha curve, so a fully-saturated
 * rgb(2,132,199) reads as a blue halo rather than the design's barely-there
 * `rgba(2,132,199,.09)`. Desaturating toward slate keeps the cool cast without
 * the glow.
 */
val ShadowTint = Color(0xFF5B7C93)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalRadii = staticCompositionLocalOf { Radii() }
val LocalElevation = staticCompositionLocalOf { Elevation() }
