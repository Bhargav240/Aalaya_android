package com.aalaya.pg.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aalaya.pg.R

// Design system fonts: Inter for UI/body text, Manrope (800) for headlines and
// money amounts. Both ship as variable-font files and are sliced per weight via
// the `wght` axis, so one file covers the whole ramp.
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font =
    Font(resId, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val InterFamily: FontFamily = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold),
    variableFont(R.font.inter, FontWeight.Bold),
)

val ManropeFamily: FontFamily = FontFamily(
    variableFont(R.font.manrope, FontWeight.SemiBold),
    variableFont(R.font.manrope, FontWeight.Bold),
    variableFont(R.font.manrope, FontWeight.ExtraBold),
)

/**
 * Every Material3 slot is defined on purpose. Any slot left null falls back to
 * Material's default Roboto ramp, which is how stock components (nav-bar
 * labels, app-bar titles, dialogs, snackbars) end up in the wrong typeface
 * while the rest of the app is Inter/Manrope.
 *
 * Mapping to the design system's CSS:
 *   .splash h1  → displayLarge      h1        → titleLarge / headlineMedium
 *   .auth h1    → displayMedium     h2        → titleMedium
 *   .hero .amt  → HeroAmountStyle   base 15px → bodyLarge
 *   .stat .v    → AmountTextStyle   .small    → bodySmall
 *   .overline   → OverlineStyle     .tiny     → labelMedium
 *   .btn        → labelLarge        bottomnav → labelSmall
 */
val HamsaTypography = Typography(
    // ── Display: welcome/splash headlines (Manrope 800) ──
    displayLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),

    // ── Headline: in-sheet and dialog titles ──
    headlineLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    // AlertDialog titles land here — keep them Manrope, not Roboto.
    headlineSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),

    // ── Title: TopAppBar uses titleLarge; the design's app bar is the big
    //    Manrope screen title, so it maps straight onto that slot. ──
    titleLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(          // h2 — card section heads
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),

    // ── Body: 15px / 1.47 is the design's base rule ──
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(           // dialog body, snackbar
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(            // .small
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),

    // ── Label: buttons and navigation ──
    labelLarge = TextStyle(           // .btn — 15px/500
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(          // .tiny
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(           // bottom-nav labels — 11px/500
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp,
    ),
)

/** `.stat .v` / `.amount` — money amounts and stat-tile values. */
val AmountTextStyle = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.5).sp,
)

/** `.hero .amt` — the tenant rent banner's headline figure. */
val HeroAmountTextStyle = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 42.sp,
    lineHeight = 50.sp,
    letterSpacing = (-1).sp,
)

/** `.overline` — tiny uppercase label above stat values. */
val OverlineTextStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.8.sp,
)

/** `.flabel` — uppercase field label above dropdowns in the explorer controls. */
val FieldLabelTextStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.7.sp,
)
