package com.aalaya.pg.feature.template

import androidx.compose.foundation.background
import com.aalaya.pg.ui.components.HamsaTopBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.ErrorState
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.LoadingState
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

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
            HamsaTextField(value = state.accent, onValueChange = viewModel::onAccent, label = "Accent colour hex (e.g. #0E70C7)")

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
