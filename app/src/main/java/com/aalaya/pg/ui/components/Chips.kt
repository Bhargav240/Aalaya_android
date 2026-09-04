package com.aalaya.pg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aalaya.pg.core.domain.model.ComplaintState
import com.aalaya.pg.ui.theme.HamsaTheme

enum class StatusTone { Neutral, Success, Warning, Error, Info, Brand, Gold }

/**
 * `.badge` — a small status pill with a leading dot in the foreground colour
 * (`.badge::before`). Used for resident state, complaint stage, occupancy
 * counts, receipt paid/unpaid.
 */
@Composable
fun StatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = HamsaTheme.colors
    val (bg: Color, fg: Color) = when (tone) {
        StatusTone.Success -> colors.successBg to colors.successFg
        StatusTone.Warning -> colors.warningBg to colors.warningFg
        StatusTone.Error -> colors.errorBg to colors.errorFg
        StatusTone.Info -> colors.brandSubtle to colors.brand
        StatusTone.Brand -> colors.brandSubtle to colors.brand
        StatusTone.Gold -> colors.goldBg to colors.gold
        StatusTone.Neutral -> colors.canvas to colors.textSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(bg, RoundedCornerShape(HamsaTheme.radii.pill))
            .border(BorderStroke(1.dp, fg.copy(alpha = 0.18f)), RoundedCornerShape(HamsaTheme.radii.pill))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Spacer(Modifier.size(6.dp).background(fg, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = HamsaTheme.type.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = fg,
        )
    }
}

/**
 * `.chip` — a selectable pill used for the occupancy filters (sharing, cooling)
 * and other single-choice rows. Selected state fills solid brand
 * (`.chip.on`); unselected is a bordered surface.
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HamsaTheme.colors
    val shape = RoundedCornerShape(HamsaTheme.radii.pill)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .defaultMinSize(minHeight = 36.dp)
            .background(if (selected) colors.brand else colors.surface, shape)
            .border(
                BorderStroke(1.dp, if (selected) colors.brand else colors.border),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = HamsaTheme.type.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) Color.White else colors.textSecondary,
        )
    }
}

/**
 * `.tag` — a small non-interactive descriptor shown on room cards
 * ("3-sharing", "AC", "₹9,500/bed"). The `accent` form is `.tag.ac`.
 */
@Composable
fun Tag(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = HamsaTheme.colors
    Text(
        text = label,
        style = HamsaTheme.type.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (accent) colors.brand else colors.textSecondary,
        modifier = modifier
            .background(
                if (accent) colors.brandSubtle else colors.canvas,
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Maps a backend status string to a sensible tone. */
/**
 * Complaint states, coloured so the stage is readable at a glance:
 * blue = we have it, amber = we're on it, green = done, grey = closed.
 *
 * Kept separate from [toneForStatus] because "open" means different things for
 * a complaint and for an invitation, and one shared mapping cannot serve both.
 */
fun toneForComplaint(state: ComplaintState): StatusTone = when (state) {
    ComplaintState.Open -> StatusTone.Info
    ComplaintState.Assigned, ComplaintState.InProgress -> StatusTone.Warning
    ComplaintState.Resolved -> StatusTone.Success
    ComplaintState.Closed -> StatusTone.Neutral
}

fun toneForStatus(status: String): StatusTone = when (status.lowercase()) {
    "active", "paid", "approved", "resolved", "published", "applied", "kyc_verified" -> StatusTone.Success
    "pending", "in_progress", "open", "invited", "email_verified", "kyc_pending", "approval_pending" -> StatusTone.Warning
    "rejected", "failed", "kyc_failed", "expired", "cancelled", "canceled", "vacated", "moved_out" -> StatusTone.Error
    else -> StatusTone.Neutral
}
