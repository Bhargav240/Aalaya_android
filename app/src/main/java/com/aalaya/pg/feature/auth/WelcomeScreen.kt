package com.aalaya.pg.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aalaya.pg.BuildConfig
import com.aalaya.pg.ui.components.ButtonSize
import com.aalaya.pg.ui.components.LogoMark
import com.aalaya.pg.ui.components.PrimaryButton
import com.aalaya.pg.ui.components.SecondaryButton
import com.aalaya.pg.ui.theme.HamsaTheme

/**
 * The first thing anyone sees who isn't signed in.
 *
 * This app is opened by two very different people: a resident or staff member
 * who already belongs to a property, and someone who has simply heard of the
 * place and wants to look. The old screen spoke only to the first — it opened
 * with "Welcome back" and closed by saying residents join by invitation only,
 * which told a prospective tenant they were in the wrong place. They were not:
 * an unknown email that asks for a code is provisioned as a prospect and lands
 * in Explore.
 *
 * So the entry point leads with the browsing promise and offers both doors.
 */
@Composable
fun WelcomeScreen(
    onSignUp: () -> Unit,
    onLogIn: () -> Unit,
    onContinueWithGoogle: () -> Unit,
) {
    val colors = HamsaTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(HamsaTheme.spacing.xxxl))
        LogoMark(size = 56.dp)
        Spacer(Modifier.height(HamsaTheme.spacing.xxl))

        Text(
            text = "Find a place that feels like home.",
            style = HamsaTheme.type.displayMedium,
            color = colors.textPrimary,
        )
        Text(
            text = "Browse PGs near you, see the rooms that are actually free, " +
                "and keep your rent, receipts and complaints in one place.",
            style = HamsaTheme.type.bodyLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = HamsaTheme.spacing.md),
        )

        Spacer(Modifier.height(HamsaTheme.spacing.xxxl))

        // Shown only when a Google OAuth client is configured; see
        // GOOGLE_SIGN_IN_ENABLED in build.gradle.kts.
        if (BuildConfig.GOOGLE_SIGN_IN_ENABLED) {
            SecondaryButton(
                text = "Continue with Google",
                onClick = onContinueWithGoogle,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Icon(
                        Icons.Outlined.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            Spacer(Modifier.height(HamsaTheme.spacing.md))
            OrDivider()
            Spacer(Modifier.height(HamsaTheme.spacing.md))
        }

        PrimaryButton(
            text = "Sign up",
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HamsaTheme.spacing.md))
        SecondaryButton(
            text = "Log in",
            onClick = onLogIn,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(HamsaTheme.spacing.xl))
        Text(
            text = "Already living in an Aalaya property? Log in with the " +
                "email your manager registered.",
            style = HamsaTheme.type.bodySmall,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun OrDivider() {
    val colors = HamsaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HamsaTheme.spacing.md),
    ) {
        androidx.compose.material3.HorizontalDivider(
            color = colors.border,
            modifier = Modifier.weight(1f),
        )
        Text("or", style = HamsaTheme.type.labelMedium, color = colors.textTertiary)
        androidx.compose.material3.HorizontalDivider(
            color = colors.border,
            modifier = Modifier.weight(1f),
        )
    }
}
