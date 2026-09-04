package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.theme.ManropeFamily
import com.aalaya.pg.ui.theme.Palette

/** Initials for an avatar: first letters of the first two words, uppercased. */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/**
 * `.avatar` — a brand-muted circle carrying a resident's or staff member's
 * initials. Appears in payment rows, the profile header and the app bar.
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    val colors = HamsaTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(colors.brandMuted, CircleShape),
    ) {
        Text(
            text = initialsOf(name),
            style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (colors.isDark) colors.textPrimary else colors.brand,
        )
    }
}

/**
 * `.logo-mark` — the Telugu "హం" monogram on a brand tile. Used on the login
 * and welcome screens and stamped on receipts.
 */
@Composable
fun LogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = 10.dp,
) {
    val colors = HamsaTheme.colors
    val background = if (colors.isDark) {
        Brush.linearGradient(listOf(colors.brand, colors.brand))
    } else {
        Brush.linearGradient(listOf(Palette.BrandGradFrom, Palette.BrandGradTo))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(cornerRadius)),
    ) {
        Text(
            text = "హం",
            fontFamily = ManropeFamily,
            fontWeight = FontWeight.ExtraBold,
            // Scale the monogram with the tile so one composable covers the
            // 36dp app-bar mark and the 76dp splash mark.
            fontSize = (size.value * 0.36f).sp,
            color = Color.White,
        )
    }
}

/**
 * The account banner at the top of More and Profile: a brand-gradient plaque
 * carrying initials, a name and a role.
 *
 * This is the one place the app spends colour on identity rather than data, and
 * it anchors both account screens so they don't open on a wall of grey rows.
 * Dark mode flattens the gradient to a bordered surface, the same rule
 * [HeroCard] follows, because a saturated slab glows on a near-black canvas.
 */
@Composable
fun GradientIdentityCard(
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(HamsaTheme.radii.lg)
    val onGradient = if (colors.isDark) colors.textPrimary else Color.White
    val onGradientMuted = if (colors.isDark) colors.textSecondary else Color.White.copy(alpha = 0.75f)
    val background = if (colors.isDark) {
        Brush.linearGradient(listOf(colors.surface, colors.surface))
    } else {
        Brush.linearGradient(listOf(Palette.BrandGradFrom, Palette.BrandGradTo))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
        modifier = modifier
            .fillMaxWidth()
            .background(background, shape)
            .then(if (colors.isDark) Modifier.border(1.dp, colors.border, shape) else Modifier)
            .let { if (onClick != null) it.pressScale(onClick = onClick) else it }
            .padding(HamsaTheme.spacing.lg),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (colors.isDark) colors.brandMuted else Color.White.copy(alpha = 0.18f),
                    CircleShape,
                ),
        ) {
            Text(
                text = initialsOf(name),
                style = HamsaTheme.type.titleMedium,
                color = onGradient,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = HamsaTheme.type.titleMedium, color = onGradient)
            Text(
                text = subtitle,
                style = HamsaTheme.type.bodySmall,
                color = onGradientMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        trailing?.invoke()
    }
}
