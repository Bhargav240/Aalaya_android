package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.steps` — the vertical progress tracker used by the complaint screens.
 *
 * Each step is one of three states, matching `.step.done` / `.step.cur` /
 * `.step.todo`: a filled brand dot with a tick, a ringed brand dot, or an empty
 * outlined dot. The connecting line is brand-coloured up to the current step.
 */
@Composable
fun Stepper(
    steps: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = HamsaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, label ->
            val done = index < currentIndex
            val current = index == currentIndex
            val isLast = index == steps.lastIndex

            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Indicator column: dot plus the line down to the next step.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                when {
                                    done -> Modifier.background(colors.brand, CircleShape)
                                    current -> Modifier
                                        .background(colors.brandSubtle, CircleShape)
                                        .border(2.dp, colors.brand, CircleShape)
                                    else -> Modifier
                                        .background(colors.surface, CircleShape)
                                        .border(2.dp, colors.border, CircleShape)
                                },
                            ),
                    ) {
                        when {
                            done -> Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                            current -> Box(
                                Modifier.size(8.dp).background(colors.brand, CircleShape),
                            )
                            else -> Unit
                        }
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .defaultMinSize(minHeight = 22.dp)
                                .background(
                                    if (done) colors.brand else colors.border,
                                    RoundedCornerShape(1.dp),
                                ),
                        )
                    }
                }

                Text(
                    text = label,
                    style = HamsaTheme.type.bodySmall.copy(
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = when {
                        current -> colors.textPrimary
                        done -> colors.textSecondary
                        else -> colors.textTertiary
                    },
                    modifier = Modifier
                        .padding(start = HamsaTheme.spacing.md, bottom = HamsaTheme.spacing.stack)
                        .align(Alignment.CenterVertically),
                )
            }
        }
    }
}

/** Horizontal progress pips (`.prog`) used by the multi-step auth flows. */
@Composable
fun StepProgress(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val colors = HamsaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (index < current) colors.brand else colors.canvas,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}
