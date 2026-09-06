package com.aalaya.pg.feature.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.HamsaTopBar
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

/** Well-known accent colours shown in the branding colour picker. */
private data class PresetColor(val label: String, val hex: String, val compose: Color)

private val PRESET_COLORS = listOf(
    PresetColor("Ocean Blue",    "#0E70C7", Color(0xFF0E70C7)),
    PresetColor("Emerald",       "#059669", Color(0xFF059669)),
    PresetColor("Crimson",       "#DC2626", Color(0xFFDC2626)),
    PresetColor("Indigo",        "#4F46E5", Color(0xFF4F46E5)),
    PresetColor("Amber",         "#D97706", Color(0xFFD97706)),
    PresetColor("Teal",          "#0D9488", Color(0xFF0D9488)),
    PresetColor("Slate",         "#475569", Color(0xFF475569)),
    PresetColor("Rose",          "#E11D48", Color(0xFFE11D48)),
)

@Composable
fun TemplateScreen(
    onBack: () -> Unit = {},
    viewModel: TemplateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(HamsaTheme.colors.canvas)) {
        HamsaTopBar(title = "Receipt branding", onBack = onBack)
        when {
            state.loading -> LoadingState(Modifier.padding(horizontal = HamsaTheme.spacing.screenH, vertical = HamsaTheme.spacing.screenTop))
            state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(HamsaTheme.spacing.lg),
            ) {
                Text(
                    "Shown on every generated receipt PDF.",
                    style = HamsaTheme.type.bodySmall,
                    color = HamsaTheme.colors.textTertiary,
                )
                Spacer(Modifier.height(HamsaTheme.spacing.md))

                HamsaTextField(value = state.displayName, onValueChange = viewModel::onDisplayName, label = "Display name")
                Spacer(Modifier.height(HamsaTheme.spacing.xs))
                HamsaTextField(value = state.address, onValueChange = viewModel::onAddress, label = "Address (optional)", singleLine = false)
                Spacer(Modifier.height(HamsaTheme.spacing.xs))
                HamsaTextField(value = state.footer, onValueChange = viewModel::onFooter, label = "Footer note (optional)", singleLine = false)
                Spacer(Modifier.height(HamsaTheme.spacing.xs))

                // ── Accent colour picker ────────────────────────────────────
                AccentColorPicker(
                    selected = state.accent,
                    onSelect = viewModel::onAccent,
                )

                state.saveError?.let {
                    Spacer(Modifier.height(HamsaTheme.spacing.xs))
                    Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.errorFg)
                }

                Spacer(Modifier.height(HamsaTheme.spacing.lg))
                PrimaryButton(
                    text = "Save branding",
                    onClick = viewModel::save,
                    enabled = state.displayName.isNotBlank(),
                    loading = state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AccentColorPicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val colors = HamsaTheme.colors
    var expanded by remember { mutableStateOf(false) }

    val currentPreset = PRESET_COLORS.firstOrNull {
        it.hex.equals(selected.trim(), ignoreCase = true)
    }

    Text(
        "Accent colour",
        style = HamsaTheme.type.labelLarge,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = HamsaTheme.spacing.xs),
    )

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colour swatch
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(currentPreset?.compose ?: colors.brand),
            )
            Text(
                text = currentPreset?.label ?: (selected.ifBlank { "Choose a colour…" }),
                style = HamsaTheme.type.bodyMedium,
                color = if (currentPreset != null) colors.textPrimary else colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = colors.textTertiary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            PRESET_COLORS.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preset.compose),
                            )
                            Text(
                                text = preset.label,
                                style = HamsaTheme.type.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            if (preset.hex.equals(selected.trim(), ignoreCase = true)) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colors.brand,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(preset.hex)
                        expanded = false
                    },
                )
            }
        }
    }
}
