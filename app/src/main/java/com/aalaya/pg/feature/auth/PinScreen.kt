package com.aalaya.pg.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aalaya.pg.core.data.PinStore
import com.aalaya.pg.ui.theme.HamsaTheme
import kotlinx.coroutines.launch

/**
 * Owner app-PIN unlock screen. Exactly the pattern from banking/UPI apps:
 * seven-round keypad entry of a 4-digit code. No biometrics integration yet —
 * the PIN is always the fallback, and that's the non-negotiable layer.
 */
@Composable
fun PinScreen(
    pinStore: PinStore,
    title: String,
    onUnlocked: () -> Unit,
    /** When set (first-time setup), a fresh 4-digit entry is saved then unlocks. */
    onSubmitPin: ((String) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val isSetup = onSubmitPin != null

    fun submit() {
        if (entry.length < 4) return
        if (isSetup) {
            onSubmitPin?.invoke(entry)
            return
        }
        scope.launch {
            val ok = pinStore.checkPin(entry)
            if (ok) {
                onUnlocked()
            } else {
                error = "Wrong PIN"
                entry = ""
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(HamsaTheme.colors.canvas).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = HamsaTheme.type.titleMedium, color = HamsaTheme.colors.textPrimary)
        Spacer(Modifier.height(HamsaTheme.spacing.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < entry.length) HamsaTheme.colors.brand else HamsaTheme.colors.border
                        ),
                )
            }
        }
        Spacer(Modifier.height(HamsaTheme.spacing.xl))
        Keypad(onDigit = {
            if (entry.length < 4) {
                entry += it
                error = null
                if (entry.length == 4) submit()
            }
        }, onDelete = { entry = entry.dropLast(1) })
        error?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            Text(it, style = HamsaTheme.type.labelLarge, color = HamsaTheme.colors.errorFg)
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf('1','2','3'), listOf('4','5','6'), listOf('7','8','9'), listOf('⌫','0',' '))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { cell ->
                    Button(
                        onClick = {
                            when (cell) {
                                '⌫' -> onDelete()
                                ' ' -> Unit
                                else -> onDigit(cell)
                            }
                        },
                        enabled = cell != ' ',
                    ) {
                        Text(cell.toString())
                    }
                }
            }
        }
    }
}
