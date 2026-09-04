package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme
import com.aalaya.pg.ui.theme.HeroAmountTextStyle
import com.aalaya.pg.ui.theme.OverlineTextStyle

/**
 * `.hero` — the tenant rent banner. A 135° brand gradient normally, switching
 * to the green `.hero.paid` gradient when nothing is owed.
 *
 * In dark mode the design flattens this to a plain bordered surface with normal
 * text colours (`body.dark .hero`), so the whole component re-colours rather
 * than just swapping the gradient.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroCard(
    overline: String,
    amount: String,
    caption: String,
    paid: Boolean,
    modifier: Modifier = Modifier,
    secondaryCaption: String? = null,
    chips: List<String> = emptyList(),
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(HamsaTheme.radii.xl)

    val onHero = if (colors.isDark) colors.textPrimary else Color.White
    val onHeroMuted = if (colors.isDark) colors.textSecondary else Color.White.copy(alpha = 0.85f)
    val onHeroFaint = if (colors.isDark) colors.textTertiary else Color.White.copy(alpha = 0.75f)

    val background = if (paid) {
        Brush.linearGradient(listOf(colors.heroPaidFrom, colors.heroPaidTo))
    } else {
        Brush.linearGradient(listOf(colors.heroFrom, colors.heroTo))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background, shape)
            .then(
                if (colors.isDark) Modifier.border(1.dp, colors.border, shape) else Modifier,
            )
            .padding(22.dp),
    ) {
        Text(text = overline.uppercase(), style = OverlineTextStyle, color = onHeroFaint)
        AutoSizeText(
            text = amount,
            style = HeroAmountTextStyle,
            color = onHero,
            minFontScale = 0.6f,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = caption,
            style = HamsaTheme.type.bodySmall,
            color = onHeroMuted,
            modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
        )
        if (secondaryCaption != null) {
            Text(
                text = secondaryCaption,
                style = HamsaTheme.type.bodySmall,
                color = onHeroMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip ->
                    Text(
                        text = chip,
                        style = HamsaTheme.type.labelMedium,
                        color = onHeroMuted,
                        modifier = Modifier
                            .background(
                                if (colors.isDark) colors.canvas else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(HamsaTheme.radii.pill),
                            )
                            .then(
                                if (colors.isDark) {
                                    Modifier.border(
                                        1.dp,
                                        colors.border,
                                        RoundedCornerShape(HamsaTheme.radii.pill),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}
