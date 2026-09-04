package com.aalaya.pg.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/** `.btn` size variants: default (48dp) and `.btn.sm` (36dp). */
enum class ButtonSize { Default, Small }

/** `.btn-primary` / `.btn-secondary` / `.btn-ghost` / `.btn-danger`. */
enum class ButtonVariant { Primary, Secondary, Ghost, Destructive }

/**
 * Single implementation behind all four button variants. Built on a Box rather
 * than Material's Button so its focus, pressed, loading and disabled states
 * remain consistent across the app.
 *
 * Primary actions use a confident solid blue rather than a repeated gradient.
 */
@Composable
fun HamsaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(
        if (size == ButtonSize.Small) 10.dp else HamsaTheme.radii.md,
    )
    val minHeight = if (size == ButtonSize.Small) 36.dp else 48.dp
    val padding = if (size == ButtonSize.Small) {
        PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    } else {
        PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    }
    val textStyle = if (size == ButtonSize.Small) {
        HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.Medium)
    } else {
        HamsaTheme.type.labelLarge
    }

    val contentColor = when (variant) {
        ButtonVariant.Primary, ButtonVariant.Destructive -> Color.White
        ButtonVariant.Secondary -> colors.textPrimary
        ButtonVariant.Ghost -> colors.brand
    }

    val clickable = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    // Every button dips slightly on press. A ripple alone reads flat on the
    // large, solid shapes this design uses, so the scale carries the feedback
    // and the ripple stays for Android's sake.
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && clickable) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "buttonPress",
    )

    var base = modifier
        .scale(scale)
        .defaultMinSize(minHeight = minHeight)
        .alpha(if (clickable) 1f else 0.45f)

    // Background per variant.
    base = when (variant) {
        ButtonVariant.Primary -> base
            .background(colors.brand, shape)
            .border(BorderStroke(1.dp, colors.brandHover), shape)
        ButtonVariant.Destructive -> base.background(colors.errorFg, shape)
        ButtonVariant.Secondary -> base
            .background(colors.surface, shape)
            .border(BorderStroke(1.dp, colors.border), shape)
        ButtonVariant.Ghost -> base
    }

    Box(
        modifier = base
            .clickable(
                enabled = clickable,
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                onClick = onClick,
            )
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                )
            } else {
                leading?.let {
                    CompositionLocalProvider(LocalTextStyle provides textStyle) { it() }
                }
            }
            Text(text = text, style = textStyle, color = contentColor)
        }
    }
}

// ── Named shorthands, so call sites read as the design does ──────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: ButtonSize = ButtonSize.Default,
    leading: @Composable (() -> Unit)? = null,
) = HamsaButton(text, onClick, modifier, ButtonVariant.Primary, size, enabled, loading, leading)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Default,
    leading: @Composable (() -> Unit)? = null,
) = HamsaButton(text, onClick, modifier, ButtonVariant.Secondary, size, enabled, leading = leading)

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Small,
    leading: @Composable (() -> Unit)? = null,
) = HamsaButton(text, onClick, modifier, ButtonVariant.Ghost, size, enabled, leading = leading)

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Default,
    leading: @Composable (() -> Unit)? = null,
) = HamsaButton(text, onClick, modifier, ButtonVariant.Destructive, size, enabled, leading = leading)
