package com.aalaya.pg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * The Android top app bar from the design: a flat surface strip carrying the
 * screen's large Manrope title, an optional back button and trailing actions.
 *
 * Screens previously rendered their own `Text("Dashboard", …)` as the first
 * list item, which let title size, weight and top padding drift from screen to
 * screen. Everything routes through here now.
 */
@Composable
fun HamsaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val colors = HamsaTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HamsaTheme.spacing.lg, vertical = HamsaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.sm),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                    )
                }
            }
            Text(
                text = title,
                style = HamsaTheme.type.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            actions?.invoke()
        }
        // The hairline keeps the app bar distinct from dense data screens in
        // either theme without relying on a heavy elevated shadow.
        HorizontalDivider(color = colors.border)
    }
}

/**
 * Standard screen shell: top app bar over a canvas-coloured scrolling list,
 * with the design's screen padding (16dp sides, 20dp top, 40dp bottom) and an
 * optional muted lead paragraph under the title.
 *
 * [content] is a LazyListScope so long lists stay virtualised.
 */
@Composable
fun HamsaScreen(
    title: String,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier,
    lead: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = HamsaTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        if (showTopBar) {
            HamsaTopBar(title = title, onBack = onBack, actions = actions)
        }

        val list: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = HamsaTheme.spacing.screenH,
                    end = HamsaTheme.spacing.screenH,
                    top = HamsaTheme.spacing.screenTop,
                    bottom = HamsaTheme.spacing.screenBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.stack),
            ) {
                if (lead != null) {
                    item("__lead") {
                        Text(
                            text = lead,
                            style = HamsaTheme.type.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
                content()
            }
        }

        if (onRefresh != null) {
            Refreshable(refreshing = refreshing, onRefresh = onRefresh) { list() }
        } else {
            Box(Modifier.fillMaxSize()) { list() }
        }
    }
}
