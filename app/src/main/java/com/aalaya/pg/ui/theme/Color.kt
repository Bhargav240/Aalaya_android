package com.aalaya.pg.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Raw palette ─────────────────────────────────────────────────────────────
// Transcribed 1:1 from the design system's CSS custom properties (`:root` in
// the Aalaya mockup) and the Figma "01 · Design System" page. Treat these
// as the canonical values — screens must read them through MaterialTheme
// .colorScheme or HamsaTheme.colors, never by referencing Palette directly.
internal object Palette {
    val Brand = Color(0xFF0E70C7)          // --brand
    val BrandHover = Color(0xFF0A589B)     // --brand-hover
    val BrandGradFrom = Color(0xFF288DD9)  // --brand-grad start
    val BrandGradTo = Color(0xFF073053)    // --brand-grad end
    val BrandSubtle = Color(0xFFE9F3F6)    // --brand-subtle
    val BrandMuted = Color(0xFFCEE2EA)     // --brand-muted

    val Gold = Color(0xFF073053)           // --gold
    val GoldBg = Color(0xFFDEEBF1)         // --gold-bg

    val Bg = Color(0xFFFFFFFF)             // --bg
    val BgSubtle = Color(0xFFF2F6F8)       // --bg-subtle  (Android canvas)
    val Surface = Color(0xFFFFFFFF)        // --surface

    val Text = Color(0xFF0D1922)           // --text
    val Text2 = Color(0xFF7E96A7)          // --text-2
    val Text3 = Color(0xFF94A7B5)          // --text-3
    val Inverse = Color(0xFFFFFFFF)        // --inverse

    val Border = Color(0xFFDBE4EB)         // --border
    val BorderStrong = Color(0xFFB9CEDB)   // --border-strong

    val Success = Color(0xFF3D7A46)        // --success
    val SuccessBg = Color(0xFFEFF6EE)      // --success-bg
    val Warn = Color(0xFFA16207)           // --warn
    val WarnBg = Color(0xFFFBF3E2)         // --warn-bg
    val Error = Color(0xFFB91C1C)          // --error
    val ErrorBg = Color(0xFFFBEDEA)        // --error-bg

    // Tenant rent banner (.hero / .hero.paid)
    val HeroFrom = Color(0xFF4990C4)
    val HeroTo = Color(0xFF0F2A41)
    val HeroPaidFrom = Color(0xFF3D7A46)
    val HeroPaidTo = Color(0xFF22452A)

    val Toast = Color(0xFF081E30)          // .toast background
    val ToastTick = Color(0xFF8FCF9B)      // .toast .tick
}

/**
 * Dark palette — deliberately NOT a tinted version of the light one. The design
 * system specifies a flat, quiet, near-black treatment ("Instagram/Snapchat
 * style"): pure-black canvas, neutral greys, no shadows, and a 1px border on
 * every surface to replace elevation.
 */
internal object DarkPalette {
    val Brand = Color(0xFF669FCC)
    val BrandHover = Color(0xFF518EBC)
    val BrandSubtle = Color(0xFF121D24)
    val BrandMuted = Color(0xFF1B2B34)

    val Gold = Color(0xFF669FCC)
    val GoldBg = Color(0xFF121D24)

    val Bg = Color(0xFF000000)
    val BgSubtle = Color(0xFF121212)
    val Surface = Color(0xFF161616)

    val Text = Color(0xFFF2F2F2)
    val Text2 = Color(0xFFA0A0A0)
    val Text3 = Color(0xFF6B6B6B)
    val Inverse = Color(0xFF000000)

    val Border = Color(0xFF272727)
    val BorderStrong = Color(0xFF333333)

    val Success = Color(0xFF3DBE72)
    val SuccessBg = Color(0xFF122016)
    val Warn = Color(0xFFD9A441)
    val WarnBg = Color(0xFF221B0D)
    val Error = Color(0xFFE5605F)
    val ErrorBg = Color(0xFF241313)

    val Toast = Color(0xFF1E1E1E)
    val ToastTick = Color(0xFF3DBE72)
}

// ─── Extended semantic colors ────────────────────────────────────────────────
// Material3's ColorScheme has no slot for "warning", for paired fg/bg status
// tints, or for the design's three-step text ramp, so they live here and reach
// screens via LocalHamsaColors.
@Immutable
data class HamsaColors(
    val brand: Color,
    val brandHover: Color,
    val brandSubtle: Color,
    val brandMuted: Color,
    val gold: Color,
    val goldBg: Color,
    val canvas: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val borderStrong: Color,
    val successFg: Color,
    val successBg: Color,
    val warningFg: Color,
    val warningBg: Color,
    val errorFg: Color,
    val errorBg: Color,
    val heroFrom: Color,
    val heroTo: Color,
    val heroPaidFrom: Color,
    val heroPaidTo: Color,
    val toast: Color,
    val toastTick: Color,
    /**
     * Light mode leans on soft azure shadows for depth; dark mode is flat and
     * uses hairline borders instead. Components branch on this rather than
     * re-deriving "am I dark?" from the color scheme.
     */
    val isDark: Boolean,
)

val LightHamsaColors = HamsaColors(
    brand = Palette.Brand,
    brandHover = Palette.BrandHover,
    brandSubtle = Palette.BrandSubtle,
    brandMuted = Palette.BrandMuted,
    gold = Palette.Gold,
    goldBg = Palette.GoldBg,
    canvas = Palette.BgSubtle,
    surface = Palette.Surface,
    textPrimary = Palette.Text,
    textSecondary = Palette.Text2,
    textTertiary = Palette.Text3,
    border = Palette.Border,
    borderStrong = Palette.BorderStrong,
    successFg = Palette.Success,
    successBg = Palette.SuccessBg,
    warningFg = Palette.Warn,
    warningBg = Palette.WarnBg,
    errorFg = Palette.Error,
    errorBg = Palette.ErrorBg,
    heroFrom = Palette.HeroFrom,
    heroTo = Palette.HeroTo,
    heroPaidFrom = Palette.HeroPaidFrom,
    heroPaidTo = Palette.HeroPaidTo,
    toast = Palette.Toast,
    toastTick = Palette.ToastTick,
    isDark = false,
)

val DarkHamsaColors = HamsaColors(
    brand = DarkPalette.Brand,
    brandHover = DarkPalette.BrandHover,
    brandSubtle = DarkPalette.BrandSubtle,
    brandMuted = DarkPalette.BrandMuted,
    gold = DarkPalette.Gold,
    goldBg = DarkPalette.GoldBg,
    canvas = DarkPalette.Bg,
    surface = DarkPalette.Surface,
    textPrimary = DarkPalette.Text,
    textSecondary = DarkPalette.Text2,
    textTertiary = DarkPalette.Text3,
    border = DarkPalette.Border,
    borderStrong = DarkPalette.BorderStrong,
    successFg = DarkPalette.Success,
    successBg = DarkPalette.SuccessBg,
    warningFg = DarkPalette.Warn,
    warningBg = DarkPalette.WarnBg,
    errorFg = DarkPalette.Error,
    errorBg = DarkPalette.ErrorBg,
    // Dark mode flattens the hero to a plain surface (body.dark .hero).
    heroFrom = DarkPalette.Surface,
    heroTo = DarkPalette.Surface,
    heroPaidFrom = DarkPalette.Surface,
    heroPaidTo = DarkPalette.Surface,
    toast = DarkPalette.Toast,
    toastTick = DarkPalette.ToastTick,
    isDark = true,
)

val LocalHamsaColors = staticCompositionLocalOf { LightHamsaColors }
