package com.aalaya.pg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * `.stat-grid` — lays [StatTile]s out [columns]-per-row with a 12dp gutter.
 * Tiles in a row are stretched to equal height so a caption on one tile doesn't
 * leave its neighbour short. An odd final tile keeps its column width instead
 * of stretching across the row.
 */
@Composable
fun StatGrid(
    tiles: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
    ) {
        tiles.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
            ) {
                row.forEach { tile ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) { tile() }
                }
                repeat(columns - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
