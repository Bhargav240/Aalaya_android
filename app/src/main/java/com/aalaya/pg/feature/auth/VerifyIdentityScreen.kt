package com.aalaya.pg.feature.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.HamsaCard
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * First-login identity verification gate (PRD flow 1). Tenants land here once,
 * right after their first sign-in, and unlock the app when the check passes.
 */
@Composable
fun VerifyIdentityScreen(
    registeredName: String?,
    onSignOut: () -> Unit,
    viewModel: VerifyIdentityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(HamsaTheme.spacing.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Verify your identity",
            style = HamsaTheme.type.headlineMedium,
            color = HamsaTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(HamsaTheme.spacing.xs))
        Text(
            "A one-time check so the PG knows who is actually living there. " +
                "Your DigiLocker record must match the registered name" +
                (registeredName?.let { " ($it)" } ?: "") + ".",
            style = HamsaTheme.type.bodyLarge,
            color = HamsaTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(HamsaTheme.spacing.lg))

        HamsaCard {
            HamsaTextField(
                value = state.providerRef,
                onValueChange = viewModel::onProviderRef,
                label = "DigiLocker reference / Aadhaar consent ID",
            )
            Spacer(Modifier.height(HamsaTheme.spacing.sm))
            HamsaTextField(
                value = state.phone,
                onValueChange = viewModel::onPhone,
                label = "Phone number",
                keyboardType = KeyboardType.Phone,
            )
            Spacer(Modifier.height(HamsaTheme.spacing.sm))
            HamsaTextField(
                value = state.emergencyContact,
                onValueChange = viewModel::onEmergencyContact,
                label = "Emergency contact (name & phone)",
            )
        }

        state.error?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.sm))
            Text(it, style = HamsaTheme.type.bodySmall, color = HamsaTheme.colors.errorFg)
        }

        Spacer(Modifier.height(HamsaTheme.spacing.lg))
        PrimaryButton(
            text = "Verify",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HamsaTheme.spacing.sm))
        SecondaryButton(
            text = "Sign out",
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HamsaTheme.spacing.md))
        Text(
            "Verification runs once. If it fails, check that your documents carry " +
                "your exact registered name, or contact your branch manager.",
            style = HamsaTheme.type.bodySmall,
            color = HamsaTheme.colors.textTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
