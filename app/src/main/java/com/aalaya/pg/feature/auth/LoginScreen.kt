package com.aalaya.pg.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aalaya.pg.ui.components.GhostButton
import com.aalaya.pg.ui.components.HamsaTextField
import com.aalaya.pg.ui.components.LogoMark
import com.aalaya.pg.ui.components.NoteBanner
import com.aalaya.pg.ui.components.OtpCodeField
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * The unauthenticated surface, in three panels: a welcome that offers both
 * doors, an email field, then the code emailed to it.
 *
 * Sign-up and log-in are the same two backend calls — an unknown email is
 * provisioned as a prospect on first request — so the panels differ only in
 * wording. That is not cosmetic: someone tapping "Sign up" must not be greeted
 * with "Welcome back", and a prospective tenant must not be told that
 * residents join by invitation only, because they don't have to.
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HamsaTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HamsaTheme.spacing.xxl, vertical = HamsaTheme.spacing.xxxl),
    ) {
        if (state.step == AuthStep.Welcome) {
            WelcomeScreen(
                onSignUp = viewModel::startSignUp,
                onLogIn = viewModel::startLogIn,
                onContinueWithGoogle = viewModel::continueWithGoogle,
            )
            return@Column
        }

        val signingUp = state.intent == AuthIntent.SignUp

        Spacer(Modifier.height(HamsaTheme.spacing.xxxl))
        LogoMark(size = 56.dp)
        Spacer(Modifier.height(HamsaTheme.spacing.xxl))

        Text(
            text = when {
                state.otpSent -> "Check your email"
                signingUp -> "Create your account"
                else -> "Welcome back"
            },
            style = HamsaTheme.type.displayMedium,
            color = colors.textPrimary,
        )
        Text(
            text = when {
                state.otpSent -> "Enter the code sent to ${state.email}"
                signingUp -> "We'll email you a one-time code to get started. No password to remember."
                else -> "We'll email you a one-time code to sign in."
            },
            style = HamsaTheme.type.bodyLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = HamsaTheme.spacing.sm),
        )

        Spacer(Modifier.height(HamsaTheme.spacing.xxl))

        if (!state.otpSent) {
            if (signingUp) {
                HamsaTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onFullName,
                    label = "Full name",
                    placeholder = "Your full name",
                )
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                HamsaTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhone,
                    label = "Phone number",
                    placeholder = "10-digit mobile number",
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(HamsaTheme.spacing.md))
            }
            HamsaTextField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                label = "Email address",
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
            )
        } else {
            OtpCodeField(
                value = state.otpCode,
                onValueChange = viewModel::onOtpCode,
            )
            if (state.isNewDevice) {
                Spacer(Modifier.height(HamsaTheme.spacing.md))
                NoteBanner(text = "New device? Confirm it's you.")
            }
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HamsaTheme.spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.resendSecondsLeft > 0) {
                    val minutes = state.resendSecondsLeft / 60
                    val seconds = state.resendSecondsLeft % 60
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Resend Code in ",
                            style = HamsaTheme.type.labelMedium,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = HamsaTheme.type.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                        )
                    }
                } else {
                    GhostButton(
                        text = "Resend Code",
                        onClick = viewModel::resend,
                    )
                }
                GhostButton(
                    text = "Change email",
                    onClick = viewModel::changeEmail,
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            Text(it, style = HamsaTheme.type.bodySmall, color = colors.errorFg)
        }

        Spacer(Modifier.height(HamsaTheme.spacing.xxl))
        PrimaryButton(
            text = if (!state.otpSent) "Send code" else "Verify",
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HamsaTheme.spacing.md))
        GhostButton(
            text = "Back",
            onClick = viewModel::backToWelcome,
        )
    }
}
