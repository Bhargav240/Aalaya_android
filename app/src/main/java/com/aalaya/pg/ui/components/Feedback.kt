package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * What a screen shows while its first page of data is in flight.
 *
 * This draws the *shape* of the content to come — stacked [SkeletonCard]s —
 * rather than a spinner, so the layout doesn't jump when rows arrive and the
 * wait reads as progress. Pass `spinner = true` for the cases where there is no
 * list to foreshadow (rendering a PDF, submitting a form).
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    skeletonCount: Int = 3,
    spinner: Boolean = false,
) {
    if (spinner) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HamsaTheme.colors.brand)
        }
    } else {
        SkeletonList(modifier = modifier, count = skeletonCount)
    }
}

/**
 * `.empty` — a large tinted icon plaque, a headline and a muted explanation.
 * The design uses this same shape for every "nothing here yet" state (no free
 * beds, no receipts, empty approval queue), so screens should not hand-roll
 * their own centred text.
 *
 * The plaque is a rounded square rather than a circle, which sits better under
 * the app's card geometry, and [tone] lets a failure state reuse the whole
 * layout in amber.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null,
    tone: IconTone = IconTone.Brand,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HamsaTheme.spacing.xxl, vertical = HamsaTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.lg),
    ) {
        if (icon != null) {
            IconTile(
                icon = icon,
                tone = tone,
                size = 68.dp,
                radius = HamsaTheme.radii.xxl,
                iconSize = 30.dp,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.xs),
        ) {
            Text(
                text = title,
                style = HamsaTheme.type.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = HamsaTheme.type.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        action?.invoke()
    }
}

/**
 * The failure twin of [EmptyState]: an amber plaque and a brand-tinted pill to
 * try again. The retry reads as a quiet suggestion rather than a primary
 * action, because the user did nothing wrong.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    EmptyState(
        modifier = modifier,
        title = "Something went wrong",
        detail = message,
        icon = Icons.Outlined.WarningAmber,
        tone = IconTone.Warning,
        action = onRetry?.let { retry -> { RetryPill(onClick = retry) } },
    )
}

@Composable
private fun RetryPill(onClick: () -> Unit) {
    val colors = HamsaTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(colors.brandSubtle, RoundedCornerShape(HamsaTheme.radii.pill))
            .pressScale(onClick = onClick)
            .padding(horizontal = HamsaTheme.spacing.xxl, vertical = HamsaTheme.spacing.sm),
    ) {
        Text(
            text = "Try again",
            style = HamsaTheme.type.labelLarge,
            color = colors.brand,
        )
    }
}
